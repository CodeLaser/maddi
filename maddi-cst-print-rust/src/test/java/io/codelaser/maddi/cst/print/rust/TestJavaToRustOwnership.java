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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The verdicts as Rust's ownership modes (CodeLaser/maddi#167), and rustc as their judge: correct verdicts compile;
 * a wrong one is an error at the line that contradicts it.
 */
public class TestJavaToRustOwnership extends CommonJavaToRust {

    @Language("java")
    private static final String INVENTORY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Inventory {
                private final List<String> names = new ArrayList<>();
                private int count;

                public void add(String name) {
                    names.add(name);
                    count++;
                }

                public int count() {
                    return count;
                }

                public List<String> names() {
                    return names;
                }

                public boolean contains(String name) {
                    return names.contains(name);
                }

                public static int total(List<Inventory> list) {
                    int sum = 0;
                    for (Inventory i : list) sum += i.count();
                    return sum;
                }
            }
            """;

    /** The verdicts the analysis computes for {@link #INVENTORY}. */
    private static TypeInfo inventory(List<TypeInfo> types) {
        TypeInfo inventory = types.getFirst();
        modifying(inventory, "add");
        nonModifying(inventory, "count", "names", "contains");
        // 'name' becomes an element of the list: kept, with hidden content
        kept(method(inventory, "add").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(inventory, "contains").parameters().getFirst());
        borrowed(method(inventory, "total").parameters().getFirst());
        dependentResult(inventory, "names");
        independentResult(inventory, "count");
        return inventory;
    }

    @DisplayName("correct verdicts compile")
    @Test
    public void correctVerdictsCompile() throws Exception {
        List<TypeInfo> types = parse(INVENTORY);
        inventory(types);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "pub fn add(&mut self, name: String) {");
        contains(rust, "pub fn count(&self) -> i32 {");
        contains(rust, "pub fn names(&self) -> &Vec<String> {");
        contains(rust, "pub fn contains(&self, name: &str) -> bool {");
        contains(rust, "pub fn total(list: &Vec<Inventory>) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("a modifying method the verdicts call non-modifying: rustc's E0596 at the modification")
    @Test
    public void wrongModificationVerdict() throws Exception {
        List<TypeInfo> types = parse(INVENTORY);
        TypeInfo inventory = types.getFirst();
        // the deliberate mistake: 'add' modifies 'names' and 'count'
        nonModifying(inventory, "add", "count", "names", "contains");
        kept(method(inventory, "add").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(inventory, "contains").parameters().getFirst());
        borrowed(method(inventory, "total").parameters().getFirst());
        dependentResult(inventory, "names");
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn add(&self, name: String) {");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String HOLDER = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Holder {
                private final List<String> list;

                public Holder(List<String> list) {
                    this.list = list;
                }

                public int size() {
                    return list.size();
                }

                public static int shared() {
                    List<String> l = new ArrayList<>();
                    Holder h = new Holder(l);
                    l.add("x");
                    return h.size();
                }
            }
            """;

    /**
     * The constructor keeps the caller's list (dependent), and the caller goes on modifying it: Java's aliasing, which
     * Rust does not allow on a moved value. The verdict is right; this is where the printer must choose another
     * representation ({@code Rc<RefCell<…>>}), and rustc's E0382 says exactly where.
     */
    @DisplayName("a dependent parameter is moved in; the caller's later use is rustc's E0382")
    @Test
    public void dependentParameterIsMoved() throws Exception {
        List<TypeInfo> types = parse(HOLDER);
        TypeInfo holder = types.getFirst();
        nonModifying(holder, "size");
        MethodInfo constructor = holder.constructors().getFirst();
        kept(constructor.parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn new(list: Vec<String>) -> Self {");
        contains(rust, "Self {\nlist: list,\n}");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0382")), () -> String.join("\n", errors) + "\n" + rust);
    }
}
