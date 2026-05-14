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

package org.e2immu.language.inspection.openjdk.statement;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestSwitchNewStyle extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              static void main(String[] args) {
                switch(args.length) {
                  case 0 ->
                    System.out.println("zero!");
                  case 1, 2 -> {
                    System.out.println("less than 3");
                    return;
                  }
                  default ->
                  // noinspection ALL
                  {
                    System.out.println("all the rest");
                  }
                }
                System.out.println("end");
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT), List.of()).getFirst();
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof SwitchStatementNewStyle ssn) {
            assertEquals("""
                            switch(args.length){case 0->System.out.println("zero!");case 1,2->{System.out.println("less than 3");return;}default->// noinspection ALL
                            {System.out.println("all the rest");}}\
                            """,
                    ssn.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C {
                public static String method(int dataType) {
                    String s;
                    switch (dataType) {
            
                        case 3 -> {
                            s = "x";
                        }
            
                        case 4 ->
                            s = "z";
            
                        default ->
                            s = "y";
            
                    }
                    return s;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT2), List.of()).getFirst();

        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(1) instanceof SwitchStatementNewStyle ssn) {
            assertEquals("""
                            switch(dataType){case 3->{s="x";}case 4->s="z";default->s="y";}\
                            """,
                    ssn.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }
}
