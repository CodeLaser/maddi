package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * A Kotlin body calling members of a class in JAVA SOURCE, static and instance (#68's example): each call is the
 * Java front end's MethodInfo, with its arguments, in both layouts -- a Kotlin source set depending on a Java one
 * (Java parsed first), and one source set holding both (interleaved).
 */
class TestKotlinCallsJavaSource {

    @field:TempDir
    lateinit var tempRoot: Path

    private val java = """
        package q;
        public class J {
            public static void fill(java.util.List<String> target) { target.add("x"); }
            public void put(java.util.List<String> target) { target.add("y"); }
        }
        """.trimIndent() + "\n"

    private val kotlin = """
        package u
        import q.J
        class Uses {
            fun handed() { J.fill(emptyList()) }
            fun staticNoArgs() { J.fill(java.util.ArrayList()) }
            fun viaInstance() { J().put(emptyList()) }
        }
        """.trimIndent() + "\n"

    // named by its jar: a class-path source set of any other name is off javac's class path
    private fun stdlib(): io.codelaser.maddi.cst.api.element.SourceSet {
        val jar = Path.of(JvmOverloads::class.java.protectionDomain.codeSource.location.toURI())
        return SourceSetImpl.Builder().setName(jar.fileName.toString()).setSourceDirectories(listOf())
            .setUri(jar.toUri()).setLibrary(true).setExternalLibrary(true).build()
    }

    private fun calls(method: MethodInfo): List<MethodInfo> {
        val out = mutableListOf<MethodInfo>()
        method.methodBody().visit { e -> if (e is MethodCall) out += e.methodInfo(); true }
        return out
    }

    private fun check(result: MixedProjectInspector.Result) {
        val census = PlaceholderCensus.of(result.kotlinTypes)
        assertEquals(0, census.total, census.dumpLines().toString())
        val j: TypeInfo = result.javaTypes.first { it.simpleName() == "J" }
        val uses = result.kotlinTypes.first { it.simpleName() == "Uses" }
        assertSame(j.findUniqueMethod("fill", 1), calls(uses.findUniqueMethod("handed", 0)).first { it.name() == "fill" })
        assertSame(j.findUniqueMethod("fill", 1), calls(uses.findUniqueMethod("staticNoArgs", 0)).first { it.name() == "fill" })
        assertSame(j.findUniqueMethod("put", 1), calls(uses.findUniqueMethod("viaInstance", 0)).first { it.name() == "put" })
    }

    @Test
    fun kotlinSourceSetDependingOnAJavaOne() {
        val tmp = Files.createTempDirectory(tempRoot, "k2j")
        val jDir = tmp.resolve("src/main/java").also { Files.createDirectories(it.resolve("q")) }
        val kDir = tmp.resolve("src/main/kotlin").also { Files.createDirectories(it.resolve("u")) }
        Files.writeString(jDir.resolve("q/J.java"), java)
        Files.writeString(kDir.resolve("u/Uses.kt"), kotlin)
        val stdlib = stdlib()
        val javaSet = SourceSetImpl.Builder().setName("java/main").setSourceDirectories(listOf(jDir))
            .setUri(jDir.toUri()).setDependencies(listOf(stdlib)).build()
        val kotlinSet = SourceSetImpl.Builder().setName("kotlin/main").setSourceDirectories(listOf(kDir))
            .setUri(kDir.toUri()).setDependencies(listOf(javaSet, stdlib)).build()
        check(MixedProjectInspector().parse(InputConfigurationImpl.Builder().addClassPathParts(stdlib)
            .addSourceSets(javaSet).addSourceSets(kotlinSet).build()))
    }

    @Test
    fun oneSourceSetHoldingBoth() {
        val dir = Files.createTempDirectory(tempRoot, "one").resolve("src/main/java")
        Files.createDirectories(dir.resolve("q"))
        Files.createDirectories(dir.resolve("u"))
        Files.writeString(dir.resolve("q/J.java"), java)
        Files.writeString(dir.resolve("u/Uses.kt"), kotlin)
        val stdlib = stdlib()
        val main = SourceSetImpl.Builder().setName("main").setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .setDependencies(listOf(stdlib)).build()
        check(MixedProjectInspector().parse(InputConfigurationImpl.Builder().addClassPathParts(stdlib)
            .addSourceSets(main).build()))
    }
}
