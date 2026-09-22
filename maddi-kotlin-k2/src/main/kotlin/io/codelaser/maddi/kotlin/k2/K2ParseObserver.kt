/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
