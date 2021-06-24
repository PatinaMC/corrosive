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
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import java.io.File

val Project.toothpick: ToothpickExtension
    get() = rootProject.extensions.findByType(ToothpickExtension::class)!!

fun Project.toothpick(receiver: ToothpickExtension.() -> Unit) {
    toothpick.project = this
    receiver(toothpick)
    allprojects {
        group = toothpick.groupId
        version = toothpick.calcVersionString
    }
    configureSubprojects()
    initToothpickTasks()
}

val Project.lastUpstream: File
    get() = rootProject.projectDir.resolve("last-${toothpick.upstreamLowercase}")

val Project.rootProjectDir: File
    get() = rootProject.projectDir

val Project.upstreamDir: File
    get() = rootProject.projectDir.resolve(toothpick.upstream)

val Project.upstream: String
    get() = toothpick.upstream

val Project.upstreams: MutableList<Upstream>
    get() = toothpick.getUpstreams(rootProject.projectDir) as MutableList<Upstream>

val Project.forkName: String
    get() = toothpick.forkName

val Project.patchCreditsOutput: String
    get() = toothpick.patchCreditsOutput

val Project.patchCreditsTemplate: String
    get() = toothpick.patchCreditsTemplate
