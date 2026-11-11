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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * Java's shared objects (rust-printing.md, "Shared objects"): a type whose objects a method keeps (a dependent
 * parameter) and a field or collection holds is {@code crate::jrt::Shared<T>}, Java's reference: a clone shares,
 * {@code ==} and hashing are identity. The borrow checker still judges the handles.
 */
public class TestJavaToRustSharing extends CommonJavaToRust {

    @Language("java")
    private static final String GRAPH = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class Graph {
                // fernflower's Statement and StatEdge: an edge keeps both its nodes, both nodes keep the edge
                public static class Node {
                    private int id;
                    private final List<Edge> succs = new ArrayList<>();
                    private final List<Edge> preds = new ArrayList<>();
                    public Node(int id) { this.id = id; }
                    public void addSuccessor(Edge e) {
                        succs.add(e);
                        e.getDestination().preds.add(e);
                    }
                    public Edge connect(Node to) {
                        Edge e = new Edge(this, to);
                        addSuccessor(e);
                        return e;
                    }
                    public List<Edge> getSuccs() { return succs; }
                    public int getId() { return id; }
                    public void setId(int id) { this.id = id; }
                }
                public static class Edge {
                    private final Node source;
                    private final Node destination;
                    public Edge(Node source, Node destination) { this.source = source; this.destination = destination; }
                    public Node getSource() { return source; }
                    public Node getDestination() { return destination; }
                }
                public static class User {
                    public int link(Node a, Node b) {
                        Edge e = a.connect(b);
                        b.setId(a.getId() + 1);
                        return e.getSource() == a ? a.getSuccs().size() : -1;
                    }
                    public int reach(Node start) {
                        Set<Node> seen = new HashSet<>();
                        List<Node> todo = new ArrayList<>();
                        todo.add(start);
                        while (!todo.isEmpty()) {
                            Node n = todo.remove(todo.size() - 1);
                            if (seen.add(n)) {
                                for (Edge e : n.getSuccs()) todo.add(e.getDestination());
                            }
                        }
                        return seen.size();
                    }
                }
            }
            """;

    private List<TypeInfo> graphTypes() {
        return graphTypes(false);
    }

    private List<TypeInfo> graphTypes(boolean wrongVerdict) {
        List<TypeInfo> types = parse(GRAPH);
        TypeInfo graph = types.getFirst();
        TypeInfo node = graph.findSubType("Node");
        TypeInfo edge = graph.findSubType("Edge");
        TypeInfo user = graph.findSubType("User");
        modifying(node, "addSuccessor", "connect", "setId");
        nonModifying(node, "getSuccs", "getId");
        nonModifying(edge, "getSource", "getDestination");
        nonModifying(user, "link", "reach");
        // kept: the edge keeps both nodes, the node keeps the edge, connect keeps 'to' in a new edge
        // kept and modified: the destination it reaches gets the edge (e.getDestination().preds.add(e))
        method(node, "addSuccessor").parameters().getFirst().analysis()
                .set(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT);
        method(node, "addSuccessor").parameters().getFirst().analysis()
                .set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
        kept(method(node, "connect").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        edge.constructors().getFirst().parameters().forEach(p -> kept(p, ValueImpl.IndependentImpl.DEPENDENT));
        dependentResult(node, "getSuccs");
        dependentResult(edge, "getSource");
        dependentResult(edge, "getDestination");
        if (wrongVerdict) {
            // as the engine could get it wrong: link's 'a' unmodified, while a.connect(b) modifies it
            borrowed(method(user, "link").parameters().get(0));
        } else {
            exclusive(method(user, "link").parameters().get(0));
        }
        exclusive(method(user, "link").parameters().get(1));
        borrowed(method(user, "reach").parameters().getFirst());
        return types;
    }

    @DisplayName("a graph whose nodes and edges keep each other: Shared handles, cloned where kept, == as identity")
    @Test
    public void graph() throws Exception {
        List<TypeInfo> types = graphTypes();
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // held as handles, no lifetimes: the edge keeps both nodes, the nodes keep the edge
        contains(rust, "pub struct GraphEdge {\n    source: crate::jrt::Shared<GraphNode>,");
        contains(rust, "succs: Vec<crate::jrt::Shared<GraphEdge>>,");
        // 'this' kept by the new edge: its own handle; the kept edge shared, as it is read again
        contains(rust, "let e = crate::jrt::Shared::new(GraphEdge::new(crate::jrt::Shared::of(self), to));");
        contains(rust, "self.add_successor(e.clone());");
        // the argument's clone first, as the receiver chain holds e mutably
        contains(rust, "{ let __e = e.clone(); e.get_destination_mut().preds.push(__e) }");
        // Java's == and an identity set
        contains(rust, "crate::jrt::Shared::is(e.get_source(), a)");
        contains(rust, "let mut seen: HashSet<crate::jrt::Shared<GraphNode>> = HashSet::new();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("the oracle stays: a handle the verdicts call unmodified cannot be written through (E0596)")
    @Test
    public void wrongVerdict() throws Exception {
        List<TypeInfo> types = graphTypes(true);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn link(&self, a: &crate::jrt::Shared<GraphNode>,");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String STATS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Set;
            public class Stats {
                public static class Stat {
                    private Stat first;
                    private final List<Stat> stats = new ArrayList<>();
                    private int type;
                    public void setFirst(Stat s) { first = s; }
                    public Stat getFirst() { return first; }
                    public void add(Stat s) { stats.add(s); }
                    public int getType() { return type; }
                }
                // fernflower's findIfHead: a nullable borrowed handle walking the chain
                public static int headType(Stat head) {
                    while (head != null && head.getType() != 2) {
                        head = head.getFirst();
                    }
                    return head == null ? -1 : head.getType();
                }
                // getDefStatement's setStats.add(stat), iterateExprents' exprents.set(i, res)
                public static void collect(Stat stat, Set<Stat> set, List<Stat> list) {
                    if (stat != null) {
                        set.add(stat);
                        list.set(0, stat);
                    }
                }
                // a list of handles searched for a nullable one: identity
                public static boolean has(List<Stat> list, Stat s) { return list.contains(s); }
            }
            """;

    @DisplayName("nullable handles: a borrowed one walking the chain, one stored where the holder is not nullable, one searched")
    @Test
    public void nullableHandles() throws Exception {
        List<TypeInfo> types = parse(STATS);
        TypeInfo stats = types.getFirst();
        TypeInfo stat = stats.findSubType("Stat");
        modifying(stat, "setFirst", "add");
        nonModifying(stat, "getFirst", "getType");
        nonModifying(stats, "headType", "has");
        modifying(stats, "collect");
        kept(method(stat, "setFirst").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        kept(method(stat, "add").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        dependentResult(stat, "getFirst");
        borrowed(method(stats, "headType").parameters().getFirst());
        borrowed(method(stats, "collect").parameters().get(0));
        exclusive(method(stats, "collect").parameters().get(1));
        exclusive(method(stats, "collect").parameters().get(2));
        method(stats, "has").parameters().forEach(CommonJavaToRust::borrowed);
        RustTranslator.Result result = translate(types, nullable(java.util.Set.of("first", "getFirst()", "head", "stat", "s")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "head = head.as_ref().unwrap().get_first();");
        contains(rust, "Some(__e) == s");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String DOM = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Dom {
                public static class Edge {
                    private Stat destination;
                    public Edge(Stat destination) { this.destination = destination; }
                    public Stat getDestination() { return destination; }
                    public void retarget(Stat d) { destination = d; }
                }
                // fernflower's Statement and IfStatement: a closed hierarchy
                public static class Stat {
                    protected final List<Edge> succs = new ArrayList<>();
                    public void addSuccessor(Edge e) { succs.add(e); }
                    public void removeSuccessor(Edge e) { succs.remove(e); e.retarget(null); }
                }
                public static class IfStat extends Stat {
                    private Edge ifEdge;
                    public Edge getIfEdge() { return ifEdge; }
                    public void setIfEdge(Edge e) { ifEdge = e; }
                }
                // DomHelper: the if-edge, reached through its getter, is modified by removeSuccessor
                public static void unlink(Stat first, IfStat stat) {
                    first.removeSuccessor(stat.getIfEdge());
                }
            }
            """;

    @DisplayName("an argument the callee modifies, from a hierarchy member's getter: the getter's exclusive twin")
    @Test
    public void hierarchyGetterTwin() throws Exception {
        List<TypeInfo> types = parse(DOM);
        TypeInfo dom = types.getFirst();
        TypeInfo edge = dom.findSubType("Edge");
        TypeInfo stat = dom.findSubType("Stat");
        TypeInfo ifStat = dom.findSubType("IfStat");
        modifying(edge, "retarget");
        nonModifying(edge, "getDestination");
        modifying(stat, "addSuccessor", "removeSuccessor");
        modifying(ifStat, "setIfEdge");
        nonModifying(ifStat, "getIfEdge");
        modifying(dom, "unlink");
        edge.constructors().getFirst().parameters().forEach(p -> kept(p, ValueImpl.IndependentImpl.DEPENDENT));
        kept(method(stat, "addSuccessor").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        kept(method(ifStat, "setIfEdge").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        kept(method(edge, "retarget").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        exclusive(method(stat, "removeSuccessor").parameters().getFirst());
        dependentResult(ifStat, "getIfEdge");
        dependentResult(edge, "getDestination");
        exclusive(method(dom, "unlink").parameters().get(0));
        exclusive(method(dom, "unlink").parameters().get(1));
        RustTranslator.Result result = translate(types, nullable(java.util.Set.of("ifEdge", "getIfEdge()", "destination", "d")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "get_if_edge_mut()");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String POINT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Point {
                public static class Stack {
                    private final List<Integer> items = new ArrayList<>();
                    public void push(int i) { items.add(i); }
                    public int top() { return items.get(items.size() - 1); }
                    public void insert(int at, int v) { items.add(at, v); }
                }
                // fernflower's DataPoint
                public static class Data {
                    private Stack stack = new Stack();
                    private final List<Stack> stacks = new ArrayList<>();
                    public Stack getStack() { return stack; }
                    public List<Stack> getStacks() { return stacks; }
                    public void setStack(Stack s) { stack = s; }
                }
                // InstructionImpact.stepTypes: the stack, read through the getter, is modified through a local
                public static void step(Data data) {
                    Stack stack = data.getStack();
                    stack.push(1);
                    stack.push(2);
                }
                // fernflower's 'for (Statement st : stat.getStats())', modifying each
                public static void pushAll(Data data) {
                    for (Stack s : data.getStacks()) s.push(3);
                }
                // fernflower's stat.getFirst().removeSuccessor(e), its holder borrowed
                public static void pushThrough(Data data) {
                    data.getStack().push(5);
                }
                // InstructionImpact: 'stack.insertByOffset(depth, stack.getByOffset(-1).copy())'
                public static void dup(Data data) {
                    Stack s = data.getStack();
                    s.insert(0, s.top());
                }
                // fernflower's isAssertionField(fexpr, ...): a borrowed handle passed where the callee modifies
                public static void grow(Stack s) { s.push(4); }
                public static void growBorrowed(Stack s) { grow(s); }
            }
            """;

    @DisplayName("a shared object modified through a local or a loop variable: a handle of its own")
    @Test
    public void localHandle() throws Exception {
        List<TypeInfo> types = parse(POINT);
        TypeInfo point = types.getFirst();
        TypeInfo stack = point.findSubType("Stack");
        TypeInfo data = point.findSubType("Data");
        modifying(stack, "push", "insert");
        nonModifying(stack, "top");
        modifying(data, "setStack");
        nonModifying(data, "getStack", "getStacks");
        modifying(point, "step", "pushAll", "pushThrough", "dup", "grow", "growBorrowed");
        exclusive(method(point, "grow").parameters().getFirst());
        borrowed(method(point, "growBorrowed").parameters().getFirst());
        exclusive(method(point, "dup").parameters().getFirst());
        kept(method(data, "setStack").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        dependentResult(data, "getStack");
        dependentResult(data, "getStacks");
        exclusive(method(point, "step").parameters().getFirst());
        exclusive(method(point, "pushAll").parameters().getFirst());
        borrowed(method(point, "pushThrough").parameters().getFirst());
        RustTranslator.Result result = translate(types, nullable(java.util.Set.of()));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "let mut stack = data.get_stack().clone();");
        contains(rust, "for mut s in data.get_stacks().iter().cloned().collect::<Vec<_>>() {");
        contains(rust, "data.get_stack().clone().push(5);");
        contains(rust, "{ let __a1 = s.top(); s.insert(0, __a1) }");
        contains(rust, "Self::grow(&mut s.clone());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SEQS = """
            package a.b;
            import java.util.ArrayList;
            public class Seqs {
                // fernflower's VBStyleCollection: a source class that is a JDK collection
                public static class Coll<E> extends ArrayList<E> {
                    public E getAt(int i) { return get(i); }
                    public void addAt(E e) { add(e); }
                }
                public static class Instr {
                    private int op;
                    public void setOp(int op) { this.op = op; }
                }
                // InstructionSequence: instructions kept in the collection, shared between sequences
                public static class Seq {
                    private final Coll<Instr> instrs = new Coll<>();
                    public void add(Instr i) { instrs.addAt(i); }
                    public Instr get(int k) { return instrs.getAt(k); }
                }
                public static void copy(Seq from, Seq to) { to.add(from.get(0)); }
            }
            """;

    @DisplayName("objects kept in a source collection class are shared")
    @Test
    public void sourceCollection() throws Exception {
        List<TypeInfo> types = parse(SEQS);
        TypeInfo seqs = types.getFirst();
        TypeInfo coll = seqs.findSubType("Coll");
        TypeInfo instr = seqs.findSubType("Instr");
        TypeInfo seq = seqs.findSubType("Seq");
        modifying(coll, "addAt");
        nonModifying(coll, "getAt");
        kept(method(coll, "addAt").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        dependentResult(coll, "getAt");
        modifying(instr, "setOp");
        modifying(seq, "add");
        nonModifying(seq, "get");
        kept(method(seq, "add").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        dependentResult(seq, "get");
        modifying(seqs, "copy");
        borrowed(method(seqs, "copy").parameters().get(0));
        exclusive(method(seqs, "copy").parameters().get(1));
        RustTranslator.Result result = translate(types, nullable(java.util.Set.of()));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "instrs: SeqsColl<crate::jrt::Shared<SeqsInstr>>,");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("found `&")), () -> String.join("\n", errors) + "\n" + rust);
    }
}
