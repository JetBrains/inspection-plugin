package org.jetbrains.idea.inspections

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.intellij.Runner
import java.io.File
import java.util.*

abstract class FileInfoRunner<T : Runner.Parameters> : IdeaRunner<T>() {
    data class FileInfo(val psiFile: PsiFile, val document: Document)

    abstract fun analyzeFileInfo(files: Collection<FileInfo>, project: Project, parameters: T): Boolean

    override fun analyze(files: Collection<File>, project: Project, parameters: T): Boolean {
        logger.info("InspectionPlugin: Before psi manager creation")
        val psiManager = PsiManager.getInstance(project)
        logger.info("InspectionPlugin: Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val fileIndex = ProjectFileIndex.getInstance(project)
        val documentManager = FileDocumentManager.getInstance()
        val psiFiles = ArrayList<FileInfo>()
        runReadAction {
            val task = Runnable {
                for (file in files) {
                    val virtualFile = virtualFileSystem.findFileByPath(file.absolutePath)
                    if (virtualFile == null) {
                        logger.warn("InspectionPlugin: Cannot find virtual file for $file")
                        continue
                    }
                    if (!fileIndex.isInSource(virtualFile)) {
                        logger.warn("InspectionPlugin: File $file is not in sources")
                        continue
                    }
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile == null) {
                        logger.warn("InspectionPlugin: Cannot find PSI file for $file")
                        continue
                    }
                    val document = documentManager.getDocument(virtualFile)
                    if (document == null) {
                        val message = when {
                            !virtualFile.isValid -> "is invalid"
                            virtualFile.isDirectory -> "is directory"
                            virtualFile.fileType.isBinary -> "is binary without decompiler"
                            FileUtilRt.isTooLarge(virtualFile.length) -> "is too large"
                            else -> ""
                        }
                        logger.warn("InspectionPlugin: Cannot get document for file $file $message")
                        continue
                    }
                    psiFiles.add(FileInfo(psiFile, document))
                }
            }
            ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
        }
        return analyzeFileInfo(psiFiles, project, parameters)
    }
}