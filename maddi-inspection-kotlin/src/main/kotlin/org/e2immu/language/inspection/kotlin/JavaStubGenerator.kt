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
 *
 * Kotlin's identifier space is wider than Java's, and the stub only exists so javac can *resolve* a reference.
 * So: parameter names are replaced by positional ones (javac matches on types, never on parameter names), and
 * a field or method whose Kotlin name is a Java keyword is **dropped** — Java source cannot name it, so it can
 * play no part in resolution, and emitting it is a syntax error. Coil's `Extras.Key(val default: T)` is the
 * case that found this.
 */
object JavaStubGenerator {

    /**
     * Java's reserved words (plus the literals `true`/`false`/`null`, which are equally unusable as
     * identifiers). Kotlin reserves a different set, so any of these can legitimately name a Kotlin member.
     */
    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "_", "true", "false", "null")

    private fun isJavaName(name: String): Boolean =
        name.isNotEmpty() && name !in JAVA_KEYWORDS
                && Character.isJavaIdentifierStart(name[0]) && name.all { Character.isJavaIdentifierPart(it) }

    /** Positional parameter names: javac resolves a call by argument types, never by parameter name. */
    private fun parameterList(m: MethodInfo): String =
        m.parameters().withIndex().joinToString(", ") { (i, p) -> javaType(p.parameterizedType()) + " p$i" }

    /**
     * An interface field is implicitly `static final`, so javac demands an initializer ("= expected").
     * Kotlin interfaces carry no state, but they do carry a `Companion` object and `const val`s, which is how
     * coil's `SizeResolver.Companion` reached this.
     */
    private fun initializer(pt: ParameterizedType): String = when {
        pt.arrays() > 0 -> " = null"
        pt.isBoolean -> " = false"
        pt.isPrimitiveExcludingVoid -> " = 0"
        else -> " = null"
    }

    fun stub(typeInfo: TypeInfo): String {
        val sb = StringBuilder()
        val pkg = typeInfo.packageName()
        if (pkg.isNotEmpty()) sb.append("package ").append(pkg).append(";\n\n")
        appendType(sb, typeInfo, "")
        return sb.toString()
    }

    private fun appendType(sb: StringBuilder, typeInfo: TypeInfo, indent: String) {
        if (typeInfo.typeNature().isEnum) {
            appendEnum(sb, typeInfo, indent)
            return
        }
        if (typeInfo.typeNature().isAnnotation) {
            appendAnnotation(sb, typeInfo, indent)
            return
        }
        val isInterface = typeInfo.typeNature().isInterface
        sb.append(indent).append("public ")
        if (!isInterface && typeInfo.methods().any { it.isAbstract }) sb.append("abstract ")
        sb.append(if (isInterface) "interface " else "class ").append(typeInfo.simpleName())
        sb.append(typeParameters(typeInfo.typeParameters()))
        if (isInterface) {
            typeInfo.interfacesImplemented().takeIf { it.isNotEmpty() }
                ?.let { sb.append(" extends ").append(it.joinToString(", ", transform = ::javaSupertype)) }
        } else {
            typeInfo.parentClass()?.takeUnless { isJavaLangObject(it) }
                ?.let { sb.append(" extends ").append(javaSupertype(it)) }
            typeInfo.interfacesImplemented().takeIf { it.isNotEmpty() }
                ?.let { sb.append(" implements ").append(it.joinToString(", ", transform = ::javaSupertype)) }
        }
        sb.append(" {\n")
        val inner = "$indent    "
        typeInfo.fields().filter { isJavaName(it.name()) }.forEach { f ->
            sb.append(inner).append("public ").append(if (f.isStatic) "static " else "")
                .append(javaFieldType(f.type())).append(" ").append(f.name())
                .append(if (isInterface) initializer(f.type()) else "").append(";\n")
        }
        typeInfo.constructors().forEach { appendMethod(sb, typeInfo, it, isInterface, inner) }
        typeInfo.methods().filter { isJavaName(it.name()) }
            .forEach { appendMethod(sb, typeInfo, it, isInterface, inner) }
        typeInfo.subTypes().forEach { appendType(sb, it, inner) } // nested types are static-nested in the stub
        sb.append(indent).append("}\n")
    }

    /**
     * An enum stub: `public enum E { A, B; ... }`. The entry constants come first (javac needs them so a Java
     * reference to `E.A` resolves); the synthetic `name()`/`values()`/`valueOf()` are dropped because javac
     * generates them for any `enum` declaration. Constructors are dropped (enum ctors are implicitly private);
     * remaining methods are emitted with a body and never `abstract` (a simple enum stub has no constant bodies).
     */
    private fun appendEnum(sb: StringBuilder, typeInfo: TypeInfo, indent: String) {
        sb.append(indent).append("public enum ").append(typeInfo.simpleName())
        typeInfo.interfacesImplemented().takeIf { it.isNotEmpty() }
            ?.let { sb.append(" implements ").append(it.joinToString(", ", transform = ::javaType)) }
        sb.append(" {\n")
        val inner = "$indent    "
        val constants = typeInfo.fields().filter { isEnumConstant(it, typeInfo) }
        sb.append(inner).append(constants.joinToString(", ") { it.name() }).append(";\n")
        typeInfo.fields().filterNot { isEnumConstant(it, typeInfo) }.filter { isJavaName(it.name()) }.forEach { f ->
            sb.append(inner).append("public ").append(if (f.isStatic) "static " else "")
                .append(javaType(f.type())).append(" ").append(f.name()).append(";\n")
        }
        typeInfo.methods().filterNot { it.isSynthetic }.filter { isJavaName(it.name()) }.forEach { m ->
            sb.append(inner).append("public ")
            if (m.isStatic) sb.append("static ")
            sb.append(typeParameters(m.typeParameters()))
            sb.append(javaType(m.returnType())).append(" ").append(m.name())
            sb.append("(").append(parameterList(m)).append(")")
            sb.append(" { throw new RuntimeException(\"stub\"); }\n")
        }
        typeInfo.subTypes().forEach { appendType(sb, it, inner) }
        sb.append(indent).append("}\n")
    }

    /** An enum constant: a static field whose declared type is the enum itself (not a synthetic `name()` etc.). */
    private fun isEnumConstant(f: org.e2immu.language.cst.api.info.FieldInfo, enumType: TypeInfo): Boolean =
        f.isStatic && f.type().typeInfo() === enumType

    private fun appendMethod(sb: StringBuilder, owner: TypeInfo, m: MethodInfo, ownerIsInterface: Boolean, indent: String) {
        // isAbstract() is unreliable for a Kotlin front-end (every method carries the plain method type, never
        // the abstract one), so key off body presence: a committed, non-empty body means an implementation.
        // An EMPTY body still counts: `fun complete() {}` on a Kotlin interface is a default method, and
        // treating it as abstract forced every Java implementor to provide it (coil's `RequestDelegate`
        // declares four such no-op defaults, and `BaseRequestDelegate` overrides only one). Presence of the
        // body object — not its contents — is what separates an implementation from an abstract declaration.
        val body = runCatching { m.methodBody() }.getOrNull()
        val hasBody = body != null
        // a Kotlin interface method WITH an implementation is a Java `default` method (javac needs the keyword,
        // else a Java class relying on it is forced to implement it); one without a body stays abstract.
        val interfaceDefault = ownerIsInterface && !m.isStatic && hasBody
        sb.append(indent).append("public ")
        if (m.isStatic) sb.append("static ")
        if (m.isAbstract && !ownerIsInterface) sb.append("abstract ")
        if (interfaceDefault) sb.append("default ")
        sb.append(typeParameters(m.typeParameters()))
        if (!m.isConstructor) sb.append(javaType(m.returnType())).append(" ")
        sb.append(if (m.isConstructor) owner.simpleName() else m.name())
        sb.append("(").append(parameterList(m)).append(")")
        val emitBody = m.isConstructor || m.isStatic || interfaceDefault || (!ownerIsInterface && !m.isAbstract)
        sb.append(if (emitBody) " { throw new RuntimeException(\"stub\"); }\n" else ";\n")
    }

    private fun typeParameters(tps: List<TypeParameter>): String =
        if (tps.isEmpty()) "" else "<" + tps.joinToString(", ") { tp ->
            val bounds = tp.typeBounds().filterNot { isJavaLangObject(it) }
            tp.simpleName() + if (bounds.isEmpty()) "" else " extends " + bounds.joinToString(" & ", transform = ::javaType)
        } + "> "

    /**
     * Kotlin's primitive array classes, which compile to the unboxed JVM arrays (unlike `Array<T>`, which
     * boxes). The CST does not yet model them that way — a `ByteArray` currently arrives as a shell `TypeInfo`
     * literally named `kotlin.ByteArray` — so the name is translated here, where it is purely a matter of
     * emitting valid Java.
     *
     * This belongs in `KotlinTypeMapper.mapClassType` alongside the other builtin mappings, and was tried
     * there: it is correct, but it perturbs the order in which library types are first reached, and that
     * order decides whether a type keeps its members (`maxMemberDepth`, first-visit-wins). It stranded
     * `java.util.Iterator` as a members-less shell — reached at depth 2 while loading `java.lang.String` —
     * and broke `TypeResolutionTest.chainedLibraryCallResolves` (`list.iterator().next()`). Raising the depth
     * to 3 traded that for four other failures. Fixing it properly means making the loader deepen a shell on a
     * later, shallower visit instead of letting the first visit decide; until then the translation stays here,
     * where it cannot affect resolution.
     */
    private val KOTLIN_PRIMITIVE_ARRAYS = mapOf(
        "kotlin.ByteArray" to "byte", "kotlin.ShortArray" to "short", "kotlin.IntArray" to "int",
        "kotlin.LongArray" to "long", "kotlin.CharArray" to "char", "kotlin.FloatArray" to "float",
        "kotlin.DoubleArray" to "double", "kotlin.BooleanArray" to "boolean")

    /** A Java type reference (erased: no generic arguments), or a type-parameter name, with array brackets. */
    private fun javaType(pt: ParameterizedType): String {
        val typeInfo = pt.typeInfo()
        KOTLIN_PRIMITIVE_ARRAYS[typeInfo?.fullyQualifiedName()]?.let { return it + "[]".repeat(pt.arrays() + 1) }
        val base = pt.typeParameter()?.simpleName() ?: typeInfo?.fullyQualifiedName() ?: "java.lang.Object"
        return base + "[]".repeat(pt.arrays())
    }

    /**
     * A field's type. Identical to [javaType] except that `void` — which Java allows only as a return type —
     * becomes `Object`.
     *
     * Kotlin's `Unit` maps to CST `void`, which is right in return position and wrong in value position: a
     * `val` of type `Unit` compiles to a field of type `kotlin.Unit`. detekt's
     * `StringLiteralDuplication.pass` (`private val pass: Unit = Unit`, used as a no-op `when` branch) emitted
     * `public void pass;` and was the single stub error left on that corpus. Substituting rather than dropping
     * the field keeps the name resolvable, and the stub's types are never authoritative — the CST is.
     *
     * The same is true of a `Unit`-typed *parameter*, which is legal Kotlin; no corpus has produced one yet,
     * and parameters are positional here, so it is left alone until something needs it.
     */
    private fun javaFieldType(pt: ParameterizedType): String =
        if (pt.isVoid) "java.lang.Object" else javaType(pt)

    /**
     * A supertype reference, which — unlike every other position — **keeps its type arguments**.
     *
     * Erasing them changes what the class inherits. `class Factory : Fetcher.Factory<Bitmap>` erased to the raw
     * `Fetcher.Factory` inherits `create(Object, …)`, which the stub's own erased `create(Bitmap, …)` does not
     * override, so javac rejects the class as not implementing its interface (12 of coil's 20 stub errors were
     * this). Elsewhere erasure is harmless and still avoids pulling in transitive stubs.
     *
     * A primitive argument would be illegal Java (`List<int>`); no Kotlin supertype should produce one, but if
     * it happens the raw type is emitted instead — a raw supertype is at worst a warning, an illegal one is an
     * error.
     */
    private fun javaSupertype(pt: ParameterizedType): String {
        val base = pt.typeParameter()?.simpleName() ?: pt.typeInfo()?.fullyQualifiedName() ?: "java.lang.Object"
        val arguments = pt.parameters()
        val rendered = if (arguments.isEmpty() || arguments.any { it.isPrimitiveExcludingVoid }) base
        else base + arguments.joinToString(", ", "<", ">", transform = ::javaType)
        return rendered + "[]".repeat(pt.arrays())
    }

    /**
     * A Kotlin `annotation class` is a Java `@interface`, not a class implementing `java.lang.annotation.
     * Annotation` — emitted as a class, javac reports "is not abstract and does not override abstract method
     * annotationType()". Its members are Kotlin `val`s, which become the annotation's methods.
     */
    private fun appendAnnotation(sb: StringBuilder, typeInfo: TypeInfo, indent: String) {
        sb.append(indent).append("public @interface ").append(typeInfo.simpleName()).append(" {\n")
        val inner = "$indent    "
        typeInfo.fields().filter { isJavaName(it.name()) }.forEach { f ->
            sb.append(inner).append("public ").append(javaType(f.type())).append(" ").append(f.name()).append("();\n")
        }
        sb.append(indent).append("}\n")
    }

    private fun isJavaLangObject(pt: ParameterizedType) = pt.typeInfo()?.fullyQualifiedName() == "java.lang.Object"
}
