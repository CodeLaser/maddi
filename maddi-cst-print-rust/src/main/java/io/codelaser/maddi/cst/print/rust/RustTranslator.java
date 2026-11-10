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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

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
        // the hierarchies among the package's types, when no translation of the whole program is in progress
        RustHierarchies hierarchies = RustHierarchies.current() == RustHierarchies.NONE ? RustHierarchies.of(types)
                : RustHierarchies.current();
        return RustHierarchies.with(hierarchies, () -> module(packageName, types, facts, crashes,
                RustGetters.modifiedThrough(types, facts), RustClosures.mutating(types, facts)));
    }

    /** ... with the getters the whole program modifies through ({@link RustGetters}). */
    private static Module module(String packageName, List<TypeInfo> types, RustFacts facts, List<String> crashes,
                                 Set<MethodInfo> mutableGetters, Set<ParameterInfo> mutatingFunctions) {
        return module(packageName, types, facts, crashes, mutableGetters, mutatingFunctions, new TreeMap<>());
    }

    private static Module module(String packageName, List<TypeInfo> types, RustFacts facts, List<String> crashes,
                                 Set<MethodInfo> mutableGetters, Set<ParameterInfo> mutatingFunctions,
                                 Map<String, String> opaque) {
        String name = RustNames.module(packageName);
        RustContext context = new RustContext(facts, name);
        context.opaque = opaque;
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
        StringBuilder lib = new StringBuilder("// The crate root: one module per Java package.\n\n");
        List<TypeInfo> all = byPackage.values().stream().flatMap(List::stream).toList();
        RustHierarchies outer = RustHierarchies.enter(RustHierarchies.of(all));
        try {
            return translate(byPackage, all, facts, out, src, lib);
        } finally {
            RustHierarchies.restore(outer);
        }
    }

    private static Result translate(Map<String, List<TypeInfo>> byPackage, List<TypeInfo> all, RustFacts facts,
                                    Path out, Path src, StringBuilder lib) throws IOException {
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<PrintMessage<RustPrintCode>> messages = new ArrayList<>();
        Set<MethodInfo> mutableGetters = RustGetters.modifiedThrough(all, facts);
        Set<ParameterInfo> mutatingFunctions = RustClosures.mutating(all, facts);
        Map<String, String> opaque = new TreeMap<>();
        for (Map.Entry<String, List<TypeInfo>> e : byPackage.entrySet()) {
            Module module = module(e.getKey(), e.getValue(), facts, crashes, mutableGetters, mutatingFunctions, opaque);
            messages.addAll(module.messages());
            Path file = src.resolve(module.name() + ".rs");
            Files.writeString(file, module.text());
            files.add(file.toAbsolutePath().normalize());
            lib.append("pub mod ").append(module.name()).append(";\n");
        }
        lib.append("pub mod jrt;\n");
        Path jrt = src.resolve("jrt.rs");
        Files.writeString(jrt, JRT + opaqueTypes(opaque));
        files.add(jrt.toAbsolutePath().normalize());
        Path librs = src.resolve("lib.rs");
        Files.writeString(librs, lib.toString());
        files.add(librs.toAbsolutePath().normalize());
        Files.writeString(out.resolve("Cargo.toml"), cargoToml());
        return new Result(files, crashes, messages);
    }

    /** What the translation needs of Java's runtime that {@code std} has no counterpart for. */
    /**
     * The opaque stand-ins of the external types without a Rust counterpart ({@link RustContext#opaque}): unit structs
     * deriving what a holder may derive, so that a struct or record holding one compiles, and only a call into it
     * (a method, a constructor) is rustc's error, in the function that makes it.
     */
    static String opaqueTypes(Map<String, String> opaque) {
        if (opaque.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n// Opaque stand-ins for the external types the translation has no counterpart for.\n");
        opaque.forEach((fqn, name) -> sb.append("\n/// `").append(fqn).append("`, not translated.\n")
                .append("#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]\n")
                .append("pub struct ").append(name).append(";\n"));
        return sb.toString();
    }

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

            /// Java's `==` on two objects either of which may be null: the same object, or both null.
            pub fn same<T: ?Sized>(a: Option<&T>, b: Option<&T>) -> bool {
                match (a, b) {
                    (Some(x), Some(y)) => std::ptr::eq(x, y),
                    (None, None) => true,
                    _ => false,
                }
            }

            /// `String.hashCode()`: Java's polynomial over the UTF-16 units.
            pub fn string_hash_code(s: &str) -> i32 {
                s.encode_utf16().fold(0i32, |h, c| h.wrapping_mul(31).wrapping_add(c as i32))
            }

            /// `java.io.DataInputStream`, and `java.io.InputStream`, over the bytes in memory: Java's big-endian reads.
            /// Java's IOException at the end of the data is a panic, as the translation prints a `throw`.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct DataInputStream {
                bytes: Vec<i8>,
                pos: usize,
            }

            impl DataInputStream {
                /// `new DataInputStream(new ByteArrayInputStream(bytes))`, whichever way the array is held
                pub fn from_bytes(bytes: impl Bytes) -> Self {
                    DataInputStream { bytes: bytes.into_bytes(), pos: 0 }
                }
                fn next(&mut self) -> u8 {
                    let b = *self.bytes.get(self.pos).unwrap_or_else(|| panic!("EOFException"));
                    self.pos += 1;
                    b as u8
                }
                fn take<const N: usize>(&mut self) -> [u8; N] {
                    std::array::from_fn(|_| self.next())
                }
                pub fn read_unsigned_byte(&mut self) -> i32 { self.next() as i32 }
                pub fn read_byte(&mut self) -> i8 { self.next() as i8 }
                pub fn read_boolean(&mut self) -> bool { self.next() != 0 }
                pub fn read_unsigned_short(&mut self) -> i32 { u16::from_be_bytes(self.take()) as i32 }
                pub fn read_short(&mut self) -> i16 { i16::from_be_bytes(self.take()) }
                pub fn read_char(&mut self) -> char {
                    char::from_u32(u16::from_be_bytes(self.take()) as u32).unwrap_or('\\u{FFFD}')
                }
                pub fn read_int(&mut self) -> i32 { i32::from_be_bytes(self.take()) }
                pub fn read_long(&mut self) -> i64 { i64::from_be_bytes(self.take()) }
                pub fn read_float(&mut self) -> f32 { f32::from_be_bytes(self.take()) }
                pub fn read_double(&mut self) -> f64 { f64::from_be_bytes(self.take()) }
                /// Java's modified UTF-8: a length, then one to three bytes per UTF-16 unit.
                pub fn read_utf(&mut self) -> String {
                    let len = self.read_unsigned_short() as usize;
                    let end = self.pos + len;
                    let mut units = Vec::new();
                    while self.pos < end {
                        let a = self.next() as u16;
                        units.push(if a < 0x80 {
                            a
                        } else if a & 0xE0 == 0xC0 {
                            ((a & 0x1F) << 6) | (self.next() as u16 & 0x3F)
                        } else {
                            ((a & 0x0F) << 12) | ((self.next() as u16 & 0x3F) << 6) | (self.next() as u16 & 0x3F)
                        });
                    }
                    String::from_utf16_lossy(&units)
                }
                pub fn read_fully(&mut self, b: &mut [i8]) {
                    for x in b.iter_mut() { *x = self.next() as i8; }
                }
                /// `InputStream.read(b, off, len)`: what is left, up to len; -1 at the end.
                pub fn read(&mut self, b: &mut [i8], off: i32, len: i32) -> i32 {
                    let left = self.bytes.len() - self.pos;
                    if len > 0 && left == 0 { return -1; }
                    let n = (len.max(0) as usize).min(left);
                    b[off as usize..off as usize + n].copy_from_slice(&self.bytes[self.pos..self.pos + n]);
                    self.pos += n;
                    n as i32
                }
                /// Java widens an int argument to the long parameter.
                pub fn skip(&mut self, n: impl Into<i64>) -> i64 {
                    let k = (n.into().max(0) as usize).min(self.bytes.len() - self.pos);
                    self.pos += k;
                    k as i64
                }
                pub fn skip_bytes(&mut self, n: i32) -> i32 { self.skip(n) as i32 }
                pub fn available(&self) -> i32 { (self.bytes.len() - self.pos) as i32 }
                pub fn close(&mut self) {}
            }

            /// A byte[] as the translation holds it: owned, borrowed, nullable.
            pub trait Bytes {
                fn into_bytes(self) -> Vec<i8>;
            }
            impl Bytes for Vec<i8> { fn into_bytes(self) -> Vec<i8> { self } }
            impl Bytes for &Vec<i8> { fn into_bytes(self) -> Vec<i8> { self.clone() } }
            impl Bytes for &[i8] { fn into_bytes(self) -> Vec<i8> { self.to_vec() } }
            impl<B: Bytes> Bytes for Option<B> {
                fn into_bytes(self) -> Vec<i8> { self.expect("NullPointerException").into_bytes() }
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
