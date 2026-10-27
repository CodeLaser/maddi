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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle;
import io.codelaser.maddi.cst.api.statement.SwitchStatementOldStyle;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestParseSwitchNewStyle extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
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
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof SwitchStatementNewStyle ssn) {
            assertEquals("""
                            switch(args.length){case 0->System.out.println("zero!");case 1,2->{System.out.println("less than 3");return;}default->// noinspection ALL
                            {System.out.println("all the rest");}}\
                            """,
                    ssn.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class NewSwitchStatement_0 {
            
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
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(1) instanceof SwitchStatementNewStyle ssn) {
            assertEquals("""
                            switch(dataType){case 3->{s="x";}case 4->s="z";default->s="y";}\
                            """,
                    ssn.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }
}
