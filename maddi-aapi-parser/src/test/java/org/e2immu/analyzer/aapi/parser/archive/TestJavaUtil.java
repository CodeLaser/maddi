package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.ToolChain;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.*;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.FieldValue;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.Independent;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.NO_VALUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaUtil extends CommonTest {

    @Test
    public void testArrayList() {
        TypeInfo typeInfo = compiledTypesManager().get(ArrayList.class);
        assertFalse(typeInfo.isInterface());
        assertFalse(typeInfo.isAtLeastImmutableHC());
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));

        HiddenContentTypes hct = typeInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=E", hct.detailedSortedTypes());
    }

    @Test
    public void testArrayListCollectionConstructor() {
        TypeInfo typeInfo = compiledTypesManager().get(ArrayList.class);
        TypeInfo collectionTypeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findConstructor(collectionTypeInfo);
        assertEquals("java.util.ArrayList.<init>(java.util.Collection<? extends E>)", methodInfo.fullyQualifiedName());
        HiddenContentTypes methodHct = methodInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=E - 1=Collection", methodHct.detailedSortedTypes());
        assertEquals("ArrayList:E - <init>:Collection", methodHct.toString());

        ParameterInfo p0 = methodInfo.parameters().get(0);
        HiddenContentSelector paramHcs = p0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", paramHcs.detailed());
    }


    @Test
    public void testArraysEquals() {
        TypeInfo typeInfo = compiledTypesManager().get(Arrays.class);
        MethodInfo methodInfo = typeInfo.methodStream()
                .filter(mi -> mi.parameters().size() == 2
                              && "equals".equals(mi.name())
                              && mi.parameters().getFirst().parameterizedType().typeInfo().equals(runtime().longParameterizedType().typeInfo()))
                .findFirst().orElseThrow();
        assertEquals("java.util.Arrays.equals(long[],long[])", methodInfo.fullyQualifiedName());
        assertFalse(methodInfo.isModifying());

        ParameterInfo add0 = methodInfo.parameters().get(0);
        assertFalse(add0.isModified());
        ParameterInfo add1 = methodInfo.parameters().get(1);
        assertFalse(add1.isModified());
    }

    @Test
    public void testArraysDeepToString() {
        TypeInfo typeInfo = compiledTypesManager().get(Arrays.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("deepToString", 1);
        assertFalse(methodInfo.isModifying());

        ParameterInfo add0 = methodInfo.parameters().getFirst();
        assertFalse(add0.isModified());
    }

    @Test
    public void testCollection() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        assertTrue(typeInfo.isInterface());
        assertFalse(typeInfo.isAtLeastImmutableHC());
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    @Test
    public void testCollectionAdd() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("add", 1);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());

        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        ParameterInfo add0 = methodInfo.parameters().get(0);
        assertFalse(add0.isModified());
    }


    @Test
    public void testCollectionSize() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("size", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
    }

    @Test
    public void testCollectionIterator() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("iterator", 0);
        assertEquals("[java.lang.Iterable.iterator()]", methodInfo.overrides().toString());
        assertFalse(methodInfo.allowsInterrupts());

        assertFalse(methodInfo.isModifying());
        assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }


    @Test
    public void testIterableIterator() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterable.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("iterator", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());

        assertFalse(methodInfo.isModifying());
        assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }


    @Test
    public void testCollectionRemove() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("remove", 1);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());

        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        ParameterInfo add0 = methodInfo.parameters().get(0);
        assertFalse(add0.isModified());
    }

    @Test
    public void testCollectionStream() {
        TypeInfo typeInfo = compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("stream", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());

        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(CONTENT_NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
    }

    @Test
    public void testCollections() {
        TypeInfo typeInfo = compiledTypesManager().get(Collections.class);
        assertFalse(typeInfo.isInterface());
        // NOTE at the moment: @UtilityClass does not enforce @Immutable (without hc)
        assertSame(IMMUTABLE_HC, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    @Test
    public void testCollectionsAddAll() {
        TypeInfo typeInfo = compiledTypesManager().get(Collections.class);
        MethodInfo addAll = typeInfo.findUniqueMethod("addAll", 2);
        assertEquals(" - 0=T, 1=Collection", addAll.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE)
                .detailedSortedTypes());

        ParameterInfo p0 = addAll.parameters().get(0);
        assertTrue(p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE).isFalse());
        Value.Independent independent0 = p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT);
        Map<Integer, Integer> map = independent0.linkToParametersReturnValue();
        assertEquals(1, map.size());
        assertEquals(1, map.get(1)); // links at HC level (1) to parameter with index 1
        assertEquals("0=0,1=*", p0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());

        ParameterInfo p1 = addAll.parameters().get(1);
        assertTrue(p1.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE).isTrue());
        Value.Independent independent1 = p1.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT);
        assertTrue(independent1.linkToParametersReturnValue().isEmpty());
        assertEquals("0=0", p1.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());
    }

    @Test
    public void testList() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        assertTrue(typeInfo.isInterface());
        assertFalse(typeInfo.isAtLeastImmutableHC());
        ParameterizedType collection = typeInfo.interfacesImplemented().get(0);
        String expectImplemented = ToolChain.currentJdkMainVersion() < 21
                ? "Type java.util.Collection<E>"
                : "Type java.util.SequencedCollection<E>";
        assertEquals(expectImplemented, collection.toString());
        assertEquals("E=TP#0 in List", collection.parameters().get(0).typeParameter().toString());

        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));

        assertEquals("List:E", typeInfo.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).toString());
    }

    @Test
    public void testListAdd() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo add = typeInfo.findUniqueMethod("add", 1);
        assertEquals(1, add.overrides().size());
        MethodInfo override = add.overrides().stream().findFirst().orElseThrow();
        assertEquals("java.util.Collection.add(E)", override.fullyQualifiedName());
        assertSame(FALSE, add.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));
    }


    @Test
    public void testListRemoveInt() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("remove", runtime().intTypeInfo());
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());

        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        ParameterInfo remove0 = methodInfo.parameters().get(0);
        assertFalse(remove0.isModified());

        assertNull(methodInfo.getSetField().field());
    }

    @Test
    public void testListRemoveObject() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("remove", runtime().objectTypeInfo());
        assertEquals("[java.util.Collection.remove(Object)]", methodInfo.overrides().toString());
        assertFalse(methodInfo.allowsInterrupts());

        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        ParameterInfo remove0 = methodInfo.parameters().get(0);
        assertFalse(remove0.isModified());
    }

    @Test
    public void testListGet() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        assertTrue(get.overrides().isEmpty());
        assertSame(TRUE, get.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));

        FieldValue getField = get.getSetField();
        assertEquals("java.util.List._synthetic_list", getField.field().fullyQualifiedName());
        assertFalse(getField.setter());
    }


    @Test
    public void testListSet() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo set = typeInfo.findUniqueMethod("set", 2);
        assertTrue(set.overrides().isEmpty());
        assertSame(FALSE, set.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));

        FieldValue setField = set.getSetField();
        assertEquals("java.util.List._synthetic_list", setField.field().fullyQualifiedName());
        assertEquals(0, setField.parameterIndexOfIndex());
        assertEquals(1, setField.parameterIndexOfValue());
        assertTrue(setField.setter());
    }

    @Test
    public void testListOf() {
        TypeInfo typeInfo = compiledTypesManager().get(List.class);
        MethodInfo of = typeInfo.methods().stream()
                .filter(m -> "of".equals(m.name()) && 1 == m.parameters().size() && 0 == m.parameters().getFirst().parameterizedType().arrays())
                .findFirst().orElseThrow();
        assertTrue(of.overrides().isEmpty());
        assertTrue(of.isNonModifying());
        ParameterInfo e = of.parameters().getFirst();
        Independent independent = e.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT);
        assertEquals(1, independent.linkToParametersReturnValue().get(-1));
    }


    @Test
    public void testArrayListGet() {
        TypeInfo typeInfo = compiledTypesManager().get(ArrayList.class);
        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        assertFalse(get.overrides().isEmpty());
        assertSame(TRUE, get.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));

        // inherited from AAPI
        FieldValue getField = get.getSetField();
        assertEquals("java.util.List._synthetic_list", getField.field().fullyQualifiedName());
        assertFalse(getField.setter());
    }

    @Test
    public void testArrayListSet() {
        TypeInfo typeInfo = compiledTypesManager().get(ArrayList.class);
        MethodInfo set = typeInfo.findUniqueMethod("set", 2);
        assertFalse(set.overrides().isEmpty());
        assertSame(FALSE, set.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));

        // inherited from AAPI
        FieldValue setField = set.getSetField();
        assertEquals("java.util.List._synthetic_list", setField.field().fullyQualifiedName());
        assertEquals(0, setField.parameterIndexOfIndex());
        assertEquals(1, setField.parameterIndexOfValue());
        assertTrue(setField.setter());
    }

    @Test
    public void testLinkedListGet() {
        TypeInfo typeInfo = compiledTypesManager().get(LinkedList.class);
        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        assertFalse(get.overrides().isEmpty());
        assertSame(TRUE, get.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE));

        // inherited from AAPI
        assertEquals("java.util.List._synthetic_list", get.getSetField().field().fullyQualifiedName());
    }


    @Test
    public void testLinkedListConstructor() {
        TypeInfo typeInfo = compiledTypesManager().get(LinkedList.class);
        MethodInfo constructor = typeInfo.findConstructor(compiledTypesManager().get(Collection.class));
        assertTrue(constructor.overrides().isEmpty());
        ParameterInfo p0 = constructor.parameters().getFirst();
        assertSame(TRUE, p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
    }

    @Test
    public void testAbstractListGet() {
        TypeInfo typeInfo = compiledTypesManager().get(AbstractList.class);
        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        assertEquals("java.util.List.get(int)",
                get.overrides().stream().map(Objects::toString).sorted().collect(Collectors.joining(",")));
        assertFalse(get.isModifying());
        // inherited from AAPI
        assertEquals("java.util.List._synthetic_list", get.getSetField().field().fullyQualifiedName());
    }

    // Vector implements List, but also extends AbstractList, AbstractCollection

    @Test
    public void testVectorGet() {
        TypeInfo typeInfo = compiledTypesManager().get(Vector.class);
        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        assertEquals("java.util.Vector.get(int)", get.fullyQualifiedName());
        assertEquals("java.util.AbstractList.get(int),java.util.List.get(int)",
                get.overrides().stream().map(Objects::toString).sorted().collect(Collectors.joining(",")));
        assertFalse(get.isModifying());
    }

    @Test
    public void testAbstractCollectionSize() {
        TypeInfo typeInfo = compiledTypesManager().get(AbstractCollection.class);
        MethodInfo get = typeInfo.findUniqueMethod("size", 0);
        assertEquals("java.util.Collection.size()",
                get.overrides().stream().map(Objects::toString).sorted().collect(Collectors.joining(",")));
        assertFalse(get.isModifying());
    }

    @Test
    public void testVectorSize() {
        TypeInfo typeInfo = compiledTypesManager().get(Vector.class);
        MethodInfo get = typeInfo.findUniqueMethod("size", 0);
        assertEquals("java.util.AbstractCollection.size(),java.util.Collection.size(),java.util.List.size()",
                get.overrides().stream().map(Objects::toString).sorted().collect(Collectors.joining(",")));
        assertFalse(get.isModifying());
    }

    @Test
    public void testHashMap() {
        TypeInfo typeInfo = compiledTypesManager().get(HashMap.class);
        assertFalse(typeInfo.isInterface());
        assertFalse(typeInfo.isAtLeastImmutableHC());

        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));

        assertEquals("HashMap:K, V",
                typeInfo.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).toString());
    }


    @Test
    public void testHashMapEntryIterator() {
        TypeInfo typeInfo = compiledTypesManager().get(HashMap.class);
        TypeInfo sub = typeInfo.findSubType("EntryIterator");
        assertEquals("0=K, 1=V",
                sub.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).detailedSortedTypes());
    }

    @Test
    public void testMapPut() {
        TypeInfo typeInfo = compiledTypesManager().get(Map.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("put", 2);
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().get(0);
        assertFalse(p0.isModified());
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));

        ParameterInfo p1 = methodInfo.parameters().get(1);
        assertFalse(p1.isModified());
        assertSame(NULLABLE, p1.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(IMMUTABLE_HC, p1.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(INDEPENDENT_HC, p1.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
    }

    @Test
    public void testMapGetOrDefault() {
        TypeInfo typeInfo = compiledTypesManager().get(Map.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("getOrDefault", 2);
        assertFalse(methodInfo.isStatic());
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(INDEPENDENT, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));

        ParameterInfo p1 = methodInfo.parameters().get(1);
        assertFalse(p1.isModified());
        assertSame(NULLABLE, p1.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(IMMUTABLE_HC, p1.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(INDEPENDENT, p1.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
    }


    @Test
    public void testMapCopyOf() {
        TypeInfo typeInfo = compiledTypesManager().get(Map.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("copyOf", 1);
        assertTrue(methodInfo.isStatic());
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(MUTABLE, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
    }

    @Test
    public void testMapKeySetEntrySetValues() {
        TypeInfo typeInfo = compiledTypesManager().get(Map.class);
        for (String name : new String[]{"keySet", "values", "entrySet"}) {
            MethodInfo methodInfo = typeInfo.findUniqueMethod(name, 0);
            assertTrue(methodInfo.overrides().isEmpty());
            assertFalse(methodInfo.isModifying());
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
            assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
            assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        }
    }

    @Test
    public void testTreeMapFirstEntry() {
        TypeInfo typeInfo = compiledTypesManager().get(TreeMap.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("firstEntry", 0);
        int expectOverrides = ToolChain.currentJdkMainVersion() < 21 ? 1 : 2;
        assertEquals(expectOverrides, methodInfo.overrides().size());
        Set<String> overrideNames = methodInfo.overrides().stream()
                .map(MethodInfo::fullyQualifiedName).collect(Collectors.toUnmodifiableSet());
        assertTrue(overrideNames.contains("java.util.NavigableMap.firstEntry()"));
        assertFalse(methodInfo.isModifying());
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testMapEntry() {
        TypeInfo typeInfo = compiledTypesManager().get(Map.Entry.class);
        assertTrue(typeInfo.isInterface());
        assertFalse(typeInfo.isAtLeastImmutableHC());
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    @Test
    public void testSortedMapValues() {
        TypeInfo typeInfo = compiledTypesManager().get(SortedMap.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("values", 0);
        assertEquals(1, methodInfo.overrides().size());
        MethodInfo override = methodInfo.overrides().stream().findFirst().orElseThrow();
        assertEquals("java.util.Map.values()", override.fullyQualifiedName());
        assertFalse(methodInfo.isModifying());
        assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }

    @Test
    public void testHashSetConstructor1() {
        TypeInfo collection = compiledTypesManager().get(Collection.class);
        TypeInfo typeInfo = compiledTypesManager().get(HashSet.class);
        MethodInfo methodInfo = typeInfo.findConstructor(collection);
        ParameterInfo p0 = methodInfo.parameters().get(0);
        assertFalse(p0.isModified());
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
    }

    @Test
    public void testHashMapConstructor1() {
        TypeInfo map = compiledTypesManager().get(Map.class);
        TypeInfo typeInfo = compiledTypesManager().get(HashMap.class);
        MethodInfo methodInfo = typeInfo.findConstructor(map);
        ParameterInfo p0 = methodInfo.parameters().get(0);
        assertFalse(p0.isModified());
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
    }

    @Test
    public void testObjectsRequireNonNull() {
        TypeInfo typeInfo = compiledTypesManager().get(Objects.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("requireNonNull", 1);
        assertTrue(methodInfo.isIdentity());
        assertFalse(methodInfo.isFluent());
        assertFalse(methodInfo.isModifying());
        assertTrue(methodInfo.isStatic());

        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
    }

    @Test
    public void testObjectsHash() {
        TypeInfo typeInfo = compiledTypesManager().get(Objects.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("hash", 1);
        assertEquals("java.util.Objects.hash(Object...)", methodInfo.fullyQualifiedName());
        assertTrue(methodInfo.isStatic());
        assertFalse(methodInfo.isIdentity());
        assertFalse(methodInfo.isFluent());
        assertFalse(methodInfo.isModifying());

        // int
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(TRUE, p0.analysis().getOrDefault(CONTAINER_PARAMETER, FALSE));
    }


    @Test
    public void testIterator() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterator.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    @Test
    public void testIteratorNext() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterator.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("next", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isModifying());

        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        assertEquals("Iterator:E - next:", methodInfo.analysis()
                .getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE).toString());
        assertEquals("*", methodInfo.analysis().getOrDefault(HCS_METHOD, NONE).toString());
    }

    @Test
    public void testIteratorHasNext() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterator.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("hasNext", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isModifying());

        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testIteratorRemove() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterator.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("remove", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isModifying());

        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(NO_VALUE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testIteratorForEachRemaining() {
        TypeInfo typeInfo = compiledTypesManager().get(Iterator.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("forEachRemaining", 1);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isDefault());
        assertTrue(methodInfo.isModifying());

        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(NO_VALUE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertTrue(p0.isModified());
        assertTrue(p0.isIgnoreModifications());
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
    }

    @Test
    public void testRandomNextInt() {
        TypeInfo typeInfo = compiledTypesManager().get(Random.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("nextInt", 0);
        assertFalse(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertTrue(methodInfo.isModifying());

        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testRandomNextBytes() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Random.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("nextBytes", 1);
        assertTrue(methodInfo.isModifying());

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isUnmodified());
    }


    @Test
    public void testOptional() {
        TypeInfo typeInfo = compiledTypesManager().get(Optional.class);
        assertSame(IMMUTABLE_HC, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    @Test
    public void testOptionalEmpty() {
        TypeInfo typeInfo = compiledTypesManager().get(Optional.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("empty", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isStatic());
        assertFalse(methodInfo.isModifying());

        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testOptionalGet() {
        TypeInfo typeInfo = compiledTypesManager().get(Optional.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("get", 0);
        assertTrue(methodInfo.overrides().isEmpty());
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertFalse(methodInfo.isModifying());

        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }


    @Test
    public void testTreeMap() {
        TypeInfo typeInfo = compiledTypesManager().get(TreeMap.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));

        String expect = ToolChain.currentJdkMainVersion() < 21
                ? "AbstractMap,Cloneable,Map,NavigableMap,Serializable,SortedMap"
                : "AbstractMap,Cloneable,Map,NavigableMap,SequencedMap,Serializable,SortedMap";
        assertEquals(expect,
                typeInfo.superTypesExcludingJavaLangObject().stream()
                        .map(TypeInfo::simpleName).sorted().collect(Collectors.joining(",")));
    }

}
