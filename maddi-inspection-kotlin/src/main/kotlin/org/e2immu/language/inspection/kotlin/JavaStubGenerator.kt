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

package org.e2immu.language.inspection.kotlin

import org.e2immu.language.cst.api.info.MethodInfo
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.info.TypeParameter
import org.e2immu.language.cst.api.type.ParameterizedType

/**
 * Generates a **signature-only Java stub** source for a CST [TypeInfo] — Phase 3 of the mixed-language
 * integration. javac cannot read Kotlin, so a Java source that references a Kotlin type is resolved against
 * this stub; the *real* TypeInfo still comes from the shared registry (the stub is throwaway scaffolding that
 * javac never turns into the authoritative type — the openjdk front-end reuses the registered Kotlin type).
 *
 * Signatures only: every method/constructor body throws, so nothing runs. Type references are **erased**
 * (raw types, no generic arguments) — enough for javac to resolve a reference without pulling in transitive
 * stubs. Members are emitted `public` (over-exposing does not break resolution; real access lives in the CST).
 */
object JavaStubGenerator {

    fun stub(typeInfo: TypeInfo): String {
        val sb = StringBuilder()
        val pkg = typeInfo.packageName()
        if (pkg.isNotEmpty()) sb.append("package ").append(pkg).append(";\n\n")
        appendType(sb, typeInfo, "")
        return sb.toString()
    }

    private fun appendType(sb: StringBuilder, typeInfo: TypeInfo, indent: String) {
        val isInterface = typeInfo.typeNature().isInterface
        sb.append(indent).append("public ")
        if (!isInterface && typeInfo.methods().any { it.isAbstract }) sb.append("abstract ")
        sb.append(if (isInterface) "interface " else "class ").append(typeInfo.simpleName())
        sb.append(typeParameters(typeInfo.typeParameters()))
        if (isInterface) {
            typeInfo.interfacesImplemented().takeIf { it.isNotEmpty() }
                ?.let { sb.append(" extends ").append(it.joinToString(", ", transform = ::javaType)) }
        } else {
            typeInfo.parentClass()?.takeUnless { isJavaLangObject(it) }
                ?.let { sb.append(" extends ").append(javaType(it)) }
            typeInfo.interfacesImplemented().takeIf { it.isNotEmpty() }
                ?.let { sb.append(" implements ").append(it.joinToString(", ", transform = ::javaType)) }
        }
        sb.append(" {\n")
        val inner = "$indent    "
        typeInfo.fields().forEach { f ->
            sb.append(inner).append("public ").append(if (f.isStatic) "static " else "")
                .append(javaType(f.type())).append(" ").append(f.name()).append(";\n")
        }
        typeInfo.constructors().forEach { appendMethod(sb, typeInfo, it, isInterface, inner) }
        typeInfo.methods().forEach { appendMethod(sb, typeInfo, it, isInterface, inner) }
        typeInfo.subTypes().forEach { appendType(sb, it, inner) } // nested types are static-nested in the stub
        sb.append(indent).append("}\n")
    }

    private fun appendMethod(sb: StringBuilder, owner: TypeInfo, m: MethodInfo, ownerIsInterface: Boolean, indent: String) {
        val noBody = ownerIsInterface || m.isAbstract
        sb.append(indent).append("public ")
        if (m.isStatic) sb.append("static ")
        if (m.isAbstract && !ownerIsInterface) sb.append("abstract ")
        sb.append(typeParameters(m.typeParameters()))
        if (!m.isConstructor) sb.append(javaType(m.returnType())).append(" ")
        sb.append(if (m.isConstructor) owner.simpleName() else m.name())
        sb.append("(").append(m.parameters().joinToString(", ") { javaType(it.parameterizedType()) + " " + it.name() }).append(")")
        sb.append(if (noBody) ";\n" else " { throw new RuntimeException(\"stub\"); }\n")
    }

    private fun typeParameters(tps: List<TypeParameter>): String =
        if (tps.isEmpty()) "" else "<" + tps.joinToString(", ") { tp ->
            val bounds = tp.typeBounds().filterNot { isJavaLangObject(it) }
            tp.simpleName() + if (bounds.isEmpty()) "" else " extends " + bounds.joinToString(" & ", transform = ::javaType)
        } + "> "

    /** A Java type reference (erased: no generic arguments), or a type-parameter name, with array brackets. */
    private fun javaType(pt: ParameterizedType): String {
        val base = pt.typeParameter()?.simpleName() ?: pt.typeInfo()?.fullyQualifiedName() ?: "java.lang.Object"
        return base + "[]".repeat(pt.arrays())
    }

    private fun isJavaLangObject(pt: ParameterizedType) = pt.typeInfo()?.fullyQualifiedName() == "java.lang.Object"
}
