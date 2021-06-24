/*
 * MIT License

 * Copyright (c) 2020-2021 Jason Penilla & Contributors

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package task

import PatchParser
import PatchParser.PatchInfo
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*
import patchCreditsOutput
import patchCreditsTemplate

internal fun Project.createPatchCreditsTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("patchCredits") {
    receiver(this)
    group = taskGroup

    val projectDirectory: File = rootDir
    val patchDirectory: File? = Paths.get("$rootDir/patches").toFile()
    val outputFileName: String = patchCreditsOutput
    val srcFileName: String = patchCreditsTemplate

    doLast {
        val src = File(projectDirectory, srcFileName)
        if (!src.exists()) {
            logger.warn("Unable to find src at '" + src.absolutePath + "'! Skipping!")
            return@doLast
        }
        if (!patchDirectory!!.exists()) {
            logger
                .warn(
                    "Unable to find patch directory at '"
                            + patchDirectory.absolutePath
                            + "'! Skipping!"
                )
            return@doLast
        }
        logger.lifecycle("Scanning '$patchDirectory' for patches!")
        val patches: MutableList<PatchInfo> = ArrayList()
        scanFolder(patchDirectory, patches, project)
        if (patches.isEmpty()) {
            logger.warn("Unable to find any patches! Skipping!")
            return@doLast
        }

        patches.sortWith { a: PatchInfo, b: PatchInfo -> a.subject.compareTo(b.subject) }
        val output = Output()
        output.patches = patches
        try {
            val mf: MustacheFactory = DefaultMustacheFactory()
            FileReader(src).use { srcReader ->
                val mustache = mf.compile(srcReader, "template")
                val outputFile = File(projectDirectory, outputFileName)
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                OutputStreamWriter(FileOutputStream(outputFile), StandardCharsets.UTF_8).use { writer ->
                    mustache.execute(
                        writer,
                        output
                    ).flush()
                }
            }
        } catch (ex: IOException) {
            error("Error while writing the output file!")
        }
    }
}


fun scanFolder(folder: File?, patches: MutableList<PatchInfo>, project: Project) {
    val files = folder!!.listFiles { dir: File, name: String ->
        if (dir.isDirectory) {
            return@listFiles !name.equals("removed", ignoreCase = true)
        } else {
            return@listFiles true
        }
    } ?: return
    for (f: File in files) {
        if (f.isDirectory) {
            scanFolder(f, patches, project)
        } else if (f.name.endsWith(".patch")) {
            try {
                patches.add(PatchParser.parsePatch(f))
            } catch (ex: IOException) {
                project.logger.warn("Exception while parsing '" + f.absolutePath + "'!", ex)
            }
        }
    }
}

class Output {
    var patches: List<PatchInfo>? = null
}

