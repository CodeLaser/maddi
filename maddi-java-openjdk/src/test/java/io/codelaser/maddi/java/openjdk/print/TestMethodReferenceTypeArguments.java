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

import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#136: a method reference whose type has type arguments kept the last type argument as its
 * scope. {@code ArrayList<String>::new} printed as {@code String::new}, which means something else.
 */
public class TestMethodReferenceTypeArguments extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.function.Function;
            import java.util.function.IntFunction;
            import java.util.function.Supplier;
            import java.util.function.ToIntFunction;
            class X {
                Supplier<List<String>> withArguments = ArrayList<String>::new;
                Supplier<List<String>> raw = ArrayList::new;
                Supplier<Map<String, List<Integer>>> nested = HashMap<String, List<Integer>>::new;
                ToIntFunction<List<String>> method = List<String>::size;
                IntFunction<String[][]> twoDimensions = String[][]::new;
                IntFunction<List<String>[]> genericArray = List[]::new;
                Function<String, Integer> plain = String::length;
            }
            """;

    private String printed(TypeInfo typeInfo, String field) {
        FieldInfo fieldInfo = typeInfo.getFieldByName(field, true);
        MethodReference mr = (MethodReference) fieldInfo.initializer();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        return new Formatter2Impl(runtime, options).write(mr.print(runtime.qualificationQualifyFromPrimaryType()))
                .strip();
    }

    @DisplayName("the scope of a method reference is the whole type as written")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals("ArrayList<String>::new", printed(typeInfo, "withArguments"));
        assertEquals("ArrayList::new", printed(typeInfo, "raw"));
        assertEquals("HashMap<String, List<Integer>>::new", printed(typeInfo, "nested"));
        assertEquals("List<String>::size", printed(typeInfo, "method"));
        assertEquals("String[][]::new", printed(typeInfo, "twoDimensions"));
        assertEquals("List[]::new", printed(typeInfo, "genericArray"));
        assertEquals("String::length", printed(typeInfo, "plain"));
    }
}
