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

import gitCmd
import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import upstreamDir
import upstreams

internal fun Project.createInitGitSubmodulesTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("initGitSubmodules") {
    receiver(this)
    group = taskGroup
    var upstreamNotInit = false
    for (upstream in upstreams) { upstreamNotInit = upstreamNotInit || upstream.repoPath.toFile().resolve(".git").exists() }
    onlyIf { !upstreamDir.resolve(".git").exists() || upstreamNotInit }
    doLast {
        var exit = gitCmd("submodule", "update", "--init", printOut = true).exitCode
        exit += gitCmd("submodule", "update", "--init", "--recursive", dir = upstreamDir, printOut = true).exitCode
        if (exit != 0) {
            error("Failed to checkout git submodules: git exited with code $exit")
        }
    }
}
