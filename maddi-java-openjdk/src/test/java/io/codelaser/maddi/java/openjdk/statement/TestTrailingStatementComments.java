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


package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#128: the single-line comment after a statement, on its last line, belongs to that statement, also
 * when the token after it is not the statement's next sibling: the closing brace of its block, or whatever follows
 * a braceless {@code if}, {@code for}, {@code while} or {@code else}. The comment pre-scan looked for it in front of
 * the next sibling only, so it ended up on the wrong element: the enclosing block's trailing comments, or, after
 * {@code } // c} followed by {@code else}, the then-block's LEADING comments. A rewriting tool that checks the
 * comments of what it replaces missed them there.
 * <p>
 * Each comment must also be attached once: the block javac wraps around a braceless body has the statement's own
 * source, and took the statement's comments a second time.
 */
public class TestTrailingStatementComments extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            class C {
                boolean has(List<String> names, String name) {
                    for (String s : names) {
                        if (name.equals(s)) return true; // found1
                    }
                    if (name.isEmpty()) { return true; } // found2
                    if (name.isBlank()) {
                        return false;
                    } // found3
                    else return true; // found4
                }
                int count(List<String> names) {
                    int n = 0;
                    while (n < names.size()) n++; // loop
                    System.out.println(n); // last
                    // own line
                    return n;
                }
            }
            """;

    @DisplayName("a comment after a statement on its line belongs to that statement, once")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);

        assertEquals("""
                ForEachStatement 5 []
                LocalVariableCreation 5 []
                Block 5 [] trailing []
                IfElseStatement 6 []
                Block 6 [] trailing []
                ReturnStatement 6 [found1]
                IfElseStatement 8 [found2]
                Block 8 [] trailing []
                ReturnStatement 8 []
                IfElseStatement 9 [found3]
                Block 9 [] trailing []
                ReturnStatement 10 []
                Block 12 [] trailing []
                ReturnStatement 12 [found4]
                """, describe(typeInfo.findUniqueMethod("has", 2)),
                "found1 and found4 belong to the return of a braceless body, found2 and found3 to the 'if' whose"
                + " block they follow; no block takes any of them");

        assertEquals("""
                LocalVariableCreation 15 []
                WhileStatement 16 []
                Block 16 [] trailing []
                ExpressionAsStatement 16 [loop]
                ExpressionAsStatement 17 [last]
                ReturnStatement 19 [own line]
                """, describe(typeInfo.findUniqueMethod("count", 1)),
                "'last' belongs to the println in front of the '}', not to the method's body");

        for (String c : List.of("found1", "found2", "found3", "found4")) {
            assertEquals(1, occurrences(typeInfo.findUniqueMethod("has", 2), c), c + " must be printed once");
        }
        for (String c : List.of("loop", "last")) {
            assertEquals(1, occurrences(typeInfo.findUniqueMethod("count", 1), c), c + " must be printed once");
        }
    }

    /** Every statement of the body, nested ones included, as its kind, its first line and its comments. */
    private static String describe(MethodInfo methodInfo) {
        List<String> lines = new ArrayList<>();
        Block body = methodInfo.methodBody();
        if (!body.trailingComments().isEmpty()) lines.add("body trailing " + strip(body.trailingComments()));
        body.visit(e -> {
            if (e != body && e instanceof Statement s) {
                String kind = s.getClass().getSimpleName().replace("Impl", "");
                lines.add(kind + " " + s.source().beginLine() + " " + strip(s.comments())
                          + (s instanceof Block b ? " trailing " + strip(b.trailingComments()) : ""));
            }
            return true;
        });
        return String.join("\n", lines) + "\n";
    }

    private static List<String> strip(List<Comment> comments) {
        return comments.stream().map(c -> c.comment().strip()).toList();
    }

    private int occurrences(MethodInfo methodInfo, String comment) {
        String printed = methodInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString();
        Matcher m = Pattern.compile("// " + comment + "\\b").matcher(printed);
        int n = 0;
        while (m.find()) n++;
        return n;
    }
}
