package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Type-level verdicts on Kotlin's type shapes: an immutable value class, a mutable counter, a collection holder, a
 generic box, a sealed hierarchy, an open class with an override, an inner class, `lateinit`, an `object`, an enum,
 an interface with a default method, a data class, and companion state. Each Java twin is what kotlinc emits: the
 accessors, the INSTANCE field, the permitted subclasses.
 */
public class TestKotlinAnalyzerTypes extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class P(val x: Int, val name: String)
            class Counter { var count = 0; fun inc() { count++ } }
            class Holder { private val list = ArrayList<String>(); fun add(s: String) { list.add(s) }; fun get(i: Int): String = list[i]; fun size(): Int = list.size }
            class Box<T>(val t: T) { fun get(): T = t }
            sealed class Shape { abstract fun area(): Double }
            class Circle(val r: Double) : Shape() { override fun area(): Double = 3.14 * r * r }
            class Sq(val side: Double) : Shape() { override fun area(): Double = side * side }
            open class Base(protected val sb: StringBuilder) { open fun touch() { } }
            class Derived(sb: StringBuilder) : Base(sb) { override fun touch() { sb.append("x") } }
            class Outer { private val log = ArrayList<String>(); inner class In { fun add(s: String) { log.add(s) } } }
            class Late { lateinit var sb: StringBuilder; fun init(s: StringBuilder) { sb = s } }
            object Reg { private val all = ArrayList<String>(); fun register(s: String) { all.add(s) }; fun count(): Int = all.size }
            enum class Color(val rgb: Int) { RED(1), GREEN(2) }
            interface Greeter { fun name(): String; fun greet(): String = "hi " + name() }
            class En : Greeter { override fun name() = "en" }
            data class Items(val name: String, val items: List<String>)
            class Ids { companion object { private var next = 0; fun nextId(): Int = next++ } }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            final class P { private final int x; private final String name; P(int x, String name) { this.x = x; this.name = name; } int getX() { return x; } String getName() { return name; } }
            final class Counter { private int count; int getCount() { return count; } void setCount(int count) { this.count = count; } void inc() { count++; } }
            final class Holder { private final List<String> list = new ArrayList<>(); void add(String s) { list.add(s); } String get(int i) { return list.get(i); } int size() { return list.size(); } }
            final class Box<T> { private final T t; Box(T t) { this.t = t; } T getT() { return t; } T get() { return t; } }
            abstract sealed class Shape permits Circle, Sq { abstract double area(); }
            final class Circle extends Shape { private final double r; Circle(double r) { this.r = r; } double getR() { return r; } double area() { return 3.14 * r * r; } }
            final class Sq extends Shape { private final double side; Sq(double side) { this.side = side; } double getSide() { return side; } double area() { return side * side; } }
            class Base { protected final StringBuilder sb; Base(StringBuilder sb) { this.sb = sb; } protected StringBuilder getSb() { return sb; } void touch() { } }
            final class Derived extends Base { Derived(StringBuilder sb) { super(sb); } void touch() { sb.append("x"); } }
            final class Outer { private final List<String> log = new ArrayList<>(); final class In { void add(String s) { log.add(s); } } }
            final class Late { private StringBuilder sb; StringBuilder getSb() { return sb; } void setSb(StringBuilder sb) { this.sb = sb; } void init(StringBuilder s) { sb = s; } }
            final class Reg { static final Reg INSTANCE = new Reg(); private final List<String> all = new ArrayList<>(); private Reg() { } void register(String s) { all.add(s); } int count() { return all.size(); } }
            enum Color { RED(1), GREEN(2); private final int rgb; Color(int rgb) { this.rgb = rgb; } int getRgb() { return rgb; } }
            interface Greeter { String name(); default String greet() { return "hi " + name(); } }
            final class En implements Greeter { public String name() { return "en"; } }
            final class Items { private final String name; private final List<String> items; Items(String name, List<String> items) { this.name = name; this.items = items; } String getName() { return name; } List<String> getItems() { return items; } Items copy(String name, List<String> items) { return new Items(name, items); } }
            final class Ids { static final class Companion { private Companion() { } int nextId() { return next++; } } private static int next = 0; static final Companion Companion = new Companion(); }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    @Test
    public void immutableValue() {
        a.assertSameAsJava("P");
        assertEquals("type P: @Immutable @Independent", a.kotlin("P").lines().findFirst().orElseThrow());
    }

    @Test
    public void mutable() {
        a.assertSameAsJava("Counter");
        a.assertSameAsJava("Late");
        assertEquals("type Counter: @Mutable @Independent", a.kotlin("Counter").lines().findFirst().orElseThrow());
    }

    @Test
    public void collectionHolder() {
        a.assertSameAsJava("Holder");
        assertEquals("type Holder: @FinalFields @Independent", a.kotlin("Holder").lines().findFirst().orElseThrow());
    }

    @Test
    public void generic() {
        a.assertSameAsJava("Box");
        assertEquals("type Box: @Immutable(hc=true) @Independent(hc=true)", a.kotlin("Box").lines().findFirst().orElseThrow());
    }

    /* a sealed class is closed: @Immutable, as a Java `sealed … permits` twin; an open abstract class would be hc */
    @Test
    public void sealedHierarchy() {
        a.assertSameAsJava("Shape");
        a.assertSameAsJava("Circle");
        a.assertSameAsJava("Sq");
    }

    @Test
    public void inheritance() {
        a.assertSameAsJava("Base");
        a.assertSameAsJava("Derived");
        a.assertSameAsJava("Greeter");
        a.assertSameAsJava("En");
    }

    @Test
    public void innerClass() {
        a.assertSameAsJava("Outer");
        a.assertSameAsJava("Outer.In");
    }

    /* an `object` is a class with an INSTANCE field, as kotlinc emits it */
    @Test
    public void objectSingleton() {
        a.assertSameAsJava("Reg");
    }

    /* Kotlin's enum also has name(), getEntries() and a static initializer; compared on what both declare */
    @Test
    public void enumClass() {
        a.assertSameAsJava("Color", "rgb", "RED", "GREEN", "<init>", "getRgb", "values", "valueOf");
    }

    /* the data class, on what the Java twin declares (componentN, equals, hashCode, toString are Kotlin-only) */
    @Test
    public void dataClass() {
        a.assertSameAsJava("Items", "name", "items", "<init>", "getName", "getItems", "copy");
    }

    /*
     ⛔ maddi#73: kotlinc puts the companion's `private var next` on Ids as a static field; K2 puts it on the
     Companion, as an instance field. Mutability moves from the class to its companion, and nextId becomes modifying.
     */
    @Test
    public void companionState() {
        assertEquals("type Ids: @Mutable @Independent", a.java("Ids").lines().findFirst().orElseThrow());
        assertEquals("type Ids: @FinalFields @Independent", a.kotlin("Ids").lines().findFirst().orElseThrow());
        assertEquals("""
                type Companion: @Immutable @Independent
                constructor: nonModifying=true @Independent
                method nextId: nonModifying=true @Independent""", a.java("Ids.Companion"));
        assertEquals("""
                type Companion: @Mutable @Independent
                constructor: nonModifying=true @Independent
                field next: final=false unmodified=true @Independent
                method nextId: nonModifying=false @Independent
                method setNext: nonModifying=false ? | 0: unmodified=true ?""", a.kotlin("Ids.Companion"));
    }
}
