package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestComputePartOfConstruction extends CommonTest {

    @DisplayName("part of construction of CallGraph test 3")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(TestCallGraph.INPUT3);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);

        Value.SetOfInfo setOfInfo = X.analysis().getOrNull(PART_OF_CONSTRUCTION, ValueImpl.SetOfInfoImpl.class);
        assertNotNull(setOfInfo);
        assertEquals("[a.b.X.<init>(int), a.b.X.initList(int)]",
                setOfInfo.infoSet().stream().map(Object::toString).sorted().toList().toString());

        FieldInfo list = X.getFieldByName("list", true);
        assertSame(TRUE, list.analysis().getOrDefault(PropertyImpl.FINAL_FIELD, FALSE));
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                interface Exit { }
            
                record ExceptionThrown(Exception exception) implements Exit { }
            
                interface LoopData {
                    LoopData withException(Exception e);
                }
            
                static class LoopDataImpl {
                    private Exit exit;
                    LoopDataImpl(Exit exit) {
                        this.exit = exit;
                    }
            
                    @Override
                    public LoopData withException(Exception e) {
                        Exit ee = new ExceptionThrown(e);
                        return new LoopDataImpl(ee);
                    }
                }
            }
            """;

    @DisplayName("final field")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);

        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        assertEquals("SetOfInfoImpl[infoSet=[a.b.X.ExceptionThrown.<init>(Exception)]]",
                exceptionThrown.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION).toString());

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        assertEquals("SetOfInfoImpl[infoSet=[a.b.X.LoopDataImpl.<init>(a.b.X.Exit)]]",
                loopDataImpl.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION).toString());

        FieldInfo exit = loopDataImpl.getFieldByName("exit", true);
        assertTrue(exit.isPropertyFinal());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                private int i;
                public X() {
                    update();
                }
                public void init() {
                    Runnable r = ()->update();
                    r.run();
                }
            
                private void update() {
                    ++i;
                }
            }
            """;

    @DisplayName("lambda and part of construction")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);

        assertEquals("[a.b.X.<init>()]",
                X.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION).infoSet().toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                private int i;
                public X() {
                    update(); 
                }
                public void init() {
                    Runnable r = new Runnable() {
                        @Override public void run() {
                            update();
                        }
                    };
                    r.run();
                }
            
                private void update() {
                    ++i;
                }
            }
            """;

    @DisplayName("anonymous type and part of construction")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);

        assertEquals("[a.b.X.<init>()]",
                X.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION).infoSet().toString());
    }
}
