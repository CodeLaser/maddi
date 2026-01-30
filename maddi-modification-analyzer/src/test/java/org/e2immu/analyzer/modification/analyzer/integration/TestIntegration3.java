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

package org.e2immu.analyzer.modification.analyzer.integration;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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


    @DisplayName("virtual field computation")
    @Test
    public void test1a() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        MethodInfo find = B.findUniqueMethod("find", 2);
        assertEquals("Type java.util.Map<Long,java.util.List<T extends X.DTO>>", find.returnType().toString());
        VirtualFields vf = virtualFieldComputer.compute(find.returnType(), false).virtualFields();
        assertEquals("§m - §$TS[] §$tss", vf.toString());

        // and ... go
        analyzer.go(ao);
    }
}
