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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class literal in an annotation value is a type reference like any other (GitHub CodeLaser/maddi#169): written
 * qualified in the source, it must print qualified, or be imported.
 */
public class TestImportAnnotationClassLiteral extends CommonTest2 {

    @Language("java")
    private static final String RESTRICTED = """
            package proj.ann;
            public @interface Restricted { Class<?>[] value(); }
            """;

    @Language("java")
    private static final String NO_EXTERNAL_USE = """
            package proj.ann.restrictions;
            public class NoExternalUse { }
            """;

    @Language("java")
    private static final String MESSAGES = """
            package proj;
            import proj.ann.Restricted;

            @Restricted(proj.ann.restrictions.NoExternalUse.class)
            public class Messages {
                @Restricted(proj.ann.restrictions.NoExternalUse.class)
                public static String hello() { return "hello"; }

                @Restricted(proj.ann.restrictions.NoExternalUse.class)
                static class Inner { }
            }
            """;

    @Language("java")
    private static final String IMPORTED = """
            package proj;
            import proj.ann.Restricted;
            import proj.ann.restrictions.NoExternalUse;

            @Restricted(NoExternalUse.class)
            public class Imported {
                @Restricted({ NoExternalUse.class })
                public static String hello() { return "hello"; }
            }
            """;

    private String print(ParseResult parseResult, String fqn) {
        TypeInfo typeInfo = parseResult.findType(fqn);
        ImportComputer ic = javaInspector.importComputer(Integer.MAX_VALUE, typeInfo.compilationUnit().sourceSet());
        return javaInspector.print2(typeInfo.compilationUnit(), (Qualification.Decorator) null, ic);
    }

    @Test
    public void qualifiedClassLiteral() throws IOException {
        ParseResult parseResult = init(Map.of("proj.ann.Restricted", RESTRICTED,
                "proj.ann.restrictions.NoExternalUse", NO_EXTERNAL_USE, "proj.Messages", MESSAGES));
        String printed = print(parseResult, "proj.Messages");
        // written qualified, so printed qualified: on the type, the nested type and the method alike
        assertFalse(printed.contains("import proj.ann.restrictions.NoExternalUse;"), printed);
        assertFalse(Pattern.compile("[^.]NoExternalUse\\.class").matcher(printed).find(), printed);
        assertEquals(3, printed.split("@Restricted\\(proj\\.ann\\.restrictions\\.NoExternalUse\\.class\\)", -1).length - 1,
                printed);
    }

    @Test
    public void importedClassLiteral() throws IOException {
        ParseResult parseResult = init(Map.of("proj.ann.Restricted", RESTRICTED,
                "proj.ann.restrictions.NoExternalUse", NO_EXTERNAL_USE, "proj.Imported", IMPORTED));
        String printed = print(parseResult, "proj.Imported");
        assertTrue(printed.contains("import proj.ann.restrictions.NoExternalUse;"), printed);
        assertTrue(printed.contains("@Restricted(NoExternalUse.class)"), printed);
    }
}
