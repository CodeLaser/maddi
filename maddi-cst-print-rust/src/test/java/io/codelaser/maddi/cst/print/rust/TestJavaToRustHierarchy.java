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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.TypeInfo;

/** A closed class hierarchy as one struct with a kind enum (RustHierarchies). */
public class TestJavaToRustHierarchy extends CommonJavaToRust {

    @Language("java")
    private static final String EXPRS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Exprs {
                public abstract static class Expr {
                    protected int id;
                    public Expr(int id) { this.id = id; }
                    public abstract int precedence();
                    public String toJava() { return "(" + body() + ")"; }
                    protected String body() { return "?"; }
                    public int getId() { return id; }
                }
                public static class Const extends Expr {
                    private final int value;
                    public Const(int id, int value) { super(id); this.value = value; }
                    public int precedence() { return 0; }
                    protected String body() { return "" + value; }
                    public int getValue() { return value; }
                }
                public static class Var extends Expr {
                    private String name;
                    public Var(int id, String name) { super(id); this.name = name; }
                    public int precedence() { return 1; }
                    protected String body() { return name; }
                    public void rename(String n) { this.name = n; }
                }
                public static class RecordVar extends Var {
                    private int index;
                    public RecordVar(int id, String name, int index) { super(id, name); this.index = index; }
                    protected String body() { return super.body() + "#" + index; }
                }
                public static class User {
                    public int sum(List<Expr> exprs) {
                        int s = 0;
                        for (Expr e : exprs) {
                            if (e instanceof Const c) s += c.getValue();
                            else if (e instanceof Var v && v.getId() > 0) s += v.precedence();
                        }
                        return s;
                    }
                    public void renameAll(List<Expr> exprs) {
                        for (Expr e : exprs) {
                            if (e instanceof Var) ((Var) e).rename("x");
                        }
                    }
                    public int firstConst(List<Expr> exprs) {
                        Expr e = exprs.get(0);
                        if (!(e instanceof Const c)) {
                            return -1;
                        }
                        return c.getValue();
                    }
                    public boolean isNamedVar(Expr e) { return e instanceof Var v && v.getId() > 0; }
                    public boolean ordered(Expr a, Expr b) {
                        if (!(a instanceof Const c1 && b instanceof Const c2 && c1.getValue() == c2.getValue())) return false;
                        return c1.getId() < c2.getId();
                    }
                    public List<Expr> make() {
                        List<Expr> l = new ArrayList<>();
                        l.add(new Const(1, 2));
                        l.add(new RecordVar(2, "r", 3));
                        return l;
                    }
                }
            }
            """;

    @DisplayName("a closed hierarchy is one struct whose kind enum holds each class's fields; overrides dispatch")
    @Test
    public void closedHierarchy() throws Exception {
        List<TypeInfo> types = parse(EXPRS);
        TypeInfo exprs = types.getFirst();
        TypeInfo expr = exprs.findSubType("Expr");
        TypeInfo constant = exprs.findSubType("Const");
        TypeInfo var = exprs.findSubType("Var");
        TypeInfo recordVar = exprs.findSubType("RecordVar");
        TypeInfo user = exprs.findSubType("User");
        nonModifying(expr, "precedence", "toJava", "body", "getId");
        nonModifying(constant, "precedence", "body", "getValue");
        nonModifying(var, "precedence", "body");
        modifying(var, "rename");
        nonModifying(recordVar, "body");
        nonModifying(user, "sum", "make", "firstConst", "isNamedVar", "ordered");
        modifying(user, "renameAll");
        borrowed(method(user, "sum").parameters().getFirst());
        borrowed(method(user, "firstConst").parameters().getFirst());
        borrowed(method(user, "isNamedVar").parameters().getFirst());
        method(user, "ordered").parameters().forEach(CommonJavaToRust::borrowed);
        exclusive(method(user, "renameAll").parameters().getFirst());
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // each class's own fields in its variant, a subclass of a subclass with its parent's data first
        contains(rust, "ExprsRecordVar(ExprsVarData, ExprsRecordVarData),");
        // the template method calls the dispatching body(), which picks the variant's own implementation
        contains(rust, "return format!(\"({})\", self.body());");
        contains(rust, "ExprsExprKind::ExprsRecordVar(..) => self.body_exprs_record_var(),");
        contains(rust, "ExprsExprKind::ExprsExpr => unreachable!(\"abstract\"),");
        // super.body(): the parent's own implementation
        contains(rust, "format!(\"{}#{}\", self.body_exprs_var(), self.kind.exprs_record_var().index)");
        // instanceof with a pattern, in an else-if and in a let chain
        contains(rust, "if let Some(c) = Some(&*e).filter(|o| matches!(o.kind, ExprsExprKind::ExprsConst(..))) {");
        contains(rust, "matches!(o.kind, ExprsExprKind::ExprsVar(..) | ExprsExprKind::ExprsRecordVar(..))) && (v.get_id() > 0)");
        // a cast is the object itself; the subclass's construction
        contains(rust, "(e).rename(\"x\".to_string());");
        contains(rust, "l.push(ExprsExpr::new_exprs_record_var(2, \"r\".to_string(), 3));");
        // a negated pattern: the variable in scope after the statement, a let-else
        contains(rust, "let Some(c) = Some(&*e).filter(|o| matches!(o.kind, ExprsExprKind::ExprsConst(..))) else {");
        // ... with a further condition: an if over it, the block repeated
        contains(rust, "if !(c1.get_value() == c2.get_value()) {");
        // a pattern in a value: is_some_and, the variable the closure's parameter
        contains(rust, ".is_some_and(|v| (v.get_id() > 0))");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ATTRIBUTES = """
            package a.b;
            public class Attrs {
                public interface Matchable { boolean matches(String s); }
                public static class Attr implements Matchable {
                    public static final int KIND_PLAIN = 0;
                    protected String name;
                    public Attr(String name) { this.name = name; }
                    public static Attr create(String name, int code) {
                        if (code == Code.KIND_CODE) return new Code(name, 7);
                        return new Attr(name);
                    }
                    public boolean matches(String s) { return name.equals(s); }
                    public String toString() { return name; }
                    public int size() { return 0; }
                }
                public static class Code extends Attr {
                    public static final int KIND_CODE = 1;
                    private int length;
                    public Code(String name, int length) { super(name); this.length = length; }
                    public boolean matches(String s) { return super.matches(s) || s.isEmpty(); }
                    public String toString() { return name + ":" + length; }
                    public int size() { return length; }
                    public boolean sameLength(Code other) { return other.length == length; }
                    public void grow() { length++; }
                }
            }
            """;

    @DisplayName("a concrete root, an interface, toString, another object's field and constants of a member")
    @Test
    public void concreteRoot() throws Exception {
        List<TypeInfo> types = parse(ATTRIBUTES);
        TypeInfo attrs = types.getFirst();
        TypeInfo attr = attrs.findSubType("Attr");
        TypeInfo code = attrs.findSubType("Code");
        nonModifying(attrs.findSubType("Matchable"), "matches");
        nonModifying(attr, "create", "matches", "toString", "size");
        nonModifying(code, "matches", "toString", "size", "sameLength");
        modifying(code, "grow");
        borrowed(method(code, "sameLength").parameters().getFirst());
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "AttrsAttr,\n    AttrsCode(AttrsCodeData),");
        contains(rust, "if code == AttrsAttr::ATTRS_CODE_KIND_CODE {");
        contains(rust, "return self.matches_attrs_attr(s.clone()) || s.is_empty();");
        contains(rust, "return other.kind.attrs_code().length == self.kind.attrs_code().length;");
        contains(rust, "self.kind.attrs_code_mut().length += 1;");
        contains(rust, "impl AttrsMatchable for AttrsAttr {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("an override that modifies where the family's top method does not: rustc's error in the dispatcher")
    @Test
    public void overrideThatModifies() throws Exception {
        List<TypeInfo> types = parse(EXPRS);
        TypeInfo exprs = types.getFirst();
        TypeInfo expr = exprs.findSubType("Expr");
        TypeInfo var = exprs.findSubType("Var");
        nonModifying(expr, "precedence", "toJava", "body", "getId");
        nonModifying(exprs.findSubType("Const"), "precedence", "body", "getValue");
        nonModifying(exprs.findSubType("RecordVar"), "body");
        // a wrong verdict, as the engine could compute it: Var's body modifies
        modifying(var, "body", "rename");
        nonModifying(var, "precedence");
        nonModifying(exprs.findSubType("User"), "sum", "make");
        modifying(exprs.findSubType("User"), "renameAll");
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String NESTED = """
            package a.b;
            public class Nodes {
                public abstract static class Node {
                    public abstract int eval();
                }
                public static class Leaf extends Node {
                    private final int v;
                    public Leaf(int v) { this.v = v; }
                    public int eval() { return v; }
                }
                public static class Neg extends Node {
                    private final Node inner;
                    public Neg(Node inner) { this.inner = inner; }
                    public int eval() { return -inner.eval(); }
                }
            }
            """;

    @DisplayName("a member holding an object of its own hierarchy is boxed: the root's struct would be infinite")
    @Test
    public void recursiveMember() throws Exception {
        List<TypeInfo> types = parse(NESTED);
        TypeInfo nodes = types.getFirst();
        nonModifying(nodes.findSubType("Node"), "eval");
        nonModifying(nodes.findSubType("Leaf"), "eval");
        nonModifying(nodes.findSubType("Neg"), "eval");
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was 'inner: NodesNode': E0072, the whole hierarchy removed (fernflower's ArrayExprent)
        contains(rust, "pub inner: Box<NodesNode>,");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String MATCHABLES = """
            package a.b;
            import java.util.List;
            public class Ms {
                public interface Matchable { int weight(); }
                public abstract static class Stat implements Matchable {
                    public int weight() { return 1; }
                }
                public static class If extends Stat {
                    private int depth;
                    public int getDepth() { return depth; }
                    public void deepen() { depth++; }
                }
                public static class Seq extends Stat {
                }
                public static class Expr implements Matchable {
                    private final String text;
                    public Expr(String text) { this.text = text; }
                    public int weight() { return text.length(); }
                    public String getText() { return text; }
                }
                public static class User {
                    public int count(List<Matchable> objs) {
                        int n = 0;
                        for (Matchable m : objs) {
                            if (m instanceof If i) n += i.getDepth();
                            else if (m instanceof Expr) n += ((Expr) m).getText().length();
                            else if (m instanceof Stat) n++;
                        }
                        return n;
                    }
                    public void deepenAll(List<Matchable> objs) {
                        for (Matchable m : objs) {
                            if (m instanceof If i) i.deepen();
                        }
                    }
                }
            }
            """;

    @DisplayName("a value of an interface tested and cast to a class: the trait's conversions, a kind test in a hierarchy")
    @Test
    public void interfaceDowncasts() throws Exception {
        List<TypeInfo> types = parse(MATCHABLES);
        TypeInfo ms = types.getFirst();
        nonModifying(ms.findSubType("Matchable"), "weight");
        nonModifying(ms.findSubType("Stat"), "weight");
        nonModifying(ms.findSubType("If"), "getDepth");
        modifying(ms.findSubType("If"), "deepen");
        nonModifying(ms.findSubType("Expr"), "weight", "getText");
        TypeInfo user = ms.findSubType("User");
        nonModifying(user, "count");
        modifying(user, "deepenAll");
        borrowed(method(user, "count").parameters().getFirst());
        exclusive(method(user, "deepenAll").parameters().getFirst());
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // the trait's conversions, None by default; the struct's impl Some(self)
        contains(rust, "fn as_ms_stat(&self) -> Option<&MsStat> {\n        None\n    }");
        contains(rust, "fn as_ms_stat(&self) -> Option<&MsStat> {\n        Some(self)\n    }");
        // a member below the hierarchy's root: the conversion to the root, its kind tested
        contains(rust, "if let Some(i) = m.as_ms_stat().filter(|o| matches!(o.kind, MsStatKind::MsIf(..))) {");
        contains(rust, "} else if m.as_ms_expr().is_some() {");
        contains(rust, "(m.as_ms_expr().unwrap()).get_text()");
        // the pattern variable modified: the exclusive conversion, the loop over the list mutable
        contains(rust, "for m in objs.iter_mut() {");
        contains(rust, "m.as_ms_stat_mut().filter(");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String IDENTITY = """
            package a.b;
            import java.util.List;
            public class Ids {
                public static class Node {
                    private final int v;
                    public Node(int v) { this.v = v; }
                }
                public static boolean same(Node a, Node maybe) { return a == maybe; }
                public static void drop(List<Node> nodes, Node e) { nodes.remove(e); }
                public static boolean has(List<Node> nodes, Node n) { return nodes.contains(n) && nodes.indexOf(n) >= 0; }
            }
            """;

    @DisplayName("== on objects and a list's lookups are identity, also with a null on one side")
    @Test
    public void identity() throws Exception {
        List<TypeInfo> types = parse(IDENTITY);
        TypeInfo ids = types.getFirst();
        nonModifying(ids, "same", "has");
        modifying(ids, "drop");
        method(ids, "same").parameters().forEach(CommonJavaToRust::borrowed);
        borrowed(method(ids, "has").parameters().get(0));
        borrowed(method(ids, "has").parameters().get(1));
        exclusive(method(ids, "drop").parameters().get(0));
        borrowed(method(ids, "drop").parameters().get(1));
        translate(types, nullable(java.util.Set.of("maybe")));
        String rust = module("a.b");
        // were '==' (no PartialEq, E0369), and 'position(|e| e == e)' comparing the parameter with itself
        contains(rust, "crate::jrt::same(Some(a), maybe)");
        contains(rust, "position(|__e| std::ptr::eq(__e, e))");
        contains(rust, "nodes.iter().any(|__e| std::ptr::eq(__e, n))");
        assertEquals(List.of(), rustcErrors(), rust);
    }
    @Language("java")
    private static final String FAMILY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Fam {
                public abstract static class Stat {
                    public abstract List<String> names();
                }
                // fernflower's SwitchHelper candidates, Instruction.clone: one member makes a new object
                public static class Fresh extends Stat {
                    public List<String> names() { return new ArrayList<>(); }
                }
                // the other returns its field
                public static class Held extends Stat {
                    private final List<String> list = new ArrayList<>();
                    public List<String> names() { return list; }
                    public void add(String s) { list.add(s); }
                }
            }
            """;

    @DisplayName("a dispatched method one member returns new: owned in every member and the dispatcher")
    @Test
    public void familyOwnedResult() throws Exception {
        List<TypeInfo> types = parse(FAMILY);
        TypeInfo fam = types.getFirst();
        nonModifying(fam.findSubType("Stat"), "names");
        nonModifying(fam.findSubType("Fresh"), "names");
        nonModifying(fam.findSubType("Held"), "names");
        modifying(fam.findSubType("Held"), "add");
        dependentResult(fam.findSubType("Held"), "names");
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        assertFalse(rust.contains("-> &Vec<String>"), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String EQUALS = """
            package a.b;
            import java.util.List;
            public class Eqs {
                // fernflower's Exprent: the root declares no equals, its members do
                public abstract static class Expr {
                    public abstract int weight();
                }
                public static class Var extends Expr {
                    private final int index;
                    public Var(int index) { this.index = index; }
                    public int weight() { return 1; }
                    @Override
                    public boolean equals(Object o) {
                        if (o == this) return true;
                        if (!(o instanceof Var v)) return false;
                        return index == v.index;
                    }
                }
                public static class Const extends Expr {
                    private final int value;
                    public Const(int value) { this.value = value; }
                    public int weight() { return 0; }
                    @Override
                    public boolean equals(Object o) {
                        return o instanceof Const c && value == c.value;
                    }
                }
                public static class Field extends Expr {
                    public int weight() { return 2; }
                }
                public static boolean same(Expr a, Expr b) { return a.equals(b); }
                public static boolean sameOrNull(Expr a, Expr maybe) { return a.equals(maybe); }
                public static void drop(List<Expr> exprs, Expr e) { exprs.remove(e); }
            }
            """;

    @DisplayName("equals in a closed hierarchy: each member's own, PartialEq dispatching on the kind")
    @Test
    public void hierarchyEquals() throws Exception {
        List<TypeInfo> types = parse(EQUALS);
        TypeInfo eqs = types.getFirst();
        for (String t : List.of("Expr", "Var", "Const", "Field")) nonModifying(eqs.findSubType(t), "weight");
        nonModifying(eqs, "same", "sameOrNull");
        modifying(eqs, "drop");
        method(eqs, "same").parameters().forEach(CommonJavaToRust::borrowed);
        method(eqs, "sameOrNull").parameters().forEach(CommonJavaToRust::borrowed);
        exclusive(method(eqs, "drop").parameters().get(0));
        borrowed(method(eqs, "drop").parameters().get(1));
        RustTranslator.Result result = translate(types, nullable(java.util.Set.of("maybe")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "fn eq_eqs_var(&self, other: &Self) -> bool {");
        contains(rust, "impl PartialEq for EqsExpr {");
        contains(rust, "EqsExprKind::EqsField(..) => std::ptr::eq(self, other),");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
