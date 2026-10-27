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
