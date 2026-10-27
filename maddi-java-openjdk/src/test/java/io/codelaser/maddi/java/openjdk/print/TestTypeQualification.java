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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTypeQualification extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.Date;
            class X {
                public java.sql.Date method(Date date) {
                   return new java.sql.Date(date.getTime());
                }
            }
            """;

    @Language("java")
    public static final String OUTPUT1 = """
            package a.b;
            import java.util.Date;
            class X { public java.sql.Date method(Date date) { return new java.sql.Date(date.getTime()); } }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        assertEquals("""
                [java.sql.Date[E FQN], java.util.Date[E], java.sql.Date[E FQN], java.util.Date[I], java.sql.Date[I]]\
                """, X.typesReferenced(null).toList().toString());

        // Original: javaInspector.importComputer(4, null) — factory method on JavaInspector.
        // Replaced with direct instantiation; no equivalent factory is available here.
        ImportComputer importComputer = new ImportComputerImpl();
        // Original: javaInspector.runtime().qualificationQualifyFromPrimaryType() — same field, different access.
        Qualification qualification = runtime.qualificationQualifyFromPrimaryType();
        assertNotNull(qualification);
        ImportComputer.Result result = importComputer.go(X.compilationUnit(), qualification);
        assertEquals(1, result.imports().size());
        assertEquals("java.util.Date", result.imports().getFirst().importString());

        assertEquals(OUTPUT1, print2(X.compilationUnit()));
    }
}
