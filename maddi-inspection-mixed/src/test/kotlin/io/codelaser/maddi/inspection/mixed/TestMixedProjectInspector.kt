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

package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Phase 5, multi-source-set: `MixedProjectInspector` places each type in its OWN CST source set (Kotlin `Foo`
 * in `kotlin/main`, Java `UseFoo` in `java/main`) — not flattened — while the cross-language reference still
 * resolves to one shared `TypeInfo`.
 */
class TestMixedProjectInspector {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root: each call still gets its own unique
     * directory, and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @field:TempDir
    lateinit var tempRoot: Path

    @Test
    fun typesLandInTheirOwnSourceSetsAndStillCrossResolve() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-proj")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        val jDir = tmp.resolve("proj/src/main/java")
        Files.createDirectories(kDir.resolve("a"))
        Files.createDirectories(jDir.resolve("b"))
        Files.writeString(kDir.resolve("a/Foo.kt"), "package a\nclass Foo(val id: Int)\n")
        Files.writeString(jDir.resolve("b/UseFoo.java"),
            "package b;\npublic class UseFoo {\n    public a.Foo foo;\n}\n")

        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri())
            .setDependencies(listOf(kotlinSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(kotlinSet)
            .addSourceSets(javaSet)
            .build()

        val result = MixedProjectInspector().parse(config)

        val foo = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Foo" }
        val useFoo = result.javaTypes.first { it.simpleName() == "UseFoo" }

        // each type is in its OWN source set (not one flattened bag)
        assertEquals("kotlin/main", foo.compilationUnit().sourceSet().name())
        assertEquals("java/main", useFoo.compilationUnit().sourceSet().name())
        // and the Java->Kotlin reference resolves to the one shared instance
        assertSame(foo, useFoo.getFieldByName("foo", true).type().typeInfo())
    }

    /**
     * A Kotlin class extending a Kotlin class with NO no-argument constructor, by a secondary `super(...)` and by a
     * supertype entry. The Java stub of each needs an explicit `super(...)`: until 2026-09-24 this flow stubbed every
     * constructor with the implicit one, and the stub compile -- and so the whole parse -- failed.
     */
    @Test
    fun aKotlinSubclassOfAClassWithoutANoArgumentConstructorIsStubbed() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-super")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        val jDir = tmp.resolve("proj/src/main/java")
        Files.createDirectories(kDir.resolve("a"))
        Files.createDirectories(jDir.resolve("b"))
        Files.writeString(kDir.resolve("a/Node.kt"), """
            package a

            class Env(val n: Int)

            open class Node(val size: Int) {
                constructor(env: Env, extra: Int) : this(env.n + extra)
            }

            class Sub : Node {
                constructor(e: Env) : super(e, 3)
            }

            class Sub2(e: Env) : Node(e, 4)
            """.trimIndent() + "\n")
        Files.writeString(jDir.resolve("b/UseSub.java"),
            "package b;\npublic class UseSub {\n    public a.Sub sub;\n    public a.Sub2 sub2;\n}\n")
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri())
            .setDependencies(listOf(kotlinSet)).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(kotlinSet).addSourceSets(javaSet).build()

        val result = MixedProjectInspector().parse(config)

        val sub = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Sub" }
        val useSub = result.javaTypes.first { it.simpleName() == "UseSub" }
        assertSame(sub, useSub.getFieldByName("sub", true).type().typeInfo())
    }

    /**
     * A Kotlin class extending a generic Java (here JDK, bytecode) class whose constructor takes the class's type
     * parameter: `ForwardingJavaFileManager<M>(M fileManager)`. Kotlin sees that parameter as `M!`; until 2026-10-08
     * only a bare type parameter was recognised, the stub called `super((java.lang.Object) null)`, and javac refused
     * it ("Object cannot be converted to StandardJavaFileManager"), failing the whole-tree audit on maddi's own
     * `MixedProjectInspector.UnnamedModule`.
     */
    @Test
    fun aKotlinSubclassOfAGenericJavaClassPassesATypeParameterArgument() {
        // one set holding both languages: the interleaved flow, whose stubs come from the Kotlin session's hints
        val dir = Files.createTempDirectory(tempRoot, "mixed-generic-super").resolve("src/main/java")
        Files.createDirectories(dir.resolve("p"))
        Files.writeString(dir.resolve("p/Forwarding.kt"), """
            package p

            import javax.tools.ForwardingJavaFileManager
            import javax.tools.StandardJavaFileManager

            class Forwarding(fm: StandardJavaFileManager) : ForwardingJavaFileManager<StandardJavaFileManager>(fm)
            """.trimIndent() + "\n")
        Files.writeString(dir.resolve("p/UseForwarding.java"),
            "package p;\npublic class UseForwarding {\n    public Forwarding forwarding;\n}\n")
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri()).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(main).build()

        val result = MixedProjectInspector().parse(config)

        val forwarding = result.kotlinTypes.first { it.simpleName() == "Forwarding" }
        val use = result.javaTypes.first { it.simpleName() == "UseForwarding" }
        assertSame(forwarding, use.getFieldByName("forwarding", true).type().typeInfo())
    }

    /**
     * kotlinc gives a primary constructor with default values for ALL its parameters a no-argument overload, whose
     * body calls the `$default` constructor the stub leaves out. The stub must delegate where THAT one delegates:
     * `this(...)` to the primary constructor, or, one level up, the primary's `super(...)`. A delegation copied as is
     * names a constructor javac cannot see.
     */
    @Test
    fun aDefaultValuedPrimaryConstructorIsStubbedWithoutItsSyntheticTarget() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-default")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        val jDir = tmp.resolve("proj/src/main/java")
        Files.createDirectories(kDir.resolve("a"))
        Files.createDirectories(jDir.resolve("b"))
        Files.writeString(kDir.resolve("a/Other.kt"), """
            package a

            open class Base(val x: Int)

            class Plain(val w: Int = 1)

            class Derived(val w: Int = 1) : Base(w)
            """.trimIndent() + "\n")
        Files.writeString(jDir.resolve("b/UseOther.java"),
            "package b;\npublic class UseOther {\n    public a.Plain plain = new a.Plain();\n    public a.Derived derived;\n}\n")
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri())
            .setDependencies(listOf(kotlinSet)).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(kotlinSet).addSourceSets(javaSet).build()

        val result = MixedProjectInspector().parse(config)

        val derived = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Derived" }
        val use = result.javaTypes.first { it.simpleName() == "UseOther" }
        assertSame(derived, use.getFieldByName("derived", true).type().typeInfo())
    }

    /**
     * A callable reference's type is a `KFunctionN`, which the CST carries as `kotlin.reflect.KFunction` with every
     * argument of the N-ary one; `KFunction` declares a single type parameter, so the stub must not print them all.
     */
    @Test
    fun aCallableReferenceTypeIsStubbed() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-kfunction")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        val jDir = tmp.resolve("proj/src/main/java")
        Files.createDirectories(kDir.resolve("a"))
        Files.createDirectories(jDir.resolve("b"))
        Files.writeString(kDir.resolve("a/Point.kt"), """
            package a

            class Point(val lat: Double) {
                fun twice(k: Int): Double = lat * k
            }

            val ref = Point::twice
            fun refOf() = Point::twice
            """.trimIndent() + "\n")
        Files.writeString(jDir.resolve("b/UsePoint.java"),
            "package b;\npublic class UsePoint {\n    public a.Point point;\n}\n")
        // kotlin-stdlib, for `kotlin.reflect.KFunction` to exist on javac's class path
        val stdlib = java.nio.file.Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        val stdlibSet = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).setDependencies(listOf(stdlibSet)).build()
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri())
            .setDependencies(listOf(kotlinSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet).addSourceSets(kotlinSet).addSourceSets(javaSet).build()

        val result = MixedProjectInspector().parse(config)

        val point = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Point" }
        val use = result.javaTypes.first { it.simpleName() == "UsePoint" }
        assertSame(point, use.getFieldByName("point", true).type().typeInfo())
    }

    /** The other direction: a Kotlin source set depends on a Java source set (Java-first order). */
    @Test
    fun kotlinSourceSetResolvesUpstreamJavaSourceType() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-k2j")
        val jDir = tmp.resolve("proj/src/main/java")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        Files.createDirectories(jDir.resolve("a"))
        Files.createDirectories(kDir.resolve("b"))
        Files.writeString(jDir.resolve("a/JavaFoo.java"),
            "package a;\npublic class JavaFoo {\n    public int id;\n}\n")
        Files.writeString(kDir.resolve("b/Bar.kt"), "package b\nimport a.JavaFoo\nclass Bar(val foo: JavaFoo)\n")

        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri())
            .setDependencies(listOf(javaSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(javaSet)
            .addSourceSets(kotlinSet)
            .build()

        val result = MixedProjectInspector().parse(config)

        val javaFoo = result.javaTypes.first { it.simpleName() == "JavaFoo" }
        val bar = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "Bar" }
        assertEquals("java/main", javaFoo.compilationUnit().sourceSet().name())
        assertEquals("kotlin/main", bar.compilationUnit().sourceSet().name())
        // Kotlin -> Java source across the boundary: one shared instance
        assertSame(javaFoo, bar.getFieldByName("foo", true).type().typeInfo())
    }

    /** Two Java sets (B depends on A) plus a Kotlin set: exercises Java->Java AND Java->Kotlin in one project. */
    @Test
    fun multiJavaModuleAndKotlinResolveTogether() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-multi")
        val aDir = tmp.resolve("a/src/main/java")
        val bDir = tmp.resolve("b/src/main/java")
        val kDir = tmp.resolve("k/src/main/kotlin")
        Files.createDirectories(aDir.resolve("a"))
        Files.createDirectories(bDir.resolve("b"))
        Files.createDirectories(kDir.resolve("k"))
        Files.writeString(aDir.resolve("a/A.java"), "package a;\npublic class A { public int x; }\n")
        Files.writeString(kDir.resolve("k/K.kt"), "package k\nclass K(val id: Int)\n")
        Files.writeString(bDir.resolve("b/UseAll.java"),
            "package b;\npublic class UseAll {\n    public a.A a;\n    public k.K k;\n}\n")

        val javaA = SourceSetImpl.Builder().setName("javaA/main")
            .setSourceDirectories(listOf(aDir)).setUri(aDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri()).build()
        val javaB = SourceSetImpl.Builder().setName("javaB/main")
            .setSourceDirectories(listOf(bDir)).setUri(bDir.toUri())
            .setDependencies(listOf(javaA, kotlinSet)).build()
        val config = InputConfigurationImpl.Builder()
            .addSourceSets(javaA).addSourceSets(kotlinSet).addSourceSets(javaB).build()

        val result = MixedProjectInspector().parse(config)

        val a = result.javaTypes.first { it.simpleName() == "A" }
        val k = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "K" }
        val useAll = result.javaTypes.first { it.simpleName() == "UseAll" }
        assertEquals("javaA/main", a.compilationUnit().sourceSet().name())
        assertEquals("javaB/main", useAll.compilationUnit().sourceSet().name())
        // Java -> Java across source sets, and Java -> Kotlin, both to one shared instance
        assertSame(a, useAll.getFieldByName("a", true).type().typeInfo())
        assertSame(k, useAll.getFieldByName("k", true).type().typeInfo())
    }

    /**
     * Both directions at once, in the shape of the jfocus workspace (173 Kotlin->Java and 34 Java->Kotlin set
     * dependencies): Kotlin set K uses Java set A, a named module, and Java set B uses K and A. Each set's `uri` is its
     * compiled output, as a Gradle configuration has it. Four defects, each of which stopped that workspace's parse:
     * - Kotlin first, K2 did not see A at all (`K.api` typed Object): the sets are now interleaved;
     * - with modules on, B's javac read A's types from A's class files, which disagreed with the source commit
     *   ("already committed ... FROM A COMPILED ARTIFACT"): the host's options carry ignoreModule, as the CLI's do;
     * - A's module-info on the stub compiler's source path crashed javac ("An exception has occurred in the
     *   compiler"); a Java-only set's types now reach the stubs through its build output;
     * - `Front.load()`, kotlinc's @JvmOverloads overload of a @JvmStatic companion function, had no static forwarder.
     */
    @Test
    fun javaToKotlinToJava() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-jkj")
        val aDir = tmp.resolve("a/src/main/java")
        val aClasses = tmp.resolve("a/build/classes")
        val kDir = tmp.resolve("k/src/main/kotlin")
        val bDir = tmp.resolve("b/src/main/java")
        Files.createDirectories(aDir.resolve("a"))
        Files.createDirectories(aClasses)
        Files.createDirectories(kDir.resolve("k"))
        Files.createDirectories(bDir.resolve("b"))
        Files.writeString(aDir.resolve("a/Element.java"), "package a;\npublic interface Element { }\n")
        // a named module, as maddi's own sets are: on the stub compiler's source path it put javac in module mode
        Files.writeString(aDir.resolve("module-info.java"), "module a { exports a; }\n")
        Files.writeString(aDir.resolve("a/Api.java"), """
            package a;
            public interface Api {
                String name();
                default <T extends CharSequence> T handle(T original, T translated) { return translated; }
                default <T extends CharSequence> java.util.List<T> handle(T original, java.util.List<T> translated) { return translated; }
                default <T extends Element> T post(T original, T translated) { return translated; }
            }
            """.trimIndent() + "\n")
        val javac = javax.tools.ToolProvider.getSystemJavaCompiler()
        assertEquals(0, javac.run(null, null, null, "-d", aClasses.toString(), aDir.resolve("a/Api.java").toString(),
            aDir.resolve("a/Element.java").toString(), aDir.resolve("module-info.java").toString()))
        Files.writeString(kDir.resolve("k/K.kt"), """
            package k
            import a.Api
            class K(val api: Api) {
                fun twice(s: String): String = api.handle(s, s + s)
            }
            """.trimIndent() + "\n")
        // the shape of maddi's KotlinFrontEnd.kt: an interface whose companion has a @JvmStatic @JvmOverloads function
        Files.writeString(kDir.resolve("k/Front.kt"), """
            package k
            interface Front {
                fun name(): String
                companion object {
                    @JvmStatic
                    @JvmOverloads
                    fun load(n: Int = 1): Front = object : Front { override fun name() = "f" + n }
                }
            }
            object Fronts {
                @JvmStatic
                fun installed(): Boolean = false
            }
            """.trimIndent() + "\n")
        Files.writeString(bDir.resolve("b/UseBoth.java"), """
            package b;
            public class UseBoth {
                public k.K kay; // not `k`: a field obscures the package of that name
                public String go(a.Api api, a.Element e) { return api.handle("x", "y") + kay.twice("z") + api.post(e, e); }
                // a @JvmStatic of an interface's companion: javac found no load() in the stub, and the parse failed
                public k.Front front() { return k.Fronts.installed() ? k.Front.load(2) : k.Front.load(); }
            }
            """.trimIndent() + "\n")
        // kotlin-stdlib: without it K2 resolves neither @JvmStatic nor @JvmOverloads
        val stdlib = java.nio.file.Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        val stdlibSet = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val javaA = SourceSetImpl.Builder().setName("a/main")
            .setSourceDirectories(listOf(aDir)).setUri(aClasses.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("k/main")
            .setSourceDirectories(listOf(kDir)).setUri(tmp.resolve("k/build/classes").toUri())
            .setDependencies(listOf(javaA, stdlibSet)).build()
        val javaB = SourceSetImpl.Builder().setName("b/main")
            .setSourceDirectories(listOf(bDir)).setUri(tmp.resolve("b/build/classes").toUri())
            .setDependencies(listOf(kotlinSet, javaA, stdlibSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet)
            .addSourceSets(javaA).addSourceSets(kotlinSet).addSourceSets(javaB).build()

        val ignoreModule = io.codelaser.maddi.inspection.api.integration.JavaInspector.ParseOptions.Builder()
            .setIgnoreModule(true).build()
        val result = MixedProjectInspector(MixedProjectInspector.Settings(ignoreModule)).parse(config)

        val api = result.javaTypes.first { it.simpleName() == "Api" }
        assertEquals(2, api.methods().count { it.name() == "handle" })
        val post = api.methods().single { it.name() == "post" }
        // the bound is a type of the same set: lost, the source commit had post(Object,Object), the class file
        // post(Element,Element), and the downstream set's read of the class file was refused
        val t = post.parameters()[0].parameterizedType().typeParameter()
        assertEquals(listOf("a.Element"), t.typeBounds().map { it.typeInfo()?.fullyQualifiedName() })
        val k = result.kotlinBySourceSet.getValue(kotlinSet).first { it.simpleName() == "K" }
        val useBoth = result.javaTypes.first { it.simpleName() == "UseBoth" }
        assertSame(api, k.getFieldByName("api", true).type().typeInfo())
        assertSame(k, useBoth.getFieldByName("kay", true).type().typeInfo())
        // the static forwarders of Front's companion, all three of them: load(int), and kotlinc's load()
        val front = result.kotlinTypes.first { it.simpleName() == "Front" }
        assertEquals(listOf(0, 1), front.methods().filter { it.name() == "load" && it.isStatic }.map { it.parameters().size }.sorted())
    }

    /**
     * A Kotlin reference to a Java-source declaration is recorded against the Java front end's Info, as one to a Kotlin
     * declaration is: the override's `super.run(n)`, a call, a call inside a lambda passed to a library function (which
     * the Kotlin CST keeps as a placeholder), the Java type in an import, and a field read. A rename of the Java
     * method reads these to find its Kotlin spellings.
     */
    @Test
    fun kotlinReferencesToJavaSourceDeclarationsAreRecorded() {
        val tmp = Files.createTempDirectory(tempRoot, "mixed-refs")
        val jDir = tmp.resolve("proj/src/main/java")
        val kDir = tmp.resolve("proj/src/main/kotlin")
        Files.createDirectories(jDir.resolve("q"))
        Files.createDirectories(kDir.resolve("p"))
        Files.writeString(jDir.resolve("q/J.java"), """
            package q;
            public class J {
                public int size;
                public String run(int n) { return "x".repeat(n); }
                public String run(String s) { return s; }
            }
            """.trimIndent() + "\n")
        Files.writeString(kDir.resolve("p/K.kt"), """
            package p
            import q.J
            class K : J() {
                override fun run(n: Int): String = super.run(n) + "k"
            }
            fun call(j: J) = j.run(2) + listOf(1).map { j.run(it) }.joinToString() + j.run("s") + j.size
            """.trimIndent() + "\n")
        val javaSet = SourceSetImpl.Builder().setName("java/main")
            .setSourceDirectories(listOf(jDir)).setUri(jDir.toUri()).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main")
            .setSourceDirectories(listOf(kDir)).setUri(kDir.toUri())
            .setDependencies(listOf(javaSet)).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(javaSet).addSourceSets(kotlinSet).build()

        val result = MixedProjectInspector().parse(config)

        val j = result.javaTypes.first { it.simpleName() == "J" }
        val runInt = j.methods().single { it.name() == "run" && it.parameters()[0].parameterizedType().isPrimitiveExcludingVoid }
        val runString = j.methods().single { it.name() == "run" && !it.parameters()[0].parameterizedType().isPrimitiveExcludingVoid }
        val size = j.getFieldByName("size", true)
        val kotlinTypes = result.kotlinTypes.flatMap { it.recursiveSubTypeStream().toList() }
        val k = kotlinTypes.first { it.simpleName() == "K" }
        val facade = kotlinTypes.first { it.simpleName() == "KKt" }
        fun at(host: io.codelaser.maddi.cst.api.info.Info, target: io.codelaser.maddi.cst.api.info.Info) =
            host.source().detailedSources().references(target).map { "${it.beginLine()}:${it.beginPos()}" }.sorted()
        val call = facade.methods().single { it.name() == "call" }
        assertEquals(listOf("4:46"), at(k.methods().single { it.name() == "run" }, runInt), "super.run(n)")
        assertEquals(listOf("6:20", "6:47"), at(call, runInt), "j.run(2), and j.run(it) inside the lambda")
        // j.run(it) inside the lambda: K2 answers the call with both overloads there (resolveSymbol gives none, the
        // reference's candidates are both), so it is recorded against both -- a rename of one is refused as sharing
        // the spelling, not planned wrong
        assertEquals(listOf("6:47", "6:76"), at(call, runString), "the other overload, by its parameter type")
        assertEquals(listOf("6:89"), at(call, size), "a Java field")
        assertEquals(listOf("2:10"), at(facade, j), "the import")
    }
}
