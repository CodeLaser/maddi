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
        SourceSet annotation = SourceSetImpl.sourceSetOf(io.codelaser.maddi.annotation.Immutable.class);
        SourceSet support = SourceSetImpl.sourceSetOf(io.codelaser.maddi.support.SetOnce.class, annotation);
        javaInspector.initialize(new InputConfigurationImpl.Builder().addSourceSets(sourceSet)
                .addClassPath("jmod:java.base").addClassPathParts(annotation, support).build());
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

    /** A parameter the method modifies and does not keep: {@code &mut T}. */
    protected static void exclusive(ParameterInfo parameter) {
        parameter.analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
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

    protected static void immutable(TypeInfo type, Value.Immutable immutable) {
        type.analysis().set(PropertyImpl.IMMUTABLE_TYPE, immutable);
    }

    /** No method modifies the object the field holds. */
    protected static void unmodifiedField(TypeInfo type, String name) {
        type.getFieldByName(name, true).analysis().set(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.TRUE);
    }

    /** A parameter the method modifies, and does not keep: {@code &mut T}. */
    protected static void modified(ParameterInfo parameter) {
        parameter.analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
        parameter.analysis().set(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.INDEPENDENT);
    }

    protected static void finalizer(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.FINALIZER_METHOD, ValueImpl.BoolImpl.TRUE);
    }

    /** {@code @Mark}, {@code @Only(before/after)} of the transition of {@code label}. */
    protected static void mark(TypeInfo type, String name, String label) {
        method(type, name).analysis().set(PropertyImpl.EVENTUAL_METHOD,
                new ValueImpl.EventualImpl(java.util.Set.of(label), true, null, null));
    }

    protected static void only(TypeInfo type, String name, String label, boolean after) {
        method(type, name).analysis().set(PropertyImpl.EVENTUAL_METHOD,
                new ValueImpl.EventualImpl(java.util.Set.of(label), false, after, null));
    }

    protected static void ignoreModifications(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.IGNORE_MODIFICATION_METHOD, ValueImpl.BoolImpl.TRUE);
    }

    protected static void staticSideEffects(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.TRUE);
    }

    /**
     * The parameter is unmodified, but handed (whole: {@code k:j}, an element: {@code k:j:e}) to the SAM of functional
     * parameter {@code k}: modified when the caller's function modifies (MODIFIED_THROUGH_PASSED_FUNCTION).
     */
    protected static void throughPassedFunction(ParameterInfo parameter, String... entries) {
        borrowed(parameter);
        parameter.analysis().set(PropertyImpl.MODIFIED_THROUGH_PASSED_FUNCTION,
                new ValueImpl.SetOfStringsImpl(java.util.Set.of(entries)));
    }

    /** The method returns {@code this}. */
    protected static void fluent(TypeInfo type, String name) {
        method(type, name).analysis().set(PropertyImpl.FLUENT_METHOD, ValueImpl.BoolImpl.TRUE);
    }

    /** Line by line, indentation ignored. */
    protected static void contains(String rust, String expected) {
        assertTrue(strip(rust).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + rust);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }
}
