package io.codelaser.maddi.java.openjdk.other;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * A library member's CLASS-retention annotations (the class file's {@code RuntimeInvisibleAnnotations} and
 * {@code RuntimeInvisibleParameterAnnotations}) are read like the RUNTIME ones: javac's ClassReader attaches both to
 * the symbol, so {@code ClassSymbolScanner.loadAnnotations} sees them. SpotBugs' {@code @CheckForNull} on jenkins
 * remoting's {@code JnlpConnectionState.getStash} is such an annotation (CodeLaser/maddi#161). Also when the
 * annotation's own class file is not on the class path, as with a compile-only spotbugs-annotations dependency.
 */
public class TestClassRetentionAnnotation extends CommonTest {

    @TempDir
    private Path tempRoot;

    private static final String SOURCE = "package a.b; public class X { lib.Lib l; }";

    @Test
    public void annotationOnTheClassPath() throws IOException {
        check(true);
    }

    @Test
    public void annotationNotOnTheClassPath() throws IOException {
        check(false);
    }

    private void check(boolean keepAnnotation) throws IOException {
        classPathOverride = List.of(fixture(keepAnnotation).toFile());
        TypeInfo lib = scan("a.b.X", SOURCE).fields().getFirst().type().typeInfo();
        classSymbolScanner.commitType(lib);
        MethodInfo get = lib.findUniqueMethod("get", 1);
        assertEquals("[@CheckForNull]", get.annotations().toString());
        assertEquals("lib.CheckForNull", get.annotations().getFirst().typeInfo().fullyQualifiedName());
        assertEquals("[@CheckForNull]", get.parameters().getFirst().annotations().toString());
    }

    private Path fixture(boolean keepAnnotation) throws IOException {
        Path dir = Files.createTempDirectory(tempRoot, "maddi-class-retention");
        Path src = Files.createDirectory(dir.resolve("src"));
        Path out = Files.createDirectory(dir.resolve("classes"));
        Files.writeString(src.resolve("CheckForNull.java"), """
                package lib;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.CLASS)
                public @interface CheckForNull { }
                """);
        Files.writeString(src.resolve("Lib.java"), """
                package lib;
                public class Lib {
                    @CheckForNull public Object get(@CheckForNull Object o) { return o; }
                }
                """);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null, "-d", out.toString(),
                src.resolve("CheckForNull.java").toString(), src.resolve("Lib.java").toString());
        assertEquals(0, rc);
        if (!keepAnnotation) {
            assertTrue(out.resolve("lib").resolve("CheckForNull.class").toFile().delete());
        }
        return out;
    }
}
