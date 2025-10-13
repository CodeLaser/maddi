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

package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverridesOfRecordAccessors extends CommonTest2 {

    @Language("java")
    String ISOURCE = """
            package io.codelaser.jfocus.stdbase.viewer;
            import io.codelaser.jfocus.stdbase.viewer.util.Processor;
            public interface ISource { Processor.ProcessResult processResult(); }
            """;

    @Language("java")
    String SOURCE = """
            package io.codelaser.jfocus.stdbase.viewer;
            import io.codelaser.jfocus.stdbase.viewer.util.Processor;
            import java.util.Set;
            public record Source(String name, String src, Set<String> tags, Processor.ProcessResult processResult) implements ISource {
            }
            """;

    @Language("java")
    String PROCESSOR = """
            package io.codelaser.jfocus.stdbase.viewer.util;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            public class Processor {
                private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
            
                public Processor() {
                }
            
                public record ProcessResult(String someResult) {
                }
            }
            """;

    @Test
    public void test() throws IOException {
        String processorFqn = "io.codelaser.jfocus.stdbase.viewer.util.Processor";
        ParseResult parseResult = init(Map.of("io.codelaser.jfocus.stdbase.viewer.ISource", ISOURCE,
                "io.codelaser.jfocus.stdbase.viewer.Source", SOURCE,
                processorFqn, PROCESSOR));
        assertEquals(3, parseResult.primaryTypes().size());
        TypeInfo processor = parseResult.findType(processorFqn);
        assertEquals("OhUf4rF0+cdKdIdanESW7g==", processor.compilationUnit().fingerPrintOrNull().toString());

        TypeInfo logger = javaInspector.compiledTypesManager().get("org.slf4j.Logger", null);
        SourceSet sourceSet = logger.compilationUnit().sourceSet();
        assertEquals("jar-on-classpath:org/slf4j/event", sourceSet.name());
        assertEquals("KbWqJ430MNlxkWaqzoLTCg==", logger.compilationUnit().fingerPrintOrNull().toString());
        // this hash corresponds to Slf4j-2.0.17; it will change when the library is updated
        assertEquals("tkgNEUojaDSYrD90b5WdLw==", sourceSet.fingerPrintOrNull().toString());
    }
}
