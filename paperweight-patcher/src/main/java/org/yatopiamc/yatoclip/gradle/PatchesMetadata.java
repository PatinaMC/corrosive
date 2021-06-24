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
package org.yatopiamc.yatoclip.gradle;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PatchesMetadata {

    public final Set<PatchMetadata> patches;
    public final Set<Relocation> relocations;
    public final Set<String> copyExcludes;

    public PatchesMetadata(Set<PatchMetadata> patches, Set<Relocation> relocations, Set<String> copyExcludes) {
        Objects.requireNonNull(copyExcludes);
        this.copyExcludes = Collections.unmodifiableSet(copyExcludes);
        Objects.requireNonNull(relocations);
        this.relocations = Collections.unmodifiableSet(relocations);
        Objects.requireNonNull(patches);
        this.patches = Collections.unmodifiableSet(patches);
    }

    public static class PatchMetadata {
        public final String name;
        public final String originalHash;
        public final String targetHash;
        public final String patchHash;

        public PatchMetadata(String name, String originalHash, String targetHash, String patchHash) {
            this.name = name;
            this.originalHash = originalHash;
            this.targetHash = targetHash;
            this.patchHash = patchHash;
        }
    }

    public static class Relocation implements Serializable {

        public final String from;
        public final String to;
        public final boolean includeSubPackages;

        public Relocation(String from, String to, boolean includeSubPackages) {
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
            this.from = from.replaceAll("\\.", "/");
            this.to = to.replaceAll("\\.", "/");
            this.includeSubPackages = includeSubPackages;
        }
    }
}
