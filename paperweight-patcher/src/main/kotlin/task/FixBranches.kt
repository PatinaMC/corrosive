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

import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import upstreams
import gitCmd
import toothpick
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import ensureSuccess

internal fun Project.createFixBranchesTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("fixBranches_") {
    receiver(this)
    group = taskGroup
    val folderArray = arrayListOf("api", "server")
    doLast {
        for (folder in folderArray) {
            val subprojectWorkDir = Paths.get("${toothpick.forkName}-${if (folder == "api") {"API"} else {"Server"}}").toFile()
            // val currentBranchCommits = gitCmd("--no-pager", "log", "${toothpick.forkName}-$folder...${toothpick.upstreamBranch}", "--pretty=oneline",
            val currentBranchCommits = gitCmd("--no-pager", "log", "master...${toothpick.upstreamBranch}", "--pretty=oneline",
                dir = subprojectWorkDir).output.toString()
            val nameMap = ConcurrentHashMap<String, String>()
            for (upstream in upstreams) {
                val patchPath = Paths.get("${upstream.patchPath}/$folder").toFile()
                if (patchPath.listFiles()?.isEmpty() != false) continue
                val commitName = gitCmd("--no-pager", "log", "${upstream.name}-$folder", "-1", "--format=\"%s\"",
                    dir = subprojectWorkDir).output.toString()
                val branchName = "${upstream.name}-$folder"
                val commitNameFiltered = commitName.substring(1, commitName.length-1)
                for (line in currentBranchCommits.split("\\n".toRegex()).stream().parallel()) {
                    val commitNameIterator = line.substring(41, line.length)
                    if (commitNameIterator == commitNameFiltered) {
                        val hash = line.substring(0, 40)
                        nameMap.put(branchName, hash)
                        continue
                    }
                }
            }
            for (upstream in upstreams) {
                val patchPath = Paths.get("${upstream.patchPath}/$folder").toFile()
                if (patchPath.listFiles()?.isEmpty() != false) continue
                val branchName = "${upstream.name}-$folder"
                ensureSuccess(gitCmd("checkout", branchName, dir = subprojectWorkDir, printOut = true))
                    ensureSuccess(gitCmd("reset", "--hard", nameMap.get(branchName) as String, dir = subprojectWorkDir,
                        printOut = true))
            }
            // ensureSuccess(gitCmd("checkout", "${toothpick.forkName}-$folder", dir = subprojectWorkDir,
            ensureSuccess(gitCmd("checkout", "master", dir = subprojectWorkDir,
                printOut = true))
        }
    }
}
