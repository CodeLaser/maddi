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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Java parsed by the openjdk front end, printed as Rust, and compiled by rustc when one is on the {@code PATH}. The
 * verdicts are set on the declarations by the test, as the analysis would store them: this module does not depend on
 * the analysis (maddi-mod), and the point of several tests is a verdict that is deliberately wrong.
 */
public abstract class CommonJavaToRust {
    protected JavaInspector javaInspector;
    private String test;

    @BeforeEach
    public void beforeEach(TestInfo info) throws IOException {
        test = info.getTestMethod().map(Method::getName).orElseThrow();
        javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL)
                .setUri(URI.create("file:/")).build();
        javaInspector.initialize(new InputConfigurationImpl.Builder().addSourceSets(sourceSet)
                .addClassPath("jmod:java.base").build());
        javaInspector.onlyPreload();
    }

    /** The primary types of {@code java}, one compilation unit. */
    protected List<TypeInfo> parse(String java) {
        String code = java.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        String pkg = code.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = code.replaceAll("(?s)^.*?(?<![@\\w])(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        return List.copyOf(javaInspector.parse(Map.of(pkg + "." + first, java),
                new JavaInspector.ParseOptions.Builder().build()).parseResult().primaryTypes());
    }

    /** The translation of {@code types}, all of one package, written as a crate under {@code build/rust-samples}. */
    protected RustTranslator.Result translate(List<TypeInfo> types) throws IOException {
        return translate(types, RustFacts.STORED);
    }

    protected RustTranslator.Result translate(List<TypeInfo> types, RustFacts facts) throws IOException {
        Path out = Path.of("build/rust-samples", test);
        return RustTranslator.translate(types, facts, out);
    }

    /** The stored ownership verdicts, with the fields, parameters and methods named in {@code nullable} nullable. */
    protected static RustFacts nullable(java.util.Set<String> nullable) {
        return RustFacts.of(new io.codelaser.maddi.cst.impl.analysis.NullabilityFacts() {
            @Override
            public io.codelaser.maddi.cst.api.type.ParameterizedType field(io.codelaser.maddi.cst.api.info.FieldInfo f) {
                return nullable.contains(f.name()) ? f.type().withNullable(NullableState.NULLABLE) : null;
            }

            @Override
            public io.codelaser.maddi.cst.api.type.ParameterizedType parameter(ParameterInfo p) {
                return nullable.contains(p.name()) ? p.parameterizedType().withNullable(NullableState.NULLABLE) : null;
            }

            @Override
            public io.codelaser.maddi.cst.api.type.ParameterizedType returnType(MethodInfo m) {
                return nullable.contains(m.name() + "()") ? m.returnType().withNullable(NullableState.NULLABLE) : null;
            }
        }, RustFacts.STORED);
    }

    /** The text of the module the translation wrote for {@code packageName}. */
    protected String module(String packageName) throws IOException {
        return Files.readString(Path.of("build/rust-samples", test, "src", RustNames.module(packageName) + ".rs"));
    }

    /**
     * rustc's errors for the crate of the last {@link #translate}, as {@code E0596 ...} lines; skips the test when rustc
     * is not installed. Warnings are not errors.
     */
    protected List<String> rustcErrors() throws IOException, InterruptedException {
        Path crate = Path.of("build/rust-samples", test).toAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder("rustc", "--edition", RustTranslator.EDITION, "--crate-type", "lib",
                "--error-format", "short", "-o", crate.resolve("lib.rlib").toString(),
                crate.resolve("src/lib.rs").toString()).redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            assumeTrue(false, "rustc is not installed");
            return List.of();
        }
        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(2, TimeUnit.MINUTES), "rustc did not finish");
        Files.writeString(crate.resolve("rustc.txt"), output);
        List<String> errors = new ArrayList<>();
        for (String line : output.lines().toList()) {
            int at = line.indexOf(": error");
            if (at >= 0) errors.add(line);
        }
        return errors;
    }

    // ---- the verdicts, as the analysis stores them

    protected static MethodInfo method(TypeInfo type, String name) {
        return type.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    protected static void nonModifying(TypeInfo type, String... names) {
        for (String name : names) {
            method(type, name).analysis().set(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.TRUE);
        }
    }

    protected static void modifying(TypeInfo type, String... names) {
        for (String name : names) {
            method(type, name).analysis().set(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE);
        }
    }

    /** A parameter the method neither modifies nor keeps: {@code &T}. */
    protected static void borrowed(ParameterInfo parameter) {
        parameter.analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.TRUE);
        parameter.analysis().set(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.INDEPENDENT);
    }

    /** A parameter the method keeps: {@code T}. */
    protected static void kept(ParameterInfo parameter, Value.Independent independent) {
        parameter.analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.TRUE);
        parameter.analysis().set(PropertyImpl.INDEPENDENT_PARAMETER, independent);
    }

    /** The method returns part of its receiver: {@code &T}. */
    protected static void dependentResult(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.DEPENDENT);
    }

    protected static void independentResult(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.INDEPENDENT);
    }

    /** Line by line, indentation ignored. */
    protected static void contains(String rust, String expected) {
        assertTrue(strip(rust).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + rust);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }
}
