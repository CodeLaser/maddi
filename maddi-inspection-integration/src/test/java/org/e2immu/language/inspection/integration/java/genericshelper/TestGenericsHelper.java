package org.e2immu.language.inspection.integration.java.genericshelper;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGenericsHelper extends CommonTest {

    @DisplayName("recursion in translateMap")
    @Test
    public void test1() {
        TypeInfo list = javaInspector.compiledTypesManager().type(List.class);
        Runtime runtime = javaInspector.runtime();
        ParameterizedType listListFormal = runtime.newParameterizedType(list, List.of(list.asParameterizedType()));
        assertEquals("Type java.util.List<java.util.List<E>>", listListFormal.toString());
        ParameterizedType listListString = runtime.newParameterizedType(list,
                List.of(runtime.newParameterizedType(list, List.of(runtime.stringParameterizedType()))));
        assertEquals("Type java.util.List<java.util.List<String>>", listListString.toString());
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        var map = genericsHelper.translateMap(listListFormal, listListString, false);
        assertEquals("{E=TP#0 in List=Type String}", map.toString());
    }

    @DisplayName("basic translateMap")
    @Test
    public void test2() {
        TypeInfo list = javaInspector.compiledTypesManager().type(List.class);
        Runtime runtime = javaInspector.runtime();
        ParameterizedType listFormal = list.asParameterizedType();
        assertEquals("Type java.util.List<E>", listFormal.toString());
        ParameterizedType listString = runtime.newParameterizedType(list, List.of(runtime.stringParameterizedType()));
        assertEquals("Type java.util.List<String>", listString.toString());
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        var map = genericsHelper.translateMap(listFormal, listString, false);
        assertEquals("{E=TP#0 in List=Type String}", map.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;

            import java.util.List;
            import java.util.Map;

            class X {
                interface Processor {
                }

                // a functional interface whose single abstract method has the interface itself,
                // over its own type parameter, in a parameter type: Factory<T> -> Map<String, Factory<T>>
                interface Factory<T extends Processor> {
                    T create(Map<String, Factory<T>> factories, String tag);
                }

                static class Sub implements Processor {
                }

                static <T extends Processor> List<T> read(Map<String, Factory<T>> factories) {
                    return List.of();
                }
            }
            """;

    @DisplayName("self-referential functional interface: Factory<T>'s SAM takes Map<String, Factory<T>>")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        TypeInfo factory = X.findSubType("Factory");
        TypeInfo sub = X.findSubType("Sub");
        MethodInfo read = X.findUniqueMethod("read", 1);

        // formal: Map<String, X.Factory<T>>, T being read's own type parameter
        ParameterizedType formal = read.parameters().getFirst().parameterizedType();
        assertEquals("Type java.util.Map<String,a.b.X.Factory<T extends a.b.X.Processor>>", formal.toString());

        // concrete: Map<String, X.Factory<X.Sub>>, as at a call site read(subFactories)
        TypeInfo map = javaInspector.compiledTypesManager().type(Map.class);
        ParameterizedType concrete = runtime.newParameterizedType(map, List.of(
                runtime.stringParameterizedType(),
                runtime.newParameterizedType(factory, List.of(sub.asSimpleParameterizedType()))));
        assertEquals("Type java.util.Map<String,a.b.X.Factory<a.b.X.Sub>>", concrete.toString());

        // the direction the link engine uses (VirtualFieldTranslationMapForMethodParameters)
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        var translation = genericsHelper.translateMap(formal, concrete, true);
        // before the fix at GenericsHelperImpl:211-219, this call did not return at all: StackOverflowError
        assertEquals("{T=TP#0 in Factory=Type a.b.X.Sub, T=TP#0 in X.read=Type a.b.X.Sub}",
                translation.toString());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;

            import java.util.List;
            import java.util.Map;

            class X {
                interface Processor {
                }

                interface Factory<T extends Processor> {
                    T create(Map<String, Factory<T>> factories, String tag);
                }

                static class Sub implements Processor {
                }

                static <T extends Processor> List<T> read(Map<String, Factory<T>> factories) {
                    return List.of();
                }

                List<Sub> go(Map<String, Factory<Sub>> subFactories) {
                    return read(subFactories);
                }
            }
            """;

    // INPUT3's shape plus the call site that resolves it. MethodResolutionImpl:574 reaches
    // translateMap(formal, concrete, true) with the same pair the link engine uses, so this shape was
    // in reach of the same non-terminating recursion at PARSE time -- i.e. it could take out a day-zero
    // parse, not just the link/solver verbs. This test is the witness for that claim.
    @DisplayName("self-referential functional interface: resolving the call site parses")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        MethodInfo go = X.findUniqueMethod("go", 1);
        assertEquals("Type java.util.List<a.b.X.Sub>", go.returnType().toString());
    }
}
