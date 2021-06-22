/*
 * paperweight is a Gradle plugin for the PaperMC project.
 *
 * Copyright (c) 2021 Kyle Wood (DemonWav)
 *                    Contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 only, no later versions.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package io.papermc.paperweight.tasks

import io.papermc.paperweight.DownloadService
import io.papermc.paperweight.util.MavenArtifact
import io.papermc.paperweight.util.deleteRecursively
import io.papermc.paperweight.util.path
import io.papermc.paperweight.util.pathOrNull
import io.papermc.paperweight.util.set
import java.nio.file.Path
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.w3c.dom.Element

abstract class DownloadTask : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val downloader: Property<DownloadService>

    @TaskAction
    fun run() = downloader.get().download(url, outputFile)
}

abstract class DownloadMcLibraries : DefaultTask() {

    @get:InputFile
    abstract val mcLibrariesFile: RegularFileProperty

    @get:Input
    abstract val mcRepo: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val sourcesOutputDir: DirectoryProperty

    @get:Internal
    abstract val downloader: Property<DownloadService>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun run() {
        val out = outputDir.path
        out.deleteRecursively()
        val sourcesOut = sourcesOutputDir.path
        sourcesOut.deleteRecursively()

        val mcRepos = listOf(mcRepo.get())

        val queue = workerExecutor.noIsolation()
        mcLibrariesFile.path.useLines { lines ->
            lines.forEach { line ->
                queue.submit(DownloadWorker::class) {
                    repos.set(mcRepos)
                    artifact.set(line)
                    target.set(out)
                    sourcesTarget.set(sourcesOut)
                    downloadToDir.set(true)
                    downloader.set(this@DownloadMcLibraries.downloader)
                }
            }
        }
    }
}

abstract class DownloadSpigotDependencies : BaseTask() {

    @get:InputFile
    abstract val apiPom: RegularFileProperty

    @get:InputFile
    abstract val serverPom: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val downloader: Property<DownloadService>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun run() {
        val apiSetup = parsePom(apiPom.path)
        val serverSetup = parsePom(serverPom.path)

        val out = outputDir.path
        out.deleteRecursively()

        val spigotRepos = mutableSetOf<String>()
        spigotRepos += apiSetup.repos
        spigotRepos += serverSetup.repos

        val artifacts = mutableSetOf<MavenArtifact>()
        artifacts += apiSetup.artifacts
        artifacts += serverSetup.artifacts

        val queue = workerExecutor.noIsolation()
        for (art in artifacts) {
            queue.submit(DownloadWorker::class) {
                repos.set(spigotRepos)
                artifact.set(art.toString())
                target.set(out)
                downloadToDir.set(true)
                downloader.set(this@DownloadSpigotDependencies.downloader)
            }
        }
    }

    private fun parsePom(pomFile: Path): MavenSetup {
        val depList = arrayListOf<MavenArtifact>()
        val repoList = arrayListOf<String>()
        // Maven Central is implicit
        repoList += "https://repo.maven.apache.org/maven2/"

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = pomFile.inputStream().buffered().use { stream ->
            stream.buffered().use { buffered ->
                builder.parse(buffered)
            }
        }

        doc.documentElement.normalize()

        val list = doc.getElementsByTagName("dependencies")
        for (i in 0 until list.length) {
            val node = list.item(i) as? Element ?: continue

            val depNode = node.getElementsByTagName("dependency")
            for (j in 0 until depNode.length) {
                val dependency = depNode.item(j) as? Element ?: continue
                val artifact = getDependency(dependency) ?: continue
                depList += artifact
            }
        }

        val repos = doc.getElementsByTagName("repositories")
        for (i in 0 until repos.length) {
            val node = repos.item(i) as? Element ?: continue
            val depNode = node.getElementsByTagName("repository")
            for (j in 0 until depNode.length) {
                val repo = depNode.item(j) as? Element ?: continue
                val repoUrl = repo.getElementsByTagName("url").item(0).textContent
                repoList += repoUrl
            }
        }

        return MavenSetup(repos = repoList, artifacts = depList)
    }

    private fun getDependency(node: Element): MavenArtifact? {
        val scopeNode = node.getElementsByTagName("scope")
        val scope = if (scopeNode.length == 0) {
            "compile"
        } else {
            scopeNode.item(0).textContent
        }

        if (scope != "compile") {
            return null
        }

        val group = node.getElementsByTagName("groupId").item(0).textContent
        val artifact = node.getElementsByTagName("artifactId").item(0).textContent
        val version = node.getElementsByTagName("version").item(0).textContent

        if (version.contains("\${")) {
            // Don't handle complicated things
            // We don't need to (for now anyways)
            return null
        }

        return MavenArtifact(
            group = group,
            artifact = artifact,
            version = version
        )
    }
}

data class MavenSetup(
    val repos: List<String>,
    val artifacts: List<MavenArtifact>
)

interface DownloadParams : WorkParameters {
    val repos: ListProperty<String>
    val artifact: Property<String>
    val target: RegularFileProperty
    val sourcesTarget: RegularFileProperty
    val downloadToDir: Property<Boolean>
    val downloader: Property<DownloadService>
}
abstract class DownloadWorker : WorkAction<DownloadParams> {
    @get:Inject
    abstract val layout: ProjectLayout

    override fun execute() {
        val target = parameters.target.path
        val artifact = MavenArtifact.parse(parameters.artifact.get())

        if (parameters.downloadToDir.get()) {
            artifact.downloadToDir(parameters.downloader.get(), target, parameters.repos.get())
            parameters.sourcesTarget.pathOrNull?.let { sourceDir ->
                try {
                    val sourceArtifact = artifact.copy(classifier = "sources")
                    sourceArtifact.downloadToDir(parameters.downloader.get(), sourceDir, parameters.repos.get())
                } catch (ignored: Exception) {}
            }
        } else {
            artifact.downloadToFile(parameters.downloader.get(), target, parameters.repos.get())
        }
    }
}
