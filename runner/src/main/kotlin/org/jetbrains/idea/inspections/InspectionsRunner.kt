package org.jetbrains.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.intellij.parameters.InspectionsParameters
import org.jetbrains.intellij.parameters.InspectionPluginParameters
import java.io.File
import java.util.*


@Suppress("unused")
class InspectionsRunner : IdeaRunner<InspectionPluginParameters>() {

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

    private fun getInspectionWrappers(parameters: InspectionPluginParameters, ideaProject: IdeaProject): List<PluginInspectionWrapper> {
        logger.info("InspectionPlugin: InheritFromIdea = ${parameters.inheritFromIdea}")
        val registeredInspectionWrappers = getRegisteredInspectionWrappers(parameters.inspections.keys)
        if (parameters.inheritFromIdea) {
            val inspectionWrappersInheritFromIdea = getInspectionWrappersInheritFromIdea(parameters, ideaProject)
            return inspectionWrappersInheritFromIdea + registeredInspectionWrappers
        }
        return registeredInspectionWrappers
    }

    private fun getInspectionWrappersInheritFromIdea(parameters: InspectionPluginParameters, ideaProject: IdeaProject): List<PluginInspectionWrapper> {
        val inspectionProfileManager = ApplicationInspectionProfileManager.getInstanceImpl()
        val profile = parameters.profileName
                ?.let { File(File(parameters.projectDir, INSPECTION_PROFILES_PATH), it) }
                ?.let { inspectionProfileManager.loadProfile(it.absolutePath) }
                ?: inspectionProfileManager.currentProfile
        logger.info("InspectionPlugin: Profile file = ${profile.name}")
        return profile.getAllEnabledInspectionTools(ideaProject).map {
            PluginInspectionWrapper(
                    tool = it.tool.tool,
                    extension = it.tool.extension,
                    classFqName = it.tool.tool.javaClass.name,
                    language = it.tool.language,
                    level = it.defaultState.level.name
            )
        }
    }

    private fun getRegisteredInspectionWrappers(inspections: Set<String>): List<PluginInspectionWrapper> {
        val tools = InspectionToolRegistrar.getInstance().createTools()
        return inspections.map { inspectionClassName ->
            tools.find { it.tool.javaClass.name == inspectionClassName }
                    ?.let { PluginInspectionWrapper(it.tool, it.extension, inspectionClassName, it.language) }
                    ?: throw IllegalArgumentException("$inspectionClassName is not found in registrar")
        }
    }

    override fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            parameters: InspectionPluginParameters
    ): Boolean {
        val ideaProject = openProject(parameters.projectDir, projectName, moduleName)
        val (success, results) = analyze(parameters, files, ideaProject)
        reportProblems(parameters, results)
        quickFixProblems(ideaProject, parameters, results)
        return success
    }

    private fun analyze(
            parameters: InspectionPluginParameters,
            files: Collection<File>,
            ideaProject: IdeaProject
    ): Pair<Boolean, Map<String, List<PinnedProblemDescriptor>>> {
        var errors = 0
        var warnings = 0
        var infos = 0
        var success = true

        logger.info("InspectionPlugin: Before psi manager creation")
        val psiManager = PsiManager.getInstance(ideaProject)
        logger.info("InspectionPlugin: Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val documentManager = FileDocumentManager.getInstance()
        val fileIndex = ProjectFileIndex.getInstance(ideaProject)

        val results: MutableMap<String, MutableList<PinnedProblemDescriptor>> = mutableMapOf()
        logger.info("InspectionPlugin: Before inspections launched: total of ${files.size} files to analyze")
        val inspectionWrappers = getInspectionWrappers(parameters, ideaProject).toList()
        logger.info("InspectionPlugin: Inspections: " + inspectionWrappers.map { it.classFqName }.toList())
        inspectionLoop@ for (inspectionWrapper in inspectionWrappers) {
            val inspectionClassName = inspectionWrapper.classFqName
            val inspectionTool = inspectionWrapper.tool
            when (inspectionTool) {
                is LocalInspectionTool -> {
                }
                is GlobalInspectionTool -> {
                    logger.info("InspectionPlugin: Global inspection tools like $inspectionClassName are not yet supported")
                    continue@inspectionLoop
                }
                else -> {
                    logger.error("InspectionPlugin: Unexpected $inspectionClassName which is neither local nor global")
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
                            val virtualFile = virtualFileSystem.findFileByPath(sourceFile.absolutePath)
                            if (virtualFile == null) {
                                logger.warn("InspectionPlugin: Cannot find virtual file for $sourceFile")
                                continue
                            }
                            if (parameters.skipBinarySources && virtualFile.fileType.isBinary)
                                continue
                            if (!fileIndex.isInSource(virtualFile)) {
                                logger.warn("InspectionPlugin: File $sourceFile is not in sources")
                                continue
                            }
                            val psiFile = psiManager.findFile(virtualFile)
                            if (psiFile == null) {
                                logger.warn("InspectionPlugin: Cannot find PSI file for $sourceFile")
                                continue
                            }
                            if (!inspectionEnabledForFile(inspectionWrapper, psiFile)) continue
                            val document = documentManager.getDocument(virtualFile)
                            if (document == null) {
                                val message = when {
                                    !virtualFile.isValid -> "is invalid"
                                    virtualFile.isDirectory -> "is directory"
                                    virtualFile.fileType.isBinary -> "is binary without decompiler"
                                    FileUtilRt.isTooLarge(virtualFile.length) -> "is too large"
                                    else -> ""
                                }
                                logger.warn("InspectionPlugin: Cannot get document for file $sourceFile $message")
                                continue
                            }
                            val fileName = psiFile.name
                            val toolName = inspectionTool.displayName
                            logger.info("InspectionPlugin: Inspection '$toolName' analyzing started for $fileName")
                            val problems = inspectionTool.analyze(psiFile, document, displayName, inspectionWrapper.level)
                            inspectionResults += problems
                            for (problem in problems) {
                                val level = problem.getLevel(inspectionClassName, parameters)
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
                                        logger.error("InspectionPlugin: Too many $name found: $number. Analysis stopped")
                                        success = false
                                        break@analysisLoop
                                    }
                                }
                            }
                        }
                    } catch (exception: InspectionException) {
                        logger.error("InspectionPlugin: Exception during inspection running ${exception.message}")
                        logger.error(exception.stackTrace.joinToString(separator = "\n") { "    $it" })
                        logger.error("Caused by: " + (exception.cause.message ?: exception.cause))
                        logger.error(exception.cause.stackTrace.joinToString(separator = "\n") { "    $it" })
                    }
                }
                ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
            }
            results[inspectionClassName] = inspectionResults
            if (!success) break
        }
        return Pair(success, results)
    }

    private fun reportProblems(parameters: InspectionPluginParameters, results: Map<String, List<PinnedProblemDescriptor>>) {
        val generators = listOfNotNull(
                parameters.reportParameters.xml?.let { XMLGenerator(it) },
                parameters.reportParameters.html?.let { HTMLGenerator(it) }
        )
        if (generators.isEmpty()) return
        val sortedResults = results.entries
                .map { entry -> entry.value.map { entry.key to it } }
                .flatten()
                .sortedBy { (it.second.line shl 16) + it.second.row }
                .groupBy { it.second.fileName }
        val numProblems = sortedResults.values.flatten().count()
        logger.info("InspectionPlugin: Total of $numProblems problem(s) found")
        runReadAction {
            for (fileInspectionAndProblems in sortedResults.values) {
                for ((inspectionClass, problem) in fileInspectionAndProblems) {
                    val level = problem.getLevel(inspectionClass, parameters)
                    if (!parameters.reportParameters.isQuiet) log(level, problem)
                    generators.forEach { it.report(problem, level, inspectionClass) }
                }
            }
        }
        generators.forEach { it.generate() }
    }

    private fun quickFixProblems(
            project: IdeaProject,
            parameters: InspectionPluginParameters,
            results: Map<String, List<PinnedProblemDescriptor>>
    ) {
        val writeFixes = ArrayList<Pair<PinnedProblemDescriptor, QuickFix<CommonProblemDescriptor>>>()
        val otherFixes = ArrayList<Pair<PinnedProblemDescriptor, QuickFix<CommonProblemDescriptor>>>()
        @Suppress("UNUSED_VARIABLE")
        for ((inspectionClassName, result) in results) {
            val quickFix = parameters.inspections[inspectionClassName]?.quickFix
            if (quickFix != true) continue
            for (problem in result) {
                val fixes = problem.fixes ?: continue
                if (fixes.size != 1) {
                    logger.error("InspectionPlugin: Can not apply problem fixes for '${problem.renderWithLocation()}'")
                    continue
                }
                val fix = fixes[0]
                when (fix.startInWriteAction()) {
                    true -> writeFixes.add(problem to fix)
                    false -> otherFixes.add(problem to fix)
                }
            }
        }
        val fileModificationService = FileModificationService.getInstance()
        val files = ArrayList<PsiFile>()
        runWriteCommandAction(project) {
            for ((problem, fix) in writeFixes) {
                fileModificationService.applyFixWithChecks(fix, project, problem)?.let {
                    files.add(it)
                }
            }
        }
        invokeAndWait {
            for ((problem, fix) in otherFixes) {
                fileModificationService.applyFixWithChecks(fix, project, problem)?.let {
                    files.add(it)
                }
            }
        }
        invokeAndWait {
            flushIdeaProjectChanges(project, files)
        }
    }

    private fun runWriteCommandAction(project: com.intellij.openapi.project.Project, action: () -> Unit) =
            WriteCommandAction.runWriteCommandAction(project, action)

    private fun FileModificationService.applyFixWithChecks(
            fix: QuickFix<CommonProblemDescriptor>,
            project: IdeaProject,
            problem: PinnedProblemDescriptor
    ): PsiFile? {
        val renderedProblem = problem.renderWithLocation()
        val file = problem.psiElement?.containingFile
        val fileName = file?.name
        runReadAction {
            val beforeText = file?.text
            if (!applyFix(fix, project, problem))
                logger.info("InspectionPlugin: Inapplicable fix for '$renderedProblem'")
            val afterText = file?.text
            when {
                beforeText == null -> logger.info("InspectionPlugin: Inapplicable fix for '$renderedProblem'")
                afterText == null -> logger.info("InspectionPlugin: File $fileName deleted after fix for '$renderedProblem'")
                afterText == beforeText -> logger.info("InspectionPlugin: File $fileName hasn't changes after fix '$renderedProblem'")
                else -> logger.info("InspectionPlugin: File $fileName has changes after fix '$renderedProblem'")
            }
        }
        return file
    }

    private fun flushIdeaProjectChanges(project: IdeaProject, files: List<PsiFile>) {
        logger.info("InspectionPlugin: Flush IDEA project")
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val fileDocumentManager = FileDocumentManager.getInstance()
        psiDocumentManager.commitAllDocuments()
        for (file in files) {
            val document = psiDocumentManager.getDocument(file)
            if (document == null) {
                logger.warn("InspectionPlugin: Document for file '${file.name}' not found.")
                continue
            }
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
            logger.info("InspectionPlugin: File '${file.name}' is flushed")
        }
        fileDocumentManager.saveAllDocuments()
    }

    private fun FileModificationService.applyFix(
            fix: QuickFix<CommonProblemDescriptor>,
            project: IdeaProject,
            problem: PinnedProblemDescriptor
    ): Boolean {
        val renderedProblem = problem.renderWithLocation()
        if (problem.psiElement == null) {
            logger.info("InspectionPlugin: Fix already applied for '$renderedProblem'")
            return false
        }
        try {
            if (!preparePsiElementForWrite(problem.psiElement)) {
                logger.warn("InspectionPlugin: Problem psiElement cannot be prepared for '$renderedProblem'")
                return false
            }
            fix.applyFix(project, problem)
            logger.info("InspectionPlugin: Applied fix for '$renderedProblem'")
            return true
        } catch (exception: Exception) {
            logger.error("InspectionPlugin: Exception during applying quick fix for '$renderedProblem'")
            logger.error("InspectionPlugin: $exception")
            return false
        }
    }

    private fun InspectionsParameters.isTooMany(value: Int) = max?.let { value > it } ?: false

    private fun PinnedProblemDescriptor.getLevel(inspectionClass: String, parameters: InspectionPluginParameters) =
            actualLevel(parameters.getLevel(inspectionClass)) ?: ProblemLevel.INFORMATION

    private fun InspectionPluginParameters.getLevel(inspectionClass: String) = when (inspectionClass) {
        in errors.inspections -> ProblemLevel.ERROR
        in warnings.inspections -> ProblemLevel.WARNING
        in infos.inspections -> ProblemLevel.INFORMATION
        else -> null
    }

    private fun log(level: ProblemLevel, problem: PinnedProblemDescriptor) = problem.renderWithLocation().let {
        when (level) {
            ProblemLevel.INFORMATION -> logger.info(it)
            ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> logger.warn(it)
            ProblemLevel.ERROR -> logger.error(it)
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

    companion object {
        private const val INSPECTION_PROFILES_PATH = ".idea/inspectionProfiles"
        private val KOTLIN_FILE_APPLICABLE_LANGUAGES = setOf(null, "kotlin", "UAST")
        private val JAVA_FILE_APPLICABLE_LANGUAGES = setOf(null, "java", "UAST", JavaLanguage.INSTANCE.id)
    }
}