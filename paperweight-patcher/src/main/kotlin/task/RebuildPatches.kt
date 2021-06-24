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

import ensureSuccess
import forkName
import gitCmd
import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import toothpick
import upstreams
import java.io.File
import java.nio.file.Paths
import Upstream

@Suppress("UNUSED_VARIABLE")
internal fun Project.createRebuildPatchesTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("rebuildPatches") {
    receiver(this)
    group = taskGroup
    doLast {
        for ((name, subproject) in toothpick.subprojects) {
            val (sourceRepo, projectDir, patchesDir) = subproject
            var previousUpstreamName = "origin/master"
            val folder = (if (patchesDir.endsWith("server")) "server" else "api")

            for (upstream in upstreams) {
                val patchPath = Paths.get("${upstream.patchPath}/$folder").toFile()

                if (patchPath.listFiles()?.isEmpty() != false) continue

                updatePatches(patchPath, upstream.name, folder, projectDir, previousUpstreamName)
                previousUpstreamName = "${upstream.name}-$folder"
            }
            // ensureSuccess(gitCmd("checkout", "$forkName-$folder", dir = projectDir,
            ensureSuccess(gitCmd("checkout", "master", dir = projectDir,
                printOut = true))

            updatePatches(patchesDir, toothpick.forkName, folder, projectDir, previousUpstreamName)

            logger.lifecycle(">>> Done rebuilding patches for $name")
        }
    }
}

private fun Project.updatePatches(
    patchPath: File,
    name: String,
    folder: String,
    projectDir: File,
    previousUpstreamName: String
) {
    logger.lifecycle(">>> Rebuilding patches for $name-$folder")
    if (!patchPath.exists()) {
        patchPath.mkdirs()
    }
    // Nuke old patches
    patchPath.listFiles()
        ?.filter { it -> it.name.endsWith(".patch") }
        ?.forEach { it -> it.delete() }

    ensureSuccess(
        if (name != "Yatopia") {
            gitCmd(
                "checkout", "$name-$folder", dir = projectDir,
                printOut = true
            )
        } else {
            gitCmd(
                "checkout", "master", dir = projectDir,
                printOut = true
            )
        }
    )
    ensureSuccess(
        gitCmd(
            "format-patch",
            "--no-stat", "--zero-commit", "--full-index", "--no-signature", "-N",
            "-o", patchPath.absolutePath, previousUpstreamName,
            dir = projectDir,
            printOut = false
        )
    )
    gitCmd(
        "add", patchPath.canonicalPath,
        dir = patchPath,
        printOut = true
    )
}
