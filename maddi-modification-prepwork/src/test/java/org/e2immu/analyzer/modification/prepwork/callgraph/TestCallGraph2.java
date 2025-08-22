package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCallGraph2 extends CommonTest2 {

    @Language("java")
    String TYPE_A_B_C = """
            package a.b.c;
            import java.util.ArrayList;
            import java.util.List;
            class C { // sort of stack
                List<String> strings = new ArrayList<>();
                public void push(String s) { strings.add(s); }
                public String get() { return strings.get(strings.size() - 1); }
                public String pop() { return strings.remove(strings.size() - 1); }
                public int size() { return strings.size(); }
                public List<String> asList() { return strings; }
            }
            """;

    @Language("java")
    String TYPE_A_B_D = """
            package a.b.d;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import a.b.c.C;
            import java.util.stream.Stream;
            class D {
                static Logger LOGGER = LoggerFactory.getLogger("D");
                final C c;
                public  D(C c) { this.c = c; }
                public void push(String s) {
                    LOGGER.info("push {}", s);
                    c.push(s);
                }
                public String pop() {
                    LOGGER.info("pop {}", c.size());
                    return c.pop();
                }
                public int size(){
                    LOGGER.debug("size {}", c.size());
                    return c.size();
                }
                public Stream<String> stream() {
                    return c.asList().stream();
                }
            }
            """;

    // the implementation
    @Language("java")
    String TYPE_A_B_E1 = """
            package a.b.e;
            import a.b.c.C;
            import a.b.d.D;
            import java.sql.Connection;
            import java.sql.ResultSet;
            import java.sql.SQLException;
            import java.sql.Statement;
            import java.util.stream.Stream;
            class E1 {
                D d  = new D(new C());
            
                public int size() {
                    return d.size();
                }
                public String pop() {
                    return d.pop();
                }
                public Stream<String> stream() {
                    return d.stream();
                }
                public void fill(Connection con) throws SQLException {
                    String query = "select NAME from COFFEES";
                    try (Statement stmt = con.createStatement()) {
                      ResultSet rs = stmt.executeQuery(query);
                      while (rs.next()) {
                        String coffeeName = rs.getString("COF_NAME");
                        d.push(coffeeName);
                      }
                    } catch (SQLException e) {
                      System.err.println(e);
                    }
                }
            }
            """;

    // uses the implementation, no interface can be introduced
    @Language("java")
    String TYPE_A_B_E2 = """
            package a.b.e;
            import java.sql.Connection;
            class E2 {
                final Connection con;
                final E1 e1 = new E1();
                E2(Connection con) {
                    this.con = con;
                }
                void go() {
                    e1.fill(con);
                }
            }
            """;

    // implementation can be partially replaced by interface
    @Language("java")
    String TYPE_A_B_F1 = """
            package a.b.f;
            import a.b.e.E1;
            import java.sql.Connection;
            class F1 {
                final Connection con;
                E2(Connection con) {
                    this.con = con;
                }
                void go(E1 e1) {
                    e1.fill(con);
                }
                String get(E1 e1) {
                    return e1.pop();
                }
            }
            """;

    // implementation can be fully replaced by interface
    @Language("java")
    String TYPE_A_B_F2 = """
            package a.b.f;
            import a.b.e.E1;
            class F2 {
              String get(E1 e1) {
                    return e1.pop();
                }
            }
            """;

    @Language("java")
    String TYPE_A_B_G1 = """
            package a.b.g;
            class G1 {
                void print() {
                    System.out.println("?");
                }
            }
            """;

    // implementation can be fully replaced by interface, uses G1, F2
    @Language("java")
    String TYPE_A_B_G2 = """
            package a.b.g;
            import a.b.f.F2;
            import a.b.e.E1;
            class G2 {
                final G1 g1 = new G1();
                final F2 f2 = new F2();
                final E1 e1;
                G2(E1 e1) {
                    this.e1 = e1;
                }
                public String go() {
                    int s = e1.size();
                    return "=" + s + g1 + f2;
                }
                public String go2() {
                    String s = e1.pop();
                    return "=" + s + g1 + f2;
                }
            }
            """;

    @Test
    public void test() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.c.C", TYPE_A_B_C, "a.b.d.D", TYPE_A_B_D,
                "a.b.e.E1", TYPE_A_B_E1, "a.b.e.E2", TYPE_A_B_E2,
                "a.b.f.F1", TYPE_A_B_F1, "a.b.f.F2", TYPE_A_B_F2,
                "a.b.g.G1", TYPE_A_B_G1, "a.b.g.G2", TYPE_A_B_G2);
        R r = init(sourcesByFqn);

        /*
         D.stream() should come after a.b.c.C.strings#a.b.d.D.c
         while it comes after C.strings, it is before D.c (which is to be expected?)
         */
        assertEquals("""
                [a.b.c.C.<init>(), a.b.c.C.asList(), a.b.c.C.get(), a.b.c.C.pop(), a.b.c.C.push(String), \
                a.b.c.C.size(), a.b.e.E1.<init>(), a.b.e.E2.<init>(java.sql.Connection), \
                a.b.f.F1.<init>(java.sql.Connection), a.b.f.F2.<init>(), a.b.g.G1.<init>(), a.b.g.G1.print(), \
                a.b.c.C.strings, a.b.d.D.pop(), a.b.d.D.push(String), a.b.d.D.size(), a.b.d.D.stream(), a.b.g.G1, \
                a.b.c.C, a.b.d.D.LOGGER, a.b.e.E1.fill(java.sql.Connection), a.b.e.E1.pop(), a.b.e.E1.size(), \
                a.b.e.E1.stream(), a.b.d.D.<init>(a.b.c.C), a.b.e.E2.go(), a.b.g.G2.go(), a.b.g.G2.go2(), \
                a.b.d.D.c, a.b.e.E2.con, a.b.g.G2.g1, a.b.d.D, a.b.e.E1.d, a.b.e.E1, a.b.e.E2.e1, \
                a.b.f.F1.get(a.b.e.E1), a.b.f.F1.go(a.b.e.E1), a.b.f.F2.get(a.b.e.E1), a.b.g.G2.<init>(a.b.e.E1), \
                a.b.e.E2, a.b.f.F1.con, a.b.f.F2, a.b.g.G2.e1, a.b.f.F1, a.b.g.G2.f2, a.b.g.G2]\
                """, r.analysisOrder().toString());
    }


    @Language("java")
    String X = """
            package a.b;
            import java.util.Map;
            abstract class X {
                protected final Map<String, Object> attributes;
                X(Map<String,Object> attributes) {
                    this.attributes = attributes;
                }
                public abstract String getEmail();
            }
            """;
    @Language("java")
    String Y = """
            package a.b;
            import java.util.Map;
            public class Y extends X {
                public Y(Map<String,Object> attributes) { super(attributes); }
                @Override
                public String getEmail() {
                    return (String) attributes.get("email");
                }
            }
            """;
    @Language("java")
    String A = """
            package d.e;
            import a.b.Y;
            import java.util.Map;
            class A {
                private Y y = new Y(Map.of());
            }
            """;

    @Test
    public void test2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.X", X, "a.b.Y", Y, "d.e.A", A);
        R r = init(sourcesByFqn);

        assertEquals("""
                a.b.X->S->a.b.X.<init>(java.util.Map<String,Object>)
                a.b.X->S->a.b.X.attributes
                a.b.X->S->a.b.X.getEmail()
                a.b.X.attributes->R->a.b.X.<init>(java.util.Map<String,Object>)
                a.b.Y->H->a.b.X
                a.b.Y->S->a.b.Y.<init>(java.util.Map<String,Object>)
                a.b.Y->S->a.b.Y.getEmail()
                a.b.Y.<init>(java.util.Map<String,Object>)->S->a.b.X.<init>(java.util.Map<String,Object>)
                a.b.Y.getEmail()->R->a.b.X.attributes
                a.b.Y.getEmail()->S->a.b.X.getEmail()
                d.e.A->S->d.e.A.<init>()
                d.e.A->S->d.e.A.y
                d.e.A.y->DR->a.b.Y
                d.e.A.y->R->a.b.Y.<init>(java.util.Map<String,Object>)\
                """, r.dependencyGraph().toString("\n", ComputeCallGraph::edgeValuePrinter));
    }

    @Language("java")
    String A3 = """
            package a.b;
            class A {
                String s = B.class.getName();
            }
            """;

    @Language("java")
    String B3 = """
            package a.b;
            class B { }
            """;

    @Test
    public void test3() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.A", A3, "a.b.B", B3);
        R r = init(sourcesByFqn);

        assertEquals("""
                a.b.A->S->a.b.A.<init>()
                a.b.A->S->a.b.A.s
                a.b.A.s->R->a.b.B
                a.b.B->S->a.b.B.<init>()\
                """, r.dependencyGraph().toString("\n", ComputeCallGraph::edgeValuePrinter));
    }

    @Language("java")
    String SOURCE_X = """
            package a.b;
            class X {
                String s;  // package-private
                String getString() { return s; }
            }
            """;

    @Language("java")
    String SOURCE_Y = """
            package a.b;
            class Y {
                X x = new X();
                String getFromX() {
                    return x.s;  // accessible - same package
                }
            }
            """;

    @Test
    public void test4() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.X", SOURCE_X, "a.b.Y", SOURCE_Y);
        R r = init(sourcesByFqn);

        assertEquals("""
                a.b.X->S->a.b.X.<init>()
                a.b.X->S->a.b.X.getString()
                a.b.X->S->a.b.X.s
                a.b.X.s->R->a.b.X.getString()
                a.b.Y->S->a.b.Y.<init>()
                a.b.Y->S->a.b.Y.getFromX()
                a.b.Y->S->a.b.Y.x
                a.b.Y.getFromX()->R->a.b.X.s
                a.b.Y.x->DR->a.b.X
                a.b.Y.x->R->a.b.X.<init>()
                a.b.Y.x->R->a.b.Y.getFromX()\
                """, r.dependencyGraph().toString("\n", ComputeCallGraph::edgeValuePrinter));
    }

    @Language("java")
    String A5 = """
            package a;
            public class A {
            }
            """;

    @Language("java")
    String B5 = """
            package a;
            import b.Annotation;
            @Annotation(uses = {A.class})
            public class B {
            }
            """;

    @Language("java")
    String annotation5 = """
            package b;
            public @interface Annotation {
               Class<?>[] uses() default {};
            }
            """;

    @Test
    public void test5() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", A5, "a.B", B5, "b.Annotation", annotation5);
        R r = init(sourcesByFqn);
        assertEquals("""
                a.A->S->a.A.<init>()
                a.B->D->a.A
                a.B->D->b.Annotation
                a.B->S->a.B.<init>()
                b.Annotation->S->b.Annotation.uses()\
                """, r.dependencyGraph().toString("\n", ComputeCallGraph::edgeValuePrinter));
    }


    @Language("java")
    String A6 = """
            package a;
            
            public class A {
            }
            """;

    @Language("java")
    String B6 = """
            package b;
            
            public class A {
            }
            """;

    @Language("java")
    String C6 = """
            package c;
            import a.*;
            import b.A;
            
            public class C extends A {
            }
            """;

    @Test
    public void testImportPriority() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", A6, "b.A", B6, "c.C", C6);
        R r = init(sourcesByFqn);
        assertEquals("""
                a.A->S->a.A.<init>()
                b.A->S->b.A.<init>()
                c.C->H->b.A
                c.C->S->c.C.<init>()\
                """, r.dependencyGraph().toString("\n", ComputeCallGraph::edgeValuePrinter));
    }
}
