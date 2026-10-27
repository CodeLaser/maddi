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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The return-type counterpart of {@link TestParameterInfoSource}.
 * <p>
 * A method can be created from its <em>symbol</em> before its declaration is scanned -- a method reference is the
 * usual way -- and the declaration pass then re-converts the return type from the <em>tree</em>, keying that fresh
 * instance into the detailed sources. {@code DetailedSources} is identity-keyed, so a caller asking for
 * {@code method.returnType()}'s source misses, because it holds the symbol-built instance.
 * <p>
 * That miss is what a consumer needs the source for: to place a type-replacement edit on the return type when a
 * method is moved to another class. Without it the rewrite is silently skipped and the moved code keeps a name
 * that no longer resolves there.
 */
public class TestReturnTypeSource extends CommonTest {

    @Language("java")
    private static final String CLASS_A = """
            package a;
            import java.util.List;
            import java.util.stream.Collectors;

            class A {
                List<R> transform(List<Integer> items) {
                    return items.stream()
                               .map(this::make)
                               .collect(Collectors.toList());
                }

                R make(Integer i) {
                    return new R(i);
                }

                record R(int i) {
                }
            }
            """;

    @DisplayName("a method created from its symbol still has a source for its own return type instance")
    @Test
    public void test1() {
        TypeInfo A = scan("a.A", CLASS_A);
        MethodInfo make = A.findUniqueMethod("make", 1);
        assertNotNull(make.source());
        assertNotNull(make.source().detailedSources(),
                "no detailed sources at all on a method reached via a method reference");
        // 'R' on line 12: '    R make(Integer i) {'
        assertNotNull(make.source().detailedSources().detail(make.returnType()),
                "no detailedSources entry for the method's OWN return type instance: the declaration pass keyed"
                + " a tree-built instance, and DetailedSources is identity-keyed");
    }

    @DisplayName("the ordinary path -- declaration scanned first -- keeps working")
    @Test
    public void test2() {
        TypeInfo A = scan("a.A", CLASS_A);
        MethodInfo transform = A.findUniqueMethod("transform", 1);
        assertNotNull(transform.source().detailedSources().detail(transform.returnType()));
    }
}
