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

package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Printed source that no longer compiles: GitHub CodeLaser/maddi #106, #107, #108. */
public class TestPrinterRoundTripIssues extends CommonTest {

    private String print(TypeInfo ti) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(120).setSpacesInTab(4).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @DisplayName("#106: a call with explicit type arguments keeps its qualifier")
    @Test
    public void explicitTypeArguments() {
        TypeInfo ti = scan("a.b.X", """
                package a.b;
                import java.util.List;
                class X {
                    static <T> List<T> empty() { return null; }
                    <T> List<T> two(T t) { return null; }
                    static <T> List<T> one(T t) { List<T> l = X.<T>empty(); return l; }
                    <T> List<T> inst() { return this.<T>two(null); }
                }
                """);
        String out = print(ti);
        assertTrue(out.contains("X.<T>empty()") || out.contains("X.<T> empty()"), out);
        assertTrue(out.contains("this.<T>two(null)") || out.contains("this.<T> two(null)"), out);
    }

    @DisplayName("#106: a call to a method named yield keeps its qualifier")
    @Test
    public void yieldCall() {
        TypeInfo ti = scan("a.b.Y", """
                package a.b;
                class Y {
                    String yield(String s) { return s; }
                    static String sYield(String s) { return s; }
                    String callYield() { return this.yield("a"); }
                }
                """);
        String out = print(ti);
        assertTrue(out.contains("this.yield(\"a\")"), out);
    }

    @DisplayName("#107: outer.new Inner() names the class by its simple name")
    @Test
    public void qualifiedInnerCreation() {
        TypeInfo ti = scan("a.b.Z", """
                package a.b;
                public class Z {
                    class Inner { final int v; Inner(int v) { this.v = v; } }
                    static Inner make(Z outer) { return outer.new Inner(1); }
                    Inner makeThis() { return this.new Inner(2); }
                }
                """);
        String out = print(ti);
        assertTrue(out.contains("outer.new Inner(1)"), out);
        assertTrue(out.contains("this.new Inner(2)"), out);
        assertFalse(out.contains("new Z.Inner"), out);
    }

    @DisplayName("#108: in a local class, a line comment before a field ends its line")
    @Test
    public void localClassFieldComment() {
        TypeInfo ti = scan("a.b.W", """
                package a.b;
                import java.util.ArrayList;
                import java.util.List;
                class W {
                    int local(List<int[]> in) {
                        class Entry {
                            public final int a;

                            // TODO: a comment before a field
                            public final List<int[]> b;

                            Entry(int a, List<int[]> b) { this.a = a; this.b = new ArrayList<>(b); }
                        }
                        return new Entry(1, in).a;
                    }
                }
                """);
        String out = print(ti);
        for (String line : out.split("\\n")) {
            int c = line.indexOf("// TODO");
            if (c >= 0) assertFalse(line.substring(c).contains(" b;"), "the field is inside the comment:\n" + out);
        }
        assertTrue(out.contains("List<int[]> b;") || out.contains("List<int []> b;"), out);
    }
}
