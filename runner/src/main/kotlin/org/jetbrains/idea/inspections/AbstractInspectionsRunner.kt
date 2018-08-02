package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.Application
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.intellij.parameters.InspectionTypeParameters
import org.jetbrains.intellij.parameters.InspectionsParameters
import java.io.File


abstract class AbstractInspectionsRunner(
        testMode: Boolean,
        private val inspections: Set<String>
) : IdeaRunner<InspectionsParameters>(testMode) {

    companion object {
        private const val KT_LIB = "kotlin-stdlib"
        private const val INSPECTION_PROFILES_PATH = ".idea/inspectionProfiles"
        private val KOTLIN_FILE_APPLICABLE_LANGUAGES = setOf(null, "kotlin", "UAST")
        private val JAVA_FILE_APPLICABLE_LANGUAGES = setOf(null, "java", "UAST", JavaLanguage.INSTANCE.id)
    }

    class PluginInspectionWrapper(
            val tool: InspectionProfileEntry,
            val extension: InspectionEP?,
            val classFqName: String,
            val language: String?,
            level: String? = null
    ) {
        val level: ProblemLevel? = level?.let { ProblemLevel.fromInspectionEPLevel(it) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PluginInspectionWrapper

            if (classFqName != other.classFqName) return false

            return true
        }

        override fun hashCode(): Int {
            return classFqName.hashCode()
        }
    }

    private fun getInspectionWrappers(parameters: InspectionsParameters, ideaProject: IdeaProject): Sequence<PluginInspectionWrapper> {
        logger.info("InheritFromIdea = ${parameters.inheritFromIdea}")
        if (parameters.inheritFromIdea) return getInspectionWrappersInheritFromIdea(parameters, ideaProject)
        return getRegisteredInspectionWrappers()
    }

    operator fun File.div(name: String) = File(this, name)

    private fun getInspectionWrappersInheritFromIdea(parameters: InspectionsParameters, ideaProject: IdeaProject): Sequence<PluginInspectionWrapper> {
        val inspectionProfileManager = ApplicationInspectionProfileManager.getInstanceImpl()
        val inspectionProfile = parameters.profileName?.let {
            val profileDir = parameters.projectDir / INSPECTION_PROFILES_PATH / it
            logger.info("Profile: $profileDir")
            inspectionProfileManager.loadProfile(profileDir.absolutePath)
        } ?: inspectionProfileManager.currentProfile
        val profileTools = inspectionProfile.getAllEnabledInspectionTools(ideaProject)
        return profileTools.asSequence().map {
            val wrapper = it.tool
            PluginInspectionWrapper(
                    wrapper.tool,
                    wrapper.extension,
                    wrapper.tool.javaClass.name,
                    wrapper.language,
                    it.defaultState.level.name
            )
        }
    }

    private fun getRegisteredInspectionWrappers(): Sequence<PluginInspectionWrapper> {
        val tools = InspectionToolRegistrar.getInstance().createTools()
        return inspections.asSequence().mapNotNull { inspectionClassName ->
            val inspectionToolWrapper = tools.find { it.tool.javaClass.name == inspectionClassName }
            if (inspectionToolWrapper == null) {
                logger.error("InspectionsTask $inspectionClassName is not found in registrar")
                null
            } else {
                inspectionToolWrapper.let {
                    PluginInspectionWrapper(it.tool, it.extension, inspectionClassName, it.language)
                }
            }
        }
    }

    override fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            parameters: InspectionsParameters
    ): Boolean {
        val ideaProject = idea.openProject(parameters.projectDir, projectName, moduleName)
        val (success, results) = idea.analyze(parameters, files, ideaProject)
        reportProblems(parameters, results)
        return success && quickFixProblems(ideaProject, parameters, results)
    }

    private fun Application.analyze(
            parameters: InspectionsParameters,
            files: Collection<File>,
            ideaProject: IdeaProject
    ): Pair<Boolean, Map<String, List<PinnedProblemDescriptor>>> {
        var errors = 0
        var warnings = 0
        var infos = 0
        var success = true

        logger.info("Before psi manager creation")
        val psiManager = PsiManager.getInstance(ideaProject)
        logger.info("Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val documentManager = FileDocumentManager.getInstance()
        val fileIndex = ProjectFileIndex.getInstance(ideaProject)

        val results: MutableMap<String, MutableList<PinnedProblemDescriptor>> = mutableMapOf()
        logger.info("Before inspections launched: total of ${files.size} files to analyze")
        val inspectionWrappers = getInspectionWrappers(parameters, ideaProject)
        logger.info("Inspections:")
        inspectionWrappers.forEach { logger.info("    ${it.classFqName}") }
        inspectionLoop@ for (inspectionWrapper in inspectionWrappers) {
            val inspectionClassName = inspectionWrapper.classFqName
            val inspectionTool = inspectionWrapper.tool
            when (inspectionTool) {
                is LocalInspectionTool -> {
                }
                is GlobalInspectionTool -> {
                    logger.info("Global inspection tools like $inspectionClassName are not yet supported")
                    continue@inspectionLoop
                }
                else -> {
                    logger.error("Unexpected $inspectionClassName which is neither local nor global")
                    continue@inspectionLoop
                }
            }
            val inspectionExtension = inspectionWrapper.extension
            val displayName = inspectionExtension?.displayName ?: "<Unknown diagnostic>"
            val inspectionResults = mutableListOf<PinnedProblemDescriptor>()
            runReadAction {
                val task = Runnable {
                    try {
                        analysisLoop@ for (sourceFile in files) {
                            val filePath = sourceFile.absolutePath
                            val virtualFile = virtualFileSystem.findFileByPath(filePath)
                                    ?: throw InspectionRunnerException("Cannot find virtual file for $filePath")
                            if (!fileIndex.isInSource(virtualFile)) {
                                logger.warn("File $filePath is not in sources")
                                continue
                            }
                            val psiFile = psiManager.findFile(virtualFile)
                                    ?: throw InspectionRunnerException("Cannot find PSI file for $filePath")
                            if (!inspectionEnabledForFile(inspectionWrapper, psiFile)) continue
                            val document = documentManager.getDocument(virtualFile)
                                    ?: throw InspectionRunnerException("Cannot get document for $filePath")
                            val problems = inspectionTool.analyze(psiFile, document, displayName, inspectionWrapper.level)
                            inspectionResults += problems
                            for (problem in problems) {
                                val level = problem.getLevel(inspectionClassName, parameters)
                                if (level == null) {
                                    logger.warn("Problem level is undefined for $inspectionClassName")
                                    continue
                                }
                                when (level) {
                                    ProblemLevel.ERROR -> errors++
                                    ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warnings++
                                    ProblemLevel.INFORMATION -> infos++
                                }
                                val inspections = listOf(
                                        Triple("errors", parameters.errors, errors),
                                        Triple("warnings", parameters.warnings, warnings),
                                        Triple("infos", parameters.infos, infos)
                                )
                                for ((name, parameter, number) in inspections) {
                                    if (parameter.isTooMany(number)) {
                                        logger.error("Too many $name found: $number. Analysis stopped")
                                        success = false
                                        break@analysisLoop
                                    }
                                }
                            }
                        }
                    } catch (ie: InspectionException) {
                        logger.error("Exception during inspection running: " + ie.message)
                        logger.error("Caused by: " + ie.cause.message)
                        logger.error(ie.cause.stackTrace.joinToString(separator = "\n"))
                    }
                }
                ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
            }
            results[inspectionClassName] = inspectionResults
            if (!success) break@inspectionLoop
        }
        return Pair(success, results)
    }

    private fun reportProblems(parameters: InspectionsParameters, results: Map<String, List<PinnedProblemDescriptor>>) {
        val generators = listOfNotNull(
                parameters.report.xml?.let { XMLGenerator(it) },
                parameters.report.html?.let { HTMLGenerator(it, idea) }
        )
        if (generators.isEmpty() && parameters.report.isQuiet) return
        val sortedResults = results.entries
                .map { entry -> entry.value.map { entry.key to it } }
                .flatten()
                .sortedBy { (it.second.line shl 16) + it.second.row }
                .groupBy { it.second.fileName }
        val numProblems = sortedResults.values
                .flatten()
                .mapNotNull {
                    val inspectionClass = it.first
                    val problem = it.second
                    problem.getLevel(inspectionClass, parameters)
                }
                .count()
        logger.info("Total of $numProblems problem(s) found")
        analysisLoop@ for (fileInspectionAndProblems in sortedResults.values) {
            for ((inspectionClass, problem) in fileInspectionAndProblems) {
                val level = problem.getLevel(inspectionClass, parameters) ?: continue
                if (!parameters.report.isQuiet) log(level, problem)
                generators.forEach { it.report(problem, level, inspectionClass) }
            }
        }
        generators.forEach { it.generate() }
    }

    private fun quickFixProblems(
            project: IdeaProject,
            parameters: InspectionsParameters,
            results: Map<String, List<PinnedProblemDescriptor>>
    ): Boolean {
        if (!parameters.quickFix) return true
        var success = true
        WriteCommandAction.runWriteCommandAction(project) {
            @Suppress("UNUSED_VARIABLE")
            for ((inspectionClassName, result) in results) {
                for (problem in result) {
                    val fixes = problem.fixes
                    if (fixes == null || fixes.size != 1) {
                        logger.error("Can not apply problem fixes to $problem")
                        success = false
                        continue
                    }
                    try {
                        fixes[0].applyFix(project, problem)
                    } catch (ignore: Exception) {
                    }
                    logger.info("Applied fix for '${problem.renderWithLocation()}'")
                }
            }
        }
        return success
    }

    private fun InspectionTypeParameters.isTooMany(value: Int) = max?.let { value > it } ?: false

    private fun PinnedProblemDescriptor.getLevel(inspectionClass: String, parameters: InspectionsParameters) =
            actualLevel(parameters.getLevel(inspectionClass))

    private fun InspectionsParameters.getLevel(inspectionClass: String) = when (inspectionClass) {
        in errors.inspections -> ProblemLevel.ERROR
        in warnings.inspections -> ProblemLevel.WARNING
        in infos.inspections -> ProblemLevel.INFORMATION
        else -> null
    }

    private fun log(level: ProblemLevel, problem: PinnedProblemDescriptor) {
        when (level) {
            ProblemLevel.INFORMATION -> logger.info(problem.renderWithLocation())
            ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> logger.warn(problem.renderWithLocation())
            ProblemLevel.ERROR -> logger.error(problem.renderWithLocation())
        }
    }

    private fun inspectionEnabledForFile(wrapper: PluginInspectionWrapper, psiFile: PsiFile): Boolean =
            when (psiFile.language.displayName) {
                "Kotlin" -> wrapper.language in KOTLIN_FILE_APPLICABLE_LANGUAGES
                "Java" -> wrapper.language in JAVA_FILE_APPLICABLE_LANGUAGES
                else -> true
            }

    private class InspectionException(
            tool: LocalInspectionTool,
            file: PsiFile,
            override val cause: Throwable
    ) : Exception("Exception during ${tool.shortName} analysis of ${file.name}", cause)

    private fun LocalInspectionTool.analyze(
            file: PsiFile,
            document: Document,
            displayName: String?,
            problemLevel: ProblemLevel?
    ): List<PinnedProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        try {
            val visitor = buildVisitor(holder, false, session)
            file.acceptRecursively(visitor)
        } catch (t: Throwable) {
            throw InspectionException(this, file, t)
        }
        return holder.results.map {
            PinnedProblemDescriptor(it, document, displayName, problemLevel)
        }
    }

    private fun Application.openProject(projectDir: File, projectName: String, moduleName: String): IdeaProject {
        logger.info("Before project creation at '$projectDir'")
        var ideaProject: IdeaProject? = null
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
            logger.info("Project SDK name: " + rootManager.projectSdkName)
            logger.info("Project SDK: " + rootManager.projectSdk)

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
            throw InspectionRunnerException("Cannot open IDEA project: '${projectFile.absolutePath}'")
        }
    }
}