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
/**
 * This is the set of extra NMS files which will be imported as part of the patch process
 *
 * See `./Paper/work/Minecraft/$MCVER/spigot/net/minecraft/server` for a list of possible files
 *
 * The `.java` extension is always assumed and should be excluded
 *
 * NOTE: Do not commit changes to this set! Instead make changes, rebuild patches, and commit the modified patches.
 *       Files already modified in existing patches will be imported automatically.
 */
val nmsImports = setOf<String>(
    // ex:
    //"EntityZombieVillager"
)

data class LibraryImport(val group: String, val library: String, val prefix: String, val file: String)

/**
 * This is the set of extra files to import into the server workspace from libraries
 *
 * Changes to this set should be committed to the repo, as these won't be automatically imported.
 */
val libraryImports = setOf<LibraryImport>(
    LibraryImport("com.mojang", "brigadier", "com/mojang/brigadier", "CommandDispatcher"),
    LibraryImport("com.mojang", "brigadier", "com/mojang/brigadier/tree", "LiteralCommandNode"),
    LibraryImport("com.mojang", "brigadier", "com/mojang/brigadier/suggestion", "SuggestionsBuilder"),
    LibraryImport("com.mojang", "brigadier", "com/mojang/brigadier/arguments", "BoolArgumentType"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "FieldFinder"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "DataFixUtils"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "TypeRewriteRule"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "Typed"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "TypedOptic"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers", "View"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/functions", "Apply"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/functions", "Comp"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/functions", "PointFree"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/functions", "PointFreeRule"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "IdAdapter"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "Inj1"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "Inj2"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "Optics"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "Proj1"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/optics", "Proj2"),
    LibraryImport("com.mojang", "datafixerupper", "com/mojang/datafixers/types", "Type")
)