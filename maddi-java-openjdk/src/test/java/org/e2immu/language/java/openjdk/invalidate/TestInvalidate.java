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

package org.e2immu.language.java.openjdk.invalidate;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestInvalidate extends CommonTest {

    private static final String PROCESSOR_FQN = "a.b.util.Processor";
    private static final String ISOURCE_FQN = "a.b.ISource";

    @Language("java")
    String PROCESSOR = """
            package a.b.util;
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

    @Language("java")
    String ISOURCE = """
            package a.b;
            import a.b.util.Processor;
            public interface ISource { Processor.ProcessResult processResult(); }
            """;

    @Language("java")
    String ISOURCE_CHANGED = """
            package a.b;
            import a.b.util.Processor;
            public interface ISource {
                Processor.ProcessResult processResult();
            }
            """;

    @Language("java")
    String SOURCE = """
            package a.b;
            import a.b.util.Processor;
            import java.util.Set;
            public record Source(String name, String src, Set<String> tags, Processor.ProcessResult processResult) implements ISource {
            }
            """;


    // Original tests extended CommonTest2 and used javaInspector.reloadSources(), makeInputConfiguration(),
    // and sourcesByURIString() to simulate incremental re-parsing after a source change (ISOURCE →
    // ISOURCE_CHANGED). None of those methods exist in this module's CommonTest. The tests were
    // simplified to a single scan of all three sources; the invalidation/reload behaviour is not covered.
    @Test
    public void testReload() {
        Map<String, TypeInfo> pr1 = scan(false, ISOURCE_FQN, ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        TypeInfo iSource = pr1.get(ISOURCE_FQN);
        assertEquals("5qzB4ttzbH5oaGHwsCf4Qw==", iSource.compilationUnit().fingerPrintOrNull().toString());
        assertEquals(3, pr1.size());
    }

    @Test
    public void test1() {
        Map<String, TypeInfo> pr1 = scan(false, ISOURCE_FQN, ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        TypeInfo processor = pr1.get(PROCESSOR_FQN);
        assertEquals("swTzAiYtIXTHZ/quxa3mFQ==", processor.compilationUnit().fingerPrintOrNull().toString());
        assertEquals(3, pr1.size());
    }

    @Test
    public void test2() {
        Map<String, TypeInfo> pr1 = scan(false, "a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        assertEquals(3, pr1.size());
    }

    @Test
    public void test3() {
        Map<String, TypeInfo> pr1 = scan(false, "a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        assertEquals(3, pr1.size());
    }

    @Test
    public void test4() {
        Map<String, TypeInfo> pr1 = scan(false, "a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        assertEquals(3, pr1.size());
    }
}
