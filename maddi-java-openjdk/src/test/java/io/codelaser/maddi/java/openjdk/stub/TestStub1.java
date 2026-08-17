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

package io.codelaser.maddi.java.openjdk.stub;

import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("stubs not implemented yet")
public class TestStub1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package io.codelaser.maddi.test;

            public class X {
                private Y y;
                public X(Y y) {
                    this.y = y;
                }
            }
            """;


    @Test
    public void test() {
        assertThrows(Summary.FailFastException.class, () ->
                scan("io.codelaser.maddi.test.X", INPUT1));
    }

}
