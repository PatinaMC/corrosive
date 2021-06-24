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

import cmd
import ensureSuccess
import jenkins
import org.gradle.api.Project
import org.gradle.api.Task
import rootProjectDir
import taskGroup
import toothpick

internal fun Project.createPaperclipTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("paperclip") {
    receiver(this)
    group = taskGroup
    doLast {
        val workDir = toothpick.paperDir.resolve("work")
        val paperclipDir = workDir.resolve("Paperclip")
        val vanillaJarPath =
            workDir.resolve("Minecraft/${toothpick.minecraftVersion}/${toothpick.minecraftVersion}.jar").absolutePath
        val patchedJarPath = inputs.files.singleFile.absolutePath
        logger.lifecycle(">>> Building paperclip")
        val paperclipCmd = arrayListOf(
            "mvn", "-T", "2C", "clean", "package",
            "-Dmcver=${toothpick.minecraftVersion}",
            "-Dpaperjar=$patchedJarPath",
            "-Dvanillajar=$vanillaJarPath"
        )
        if (System.getProperty("os.name").startsWith("Windows")) paperclipCmd[0] = "mvn.cmd"
        if (jenkins) paperclipCmd.add("-Dstyle.color=never")
        ensureSuccess(cmd(*paperclipCmd.toTypedArray(), dir = paperclipDir, printOut = true))
        val paperClip = paperclipDir.resolve("assembly/target/paperclip-${toothpick.minecraftVersion}.jar")
        val destination = rootProjectDir.resolve(toothpick.calcPaperclipName)
        paperClip.copyTo(destination, overwrite = true)
        logger.lifecycle(">>> ${toothpick.calcPaperclipName} saved to root project directory")
    }
}
