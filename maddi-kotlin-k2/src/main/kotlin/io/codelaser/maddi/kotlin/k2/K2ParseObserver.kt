/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.KotlinParseObserver
import io.codelaser.maddi.cst.api.runtime.Runtime
import org.jetbrains.kotlin.psi.KtFile

/**
 * Something that reads a Kotlin parse through the K2 session that produced it: called once, after every file of
 * the parse has been converted to CST, while the session (and so PSI resolution) is still alive. The session does
 * not outlive the parse, so anything that needs K2's answers has to collect them here.
 *
 * ⚠ The HOST-side type is the marker [KotlinParseObserver]: this callback takes the compiler's own PSI,
 * so it cannot cross a classloader boundary. A host obtains an observer from the front end and hands it
 * back; it never calls it. The observers in this module are [ReferenceRecall]
 * (an instrument) and [KotlinReferenceIndex] (where every project declaration is referenced).
 */
abstract class K2ParseObserver : KotlinParseObserver {
    /**
     * @param ktFiles every Kotlin file of the parse
     * @param types   every CST type the parse produced (a file's types share its compilation-unit URI)
     * @param sourceSetName the source set a file belongs to
     */
    internal abstract fun observe(runtime: Runtime, ktFiles: List<KtFile>, types: List<TypeInfo>,
                                  sourceSetName: (KtFile) -> String)
}
