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
