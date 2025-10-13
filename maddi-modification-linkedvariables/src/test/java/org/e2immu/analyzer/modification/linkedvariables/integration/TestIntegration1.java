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

public class TestIntegration1 extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            import java.util.List;
            import java.util.Optional;
            public class X {
                interface DTO2 { }
                interface DTO {
                    List<DTO2> capabilities();
                 }
                interface Log {
                    void debug(String s, Long l);
                }
                interface Service {
                    Optional<DTO> findOne(Long id);
                }
                private Log LOG;
                private Service service;
                enum HttpStatus { OK, NOT_FOUND }
                static class ResponseStatusException {
                    ResponseStatusException(HttpStatus httpStatus) { }
                }
                List<DTO2> convert(Long id) {
                    LOG.debug("convert '{}'", id);
                    return service
                      .findOne(id)
                      .map(this::mapper)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                }
                List<DTO2> mapper(DTO dto) {
                    return dto.capabilities();
                }
            }
            """;

    @DisplayName("what is null")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
