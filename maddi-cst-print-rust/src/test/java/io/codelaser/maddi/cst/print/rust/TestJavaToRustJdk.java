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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The JDK as std: comparators, a map's value modified in place, Object values, null-safe string calls. */
public class TestJavaToRustJdk extends CommonJavaToRust {

    @Language("java")
    private static final String CATALOG = """
            package a.b;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Catalog {
                private final Map<String, Integer> views = new HashMap<>();
                private final Map<String, Map<String, String>> articles = new HashMap<>();
                private final Map<String, Object> data = new HashMap<>();

                public List<String> top(int n) {
                    return views.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(n).map(Map.Entry::getKey).toList();
                }

                public void setTitle(String id, String title) {
                    articles.getOrDefault(id, new HashMap<>()).put("title", title);
                }

                public String describe(String key) {
                    return "value: " + data.get(key);
                }

                public boolean isExpress(String destination) {
                    return "express".equalsIgnoreCase(destination);
                }

                public void count(String key) {
                    views.merge(key, 1, Integer::sum);
                }
            }
            """;

    @DisplayName("comparator, getOrDefault modified in place, Object displayed, null-safe equalsIgnoreCase: compiles")
    @Test
    public void catalog() throws Exception {
        List<TypeInfo> types = parse(CATALOG);
        TypeInfo catalog = types.getFirst();
        modifying(catalog, "setTitle", "count");
        nonModifying(catalog, "top", "describe", "isExpress");
        independentResult(catalog, "top");
        independentResult(catalog, "describe");
        for (String m : List.of("setTitle", "describe", "isExpress")) {
            method(catalog, m).parameters().forEach(CommonJavaToRust::borrowed);
        }
        kept(method(catalog, "count").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);

        RustTranslator.Result result = translate(types, nullable(Set.of("destination", "describe()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "v.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());");
        contains(rust, "self.articles.get_mut(id).unwrap_or(&mut HashMap::new()).insert(");
        contains(rust, "crate::jrt::to_string(");
        contains(rust, "destination.as_ref().map_or(false, |v| \"express\".eq_ignore_ascii_case(v))");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
