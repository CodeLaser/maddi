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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        String name = RustNames.module(packageName);
        RustContext context = new RustContext(facts, name);
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
        for (TypeInfo type : types) byPackage.computeIfAbsent(type.packageName(), p -> new ArrayList<>()).add(type);
        Path src = out.resolve("src");
        Files.createDirectories(src);
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<PrintMessage<RustPrintCode>> messages = new ArrayList<>();
        StringBuilder lib = new StringBuilder("// The crate root: one module per Java package.\n\n");
        for (Map.Entry<String, List<TypeInfo>> e : byPackage.entrySet()) {
            Module module = module(e.getKey(), e.getValue(), facts, crashes);
            messages.addAll(module.messages());
            Path file = src.resolve(module.name() + ".rs");
            Files.writeString(file, module.text());
            files.add(file.toAbsolutePath().normalize());
            lib.append("pub mod ").append(module.name()).append(";\n");
        }
        Path librs = src.resolve("lib.rs");
        Files.writeString(librs, lib.toString());
        files.add(librs.toAbsolutePath().normalize());
        Files.writeString(out.resolve("Cargo.toml"), cargoToml());
        return new Result(files, crashes, messages);
    }

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
