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
import gitCmd
import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import toothpick
import upstreamDir
import upstreams
import java.io.File
import Upstream
import java.util.concurrent.CopyOnWriteArrayList

internal fun Project.createUpstreamCommitTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("upstreamCommit_") {
    receiver(this)
    group = taskGroup
    doLast {
        gitChangelog = getUpstreamChanges(project,this, toothpick.upstream,
            upstreamDir, toothpick.upstream)

        for (upstream in upstreams) {
            gitChangelog = getUpstreamChanges(project,this, upstream.name,
                upstream.repoPath.toFile(), "upstream/${upstream.name}")
        }

        var changedUpstreamsString = ""
        for (upstreamName in changedUpstreams) {
            if (changedUpstreamsString.isNotEmpty()) {
                changedUpstreamsString += "/"
            }
            changedUpstreamsString += upstreamName
        }
        if (changedUpstreamsString.isNotEmpty()) {
            val commitMessage = """
                    |Updated Upstream and Sidestream(s) ($changedUpstreamsString)
                    |
                    |Upstream/An Sidestream has released updates that appears to apply and compile correctly
                    |This update has NOT been tested by YatopiaMC and as with ANY update, please do your own testing.
                    |
                    |
                    |$gitChangelog
                """.trimMargin()
            ensureSuccess(gitCmd("commit", "-m", commitMessage, printOut = true))
        }

    }
}

private fun getUpstreamChanges(
    project: Project,
    task: Task,
    name: String,
    dir: File,
    path: String
): String {
    var gitChangelog1 = gitChangelog
    val oldRev = ensureSuccess(project.gitCmd("ls-tree", "HEAD", path))
        ?.substringAfter("commit ")?.substringBefore("\t")
    val upstreamTmp = ensureSuccess(
        project.gitCmd(
            "log",
            "--oneline",
            "$oldRev...HEAD",
            printOut = true,
            dir = dir
        )
    ) {
        task.logger.lifecycle("No $name changes to commit.")
    }
    if (!upstreamTmp.isNullOrBlank()) {
        changedUpstreams.add(name)
        gitChangelog1 += "$name Changes:\n$upstreamTmp\n\n"
    }
    return gitChangelog1
}

val changedUpstreams = CopyOnWriteArrayList<String>()

var gitChangelog = ""
