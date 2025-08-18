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
