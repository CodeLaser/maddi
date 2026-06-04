package org.e2immu.language.java.openjdk.genericshelper;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGenericsHelper extends CommonTest {

    @DisplayName("recursion in translateMap")
    @Test
    public void test1() {
        // Original: javaInspector.compiledTypesManager().getOrLoad(List.class) — loads java.util.List
        // from bytecode without needing a prior scan. Replaced with classSymbolScanner.getType() after
        // a minimal scan that causes javac to resolve java.util.List.
        scan("a.b.X", "package a.b; import java.util.List; class X { List<List<String>> field; }");
        TypeInfo list = classSymbolScanner.getType("java.util.List");
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
        // Same replacement as test1: compiledTypesManager().getOrLoad(List.class) → scan + classSymbolScanner.
        scan("a.b.X", "package a.b; import java.util.List; class X { List<String> field; }");
        TypeInfo list = classSymbolScanner.getType("java.util.List");
        ParameterizedType listFormal = list.asParameterizedType();
        assertEquals("Type java.util.List<E>", listFormal.toString());
        ParameterizedType listString = runtime.newParameterizedType(list, List.of(runtime.stringParameterizedType()));
        assertEquals("Type java.util.List<String>", listString.toString());
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        var map = genericsHelper.translateMap(listFormal, listString, false);
        assertEquals("{E=TP#0 in List=Type String}", map.toString());
    }
}
