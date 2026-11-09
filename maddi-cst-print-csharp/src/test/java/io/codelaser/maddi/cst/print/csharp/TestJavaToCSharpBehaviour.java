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
package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The behavioural check ({@link CommonJavaToCSharp#behaves}): each sample runs on the JVM and, translated, on .NET,
 * and both must print the same lines and end the same way. Where the other tests say what the printer writes and
 * that Roslyn accepts it, these say that the C# does what the Java did. A probe that does not yet is disabled with
 * the issue that lists it (#188), and enabled in the commit that fixes it.
 */
public class TestJavaToCSharpBehaviour extends CommonJavaToCSharp {

    @Test
    public void stringConcatenation() {
        behaves("Concat", """
                package a;
                class Concat {
                    public static void main(String[] args) {
                        Object o = null;
                        boolean b = true;
                        char c = 'x';
                        long l = 1234567890123L;
                        float f = 1.5f;
                        double d = 2.0;
                        System.out.println("o=" + o);
                        System.out.println("b=" + b + " c=" + c + " l=" + l);
                        System.out.println("f=" + f + " d=" + d);
                        System.out.println("x" + 'a' + 1);
                        System.out.println('a' + 1 + "x");
                        System.out.println(1 + 2 + "x" + 1 + 2);
                    }
                }
                """);
    }

    /**
     * A String that may be null concatenates as {@code null}, {@code (s ?? "null")}: only where the nullability
     * analysis says it may be null, without which a String is taken to be one (and prints as the empty string).
     */
    @Test
    public void nullStringConcatenation() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }

            @Override
            public NullableState nullable(ParameterInfo parameter) {
                return "label".equals(parameter.name()) ? NullableState.NULLABLE : NullableState.NONNULL;
            }
        });
        String cs = behaves("NullConcat", """
                package a;
                class NullConcat {
                    static String describe(String label, int n) { return "label=" + label + " n=" + n; }
                    public static void main(String[] args) {
                        System.out.println(describe(null, 1));
                        System.out.println(describe("x", 2));
                    }
                }
                """);
        contains(cs, "\"label=\" + (label ?? \"null\") + \" n=\" + n");
    }

    @Test
    public void doubleFormatting() {
        behaves("Doubles", """
                package a;
                class Doubles {
                    public static void main(String[] args) {
                        System.out.println(1.0);
                        System.out.println(0.1 + 0.2);
                        System.out.println(100.0 / 3);
                        System.out.println(1e10);
                        System.out.println(1e-5);
                        System.out.println(-0.0);
                        System.out.println(1.0 / 0);
                        System.out.println(0.0 / 0);
                        System.out.println(String.valueOf(3.0f));
                        System.out.println(Double.toString(12345678.9));
                    }
                }
                """);
    }

    @Test
    public void integerArithmetic() {
        behaves("Ints", """
                package a;
                class Ints {
                    public static void main(String[] args) {
                        System.out.println(-7 / 2);
                        System.out.println(-7 % 2);
                        System.out.println(Integer.MAX_VALUE + 1);
                        System.out.println(Long.MIN_VALUE - 1);
                        System.out.println(-16 >>> 28);
                        System.out.println(-16 >> 2);
                        System.out.println(1 << 33);
                        System.out.println((byte) 200);
                        System.out.println((short) 70000);
                        System.out.println((int) -2.7);
                        System.out.println((int) 3.9e10);
                        System.out.println((long) Double.NaN);
                        System.out.println((char) ('a' + 1));
                        char c = 'a';
                        c += 2;
                        System.out.println(c);
                        System.out.println(Math.floorMod(-7, 3));
                        System.out.println(Math.abs(Integer.MIN_VALUE));
                    }
                }
                """);
    }

    @Test
    public void rounding() {
        behaves("Rounding", """
                package a;
                class Rounding {
                    public static void main(String[] args) {
                        System.out.println(Math.round(2.5));
                        System.out.println(Math.round(-2.5));
                        System.out.println(Math.round(3.5));
                        System.out.println(Math.round(2.4f));
                        System.out.println(Math.floor(-1.5));
                        System.out.println(Math.ceil(-1.5));
                        System.out.println(Math.rint(2.5));
                    }
                }
                """);
    }

    @Test
    public void divisionByZero() {
        behaves("DivZero", """
                package a;
                class DivZero {
                    public static void main(String[] args) {
                        int zero = args.length;
                        System.out.println("before");
                        System.out.println(1 / zero);
                    }
                }
                """);
    }

    @Test
    public void recordEquality() {
        behaves("Records", """
                package a;
                class Records {
                    record Point(int x, int y) {}
                    public static void main(String[] args) {
                        Point p = new Point(1, 2);
                        Point q = new Point(1, 2);
                        System.out.println(p == q);
                        System.out.println(p.equals(q));
                        System.out.println(p.hashCode() == q.hashCode());
                        System.out.println(p);
                    }
                }
                """);
    }

    @Test
    public void instanceOfGeneric() {
        behaves("InstanceOf", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                class InstanceOf {
                    interface Shape<S> { String name(); }
                    record Circle<S>(String name) implements Shape<S> {}
                    record Square(String name) implements Shape<String> {}
                    public static void main(String[] args) {
                        List<Object> all = new ArrayList<>();
                        all.add(new Circle<Integer>("c"));
                        all.add(new Square("s"));
                        all.add("text");
                        for (Object o : all) {
                            if (o instanceof Shape<?> shape) System.out.println("shape " + shape.name());
                            else System.out.println("other " + o);
                        }
                    }
                }
                """);
    }

    @Test
    public void collectionsToString() {
        behaves("Printing", """
                package a;
                import java.util.ArrayList;
                import java.util.Arrays;
                import java.util.LinkedHashMap;
                import java.util.List;
                import java.util.Map;
                class Printing {
                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>(List.of(3, 1, 2));
                        System.out.println(list);
                        Map<String, Integer> map = new LinkedHashMap<>();
                        map.put("a", 1);
                        map.put("b", 2);
                        System.out.println(map);
                        int[] array = {3, 1, 2};
                        Arrays.sort(array);
                        System.out.println(Arrays.toString(array));
                        System.out.println(List.of());
                        String text = list.toString() + map.toString() + map.entrySet().iterator().next().toString();
                        System.out.println(text);
                        Object boxed = true;
                        System.out.println(boxed.toString() + Boolean.toString(false) + Double.valueOf(2).toString());
                    }
                }
                """);
    }

    @Test
    public void listRemove() {
        behaves("Remove", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                class Remove {
                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>(List.of(10, 20, 30, 1));
                        list.remove(1);
                        list.remove(Integer.valueOf(1));
                        System.out.println(list.size() + " " + list.get(0) + " " + list.get(1));
                        System.out.println(list.indexOf(30) + " " + list.contains(20));
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: compareToIgnoreCase is unmapped")
    public void stringOrdering() {
        behaves("Strings", """
                package a;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                class Strings {
                    public static void main(String[] args) {
                        List<String> words = new ArrayList<>(List.of("b", "B", "a", "A", "_", "1"));
                        Collections.sort(words);
                        System.out.println(String.join(",", words));
                        System.out.println("a".compareTo("B") > 0);
                        System.out.println("apple".compareToIgnoreCase("APPLE"));
                        System.out.println("Title".toUpperCase() + "Title".toLowerCase());
                    }
                }
                """);
    }

    @Test
    public void stringMethods() {
        behaves("StringMethods", """
                package a;
                class StringMethods {
                    public static void main(String[] args) {
                        String s = "  Hello, World  ";
                        System.out.println("[" + s.trim() + "]");
                        System.out.println(s.indexOf('o') + " " + s.lastIndexOf("o") + " " + s.indexOf("x"));
                        System.out.println(s.trim().substring(7) + "|" + s.trim().substring(0, 5));
                        System.out.println("a,b,,".split(",").length);
                        System.out.println("a1b22c".split("[0-9]+").length);
                        System.out.println("ab".repeat(3) + " " + "".isEmpty() + " " + " ".isBlank());
                        System.out.println("hello".replace('l', 'L') + " " + "a.b.c".replace(".", "-"));
                        System.out.println("abc".charAt(1) + "abc".length());
                        System.out.println(String.join("/", "x", "y", "z"));
                        System.out.println("hello".contains("ell") + " " + "hello".startsWith("he"));
                    }
                }
                """);
    }

    @Test
    public void stringFormat() {
        behaves("Format", """
                package a;
                class Format {
                    public static void main(String[] args) {
                        System.out.println(String.format("%5.2f|%d|%s|%x|%-4s|%05d", 3.14159, 42, "s", 255, "ab", 7));
                        System.out.println("%s and %s".formatted("x", null));
                        System.out.println(String.format("%b %c %%", true, 'z'));
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: Integer.toString(int, int) is unmapped")
    public void numberParsing() {
        behaves("Parsing", """
                package a;
                class Parsing {
                    public static void main(String[] args) {
                        System.out.println(Integer.parseInt("-42") + Integer.parseInt("+7"));
                        System.out.println(Integer.toString(255, 16) + " " + Integer.toHexString(255));
                        System.out.println(Integer.toBinaryString(10) + " " + Long.MAX_VALUE);
                        System.out.println(Double.parseDouble("1.5e2"));
                        System.out.println(Integer.valueOf(5).equals(5) + " " + Integer.compare(3, 7));
                        System.out.println(Integer.parseInt("x1"));
                    }
                }
                """);
    }

    @Test
    public void exceptions() {
        behaves("Exceptions", """
                package a;
                class Exceptions {
                    static int depth(int n) {
                        try {
                            if (n == 0) throw new IllegalStateException("bottom");
                            return depth(n - 1);
                        } finally {
                            System.out.println("finally " + n);
                        }
                    }
                    public static void main(String[] args) {
                        try {
                            depth(2);
                        } catch (IllegalArgumentException e) {
                            System.out.println("wrong");
                        } catch (IllegalStateException e) {
                            System.out.println("caught " + e.getMessage());
                        }
                        try {
                            String s = null;
                            s.length();
                        } catch (NullPointerException e) {
                            System.out.println("npe");
                        }
                        try {
                            Object o = "s";
                            Integer i = (Integer) o;
                        } catch (ClassCastException e) {
                            System.out.println("cce");
                        }
                        throw new UnsupportedOperationException("end");
                    }
                }
                """);
    }

    @Test
    public void enums() {
        behaves("Enums", """
                package a;
                class Enums {
                    enum Color {
                        RED("r"), GREEN("g"), BLUE("b");
                        private final String code;
                        Color(String code) { this.code = code; }
                        String code() { return code; }
                    }
                    public static void main(String[] args) {
                        for (Color c : Color.values()) {
                            System.out.println(c + " " + c.name() + " " + c.ordinal() + " " + c.code());
                        }
                        System.out.println(Color.valueOf("GREEN").compareTo(Color.RED));
                        switch (Color.BLUE) {
                            case RED -> System.out.println("red");
                            case BLUE -> System.out.println("blue");
                            default -> System.out.println("other");
                        }
                    }
                }
                """);
    }

    @Test
    public void switches() {
        behaves("Switches", """
                package a;
                class Switches {
                    static String kind(String s) {
                        switch (s) {
                            case "a":
                            case "b":
                                return "ab";
                            case "c":
                                return "c";
                            default:
                                return "?";
                        }
                    }
                    static int fall(int i) {
                        int r = 0;
                        switch (i) {
                            case 1: r += 1;
                            case 2: r += 2; break;
                            case 3: r += 3;
                            default: r += 10;
                        }
                        return r;
                    }
                    public static void main(String[] args) {
                        System.out.println(kind("a") + kind("c") + kind("z"));
                        System.out.println(fall(1) + " " + fall(2) + " " + fall(3) + " " + fall(4));
                        int x = 5;
                        String y = switch (x) { case 1, 2 -> "low"; case 5 -> { String t = "five"; yield t; } default -> "x"; };
                        System.out.println(y);
                    }
                }
                """);
    }

    @Test
    public void lambdasAndCapture() {
        behaves("Lambdas", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                import java.util.function.Function;
                import java.util.function.Supplier;
                class Lambdas {
                    public static void main(String[] args) {
                        List<Supplier<Integer>> suppliers = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            int j = i;
                            suppliers.add(() -> j * 10);
                        }
                        for (var s : suppliers) System.out.println(s.get());
                        Function<Integer, Integer> twice = x -> x * 2;
                        Function<Integer, Integer> plus = x -> x + 1;
                        System.out.println(twice.andThen(plus).apply(5) + " " + twice.compose(plus).apply(5));
                    }
                }
                """);
    }

    @Test
    public void streams() {
        behaves("Streams", """
                package a;
                import java.util.List;
                import java.util.stream.Collectors;
                class Streams {
                    public static void main(String[] args) {
                        List<String> words = List.of("pear", "apple", "fig", "apple", "kiwi");
                        System.out.println(words.stream().filter(w -> w.length() > 3).map(String::toUpperCase)
                                .collect(Collectors.joining(", ", "[", "]")));
                        System.out.println(words.stream().distinct().sorted().collect(Collectors.toList()));
                        System.out.println(words.stream().mapToInt(String::length).sum());
                        System.out.println(words.stream().filter(w -> w.startsWith("a")).count());
                        System.out.println(words.stream().reduce("", (a, b) -> a + b.charAt(0)));
                        System.out.println(words.stream().anyMatch(w -> w.equals("fig")));
                    }
                }
                """);
    }

    @Test
    public void maps() {
        behaves("Maps", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.TreeMap;
                class Maps {
                    public static void main(String[] args) {
                        Map<String, Integer> counts = new TreeMap<>();
                        for (String w : "b a c a b a".split(" ")) counts.merge(w, 1, Integer::sum);
                        for (var e : counts.entrySet()) System.out.println(e.getKey() + "=" + e.getValue());
                        System.out.println(counts.getOrDefault("z", -1) + " " + counts.get("z"));
                        System.out.println(counts.put("a", 9) + " " + counts.put("q", 1));
                        Map<Integer, List<String>> groups = new TreeMap<>();
                        groups.computeIfAbsent(1, k -> new ArrayList<>()).add("x");
                        groups.computeIfAbsent(1, k -> new ArrayList<>()).add("y");
                        System.out.println(groups.get(1).size() + " " + counts.containsKey("q"));
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: hash order versus insertion order")
    public void hashMapIterationOrder() {
        behaves("HashOrder", """
                package a;
                import java.util.HashMap;
                import java.util.HashSet;
                import java.util.Map;
                import java.util.Set;
                class HashOrder {
                    public static void main(String[] args) {
                        Map<String, Integer> map = new HashMap<>();
                        for (String k : new String[]{"banana", "apple", "cherry", "date"}) map.put(k, k.length());
                        System.out.println(String.join(",", map.keySet()));
                        Set<Integer> set = new HashSet<>();
                        for (int i : new int[]{40, 3, 17, 1}) set.add(i);
                        StringBuilder sb = new StringBuilder();
                        for (int i : set) sb.append(i).append(' ');
                        System.out.println(sb.toString().trim());
                    }
                }
                """);
    }

    @Test
    public void concurrentModification() {
        behaves("Modification", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                class Modification {
                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
                        list.removeIf(i -> i == 2);
                        System.out.println(list);
                        for (Integer i : list) {
                            if (i == 1) list.add(4);
                        }
                    }
                }
                """);
    }

    @Test
    public void initializationOrder() {
        behaves("Init", """
                package a;
                class Init {
                    static abstract class Base {
                        protected final String tag = log("base field");
                        Base() {
                            log("base constructor");
                            describe();
                        }
                        abstract void describe();
                    }
                    static final class Derived extends Base {
                        private String name = log("derived field");
                        Derived() {
                            log("derived constructor " + name);
                        }
                        void describe() { log("describe, name set: " + (name != null)); }
                    }
                    static String log(String s) {
                        System.out.println(s);
                        return s;
                    }
                    public static void main(String[] args) {
                        new Derived();
                    }
                }
                """);
    }

    @Test
    public void stringBuilder() {
        behaves("Builder", """
                package a;
                class Builder {
                    public static void main(String[] args) {
                        StringBuilder sb = new StringBuilder("hello");
                        sb.insert(0, '>').append(1).append('c').append(2.5).append(true).append((Object) null);
                        System.out.println(sb);
                        sb.setLength(6);
                        System.out.println(sb.reverse() + " " + sb.length());
                        sb.deleteCharAt(0).setCharAt(0, 'L');
                        System.out.println(sb.toString() + sb.indexOf("e"));
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: an anonymous Comparator into list.sort is rejected")
    public void innerAndAnonymous() {
        behaves("Inner", """
                package a;
                import java.util.Comparator;
                import java.util.ArrayList;
                import java.util.List;
                class Inner {
                    private int base = 10;
                    class Adder {
                        int add(int x) { return base + x; }
                    }
                    interface Greeter {
                        String name();
                        default String greet() { return "hi " + name(); }
                    }
                    public static void main(String[] args) {
                        Inner outer = new Inner();
                        Inner.Adder adder = outer.new Adder();
                        outer.base = 20;
                        System.out.println(adder.add(1));
                        Greeter g = new Greeter() {
                            public String name() { return "anon"; }
                        };
                        System.out.println(g.greet());
                        List<String> list = new ArrayList<>(List.of("ccc", "a", "bb"));
                        list.sort(new Comparator<String>() {
                            public int compare(String x, String y) { return Integer.compare(x.length(), y.length()); }
                        });
                        System.out.println(list);
                    }
                }
                """);
    }

    @Test
    public void labelledLoops() {
        behaves("Labels", """
                package a;
                class Labels {
                    public static void main(String[] args) {
                        outer:
                        for (int i = 0; i < 4; i++) {
                            for (int j = 0; j < 4; j++) {
                                if (j == 2) continue outer;
                                if (i == 3) break outer;
                                System.out.print(i + "" + j + " ");
                            }
                        }
                        System.out.println();
                        int k = 0;
                        do { k += 3; } while (k < 10);
                        System.out.println(k);
                    }
                }
                """);
    }

    @Test
    public void ternaryPromotion() {
        behaves("Ternary", """
                package a;
                class Ternary {
                    public static void main(String[] args) {
                        boolean t = args.length == 0;
                        Object o = t ? 1 : 2.0;
                        System.out.println(o);
                        Integer nothing = null;
                        Integer some = t ? nothing : Integer.valueOf(0);
                        System.out.println(some);
                        char c = 'a';
                        System.out.println(t ? c : 0);
                        System.out.println(t ? 'b' : c);
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: new int[2][3] and a qualified java.util.Arrays are rejected")
    public void arrays() {
        behaves("ArrayProbe", """
                package a;
                class ArrayProbe {
                    public static void main(String[] args) {
                        int[][] grid = new int[2][3];
                        grid[1][2] = 5;
                        System.out.println(java.util.Arrays.deepToString(grid));
                        String[] names = new String[2];
                        System.out.println(names[0] + " " + names.length);
                        long[] ls = {3, 1, 2};
                        java.util.Arrays.fill(ls, 1, 3, 7);
                        System.out.println(java.util.Arrays.toString(ls));
                        int[] copy = java.util.Arrays.copyOf(new int[]{1, 2}, 4);
                        System.out.println(java.util.Arrays.toString(copy) + java.util.Arrays.equals(copy, new int[]{1, 2, 0, 0}));
                        System.out.println(names[2]);
                    }
                }
                """);
    }

    @Test
    public void tryWithResources() {
        behaves("Resources", """
                package a;
                class Resources {
                    record Resource(String name) implements AutoCloseable {
                        Resource {
                            System.out.println("open " + name);
                        }
                        public void close() { System.out.println("close " + name); }
                    }
                    public static void main(String[] args) {
                        try (Resource a = new Resource("a"); Resource b = new Resource("b")) {
                            System.out.println("body " + a.name() + b.name());
                            throw new IllegalStateException("x");
                        } catch (IllegalStateException e) {
                            System.out.println("caught " + e.getMessage());
                        } finally {
                            System.out.println("finally");
                        }
                    }
                }
                """);
    }

    @Test
    @Disabled("#188: Optional.ofNullable(null) is rejected")
    public void optionals() {
        behaves("Optionals", """
                package a;
                import java.util.Optional;
                class Optionals {
                    public static void main(String[] args) {
                        Optional<String> some = Optional.of("x");
                        Optional<String> none = Optional.empty();
                        System.out.println(some.map(s -> s + "!").orElse("none") + " " + none.map(s -> s + "!").orElse("none"));
                        System.out.println(some.isPresent() + " " + none.isEmpty() + " " + Optional.ofNullable(null).isPresent());
                        System.out.println(none.orElseGet(() -> "lazy"));
                        System.out.println(none.get());
                    }
                }
                """);
    }

    @Test
    public void genericsAndVarargs() {
        behaves("Generics", """
                package a;
                import java.util.ArrayList;
                import java.util.List;
                class Generics {
                    static <T extends Comparable<T>> T max(List<T> list) {
                        T best = list.get(0);
                        for (T t : list) if (t.compareTo(best) > 0) best = t;
                        return best;
                    }
                    static int sum(int... xs) {
                        int s = 0;
                        for (int x : xs) s += x;
                        return s;
                    }
                    static String count(Object... xs) { return xs.length + ""; }
                    public static void main(String[] args) {
                        System.out.println(max(List.of(3, 9, 2)) + " " + max(List.of("b", "c", "a")));
                        System.out.println(sum() + " " + sum(1) + " " + sum(1, 2, 3) + " " + sum(new int[]{4, 5}));
                        System.out.println(count("a", "b") + count() + count((Object) new String[]{"x", "y"}));
                        List<? extends Number> nums = new ArrayList<>(List.of(1, 2.5));
                        double total = 0;
                        for (Number n : nums) total += n.doubleValue();
                        System.out.println(total);
                    }
                }
                """);
    }
}
