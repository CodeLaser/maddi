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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.kotlin.api.ConstructorDelegation
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
     * declarations, before any body (see `KotlinSession.declare`), and ask the session.
     */
    interface StubHints {
        /** An implementation rather than an abstract declaration: a Kotlin interface's `default` method. */
        fun hasBody(method: MethodInfo): Boolean = runCatching { method.methodBody() }.getOrNull() != null

        /**
         * The `super(...)`/`this(...)` [constructor] calls; null for the implicit `super()`. By default read off the
         * CST: a converted constructor's first statement is its delegation.
         *
         * ⛔ Until 2026-09-24 the default answered null ALWAYS, and only the interleaved flow (a session, before any
         * body) passed hints. So the ordinary Java-depends-on-Kotlin flow stubbed every constructor with an
         * implicit `super()`, and a Kotlin class extending one with no no-argument constructor --
         * `class Sub(e: Env) : Node(e, 4)` -- failed the stub compile, and with it the whole parse.
         */
        fun delegation(constructor: MethodInfo): ConstructorDelegation? {
            val first = runCatching { constructor.methodBody() }.getOrNull()?.statements()?.firstOrNull()
            val eci = first as? ExplicitConstructorInvocation ?: return null
            val target = eci.methodInfo() ?: return null
            if (!isStubbed(target)) return delegationForDefault(target)
            return ConstructorDelegation(eci.isSuper,
                target.parameters().map { p -> p.parameterizedType().takeIf { it.typeParameter() == null } },
                runCatching { target.exceptionTypes() }.getOrDefault(emptyList()))
        }

        /**
         * The Java return type to write for [method] instead of its own, or null to write its own. kotlinc compiles
         * an override whose type does not fit what it overrides in JAVA's rules -- `List<List<X>>` for an
         * `Iterable<Iterable<X>>` (Kotlin's are covariant, Java's invariant), `Nothing` for a `List<String>` -- with
         * the declared type AND a bridge returning the overridden one's erasure (javap on Exposed: `List arguments()`,
         * `List createStatement()`). A stub is SOURCE, which cannot hold both, so its caller supplies the bridge's
         * type where javac refused the declared one.
         */
        fun returnType(method: MethodInfo): String? = null

        /**
         * Whole Java method declarations to add to [type]'s stub: members the JVM class has and the CST does not.
         * A Kotlin class implementing a library interface whose defaults are compiled the `DefaultImpls` way (the
         * stdlib's `CoroutineContext.Element`: `minusKey`, `get`, `fold`) gets forwarders from kotlinc; Java source
         * implementing the same interface must declare them, so its caller supplies the ones javac asks for.
         */
        fun extraMethods(type: TypeInfo): List<String> = emptyList()
    }

    /**
     * kotlinc gives a constructor with default values an overload without them, whose body calls the `$default`
     * constructor (the parameters, an `int` mask per 32 of them, a `DefaultConstructorMarker`) that the stub leaves out. The stub
     * calls the constructor that `$default` one stands for instead: same owner, its parameters minus the masks and the marker.
     * The CST does not give the `$default` constructor a delegation of its own to follow. Null when there is none.
     */
    private fun delegationForDefault(synthetic: MethodInfo): ConstructorDelegation? {
        val params = synthetic.parameters()
        if (params.size < 2 || params.last().name() != "\$marker") return null
        val types = params.dropLast(1).map { it.parameterizedType() }
        // one `int` mask per 32 parameters
        val real = synthetic.typeInfo().constructors().filter { isStubbed(it) }.firstOrNull { c ->
            val n = c.parameters().size
            val masks = types.size - n
            masks >= 1 && masks == (n + 31) / 32 && c.parameters().map { it.parameterizedType() } == types.take(n)
        } ?: return null
        return ConstructorDelegation(false,
            real.parameters().map { p -> p.parameterizedType().takeIf { it.typeParameter() == null } },
            runCatching { real.exceptionTypes() }.getOrDefault(emptyList()))
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

    /**
     * The Java method a GENERIC [m] overrides, whose erased signature the stub must print: kotlinc compiles `override
     * fun remove(element: E)` in an `ArrayList<E>` as `remove(Object)`, and `override fun addAll(c: Collection<E>)` as
     * `addAll(Collection<? extends E>)`. Written as in Kotlin, the stub declares a second method of the same erasure,
     * and javac refuses it ("name clash", fernflower's VBStyleCollection). Erased, it overrides (JLS 8.4.2) whatever the
     * Kotlin-mapped types said. A Kotlin method is stubbed by the same rules as [m], and a non-generic one already
     * matches. The overridden method's own parameter types cannot be compared: maddi records `ArrayList.remove`'s as `E`,
     * Kotlin's `MutableCollection` view.
     */
    private fun javaOverridden(m: MethodInfo): MethodInfo? {
        if (m.isConstructor || m.isStatic) return null
        if (m.parameters().none { javaType(it.parameterizedType()) != erasure(it.parameterizedType()) }) return null
        fun isJava(t: TypeInfo) = t.compilationUnit()?.uri()?.toString()?.endsWith(".kt") != true
        return runCatching { m.overrides() }.getOrDefault(emptySet()).firstOrNull { o ->
            o.parameters().size == m.parameters().size && isJava(o.typeInfo())
        } ?: sameErasureInJavaSupertype(m, m.typeInfo(), ::isJava, HashSet())
    }

    /**
     * True when [m]'s parameters are printed erased ([javaOverridden]) and [m] declares type parameters of its own: then
     * the whole method is erased, no `<T>` and an erased return type. ⛔ A generic method with erased parameters
     * neither overrides the generic one it stands for nor may sit beside it: maddi-kotlin-k2's `override fun <T : Any>
     * getRegisteredExtensions(module: KaModule, extensionType: ExtensionPointDescriptor<T>): List<T>` became `public <T>
     * List<T> getRegisteredExtensions(KaModule, ExtensionPointDescriptor)`, and javac reported "does not override
     * abstract method" and "name clash ... same erasure". Fully erased it has the inherited method's erasure, so it
     * overrides it (JLS 8.4.2), with an unchecked return type (8.4.8.3).
     */
    private fun erasedWhole(m: MethodInfo): Boolean =
        !m.isConstructor && m.typeParameters().isNotEmpty() && javaOverridden(m) != null

    private fun returnType(m: MethodInfo, erased: Boolean): String =
        if (erased) erasure(m.returnType()) else javaType(m.returnType())

    /**
     * [m]'s override in a Java supertype, found by erasure. `overrides()` substitutes type variables and misses what
     * Kotlin maps as `E` and a bytecode-loaded JDK declares `Object`: `remove(element: E)` against `ArrayList`'s
     * `remove(Object)` (also `contains`, `indexOf`, `Map.get`). Equal erasures are exactly javac's name clash.
     */
    private fun sameErasureInJavaSupertype(m: MethodInfo, type: TypeInfo, isJava: (TypeInfo) -> Boolean,
                                           seen: MutableSet<TypeInfo>): MethodInfo? {
        val own = m.parameters().map { erasure(it.parameterizedType()) }
        for (st in listOfNotNull(type.parentClass()) + type.interfacesImplemented()) {
            val t = st.typeInfo() ?: continue
            if (!seen.add(t)) continue
            if (isJava(t)) t.methods().firstOrNull { o ->
                !o.isStatic && o.name() == m.name() && o.parameters().size == own.size
                    && o.parameters().map { erasure(it.parameterizedType()) } == own
            }?.let { return it }
            sameErasureInJavaSupertype(m, t, isJava, seen)?.let { return it }
        }
        return null
    }

    /**
     * The erasure of [overridden]'s parameter [index] AS [sub] INHERITS IT: a type variable of the overridden method's
     * class is first replaced by the argument [sub]'s supertype chain gives it. ⛔ The declared erasure alone made
     * quarkus's `override fun list(query: PanacheQuery<*>)` in a `MongoOperations<PanacheQuery<*>, …>` into
     * `list(Object)`, a name clash with the inherited `list(PanacheQuery<?>)`; erased as inherited it is
     * `list(PanacheQuery)`, which overrides it. ArrayList's `remove(Object)` and `addAll(Collection<? extends E>)` are
     * unchanged by this: no top-level type variable.
     */
    private fun inheritedErasure(sub: TypeInfo, overridden: MethodInfo, index: Int): String {
        val declared = overridden.parameters()[index].parameterizedType()
        val tp = declared.typeParameter() ?: return erasure(declared)
        val path = supertypePath(sub, overridden.typeInfo(), HashSet()) ?: return erasure(declared)
        // walk down from the overridden class: its type parameter -> the argument one level below, and so on
        var current: ParameterizedType = declared
        for (step in path.asReversed()) {
            val variable = current.typeParameter() ?: break
            val position = step.typeInfo()?.typeParameters()?.indexOfFirst { it == variable } ?: -1
            if (position < 0 || position >= step.parameters().size) break
            current = step.parameters()[position]
        }
        val arrays = declared.arrays()
        val erased = erasure(current)
        return if (current === declared || arrays == 0) erased else erased + "[]".repeat(arrays)
    }

    /** The supertypes from [from] up to [target], [from]'s own first; null when [target] is not a supertype. */
    private fun supertypePath(from: TypeInfo, target: TypeInfo, seen: MutableSet<TypeInfo>): List<ParameterizedType>? {
        if (!seen.add(from)) return null
        for (st in listOfNotNull(from.parentClass()) + from.interfacesImplemented()) {
            val t = st.typeInfo() ?: continue
            if (t == target) return listOf(st)
            supertypePath(t, target, seen)?.let { return listOf(st) + it }
        }
        return null
    }

    /** The JLS erasure: a type variable is its leftmost bound's erasure, `Object` when unbounded. */
    private fun erasure(pt: ParameterizedType): String {
        val tp = pt.typeParameter() ?: return rawType(pt)
        val bound = tp.typeBounds().firstOrNull { it.typeParameter() != tp }
        return (bound?.let { erasure(it) } ?: "java.lang.Object") + "[]".repeat(pt.arrays())
    }

    /**
     * Positional parameter names: javac resolves a call by argument types, never by parameter name. An override of a
     * Java method whose types differ ([javaOverridden]) takes that method's ERASED signature, all of it: a signature
     * that is the erasure of the overridden one overrides it (JLS 8.4.2), a partly erased one does not.
     */
    private fun parameterList(m: MethodInfo): String {
        val overridden = javaOverridden(m)
        return m.parameters().withIndex().joinToString(", ") { (i, p) ->
            if (overridden != null) return@joinToString inheritedErasure(m.typeInfo(), overridden, i) + " p$i"
            val type = javaType(p.parameterizedType())
            // a `vararg` is `T...`: as `T[]`, a Java call passing the elements does not resolve. Only a TRAILING one:
            // anywhere else it is a plain array on the JVM, and `T...` does not compile (Exposed's
            // `create(vararg tables: Table, inBatch: Boolean = false)`)
            val trailing = i == m.parameters().lastIndex
            (if (p.isVarArgs && trailing && type.endsWith("[]")) type.dropLast(2) + "..." else type) + " p$i"
        }
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
            // a `@JvmRecord data class` has java.lang.Record for parent, which only a Java `record` may extend ("classes
            // cannot directly extend java.lang.Record"): a plain class stands in for it. Resolved only with
            // kotlin-stdlib on the class path, which fernflower's J2K measurement lacked until maddi-mod#146
            typeInfo.parentClass()?.takeUnless { isJavaLangObject(it) || it.typeInfo()?.fullyQualifiedName() == "java.lang.Record" }
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
        hints.extraMethods(typeInfo).forEach { sb.append(inner).append(it).append("\n") }
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
            val erased = erasedWhole(m)
            if (!erased) sb.append(typeParameters(m.typeParameters()))
            sb.append(hints.returnType(m) ?: returnType(m, erased)).append(" ").append(m.name())
            sb.append("(").append(parameterList(m)).append(")")
            sb.append(" { throw new RuntimeException(\"stub\"); }\n")
        }
        typeInfo.subTypes().forEach { appendType(sb, it, inner, hints) }
        sb.append(indent).append("}\n")
    }

    /** An enum constant: a static field whose declared type is the enum itself (not a synthetic `name()` etc.). */
    private fun isEnumConstant(f: io.codelaser.maddi.cst.api.info.FieldInfo, enumType: TypeInfo): Boolean =
        f.isStatic && f.type().typeInfo() === enumType

    /** False for a synthetic constructor the stub leaves out (see [appendMethod]). */
    internal fun isStubbed(m: MethodInfo): Boolean = !(m.isConstructor && m.isSynthetic
            && (m.methodModifiers().any { it.isPrivate } || m.parameters().lastOrNull()?.name() == "\$marker"))

    private fun appendMethod(sb: StringBuilder, owner: TypeInfo, m: MethodInfo, ownerIsInterface: Boolean, indent: String,
                             hints: StubHints, emitted: MutableSet<String> = HashSet()) {
        // a `$default` constructor (the trailing `int` mask and DefaultConstructorMarker) is kotlinc's, called by
        // Kotlin only; it would need a `this(...)` of its own, and nothing in Java can name its marker. A companion's
        // private one is not Java's to call either; an overload kotlinc adds (a no-argument one) is
        // ...unless it calls a supertype constructor that takes arguments (`companion object : NameProvider("H2")`):
        // left out, javac gives the stub an implicit `super()` the parent does not have, and refuses it
        if (m.isConstructor && !isStubbed(m)
            && (m.parameters().lastOrNull()?.name() == "\$marker" || hints.delegation(m) == null)) return
        val isStatic = m.isStatic
        // a Kotlin interface method WITH an implementation is a Java `default` method (javac needs the keyword,
        // else a Java class relying on it is forced to implement it); one without a body stays abstract.
        // ⚠ an abstract method is never a default, whatever hasBody says: a `fun interface`'s single method carries an
        // empty body block, and stubbed `default` it left the interface without an abstract method -- "not a
        // functional interface" for every Java lambda (fernflower's IBytecodeProvider, maddi-mod#146)
        val interfaceDefault = ownerIsInterface && !isStatic && !m.isAbstract && hints.hasBody(m)
        val delegated = if (m.isConstructor) hints.delegation(m) else null
        run {
            val signature = (if (m.isConstructor) "<init>" else m.name()) +
                    (javaOverridden(m)?.let { o -> o.parameters().indices.joinToString(",", "(", ")") { inheritedErasure(m.typeInfo(), o, it) } }
                        ?: m.parameters().joinToString(",", "(", ")") { rawType(it.parameterizedType()) })
            if (!emitted.add(signature)) return@run
            sb.append(indent).append("public ")
            if (isStatic) sb.append("static ")
            if (m.isAbstract && !ownerIsInterface) sb.append("abstract ")
            if (interfaceDefault) sb.append("default ")
            val erased = erasedWhole(m)
            if (!erased) sb.append(typeParameters(m.typeParameters()))
            if (!m.isConstructor) sb.append(hints.returnType(m) ?: returnType(m, erased)).append(" ")
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
     * [ConstructorDelegation]).
     */
    private fun explicitInvocation(delegation: ConstructorDelegation): String =
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
        // ⛔ a callable reference's type, `KFunction3<Point, String, Int, Point>`, arrives as `kotlin.reflect.KFunction`
        // with FOUR arguments, and KFunction declares one: javac rejects `KFunction<a.Point, ...>`, and the whole
        // parse with it. Arguments that do not fit the class's own parameters are dropped, leaving the raw type.
        val declared = typeInfo?.typeParameters()?.size ?: 0
        val arguments = if (typeParameter != null || declared > 0 && declared != pt.parameters().size) emptyList()
        else pt.parameters()
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
