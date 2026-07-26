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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An annotation on a <em>binary</em> type whose enum-constant value cannot be resolved, because the enum's own
 * class file is not on the class path.
 * <p>
 * javac does not fail on this. {@code ClassReader.deproxy} substitutes
 * {@code new VarSymbol(0, name, syms.botType, enumTypeSym)} for the constant and reports "unknown enum constant"
 * as a <em>warning</em> — and {@code MaddiDiagnosticCollector} keeps only ERROR diagnostics, so nothing of it
 * reaches the caller. The {@code <nulltype>} that arrives in {@code ClassSymbolScanner.convert} is therefore not
 * the type of {@code null} but a failure marker, and the field it names does not exist.
 * <p>
 * Before {@code unresolvedEnumConstant} recognised it, converting that type hit the no-case throw and
 * {@code annotationValue}'s blanket {@code catch (RuntimeException)} dropped the key/value pair without a trace:
 * on a run whose class path had junit but not apiguardian, that was 838 swallowed
 * {@code UnsupportedOperationException}s while the parse still reported no errors.
 * <p>
 * What this pins: the parse survives, the annotation is still attached, and only the unresolvable value is gone.
 * {@link #resolvableEnumConstantIsKept()} is the control — with the enum present, the value must be there.
 */
public class TestUnresolvedEnumConstantInAnnotation extends CommonTest {

    private static final String SOURCE = "package a.b; public class X { lib.Target t; }";

    @Test
    public void unresolvableEnumConstantDropsOnlyThatValue() throws IOException {
        classPathOverride = List.of(fixture(false).toFile());

        TypeInfo target = targetOf(scan("a.b.X", SOURCE));
        AnnotationExpression ann = annotation(target);
        // the annotation survives; its single value did not, because there is no constant to point at
        assertTrue(ann.keyValuePairs().isEmpty(),
                "expected the unresolvable value to be dropped, got " + ann.keyValuePairs());
    }

    @Test
    public void resolvableEnumConstantIsKept() throws IOException {
        classPathOverride = List.of(fixture(true).toFile());

        TypeInfo target = targetOf(scan("a.b.X", SOURCE));
        AnnotationExpression ann = annotation(target);
        assertEquals(1, ann.keyValuePairs().size(), "expected the enum value to be read");
        AnnotationExpression.KV kv = ann.keyValuePairs().getFirst();
        assertEquals("value", kv.key());
        assertEquals("E.X", kv.value().toString());
    }

    private static AnnotationExpression annotation(TypeInfo target) {
        List<AnnotationExpression> annotations = target.annotations();
        assertEquals(1, annotations.size(), "expected exactly one annotation on lib.Target, got " + annotations);
        AnnotationExpression ann = annotations.getFirst();
        assertEquals("lib.Ann", ann.typeInfo().fullyQualifiedName());
        return ann;
    }

    /** {@code lib.Target}, reached through the field declared in the scanned source. */
    private static TypeInfo targetOf(TypeInfo x) {
        TypeInfo target = x.fields().getFirst().type().typeInfo();
        assertEquals("lib.Target", target.fullyQualifiedName());
        return target;
    }

    /**
     * A class path holding {@code lib.Ann} and {@code lib.Target} (annotated {@code @Ann(E.X)}), with the enum
     * {@code lib.E} present or removed. Removing the class file after compilation is what makes the annotation
     * unresolvable while the annotated class itself stays perfectly readable — the same shape as a library whose
     * annotation dependency was not put on the class path.
     */
    private Path fixture(boolean keepEnum) throws IOException {
        Path dir = Files.createTempDirectory("maddi-unresolved-enum");
        dir.toFile().deleteOnExit();
        Path src = Files.createDirectory(dir.resolve("src"));
        Path out = Files.createDirectory(dir.resolve("classes"));
        Files.writeString(src.resolve("E.java"), "package lib; public enum E { X, Y }");
        Files.writeString(src.resolve("Ann.java"), """
                package lib;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Ann { E value(); }
                """);
        Files.writeString(src.resolve("Target.java"), "package lib; @Ann(E.X) public class Target { }");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null, "-d", out.toString(),
                src.resolve("E.java").toString(), src.resolve("Ann.java").toString(),
                src.resolve("Target.java").toString());
        assertEquals(0, rc, "fixture must compile");

        if (!keepEnum) {
            File enumClass = out.resolve("lib").resolve("E.class").toFile();
            assertTrue(enumClass.delete(), "must be able to remove the enum's class file");
        }
        return out;
    }
}
