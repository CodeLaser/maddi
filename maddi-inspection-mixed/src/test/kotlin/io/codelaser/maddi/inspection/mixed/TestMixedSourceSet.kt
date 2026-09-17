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
        """.trimIndent()

    @Test
    fun javaAndKotlinInOneDirectoryShareTheirTypesBothWays() {
        val dir = Files.createTempDirectory(tempRoot, "mixed-one-set").resolve("src/main/java")
        Files.createDirectories(dir.resolve("p"))
        Files.writeString(dir.resolve("p/Handler.java"), handler)
        Files.writeString(dir.resolve("p/Router.java"), router)
        Files.writeString(dir.resolve("p/Echo.java"), echo)
        Files.writeString(dir.resolve("p/Context.kt"), context)
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .build()
        val config = InputConfigurationImpl.Builder().addSourceSets(main).build()

        val result = MixedProjectInspector().parse(config)

        fun java(name: String): TypeInfo = result.javaTypes.first { it.simpleName() == name }
        fun kotlin(name: String): TypeInfo = result.kotlinTypes.first { it.simpleName() == name }
        val handlerType = java("Handler")
        val routerType = java("Router")
        val echoType = java("Echo")
        val contextType = kotlin("Context")
        val defaultContext = kotlin("DefaultContext")
        assertEquals(setOf("Echo", "Handler", "Router"), result.javaTypes.map { it.simpleName() }.toSet())
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
