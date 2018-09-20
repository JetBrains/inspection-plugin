package org.jetbrains.idea.inspections.runners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.intellij.ProxyLogger
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import java.util.*

abstract class FileInfoRunner<T>(logger: ProxyLogger) : IdeaRunner<FileInfoRunnerParameters<T>>(logger) {
    data class FileInfo(val psiFile: PsiFile, val document: Document, val virtualFile: VirtualFile)

    abstract fun analyze(files: Collection<FileInfo>, project: Project, parameters: T): Boolean

    override fun analyze(project: Project, parameters: FileInfoRunnerParameters<T>): Boolean {
        logger.info("Before psi manager creation")
        val psiManager = PsiManager.getInstance(project)
        logger.info("Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val fileIndex = ProjectFileIndex.getInstance(project)
        val documentManager = FileDocumentManager.getInstance()
        val files = ArrayList<FileInfo>()
        runReadAction {
            val task = Runnable {
                for (file in parameters.files) {
                    val virtualFile = virtualFileSystem.findFileByPath(file.absolutePath)
                    if (virtualFile == null) {
                        logger.warn("Cannot find virtual file for $file")
                        continue
                    }
                    if (!fileIndex.isInSource(virtualFile)) {
                        logger.warn("File $file is not in sources")
                        continue
                    }
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile == null) {
                        logger.warn("Cannot find PSI file for $file")
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
                        logger.warn("Cannot get document for file $file $message")
                        continue
                    }
                    files.add(FileInfo(psiFile, document, virtualFile))
                }
            }
            ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
        }
        return analyze(files, project, parameters.childParameters)
    }
}