package org.jetbrains.idea.inspections

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.createCommandLineApplication
import com.intellij.idea.getCommandLineApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.impl.ProjectManagerImpl
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PlatformUtils
import org.jetbrains.intellij.Analyzer
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

abstract class IdeaRunner<T: Analyzer.Parameters>(private val testMode: Boolean) : AbstractAnalyzer<T>() {

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

        private const val DEFAULT_BUILD_NUMBER = "172.1"
    }

    private var application: ApplicationEx? = null

    val idea: ApplicationEx
        get() = application ?: throw IllegalStateException("Idea not runned")

    private data class BuildConfiguration(val buildNumber: String, val usesUltimate: Boolean)

    private val File.buildConfiguration: BuildConfiguration
        get() = let {
            if (it.exists()) {
                val text = it.readText()
                val usesUltimate = text.startsWith("IU")
                text.dropWhile { !it.isDigit() }.let {
                    BuildConfiguration(if (it.isNotEmpty()) it else DEFAULT_BUILD_NUMBER, usesUltimate)
                }
            } else {
                BuildConfiguration(DEFAULT_BUILD_NUMBER, false)
            }
        }

    abstract fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            parameters: T
    ): Boolean

    override fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            ideaHomeDirectory: File,
            parameters: T
    ): Boolean {
        logger.info("Class loader: " + this.javaClass.classLoader)
        val (idea, systemPathMarkerChannel) = try {
            loadApplication(ideaHomeDirectory)
        } catch (e: Throwable) {
            logger.error("Exception during IDEA loading: " + e.message)
            if (e is InspectionRunnerException) throw e
            throw InspectionRunnerException("EXCEPTION caught in inspection plugin (IDEA loading): $e", e)
        }
        this.application = idea
        try {
            idea.doNotSave()
            idea.configureJdk()
            return analyze(files, projectName, moduleName, parameters)
        } catch (e: Throwable) {
            logger.error("Exception during IDEA runReadAction " + e.message)
            if (e is InspectionRunnerException) throw e
            throw InspectionRunnerException("EXCEPTION caught in inspection plugin (IDEA runReadAction): $e", e)
        } finally {
            systemPathMarkerChannel.close()
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

    private fun loadApplication(ideaHomeDirectory: File): Pair<ApplicationEx, FileChannel> {
        System.setProperty(IDEA_HOME_PATH, ideaHomeDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        val ideaBuildNumberFile = File(ideaHomeDirectory, "build.txt")
        val (buildNumber, usesUltimate) = ideaBuildNumberFile.buildConfiguration
        System.setProperty(BUILD_NUMBER, buildNumber)
        val (systemPath, systemPathMarkerChannel) = generateSystemPath(buildNumber, usesUltimate)
        System.setProperty(SYSTEM_PATH, systemPath)

        System.setProperty(
                PlatformUtils.PLATFORM_PREFIX_KEY,
                PlatformUtils.getPlatformPrefix(
                        if (usesUltimate) PlatformUtils.IDEA_PREFIX else PlatformUtils.IDEA_CE_PREFIX
                )
        )
        logger.info("IDEA home path: " + PathManager.getHomePath())
        logger.info("IDEA system path: $systemPath")
        if (getCommandLineApplication() == null) {
            createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
            for (plugin in USELESS_PLUGINS) {
                PluginManagerCore.disablePlugin(plugin)
            }

            if (usesUltimate) {
                throw InspectionRunnerException("Using of IDEA Ultimate is not yet supported in inspection runner")
            }
            // Do not remove the call of PluginManagerCore.getPlugins(), it prevents NPE in IDEA
            // NB: IdeaApplication.getStarter() from IJ community contains the same call
            logger.info("Plugins enabled:")
            PluginManagerCore.getPlugins().forEach { logger.info("    $it") }
            ApplicationManagerEx.getApplicationEx().load()
        } else {
            logger.info("IDEA application already exists, don't bother to run it again")
            shutdownNecessary = false
        }
        return (ApplicationManagerEx.getApplicationEx() ?: run {
            throw InspectionRunnerException("Cannot create IDEA application")
        }) to systemPathMarkerChannel
    }

    private fun generateSystemPath(buildNumber: String, usesUltimate: Boolean): Pair<String, FileChannel> {
        val homeDir = System.getProperty(USER_HOME).replace("\\", "/")
        val buildPrefix = (if (usesUltimate) "U_" else "") + buildNumber.replace(".", "_")
        var path: String
        var code = 0
        var channel: FileChannel
        do {
            code++
            path = "$homeDir/.IntellijIDEAInspections/${buildPrefix}_code$code/system"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            File(file, SYSTEM_MARKER_FILE).createNewFile()
            // To prevent usages by multiple processes
            val lock = try {
                channel = FileChannel.open(Paths.get(path, SYSTEM_MARKER_FILE), StandardOpenOption.WRITE)
                channel.tryLock()
            } catch (e: IOException) {
                logger.warn("IO exception while locking: ${e.message}")
                throw InspectionRunnerException("EXCEPTION caught in inspection plugin (IDEA system dir lock): $e", e)
            }
            if (lock == null) {
                if (code == 256) {
                    throw InspectionRunnerException("Cannot create IDEA system directory (all locked)")
                }
            }
        } while (lock == null)
        return path to channel
    }

    private var shutdownNecessary = true

    override fun shutdownIdea() {
        val application = this.application
        if (testMode) {
            application?.invokeLater {
                val manager = ProjectManagerEx.getInstanceEx() as? ProjectManagerImpl
                CommandProcessor.getInstance().executeCommand(null, {
                    manager?.closeAndDisposeAllProjects(/* checkCanCloseProject = */ false)
                }, "", null)
            }
        } else if (shutdownNecessary) {
            // NB: exit is actually performed on EDT thread!
            logger.info("Shutting IDEA down!!!")
            if (application is ApplicationImpl) {
                application.exit(true, true, false)
            } else {
                application?.exit(true, true)
            }
        }
    }
}