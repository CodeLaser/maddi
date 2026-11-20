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
        RustShared outerShared = RustShared.enter(RustShared.of(all, facts));
        try {
            return translate(byPackage, all, facts, out, src, lib);
        } finally {
            RustShared.restore(outerShared);
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

            /// `String.format` with a format known only at run time: `%s` and `%d` take the (displayed) arguments in
            /// order, `%n` is a line break, `%%` a percent sign; any other conversion is copied as it is.
            pub fn java_format(format: &str, args: &[String]) -> String {
                let mut out = String::new();
                let mut next = args.iter();
                let mut chars = format.chars();
                while let Some(c) = chars.next() {
                    if c != '%' { out.push(c); continue; }
                    match chars.next() {
                        Some('s') | Some('d') => out.push_str(next.next().map(|s| s.as_str()).unwrap_or("")),
                        Some('n') => out.push('\\n'),
                        Some('%') => out.push('%'),
                        Some(other) => { out.push('%'); out.push(other); }
                        None => out.push('%'),
                    }
                }
                out
            }

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

            /// `java.util.BitSet`: Java's semantics, the words growing as bits are set.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct BitSet {
                words: Vec<u64>,
            }

            impl BitSet {
                pub fn new() -> Self { BitSet { words: Vec::new() } }
                pub fn with_capacity(nbits: i32) -> Self {
                    BitSet { words: Vec::with_capacity(((nbits.max(0) as usize) + 63) / 64) }
                }
                fn index(i: i32) -> usize {
                    if i < 0 { panic!("IndexOutOfBoundsException: {}", i); }
                    i as usize
                }
                pub fn set(&mut self, i: i32) {
                    let i = Self::index(i);
                    if i / 64 >= self.words.len() { self.words.resize(i / 64 + 1, 0); }
                    self.words[i / 64] |= 1u64 << (i % 64);
                }
                pub fn set_to(&mut self, i: i32, value: bool) {
                    if value { self.set(i) } else { self.clear(i) }
                }
                /// `set(from, to)`: from inclusive, to exclusive
                pub fn set_range(&mut self, from: i32, to: i32) {
                    for i in from..to { self.set(i); }
                }
                pub fn clear(&mut self, i: i32) {
                    let i = Self::index(i);
                    if let Some(w) = self.words.get_mut(i / 64) { *w &= !(1u64 << (i % 64)); }
                    self.trim();
                }
                pub fn clear_range(&mut self, from: i32, to: i32) {
                    for i in from..to { self.clear(i); }
                }
                pub fn clear_all(&mut self) { self.words.clear(); }
                pub fn flip(&mut self, i: i32) {
                    if self.get(i) { self.clear(i) } else { self.set(i) }
                }
                pub fn get(&self, i: i32) -> bool {
                    let i = Self::index(i);
                    self.words.get(i / 64).is_some_and(|w| w & (1u64 << (i % 64)) != 0)
                }
                /// the index of the first set bit at or after `from`; -1 when there is none
                pub fn next_set_bit(&self, from: i32) -> i32 {
                    let mut i = Self::index(from);
                    while i / 64 < self.words.len() {
                        let w = self.words[i / 64] >> (i % 64);
                        if w != 0 { return (i + w.trailing_zeros() as usize) as i32; }
                        i = (i / 64 + 1) * 64;
                    }
                    -1
                }
                pub fn next_clear_bit(&self, from: i32) -> i32 {
                    let mut i = Self::index(from);
                    while self.get(i as i32) { i += 1; }
                    i as i32
                }
                /// one more than the highest set bit; 0 when none is set
                pub fn length(&self) -> i32 {
                    match self.words.iter().rposition(|w| *w != 0) {
                        Some(k) => (k * 64 + 64 - self.words[k].leading_zeros() as usize) as i32,
                        None => 0,
                    }
                }
                /// the bits in use: a multiple of 64
                pub fn size(&self) -> i32 { (self.words.capacity().max(self.words.len()) * 64) as i32 }
                pub fn cardinality(&self) -> i32 { self.words.iter().map(|w| w.count_ones() as i32).sum() }
                pub fn is_empty(&self) -> bool { self.words.iter().all(|w| *w == 0) }
                pub fn or(&mut self, other: &BitSet) {
                    if other.words.len() > self.words.len() { self.words.resize(other.words.len(), 0); }
                    for (w, o) in self.words.iter_mut().zip(&other.words) { *w |= o; }
                }
                pub fn and(&mut self, other: &BitSet) {
                    for (k, w) in self.words.iter_mut().enumerate() { *w &= other.words.get(k).copied().unwrap_or(0); }
                    self.trim();
                }
                pub fn and_not(&mut self, other: &BitSet) {
                    for (w, o) in self.words.iter_mut().zip(&other.words) { *w &= !o; }
                    self.trim();
                }
                pub fn xor(&mut self, other: &BitSet) {
                    if other.words.len() > self.words.len() { self.words.resize(other.words.len(), 0); }
                    for (w, o) in self.words.iter_mut().zip(&other.words) { *w ^= o; }
                    self.trim();
                }
                pub fn intersects(&self, other: &BitSet) -> bool {
                    self.words.iter().zip(&other.words).any(|(w, o)| w & o != 0)
                }
                /// no trailing zero words: Java's equals compares the set bits, which derived PartialEq then does
                fn trim(&mut self) {
                    while self.words.last() == Some(&0) { self.words.pop(); }
                }
            }

            /// Java's reference to an object that more than one holder keeps (rust-printing.md, "Shared objects"): a
            /// clone shares the object, `==` and hashing are identity. It dereferences to the object as Java does,
            /// without Rust's aliasing check between handles: two handles may hand out a `&mut` to the same object,
            /// Java's aliasing, which Rust calls undefined behaviour. The borrow checker still judges each handle.
            pub struct Shared<T>(std::rc::Rc<std::cell::UnsafeCell<T>>);

            impl<T> Shared<T> {
                pub fn new(value: T) -> Self { Shared(std::rc::Rc::new(std::cell::UnsafeCell::new(value))) }
                /// `this` as a reference to keep: the handle of the object `value` is, which must live in a `Shared`
                /// (not yet inside its own constructor).
                pub fn of(value: &T) -> Self {
                    let ptr = value as *const T as *const std::cell::UnsafeCell<T>;
                    unsafe {
                        std::rc::Rc::increment_strong_count(ptr);
                        Shared(std::rc::Rc::from_raw(ptr))
                    }
                }
                /// Java's constructor that hands out `this` (rust-printing.md, "Back-links"): the object is built
                /// in its handle, given a weak handle of itself to hand out.
                pub fn new_cyclic(f: impl FnOnce(&WeakShared<T>) -> T) -> Self {
                    Shared(std::rc::Rc::new_cyclic(|w| std::cell::UnsafeCell::new(f(&WeakShared(w.clone())))))
                }
                /// A weak handle of the object, as a back-link holds it: `WeakShared(s.weak())` coerces to the
                /// back-link's trait object.
                pub fn weak(&self) -> std::rc::Weak<std::cell::UnsafeCell<T>> { std::rc::Rc::downgrade(&self.0) }
                /// Java's `==`: the same object.
                pub fn is(a: &Shared<T>, b: &Shared<T>) -> bool { std::rc::Rc::ptr_eq(&a.0, &b.0) }
                /// Java's `a.equals(b)`: the objects' own equality.
                pub fn equals(a: &Shared<T>, b: &Shared<T>) -> bool where T: PartialEq { **a == **b }
            }

            impl<T> Clone for Shared<T> {
                fn clone(&self) -> Self { Shared(self.0.clone()) }
            }

            impl<T> std::ops::Deref for Shared<T> {
                type Target = T;
                fn deref(&self) -> &T { unsafe { &*self.0.get() } }
            }

            impl<T> std::ops::DerefMut for Shared<T> {
                fn deref_mut(&mut self) -> &mut T { unsafe { &mut *self.0.get() } }
            }

            impl<T> PartialEq for Shared<T> {
                fn eq(&self, other: &Self) -> bool { std::rc::Rc::ptr_eq(&self.0, &other.0) }
            }

            impl<T> Eq for Shared<T> {}

            impl<T> std::hash::Hash for Shared<T> {
                fn hash<H: std::hash::Hasher>(&self, state: &mut H) { std::rc::Rc::as_ptr(&self.0).hash(state) }
            }

            impl<T> std::fmt::Debug for Shared<T> {
                fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                    write!(f, "Shared@{:p}", std::rc::Rc::as_ptr(&self.0))
                }
            }

            /// A back-link: Java's reference to an object handed out by its own constructor (`this`), held
            /// without keeping the object alive, as the object holds its holder. It dereferences to the object
            /// while that lives, and panics as Java's NullPointerException would once it is gone.
            pub struct WeakShared<T: ?Sized>(pub std::rc::Weak<std::cell::UnsafeCell<T>>);

            impl<T: ?Sized> Clone for WeakShared<T> {
                fn clone(&self) -> Self { WeakShared(self.0.clone()) }
            }

            impl<T: ?Sized> std::ops::Deref for WeakShared<T> {
                type Target = T;
                fn deref(&self) -> &T {
                    assert!(self.0.strong_count() > 0, "NullPointerException: a back-link to an object that is gone");
                    unsafe { &*(*self.0.as_ptr()).get() }
                }
            }

            impl<T: ?Sized> std::ops::DerefMut for WeakShared<T> {
                fn deref_mut(&mut self) -> &mut T {
                    assert!(self.0.strong_count() > 0, "NullPointerException: a back-link to an object that is gone");
                    unsafe { &mut *(*self.0.as_ptr()).get() }
                }
            }

            impl<T: ?Sized> PartialEq for WeakShared<T> {
                fn eq(&self, other: &Self) -> bool { std::rc::Weak::ptr_eq(&self.0, &other.0) }
            }

            impl<T: ?Sized> std::fmt::Debug for WeakShared<T> {
                fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                    write!(f, "WeakShared@{:p}", self.0.as_ptr() as *const u8)
                }
            }

            /// Java's `==` on two shared objects either of which may be null.
            pub fn same_shared<T>(a: Option<&Shared<T>>, b: Option<&Shared<T>>) -> bool {
                match (a, b) {
                    (Some(x), Some(y)) => Shared::is(x, y),
                    (None, None) => true,
                    _ => false,
                }
            }

            /// Java's `Objects.equals` of two shared objects either of which may be null: their own equality.
            pub fn equals_shared<T: PartialEq>(a: Option<&Shared<T>>, b: Option<&Shared<T>>) -> bool {
                match (a, b) {
                    (Some(x), Some(y)) => Shared::equals(x, y),
                    (None, None) => true,
                    _ => false,
                }
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

            /// A copy of a Java `Object` held as `Box<dyn Any>`. Java shares the reference; for an immutable value (a
            /// string, a boxed number) a copy cannot be told from it. Anything else is not copied: a panic.
            pub fn clone_any(v: &Box<dyn std::any::Any>) -> Box<dyn std::any::Any> {
                macro_rules! copy {
                    ($($t:ty),*) => { $( if let Some(x) = v.downcast_ref::<$t>() { return Box::new(x.clone()); } )* };
                }
                copy!(String, i32, i64, i16, i8, f64, f32, bool, char);
                panic!("a Java Object that is not an immutable value cannot be copied")
            }

            /// `System.getProperty(key)`: the JVM's own ones this process has a counterpart for, any other from the
            /// environment variable of that name; null when unset.
            pub fn system_property(key: &str) -> Option<String> {
                match key {
                    "user.dir" => std::env::current_dir().ok().map(|d| d.to_string_lossy().into_owned()),
                    "java.io.tmpdir" => Some(std::env::temp_dir().to_string_lossy().into_owned()),
                    "line.separator" => Some("\\n".to_string()),
                    "file.separator" => Some(std::path::MAIN_SEPARATOR.to_string()),
                    _ => std::env::var(key).ok(),
                }
            }

            /// `java.io.PrintStream`: `System.out` or `System.err`, held or passed on.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct PrintStream {
                err: bool,
            }

            impl PrintStream {
                pub fn stdout() -> PrintStream { PrintStream { err: false } }
                pub fn stderr() -> PrintStream { PrintStream { err: true } }
                pub fn println(&self, s: &str) { if self.err { eprintln!("{}", s) } else { println!("{}", s) } }
                pub fn print(&self, s: &str) { if self.err { eprint!("{}", s) } else { print!("{}", s) } }
                pub fn flush(&self) {}
            }

            /// What Java passes as a file name or a path: a string, a `Path` or a `File`, owned, borrowed or nullable (a null
            /// one is Java's NullPointerException, a panic).
            pub trait JPath {
                fn jpath(self) -> std::path::PathBuf;
            }
            impl JPath for &str { fn jpath(self) -> std::path::PathBuf { std::path::PathBuf::from(self) } }
            impl JPath for String { fn jpath(self) -> std::path::PathBuf { std::path::PathBuf::from(self) } }
            impl JPath for &String { fn jpath(self) -> std::path::PathBuf { std::path::PathBuf::from(self) } }
            impl JPath for &&str { fn jpath(self) -> std::path::PathBuf { std::path::PathBuf::from(*self) } }
            impl JPath for Path { fn jpath(self) -> std::path::PathBuf { self.0 } }
            impl JPath for &Path { fn jpath(self) -> std::path::PathBuf { self.0.clone() } }
            impl JPath for &mut Path { fn jpath(self) -> std::path::PathBuf { self.0.clone() } }
            impl JPath for File { fn jpath(self) -> std::path::PathBuf { self.0 } }
            impl JPath for &File { fn jpath(self) -> std::path::PathBuf { self.0.clone() } }
            impl JPath for &mut File { fn jpath(self) -> std::path::PathBuf { self.0.clone() } }
            impl<T: JPath> JPath for Option<T> {
                fn jpath(self) -> std::path::PathBuf { self.expect("NullPointerException").jpath() }
            }
            impl<'x, T> JPath for &'x Option<T> where &'x T: JPath {
                fn jpath(self) -> std::path::PathBuf { self.as_ref().expect("NullPointerException").jpath() }
            }

            fn path_string(p: &std::path::Path) -> String {
                p.to_string_lossy().into_owned()
            }

            /// `java.nio.file.Path`: a path, not a file; nothing touches the file system but `to_absolute_path`.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct Path(pub std::path::PathBuf);

            impl Path {
                /// `Path.of(first)`, `Paths.get(first)`.
                pub fn of<P: JPath>(first: P) -> Path { Path(first.jpath()) }
                /// `Path.of(first, more...)`.
                pub fn of_all<P: JPath>(first: P, more: &[String]) -> Path {
                    let mut p = first.jpath();
                    for m in more { p.push(m); }
                    Path(p)
                }
                pub fn resolve<P: JPath>(&self, other: P) -> Path { Path(self.0.join(other.jpath())) }
                pub fn resolve_sibling<P: JPath>(&self, other: P) -> Path {
                    let parent = self.0.parent().map(|p| p.to_path_buf()).unwrap_or_default();
                    Path(parent.join(other.jpath()))
                }
                pub fn relativize<P: JPath>(&self, other: P) -> Path {
                    let other = other.jpath();
                    Path(other.strip_prefix(&self.0).map(|p| p.to_path_buf()).unwrap_or(other))
                }
                pub fn to_file(&self) -> File { File(self.0.clone()) }
                pub fn to_absolute_path(&self) -> Path {
                    Path(std::path::absolute(&self.0).unwrap_or_else(|_| self.0.clone()))
                }
                pub fn normalize(&self) -> Path {
                    let mut out = std::path::PathBuf::new();
                    for c in self.0.components() {
                        match c {
                            std::path::Component::CurDir => {}
                            std::path::Component::ParentDir => { if !out.pop() { out.push(".."); } }
                            other => out.push(other.as_os_str()),
                        }
                    }
                    Path(out)
                }
                pub fn get_file_name(&self) -> Option<Path> { self.0.file_name().map(|n| Path(std::path::PathBuf::from(n))) }
                pub fn get_parent(&self) -> Option<Path> {
                    self.0.parent().filter(|p| !p.as_os_str().is_empty()).map(|p| Path(p.to_path_buf()))
                }
                pub fn get_name_count(&self) -> i32 { self.0.components().count() as i32 }
                pub fn starts_with<P: JPath>(&self, other: P) -> bool { self.0.starts_with(other.jpath()) }
                pub fn ends_with<P: JPath>(&self, other: P) -> bool { self.0.ends_with(other.jpath()) }
                pub fn is_absolute(&self) -> bool { self.0.is_absolute() }
            }

            impl std::fmt::Display for Path {
                fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result { write!(f, "{}", path_string(&self.0)) }
            }

            /// `java.io.File`: a path name, which the methods resolve against the file system when they are called.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct File(pub std::path::PathBuf);

            impl File {
                /// `new File(pathname)`.
                pub fn new<P: JPath>(pathname: P) -> File { File(pathname.jpath()) }
                /// `new File(parent, child)`, the parent a `File` or a string.
                pub fn child<P: JPath, C: JPath>(parent: P, child: C) -> File { File(parent.jpath().join(child.jpath())) }
                pub fn get_name(&self) -> String { self.0.file_name().map(|n| n.to_string_lossy().into_owned()).unwrap_or_default() }
                pub fn get_path(&self) -> String { path_string(&self.0) }
                pub fn get_absolute_path(&self) -> String {
                    path_string(&std::path::absolute(&self.0).unwrap_or_else(|_| self.0.clone()))
                }
                pub fn get_absolute_file(&self) -> File { File(std::path::absolute(&self.0).unwrap_or_else(|_| self.0.clone())) }
                pub fn get_canonical_path(&self) -> String {
                    path_string(&std::fs::canonicalize(&self.0).unwrap_or_else(|_| self.0.clone()))
                }
                pub fn get_parent(&self) -> Option<String> {
                    self.0.parent().filter(|p| !p.as_os_str().is_empty()).map(path_string)
                }
                pub fn get_parent_file(&self) -> Option<File> {
                    self.0.parent().filter(|p| !p.as_os_str().is_empty()).map(|p| File(p.to_path_buf()))
                }
                pub fn to_path(&self) -> Path { Path(self.0.clone()) }
                pub fn exists(&self) -> bool { self.0.exists() }
                pub fn is_directory(&self) -> bool { self.0.is_dir() }
                pub fn is_file(&self) -> bool { self.0.is_file() }
                pub fn length(&self) -> i64 { std::fs::metadata(&self.0).map(|m| m.len() as i64).unwrap_or(0) }
                pub fn last_modified(&self) -> i64 {
                    std::fs::metadata(&self.0).and_then(|m| m.modified()).ok()
                        .and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok()).map(|d| d.as_millis() as i64).unwrap_or(0)
                }
                /// null when this is not a directory, as Java's.
                pub fn list_files(&self) -> Option<Vec<File>> {
                    let entries = std::fs::read_dir(&self.0).ok()?;
                    let mut files: Vec<File> = entries.filter_map(|e| e.ok()).map(|e| File(e.path())).collect();
                    files.sort();
                    Some(files)
                }
                pub fn list(&self) -> Option<Vec<String>> {
                    self.list_files().map(|v| v.iter().map(|f| f.get_name()).collect())
                }
                pub fn mkdirs(&self) -> bool { std::fs::create_dir_all(&self.0).is_ok() }
                pub fn mkdir(&self) -> bool { std::fs::create_dir(&self.0).is_ok() }
                pub fn delete(&self) -> bool {
                    if self.0.is_dir() { std::fs::remove_dir(&self.0).is_ok() } else { std::fs::remove_file(&self.0).is_ok() }
                }
                pub fn create_new_file(&self) -> bool {
                    std::fs::OpenOptions::new().write(true).create_new(true).open(&self.0).is_ok()
                }
                pub fn rename_to<P: JPath>(&self, dest: P) -> bool { std::fs::rename(&self.0, dest.jpath()).is_ok() }
            }

            impl std::fmt::Display for File {
                fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result { write!(f, "{}", path_string(&self.0)) }
            }

            /// `java.nio.file.Files`: an IOException is a panic, the test or the caller fails with its message.
            #[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
            pub struct Files;

            impl Files {
                pub fn create_temp_directory<P: JPath>(prefix: P) -> Path {
                    let prefix = prefix.jpath();
                    let base = std::env::temp_dir();
                    let pid = std::process::id();
                    for i in 0..10_000u32 {
                        let nanos = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH)
                            .map(|d| d.subsec_nanos()).unwrap_or(0);
                        let dir = base.join(format!("{}{}{}{}", prefix.to_string_lossy(), pid, nanos, i));
                        if std::fs::create_dir(&dir).is_ok() { return Path(dir); }
                    }
                    panic!("IOException: cannot create a temporary directory in {}", base.display())
                }
                pub fn create_directories<P: JPath>(dir: P) -> Path {
                    let dir = dir.jpath();
                    std::fs::create_dir_all(&dir).unwrap_or_else(|e| panic!("IOException: {}: {}", dir.display(), e));
                    Path(dir)
                }
                pub fn create_directory<P: JPath>(dir: P) -> Path {
                    let dir = dir.jpath();
                    std::fs::create_dir(&dir).unwrap_or_else(|e| panic!("IOException: {}: {}", dir.display(), e));
                    Path(dir)
                }
                pub fn exists<P: JPath>(p: P) -> bool { p.jpath().exists() }
                pub fn not_exists<P: JPath>(p: P) -> bool { !p.jpath().exists() }
                pub fn is_directory<P: JPath>(p: P) -> bool { p.jpath().is_dir() }
                pub fn is_regular_file<P: JPath>(p: P) -> bool { p.jpath().is_file() }
                /// UTF-8, whatever charset Java names.
                pub fn read_string<P: JPath>(p: P) -> String {
                    let p = p.jpath();
                    std::fs::read_to_string(&p).unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e))
                }
                pub fn read_all_bytes<P: JPath>(p: P) -> Vec<i8> {
                    let p = p.jpath();
                    std::fs::read(&p).unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e))
                        .into_iter().map(|b| b as i8).collect()
                }
                pub fn read_all_lines<P: JPath>(p: P) -> Vec<String> {
                    Files::read_string(p).lines().map(|l| l.to_string()).collect()
                }
                pub fn write_string<P: JPath>(p: P, content: &str) -> Path {
                    let p = p.jpath();
                    std::fs::write(&p, content).unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e));
                    Path(p)
                }
                pub fn delete<P: JPath>(p: P) {
                    let p = p.jpath();
                    let r = if p.is_dir() { std::fs::remove_dir(&p) } else { std::fs::remove_file(&p) };
                    r.unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e));
                }
                pub fn delete_if_exists<P: JPath>(p: P) -> bool {
                    let p = p.jpath();
                    if !p.exists() { return false; }
                    Files::delete(Path(p));
                    true
                }
                /// `Files.copy(source, target)` of two paths.
                pub fn copy<P: JPath, Q: JPath>(source: P, target: Q) -> Path {
                    let (s, t) = (source.jpath(), target.jpath());
                    std::fs::copy(&s, &t).unwrap_or_else(|e| panic!("IOException: {} -> {}: {}", s.display(), t.display(), e));
                    Path(t)
                }
                pub fn size<P: JPath>(p: P) -> i64 {
                    let p = p.jpath();
                    std::fs::metadata(&p).map(|m| m.len() as i64).unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e))
                }
                /// `Files.list(dir)` collected: the directory's entries, sorted.
                pub fn list<P: JPath>(dir: P) -> Vec<Path> {
                    let dir = dir.jpath();
                    let mut v: Vec<Path> = std::fs::read_dir(&dir).unwrap_or_else(|e| panic!("IOException: {}: {}", dir.display(), e))
                        .filter_map(|e| e.ok()).map(|e| Path(e.path())).collect();
                    v.sort();
                    v
                }
                /// `Files.walk(start)` collected: the start and everything below it, a directory before its contents.
                pub fn walk<P: JPath>(start: P) -> Vec<Path> {
                    fn go(p: &std::path::Path, out: &mut Vec<Path>) {
                        out.push(Path(p.to_path_buf()));
                        if p.is_dir() {
                            if let Ok(rd) = std::fs::read_dir(p) {
                                let mut kids: Vec<std::path::PathBuf> = rd.filter_map(|e| e.ok()).map(|e| e.path()).collect();
                                kids.sort();
                                for k in kids { go(&k, out); }
                            }
                        }
                    }
                    let mut out = Vec::new();
                    go(&start.jpath(), &mut out);
                    out
                }
                /// Deletes `p` and everything below it: what a `walkFileTree` with a deleting visitor does.
                pub fn delete_recursively<P: JPath>(p: P) {
                    let p = p.jpath();
                    let r = if p.is_dir() { std::fs::remove_dir_all(&p) } else { std::fs::remove_file(&p) };
                    r.unwrap_or_else(|e| panic!("IOException: {}: {}", p.display(), e));
                }
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
