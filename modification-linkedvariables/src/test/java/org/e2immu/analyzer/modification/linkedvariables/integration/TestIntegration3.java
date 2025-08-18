package org.e2immu.analyzer.modification.linkedvariables.integration;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestIntegration3 extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestIntegration3.class);

    @Language("java")
    private static final String INPUT1 = """
            import java.util.ArrayList;
            import java.util.Map;
            import java.util.List;
            import java.util.Optional;
            import java.util.function.Function;
            import java.util.stream.Collectors;
            public class X {
                interface DTO {
                    Long getId();
                }
                static class Lists {
                    static <T> List<T> partition(List<T> list, int groups) {
                        throw new UnsupportedOperationException("NYI");
                    }
                }
                interface A {
                    Optional<Long> getId();
                    boolean isA();
                }
                private A a;
                <T extends DTO> Map<Long, List<T>> find(List<Long> ids, Function<List<Long>, List<T>> function) {
                    return a
                        .getId()
                        .map(currentUserId -> {
                            ArrayList<T> logs = new ArrayList<>();
                            Lists.partition(ids, 200).forEach(partitions -> {
                              List<T> list = function.apply(partitions);
                              logs.addAll(list);
                            });
                            return logs
                                 .stream()
                                 .filter(log -> a.isA() && log != null)
                                 .collect(Collectors.groupingBy(DTO::getId));
                        })
                        .orElseGet(Map::of);
                }
            }
            """;

    @DisplayName("variable logs: overwrite modification true to false")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        LOGGER.info("Order: {}", ao);
        analyzer.go(ao);
    }
}
