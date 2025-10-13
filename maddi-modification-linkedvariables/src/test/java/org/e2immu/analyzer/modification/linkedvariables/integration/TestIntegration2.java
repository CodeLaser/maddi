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

package org.e2immu.analyzer.modification.linkedvariables.integration;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestIntegration2 extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            public class X {
                interface Root {
                   Long get(String key);
                }
                interface Query { }
                interface Predicate { }
                interface CriteriaBuilder {
                    Predicate and(Predicate p1, Predicate p2);
                    Predicate equal(Long l1, Long l2);
                    Predicate conjunction();
                }
                interface Specification<T> {
                    Predicate toPredicate(Root root, Query query, CriteriaBuilder criteriaBuilder);
                }
                interface Rating { }
                interface Criteria {
                    Long id();
                    Long id2();
                }

                static Specification<Rating> createSpecification(Criteria criteria) {
                    return (root, query, criteriaBuilder) -> {
                        Predicate predicate = criteriaBuilder.conjunction();
                        predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("id"), criteria.id()));
                        predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("id2"), criteria.id2()));
                        return predicate;
                    };
                }
            }
            """;

    @DisplayName("Integer null")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
