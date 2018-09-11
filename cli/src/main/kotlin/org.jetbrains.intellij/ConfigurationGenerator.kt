package org.jetbrains.intellij

import org.jdom2.Attribute
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.io.File

class ConfigurationGenerator() {
    fun generate(runner: File, idea: File, kotlin: File, projectDir: File?, html: File?, xml: File?): Configuration {
        val workingDirectory = File(System.getProperty("user.dir"))
        if (!kotlin.exists()) {
            throw IllegalArgumentException("Kotlin plugin not found. Please check ${kotlin.absolutePath}")
        }
        val projectDirectory = projectDir ?: workingDirectory
        val projectFile = projectDirectory.listFiles()?.find { it.extension == "ipr" }
                ?: throw IllegalArgumentException("Project not found in directory: $projectDirectory")
        val project = loadProject(projectFile)
        val report = Report(false, html, xml)
        return Configuration(
                runner,
                idea,
                kotlin,
                project.name,
                project.modules,
                report
        )
    }

    private operator fun List<Attribute>.get(name: String) = first { it.name == name }.value!!

    private operator fun List<Element>.get(name: String) = filter { it.name == name }

    private fun loadModule(moduleFile: File): Module {
        val sourceSets = SAXBuilder().build(moduleFile).rootElement
                .children["component"].first { it.attributes["name"] == "NewModuleRootManager" }
                .children["content"].first()
                .children["sourceFolder"].asSequence()
                .map { it.attributes["url"] }
                .map { it.removePrefix("file://${'$'}MODULE_DIR$/") }
                .map { File(moduleFile.parentFile, it) }
                .toList()
        return Module(moduleFile.nameWithoutExtension, moduleFile.parentFile, sourceSets)
    }

    private fun loadProject(projectFile: File): Project {
        val modules = SAXBuilder().build(projectFile).rootElement
                .children["component"].first { it.attributes["name"] == "ProjectModuleManager" }
                .children["modules"].first()
                .children["module"].asSequence()
                .map { it.attributes["filepath"] }
                .map { it.removePrefix("${'$'}PROJECT_DIR$/") }
                .map { File(projectFile.parentFile, it) }
                .map { loadModule(it) }
                .toList()
        return Project(projectFile.nameWithoutExtension, projectFile.parentFile, modules)
    }

    data class Project(val name: String, val directory: File, val modules: List<Module>)
}