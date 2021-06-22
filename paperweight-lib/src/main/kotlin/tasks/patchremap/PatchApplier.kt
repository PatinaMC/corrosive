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

package io.papermc.paperweight.tasks.patchremap

import io.papermc.paperweight.PaperweightException
import io.papermc.paperweight.util.Git
import io.papermc.paperweight.util.deleteRecursively
import java.nio.file.Path
import kotlin.io.path.*

class PatchApplier(
    private val remappedBranch: String,
    private val unmappedBranch: String,
    targetDir: Path
) {

    private val git = Git(targetDir)

    private var commitMessage: String? = null
    private var commitAuthor: String? = null
    private var commitTime: String? = null

    private val remappedBaseTag: String = "remapped-base"

    fun checkoutRemapped() {
        println("Switching to $remappedBranch without losing changes")
        git("symbolic-ref", "HEAD", "refs/heads/$remappedBranch").executeSilently()
    }

    fun checkoutOld() {
        println("Resetting back to $unmappedBranch branch")
        git("checkout", unmappedBranch).executeSilently()
    }

    fun commitInitialSource() {
        git("checkout", "-b", unmappedBranch).executeSilently()
        git("add", ".").executeSilently()
        git("commit", "-m", "Initial Source", "--author=Initial <auto@mated.null>").executeSilently()
        git("branch", remappedBranch).executeSilently()
    }

    fun commitInitialRemappedSource() {
        git("add", ".").executeSilently()
        git("commit", "-m", "Initial Remapped Source", "--author=Initial <auto@mated.null>").executeSilently()
        git("tag", remappedBaseTag)
    }

    fun recordCommit() {
        commitMessage = git("log", "--format=%B", "-n", "1", "HEAD").getText()
        commitAuthor = git("log", "--format=%an <%ae>", "-n", "1", "HEAD").getText()
        commitTime = git("log", "--format=%aD", "-n", "1", "HEAD").getText()
    }

    private fun clearCommit() {
        commitMessage = null
        commitAuthor = null
        commitTime = null
    }

    fun commitChanges() {
        println("Committing remapped changes to $remappedBranch")
        val message = commitMessage ?: throw PaperweightException("commitMessage not set")
        val author = commitAuthor ?: throw PaperweightException("commitAuthor not set")
        val time = commitTime ?: throw PaperweightException("commitTime not set")
        clearCommit()

        git("add", ".").executeSilently()
        git("commit", "-m", message, "--author=$author", "--date=$time").execute()
    }

    fun applyPatch(patch: Path) {
        println("Applying patch ${patch.name}")
        val result = git("am", "--3way", "--ignore-whitespace", patch.absolutePathString()).runOut()
        if (result != 0) {
            System.err.println("Patch failed to apply: $patch")
            throw RuntimeException("Patch failed to apply: $patch")
        }
    }

    fun generatePatches(target: Path) {
        target.deleteRecursively()
        target.createDirectories()
        git("checkout", remappedBranch).executeSilently()
        git(
            "format-patch", "--zero-commit", "--full-index", "--no-signature", "--no-stat", "-N", "-o",
            target.absolutePathString(), remappedBaseTag
        ).runOut()
    }

    fun isUnfinishedPatch(): Boolean {
        if (git("branch", "--show-current").getText().trim() != unmappedBranch) {
            return false
        }

        git("update-index", "--refresh").executeSilently()
        if (git("diff-index", "--quiet", "HEAD", "--").runSilently() == 0) {
            return git("log", unmappedBranch, "-1", "--pretty=%B").getText().trim() !=
                git("log", remappedBranch, "-1", "--pretty=%B").getText().trim()
        }

        throw PaperweightException("Unknown state: repo has uncommitted changes")
    }
}
