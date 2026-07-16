package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for notes/getset-list-element-getter-npe.md, openjdk (stdbase) path. The JDK's java.util.List is
 * inspected from jmod bytecode, which carries no physical {@code @GetSet}. CreateSyntheticFieldsForGetSet must
 * still create the {@code _synthetic_list} element field and set List.get(int)'s GET_SET_FIELD, pre-commit,
 * keyed on the interface FQN -- otherwise RuntimeImpl.getSetVariable NPEs when resolving an indexed list-getter
 * call. A refactor (a54eb89c) had dropped that hard-coded watcher; this pins it back.
 */
public class TestListSyntheticGetSet extends CommonTest {

    // preload java.util so java.util.List is fully loaded (LOAD_MEMBERS), which is what runs
    // createSyntheticFields on it; a bare scan only partially resolves List (enough for get()) and never does.
    public TestListSyntheticGetSet() {
        super(List.of("java.base::java.util"));
    }

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class X {
              private final List<Integer> myList = new ArrayList<>();
              public Integer get(int i) { return myList.get(i); }
            }
            """;

    @Test
    public void listGetHasSyntheticField() {
        TypeInfo X = scan("a.b.X", INPUT);
        FieldInfo myList = X.getFieldByName("myList", true);
        TypeInfo list = myList.type().typeInfo();
        assertEquals("java.util.List", list.fullyQualifiedName());

        MethodInfo listGet = list.findUniqueMethod("get", 1);
        Value.FieldValue getField = listGet.getSetField();
        assertNotNull(getField.field(), "java.util.List.get(int) must carry a synthetic GetSet field");
        assertEquals("_synthetic_list", getField.field().name());
        assertFalse(getField.setter());
        assertEquals(0, getField.parameterIndexOfIndex());
        assertTrue(getField.list());

        MethodInfo listSet = list.findUniqueMethod("set", 2);
        Value.FieldValue setField = listSet.getSetField();
        assertNotNull(setField.field(), "java.util.List.set(int,Object) must carry a synthetic GetSet field");
        assertEquals("_synthetic_list", setField.field().name(), "get and set share one _synthetic_list field");
        assertTrue(setField.setter());
        assertTrue(setField.list());
    }

    /** Gate off (ParseOptions.syntheticListField == false): no _synthetic_list field, leaner model. */
    @Test
    public void gateOffSuppressesSyntheticField() {
        syntheticListField = false; // mirrors JavaInspector.ParseOptions.Builder().setSyntheticListField(false)
        TypeInfo X = scan("a.b.X", INPUT);
        TypeInfo list = X.getFieldByName("myList", true).type().typeInfo();
        assertEquals("java.util.List", list.fullyQualifiedName());

        MethodInfo listGet = list.findUniqueMethod("get", 1);
        assertNull(listGet.getSetField().field(), "gate off: java.util.List.get must have no synthetic field");
        assertTrue(list.fields().stream().noneMatch(f -> "_synthetic_list".equals(f.name())),
                "gate off: no _synthetic_list field on java.util.List");
    }
}
