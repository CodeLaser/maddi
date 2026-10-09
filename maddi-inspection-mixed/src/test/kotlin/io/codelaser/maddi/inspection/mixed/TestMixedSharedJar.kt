package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider

/**
 * One library jar on the class path of Kotlin AND Java source sets, parsed under -ea: "type
 * io.micronaut.http.client.HttpClient committed twice from jar:…HttpClient.class" on a 225-set mixed project
 * (2026-10-09), whose parse is single-threaded. Each test is one of [MixedProjectInspector.parse]'s paths.
 */
class TestMixedSharedJar {

    @field:TempDir
    lateinit var tempRoot: Path

    private fun assertionsOn(): Boolean {
        var on = false
        assert(true.also { on = it })
        return on
    }

    private fun jar(): SourceSet {
        val src = tempRoot.resolve("lib-src/a/b")
        Files.createDirectories(src)
        Files.writeString(src.resolve("Client.java"), """
            package a.b;
            public interface Client extends AutoCloseable {
                String retrieve(String uri);
                static Client create(String base) { return u -> base + u; }
                @Override default void close() { }
            }
            """.trimIndent())
        Files.writeString(src.resolve("Config.java"), """
            package a.b;
            public class Config {
                public Client client() { return Client.create("x"); }
            }
            """.trimIndent())
        val classes = Files.createDirectories(tempRoot.resolve("lib-classes"))
        val compiler = ToolProvider.getSystemJavaCompiler()
        assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(),
            src.resolve("Client.java").toString(), src.resolve("Config.java").toString()))
        val jar = tempRoot.resolve("client-core-1.0.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { jos ->
            Files.walk(classes).filter { Files.isRegularFile(it) }.forEach {
                jos.putNextEntry(JarEntry(classes.relativize(it).toString().replace('\\', '/')))
                jos.write(Files.readAllBytes(it))
                jos.closeEntry()
            }
        }
        return SourceSetImpl.Builder().setName(jar.fileName.toString()).setSourceDirectories(listOf())
            .setUri(jar.toUri()).setLibrary(true).setExternalLibrary(true).build()
    }

    private fun dir(name: String, files: Map<String, String>): Path {
        val dir = tempRoot.resolve(name).resolve("src/main/java")
        files.forEach { (path, content) ->
            val file = dir.resolve(path)
            Files.createDirectories(file.parent)
            Files.writeString(file, content.trimIndent())
        }
        return dir
    }

    private fun set(name: String, dir: Path, vararg deps: SourceSet): SourceSet =
        SourceSetImpl.Builder().setName(name).setSourceDirectories(listOf(dir)).setUri(dir.toUri())
            .setDependencies(deps.toList()).build()

    private val javaUse = """
        package j;
        import a.b.Client;
        public class LicenseService {
            private final Client client = Client.create("http://h");
            public String check(String key) { try (Client c = client) { return c.retrieve("/l/" + key); } catch (Exception e) { return null; } }
        }
        """

    private val kotlinUse = """
        package k
        import a.b.Client
        import a.b.Config
        class Fetcher(private val client: Client) {
            fun fetch(path: String): String = client.retrieve(path)
            fun fromConfig(): Client = Config().client()
        }
        """

    private fun parse(vararg sets: SourceSet, lib: SourceSet) {
        assertTrue(assertionsOn(), "run with -ea, or this test asserts nothing")
        val builder = InputConfigurationImpl.Builder().addClassPathParts(lib)
        sets.forEach { builder.addSourceSets(it) }
        val result = MixedProjectInspector().parse(builder.build())
        val summary = result.javaSummary!!
        assertTrue(!summary.haveErrors(), summary.parseExceptions().map { it.message }.toString())
    }

    /** Kotlin first, then Java through the stubs: Java sets on the jar, one of them on the Kotlin set too. */
    @Test
    fun javaOnKotlin() {
        val lib = jar()
        val k = set("k", dir("k", mapOf("k/Fetcher.kt" to kotlinUse)), lib)
        val j1 = set("j1", dir("j1", mapOf("j/LicenseService.java" to javaUse)), lib, k)
        val j2 = set("j2", dir("j2", mapOf("j/LicenseService.java" to javaUse)), lib)
        parse(k, j1, j2, lib = lib)
    }

    /** Java first, then Kotlin: the Kotlin set depends on a Java set, both on the jar. */
    @Test
    fun kotlinOnJava() {
        val lib = jar()
        val j1 = set("j1", dir("j1", mapOf("j/LicenseService.java" to javaUse)), lib)
        val k = set("k", dir("k", mapOf("k/Fetcher.kt" to kotlinUse)), lib, j1)
        val j2 = set("j2", dir("j2", mapOf("j/LicenseService.java" to javaUse)), lib)
        parse(j1, k, j2, lib = lib)
    }

    /** Interleaved: a set with Java and Kotlin, then a Java set and a Kotlin set on the jar. */
    @Test
    fun interleaved() {
        val lib = jar()
        val m = set("m", dir("m", mapOf("k/Fetcher.kt" to kotlinUse, "j/LicenseService.java" to javaUse)), lib)
        val j2 = set("j2", dir("j2", mapOf("j2/LicenseService.java" to javaUse.replace("package j;", "package j2;"))), lib, m)
        val k2 = set("k2", dir("k2", mapOf("k2/Fetcher.kt" to kotlinUse.replace("package k", "package k2"))), lib)
        parse(m, j2, k2, lib = lib)
    }

    /** Interleaved across sets: Kotlin on Java and Java on Kotlin. */
    @Test
    fun bothDirections() {
        val lib = jar()
        val j1 = set("j1", dir("j1", mapOf("j/LicenseService.java" to javaUse)), lib)
        val k = set("k", dir("k", mapOf("k/Fetcher.kt" to kotlinUse)), lib, j1)
        val j2 = set("j2", dir("j2", mapOf("j2/Use.java" to """
            package j2;
            public class Use { public String f(k.Fetcher f) { return f.fetch("/"); } a.b.Client c() { return a.b.Client.create("y"); } }
            """)), lib, k)
        val k2 = set("k2", dir("k2", mapOf("k2/Fetcher.kt" to kotlinUse.replace("package k", "package k2"))), lib, j2)
        parse(j1, k, j2, k2, lib = lib)
    }
}
