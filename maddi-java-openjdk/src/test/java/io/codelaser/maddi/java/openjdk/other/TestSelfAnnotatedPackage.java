package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A package annotated with an annotation type declared in that same package, read from class files: Spring's
 * {@code org.springframework.lang} and micrometer's {@code io.micrometer.common.lang} carry their own
 * {@code @NonNullApi}. Loading the annotation's type builds its unit, which asks for its package's annotations,
 * which builds {@code @NonNullApi} again: a StackOverflowError in ClassSymbolScanner.packageAnnotations that dropped
 * 16 of nacos's units (maddi 29745e935).
 */
public class TestSelfAnnotatedPackage extends CommonTest {

    @TempDir
    private Path tempRoot;

    @Test
    public void packageAnnotatedWithItsOwnAnnotation() throws IOException {
        classPathOverride = List.of(fixture().toFile());
        TypeInfo x = scan("a.b.X", "package a.b; public class X { lib.Service t; }");
        TypeInfo service = x.fields().getFirst().type().typeInfo();
        assertEquals("lib.Service", service.fullyQualifiedName());
        assertEquals("[lib.NonNullApi]", service.compilationUnit().packageAnnotations().stream()
                .map(ae -> ae.typeInfo().fullyQualifiedName()).toList().toString());
    }

    private Path fixture() throws IOException {
        Path dir = Files.createTempDirectory(tempRoot, "maddi-self-annotated-package");
        Path src = Files.createDirectories(dir.resolve("src").resolve("lib"));
        Path out = Files.createDirectory(dir.resolve("classes"));
        Files.writeString(src.resolve("NonNullApi.java"), """
                package lib;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PACKAGE)
                public @interface NonNullApi { }
                """);
        Files.writeString(src.resolve("package-info.java"), "@NonNullApi package lib;");
        Files.writeString(src.resolve("Service.java"), "package lib; public class Service { }");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null, "-d", out.toString(), src.resolve("NonNullApi.java").toString(),
                src.resolve("package-info.java").toString(), src.resolve("Service.java").toString());
        assertEquals(0, rc, "fixture must compile");
        return out;
    }
}
