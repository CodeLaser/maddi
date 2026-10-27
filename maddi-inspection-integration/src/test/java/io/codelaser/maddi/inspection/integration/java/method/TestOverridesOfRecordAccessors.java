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

package io.codelaser.maddi.inspection.integration.java.method;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
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

        TypeInfo logger = javaInspector.compiledTypesManager().typeIfLoaded("org.slf4j.Logger", null);
        SourceSet sourceSet = logger.compilationUnit().sourceSet();
        assertEquals("jar-on-classpath:org/slf4j/event", sourceSet.name());
        assertEquals("KbWqJ430MNlxkWaqzoLTCg==", logger.compilationUnit().fingerPrintOrNull().toString());
        // this hash corresponds to Slf4j-2.0.17; it will change when the library is updated
        assertEquals("tkgNEUojaDSYrD90b5WdLw==", sourceSet.fingerPrintOrNull().toString());
    }
}
