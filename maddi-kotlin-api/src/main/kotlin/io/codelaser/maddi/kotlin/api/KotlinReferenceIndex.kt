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
package io.codelaser.maddi.kotlin.api

import io.codelaser.maddi.cst.api.info.Info

/**
 * Where every declaration of a Kotlin project is referenced: the oracle a rename census compares against.
 * Built by passing one to a parse as a [KotlinParseObserver], then queried afterwards.
 */
interface KotlinReferenceIndex : KotlinParseObserver {

    /** A declaration, by the position of the first character of its name. */
    data class DeclarationKey(val uri: String, val line: Int, val column: Int)

    /**
     * One spelling of a declaration's name: 1-based, end inclusive, as every maddi `Source`. [sharedWith]
     * lists the other declarations the same spelling names (an import of an overloaded name): renaming one of
     * them alone cannot rewrite this occurrence.
     */
    data class Occurrence(val uri: String, val beginLine: Int, val beginPos: Int, val endLine: Int,
                          val endPos: Int, val name: String, val isDeclaration: Boolean,
                          val sharedWith: Set<DeclarationKey> = emptySet(), val isDoc: Boolean = false)

    val referenceCount: Int
    val docReferenceCount: Int
    val unresolvedReferences: Int

    fun keyOf(info: Info): DeclarationKey?
    fun declaration(key: DeclarationKey): Occurrence?
    fun references(key: DeclarationKey): List<Occurrence>
    fun family(key: DeclarationKey): Set<DeclarationKey>
    fun overridesOutsideProject(key: DeclarationKey): Boolean
    fun occurrencesOfFamily(key: DeclarationKey): List<Occurrence>
    fun declarationCount(): Int
    fun keys(): Set<DeclarationKey>
}
