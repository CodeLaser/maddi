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

package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.type.IsAssignableFrom2;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

import static org.junit.jupiter.api.Assertions.*;

public class TestIsAssignableFrom2 extends CommonTest {

    @Test
    public void test() {
        TypeInfo closeable = javaInspector.compiledTypesManager().getOrLoad(Closeable.class);
        assertEquals("java.io.Closeable", closeable.fullyQualifiedName());
        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertEquals("java.lang.Iterable", iterable.fullyQualifiedName());
        ParameterizedType closeablePt = runtime.newParameterizedType(closeable, 0);
        ParameterizedType iterableCloseable = runtime.newParameterizedType(iterable, List.of(closeablePt));
        assertEquals("Iterable<java.io.Closeable>", iterableCloseable.fullyQualifiedName());
        ParameterizedType closeableArray = runtime.newParameterizedType(closeable, 1);
        assertEquals("java.io.Closeable[]", closeableArray.fullyQualifiedName());

        // there is no special code in IsAssignableFrom to make this work!
        // there is, however, special code in HiddenContentSelector to deal with the 'transfer'
        // from array base to type parameter
        assertFalse(runtime.isAssignableFrom(iterableCloseable, closeableArray));
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                interface Parent<T extends Parent<?>> {
                }
                interface Child extends Parent<Child> {
                }
            }
            """;

    static class X {
        interface Parent<T extends Parent<?>> {
        }
        interface Child extends Parent<Child> {
        }
    }

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        TypeInfo parent = X.findSubType("Parent");
        TypeInfo child = X.findSubType("Child");
        assertTrue(X.Parent.class.isAssignableFrom(X.Child.class));
        assertTrue(runtime.isAssignableFrom(parent.asParameterizedType(), child.asParameterizedType()));
    }

    @Test
    public void test3() {
        ParameterizedType stringPt = runtime.stringParameterizedType();
        TypeInfo longPredicate = javaInspector.compiledTypesManager().get(LongPredicate.class);
        assertNotNull(longPredicate);
        ParameterizedType longPredicatePt = longPredicate.asParameterizedType();
        assertFalse(runtime.isAssignableFrom(longPredicatePt, stringPt));
        assertFalse(runtime.isAssignableFrom(stringPt, longPredicatePt));

        TypeInfo comparable = javaInspector.compiledTypesManager().get(Comparable.class);
        ParameterizedType erasedComparable = comparable.asSimpleParameterizedType();
        assertFalse(runtime.isAssignableFrom(erasedComparable, longPredicatePt));
        assertFalse(runtime.isAssignableFrom(longPredicatePt, erasedComparable));
    }

    @Test
    public void test4() {
        TypeInfo longPredicate = javaInspector.compiledTypesManager().get(LongPredicate.class);
        assertNotNull(longPredicate);
        TypeInfo longConsumer = javaInspector.compiledTypesManager().get(LongConsumer.class);
        assertNotNull(longConsumer);
        ParameterizedType longPredicatePt = longPredicate.asSimpleParameterizedType();
        ParameterizedType longConsumerPt = longConsumer.asSimpleParameterizedType();
        assertFalse(runtime.isAssignableFrom(longPredicatePt, longConsumerPt));

        // LongPredicate and LongConsumer are unrelated types. They are "SAM-compatible" in the sense that
        // the same lambda may target either, but that is not an assignment conversion (JLS 5.2):
        // 'LongConsumer c = someLongPredicate;' does not compile. So neither is assignable from the other.
        assertFalse(runtime.isAssignableFrom(longConsumerPt, longPredicatePt));
        assertFalse(runtime.isAssignableFrom(longPredicatePt, longConsumerPt));
    }

    @Test
    public void test5() {
        ParameterizedType longPt = runtime.longParameterizedType();
        ParameterizedType intPt = runtime.intParameterizedType();
        ParameterizedType boxedLongPt = runtime.boxedLongTypeInfo().asParameterizedType();
        ParameterizedType integerPt = runtime.integerTypeInfo().asParameterizedType();

        ParameterizedType objectPt = runtime.objectParameterizedType();
        ParameterizedType objectPt1 = runtime.objectParameterizedType().copyWithArrays(1);
        ParameterizedType stringPt = runtime.stringParameterizedType();
        ParameterizedType stringPt1 = runtime.stringParameterizedType().copyWithArrays(1);
        ParameterizedType nullPt = runtime.newNullConstant(List.of(), runtime.noSource()).parameterizedType();

        assertTrue(runtime.isAssignableFrom(longPt, longPt));
        assertTrue(runtime.isAssignableFrom(longPt, intPt));
        assertFalse(runtime.isAssignableFrom(intPt, longPt));
        assertFalse(runtime.isAssignableFrom(intPt, nullPt));
        assertFalse(runtime.isAssignableFrom(nullPt, intPt));

        assertTrue(runtime.isAssignableFrom(longPt, boxedLongPt));
        assertTrue(runtime.isAssignableFrom(boxedLongPt, longPt));
        assertFalse(runtime.isAssignableFrom(intPt, boxedLongPt));
        assertFalse(runtime.isAssignableFrom(boxedLongPt, intPt));
        assertFalse(runtime.isAssignableFrom(boxedLongPt, integerPt));

        assertTrue(runtime.isAssignableFrom(objectPt, nullPt));
        assertFalse(runtime.isAssignableFrom(nullPt, objectPt));
        assertTrue(runtime.isAssignableFrom(objectPt, stringPt));

        assertTrue(runtime.isAssignableFrom(objectPt, objectPt));

        assertFalse(runtime.isAssignableFrom(stringPt1, stringPt));
        assertFalse(runtime.isAssignableFrom(stringPt, stringPt1));

        assertTrue(runtime.isAssignableFrom(objectPt, objectPt1));
        assertFalse(runtime.isAssignableFrom(objectPt1, objectPt));
    }

    // convenience: test IsAssignableFrom2 directly (runtime.isAssignableFrom routes here too,
    // but pt.isAssignableFrom(runtime, ...) still uses the old IsAssignableFrom)
    private boolean assignable(ParameterizedType to, ParameterizedType from) {
        return new IsAssignableFrom2(runtime).test(to, from);
    }

    // testPrimitiveWidening: only widening is allowed, and only between primitives
    @Test
    public void testPrimitiveWidening() {
        ParameterizedType bytePt = runtime.byteParameterizedType();
        ParameterizedType shortPt = runtime.shortParameterizedType();
        ParameterizedType charPt = runtime.charParameterizedType();
        ParameterizedType intPt = runtime.intParameterizedType();
        ParameterizedType longPt = runtime.longParameterizedType();
        ParameterizedType floatPt = runtime.floatParameterizedType();
        ParameterizedType doublePt = runtime.doubleParameterizedType();
        ParameterizedType booleanPt = runtime.booleanParameterizedType();

        // widening up the chain byte -> short -> int -> long -> float -> double, and char -> int
        assertTrue(assignable(shortPt, bytePt));
        assertTrue(assignable(intPt, bytePt));
        assertTrue(assignable(intPt, charPt));
        assertTrue(assignable(intPt, shortPt));
        assertTrue(assignable(longPt, intPt));
        assertTrue(assignable(floatPt, longPt));
        assertTrue(assignable(doublePt, floatPt));
        assertTrue(assignable(doublePt, intPt));

        // no narrowing
        assertFalse(assignable(bytePt, shortPt));
        assertFalse(assignable(intPt, longPt));
        assertFalse(assignable(charPt, intPt));
        assertFalse(assignable(shortPt, charPt));

        // boolean does not widen, and primitive widening never crosses to/from non-numeric
        assertFalse(assignable(intPt, booleanPt));
        assertFalse(assignable(doublePt, booleanPt));
    }

    // testBoxUnbox: a primitive is assignable from its own boxed type and vice versa, exact match only
    @Test
    public void testBoxUnbox() {
        ParameterizedType intPt = runtime.intParameterizedType();
        ParameterizedType longPt = runtime.longParameterizedType();
        ParameterizedType charPt = runtime.charParameterizedType();
        ParameterizedType booleanPt = runtime.booleanParameterizedType();
        ParameterizedType doublePt = runtime.doubleParameterizedType();
        ParameterizedType integerPt = runtime.integerTypeInfo().asParameterizedType();
        ParameterizedType boxedLongPt = runtime.boxedLongTypeInfo().asParameterizedType();
        ParameterizedType characterPt = runtime.characterTypeInfo().asParameterizedType();
        ParameterizedType boxedBooleanPt = runtime.boxedBooleanTypeInfo().asParameterizedType();
        ParameterizedType numberPt = javaInspector.compiledTypesManager().getOrLoad(Number.class).asParameterizedType();
        ParameterizedType objectPt = runtime.objectParameterizedType();

        // matched primitive <-> wrapper, both directions
        assertTrue(assignable(intPt, integerPt));
        assertTrue(assignable(integerPt, intPt));
        assertTrue(assignable(charPt, characterPt));
        assertTrue(assignable(characterPt, charPt));
        assertTrue(assignable(booleanPt, boxedBooleanPt));
        assertTrue(assignable(boxedBooleanPt, booleanPt));

        // boxing followed by a widening reference conversion (JLS 5.2)
        assertTrue(assignable(numberPt, intPt));   // Number <- int
        assertTrue(assignable(objectPt, intPt));   // Object <- int

        // unboxing followed by a widening primitive conversion (JLS 5.2)
        assertTrue(assignable(longPt, integerPt));   // long   <- Integer
        assertTrue(assignable(doublePt, integerPt)); // double <- Integer

        // no narrowing: int <- Long would require unbox Long->long then narrow long->int
        assertFalse(assignable(intPt, boxedLongPt));   // int  <- Long
        // widening primitive followed by boxing is NOT an assignment conversion
        assertFalse(assignable(boxedLongPt, intPt));   // Long <- int
        // two wrappers are unrelated reference types
        assertFalse(assignable(boxedLongPt, integerPt)); // Long <- Integer
    }

    // testArrayAssignability: covariance, the OCS special cases, and base-type checks
    @Test
    public void testArrays() {
        ParameterizedType object = runtime.objectParameterizedType();
        ParameterizedType string = runtime.stringParameterizedType();
        ParameterizedType stringArray = string.copyWithArrays(1);
        ParameterizedType objectArray = object.copyWithArrays(1);
        ParameterizedType string2D = string.copyWithArrays(2);
        ParameterizedType intArray = runtime.intParameterizedType().copyWithArrays(1);
        ParameterizedType longArray = runtime.longParameterizedType().copyWithArrays(1);

        TypeInfo serializable = javaInspector.compiledTypesManager().getOrLoad(java.io.Serializable.class);
        TypeInfo cloneable = javaInspector.compiledTypesManager().getOrLoad(Cloneable.class);

        // reference array covariance
        assertTrue(assignable(stringArray, stringArray));
        assertTrue(assignable(objectArray, stringArray));
        assertFalse(assignable(stringArray, objectArray));

        // any array is assignable to Object, Serializable, Cloneable (OCS)
        assertTrue(assignable(object, stringArray));
        assertTrue(assignable(serializable.asParameterizedType(), stringArray));
        assertTrue(assignable(cloneable.asParameterizedType(), stringArray));

        // String[][] -> Object[] : recurse one dimension, then String[] -> Object via OCS
        assertTrue(assignable(objectArray, string2D));
        assertFalse(assignable(string2D, objectArray));

        // primitive arrays are invariant: no covariance, no component widening (JLS 4.10.3)
        assertTrue(assignable(intArray, intArray));
        assertFalse(assignable(longArray, intArray));
        assertFalse(assignable(intArray, longArray));

        // the null constant is assignable to any array type
        ParameterizedType nullPt = runtime.newNullConstant(List.of(), runtime.noSource()).parameterizedType();
        assertTrue(assignable(stringArray, nullPt));
        assertTrue(assignable(intArray, nullPt));
    }

    @Language("java")
    public static final String INPUT_HIERARCHY = """
            package a.b;
            class X {
                static class A { }
                static class B extends A { }
                static class C extends B { }
                interface I { }
                interface J extends I { }
                static class D implements J { }
            }
            """;

    // testHierarchy: superclass chain and interface implementation (incl. recursion through super-interfaces)
    @Test
    public void testHierarchy() {
        TypeInfo X = javaInspector.parse(INPUT_HIERARCHY);
        ParameterizedType a = X.findSubType("A").asParameterizedType();
        ParameterizedType b = X.findSubType("B").asParameterizedType();
        ParameterizedType c = X.findSubType("C").asParameterizedType();
        ParameterizedType i = X.findSubType("I").asParameterizedType();
        ParameterizedType j = X.findSubType("J").asParameterizedType();
        ParameterizedType d = X.findSubType("D").asParameterizedType();
        ParameterizedType object = runtime.objectParameterizedType();

        // class chain C extends B extends A
        assertTrue(assignable(a, b));
        assertTrue(assignable(a, c)); // walk two levels up
        assertTrue(assignable(b, c));
        assertFalse(assignable(b, a));
        assertFalse(assignable(c, a));

        // interfaces: D implements J, J extends I
        assertTrue(assignable(j, d));
        assertTrue(assignable(i, d)); // recursion through J
        assertFalse(assignable(d, j));
        assertFalse(assignable(a, d)); // unrelated

        // everything is assignable to Object
        assertTrue(assignable(object, c));
        assertTrue(assignable(object, d));
    }

    @Language("java")
    public static final String INPUT_GENERICS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.List;
            class X {
                static class Box<T> { }
                interface Holder<T> { }
                static class IntHolder implements Holder<Integer> { }
                static class MyList extends ArrayList<String> { }
                Box<Number> bNum;
                Box<Integer> bInt;
                Box<String> bStr;
                Box<? extends Number> bExtNum;
                Box<? extends Integer> bExtInt;
                Box<? super Integer> bSupInt;
                Box<? super Number> bSupNum;
                Box<? super Object> bSupObj;
                Box<?> bWild;
                Holder<Number> hNum;
                Holder<Integer> hInt;
                Holder<? extends Number> hExtNum;
                List<String> listString;
                List<Number> listNumber;
                List<? extends Number> listExtNumber;
                Collection<Number> collectionNumber;
                Iterable<String> iterableString;
                ArrayList<String> arrayListString;
                ArrayList<Integer> arrayListInteger;
                <E extends Number> void boundedTp(E e) { }
                <U> void freeTp(U u) { }
                <T extends Number & Comparable<T>> void multiBound(T t) { }
            }
            """;

    // testTypeArgumentCompatibility: invariance, unbounded/extends/super wildcards (same erasure, class type)
    @Test
    public void testGenericTypeArguments() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType bNum = X.getFieldByName("bNum", true).type();
        ParameterizedType bInt = X.getFieldByName("bInt", true).type();
        ParameterizedType bStr = X.getFieldByName("bStr", true).type();
        ParameterizedType bExtNum = X.getFieldByName("bExtNum", true).type();
        ParameterizedType bExtInt = X.getFieldByName("bExtInt", true).type();
        ParameterizedType bSupInt = X.getFieldByName("bSupInt", true).type();
        ParameterizedType bSupNum = X.getFieldByName("bSupNum", true).type();
        ParameterizedType bWild = X.getFieldByName("bWild", true).type();

        // exact match
        assertTrue(assignable(bNum, bNum));
        // invariance: Box<Number> is NOT assignable from Box<Integer>
        assertFalse(assignable(bNum, bInt));

        // unbounded wildcard accepts anything
        assertTrue(assignable(bWild, bStr));
        assertTrue(assignable(bWild, bInt));

        // ? extends Number
        assertTrue(assignable(bExtNum, bInt));     // concrete Integer
        assertTrue(assignable(bExtNum, bNum));     // concrete Number
        assertFalse(assignable(bExtNum, bStr));    // String is not a Number
        assertTrue(assignable(bExtNum, bExtInt));  // ? extends Integer fits within ? extends Number
        assertFalse(assignable(bExtNum, bSupNum)); // super vs extends mismatch

        // ? super Integer
        assertTrue(assignable(bSupInt, bNum));     // concrete Number is a supertype of Integer
        assertTrue(assignable(bSupInt, bSupNum));  // ? super Number fits within ? super Integer
        assertFalse(assignable(bSupInt, bStr));    // String is not a supertype of Integer
        assertFalse(assignable(bSupInt, bExtNum)); // extends vs super mismatch
        assertFalse(assignable(bSupNum, bSupInt)); // ? super Integer does NOT fit within ? super Number
    }

    // testHierarchy + testTypeArgumentCompatibility through an implemented interface with concrete arguments
    @Test
    public void testGenericInterface() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType hNum = X.getFieldByName("hNum", true).type();
        ParameterizedType hInt = X.getFieldByName("hInt", true).type();
        ParameterizedType hExtNum = X.getFieldByName("hExtNum", true).type();
        ParameterizedType intHolder = X.findSubType("IntHolder").asParameterizedType();

        assertTrue(assignable(hInt, intHolder));     // IntHolder implements Holder<Integer>
        assertFalse(assignable(hNum, intHolder));    // invariance: Holder<Number> <- Holder<Integer>
        assertTrue(assignable(hExtNum, intHolder));  // Holder<? extends Number> <- Holder<Integer>
    }

    // testTypeParameter: the 'from' is a type parameter -> use its (upper) bound
    @Test
    public void testTypeParameterAsFrom() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType number = javaInspector.compiledTypesManager().getOrLoad(Number.class).asParameterizedType();
        ParameterizedType integer = runtime.integerTypeInfo().asParameterizedType();
        ParameterizedType object = runtime.objectParameterizedType();

        // E extends Number
        ParameterizedType boundedTp = X.findUniqueMethod("boundedTp", 1).parameters().getFirst().parameterizedType();
        assertTrue(assignable(number, boundedTp));   // E's bound is Number
        assertTrue(assignable(object, boundedTp));    // ... which is an Object
        assertFalse(assignable(integer, boundedTp));  // Number (the bound) is not an Integer

        // U unbounded -> bound is Object
        ParameterizedType freeTp = X.findUniqueMethod("freeTp", 1).parameters().getFirst().parameterizedType();
        assertTrue(assignable(object, freeTp));
        assertFalse(assignable(number, freeTp));
    }

    // testTypeParameter: the 'to' is a type parameter and the 'from' is a '? super X' wildcard -> use lower bound
    @Test
    public void testTypeParameterAsToWithSuper() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType boundedTp = X.findUniqueMethod("boundedTp", 1).parameters().getFirst().parameterizedType();
        ParameterizedType freeTp = X.findUniqueMethod("freeTp", 1).parameters().getFirst().parameterizedType();

        // the wildcards '? super Integer' and '? super Object', extracted as type arguments
        ParameterizedType superInteger = X.getFieldByName("bSupInt", true).type().parameters().getFirst();
        ParameterizedType superObject = X.getFieldByName("bSupObj", true).type().parameters().getFirst();

        // E extends Number: lower bound Integer must be assignable to E's bound Number -> false
        assertFalse(assignable(boundedTp, superInteger));
        // U unbounded (bound Object): lower bound Object is assignable to Object -> true
        assertTrue(assignable(freeTp, superObject));
    }

    // typeIntersection: '? extends A & B', built directly via the factory
    @Test
    public void testIntersection() {
        TypeInfo number = javaInspector.compiledTypesManager().getOrLoad(Number.class);
        TypeInfo comparable = javaInspector.compiledTypesManager().getOrLoad(Comparable.class);
        TypeInfo runnable = javaInspector.compiledTypesManager().getOrLoad(Runnable.class);
        ParameterizedType numberPt = number.asParameterizedType();
        ParameterizedType comparablePt = comparable.asSimpleParameterizedType();
        ParameterizedType runnablePt = runnable.asParameterizedType();
        ParameterizedType integerPt = runtime.integerTypeInfo().asParameterizedType();
        ParameterizedType stringPt = runtime.stringParameterizedType();
        ParameterizedType objectPt = runtime.objectParameterizedType();

        ParameterizedType intersection = runtime.newIntersectionType(null, List.of(numberPt, comparablePt));

        // 'from' is the intersection A & B: assignable to S if ANY component is (A & B <: S)
        assertTrue(assignable(numberPt, intersection));
        assertTrue(assignable(comparablePt, intersection));
        assertFalse(assignable(runnablePt, intersection)); // neither a Number nor Comparable

        // 'to' is the intersection A & B: satisfied only if 'from' is assignable to EVERY component
        assertTrue(assignable(intersection, integerPt));  // Integer is both a Number and Comparable
        assertFalse(assignable(intersection, stringPt));   // String is Comparable but not a Number
        assertFalse(assignable(intersection, objectPt));   // Object is neither
    }

    // testHierarchy via concreteSuperType: type arguments are substituted along the hierarchy (JLS 4.10.2)
    @Test
    public void testGenericHierarchySubstitution() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType listString = X.getFieldByName("listString", true).type();
        ParameterizedType listNumber = X.getFieldByName("listNumber", true).type();
        ParameterizedType listExtNumber = X.getFieldByName("listExtNumber", true).type();
        ParameterizedType collectionNumber = X.getFieldByName("collectionNumber", true).type();
        ParameterizedType iterableString = X.getFieldByName("iterableString", true).type();
        ParameterizedType arrayListString = X.getFieldByName("arrayListString", true).type();
        ParameterizedType arrayListInteger = X.getFieldByName("arrayListInteger", true).type();
        ParameterizedType myList = X.findSubType("MyList").asParameterizedType();

        // ArrayList<String> -> List<String> -> Collection<String> -> Iterable<String>
        assertTrue(assignable(iterableString, arrayListString));
        assertTrue(assignable(listString, arrayListString));
        // Collection<Number> <- List<Number> (interface super-interface, arguments preserved)
        assertTrue(assignable(collectionNumber, listNumber));
        // List<? extends Number> <- ArrayList<Integer> (wildcard + substitution)
        assertTrue(assignable(listExtNumber, arrayListInteger));
        // invariance survives substitution: List<Number> <- ArrayList<Integer> is false
        assertFalse(assignable(listNumber, arrayListInteger));
        // interface inherited through a superclass: MyList extends ArrayList<String>
        assertTrue(assignable(listString, myList));
        assertTrue(assignable(iterableString, myList));
        assertFalse(assignable(listNumber, myList));
    }

    // testTypeParameter: a type parameter with multiple bounds is assignable to any of its bounds
    @Test
    public void testMultiBoundTypeParameter() {
        TypeInfo X = javaInspector.parse(INPUT_GENERICS);
        ParameterizedType number = javaInspector.compiledTypesManager().getOrLoad(Number.class).asParameterizedType();
        ParameterizedType comparable = javaInspector.compiledTypesManager().getOrLoad(Comparable.class)
                .asSimpleParameterizedType();
        ParameterizedType string = runtime.stringParameterizedType();

        // T extends Number & Comparable<T>
        ParameterizedType multiBound = X.findUniqueMethod("multiBound", 1).parameters().getFirst().parameterizedType();
        assertTrue(assignable(number, multiBound));     // first bound
        assertTrue(assignable(comparable, multiBound));  // second bound
        assertFalse(assignable(string, multiBound));     // neither bound is assignable to String
    }
}
