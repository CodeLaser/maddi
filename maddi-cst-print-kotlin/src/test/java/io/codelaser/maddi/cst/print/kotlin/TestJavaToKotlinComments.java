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
package io.codelaser.maddi.cst.print.kotlin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java's comments come through: the licence header above {@code package}, a Javadoc as a KDoc, line and block comments
 * on types and members where Java has them.
 */
public class TestJavaToKotlinComments extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    @DisplayName("licence header, class Javadoc, member Javadoc, line and block comments")
    @Test
    public void typesAndMembers() {
        String kotlin = kotlin("""
                /*
                 * Licence header.
                 */
                package a;

                /**
                 * The inventory of the shop.
                 */
                public class Inventory {
                    /** How many there are. */
                    private int count;
                    // the name, as shown
                    private String name = "x"; // trailing

                    /**
                     * Adds some.
                     * @param n how many
                     * @return the new count
                     */
                    public int add(int n) {
                        count += n;
                        return count;
                    }

                    /* a block comment */
                    void reset() { count = 0; }

                    /** The kinds. */
                    enum Kind {
                        /** a big one */
                        BIG,
                        SMALL
                    }
                    // at the end
                }
                """);
        contains(kotlin, """
                /*
                 * Licence header.
                 */
                package a""");
        contains(kotlin, """
                /**
                 * The inventory of the shop.
                 */
                open class Inventory""");
        contains(kotlin, """
                /**
                 * How many there are.
                 */
                private var count""");
        contains(kotlin, "// the name, as shown\nprivate val name");
        contains(kotlin, "\"x\" // trailing");
        contains(kotlin, """
                /**
                 * Adds some.
                 * @param n how many
                 * @return the new count
                 */
                open fun add(n: Int): Int""");
        contains(kotlin, "/* a block comment */");
        contains(kotlin, "* The kinds.");
        contains(kotlin, """
                /**
                 * a big one
                 */
                BIG,""");
        contains(kotlin, "// at the end");
        compiles(kotlin);
    }

    @DisplayName("inline tags in KDoc's Markdown; a nested /* would open a Kotlin comment")
    @Test
    public void kdocTags() {
        String kotlin = kotlin("""
                package a;
                import java.util.List;
                /**
                 * Holds {@code n} items, see {@link List} and {@link #size() its size}; matches src/**{@literal /}*.java.
                 * @throws IllegalStateException never
                 * @see #size
                 */
                public class Box {
                    /** {@inheritDoc} */
                    @Override public String toString() { return "box"; }
                    /* not /* nested */
                    int size() { return 0; }
                    /**
                     * Empty when {@code
                     * size() == 0}, as in
                     * {@snippet :
                     * if (box.isEmpty()) { return; }
                     * }
                     */
                    boolean isEmpty() { return size() == 0; }
                    /** Sets as {@code {{}, {1}, {2}}}, with {@link List}. */
                    void powerSet() { }
                }
                """);
        contains(kotlin, "Empty when `size() == 0`, as in");
        contains(kotlin, "* ```\n* if (box.isEmpty()) { return; }\n* ```\n*/");
        contains(kotlin, "Sets as `{{}, {1}, {2}}`, with [List].");
        // a Javadoc of {@inheritDoc} alone is no KDoc at all
        contains(kotlin, "{\noverride fun toString()");
        contains(kotlin, "Holds `n` items, see [List] and [its size][size];");
        contains(kotlin, "src/ **");
        contains(kotlin, "@throws IllegalStateException never");
        contains(kotlin, "@see size");
        contains(kotlin, "/* not / * nested */");
        compiles(kotlin);
    }

    @DisplayName("a primary constructor's Javadoc before its keyword; a header property's before the property")
    @Test
    public void primaryConstructor() {
        String kotlin = kotlin("""
                package a;
                public class Point {
                    /** the x */
                    private final int x;
                    private final int y;
                    /** Makes a point. */
                    public Point(int x, int y) {
                        this.x = x;
                        this.y = y;
                    }
                    int x() { return x; }
                    int y() { return y; }
                }
                """);
        contains(kotlin, "* Makes a point.");
        contains(kotlin, "constructor(");
        contains(kotlin, "* the x");
        compiles(kotlin);
    }
}
