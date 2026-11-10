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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/** The JDK as std: comparators, a map's value modified in place, Object values, null-safe string calls. */
public class TestJavaToRustJdk extends CommonJavaToRust {

    @Language("java")
    private static final String CATALOG = """
            package a.b;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Catalog {
                private final Map<String, Integer> views = new HashMap<>();
                private final Map<String, Map<String, String>> articles = new HashMap<>();
                private final Map<String, Object> data = new HashMap<>();
                private static final Map<String, Double> RATES = Map.of("basic", 1.0, "premium", 2.5);

                public static double rate(String tier) {
                    return RATES.getOrDefault(tier, 1.0);
                }

                public List<String> top(int n) {
                    return views.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(n).map(Map.Entry::getKey).toList();
                }

                public void setTitle(String id, String title) {
                    articles.getOrDefault(id, new HashMap<>()).put("title", title);
                }

                public String describe(String key) {
                    return "value: " + data.get(key);
                }

                public boolean isExpress(String destination) {
                    return "express".equalsIgnoreCase(destination);
                }

                public void count(String key) {
                    views.merge(key, 1, Integer::sum);
                }
            }
            """;

    @DisplayName("comparator, getOrDefault in place, Object displayed, null-safe equalsIgnoreCase, a static Map.of: compiles")
    @Test
    public void catalog() throws Exception {
        List<TypeInfo> types = parse(CATALOG);
        TypeInfo catalog = types.getFirst();
        modifying(catalog, "setTitle", "count");
        nonModifying(catalog, "top", "describe", "isExpress");
        independentResult(catalog, "top");
        independentResult(catalog, "describe");
        for (String m : List.of("setTitle", "describe", "isExpress")) {
            method(catalog, m).parameters().forEach(CommonJavaToRust::borrowed);
        }
        kept(method(catalog, "count").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(catalog, "rate").parameters().getFirst());
        catalog.getFieldByName("RATES", true).analysis().set(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.TRUE);

        RustTranslator.Result result = translate(types, nullable(Set.of("destination", "describe()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "v.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());");
        contains(rust, "self.articles.get_mut(id).unwrap_or(&mut HashMap::new()).insert(");
        contains(rust, "crate::jrt::to_string(");
        contains(rust, "static CATALOG_RATES: std::sync::LazyLock<HashMap<String, f64>> = std::sync::LazyLock::new(|| "
                       + "HashMap::from([(\"basic\".to_string(), 1.0), (\"premium\".to_string(), 2.5)]));");
        contains(rust, "return CATALOG_RATES.get(tier).copied().unwrap_or(1.0);");
        contains(rust, "destination.as_ref().map_or(false, |v| \"express\".eq_ignore_ascii_case(v))");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String EVENTS = """
            package a.b;
            public class Events {
                public interface Listener {
                    void on(String event);
                }

                public interface Factory {
                    String make(String a, int n);
                }

                public static void fire(Listener listener, String event) {
                    listener.on(event);
                }

                public static void start() {
                    fire(e -> System.out.println(e), "start");
                }

                public static String made() {
                    Factory f = (a, n) -> a.repeat(n);
                    return f.make("x", 3);
                }
            }
            """;

    /** A closure implements a source interface with one method: a blanket impl, and the closure's parameters typed. */
    @DisplayName("lambdas implementing source functional interfaces: compiles")
    @Test
    public void events() throws Exception {
        List<TypeInfo> types = parse(EVENTS);
        TypeInfo events = types.getFirst();
        TypeInfo listener = events.findSubType("Listener");
        TypeInfo factory = events.findSubType("Factory");
        nonModifying(listener, "on");
        nonModifying(factory, "make");
        borrowed(method(listener, "on").parameters().getFirst());
        borrowed(method(factory, "make").parameters().getFirst());
        independentResult(factory, "make");
        method(events, "fire").parameters().forEach(CommonJavaToRust::borrowed);

        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "impl<F: Fn(&str)> EventsListener for F {");
        contains(rust, "impl<F: Fn(&str, i32) -> String> EventsFactory for F {");
        contains(rust, "Self::fire(&|e: &str| { println!(\"{}\", e); }, \"start\");");
        contains(rust, "let f = |a: &str, n: i32| a.repeat(n as usize);");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String CLASS_FILE_STREAM = """
            package a.b;
            import java.io.ByteArrayInputStream;
            import java.io.DataInputStream;
            import java.io.IOException;
            import java.io.InputStream;
            public class Cls {
                // fernflower's DataInputFullStream
                public static class Full extends DataInputStream {
                    public Full(byte[] bytes) { super(new ByteArrayInputStream(bytes)); }
                    public void discard(int n) throws IOException { Util.discardBytes(this, n); }
                }
                public static class Util {
                    public static void discardBytes(InputStream stream, int length) throws IOException {
                        if (stream.skip(length) != length) throw new IOException("premature end of stream");
                    }
                    // InterpreterUtil.readBytes
                    public static byte[] readBytes(InputStream stream, int length) throws IOException {
                        byte[] bytes = new byte[length];
                        int n = 0, off = 0;
                        while (n < length) {
                            int count = stream.read(bytes, off + n, length - n);
                            if (count < 0) throw new IOException("premature end of stream");
                            n += count;
                        }
                        return bytes;
                    }
                }
                public static class Pool {
                    // ConstantPool(DataInputStream in)
                    public static int magic(DataInputStream in) throws IOException { return in.readInt(); }
                }
                // StructClass.create: a nullable stream, unwrapped at each read
                public static int versions(Full in) throws IOException {
                    in.discard(4);
                    int minor = in.readUnsignedShort();
                    return minor + in.readUnsignedShort();
                }
                public static int header(byte[] bytes) throws IOException {
                    Full in = new Full(bytes);
                    int magic = Pool.magic(in);
                    int minor = in.readUnsignedShort();
                    in.discard(2);
                    int count = in.readUnsignedByte();
                    return magic == 0xCAFEBABE ? minor + count : -1;
                }
            }
            """;

    @DisplayName("a class extending DataInputStream reads big-endian through its base, and passes as a DataInputStream")
    @Test
    public void classFileStream() throws Exception {
        List<TypeInfo> types = parse(CLASS_FILE_STREAM);
        TypeInfo cls = types.getFirst();
        modifying(cls.findSubType("Full"), "discard");
        modified(method(cls.findSubType("Util"), "discardBytes").parameters().getFirst());
        modified(method(cls.findSubType("Pool"), "magic").parameters().getFirst());
        modified(method(cls.findSubType("Util"), "readBytes").parameters().getFirst());
        modified(method(cls, "versions").parameters().getFirst());
        nonModifying(cls, "header", "versions");
        RustTranslator.Result result = translate(types, nullable(Set.of("in")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was an empty struct, and a unit-struct DataInputStream without methods: every read unresolved
        contains(rust, "pub base: crate::jrt::DataInputStream,");
        contains(rust, "base: crate::jrt::DataInputStream::from_bytes(");
        contains(rust, "stream.read(&mut bytes, off + n, length - n)");
        contains(rust, "let minor = r#in.as_mut().unwrap().read_unsigned_short();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String BIT_SETS = """
            package a.b;
            import java.util.BitSet;
            public class Offsets {
                // fernflower's Exprent.bytecode
                public BitSet bytecode = null;
                public void addBytecodeOffsets(BitSet bytecodeOffsets) {
                    if (bytecodeOffsets != null) {
                        if (bytecode == null) bytecode = new BitSet();
                        bytecode.or(bytecodeOffsets);
                    }
                }
                public static BitSet range(int offset, int length) {
                    BitSet offsets = new BitSet();
                    offsets.set(offset);
                    offsets.set(offset, offset + length);
                    return offsets;
                }
                // ConstantPool's passes
                public static int count(int size) {
                    BitSet pass = new BitSet(size);
                    pass.set(1);
                    pass.set(size - 1);
                    int idx = 0, n = 0;
                    while ((idx = pass.nextSetBit(idx + 1)) > 0) n++;
                    return n + (pass.get(0) ? 100 : 0) + pass.length();
                }
                public static Object file(String name) { return new java.io.File(name); }
                // Exprent.fillBytecodeRange / measureBytecode: a nullable set passed on, in a loop and after it
                public java.util.List<Offsets> kids = new java.util.ArrayList<>();
                public void fillBytecodeRange(BitSet values) {
                    for (Offsets o : kids) o.fillBytecodeRange(values);
                    measure(values);
                }
                public void measure(BitSet values) {
                    if (values != null && bytecode != null) values.or(bytecode);
                }
                // a nullable field where an Option<&mut BitSet> is expected
                public void into(Offsets other) { other.fillBytecodeRange(bytecode); }
            }
            """;

    @DisplayName("java.util.BitSet as jrt.rs's: set, set(from, to), or of a nullable field, nextSetBit, length")
    @Test
    public void bitSets() throws Exception {
        List<TypeInfo> types = parse(BIT_SETS);
        TypeInfo offsets = types.getFirst();
        modifying(offsets, "addBytecodeOffsets");
        borrowed(method(offsets, "addBytecodeOffsets").parameters().getFirst());
        nonModifying(offsets, "range", "count", "file", "fillBytecodeRange", "measure");
        modifying(offsets, "into");
        exclusive(method(offsets, "fillBytecodeRange").parameters().getFirst());
        exclusive(method(offsets, "measure").parameters().getFirst());
        borrowed(method(offsets, "into").parameters().getFirst());
        RustTranslator.Result result = translate(types, nullable(Set.of("bytecode", "bytecodeOffsets", "values")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "pub bytecode: Option<crate::jrt::BitSet>,");
        contains(rust, "offsets.set_range(offset, offset + length);");
        contains(rust, "let mut pass = crate::jrt::BitSet::with_capacity(size);");
        // an unmapped JDK constructor names the opaque stand-in, as its type does
        contains(rust, "crate::jrt::File::new(");
        contains(rust, "o.fill_bytecode_range(values.as_deref_mut());");
        contains(rust, "other.fill_bytecode_range(self.bytecode.as_mut());");
        List<String> errors = rustcErrors();
        // only the opaque File's constructor, which does not exist
        assertEquals(1, errors.size(), () -> String.join("\n", errors) + "\n" + rust);
        assertTrue(errors.getFirst().contains("E0599") && errors.getFirst().contains("File"), errors::toString);
    }

    @Language("java")
    private static final String BULK = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class Bulk {
                // fernflower's ExceptionDeobfuscator: sets intersected and subtracted
                public static void intersect(Set<Integer> common, List<Integer> successors) {
                    common.retainAll(successors);
                }
                public static void subtract(Set<String> all, Set<String> stacked) {
                    all.removeAll(stacked);
                }
                public static boolean covers(Set<String> all, List<String> some) {
                    return all.containsAll(some);
                }
                public static void drop(List<Integer> list, Set<Integer> gone) {
                    list.removeAll(gone);
                }
                // ControlFlowGraph's 'HashSet<BasicBlock> setBoth = new HashSet<>(common); setBoth.retainAll(range)':
                // HashSet inherits retainAll from AbstractCollection
                public static int both(Set<Integer> common, List<Integer> range) {
                    HashSet<Integer> setBoth = new HashSet<>(common);
                    setBoth.retainAll(range);
                    return setBoth.size();
                }
            }
            """;

    @DisplayName("removeAll, retainAll and containsAll of sets and lists")
    @Test
    public void bulkOperations() throws Exception {
        List<TypeInfo> types = parse(BULK);
        TypeInfo bulk = types.getFirst();
        modifying(bulk, "intersect", "subtract", "drop");
        nonModifying(bulk, "covers", "both");
        method(bulk, "both").parameters().forEach(CommonJavaToRust::borrowed);
        exclusive(method(bulk, "intersect").parameters().get(0));
        borrowed(method(bulk, "intersect").parameters().get(1));
        exclusive(method(bulk, "subtract").parameters().get(0));
        borrowed(method(bulk, "subtract").parameters().get(1));
        method(bulk, "covers").parameters().forEach(CommonJavaToRust::borrowed);
        exclusive(method(bulk, "drop").parameters().get(0));
        borrowed(method(bulk, "drop").parameters().get(1));
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, ".retain(|__e| __c.contains(__e));");
        contains(rust, ".retain(|__e| !__c.contains(__e));");
        contains(rust, ".iter().all(|__e| ");
        contains(rust, "set_both.retain(|__e| __c.contains(__e));");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLABLE_KEY = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class Keyed {
                private final Map<String, Integer> sizes = new HashMap<>();
                private String name;
                // fernflower's getMapRootClasses().get(field.getClassname()): a key that may be null
                public Integer size() { return sizes.get(name); }
                public boolean known() { return sizes.containsKey(name); }
                public void forget() { sizes.remove(name); }
                public void set(String n) { name = n; sizes.put(n, 1); }
            }
            """;

    @DisplayName("a map looked up by a nullable key: absent for null, as Java's HashMap")
    @Test
    public void nullableKey() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_KEY);
        TypeInfo keyed = types.getFirst();
        nonModifying(keyed, "size", "known");
        modifying(keyed, "forget", "set");
        RustTranslator.Result result = translate(types, nullable(Set.of("name", "size()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "self.name.as_ref().and_then(|__k| self.sizes.get(__k))");
        contains(rust, "self.name.as_ref().is_some_and(|__k| self.sizes.contains_key(__k))");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ENUM_MAP = """
            package a.b;
            import java.util.EnumMap;
            import java.util.Map;
            public class Edges {
                public enum EdgeType { REGULAR, BREAK, CONTINUE }
                // fernflower's Statement.mapSuccEdges
                private final Map<EdgeType, Integer> counts = new EnumMap<>(EdgeType.class);
                public void count(EdgeType t) { counts.put(t, counts.getOrDefault(t, 0) + 1); }
                public Integer get(EdgeType t) { return counts.get(t); }
            }
            """;

    @DisplayName("an EnumMap: a HashMap keyed by the enum")
    @Test
    public void enumMap() throws Exception {
        List<TypeInfo> types = parse(ENUM_MAP);
        TypeInfo edges = types.getFirst();
        modifying(edges, "count");
        nonModifying(edges, "get");
        RustTranslator.Result result = translate(types, nullable(Set.of("get()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "counts: HashMap::new(),");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String OBJECT_ARGUMENT = """
            package a.b;
            public class Consts {
                private String name;
                // fernflower's new ConstExprent(VarType.VARTYPE_INT, 1, bytecode): an Object the callee only reads
                public static boolean present(Object value) { return value != null; }
                public boolean all() { return present(1) && present("s") && present(name); }
                public void setName(String n) { name = n; }
            }
            """;

    @DisplayName("a value passed where a borrowed Object is expected: a box of its own, borrowed")
    @Test
    public void objectArgument() throws Exception {
        List<TypeInfo> types = parse(OBJECT_ARGUMENT);
        TypeInfo consts = types.getFirst();
        nonModifying(consts, "present", "all");
        modifying(consts, "setName");
        borrowed(method(consts, "present").parameters().getFirst());
        RustTranslator.Result result = translate(types, nullable(Set.of("value", "name", "n")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "Self::present(Some(&(Box::new(1) as Box<dyn Any>)))");
        contains(rust, ".map(|v| Box::new(v) as Box<dyn Any>).as_ref()");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLABLE_ELEMENT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class Visited {
                private final Set<String> seen = new HashSet<>();
                private final List<String> order = new ArrayList<>();
                private String current;
                // fernflower's '!setVisited.contains(succ)', succ nullable
                public boolean known() { return seen.contains(current) || order.contains(current); }
                public void forget() { seen.remove(current); }
                public void visit(String s) { current = s; seen.add(s); order.add(s); }
            }
            """;

    @DisplayName("a set or list asked about a nullable element: false for null")
    @Test
    public void nullableElement() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_ELEMENT);
        TypeInfo visited = types.getFirst();
        nonModifying(visited, "known");
        modifying(visited, "forget", "visit");
        RustTranslator.Result result = translate(types, nullable(Set.of("current")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "self.current.as_ref().is_some_and(|__k| self.seen.contains(__k))");
        contains(rust, "self.current.as_ref().is_some_and(|__k| self.seen.remove(__k))");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
