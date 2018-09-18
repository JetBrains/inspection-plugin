package org.jetbrains.intellij

import org.jdom2.Attribute
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.io.File

class StructureGenerator {
    fun generate(projectDir: File?): Structure {
        val workingDirectory = File(System.getProperty("user.dir"))
        val projectDirectory = projectDir ?: workingDirectory
        val project = loadProjectLikeFile(projectDirectory)
                ?: loadProjectLikeDirectory(projectDirectory)
                ?: throw IllegalArgumentException("Project not found in directory: $projectDirectory")
        return Structure(project.name, project.modules)
    }

    private operator fun List<Attribute>.get(name: String) = first { it.name == name }.value!!

    private operator fun List<Element>.get(name: String) = filter { it.name == name }

    private fun <T> safe(file: File, apply: () -> T) = try {
        apply()
    } catch (ex: NullPointerException) {
        throw IllegalArgumentException("Cannot parse file '$file'", ex)
    }

    private fun loadModule(moduleFile: File): Structure.Module {
        val sourceSets = safe(moduleFile) {
            SAXBuilder().build(moduleFile).rootElement
                    .children["component"].first { it.attributes["name"] == "NewModuleRootManager" }
                    .children["content"].first()
                    .children["sourceFolder"].asSequence()
                    .map { it.attributes["url"] }
                    .map { it.removePrefix("file://${'$'}MODULE_DIR$/") }
                    .map { File(moduleFile.parentFile, it) }
                    .toSet()
        }
        return Structure.Module(moduleFile.nameWithoutExtension, moduleFile.parentFile, sourceSets)
    }

    private fun loadProjectLikeFile(projectDirectory: File): Project? {
        val projectFile = projectDirectory.listFiles()
                ?.asSequence()
                ?.filter { it.isFile }
                ?.find { it.extension == "ipr" }
                ?: return null
        val modules = safe(projectFile) {
            SAXBuilder().build(projectFile).rootElement
                    .children["component"].first { it.attributes["name"] == "ProjectModuleManager" }
                    .children["modules"].first()
                    .children["module"].asSequence()
                    .map { it.attributes["filepath"] }
                    .map { it.removePrefix("${'$'}PROJECT_DIR$/") }
                    .map { File(projectFile.parentFile, it) }
                    .map { loadModule(it) }
                    .toList()
        }
        return Project(projectFile.nameWithoutExtension, projectDirectory, modules)
    }

    private fun loadProjectLikeDirectory(projectDirectory: File): Project? {
        val modulesFile = File(projectDirectory, ".idea/modules.xml")
        if (!modulesFile.exists()) return null
        val modules = safe(modulesFile) {
            SAXBuilder().build(modulesFile).rootElement
                    .children["component"].first { it.attributes["name"] == "ProjectModuleManager" }
                    .children["modules"].first()
                    .children["module"].asSequence()
                    .map { it.attributes["filepath"] }
                    .map { it.removePrefix("${'$'}PROJECT_DIR$/") }
                    .map { File(projectDirectory, it) }
                    .map { loadModule(it) }
                    .toList()
        }
        val projectModule = modules.find { it.directory == projectDirectory } ?: return null
        return Project(projectModule.name, projectDirectory, modules)
    }

    data class Project(val name: String, val directory: File, val modules: List<Structure.Module>)
}