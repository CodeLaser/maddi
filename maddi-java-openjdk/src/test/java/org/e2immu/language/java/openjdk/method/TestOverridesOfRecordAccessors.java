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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverridesOfRecordAccessors extends CommonTest {

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

    @Disabled("fingerprints not implemented yet")
    @Test
    public void test() {
        String processorFqn = "io.codelaser.jfocus.stdbase.viewer.util.Processor";
        Map<String, TypeInfo> types = scan(false,
                "io.codelaser.jfocus.stdbase.viewer.ISource", ISOURCE,
                "io.codelaser.jfocus.stdbase.viewer.Source", SOURCE,
                processorFqn, PROCESSOR);
        assertEquals(3, types.size());
        TypeInfo processor = types.get(processorFqn);
        assertEquals("OhUf4rF0+cdKdIdanESW7g==", processor.compilationUnit().fingerPrintOrNull().toString());
    }
}
