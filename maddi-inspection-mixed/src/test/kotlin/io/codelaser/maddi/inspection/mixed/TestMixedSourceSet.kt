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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * ONE source set, Java and Kotlin side by side in one directory, each naming the other -- javalin's layout, where
 * `Handler.java` takes a Kotlin `Context` and Kotlin code takes a `Handler`. Neither front end can go first, so the
 * Kotlin declarations are made before javac attributes the set and the Kotlin bodies after its Java types commit
 * (`SourceSetInterleave`). What must hold is identity in both directions, in signatures AND in bodies.
 */
class TestMixedSourceSet {

    @field:TempDir
    lateinit var tempRoot: Path

    private val handler = """
        package p;

        @FunctionalInterface
        public interface Handler {
            void handle(Context ctx) throws Exception;
        }
        """.trimIndent()

    private val router = """
        package p;

        import java.util.ArrayList;
        import java.util.List;

        public final class Router {
            private final List<Handler> handlers = new ArrayList<>();

            public Router add(Handler handler) {
                handlers.add(handler);
                return this;
            }

            public void run(Context ctx) throws Exception {
                for (Handler h : handlers) h.handle(ctx);
                ctx.result("done");
            }

            public static class Nested {
                public int n;
            }
        }
        """.trimIndent()

    // a Java class implementing a Kotlin interface that has a default method: its stub must say `default`,
    // though the stub is made before the Kotlin body exists
    private val echo = """
        package p;

        public class Echo implements Context {
            @Override
            public Context result(String text) {
                return this;
            }
        }
        """.trimIndent()

    private val context = """
        package p

        interface Context {
            fun result(text: String): Context
            fun status(): Int = 200
        }

        class DefaultContext(private val router: Router) : Context {
            private var last: String = ""

            override fun result(text: String): Context {
                last = text
                return this
            }

            fun route(handler: Handler): Router = router.add(handler)

            fun nested(): Router.Nested = Router.Nested()
        }

        class Settings(val level: Int = 1, val name: String = "x") {
            @JvmOverloads
            fun greet(who: String, times: Int = 1, loud: Boolean = false): String = who.repeat(times) + loud

            // use-site projections are wildcards on the JVM: Java passes an Integer.class and an ArrayList<Object>
            fun accept(type: Class<*>, sink: MutableList<in String>, source: List<out Number>): Int = source.size

            // `T & Any` is `T`: Java's `Integer i = settings.first(Integer.class)`
            fun <T> first(type: Class<T>): T & Any = type.getDeclaredConstructor().newInstance()!!
        }

        // an object's JVM statics, named on the type by Java
        object Registry {
            const val KEY = "k"
            @JvmField val shared = StringBuilder()
            @JvmStatic @JvmOverloads fun log(message: String, level: Int = 0): String = message + level
            @JvmStatic val Any.tag: String get() = toString()
        }
        """.trimIndent()

    // Java leaving Kotlin's defaults out: it calls the overloads kotlinc adds, which must be CST members
    private val user = """
        package p;

        public class User {
            public String use() {
                java.util.List<Object> sink = new java.util.ArrayList<>();
                java.util.List<Integer> source = java.util.List.of(1);
                return new Settings().greet("a") + new Settings().greet("b", 2)
                    + new Settings().accept(Integer.class, sink, source) + first();
            }

            private static String statics(Object o) {
                return Registry.KEY + Registry.shared + Registry.log("x") + Registry.log("y", 1) + Registry.getTag(o);
            }

            private static Integer first() {
                Integer i = new Settings().first(Integer.class);
                return i;
            }
        }
        """.trimIndent()

    @Test
    fun javaAndKotlinInOneDirectoryShareTheirTypesBothWays() {
        val dir = Files.createTempDirectory(tempRoot, "mixed-one-set").resolve("src/main/java")
        Files.createDirectories(dir.resolve("p"))
        Files.writeString(dir.resolve("p/Handler.java"), handler)
        Files.writeString(dir.resolve("p/Router.java"), router)
        Files.writeString(dir.resolve("p/Echo.java"), echo)
        Files.writeString(dir.resolve("p/User.java"), user)
        Files.writeString(dir.resolve("p/Context.kt"), context)
        // kotlin-stdlib, for `@JvmOverloads` to resolve
        val stdlib = Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        val stdlibSet = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .setDependencies(listOf(stdlibSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet).addSourceSets(main).build()

        val result = MixedProjectInspector().parse(config)

        fun java(name: String): TypeInfo = result.javaTypes.first { it.simpleName() == name }
        fun kotlin(name: String): TypeInfo = result.kotlinTypes.first { it.simpleName() == name }
        val handlerType = java("Handler")
        val routerType = java("Router")
        val echoType = java("Echo")
        val contextType = kotlin("Context")
        val defaultContext = kotlin("DefaultContext")
        assertEquals(setOf("Echo", "Handler", "Router", "User"), result.javaTypes.map { it.simpleName() }.toSet())
        listOf(handlerType, routerType, contextType, defaultContext).forEach {
            assertEquals("main", it.compilationUnit().sourceSet().name(), it.fullyQualifiedName())
            assertTrue(it.hasBeenInspected(), it.fullyQualifiedName())
        }

        // Java signature -> Kotlin type
        assertSame(contextType, handlerType.findUniqueMethod("handle", 1).parameters()[0].parameterizedType().typeInfo())
        assertTrue(echoType.interfacesImplemented().any { it.typeInfo() === contextType })
        // Kotlin signature -> Java type, a nested one included
        val route = defaultContext.findUniqueMethod("route", 1)
        assertSame(handlerType, route.parameters()[0].parameterizedType().typeInfo())
        assertSame(routerType, route.returnType().typeInfo())
        assertSame(routerType.findSubType("Nested"), defaultContext.findUniqueMethod("nested", 0).returnType().typeInfo())

        // bodies, both ways: the callee is the other front end's MethodInfo, not a placeholder or a copy
        val add = routerType.findUniqueMethod("add", 1)
        assertSame(add, calls(route).single())
        val result0 = contextType.findUniqueMethod("result", 1)
        assertTrue(calls(routerType.findUniqueMethod("run", 1)).any { it === result0 },
            "Router.run calls the Kotlin Context.result")
        // and overriding across the boundary
        assertTrue(echoType.findUniqueMethod("result", 1).overrides().contains(result0))

        // kotlinc's overloads: a Java call binds to a synthetic member whose body calls the `$default`
        val settings = kotlin("Settings")
        val noArgs = settings.constructors().single { it.parameters().isEmpty() }
        assertTrue(noArgs.isSynthetic)
        val use = java("User").findUniqueMethod("use", 0)
        val constructed = mutableListOf<MethodInfo>()
        use.methodBody().visit { e: Element ->
            if (e is ConstructorCall) constructed += e.constructor()
            true
        }
        assertEquals(listOf(noArgs, noArgs, noArgs), constructed.filter { it.typeInfo() === settings })
        val registry = kotlin("Registry")
        assertEquals(listOf("log", "log", "getTag"),
            calls(java("User").findUniqueMethod("statics", 1)).filter { it.typeInfo() === registry }.map { it.name() })
        val greets = calls(use).filter { it.name() == "greet" }
        assertEquals(listOf(1, 2), greets.map { it.parameters().size })
        val accept = settings.findUniqueMethod("accept", 3)
        assertTrue(calls(use).any { it === accept })
        assertEquals(listOf("?", "? super String", "? extends Number"),
            accept.parameters().map { it.parameterizedType().parameters().single().let { a ->
                if (a.isUnboundWildcard) "?" else (if (a.wildcard().isSuper) "? super " else "? extends ") + a.typeInfo().simpleName()
            } })
        greets.forEach { greet ->
            assertTrue(greet.isSynthetic && greet.typeInfo() === settings, greet.fullyQualifiedName())
            assertEquals(listOf("greet\$default"), calls(greet).map { it.name() })
        }
    }

    /**
     * A mixed set downstream of a KOTLIN-ONLY set: Exposed's `exposed-maven-plugin` (a generated `HelpMojo.java`
     * beside `GenerateMigrationsMojo.kt`) on `exposed-plugin-core`. The mixed set's stubs name the upstream set's
     * types, so those must be declared and stubbed first. The interleave walked the REBUILT Java set's dependencies,
     * from which every Kotlin-only set had been dropped, so the upstream set was never converted and the stub
     * compilation failed: "package ...plugin.core.migration does not exist".
     */
    @Test
    fun aMixedSetSeesTheKotlinOnlySetItDependsOn() {
        val root = Files.createTempDirectory(tempRoot, "mixed-downstream")
        val coreDir = root.resolve("core/src/main/kotlin")
        Files.createDirectories(coreDir.resolve("core"))
        Files.writeString(coreDir.resolve("core/Format.kt"), """
            package core

            enum class Format { SHORT, LONG }
            class Config(val format: Format)
            """.trimIndent())
        val pluginDir = root.resolve("plugin/src/main/kotlin")
        Files.createDirectories(pluginDir.resolve("plugin"))
        Files.writeString(pluginDir.resolve("plugin/Help.java"), """
            package plugin;

            public class Help {
                public String text() { return "help"; }
            }
            """.trimIndent())
        Files.writeString(pluginDir.resolve("plugin/Mojo.kt"), """
            package plugin

            import core.Config
            import core.Format

            class Mojo(private val help: Help) {
                var format: Format = Format.SHORT
                fun config(): Config = Config(format)
                fun describe(): String = help.text()
            }
            """.trimIndent())
        val stdlib = Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        val stdlibSet = SourceSetImpl.Builder().setName("kotlin-stdlib").setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val core = SourceSetImpl.Builder().setName("core").setSourceDirectories(listOf(coreDir))
            .setUri(coreDir.toUri()).setDependencies(listOf(stdlibSet)).build()
        val plugin = SourceSetImpl.Builder().setName("plugin").setSourceDirectories(listOf(pluginDir))
            .setUri(pluginDir.toUri()).setDependencies(listOf(core, stdlibSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet)
            .addSourceSets(core).addSourceSets(plugin).build()

        val result = MixedProjectInspector().parse(config)

        val mojo = result.kotlinTypes.first { it.simpleName() == "Mojo" }
        val format = result.kotlinTypes.first { it.simpleName() == "Format" }
        assertEquals("core", format.compilationUnit().sourceSet().name())
        assertSame(format, mojo.findUniqueMethod("getFormat", 0).returnType().typeInfo())
        assertSame(result.javaTypes.single { it.simpleName() == "Help" },
            mojo.findUniqueMethod("describe", 0).let { calls(it).single() }.typeInfo())
    }

    /**
     * Overrides that kotlinc compiles with a BRIDGE (javap on Exposed): a covariant Kotlin return that Java's invariant
     * generics refuse (`List<List<String>>` for `Iterable<Iterable<String>>`), and `Nothing` (`Void`) for a
     * `List<String>`. Stubbed as declared, javac refused both overrides and the mixed parse failed; the stubs now carry
     * the bridge's type where javac refuses the declared one -- `void` included, for a `Nothing` overriding a `Unit`.
     */
    @Test
    fun overridesKotlincBridgesStillStub() {
        val dir = Files.createTempDirectory(tempRoot, "mixed-bridges").resolve("src/main/java")
        Files.createDirectories(dir.resolve("q"))
        Files.writeString(dir.resolve("q/Statement.kt"), """
            package q

            abstract class Statement {
                abstract fun arguments(): Iterable<Iterable<String>>
                open fun create(): List<String> = listOf()
                open fun <T, S : T?> update(t: T, s: S) {}
            }

            class Query : Statement() {
                override fun arguments(): List<List<String>> = listOf(listOf("a"))
                override fun create(): Nothing = throw UnsupportedOperationException()
                // `Nothing` for `Unit`: kotlinc's bridge is `void update(Object, Object)`
                override fun <T, S : T?> update(t: T, s: S) = error("unsupported")
            }
            """.trimIndent())
        Files.writeString(dir.resolve("q/Runner.java"), """
            package q;

            public class Runner {
                public int count(Statement s) {
                    int n = 0;
                    for (Iterable<String> row : s.arguments()) n++;
                    return n;
                }
                public Object query(Query q) { return q.arguments(); }
            }
            """.trimIndent())
        val stdlib = Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        // named after its jar: the Java side reaches stdlib types the other fixtures never touch
        val stdlibSet = SourceSetImpl.Builder().setName(stdlib.fileName.toString()).setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .setDependencies(listOf(stdlibSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet).addSourceSets(main).build()

        val result = MixedProjectInspector().parse(config)

        val query = result.kotlinTypes.first { it.simpleName() == "Query" }
        val runner = result.javaTypes.single { it.simpleName() == "Runner" }
        assertSame(query.findUniqueMethod("arguments", 0), calls(runner.findUniqueMethod("query", 1)).single(),
            "the Java call binds the Kotlin override itself, not a stub")
    }

    /**
     * A Kotlin class implementing the stdlib's `CoroutineContext.Element`, whose `get`/`fold`/`minusKey` have Kotlin
     * bodies but are ABSTRACT in the class file (DefaultImpls): kotlinc adds forwarders the CST does not have, so the
     * stub was "not abstract and does not override abstract method minusKey(Key<?>)" (Exposed's
     * TransactionContextHolderImpl). The stub now declares what javac asks for.
     */
    @Test
    fun defaultImplsForwardersStillStub() {
        val dir = Files.createTempDirectory(tempRoot, "mixed-defaultimpls").resolve("src/main/java")
        Files.createDirectories(dir.resolve("r"))
        Files.writeString(dir.resolve("r/Holder.kt"), """
            package r

            import kotlin.coroutines.CoroutineContext

            class Holder(val name: String) : CoroutineContext.Element {
                override val key: CoroutineContext.Key<*> get() = Key
                companion object Key : CoroutineContext.Key<Holder>
            }
            """.trimIndent())
        Files.writeString(dir.resolve("r/Use.java"), """
            package r;

            public class Use {
                public String name() { return new Holder("h").getName(); }
            }
            """.trimIndent())
        val stdlib = Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        val stdlibSet = SourceSetImpl.Builder().setName(stdlib.fileName.toString()).setSourceDirectories(listOf())
            .setUri(stdlib.toUri()).setLibrary(true).setExternalLibrary(true).build()
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .setDependencies(listOf(stdlibSet)).build()
        val config = InputConfigurationImpl.Builder().addClassPathParts(stdlibSet).addSourceSets(main).build()

        val result = MixedProjectInspector().parse(config)

        val holder = result.kotlinTypes.first { it.simpleName() == "Holder" }
        assertSame(holder.findUniqueMethod("getName", 0),
            calls(result.javaTypes.single { it.simpleName() == "Use" }.findUniqueMethod("name", 0)).single())
    }

    private fun calls(method: MethodInfo): List<MethodInfo> {
        val found = mutableListOf<MethodInfo>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall) found += e.methodInfo()
            true
        }
        return found
    }
}
