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

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter.TestFormatter1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCollectElements {
    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void test1() {
        OutputBuilder outputBuilder = TestFormatter1.createExample1();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                public int method(
                
                  int p1,
                  int p2){
                
                  return p1+p2;}\
                """;
        assertEquals(expect, out);
    }

    @Test
    public void test2() {
        OutputBuilder outputBuilder = TestFormatter1.createExample2();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                public int method(
                
                  int p1,
                  int p2,
                  double somewhatLonger,
                  double d){
                
                  log(

                    p1,
                    p2);
                  return p1+p2;}\
                """;
        assertEquals(expect, out);
    }


    @Test
    public void test3() {
        OutputBuilder outputBuilder = TestFormatter1.createExample3();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                try{
                
                  if(a){

                    assert b;}else{

                    assert c;
                    exit(1);}}\
                """;
        assertEquals(expect, out);
    }
}
