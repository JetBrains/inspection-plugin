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
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.idea.inspections.generators.HTMLGenerator
import org.jetbrains.idea.inspections.generators.XMLGenerator
import org.jetbrains.intellij.parameters.InspectionsParameters
import org.jetbrains.intellij.parameters.InspectionPluginParameters
import java.io.File
import java.util.*


@Suppress("unused")
class InspectionsRunner : FileInfoRunner<InspectionPluginParameters>() {

    class PluginInspectionWrapper<out T : InspectionProfileEntry>(
            val tool: T,
            val extension: InspectionEP?,
            val classFqName: String,
            val language: String?,
            level: String? = null
    ) {
        val level: ProblemLevel? = level?.let { ProblemLevel.fromInspectionEPLevel(it) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PluginInspectionWrapper<*>

            if (classFqName != other.classFqName) return false

            return true
        }

        override fun hashCode(): Int {
            return classFqName.hashCode()
        }
    }

    private fun getInspectionWrappers(parameters: InspectionPluginParameters, project: Project): List<PluginInspectionWrapper<*>> {
        logger.info("InspectionPlugin: InheritFromIdea = ${parameters.inheritFromIdea}")
        val registeredInspectionWrappers = getRegisteredInspectionWrappers(parameters.inspections.keys)
        if (parameters.inheritFromIdea) {
            val inspectionWrappersInheritFromIdea = getInspectionWrappersInheritFromIdea(parameters, project)
            return inspectionWrappersInheritFromIdea + registeredInspectionWrappers
        }
        return registeredInspectionWrappers
    }

    private fun getInspectionWrappersInheritFromIdea(parameters: InspectionPluginParameters, project: Project): List<PluginInspectionWrapper<*>> {
        val inspectionProfileManager = ApplicationInspectionProfileManager.getInstanceImpl()
        val profile = parameters.profileName
                ?.let { File(project.basePath, "$INSPECTION_PROFILES_PATH/$it") }
                ?.let { inspectionProfileManager.loadProfile(it.absolutePath) }
                ?: inspectionProfileManager.currentProfile
        logger.info("InspectionPlugin: Profile file = ${profile.name}")
        return profile.getAllEnabledInspectionTools(project).map {
            PluginInspectionWrapper(
                    tool = it.tool.tool,
                    extension = it.tool.extension,
                    classFqName = it.tool.tool.javaClass.name,
                    language = it.tool.language,
                    level = it.defaultState.level.name
            )
        }
    }

    private fun getRegisteredInspectionWrappers(inspections: Set<String>): List<PluginInspectionWrapper<*>> {
        val tools = InspectionToolRegistrar.getInstance().createTools()
        return inspections.map { inspectionClassName ->
            tools.find { it.tool.javaClass.name == inspectionClassName }
                    ?.let { PluginInspectionWrapper(it.tool, it.extension, inspectionClassName, it.language) }
                    ?: throw IllegalArgumentException("$inspectionClassName is not found in registrar")
        }
    }

    override fun analyzeFileInfo(
            files: Collection<FileInfo>,
            project: Project,
            parameters: InspectionPluginParameters
    ): Boolean {
        val checker = InspectionChecker()
        val results = analyze(files, project, parameters, checker)
        reportProblems(parameters, results)
        quickFixProblems(project, parameters, results)
        return checker.isSuccess
    }

    private fun analyze(
            files: Collection<FileInfo>,
            project: Project,
            parameters: InspectionPluginParameters,
            checker: InspectionChecker
    ): Map<String, List<PinnedProblemDescriptor>> {
        val results = HashMap<String, List<PinnedProblemDescriptor>>()
        logger.info("InspectionPlugin: Before inspections launched: total of ${files.size} files to analyze")
        val inspectionWrappers = getInspectionWrappers(parameters, project).toList()
        logger.info("InspectionPlugin: Inspections: " + inspectionWrappers.map { it.classFqName }.toList())
        for (inspectionWrapper in inspectionWrappers) {
            val inspectionName = inspectionWrapper.classFqName
            val inspectionTool = inspectionWrapper.tool
            when (inspectionTool) {
                is LocalInspectionTool -> {
                    @Suppress("UNCHECKED_CAST")
                    inspectionWrapper as PluginInspectionWrapper<LocalInspectionTool>
                    results[inspectionName] = inspectionWrapper.apply(files, parameters, checker)
                }
                is GlobalInspectionTool -> {
                    logger.warn("InspectionPlugin: Global inspection tools like $inspectionName are not yet supported")
                }
                else -> {
                    logger.error("InspectionPlugin: Unexpected $inspectionName which is neither local nor global")
                }
            }
            if (checker.isFail) break
        }
        return results
    }

    class InspectionChecker {
        private var errors: Int = 0
        private var warnings: Int = 0
        private var infos: Int = 0
        private var success: Boolean = true

        val isSuccess: Boolean
            get() = success

        val isFail: Boolean
            get() = !success

        fun apply(level: ProblemLevel, parameters: InspectionPluginParameters, errorListener: (String, Int) -> Unit) {
            when (level) {
                ProblemLevel.ERROR -> errors++
                ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warnings++
                ProblemLevel.INFORMATION -> infos++
            }
            when {
                parameters.errors.isTooMany(errors) -> errorListener("errors", errors)
                parameters.warnings.isTooMany(warnings) -> errorListener("warnings", warnings)
                parameters.infos.isTooMany(infos) -> errorListener("infos", infos)
                else -> return
            }
            success = false
        }

        private fun InspectionsParameters.isTooMany(value: Int) = max?.let { value > it } ?: false
    }

    private fun PluginInspectionWrapper<LocalInspectionTool>.apply(
            files: Collection<FileInfo>,
            parameters: InspectionPluginParameters,
            checker: InspectionChecker
    ): List<PinnedProblemDescriptor> {
        val displayName = extension?.displayName ?: "<Unknown diagnostic>"
        val inspectionResults = mutableListOf<PinnedProblemDescriptor>()
        runReadAction {
            val task = Runnable {
                try {
                    for ((psiFile, document) in files) {
                        if (!inspectionEnabledForFile(psiFile)) continue
                        val fileName = psiFile.name
                        val toolName = tool.displayName
                        logger.info("InspectionPlugin: Inspection '$toolName' analyzing started for $fileName")
                        val problems = tool.analyze(psiFile, document, displayName, level)
                        inspectionResults += problems
                        for (problem in problems) {
                            val level = problem.getLevel(classFqName, parameters)
                            checker.apply(level, parameters) { name, number ->
                                logger.error("InspectionPlugin: Too many $name found: $number. Analysis stopped")
                            }
                            if (checker.isFail) return@Runnable
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
        return inspectionResults
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
            generators.forEach { it.generate() }
        }
    }

    private fun quickFixProblems(
            project: Project,
            parameters: InspectionPluginParameters,
            results: Map<String, List<PinnedProblemDescriptor>>
    ) {
        if (!parameters.isAvailableCodeChanging) return
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
            project.flushChanges(files)
        }
    }

    private fun runWriteCommandAction(project: com.intellij.openapi.project.Project, action: () -> Unit) =
            WriteCommandAction.runWriteCommandAction(project, action)

    private fun FileModificationService.applyFixWithChecks(
            fix: QuickFix<CommonProblemDescriptor>,
            project: Project,
            problem: PinnedProblemDescriptor
    ): PsiFile? {
        val renderedProblem = problem.renderWithLocation()
        val file = problem.psiElement?.containingFile
        val fileName = file?.name
        runReadAction {
            val beforeText = file?.text
            applyFix(fix, project, problem)
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

    private fun Project.flushChanges(files: List<PsiFile>) {
        logger.info("InspectionPlugin: Flush IDEA project")
        val psiDocumentManager = PsiDocumentManager.getInstance(this)
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
            project: Project,
            problem: PinnedProblemDescriptor
    ) {
        val renderedProblem = problem.renderWithLocation()
        if (problem.psiElement == null) {
            logger.info("InspectionPlugin: Fix already applied for '$renderedProblem'")
            return
        }
        try {
            if (!preparePsiElementForWrite(problem.psiElement)) {
                logger.warn("InspectionPlugin: Problem psiElement cannot be prepared for '$renderedProblem'")
                return
            }
            fix.applyFix(project, problem)
            logger.info("InspectionPlugin: Applied fix for '$renderedProblem'")
            return
        } catch (exception: Exception) {
            logger.error("InspectionPlugin: Exception during applying quick fix for '$renderedProblem'")
            logger.error("InspectionPlugin: $exception")
            return
        }
    }

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

    private fun PluginInspectionWrapper<*>.inspectionEnabledForFile(psiFile: PsiFile): Boolean =
            when (psiFile.language.displayName) {
                "Kotlin" -> language in KOTLIN_FILE_APPLICABLE_LANGUAGES
                "Java" -> language in JAVA_FILE_APPLICABLE_LANGUAGES
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