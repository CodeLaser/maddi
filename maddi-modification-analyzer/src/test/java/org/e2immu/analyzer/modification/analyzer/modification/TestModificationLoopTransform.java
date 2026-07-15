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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/*
The fourth application of linking: CodeLaser's jfocus-transform rewrites loops and try-catch statements into
simple statements. Local variables are packed into an Object[] "variables" slot array inside a LoopData/TryData,
the body becomes a method reference, and Loop.run/Try.run drive the iteration. Correctness of the transformation
requires that modification analysis gives identical verdicts before and after: a mutation of a local unpacked
via a downcast '(float[][]) ld.get(1)' must cascade through the @GetSet("variables") slot array, the functional
interface application inside Loop.run, and the builder's 'set(pos, value)' store, back to the original variable.

LOOP and TRY below are literal copies of io.codelaser.jfocus.transform.support.Loop and Try (jfocus-transform),
repackaged to a.b. The X input and the assertions mirror the active assertions of jfocus-transform's
codelaser-transform-loops TestModification (UpperTriangle, a 4-level nested loop/try-catch transformation).
 */
public class TestModificationLoopTransform extends CommonTest {

    @Language("java")
    private static final String LOOP = """
            package a.b;

            import org.e2immu.annotation.Fluent;
            import org.e2immu.annotation.method.GetSet;

            import java.util.Iterator;
            import java.util.function.Function;

            public class Loop {

                public static LoopData run(LoopData initial) {
                    LoopData ld = initial;
                    while (ld.hasNext()) {
                        if (ld.loop() != null) {
                            Object loopValue = ld.loop().next();
                            LoopData nextLd = ld.withLoopValue(loopValue);
                            ld = ld.body().apply(nextLd);
                        } else {
                            ld = ld.body().apply(ld);
                        }
                    }
                    return ld;
                }

                public interface LoopData {

                    Throwable exception();

                    @GetSet("variables")
                    Object get(int i);

                    Object getReturnValue();

                    boolean hasNext();

                    boolean isReturn();

                    Object loopValue();

                    int level();

                    LoopData with(int pos, Object value);

                    LoopData withBreak(boolean doBreak);

                    LoopData withBreakLevel(boolean doBreak, int level);

                    LoopData withContinueLevel(boolean doContinue, int level);

                    LoopData withException(Throwable e);

                    LoopData withLoopValue(Object loopValue);

                    LoopData withReturn(boolean doReturn);

                    LoopData withReturnValue(boolean doReturn, Object returnValue);

                    @GetSet
                    Iterator<?> loop();

                    @GetSet
                    Function<LoopData, LoopData> body();
                }

                public interface Exit {
                }

                public static class Break implements Exit {
                    private final int level;

                    public Break(int level) {
                        this.level = level;
                    }
                }

                public static class Continue implements Exit {
                    private final int level;

                    public Continue(int level) {
                        this.level = level;
                    }
                }

                public static class Return implements Exit {
                    private final Object value;

                    public Return(Object value) {
                        this.value = value;
                    }

                    public Object value() {
                        return value;
                    }
                }

                public static class ExceptionThrown implements Exit {
                    private final Throwable exception;

                    public ExceptionThrown(Throwable exception) {
                        this.exception = exception;
                    }

                    public Throwable exception() {
                        return exception;
                    }
                }

                public static class LoopDataImpl implements LoopData {
                    private final Iterator<?> loop;
                    private final Exit exit;
                    private final Object[] variables;
                    private final Function<LoopData, LoopData> body;
                    private final Object loopValue;

                    public LoopDataImpl(Iterator<?> loop,
                                        Function<LoopData, LoopData> body,
                                        Exit exit,
                                        Object[] variables,
                                        Object loopValue) {
                        this.exit = exit;
                        this.variables = variables;
                        this.loop = loop;
                        this.body = body;
                        this.loopValue = loopValue;
                    }

                    @Override
                    public Iterator<?> loop() {
                        return loop;
                    }

                    @Override
                    public Function<LoopData, LoopData> body() {
                        return body;
                    }

                    @Override
                    public int level() {
                        if (exit instanceof Break) {
                            Break b = (Break) exit;
                            return b.level;
                        } else if (exit instanceof Continue) {
                            Continue c = (Continue) exit;
                            return c.level;
                        } else {
                            return -1;
                        }
                    }

                    @Override
                    public Throwable exception() {
                        if (exit instanceof ExceptionThrown) return ((ExceptionThrown) exit).exception();
                        return null;
                    }

                    @Override
                    public Object get(int i) {
                        return variables[i];
                    }

                    @Override
                    public Object getReturnValue() {
                        return ((Return) exit).value;
                    }

                    @Override
                    public boolean hasNext() {
                        if (exit != null) return false;
                        return loop == null || loop.hasNext();
                    }

                    @Override
                    public boolean isReturn() {
                        return exit instanceof Return;
                    }

                    @Override
                    public Object loopValue() {
                        return loopValue;
                    }

                    @Override
                    public LoopData with(int pos, Object value) {
                        Object[] newVariables = variables.clone();
                        newVariables[pos] = value;
                        return new LoopDataImpl(loop, body, exit, newVariables, loopValue);
                    }

                    @Override
                    public LoopData withException(Throwable e) {
                        return new LoopDataImpl(loop, body, new ExceptionThrown(e), variables, loopValue);
                    }

                    @Override
                    public LoopData withBreak(boolean doBreak) {
                        return withBreakLevel(doBreak, -1);
                    }

                    @Override
                    public LoopData withBreakLevel(boolean doBreak, int level) {
                        if (exit instanceof ExceptionThrown || exit instanceof Return) {
                            // the break has lower priority
                            return this;
                        }
                        if (exit instanceof Continue || exit instanceof Break) {
                            if (!doBreak) {
                                return this;
                            }
                            throw new UnsupportedOperationException("Runtime: we cannot already have a break or continue");
                        }
                        Exit exit = doBreak ? new Break(level) : null;
                        return new LoopDataImpl(loop, body, exit, variables, loopValue);
                    }

                    @Override
                    public LoopData withContinueLevel(boolean doContinue, int level) {
                        if (exit instanceof ExceptionThrown || exit instanceof Return) {
                            // the 'continue' has lower priority
                            return this;
                        }
                        if (exit instanceof Continue || exit instanceof Break) {
                            if (!doContinue) {
                                return this;
                            }
                            throw new UnsupportedOperationException("Runtime: we cannot already have a break or continue");
                        }
                        Exit exit = doContinue ? new Continue(level) : null;
                        return new LoopDataImpl(loop, body, exit, variables, loopValue);
                    }

                    @Override
                    public LoopData withReturnValue(boolean doReturn, Object returnValue) {
                        if (exit instanceof ExceptionThrown || !doReturn) {
                            // the return value has lower priority
                            return this;
                        }
                        return new LoopDataImpl(loop, body, new Return(returnValue), variables, loopValue);
                    }

                    @Override
                    public LoopData withReturn(boolean doReturn) {
                        return withReturnValue(doReturn, null);
                    }

                    @Override
                    public LoopData withLoopValue(Object loopValue) {
                        return new LoopDataImpl(loop, body, exit, variables, loopValue);
                    }

                    private static final int NUM_VARIABLES = 20;

                    public static class Builder {
                        private final Object[] variables = new Object[NUM_VARIABLES];
                        private Iterator<?> loop;
                        private Function<LoopData, LoopData> body;

                        @GetSet
                        @Fluent
                        public Builder body(Function<LoopData, LoopData> body) {
                            this.body = body;
                            return this;
                        }

                        public LoopDataImpl build() {
                            return new LoopDataImpl(loop, body, null, variables, null);
                        }

                        @GetSet
                        @Fluent
                        public Builder iterator(Iterator<?> iterator) {
                            this.loop = iterator;
                            return this;
                        }

                        @GetSet
                        @Fluent
                        public Builder set(int pos, Object value) {
                            variables[pos] = value;
                            return this;
                        }
                    }
                }
            }
            """;

    @Language("java")
    private static final String TRY = """
            package a.b;

            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;

            public class Try {

                public static TryData run(TryData td) {
                    try {
                        return td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        return td.withException(e);
                    }
                }

                public static TryData run1(TryData td) {
                    try {
                        td.createResources();
                    } catch (Throwable e) {
                        return td.withException(e);
                    }
                    try (AutoCloseable a1 = td.resource(0)) {
                        return td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        return td.withException(e);
                    }
                }

                @FunctionalInterface
                public interface ThrowingFunction {
                    TryData apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }

                @FunctionalInterface
                public interface ResourceFunction {
                    AutoCloseable[] apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }

                // so while TryData is an @Immutable(hc=true) object, it will undergo modification
                // due to downcasts to the "variables" getter
                public interface TryData {

                    void createResources() throws Throwable;

                    @GetSet("resources")
                    @NotModified
                    AutoCloseable resource(int i);

                    @NotModified
                    Throwable exception();

                    @GetSet("variables")
                    @NotModified
                    Object get(int i);

                    @NotModified
                    Object getReturnValue();

                    @NotModified
                    boolean isReturn();

                    @NotModified
                    String breakToLabel();

                    @NotModified
                    String continueToLabel();

                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();

                    @NotModified
                    @Independent
                    TryData with(int i, Object o);

                    @NotModified
                    @Independent
                    TryData withException(Throwable t);

                    @NotModified
                    @Independent
                    TryData withReturnValue(boolean doReturn, Object newValue);

                    @NotModified
                    @Independent
                    TryData withReturn(boolean doReturn);

                    @NotModified
                    @Independent
                    TryData withBreakToLabel(String label);

                    @NotModified
                    @Independent
                    TryData withContinueToLabel(String label);
                }

                public static class TryDataImpl implements TryData {
                    private final Throwable exception;
                    private final ThrowingFunction throwingFunction;
                    private final ResourceFunction resourceFunction;
                    private final Object[] variables;
                    private final boolean doReturn;
                    private final Object returnValue;
                    private final String continueToLabel;
                    private final String breakToLabel;

                    private AutoCloseable[] resources;

                    public TryDataImpl(ThrowingFunction throwingFunction,
                                       Object[] variables,
                                       ResourceFunction resourceFunction,
                                       boolean doReturn,
                                       Object returnValue,
                                       Throwable exception,
                                       String breakToLabel,
                                       String continueToLabel) {
                        this.exception = exception;
                        this.throwingFunction = throwingFunction;
                        this.variables = variables;
                        this.doReturn = doReturn;
                        this.returnValue = returnValue;
                        this.resourceFunction = resourceFunction;
                        this.breakToLabel = breakToLabel;
                        this.continueToLabel = continueToLabel;
                    }

                    @Override
                    public void createResources() throws Throwable {
                        resources = resourceFunction.apply(this);
                    }

                    @Override
                    public AutoCloseable resource(int i) {
                        return resources[i];
                    }

                    @Override
                    public boolean isReturn() {
                        return doReturn;
                    }

                    @Override
                    public Throwable exception() {
                        return exception;
                    }

                    @Override
                    public Object get(int i) {
                        return variables[i];
                    }

                    @Override
                    public Object getReturnValue() {
                        return returnValue;
                    }

                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }

                    @Override
                    public String breakToLabel() {
                        return breakToLabel;
                    }

                    @Override
                    public String continueToLabel() {
                        return continueToLabel;
                    }

                    @Override
                    public TryData with(int pos, Object value) {
                        Object[] newVariables = variables.clone();
                        newVariables[pos] = value;
                        return new TryDataImpl(throwingFunction, newVariables, resourceFunction, doReturn, returnValue, exception,
                                breakToLabel, continueToLabel);
                    }

                    @Override
                    public TryData withException(Throwable exception) {
                        return new TryDataImpl(throwingFunction, variables, resourceFunction, doReturn, returnValue, exception,
                                breakToLabel, continueToLabel);
                    }

                    @Override
                    public TryData withReturnValue(boolean doReturn, Object newValue) {
                        return new TryDataImpl(throwingFunction, variables, resourceFunction, doReturn, newValue, exception,
                                breakToLabel, continueToLabel);
                    }

                    @Override
                    public TryData withBreakToLabel(String label) {
                        return new TryDataImpl(throwingFunction, variables, resourceFunction, doReturn, returnValue, exception,
                                label, continueToLabel);
                    }

                    @Override
                    public TryData withContinueToLabel(String label) {
                        return new TryDataImpl(throwingFunction, variables, resourceFunction, doReturn, returnValue, exception,
                                breakToLabel, label);
                    }

                    @Override
                    public TryData withReturn(boolean doReturn) {
                        return withReturnValue(doReturn, null);
                    }

                    private static final int NUM_VARIABLES = 20;

                    public static class Builder {
                        private final Object[] variables = new Object[NUM_VARIABLES];
                        private ResourceFunction resourceFunction;
                        private ThrowingFunction bodyThrowingFunction;

                        public Builder body(ThrowingFunction throwingFunction) {
                            this.bodyThrowingFunction = throwingFunction;
                            return this;
                        }

                        public TryDataImpl build() {
                            return new TryDataImpl(bodyThrowingFunction, variables, resourceFunction, false,
                                    null, null, null, null);
                        }

                        public Builder set(int pos, Object value) {
                            variables[pos] = value;
                            return this;
                        }

                        public Builder resources(ResourceFunction resourceFunction) {
                            this.resourceFunction = resourceFunction;
                            return this;
                        }
                    }
                }
            }
            """;

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                public static void method(Class<?>[] declaredExceptions) {
                    Loop.LoopDataImpl.Builder builder = new Loop.LoopDataImpl.Builder();
                    builder.set(0, declaredExceptions);
                }
            }
            """;

    @DisplayName("crash in HCS.translateHcs()")
    @Test
    public void test1() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.Loop", LOOP, "a.b.X", INPUT1),
                javaInspector.failFast()).parseResult();
        TypeInfo loop = parseResult.findType("a.b.Loop");
        TypeInfo X = parseResult.findType("a.b.X");
        List<Info> analysisOrder = prepAnalyzer.doPrimaryTypes(Set.of(loop, X));
        analyzer.go(analysisOrder);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.stream.IntStream;
            class X {
                private static float iDF;

                public static float[][] UpperTriangle(float[][] m) {
                    float f1 = 0;
                    float temp = 0;
                    int tms = m.length;
                    int v = 1;
                    iDF = 1;

                    Loop.LoopData ldIn3 = new Loop.LoopDataImpl.Builder().set(0, f1)
                        .set(1, m)
                        .set(2, temp)
                        .set(3, tms)
                        .set(4, v)
                        .iterator(IntStream.iterate(0, col -> col < tms - 1, col -> col + 1).iterator())
                        .body(X::UpperTriangleLoopBody3)
                        .build();

                    Loop.LoopData ld0 = Loop.run(ldIn3);
                    if(ld0.isReturn()) { return (float[][])ld0.getReturnValue(); }
                    return m;
                }

                private static Loop.LoopData UpperTriangleLoopBody3(Loop.LoopData ld0) {
                    float f1 =(float)ld0.get(0);
                    float[][] m =(float[][])ld0.get(1);
                    float temp =(float)ld0.get(2);
                    int tms =(int)ld0.get(3);
                    int v =(int)ld0.get(4);
                    int col =(int)ld0.loopValue();
                    boolean doReturn = false;
                    float[][] returnValue = null;

                    Loop.LoopData ldIn2 = new Loop.LoopDataImpl.Builder().set(0, col)
                        .set(1, f1)
                        .set(2, m)
                        .set(3, temp)
                        .set(4, tms)
                        .set(5, v)
                        .iterator(IntStream.iterate(col + 1, row -> row < tms, row -> row + 1).iterator())
                        .body(X::UpperTriangleLoopBody2)
                        .build();

                    Loop.LoopData ld = Loop.run(ldIn2);
                    if(ld.isReturn()) { returnValue =(float[][])ld.getReturnValue(); doReturn = true; }
                    return ld0.withReturnValue(doReturn, returnValue);
                }

                private static Loop.LoopData UpperTriangleLoopBody2(Loop.LoopData ld) {
                    int col =(int)ld.get(0);
                    float f1 =(float)ld.get(1);
                    float[][] m =(float[][])ld.get(2);
                    float temp =(float)ld.get(3);
                    int tms =(int)ld.get(4);
                    int v =(int)ld.get(5);
                    int row =(int)ld.loopValue();
                    boolean doReturn = false;
                    float[][] returnValue = null;
                    v = 1;

                    Loop.LoopData ldIn1 = new Loop.LoopDataImpl.Builder().set(0, col)
                        .set(1, m)
                        .set(2, temp)
                        .set(3, tms)
                        .set(4, v)
                        .body(X::UpperTriangleLoopBody1)
                        .build();

                    Loop.run(ldIn1);

                    if(m[col][col] != 0) {
                        Try.TryData tdIn = new Try.TryDataImpl.Builder().set(0, col)
                            .set(1, f1)
                            .set(2, m)
                            .set(3, row)
                            .set(4, tms)
                            .body(X::UpperTriangleBody)
                            .build();
                        Try.TryData td = Try.run(tdIn);
                        if(td.exception() instanceof Exception e) {
                            System.out.println("Still Here!!!");
                        } else {
                            if(td.exception() instanceof Error e0) { throw e0; }
                            if(td.isReturn()) { returnValue =(float[][])td.getReturnValue(); doReturn = true; }
                        }
                    }

                    return ld.with(5, v).withReturnValue(doReturn, returnValue);
                }

                private static Try.TryData UpperTriangleBody(Try.TryData td) throws Exception {
                    int col =(int)td.get(0);
                    float f1 =(float)td.get(1);
                    float[][] m =(float[][])td.get(2);
                    int row =(int)td.get(3);
                    int tms =(int)td.get(4);
                    f1 =(-1) * m[row][col] / m[col][col];

                    Loop.LoopData ldIn0 = new Loop.LoopDataImpl.Builder().set(0, col)
                        .set(1, f1)
                        .set(2, m)
                        .set(3, row)
                        .iterator(IntStream.iterate(col, i -> i < tms, i -> i + 1).iterator())
                        .body(X::UpperTriangleLoopBody0)
                        .build();

                    Loop.run(ldIn0);
                    return td.with(1, f1);
                }

                private static Loop.LoopData UpperTriangleLoopBody1(Loop.LoopData ld) {
                    int col =(int)ld.get(0);
                    float[][] matrix =(float[][])ld.get(1);
                    float temp =(float)ld.get(2);
                    int tms =(int)ld.get(3);
                    int v =(int)ld.get(4);
                    boolean doBreak = false;
                    if(!(matrix[col][col] == 0)) { doBreak = true; }

                    if(!doBreak) {
                        if(col + v >= tms) {
                            iDF = 0;
                            doBreak = true;
                        } else {
                            Loop.LoopData ldIn = new Loop.LoopDataImpl.Builder()
                                .set(0, col)
                                .set(1, matrix)
                                .set(2, temp)
                                .set(3, v)
                                .iterator(IntStream.iterate(0, c -> c < tms, c -> c + 1).iterator())
                                .body(X::UpperTriangleLoopBody)
                                .build();
                            Loop.run(ldIn);
                            v++;
                            iDF = iDF * -1;
                        }
                    }

                    return ld.with(4, v).withBreak(doBreak);
                }

                private static Loop.LoopData UpperTriangleLoopBody0(Loop.LoopData ld) {
                    int col =(int)ld.get(0);
                    float f1 =(float)ld.get(1);
                    float[][] m =(float[][])ld.get(2);
                    int row =(int)ld.get(3);
                    int i =(int)ld.loopValue();
                    m[row][i] = f1 * m[col][i] + m[row][i];
                    return ld;
                }

                private static Loop.LoopData UpperTriangleLoopBody(Loop.LoopData ld) {
                    int col =(int)ld.get(0);
                    float[][] m =(float[][])ld.get(1);
                    float temp =(float)ld.get(2);
                    int v =(int)ld.get(3);
                    int c =(int)ld.loopValue();
                    temp = m[col][c];
                    m[col][c] = m[col + v][c];
                    m[col + v][c] = temp;
                    Loop.LoopData ldRv = ld.with(2, temp);
                    return ldRv;
                }
            }
            """;

    @DisplayName("cascade in modification")
    @Test
    public void test2() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.Loop", LOOP, "a.b.Try", TRY, "a.b.X", INPUT2),
                javaInspector.failFast()).parseResult();
        TypeInfo loop = parseResult.findType("a.b.Loop");
        TypeInfo tryType = parseResult.findType("a.b.Try");
        TypeInfo X = parseResult.findType("a.b.X");
        List<Info> analysisOrder = prepAnalyzer.doPrimaryTypes(Set.of(loop, tryType, X));
        analyzer.go(analysisOrder);

        testLoopRun(loop);
        testUpperTriangleLoopBody(X);
        testUpperTriangleLoopBody1(X);
        testUpperTriangleLoopBody2(X);
        testUpperTriangleLoopBody3(X);
        testUpperTriangle(X);
    }

    // the functional interface stored in the 'body' field is applied inside Loop.run: the parameter 'initial'
    // must be modified through the $_afi application variables, so that a modifying body lambda propagates
    // its modification to the call site's argument
    private static void testLoopRun(TypeInfo loop) {
        MethodInfo run = loop.findUniqueMethod("run", 1);
        ParameterInfo initial = run.parameters().getFirst();
        assertTrue(initial.isModified());
        MethodLinkedVariables mlvRun = run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        Variable to = mlvRun.ofReturnValue().link(0).to();
        if (to instanceof FieldReference fr && fr.scopeVariable() instanceof AppliedFunctionalInterfaceVariable afi) {
            assertEquals(1, afi.params().size());
            Links links = afi.params().getFirst().links();
            assertEquals("-", links.toString());
            assertEquals("nextLd", links.primary().toString());
        } else fail("expected the first return link to point into an applied functional interface, but got: " + to);
    }

    private static void testUpperTriangle(TypeInfo X) {
        MethodInfo UpperTriangle = X.findUniqueMethod("UpperTriangle", 1);
        ParameterInfo p0 = UpperTriangle.parameters().getFirst();

        Statement s6 = UpperTriangle.methodBody().statements().get(6);
        assertEquals("LoopData ld0=Loop.run(ldIn3);", s6.toString());
        VariableData vd6 = VariableDataImpl.of(s6);
        VariableInfo viLdIn6 = vd6.variableInfo("ldIn3");
        assertTrue(viLdIn6.isModified());
        VariableInfo viM = vd6.variableInfo(p0);
        assertTrue(viM.isModified());
    }

    private static void testUpperTriangleLoopBody3(TypeInfo X) {
        MethodInfo UpperTriangleLoopBody3 = X.findUniqueMethod("UpperTriangleLoopBody3", 1);

        Statement s9 = UpperTriangleLoopBody3.methodBody().statements().get(9);
        assertEquals("LoopData ld=Loop.run(ldIn2);", s9.toString());
        VariableData vd9 = VariableDataImpl.of(s9);
        VariableInfo viLdIn9 = vd9.variableInfo("ldIn2");
        assertTrue(viLdIn9.isModified());
        VariableInfo viM = vd9.variableInfo("m");
        assertTrue(viM.isModified());
    }

    private static void testUpperTriangleLoopBody2(TypeInfo X) {
        MethodInfo UpperTriangleLoopBody2 = X.findUniqueMethod("UpperTriangleLoopBody2", 1);

        Statement s11 = UpperTriangleLoopBody2.methodBody().statements().get(11);
        assertEquals("Loop.run(ldIn1);", s11.toString());
        VariableData vd11 = VariableDataImpl.of(s11);
        VariableInfo viLdIn11 = vd11.variableInfo("ldIn1");
        assertTrue(viLdIn11.isModified());
        VariableInfo viM = vd11.variableInfo("m");
        assertTrue(viM.isModified());
    }

    private void testUpperTriangleLoopBody1(TypeInfo X) {
        MethodInfo UpperTriangleLoopBody1 = X.findUniqueMethod("UpperTriangleLoopBody1", 1);
        {
            // the @GetSet downcast read: 'matrix = (float[][]) ld.get(1)' must link matrix into the slot array
            Statement s1 = UpperTriangleLoopBody1.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1M = vd1.variableInfo("matrix");
            assertEquals("matrix.§m≡0:ld.variables[1].§m,matrix∈0:ld.variables,matrix←0:ld.variables[1]", vi1M.linkedVariables().toString());
        }
        IfElseStatement s700 = (IfElseStatement) UpperTriangleLoopBody1.methodBody().statements().get(7)
                .block().statements().getFirst();
        {
            // the @GetSet store: builder.set(pos, value) translates to a slot-array assignment
            LocalVariableCreation s70010 = (LocalVariableCreation) s700.elseBlock().statements().getFirst();
            assertEquals("""
                    (new Builder().variables[0]=col,\
                    new Builder().variables[1]=matrix,\
                    new Builder().variables[2]=temp,\
                    new Builder().variables[3]=v,\
                    new Builder().loop=IntStream.iterate(0,c->c<tms,c->c+1).iterator(),\
                    new Builder().body=X::UpperTriangleLoopBody,\
                    new Builder()).build()\
                    """, s70010.localVariable().assignmentExpression().translate(new ApplyGetSetTranslation(runtime))
                    .toString());
        }
        {
            Statement s70011 = s700.elseBlock().statements().get(1);
            assertEquals("Loop.run(ldIn);", s70011.toString());

            // this should trigger ld.variables[1] to be modified
            VariableData vd70011 = VariableDataImpl.of(s70011);
            VariableInfo viLdIn = vd70011.variableInfo("ldIn");
            assertTrue(viLdIn.isModified());

            VariableInfo viLd1 = vd70011.variableInfo(
                    "a.b.Loop.LoopData.variables#a.b.X.UpperTriangleLoopBody1(a.b.Loop.LoopData):0:ld[1]");
            assertTrue(viLd1.isModified());

            VariableInfo viMatrix = vd70011.variableInfo("matrix");
            assertTrue(viMatrix.isModified());
        }
    }

    private static void testUpperTriangleLoopBody(TypeInfo X) {
        MethodInfo UpperTriangleLoopBody = X.findUniqueMethod("UpperTriangleLoopBody", 1);
        ParameterInfo ld = UpperTriangleLoopBody.parameters().getFirst();

        assertTrue(ld.isModified());
        MethodLinkedVariables mlv = UpperTriangleLoopBody.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                a.b.X.UpperTriangleLoopBody(a.b.Loop.LoopData):0:ld, ld.variables, ld.variables[1]\
                """, mlv.sortedModifiedString());
    }
}
