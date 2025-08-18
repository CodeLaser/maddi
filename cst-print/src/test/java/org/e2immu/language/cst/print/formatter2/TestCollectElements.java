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
