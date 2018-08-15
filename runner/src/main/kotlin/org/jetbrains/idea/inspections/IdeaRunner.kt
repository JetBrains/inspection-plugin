package org.jetbrains.idea.inspections

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.createCommandLineApplication
import com.intellij.idea.getCommandLineApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PlatformUtils
import org.jetbrains.intellij.Runner
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.lang.management.ManagementFactory
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException


abstract class IdeaRunner<T : Runner.Parameters>(private val testMode: Boolean) : AbstractRunner<T>() {

    abstract fun analyze(files: Collection<File>, projectName: String, moduleName: String, parameters: T): Boolean

    fun openProject(projectDir: File, projectName: String, moduleName: String): Project {
        logger.info("InspectionPlugin: Before project creation at '$projectDir'")
        var ideaProject: Project? = null
        val projectFile = File(projectDir, projectName + ProjectFileType.DOT_DEFAULT_EXTENSION)
        invokeAndWait {
            ideaProject = ProjectUtil.openOrImport(
                    projectFile.absolutePath,
                    /* projectToClose = */ null,
                    /* forceOpenInNewFrame = */ true
            )
        }
        return ideaProject?.apply {
            val rootManager = ProjectRootManager.getInstance(this)
            logger.info("InspectionPlugin: Project SDK name: " + rootManager.projectSdkName)
            logger.info("InspectionPlugin: Project SDK: " + rootManager.projectSdk)

            val modules = ModuleManager.getInstance(this).modules.toList()
            for (module in modules) {
                if (module.name != moduleName) continue
                val moduleRootManager = ModuleRootManager.getInstance(module)
                val dependencyEnumerator =
                        moduleRootManager.orderEntries().compileOnly().recursively().exportedOnly()
                var dependsOnKotlinCommon = false
                var dependsOnKotlinJS = false
                var dependsOnKotlinJVM = false
                dependencyEnumerator.forEach { orderEntry ->
                    if (orderEntry is LibraryOrderEntry) {
                        val library = orderEntry.library
                        if (library != null) {
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-common" in it }) {
                                dependsOnKotlinCommon = true
                            }
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-js" in it }) {
                                dependsOnKotlinJS = true
                            }
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-jdk" in it || "$KT_LIB-1" in it }) {
                                dependsOnKotlinJVM = true
                            }
                        }
                    }
                    true
                }
                when {
                    dependsOnKotlinJVM ->
                        logger.info("InspectionPlugin: Under analysis: Kotlin JVM module $module with SDK: " + moduleRootManager.sdk)
                    dependsOnKotlinJS ->
                        logger.warn("InspectionPlugin: Under analysis: Kotlin JS module $module (JS SDK is not supported yet)")
                    dependsOnKotlinCommon ->
                        logger.warn("InspectionPlugin: Under analysis: Kotlin common module $module (common SDK is not supported yet)")
                    else ->
                        logger.info("InspectionPlugin: Under analysis: pure Java module $module with SDK: " + moduleRootManager.sdk)
                }
            }
        } ?: run {
            throw RunnerException("Cannot open IDEA project: '${projectFile.absolutePath}'")
        }
    }

    private fun invokeAndWait(action: () -> Unit) = idea.invokeAndWait(action)

    fun runReadAction(action: () -> Unit) = idea.runReadAction(action)

    private var application: ApplicationEx? = null

    private var systemPathMarker: FileChannel? = null

    private var ideaLockChannel: FileChannel? = null
    private var ideaLock: FileLock? = null

    private var shutdownLockChannel: FileChannel? = null
    private var shutdownLock: FileLock? = null

    private val idea: ApplicationEx
        get() = application ?: throw IllegalStateException("Idea not runned")

    private data class BuildConfiguration(val buildNumber: String, val usesUltimate: Boolean)

    private val File.buildConfiguration: BuildConfiguration
        get() = let { buildFile ->
            if (buildFile.exists()) {
                val text = buildFile.readText()
                val usesUltimate = text.startsWith("IU")
                text.dropWhile { !it.isDigit() }.let {
                    BuildConfiguration(if (it.isNotEmpty()) it else DEFAULT_BUILD_NUMBER, usesUltimate)
                }
            } else {
                BuildConfiguration(DEFAULT_BUILD_NUMBER, false)
            }
        }

    override fun run(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            ideaHomeDirectory: File,
            parameters: T
    ): Boolean {
        // Don't delete, change and downgrade level of log because this
        // information used in unit tests for identification of daemon.
        logger.info("InspectionPlugin: Daemon PID is ${getPID()}")
        acquireIdeaLockIfNeeded()
        logger.info("InspectionPlugin: Class loader: " + this.javaClass.classLoader)
        try {
            application = loadApplication(ideaHomeDirectory)
        } catch (e: Throwable) {
            if (e is RunnerException) throw e
            throw RunnerException("Exception caught in inspection plugin (IDEA loading): $e", e)
        }
        try {
            application?.doNotSave()
            application?.configureJdk()
            return analyze(files, projectName, moduleName, parameters)
        } catch (e: Throwable) {
            if (e is RunnerException) throw e
            throw RunnerException("Exception caught in inspection plugin (IDEA readAction): $e", e)
        } finally {
            systemPathMarker?.close()
        }
    }

    private fun Application.configureJdk() {
        logger.info("InspectionPlugin: Before SDK configuration")
        invokeAndWait {
            runWriteAction {
                val javaHomePath = System.getenv(JAVA_HOME) ?: ""
                val jdkTable = ProjectJdkTable.getInstance()
                for ((jdkVersion, jdkEnvironmentVariable) in JDK_ENVIRONMENT_VARIABLES) {
                    if (jdkTable.findJdk(jdkVersion) != null) continue
                    val homePath = System.getenv(jdkEnvironmentVariable)
                            ?: if (jdkVersion in javaHomePath && "jdk" in javaHomePath) javaHomePath else continue
                    logger.info("InspectionPlugin: Configuring JDK $jdkVersion")
                    val sdk = SdkConfigurationUtil.createAndAddSDK(
                            FileUtil.toSystemIndependentName(homePath),
                            JavaSdk.getInstance()
                    ) ?: continue
                    logger.info("InspectionPlugin: Home path is ${sdk.homePath}, version string is ${sdk.versionString}")
                }
            }
        }
    }

    private fun loadApplication(ideaHomeDirectory: File): ApplicationEx {
        System.setProperty(IDEA_HOME_PATH, ideaHomeDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        val ideaBuildNumberFile = File(ideaHomeDirectory, "build.txt")
        val (buildNumber, usesUltimate) = ideaBuildNumberFile.buildConfiguration
        System.setProperty(BUILD_NUMBER, buildNumber)
        val systemPath = generateSystemPath(buildNumber, usesUltimate)
        System.setProperty(SYSTEM_PATH, systemPath)
        val platformPrefix = if (usesUltimate) PlatformUtils.IDEA_PREFIX else PlatformUtils.IDEA_CE_PREFIX
        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(platformPrefix))

        logger.info("InspectionPlugin: IDEA home dir: $ideaHomeDirectory")
        logger.info("InspectionPlugin: IDEA home path: " + PathManager.getHomePath())
        logger.info("InspectionPlugin: IDEA system path: $systemPath")
        if (usesUltimate)
            throw RunnerException("Using of IDEA Ultimate is not yet supported in inspection runner")
        if (getCommandLineApplication() != null) {
            logger.info("InspectionPlugin: IDEA command line application already exists, don't bother to run it again")
            val realIdeaHomeDirectory = File(PathManager.getHomePath())
            val ideaHomePath = ideaHomeDirectory.canonicalPath
            val realIdeaHomePath = realIdeaHomeDirectory.canonicalPath
            if (ideaHomePath != realIdeaHomePath)
                throw RunnerException("IDEA command line application already exists, but have other instance: $ideaHomePath and $realIdeaHomePath")
            return ApplicationManagerEx.getApplicationEx()
        }
        logger.info("InspectionPlugin: IDEA starting in command line mode")
        createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
        USELESS_PLUGINS.forEach { PluginManagerCore.disablePlugin(it) }

//        val kotlinPluginDirectory = File(ideaHomeDirectory, "plugins/Kotlin")
//        val kotlinPluginDescriptor = IdeaPluginDescriptorImpl(kotlinPluginDirectory)
//        kotlinPluginDescriptor.pluginId
//        val plugins = PluginManagerCore.getPlugins() + kotlinPluginDescriptor
//        PluginManagerCore.setPlugins(plugins)

        // Do not remove the call of PluginManagerCore.getPlugins(), it prevents NPE in IDEA
        // NB: IdeaApplication.getStarter() from IJ community contains the same call
        val enabledPlugins = PluginManagerCore.getPlugins().map { it.name to it.pluginId }.toMap()
        val disabledPlugins = PluginManagerCore.getDisabledPlugins().toList()
        logger.info("InspectionPlugin: Enabled plugins: $enabledPlugins")
        logger.info("InspectionPlugin: Disabled plugins $disabledPlugins")
        return ApplicationManagerEx.getApplicationEx().apply { load() }
    }

    private fun generateSystemPath(buildNumber: String, usesUltimate: Boolean): String {
        val homeDir = System.getProperty(USER_HOME).replace("\\", "/")
        val buildPrefix = (if (usesUltimate) "U_" else "") + buildNumber.replace(".", "_")
        var path: String
        var code = 0
        do {
            code++
            path = "$homeDir/.IntellijIDEAInspections/${buildPrefix}_code$code/system"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            val systemMarkerFile = File(file, SYSTEM_MARKER_FILE)
            systemMarkerFile.createNewFile()
            // To prevent usages by multiple processes
            val lock = try {
                systemPathMarker = FileChannel.open(systemMarkerFile.toPath(), StandardOpenOption.WRITE)
                systemPathMarker?.tryLock()
            } catch (e: IOException) {
                logger.warn("InspectionPlugin: IO exception while locking: ${e.message}")
                throw RunnerException("Exception caught in inspection plugin (IDEA system dir lock): $e", e)
            }
            if (lock == null && code == 256) {
                throw RunnerException("Cannot create IDEA system directory (all locked)")
            }
        } while (lock == null)
        return path
    }

    private fun lockAcquireIfNeeded(lockName: String): Pair<FileLock?, FileChannel?> {
        if (!LOCKS_DIRECTORY.exists()) LOCKS_DIRECTORY.mkdirs()
        val lockFile = File(LOCKS_DIRECTORY, lockName)
        if (!lockFile.exists()) lockFile.createNewFile()
        val channel = FileChannel.open(lockFile.toPath(), StandardOpenOption.WRITE)
        val lock = try {
            channel?.tryLock()
        } catch (ignore: OverlappingFileLockException) {
            null
        }
        when (lock) {
            null -> logger.info("InspectionPlugin: lock $lockName acquire skipped")
            else -> logger.info("InspectionPlugin: lock $lockName acquired")
        }
        return lock to channel
    }

    private fun lockRelease(lock: FileLock?, channel: FileChannel?) {
        lock?.release()
        channel?.close()
        logger.info("InspectionPlugin: lock released")
    }

    private fun acquireIdeaLockIfNeeded() {
        val lockName = ideaLockFileName()
        val (lock, channel) = lockAcquireIfNeeded(lockName)
        ideaLock = lock
        ideaLockChannel = channel
    }

    private fun releaseIdeaLock() = lockRelease(ideaLock, ideaLockChannel)

    private fun acquireShutdownLockIfNeeded(): Boolean {
        val lockName = shutdownLockFileName()
        val (lock, channel) = lockAcquireIfNeeded(lockName)
        shutdownLock = lock
        shutdownLockChannel = channel
        return shutdownLock != null
    }

    private fun releaseShutdownLock() = lockRelease(shutdownLock, shutdownLockChannel)

    /**
     * Killed current gradle daemon for classpath resetting
     */
    private fun finishGradleDaemon() {
        // ToDo automatically finishing of gradle daemon
        logger.error("InspectionPlugin: Gradle daemon is alive. Please do it manually.")
    }

    override fun finalize() {
        val shutdownNeeded = acquireShutdownLockIfNeeded()
        if (!shutdownNeeded) {
            logger.info("InspectionPlugin: IDEA shutting down skipped.")
            return
        }
        // NB: exit is actually performed on EDT thread!
        logger.info("InspectionPlugin: IDEA shutting down.")
        val application = application
        when (application) {
            is ApplicationImpl -> application.exit(true, true, false)
            else -> application?.exit(true, true)
        }
        // Release not needed if application dispatch thread is death because
        // if dispatch is death then gradle daemon must be death. After death
        // of gradle daemon operation system automatically released all locks.
        application?.invokeLater {
            finishGradleDaemon()
            releaseIdeaLock()
            releaseShutdownLock()
        }
    }

    companion object {
        private const val AWT_HEADLESS = "java.awt.headless"
        private const val IDEA_HOME_PATH = "idea.home.path"
        private const val BUILD_NUMBER = "idea.plugins.compatible.build"
        private const val SYSTEM_PATH = "idea.system.path"
        private const val USER_HOME = "user.home"
        private const val SYSTEM_MARKER_FILE = "marker.ipl"
        private const val JAVA_HOME = "JAVA_HOME"
        private val JDK_ENVIRONMENT_VARIABLES = mapOf(
                "1.6" to "JDK_16",
                "1.7" to "JDK_17",
                "1.8" to "JDK_18"
                // TODO: un-comment me
                //"9" to "JDK_9"
        )

        // TODO: change to USEFUL_PLUGINS
        private val USELESS_PLUGINS = listOf(
                "mobi.hsz.idea.gitignore",
                "org.jetbrains.plugins.github",
                "Git4Idea"
        )

        private const val KT_LIB = "kotlin-stdlib"

        private const val DEFAULT_BUILD_NUMBER = "172.1"

        private val LOCKS_DIRECTORY = File(System.getProperty("java.io.tmpdir"), "inspection-plugin/locks")

        private const val IDEA_LOCK_EXTENSION = "idea-lock"

        private const val SHUTDOWN_LOCK_EXTENSION = "shutdown-lock"

        private fun getPID() = ManagementFactory.getRuntimeMXBean().name

        private fun ideaLockFileName() = "${getPID()}.$IDEA_LOCK_EXTENSION"

        private fun shutdownLockFileName() = "${getPID()}.$SHUTDOWN_LOCK_EXTENSION"
    }
}