package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReadFieldReferences extends CommonTest {

    @Language("java")
    public static String INPUT = """
            package a.b;
            import java.util.stream.IntStream;
            import org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop;
            
            class X {
                private double[][] nxmCosTableX = null;
                private double[][] nxmCosTableY = null;
                private int N = 0;
                private int M = 0;
                private int pixelRange(int p) { return ((p > 255) ? 255 :(p < 0) ? 0 : p); }
            
                public void method(double[][] dcts, int[][] pixels) {
                    int x = 0;
                    int y = 0;
                    int i = 0;
                    int j = 0;
                    double t = 0.0;
                    double cx0 = Math.sqrt(1.0 / this.N);
                    double cy0 = Math.sqrt(1.0 / this.M);
                    for(x = 0; x < this.N; x++ ) {
                        for(y = 0; y < this.M; y++ ) {
                            t = cx0 * cy0 * dcts[0][0];
                            Loop.LoopData ldIn = new Loop.LoopDataImpl.Builder()
                                .set(0, cy0)
                                .set(1, dcts)
                                .set(2, t)
                                .set(3, x)
                                .iterator(IntStream.iterate(1, i0 -> i0 < this.N, i0 -> i0 + 1).iterator())
                                .build();
                            Loop.LoopData ld = Loop.run(ldIn);
                            t = (double)ld.get(2);
                            Loop.LoopData ldIn0 = new Loop.LoopDataImpl.Builder()
                                .set(0, cx0)
                                .set(1, dcts)
                                .set(2, t)
                                .set(3, y)
                                .iterator(IntStream.iterate(1, j0 -> j0 < this.M, j0 -> j0 + 1).iterator())
                                .build();
                            Loop.LoopData ld0 = Loop.run(ldIn0);
                            t = (double)ld0.get(2);
                            for(i = 1; i < this.N; i++ ) {
                                Loop.LoopData ldIn1 = new Loop.LoopDataImpl.Builder()
                                    .set(0, dcts)
                                    .set(1, i)
                                    .set(2, t)
                                    .set(3, x)
                                    .set(4, y)
                                    .iterator(IntStream.iterate(1, j1 -> j1 < this.M, j1 -> j1 + 1).iterator())
                                    .build();
                                Loop.LoopData ld1 = Loop.run(ldIn1);
                                t = (double)ld1.get(2);
                            }
                            pixels[x][y] = this.pixelRange((int)(t + 128.5));
                        }
                    }
                }
            }
            """;

    @Test
    public void test1() throws IOException {
        String loopFile = "./src/test/java/org/e2immu/analyzer/modification/linkedvariables/staticvalues/Loop.java";
        String loopJava = Files.readString(Path.of(loopFile));
        {
            TypeInfo loop = javaInspector.parse(loopJava);
            List<Info> analysisOrder = prepWork(loop);
            analyzer.go(analysisOrder);
        }
        {
            TypeInfo X = javaInspector.parse(INPUT);
            List<Info> analysisOrder = prepWork(X);
            assertEquals("""
                   [a.b.X.$11.applyAsInt(int), a.b.X.$3.applyAsInt(int), a.b.X.$7.applyAsInt(int), a.b.X.<init>(), \
                   a.b.X.nxmCosTableX, a.b.X.nxmCosTableY, a.b.X.pixelRange(int), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop, \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopData, \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopData.get(int), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopDataImpl.Builder, \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopDataImpl.Builder.<init>(), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopDataImpl.Builder.build(), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopDataImpl.Builder.iterator(java.util.Iterator<?>), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopDataImpl.Builder.set(int,Object), \
                   org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.run(org.e2immu.analyzer.modification.linkedvariables.staticvalues.Loop.LoopData), \
                   a.b.X.$11, a.b.X.$3, a.b.X.$7, a.b.X.$10, a.b.X.$10.test(int), a.b.X.$2, a.b.X.$2.test(int), \
                   a.b.X.$6, a.b.X.$6.test(int), a.b.X.M, a.b.X.N, a.b.X.method(double[][],int[][]), a.b.X]\
                   """, analysisOrder.toString());

            MethodInfo methodInfo = X.findUniqueMethod("method", 2);
            Block innerLoop = methodInfo.methodBody().statements().get(7).block().statements().getFirst().block();
            LocalVariableCreation lvc1 = (LocalVariableCreation) innerLoop.statements().get(1);
            Expression translated = lvc1.localVariable().assignmentExpression()
                    .translate(new ApplyGetSetTranslation(runtime));
            assertEquals("""
                    (new Builder().variables[0]=cy0,\
                    new Builder().variables[1]=dcts,\
                    new Builder().variables[2]=t,\
                    new Builder().variables[3]=x,\
                    new Builder().loop=IntStream.iterate(1,i0->i0<this.N,i0->i0+1).iterator(),\
                    new Builder()).build()\
                    """, translated.toString());

            analyzer.go(analysisOrder);
        }
    }
}
