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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A Java program as one Rust library crate: {@code Cargo.toml}, and under {@code src/} a module per Java package,
 * declared in {@code lib.rs}. What {@code --translate rust} writes (CodeLaser/maddi#167).
 * <p>
 * The printer emits Rust text directly, not through the {@code OutputElement} IR the Kotlin and C# printers share:
 * a Java type becomes several Rust items ({@code struct}, {@code impl}, {@code impl Trait for}), which the
 * one-declaration shape of the {@code TypePrinter} interface does not fit, and {@code rustfmt} formats Rust better
 * than a generic formatter would.
 */
public final class RustTranslator {

    /** The edition the crate is written for. */
    public static final String EDITION = "2024";

    public record Result(List<Path> files, List<String> crashes, List<PrintMessage<RustPrintCode>> messages) {
    }

    private RustTranslator() {
    }

    /** The module text of one package's types, and the messages printing it produced. */
    public record Module(String name, String text, List<PrintMessage<RustPrintCode>> messages) {
    }

    /** The types of one package as one Rust module. {@code crashes} receives what could not be printed. */
    public static Module module(String packageName, List<TypeInfo> types, RustFacts facts, List<String> crashes) {
        return module(packageName, types, facts, crashes, RustGetters.modifiedThrough(types, facts),
                RustClosures.mutating(types, facts));
    }

    /** ... with the getters the whole program modifies through ({@link RustGetters}). */
    private static Module module(String packageName, List<TypeInfo> types, RustFacts facts, List<String> crashes,
                                 Set<MethodInfo> mutableGetters, Set<ParameterInfo> mutatingFunctions) {
        String name = RustNames.module(packageName);
        RustContext context = new RustContext(facts, name);
        context.mutableGetters = mutableGetters;
        context.mutatingFunctions = mutatingFunctions;
        StringBuilder items = new StringBuilder();
        for (TypeInfo type : types) {
            try {
                items.append("\n").append(new RustTypePrinter(context).print(type));
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                StackTraceElement[] trace = e.getStackTrace();
                crashes.add(type.fullyQualifiedName() + ": " + e + Arrays.stream(trace).limit(12)
                        .map(f -> "\n      at " + f).collect(Collectors.joining()));
            }
        }
        StringBuilder text = new StringBuilder("// Translated from the Java package `" + packageName + "` by maddi.\n");
        if (!context.uses().isEmpty()) text.append("\n");
        context.uses().forEach(u -> text.append("use ").append(u).append(";\n"));
        text.append(items);
        return new Module(name, text.toString(), context.messages());
    }

    /**
     * The crate for {@code types}, the whole program, under {@code out}: {@code Cargo.toml} and {@code src/}. Types are
     * grouped by package; a file with several primary types contributes them all to its package's module.
     */
    public static Result translate(List<TypeInfo> types, RustFacts facts, Path out) throws IOException {
        Map<String, List<TypeInfo>> byPackage = new TreeMap<>();
        for (TypeInfo type : types) {
            // package-info.java declares the package's annotations and documentation, no type
            if ("package-info".equals(type.simpleName())) continue;
            byPackage.computeIfAbsent(type.packageName(), p -> new ArrayList<>()).add(type);
        }
        Path src = out.resolve("src");
        Files.createDirectories(src);
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<PrintMessage<RustPrintCode>> messages = new ArrayList<>();
        StringBuilder lib = new StringBuilder("// The crate root: one module per Java package.\n\n");
        List<TypeInfo> all = byPackage.values().stream().flatMap(List::stream).toList();
        Set<MethodInfo> mutableGetters = RustGetters.modifiedThrough(all, facts);
        Set<ParameterInfo> mutatingFunctions = RustClosures.mutating(all, facts);
        for (Map.Entry<String, List<TypeInfo>> e : byPackage.entrySet()) {
            Module module = module(e.getKey(), e.getValue(), facts, crashes, mutableGetters, mutatingFunctions);
            messages.addAll(module.messages());
            Path file = src.resolve(module.name() + ".rs");
            Files.writeString(file, module.text());
            files.add(file.toAbsolutePath().normalize());
            lib.append("pub mod ").append(module.name()).append(";\n");
        }
        lib.append("pub mod jrt;\n");
        Path jrt = src.resolve("jrt.rs");
        Files.writeString(jrt, JRT);
        files.add(jrt.toAbsolutePath().normalize());
        Path librs = src.resolve("lib.rs");
        Files.writeString(librs, lib.toString());
        files.add(librs.toAbsolutePath().normalize());
        Files.writeString(out.resolve("Cargo.toml"), cargoToml());
        return new Result(files, crashes, messages);
    }

    /** What the translation needs of Java's runtime that {@code std} has no counterpart for. */
    static final String JRT = """
            // Java's runtime, as far as the translated crate needs it.
            #![allow(dead_code)]

            use std::any::Any;

            /// The state of an eventually immutable object before its `@Mark` (typestate).
            pub struct Before;
            /// The state of an eventually immutable object after its `@Mark`.
            pub struct After;

            /// `String.valueOf(Object)` of a `Box<dyn Any>`: the values the translation boxes, else the type's name.
            pub fn to_string(v: &Box<dyn Any>) -> String {
                let v: &dyn Any = &**v;
                if let Some(s) = v.downcast_ref::<String>() { return s.clone(); }
                if let Some(s) = v.downcast_ref::<&str>() { return s.to_string(); }
                if let Some(x) = v.downcast_ref::<i32>() { return x.to_string(); }
                if let Some(x) = v.downcast_ref::<i64>() { return x.to_string(); }
                if let Some(x) = v.downcast_ref::<f64>() { return x.to_string(); }
                if let Some(x) = v.downcast_ref::<bool>() { return x.to_string(); }
                if let Some(x) = v.downcast_ref::<char>() { return x.to_string(); }
                if let Some(x) = v.downcast_ref::<Vec<String>>() { return format!("[{}]", x.join(", ")); }
                if v.is::<Null>() { return "null".to_string(); }
                "java.lang.Object".to_string()
            }

            /// What `new Object[n]` holds before it is filled: Java's null, as a `Box<dyn Any>`.
            pub struct Null;
            pub fn null() -> Box<dyn Any> {
                Box::new(Null)
            }

            /// `String.hashCode()`: Java's polynomial over the UTF-16 units.
            pub fn string_hash_code(s: &str) -> i32 {
                s.encode_utf16().fold(0i32, |h, c| h.wrapping_mul(31).wrapping_add(c as i32))
            }
            """;

    static String cargoToml() {
        return """
                [package]
                name = "translated"
                version = "0.1.0"
                edition = "%s"

                [lib]
                path = "src/lib.rs"
                """.formatted(EDITION);
    }
}
