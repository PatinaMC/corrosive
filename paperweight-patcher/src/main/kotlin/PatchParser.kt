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
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.function.Function
import javax.mail.internet.MimeUtility

/**
 * Rudimentary parser to get Author, subject and coAuthors of a patch file
 *
 * @author tr7zw
 */
object PatchParser {
    fun parsePatch(file: File): PatchInfo {
        val lines: List<String> = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)
        var from: String = "Unknown"
        var subject: String = "Unknown"
        val coAuthors: MutableList<String> = ArrayList()
        for (line: String in lines) {
            when {
                line.startsWith("From: ") -> {
                    from =
                        decodeStringIfNeeded(line.replace("From: ", "").split("<").toTypedArray()[0].trim { it <= ' ' })
                }
                line.startsWith("Subject: ") -> {
                    subject = line.replace("Subject: ", "").replace("[PATCH]", "").trim { it <= ' ' }
                }
                line.startsWith("Co-authored-by: ") -> {
                    coAuthors.add(
                        decodeStringIfNeeded(
                            line.replace("Co-authored-by: ", "").split("<").toTypedArray()[0].trim { it <= ' ' })
                    )
                }
            }
        }
        return PatchInfo(file.parentFile.name, from, subject, coAuthors)
    }

    private fun decodeStringIfNeeded(org: String): String {
        if (org.contains("=") || org.startsWith("=?UTF-8")) {
            try {
                return MimeUtility.decodeText(org)
            } catch (ex: UnsupportedEncodingException) {
                throw IOException(ex)
            }
        }
        return org
    }

    class PatchInfo(val parent: String, val from: String, val subject: String, val coAuthors: List<String>) {
        val coAuthorString: Function<String, String>
            get() = Function {
                java.lang.String.join(
                    ", ",
                    coAuthors
                )
            }

        override fun toString(): String {
            return ("PatchInfo{"
                    + "parent='"
                    + parent
                    + '\''
                    + ", from='"
                    + from
                    + '\''
                    + ", subject='"
                    + subject
                    + '\''
                    + ", coAuthors="
                    + coAuthors
                    + '}')
        }
    }
}