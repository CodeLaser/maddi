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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.kotlin.k2.KotlinScan
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.info.TypeParameter
import io.codelaser.maddi.cst.api.type.ParameterizedType

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
     * What a stub needs to know that the CST does not say, or does not say yet. The defaults read the CST, which is
     * enough for a stub of a fully converted type; a mixed source set's stubs are made from a Kotlin scan's
     * declarations, before any body (see `KotlinScan.declare`), and ask the scan.
     */
    interface StubHints {
        /** An implementation rather than an abstract declaration: a Kotlin interface's `default` method. */
        fun hasBody(method: MethodInfo): Boolean = runCatching { method.methodBody() }.getOrNull() != null

        /** Static on the JVM though not in the CST: an `object`'s `@JvmStatic` function. */
        fun isJvmStatic(method: MethodInfo): Boolean = false

        /** The `super(...)`/`this(...)` [constructor] calls; null for the implicit `super()`. */
        fun delegation(constructor: MethodInfo): KotlinScan.ConstructorDelegation? = null
    }

    private val DEFAULT_HINTS = object : StubHints {}

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
        m.parameters().withIndex().joinToString(", ") { (i, p) ->
            val type = javaType(p.parameterizedType())
            // a `vararg` is `T...`: as `T[]`, a Java call passing the elements does not resolve
            (if (p.isVarArgs && type.endsWith("[]")) type.dropLast(2) + "..." else type) + " p$i"
        }

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

    fun stub(typeInfo: TypeInfo, hints: StubHints = DEFAULT_HINTS): String {
        val sb = StringBuilder()
        val pkg = typeInfo.packageName()
        if (pkg.isNotEmpty()) sb.append("package ").append(pkg).append(";\n\n")
        appendType(sb, typeInfo, "", hints)
        return sb.toString()
    }

    private fun appendType(sb: StringBuilder, typeInfo: TypeInfo, indent: String, hints: StubHints) {
        if (typeInfo.typeNature().isEnum) {
            appendEnum(sb, typeInfo, indent, hints)
            return
        }
        if (typeInfo.typeNature().isAnnotation) {
            appendAnnotation(sb, typeInfo, indent)
            return
        }
        val isInterface = typeInfo.typeNature().isInterface
        sb.append(indent).append("public ")
        // a Kotlin class nested in another is static unless it is `inner`; an inner stub cannot even be constructed
        // without an enclosing instance
        // (only inside its enclosing stub: a driver may stub a nested type on its own, at top level)
        if (indent.isNotEmpty() && typeInfo.isStatic) sb.append("static ")
        // a `sealed` class is abstract on the JVM, and its subclasses need not implement what it leaves abstract
        // (javalin's `PathSegment.Normal`, between `PathSegment` and the two classes that do)
        if (!isInterface && (typeInfo.isAbstract || typeInfo.isSealed || typeInfo.methods().any { it.isAbstract })) {
            sb.append("abstract ")
        }
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
        // one member per Java signature: a private `var`'s synthesized setter and a written `fun setX(x)` are two CST
        // methods and one JVM method (javalin's JavalinServletContext.setRouteRoles); so are an overload kotlinc adds
        // and one written by hand
        val emitted = HashSet<String>()
        typeInfo.constructors().forEach { appendMethod(sb, typeInfo, it, isInterface, inner, hints, emitted) }
        typeInfo.methods().filter { isJavaName(it.name()) }
            .forEach { appendMethod(sb, typeInfo, it, isInterface, inner, hints, emitted) }
        typeInfo.subTypes().forEach { appendType(sb, it, inner, hints) } // nested types are static-nested in the stub
        sb.append(indent).append("}\n")
    }

    /**
     * An enum stub: `public enum E { A, B; ... }`. The entry constants come first (javac needs them so a Java
     * reference to `E.A` resolves); the synthetic `name()`/`values()`/`valueOf()` are dropped because javac
     * generates them for any `enum` declaration. Constructors are dropped (enum ctors are implicitly private);
     * remaining methods are emitted with a body and never `abstract` (a simple enum stub has no constant bodies).
     */
    private fun appendEnum(sb: StringBuilder, typeInfo: TypeInfo, indent: String, hints: StubHints) {
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
        typeInfo.subTypes().forEach { appendType(sb, it, inner, hints) }
        sb.append(indent).append("}\n")
    }

    /** An enum constant: a static field whose declared type is the enum itself (not a synthetic `name()` etc.). */
    private fun isEnumConstant(f: io.codelaser.maddi.cst.api.info.FieldInfo, enumType: TypeInfo): Boolean =
        f.isStatic && f.type().typeInfo() === enumType

    private fun appendMethod(sb: StringBuilder, owner: TypeInfo, m: MethodInfo, ownerIsInterface: Boolean, indent: String,
                             hints: StubHints, emitted: MutableSet<String> = HashSet()) {
        // a `$default` constructor (the trailing `int` mask and DefaultConstructorMarker) is kotlinc's, called by
        // Kotlin only; it would need a `this(...)` of its own, and nothing in Java can name its marker. A companion's
        // private one is not Java's to call either; an overload kotlinc adds (a no-argument one) is
        if (m.isConstructor && m.isSynthetic && (m.methodModifiers().any { it.isPrivate } || m.parameters().lastOrNull()?.name() == "\$marker")) return
        val isStatic = m.isStatic || hints.isJvmStatic(m)
        // a Kotlin interface method WITH an implementation is a Java `default` method (javac needs the keyword,
        // else a Java class relying on it is forced to implement it); one without a body stays abstract.
        val interfaceDefault = ownerIsInterface && !isStatic && hints.hasBody(m)
        val delegated = if (m.isConstructor) hints.delegation(m) else null
        run {
            val signature = (if (m.isConstructor) "<init>" else m.name()) +
                    m.parameters().joinToString(",", "(", ")") { rawType(it.parameterizedType()) }
            if (!emitted.add(signature)) return@run
            sb.append(indent).append("public ")
            if (isStatic) sb.append("static ")
            if (m.isAbstract && !ownerIsInterface) sb.append("abstract ")
            if (interfaceDefault) sb.append("default ")
            sb.append(typeParameters(m.typeParameters()))
            if (!m.isConstructor) sb.append(javaType(m.returnType())).append(" ")
            sb.append(if (m.isConstructor) owner.simpleName() else m.name())
            sb.append("(").append(parameterList(m)).append(")")
            val emitBody = m.isConstructor || isStatic || interfaceDefault || (!ownerIsInterface && !m.isAbstract)
            delegated?.thrown?.takeIf { it.isNotEmpty() }
                ?.let { thrown -> sb.append(" throws ").append(thrown.joinToString(", ", transform = ::rawType)) }
            val delegation = delegated?.let { explicitInvocation(it) + " " } ?: ""
            sb.append(if (emitBody) " { ${delegation}throw new RuntimeException(\"stub\"); }\n" else ";\n")
        }
    }

    /**
     * `super(...)`/`this(...)` with one typed dummy per parameter: the cast is what picks the overload, since the
     * values are never evaluated. A parameter typed by a type parameter gets a bare `null` (see
     * [KotlinScan.ConstructorDelegation]).
     */
    private fun explicitInvocation(delegation: KotlinScan.ConstructorDelegation): String =
        (if (delegation.isSuper) "super(" else "this(") + delegation.parameterTypes.joinToString(", ") { pt ->
            when {
                pt == null -> "null"
                pt.arrays() == 0 && pt.isBoolean -> "false"
                pt.arrays() == 0 && pt.isPrimitiveExcludingVoid -> "(" + rawType(pt) + ") 0"
                else -> "(" + rawType(pt) + ") null"
            }
        } + ");"

    private fun typeParameters(tps: List<TypeParameter>): String =
        if (tps.isEmpty()) "" else "<" + tps.joinToString(", ") { tp ->
            val bounds = tp.typeBounds().filterNot { isJavaLangObject(it) }
            tp.simpleName() + if (bounds.isEmpty()) "" else " extends " + bounds.joinToString(" & ", transform = ::javaType)
        } + "> "

    /** A Java type reference without generic arguments, or a type-parameter name, with array brackets. */
    private fun rawType(pt: ParameterizedType): String {
        val typeInfo = pt.typeInfo()
        val base = pt.typeParameter()?.simpleName() ?: typeInfo?.fullyQualifiedName() ?: "java.lang.Object"
        return base + "[]".repeat(pt.arrays())
    }

    /**
     * A Java type reference WITH its generic arguments.
     *
     * ⛔ Erased, as stubs were until javalin, every generic Kotlin member reached Java as `Object`: its
     * `state.servlet.getValue().getServlet()` became "cannot find symbol: getServlet() in Object", a Kotlin
     * `List<WsHandlerEntry>` gave its elements no `getType()`, and a `Validator<Instant>` returned an `Object` that
     * cannot be an `Instant` -- 60 of the 101 javac errors on the first parse. Erasure only ever made sense while a
     * stub was compiled alone; the stubs of a source set are compiled together, against the set's Java sources.
     */
    private fun javaType(pt: ParameterizedType): String {
        if (pt.isUnboundWildcard) return "?"
        val typeInfo = pt.typeInfo()
        val typeParameter = pt.typeParameter()
        val base = typeParameter?.simpleName() ?: typeInfo?.fullyQualifiedName() ?: "java.lang.Object"
        val arguments = if (typeParameter != null) emptyList() else pt.parameters()
        val rendered = (if (arguments.isEmpty()) base else base + arguments.joinToString(", ", "<", ">", transform = ::typeArgument)) +
                "[]".repeat(pt.arrays())
        val wildcard = pt.wildcard()
        return when {
            wildcard == null || wildcard.isUnbound -> rendered
            wildcard.isSuper -> "? super $rendered"
            else -> "? extends $rendered"
        }
    }

    // a type argument cannot be primitive, and Kotlin's `Unit` (CST `void`) is `kotlin.Unit` there
    private fun typeArgument(pt: ParameterizedType): String = when {
        pt.arrays() > 0 || pt.typeParameter() != null -> javaType(pt)
        pt.isVoid -> "kotlin.Unit"
        pt.isPrimitiveExcludingVoid -> BOXED[pt.typeInfo()?.fullyQualifiedName()] ?: javaType(pt)
        else -> javaType(pt)
    }

    private val BOXED = mapOf("int" to "java.lang.Integer", "long" to "java.lang.Long", "short" to "java.lang.Short",
        "byte" to "java.lang.Byte", "char" to "java.lang.Character", "boolean" to "java.lang.Boolean",
        "float" to "java.lang.Float", "double" to "java.lang.Double")

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
    private fun javaSupertype(pt: ParameterizedType): String = javaType(pt)

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
