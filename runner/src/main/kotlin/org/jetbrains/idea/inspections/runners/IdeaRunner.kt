package org.jetbrains.idea.inspections.runners

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
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
import org.jetbrains.intellij.ProxyLogger
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.plugins.Plugin
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption


abstract class IdeaRunner<T>(logger: ProxyLogger) : Runner<IdeaRunnerParameters<T>>(logger) {

    abstract fun analyze(project: Project, parameters: T): Boolean

    private fun openProject(projectDir: File, projectName: String, moduleName: String): Project {
        logger.info("Before project creation at '$projectDir'")
        val projectFile = File(projectDir, projectName + ProjectFileType.DOT_DEFAULT_EXTENSION)
        val ideaProject = invokeAndWait {
            when (projectFile.exists()) {
                true -> ProjectUtil.openOrImport(projectFile.absolutePath, null, true)
                false -> ProjectUtil.openOrImport(projectDir.absolutePath, null, true)
            }
        }
        return ideaProject?.apply {
            val rootManager = ProjectRootManager.getInstance(this)
            logger.info("Project SDK name: " + rootManager.projectSdkName)
            logger.info("Project SDK: " + rootManager.projectSdk)

            val modules = ModuleManager.getInstance(this).modules.toList()
            for (module in modules) {
                if (module.name != moduleName) continue
                val moduleRootManager = ModuleRootManager.getInstance(module)
                val dependencyEnumerator = moduleRootManager.orderEntries().compileOnly().recursively().exportedOnly()
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
                        logger.info("Under analysis: Kotlin JVM module $module with SDK: " + moduleRootManager.sdk)
                    dependsOnKotlinJS ->
                        logger.warn("Under analysis: Kotlin JS module $module (JS SDK is not supported yet)")
                    dependsOnKotlinCommon ->
                        logger.warn("Under analysis: Kotlin common module $module (common SDK is not supported yet)")
                    else ->
                        logger.info("Under analysis: pure Java module $module with SDK: " + moduleRootManager.sdk)
                }
            }
        } ?: run {
            throw RunnerException("Cannot open IDEA project: '${projectDir.absolutePath}'")
        }
    }

    fun <R> invokeAndWait(action: () -> R): R = idea.invoke(Application::invokeAndWait, action)

    fun <R> runReadAction(action: () -> R): R = idea.invoke(Application::runReadAction, action)

    fun <T, R> T.invoke(runner: T.(Runnable) -> Unit, action: () -> R): R {
        var result: R? = null
        runner(Runnable {
            result = action()
        })
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    private var application: ApplicationEx? = null

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

    override fun run(parameters: IdeaRunnerParameters<T>): Boolean {
        logger.info("Class loader: " + this.javaClass.classLoader)
        try {
            with(parameters) {
                application = loadApplication(ideaVersion, ideaHomeDirectory, ideaSystemDirectory, plugins)
                @Suppress("DEPRECATION")
                application?.doNotSave()
                application?.configureJdk()
                val project = openProject(projectDir, projectName, moduleName)
                return analyze(project, parameters.childParameters)
            }
        } catch (e: Throwable) {
            if (e is RunnerException) throw e
            throw RunnerException("Exception caught in inspection plugin: $e", e)
        }
    }

    private fun Application.configureJdk() {
        logger.info("Before SDK configuration")
        invokeAndWait {
            runWriteAction {
                val javaHomePath = System.getenv(JAVA_HOME) ?: ""
                val jdkTable = ProjectJdkTable.getInstance()
                for ((jdkVersion, jdkEnvironmentVariable) in JDK_ENVIRONMENT_VARIABLES) {
                    if (jdkTable.findJdk(jdkVersion) != null) continue
                    val homePath = System.getenv(jdkEnvironmentVariable)
                            ?: if (jdkVersion in javaHomePath && "jdk" in javaHomePath) javaHomePath else continue
                    logger.info("Configuring JDK $jdkVersion")
                    val sdk = SdkConfigurationUtil.createAndAddSDK(
                            FileUtil.toSystemIndependentName(homePath),
                            JavaSdk.getInstance()
                    ) ?: continue
                    logger.info("Home path is ${sdk.homePath}, version string is ${sdk.versionString}")
                }
            }
        }
    }

    private fun checkCompatibility(plugin: Plugin, plugins: List<IdeaPluginDescriptor>, buildNumber: String, ideaVersion: String) {
        val descriptor = plugins.find { it.name == plugin.name } ?: throw RunnerException("${plugin.name} not loaded")
        if (PluginManagerCore.isIncompatible(descriptor)) throw RunnerException("${plugin.name} not loaded")
        val pluginDescriptor = Plugin.PluginDescriptor(descriptor.version, descriptor.sinceBuild, descriptor.untilBuild)
        val ideaDescriptor = Plugin.IdeaDescriptor(ideaVersion, buildNumber)
        val reason = plugin.checkCompatibility(pluginDescriptor, ideaDescriptor) ?: return
        logger.info(reason)
        throw RunnerException("${plugin.name} not loaded")
    }

    private fun initializeIdeaPlugins(): Array<IdeaPluginDescriptor> {
        val errors = arrayListOf<String>()
        val descriptors = PluginManagerCore.loadDescriptors(null, errors)
        errors.forEach { logger.error(it) }
        if (errors.isNotEmpty()) throw RunnerException("Plugin descriptors loading error")
        val plugins = descriptors.map { it.pluginId.idString }.toSet()
        plugins.forEach { PluginManagerCore.enablePlugin(it) }
        plugins.filter { it !in USEFUL_PLUGINS }.forEach { PluginManagerCore.disablePlugin(it) }
        // Do not remove the call of PluginManagerCore.getPlugins(), it prevents NPE in IDEA
        // NB: IdeaApplication.getStarter() from IJ community contains the same call
        return PluginManagerCore.getPlugins()
    }

    private fun loadApplication(ideaVersion: String, ideaHomeDirectory: File, ideaSystemDirectory: File, plugins: List<Plugin>): ApplicationEx {
        val ideaBuildNumberFile = File(ideaHomeDirectory, "build.txt")
        val (buildNumber, usesUltimate) = ideaBuildNumberFile.buildConfiguration
        if (usesUltimate) throw RunnerException("Using of IDEA Ultimate is not yet supported in inspection runner")
        val systemPath = SystemPathManager.allocateSystemDirectory(ideaSystemDirectory, buildNumber, usesUltimate)
        val pluginsPath = plugins.joinToString(":") { it.directory.absolutePath }
        val platformPrefix = if (usesUltimate) PlatformUtils.IDEA_PREFIX else PlatformUtils.IDEA_CE_PREFIX

        System.setProperty(IDEA_HOME_PATH, ideaHomeDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        System.setProperty(BUILD_NUMBER, buildNumber)
        System.setProperty(SYSTEM_PATH, systemPath)
        System.setProperty(PLUGINS_PATH, pluginsPath)
        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(platformPrefix))

        logger.info("IDEA home path: $ideaHomeDirectory")
        logger.info("IDEA home path: ${PathManager.getHomePath()}")
        logger.info("IDEA plugin path: $pluginsPath")
        logger.info("IDEA plugin path: ${PathManager.getPluginsPath()}")
        logger.info("IDEA system path: $systemPath")
        if (getCommandLineApplication() != null) {
            logger.info("IDEA command line application already exists, don't bother to run it again.")
            val realIdeaHomeDirectory = File(PathManager.getHomePath())
            val ideaHomePath = ideaHomeDirectory.canonicalPath
            val realIdeaHomePath = realIdeaHomeDirectory.canonicalPath
            if (ideaHomePath != realIdeaHomePath)
                throw RunnerException("IDEA command line application already exists, but have other instance: $ideaHomePath and $realIdeaHomePath")
            return ApplicationManagerEx.getApplicationEx()
        }
        logger.info("IDEA starting in command line mode")
        createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
        val allPlugins = initializeIdeaPlugins()
        val disabledPlugins = PluginManagerCore.getDisabledPlugins()
        val enabledPlugins = allPlugins.filter { it.pluginId.idString !in disabledPlugins }
        logger.info("All plugins: ${allPlugins.map { it.name to it.pluginId }.toMap()}")
        logger.info("Enabled plugins: ${enabledPlugins.map { it.name to it.pluginId }.toMap()}")
        logger.info("Disabled plugins ${disabledPlugins.toList()}")
        plugins.forEach { checkCompatibility(it, enabledPlugins, buildNumber, ideaVersion) }
        return ApplicationManagerEx.getApplicationEx().apply { load() }
    }

    override fun finalize() {
        logger.info("IDEA shutting down")
        val application = application
        when (application) {
            is ApplicationImpl -> application.exit(false, true, false)
            else -> application?.exit(false, true)
        }
        logger.info("IDEA shutdown")
        SystemPathManager.freeSystemDirectory()
        logger.info("System lock free")
    }

    object SystemPathManager {
        @JvmStatic
        private var systemDirectory: File? = null

        @JvmStatic
        private var systemLockChannel: FileChannel? = null

        @JvmStatic
        private var systemLock: FileLock? = null

        fun allocateSystemDirectory(ideaSystemDirectory: File, buildNumber: String, usesUltimate: Boolean): String {
            systemDirectory?.let { return it.absolutePath }
            val systemDirectory = generateSystemPath(ideaSystemDirectory, buildNumber, usesUltimate)
            this.systemDirectory = systemDirectory
            return systemDirectory.absolutePath
        }

        fun freeSystemDirectory() {
            releaseSystemLock()
        }

        private fun generateSystemPath(ideaSystemDirectory: File, buildNumber: String, usesUltimate: Boolean): File {
            val buildPrefix = (if (usesUltimate) "U_" else "") + buildNumber.replace(".", "_")
            for (code in 1..256) {
                val file = File(ideaSystemDirectory, "${buildPrefix}_code$code/system")
                if (!file.exists()) file.mkdirs()
                if (!file.canWrite()) continue
                val systemMarkerFile = File(file, SYSTEM_MARKER_FILE)
                // To prevent usages by multiple processes
                if (acquireSystemLock(systemMarkerFile) == LockStatus.FREE) return file
            }
            throw RunnerException("Cannot create IDEA system directory (all locked)")
        }

        private fun acquireSystemLock(systemLockFile: File): LockStatus {
            systemLockFile.createNewFile()
            val channel = FileChannel.open(systemLockFile.toPath(), StandardOpenOption.WRITE)
            return try {
                systemLock = channel?.tryLock()
                when (systemLock) {
                    null -> LockStatus.USED
                    else -> LockStatus.FREE
                }
            } catch (ignore: OverlappingFileLockException) {
                throw RunnerException("IDEA system path is already used in current process")
            } catch (e: IOException) {
                throw RunnerException("IDEA system lock ${systemLockFile.name} locking: $e", e)
            }
        }

        private fun releaseSystemLock() {
            systemLock?.release()
            systemLockChannel?.close()
        }

        enum class LockStatus { FREE, USED }
    }

    companion object {
        private const val AWT_HEADLESS = "java.awt.headless"
        private const val IDEA_HOME_PATH = "idea.home.path"
        private const val BUILD_NUMBER = "idea.plugins.compatible.build"
        private const val SYSTEM_PATH = "idea.system.path"
        private const val PLUGINS_PATH = "plugin.path"
        private const val SYSTEM_MARKER_FILE = "marker.ipl"
        private const val JAVA_HOME = "JAVA_HOME"
        private val JDK_ENVIRONMENT_VARIABLES = mapOf(
                "1.6" to "JDK_16",
                "1.7" to "JDK_17",
                "1.8" to "JDK_18"
                // TODO: un-comment me
                //"9" to "JDK_9"
        )

        private val USEFUL_PLUGINS = setOf(
                "com.intellij", // IDEA CORE
                "org.jetbrains.kotlin", // Kotlin
                "org.intellij.intelliLang", // IntelliLang
                "org.jetbrains.plugins.javaFX", // JavaFX
                "org.jetbrains.plugins.gradle", // Gradle
                "org.jetbrains.idea.maven", // Maven Integration
                "com.intellij.properties", // Properties Support
                "org.intellij.groovy", // Groovy
                "JUnit" // JUnit
        )

        private const val KT_LIB = "kotlin-stdlib"

        private const val DEFAULT_BUILD_NUMBER = "172.1"
    }
}