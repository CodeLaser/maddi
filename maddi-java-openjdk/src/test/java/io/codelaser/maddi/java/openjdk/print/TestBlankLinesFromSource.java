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


package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#49: {@link FormattingOptions#blankLinesFromSource()}. The blank line after the imports was lost,
 * and the formatter added blank lines of its own around every statement or member that spans several lines, a
 * javadoc and its method included. With the option, the blank lines are the source's.
 */
public class TestBlankLinesFromSource extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;

            import java.util.ArrayList;
            import java.util.List;

            class X {
                private final List<String> names = new ArrayList<>();
                private int count;

                /**
                 * Adds a name.
                 *
                 * @param name the name
                 */
                void add(String name) {
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException();
                    }
                    names.add(name);

                    count++;
                }
                int count() {
                    return count;
                }
            }
            """;

    private String print(TypeInfo typeInfo, boolean blankLinesFromSource) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(typeInfo.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .setAlwaysBreakPriorityBlocks(true).setBlankLinesFromSource(blankLinesFromSource).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @DisplayName("with blankLinesFromSource, the blank lines are those of the source, and no others")
    @Test
    public void fromSource() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals(INPUT, print(typeInfo, true));
    }

    @DisplayName("control: by default, the formatter's own blank lines")
    @Test
    public void formatters() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals("""
                package a.b;
                import java.util.ArrayList;
                import java.util.List;
                class X {
                    private final List<String> names = new ArrayList<>();
                    private int count;
                    /**
                     * Adds a name.
                     *
                     * @param name the name
                     */
                    void add(String name) {
                        if (name.isEmpty()) {
                            throw new IllegalArgumentException();
                        }
                        names.add(name);
                        count++;
                    }

                    int count() {
                        return count;
                    }
                }
                """, print(typeInfo, false));
    }
}
