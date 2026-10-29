/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.aapi.archive.jdk;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.annotation.rare.StaticSideEffects;
import io.codelaser.maddi.annotation.type.UtilityClass;
public class JavaLang {
    public static final String PACKAGE_NAME = "java.lang";
    //public interface Appendable
    @Container
    @Independent
    class Appendable$ {
        //@Independent[O]
        @Modified Appendable append(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ @Nullable CharSequence charSequence) {
            return null;
        }

        //@Independent[O]
        @Modified Appendable append(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ @Nullable CharSequence charSequence,
            int i,
            int i1) { return null; }

        //@Independent[O]
        @Modified Appendable append(char c) { return null; }
    }

    //public class AssertionError extends Error
    class AssertionError$ {
        AssertionError$() { }
        AssertionError$(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Nullable Object detailMessage) { }
        AssertionError$(boolean detailMessage) { }
        AssertionError$(char detailMessage) { }
        AssertionError$(int detailMessage) { }
        AssertionError$(long detailMessage) { }
        AssertionError$(float detailMessage) { }
        AssertionError$(double detailMessage) { }
        AssertionError$(@Nullable String message, @Nullable @NotModified Throwable cause) { }
    }

    //public interface AutoCloseable
    //@Container[M]
    @Modified @Independent class AutoCloseable$ {void close() { } }

    //public final class Boolean implements Serializable, Comparable<Boolean>, Constable
    @ImmutableContainer
    class Boolean$ {
        //@Immutable[T] @Independent[T] @NotModified[T]
        static final Boolean TRUE = null;

        //@Immutable[T] @Independent[T] @NotModified[T]
        static final Boolean FALSE = null;
        static final Class<Boolean> TYPE = null;
        Boolean$(boolean value) { }
        Boolean$(@Nullable String s) { }
        //frequency 1
        //@NotModified[T]
        static boolean parseBoolean(@Nullable String s) { return false; }

        //@NotModified[T]
        boolean booleanValue() { return false; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Boolean valueOf(boolean b) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Boolean valueOf(@Nullable String s) { return null; }

        //@NotModified[T]
        static String toString(boolean b) { return null; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        static int hashCode(boolean value) { return 0; }

        //override from java.lang.Object
        //frequency 7
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@NotModified[T]
        static boolean getBoolean(@Nullable String name) { return false; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull Boolean b) { return 0; }

        //@NotModified[T]
        static int compare(boolean x, boolean y) { return 0; }

        //@NotModified[T]
        static boolean logicalAnd(boolean a, boolean b) { return false; }

        //@NotModified[T]
        static boolean logicalOr(boolean a, boolean b) { return false; }

        //@NotModified[T]
        static boolean logicalXor(boolean a, boolean b) { return false; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<DynamicConstantDesc<Boolean>> describeConstable() { return null; }
    }

    //public final class Byte extends Number implements Comparable<Byte>, Constable
    @ImmutableContainer
    class Byte$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Byte$(@NotNull java.lang.String p0) { }
        int compareTo(@NotNull java.lang.Byte p0) { return 0; }
        static java.lang.Byte decode(@NotNull java.lang.String p0) { return null; }
        static byte parseByte(@NotNull java.lang.String p0) { return 0; }
        static byte parseByte(@NotNull java.lang.String p0, int p1) { return 0; }
        static java.lang.Byte valueOf(@NotNull java.lang.String p0) { return null; }
        static java.lang.Byte valueOf(@NotNull java.lang.String p0, int p1) { return null; }
    }

    //public interface CharSequence
    // NOT a @Container: the getChars(...,char[] dst,...) default method fills its array argument (that
    // would otherwise force dst @NotModified and propagate to String/StringBuilder/StringBuffer.getChars).
    @Immutable(hc = true)
    @Independent
    class CharSequence$ {
        //override has frequency 17
        //@NotModified[T]
        int length() { return 0; }

        //@NotModified[T]
        char charAt(int i) { return '\0'; }

        //override has frequency 21
        //@NotModified[T]
        boolean isEmpty() { return false; }

        // NOTE: can we demand that the result is @Independent?
        //override has frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        CharSequence subSequence(int i, int i1) { return null; }

        //override from java.lang.Object
        //override has frequency 3
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //@Independent[O] @NotModified[T]
        IntStream chars() { return null; }

        //@Independent[O] @NotModified[T]
        IntStream codePoints() { return null; }

        //@NotModified[T]
        static int compare(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence cs1,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence cs2) { return 0; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void getChars(int p0, int p1, @NotNull char[] p2, int p3) { }
    }

    //public final class Character implements Serializable, Comparable<Character>, Constable
    // has static toChars(int, char[], int) which fills its array argument -> @Immutable (not @Container), like String
    @Immutable
    class Character$ { 
        //public java.lang.Character.Subset (shadow created for its nullability, generated)
        class Subset {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            Subset(@NotNull java.lang.String p0) { }
        }

        //public java.lang.Character.UnicodeBlock (shadow created for its nullability, generated)
        class UnicodeBlock {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            static java.lang.Character.UnicodeBlock forName(@NotNull java.lang.String p0) { return null; }
            @Nullable static java.lang.Character.UnicodeBlock of(char p0) { return null; }
            @Nullable static java.lang.Character.UnicodeBlock of(int p0) { return null; }
        }

        //public java.lang.Character.UnicodeScript (shadow created for its nullability, generated)
        class UnicodeScript {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            static java.lang.Character.UnicodeScript forName(@NotNull java.lang.String p0) { return null; }
        }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static int codePointAt(@NotNull java.lang.CharSequence p0, int p1) { return 0; }
        static int codePointAt(@NotModified @NotNull char[] p0, int p1) { return 0; }
        static int codePointAt(@NotModified @NotNull char[] p0, int p1, int p2) { return 0; }
        static int codePointBefore(@NotNull java.lang.CharSequence p0, int p1) { return 0; }
        static int codePointBefore(@NotModified @NotNull char[] p0, int p1) { return 0; }
        static int codePointBefore(@NotModified @NotNull char[] p0, int p1, int p2) { return 0; }
        static int codePointCount(@NotNull java.lang.CharSequence p0, int p1, int p2) { return 0; }
        static int codePointCount(@NotModified @NotNull char[] p0, int p1, int p2) { return 0; }
        static int codePointOf(@NotNull java.lang.String p0) { return 0; }
        int compareTo(@NotNull java.lang.Character p0) { return 0; }
        @Nullable static java.lang.String getName(int p0) { return null; }
        static int offsetByCodePoints(@NotNull java.lang.CharSequence p0, int p1, int p2) { return 0; }
        static int offsetByCodePoints(@NotModified @NotNull char[] p0, int p1, int p2, int p3, int p4) { return 0; }
        static int toChars(int p0, @NotNull char[] p1, int p2) { return 0; }
    }

    //public final class Class implements Serializable, GenericDeclaration, Type, AnnotatedElement, TypeDescriptor.OfField<Class<?>>, Constable
    @ImmutableContainer
    class Class$<@Independent T> {
        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //@NotModified[T]
        String toGenericString() { return null; }

        //@NotModified[T]
        static Class<?> forName(@NotNull String className) { return null; }

        //@NotModified[T]
        static Class<?> forName(@NotNull String name, boolean initialize, /*@Independent[T] @NotModified[O]*/ @Nullable ClassLoader loader) {
            return null;
        }

        //@NotModified[T]
        @Nullable static Class<?> forName(/*@Independent[T] @NotModified[O]*/ @NotNull Module module, @NotNull String name) { return null; }

        //@NotModified[T]
        @Nullable static Class<?> forPrimitiveName(@NotNull String primitiveName) { return null; }

        //@Independent[O] @NotModified[T]
        T newInstance() { return null; }

        //@NotModified[T]
        boolean isInstance(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return false; }

        //@NotModified[T]
        boolean isAssignableFrom(@NotNull Class<?> class0) { return false; }

        //@NotModified[T]
        boolean isInterface() { return false; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        boolean isArray() { return false; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        boolean isPrimitive() { return false; }

        //@NotModified[T]
        boolean isAnnotation() { return false; }

        //@NotModified[T]
        boolean isSynthetic() { return false; }

        //@NotModified[T]
        String getName() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable ClassLoader getClassLoader() { return null; }

        //@Independent[O] @NotModified[T]
        Module getModule() { return null; }

        //override from java.lang.reflect.GenericDeclaration
        //@Independent[O] @NotModified[T]
        TypeVariable<Class<T>> [] getTypeParameters() { return null; }

        //@NotModified[T]
        @Nullable Class<? super T> getSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable Type getGenericSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable Package getPackage() { return null; }

        //@NotModified[T]
        String getPackageName() { return null; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getInterfaces() { return null; }

        //@Independent[O] @NotModified[T]
        Type [] getGenericInterfaces() { return null; }

        //@NotModified[T]
        @Nullable Class<?> getComponentType() { return null; }

        //@NotModified[T]
        int getModifiers() { return 0; }

        //@Independent[O] @NotModified[T]
        Set<AccessFlag> accessFlags() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable Object [] getSigners() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable Method getEnclosingMethod() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable Constructor<?> getEnclosingConstructor() { return null; }

        //@NotModified[T]
        @Nullable Class<?> getDeclaringClass() { return null; }

        //@NotModified[T]
        @Nullable Class<?> getEnclosingClass() { return null; }

        //@NotModified[T]
        String getSimpleName() { return null; }

        //override from java.lang.reflect.Type
        //@NotModified[T]
        String getTypeName() { return null; }

        //@NotModified[T]
        @Nullable String getCanonicalName() { return null; }

        //@NotModified[T]
        boolean isAnonymousClass() { return false; }

        //@NotModified[T]
        boolean isLocalClass() { return false; }

        //@NotModified[T]
        boolean isMemberClass() { return false; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getClasses() { return null; }

        //@Independent[O] @NotModified[T]
        Field [] getFields() { return null; }

        //@Independent[O] @NotModified[T]
        Method [] getMethods() { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<?> [] getConstructors() { return null; }

        //@Independent[O] @NotModified[T]
        Field getField(@NotNull String name) { return null; }

        //@Independent[O] @NotModified[T]
        Method getMethod(@NotNull String name, /*@Independent[M] @NotModified[O]*/ @Nullable Class<?> ... parameterTypes) { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<T> getConstructor(/*@Independent[M] @NotModified[O]*/ @Nullable Class<?> ... parameterTypes) { return null; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getDeclaredClasses() { return null; }

        //@Independent[O] @NotModified[T]
        Field [] getDeclaredFields() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable RecordComponent [] getRecordComponents() { return null; }

        //@Independent[O] @NotModified[T]
        Method [] getDeclaredMethods() { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<?> [] getDeclaredConstructors() { return null; }

        //@Independent[O] @NotModified[T]
        Field getDeclaredField(@NotNull String name) { return null; }

        //@Independent[O] @NotModified[T]
        Method getDeclaredMethod(@NotNull String name, /*@Independent[M] @NotModified[O]*/ @Nullable Class<?> ... parameterTypes) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        Constructor<T> getDeclaredConstructor(/*@Independent[M] @NotModified[O]*/ @Nullable Class<?> ... parameterTypes) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        @Nullable InputStream getResourceAsStream(@NotNull String name) { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        @Nullable URL getResource(@NotNull String name) { return null; }

        //@Independent[O] @NotModified[T]
        ProtectionDomain getProtectionDomain() { return null; }

        //@NotModified[T]
        boolean desiredAssertionStatus() { return false; }

        //@NotModified[T]
        boolean isEnum() { return false; }

        //@NotModified[T]
        boolean isRecord() { return false; }

        //@Independent[O] @NotModified[T]
        @Nullable T [] getEnumConstants() { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        T cast(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object obj) { return null; }

        //@NotModified[T]
        <U> Class<? extends U> asSubclass(@NotNull Class<U> clazz) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        @Nullable <A extends Annotation> A getAnnotation(@NotNull Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@NotModified[T]
        boolean isAnnotationPresent(@NotNull Class<? extends Annotation> annotationClass) { return false; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A [] getAnnotationsByType(@NotNull Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        Annotation [] getAnnotations() { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        @Nullable <A extends Annotation> A getDeclaredAnnotation(@NotNull Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A [] getDeclaredAnnotationsByType(@NotNull Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        Annotation [] getDeclaredAnnotations() { return null; }

        //@Independent[O] @NotModified[T]
        @Nullable AnnotatedType getAnnotatedSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        AnnotatedType [] getAnnotatedInterfaces() { return null; }

        //@NotModified[T]
        Class<?> getNestHost() { return null; }

        //@NotModified[T]
        boolean isNestmateOf(@NotNull Class<?> c) { return false; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getNestMembers() { return null; }

        //override from java.lang.invoke.TypeDescriptor
        //@NotModified[T]
        String descriptorString() { return null; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        @Nullable Class<?> componentType() { return null; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        Class<?> arrayType() { return null; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<ClassDesc> describeConstable() { return null; }

        //@NotModified[T]
        boolean isHidden() { return false; }

        //@Independent[O] @NotModified[T]
        @Nullable Class<?> [] getPermittedSubclasses() { return null; }

        //@NotModified[T]
        boolean isSealed() { return false; }
    }

    //public interface Cloneable
    //@Container[M]
    @Independent(hc = true) class Cloneable$ { }

    //public interface Comparable
    @ImmutableContainer(hc = true)
    @Independent
    class Comparable$<T> {
        //override has frequency 1
        //@NotModified[T]
        int compareTo(/*@Independent[M] @NotModified[O]*/ @NotNull T t) { return 0; }
    }

    //public final class Double extends Number implements Comparable<Double>, Constable, ConstantDesc
    @ImmutableContainer
    class Double$ {
        static final double POSITIVE_INFINITY = 0.0;
        static final double NEGATIVE_INFINITY = 0.0;
        static final double NaN = 0.0;
        static final double MAX_VALUE = 0.0;
        static final double MIN_NORMAL = 0.0;
        static final double MIN_VALUE = 0.0;
        static final int SIZE = 0;
        static final int PRECISION = 0;
        static final int MAX_EXPONENT = 0;
        static final int MIN_EXPONENT = 0;
        static final int BYTES = 0;
        static final Class<Double> TYPE = null;
        Double$(double value) { }
        Double$(@NotNull String s) { }
        //@NotModified[T]
        static String toString(double d) { return null; }

        //@NotModified[T]
        static String toHexString(double d) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Double valueOf(@NotNull String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Double valueOf(double d) { return null; }

        //@NotModified[T]
        static double parseDouble(@NotNull String s) { return 0.0; }

        //@NotModified[T]
        static boolean isNaN(double v) { return false; }

        //@NotModified[T]
        static boolean isInfinite(double v) { return false; }

        //@NotModified[T]
        static boolean isFinite(double d) { return false; }

        //@NotModified[T]
        boolean isNaN() { return false; }

        //@NotModified[T]
        boolean isInfinite() { return false; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Number
        //@NotModified[T]
        byte byteValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        short shortValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        int intValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[T]
        float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[T]
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        static int hashCode(double value) { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@NotModified[T]
        static long doubleToLongBits(double value) { return 0L; }

        //@NotModified[T]
        static long doubleToRawLongBits(double d) { return 0L; }

        //@NotModified[T]
        static double longBitsToDouble(long l) { return 0.0; }

        //override from java.lang.Comparable
        //frequency 1
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull Double anotherDouble) { return 0; }

        //@NotModified[T]
        static int compare(double d1, double d) { return 0; }

        //@NotModified[T]
        static double sum(double a, double d) { return 0.0; }

        //@NotModified[T]
        static double max(double a, double d) { return 0.0; }

        //@NotModified[T]
        static double min(double a, double d) { return 0.0; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<Double> describeConstable() { return null; }

        //override from java.lang.constant.ConstantDesc
        //@Immutable[T] @Independent[O] @NotModified[T]
        Double resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ @NotNull MethodHandles.Lookup lookup) { return null; }
    }

    //public abstract class Enum implements Constable, Comparable<E>, Serializable
    @ImmutableContainer(hc = true)
    @Independent
    class Enum$<E extends Enum<E>> {
        //public static final class EnumDesc extends DynamicConstantDesc<E>
        class EnumDesc<E extends Enum<E>> {
            static <E extends Enum<E>> Enum.EnumDesc<E> of(@NotNull ClassDesc enumClass, @NotNull String constantName) { return null; }
            //override from java.lang.constant.ConstantDesc, java.lang.constant.DynamicConstantDesc
            //@Immutable(hc=true)[T] @Independent(hc=true)[H]
            E resolveConstantDesc(@NotNull MethodHandles.Lookup lookup) { return null; }

            //override from java.lang.Object, java.lang.constant.DynamicConstantDesc
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }

        //frequency 2
        //@NotModified[T]
        String name() { return null; }

        //frequency 1
        //@NotModified[T]
        int ordinal() { return 0; }

        //override from java.lang.Object
        //frequency 22
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //frequency 23
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object other) { return false; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O] @NotNull[H]*/ E o) { return 0; }

        //@NotModified[T]
        Class<E> getDeclaringClass() { return null; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Optional<Enum.EnumDesc<E>> describeConstable() { return null; }

        //@Immutable(hc=true)[T] @Independent[T] @NotModified[T]
        static <T extends Enum<T>> T valueOf(@NotNull Class<T> enumClass, @NotNull String name) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Enum$(@NotNull java.lang.String p0, int p1) { }
    }

    //public class Error extends Throwable
    class Error$ {
        Error$() { }
        //override has frequency 7
        Error$(@Nullable String message) { }
        Error$(@Nullable String message, @Nullable @NotModified Throwable cause) { }
        Error$(@Nullable @NotModified Throwable cause) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Error$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1, boolean p2, boolean p3) { }
    }

    //public class Exception extends Throwable
    class Exception$ {
        //override has frequency 1
        Exception$() { }

        //override has frequency 8
        Exception$(@Nullable String message) { }

        //override has frequency 1
        Exception$(@Nullable String message, @Nullable @NotModified Throwable cause) { }

        //override has frequency 4
        Exception$(@Nullable @NotModified Throwable cause) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Exception$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1, boolean p2, boolean p3) { }
    }

    //public final class Float extends Number implements Comparable<Float>, Constable, ConstantDesc
    @ImmutableContainer
    class Float$ {
        static final float POSITIVE_INFINITY = 0.0F;
        static final float NEGATIVE_INFINITY = 0.0F;
        static final float NaN = 0.0F;
        static final float MAX_VALUE = 0.0F;
        static final float MIN_NORMAL = 0.0F;
        static final float MIN_VALUE = 0.0F;
        static final int SIZE = 0;
        static final int PRECISION = 0;
        static final int MAX_EXPONENT = 0;
        static final int MIN_EXPONENT = 0;
        static final int BYTES = 0;
        static final Class<Float> TYPE = null;
        Float$(float value) { }
        Float$(double value) { }
        Float$(@NotNull String s) { }
        //@NotModified[T]
        static String toString(float f) { return null; }

        //@NotModified[T]
        static String toHexString(float f) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Float valueOf(@NotNull String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Float valueOf(float f) { return null; }

        //frequency 2
        //@NotModified[T]
        static float parseFloat(@NotNull String s) { return 0.0F; }

        //@NotModified[T]
        static boolean isNaN(float v) { return false; }

        //@NotModified[T]
        static boolean isInfinite(float v) { return false; }

        //@NotModified[T]
        static boolean isFinite(float f) { return false; }

        //@NotModified[T]
        boolean isNaN() { return false; }

        //@NotModified[T]
        boolean isInfinite() { return false; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Number
        //@NotModified[T]
        byte byteValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        short shortValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        int intValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[T]
        float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[T]
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        static int hashCode(float value) { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@NotModified[T]
        static int floatToIntBits(float value) { return 0; }

        //@NotModified[T]
        static int floatToRawIntBits(float f) { return 0; }

        //@NotModified[T]
        static float intBitsToFloat(int i) { return 0.0F; }

        //@NotModified[T]
        static float float16ToFloat(short floatBinary16) { return 0.0F; }

        //@NotModified[T]
        static short floatToFloat16(float f) { return 0; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull Float anotherFloat) { return 0; }

        //@NotModified[T]
        static int compare(float f1, float f2) { return 0; }

        //@NotModified[T]
        static float sum(float a, float b) { return 0.0F; }

        //@NotModified[T]
        static float max(float a, float b) { return 0.0F; }

        //@NotModified[T]
        static float min(float a, float b) { return 0.0F; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<Float> describeConstable() { return null; }

        //override from java.lang.constant.ConstantDesc
        //@Immutable[T] @Independent[O] @NotModified[T]
        Float resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ @NotNull MethodHandles.Lookup lookup) { return null; }
    }

    //public class IllegalArgumentException extends RuntimeException
    class IllegalArgumentException$ {
        IllegalArgumentException$() { }
        //frequency 12
        IllegalArgumentException$(@Nullable String s) { }

        //frequency 2
        IllegalArgumentException$(@Nullable String message, @Nullable @NotModified Throwable cause) { }
        IllegalArgumentException$(@Nullable @NotModified Throwable cause) { }
    }

    //public class IllegalStateException extends RuntimeException
    class IllegalStateException$ {
        IllegalStateException$() { }
        //frequency 9
        IllegalStateException$(@Nullable String s) { }

        //frequency 1
        IllegalStateException$(@Nullable String message, @Nullable @NotModified Throwable cause) { }

        //frequency 4
        IllegalStateException$(@Nullable @NotModified Throwable cause) { }
    }

    //public final class Integer extends Number implements Comparable<Integer>, Constable, ConstantDesc
    @ImmutableContainer
    class Integer$ {
        static final int MIN_VALUE = 0;
        static final int MAX_VALUE = 0;
        static final Class<Integer> TYPE = null;
        static final int SIZE = 0;
        static final int BYTES = 0;
        Integer$(int value) { }
        Integer$(@NotNull String s) { }
        //@NotModified[T]
        static String toString(int i, int radix) { return null; }

        //@NotModified[T]
        static String toUnsignedString(int i, int radix) { return null; }

        //@NotModified[T]
        static String toHexString(int i) { return null; }

        //@NotModified[T]
        static String toOctalString(int i) { return null; }

        //@NotModified[T]
        static String toBinaryString(int i) { return null; }

        //frequency 2
        //@NotModified[T]
        static String toString(int i) { return null; }

        //@NotModified[T]
        static String toUnsignedString(int i) { return null; }

        //@NotModified[T]
        static int parseInt(@NotNull String s, int radix) { return 0; }

        //@NotModified[T]
        static int parseInt(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0; }

        //frequency 13
        //@NotModified[T]
        static int parseInt(@NotNull String s) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(@NotNull String s, int radix) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(@NotNull String s) { return 0; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer valueOf(@NotNull String s, int radix) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer valueOf(@NotNull String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer valueOf(int i) { return null; }

        //override from java.lang.Number
        //@NotModified[T]
        byte byteValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        short shortValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        int intValue() { return 0; }

        //override from java.lang.Number
        //frequency 1
        //@NotModified[T]
        long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[T]
        float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[T]
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        //frequency 1
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        static int hashCode(int value) { return 0; }

        //override from java.lang.Object
        //frequency 1
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        @Nullable static Integer getInteger(@Nullable String nm) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer getInteger(@Nullable String nm, int val) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer getInteger(@Nullable String nm, /*@Immutable[T] @Independent[T] @NotModified[O]*/ @Nullable Integer val) {
            return null;
        }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer decode(@NotNull String nm) { return null; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull Integer anotherInteger) { return 0; }

        //@NotModified[T]
        static int compare(int x, int y) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(int x, int y) { return 0; }

        //@NotModified[T]
        static long toUnsignedLong(int x) { return 0L; }

        //@NotModified[T]
        static int divideUnsigned(int dividend, int divisor) { return 0; }

        //@NotModified[T]
        static int remainderUnsigned(int dividend, int divisor) { return 0; }

        //@NotModified[T]
        static int highestOneBit(int i) { return 0; }

        //@NotModified[T]
        static int lowestOneBit(int i) { return 0; }

        //@NotModified[T]
        static int numberOfLeadingZeros(int i) { return 0; }

        //@NotModified[T]
        static int numberOfTrailingZeros(int i) { return 0; }

        //@NotModified[T]
        static int bitCount(int i) { return 0; }

        //@NotModified[T]
        static int rotateLeft(int i, int distance) { return 0; }

        //@NotModified[T]
        static int rotateRight(int i, int distance) { return 0; }

        //@NotModified[T]
        static int reverse(int i) { return 0; }

        //@NotModified[T]
        static int compress(int i, int mask) { return 0; }

        //@NotModified[T]
        static int expand(int i, int mask) { return 0; }

        //@NotModified[T]
        static int signum(int i) { return 0; }

        //@NotModified[T]
        static int reverseBytes(int i) { return 0; }

        //@NotModified[T]
        static int sum(int a, int b) { return 0; }

        //@NotModified[T]
        static int max(int a, int b) { return 0; }

        //@NotModified[T]
        static int min(int a, int b) { return 0; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<Integer> describeConstable() { return null; }

        //override from java.lang.constant.ConstantDesc
        //@Immutable[T] @Independent[O] @NotModified[T]
        Integer resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ @NotNull MethodHandles.Lookup lookup) { return null; }
    }

    //public interface Iterable
    // @Container only, NOT @ImmutableContainer(hc = true) (tried 2026-09-23 in f8e855ce1, reverted 2026-09-25). The
    // claim contradicts iterator()'s own `except = "remove"`: an immutable type has no §m modification component, so
    // the it.§m ☷ this.§m link is never made and removing through the iterator of anything typed Iterable was lost
    // (Guava's Iterables.removeIf(Iterable, Predicate); TestOptimisticModificationShapes.o4). The price: source types
    // implementing Iterable read this supertype as undecided and floor at FINAL_FIELDS (vavr: 27 types, e.g. Option).
    @Container
    class Iterable$<T> {
        //override has frequency 2
        @NotModified @NotNull @Independent(hc = true, except = "remove")
        Iterator<T> iterator() { return null; }

        //frequency 69
        @NotModified

        void forEach(
            /*@IgnoreModifications[T] @NotModified[O]*/ @Independent(hc = true) @NotNull Consumer<? super T> action) { }
        @Independent(hc = true) @NotModified @NotNull Spliterator<T> spliterator() { return null; }
    }

    //public final class Long extends Number implements Comparable<Long>, Constable, ConstantDesc
    @ImmutableContainer
    class Long$ {
        static final long MIN_VALUE = 0L;
        static final long MAX_VALUE = 0L;
        static final Class<Long> TYPE = null;
        static final int SIZE = 0;
        static final int BYTES = 0;
        Long$(long value) { }
        Long$(@NotNull String s) { }
        //@NotModified[T]
        static String toString(long i, int i1) { return null; }

        //@NotModified[T]
        static String toUnsignedString(long i, int i1) { return null; }

        //@NotModified[T]
        static String toHexString(long i) { return null; }

        //@NotModified[T]
        static String toOctalString(long i) { return null; }

        //@NotModified[T]
        static String toBinaryString(long i) { return null; }

        //frequency 2
        //@NotModified[T]
        static String toString(long i) { return null; }

        //@NotModified[T]
        static String toUnsignedString(long i) { return null; }

        //@NotModified[T]
        static long parseLong(@NotNull String s, int radix) { return 0L; }

        //@NotModified[T]
        static long parseLong(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0L; }

        //frequency 7
        //@NotModified[T]
        static long parseLong(@NotNull String s) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(@NotNull String s, int radix) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(@NotNull String s) { return 0L; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(@NotNull String s, int radix) { return null; }

        //frequency 1
        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(@NotNull String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(long l) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long decode(@NotNull String nm) { return null; }

        //override from java.lang.Number
        //@NotModified[T]
        byte byteValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        short shortValue() { return 0; }

        //override from java.lang.Number
        //frequency 3
        //@NotModified[T]
        int intValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[T]
        long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[T]
        float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[T]
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        //frequency 112
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        static int hashCode(long value) { return 0; }

        //override from java.lang.Object
        //frequency 46
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        @Nullable static Long getLong(@Nullable String nm) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long getLong(@Nullable String nm, long val) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long getLong(@Nullable String nm, /*@Immutable[T] @Independent[T] @NotModified[O]*/ @Nullable Long val) { return null; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull Long anotherLong) { return 0; }

        //frequency 1
        //@NotModified[T]
        static int compare(long x, long l) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(long x, long l) { return 0; }

        //@NotModified[T]
        static long divideUnsigned(long dividend, long l) { return 0L; }

        //@NotModified[T]
        static long remainderUnsigned(long dividend, long l) { return 0L; }

        //@NotModified[T]
        static long highestOneBit(long i) { return 0L; }

        //@NotModified[T]
        static long lowestOneBit(long i) { return 0L; }

        //@NotModified[T]
        static int numberOfLeadingZeros(long i) { return 0; }

        //@NotModified[T]
        static int numberOfTrailingZeros(long i) { return 0; }

        //@NotModified[T]
        static int bitCount(long i) { return 0; }

        //@NotModified[T]
        static long rotateLeft(long i, int i1) { return 0L; }

        //@NotModified[T]
        static long rotateRight(long i, int i1) { return 0L; }

        //@NotModified[T]
        static long reverse(long i) { return 0L; }

        //@NotModified[T]
        static long compress(long i, long l) { return 0L; }

        //@NotModified[T]
        static long expand(long i, long l) { return 0L; }

        //@NotModified[T]
        static int signum(long i) { return 0; }

        //@NotModified[T]
        static long reverseBytes(long i) { return 0L; }

        //@NotModified[T]
        static long sum(long a, long l) { return 0L; }

        //@NotModified[T]
        static long max(long a, long l) { return 0L; }

        //@NotModified[T]
        static long min(long a, long l) { return 0L; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<Long> describeConstable() { return null; }

        //override from java.lang.constant.ConstantDesc
        //@Immutable[T] @Independent[O] @NotModified[T]
        Long resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ @NotNull MethodHandles.Lookup lookup) { return null; }
    }

    //public final class Math
    @UtilityClass
    class Math$ {
        static final double E = 0.0;
        static final double PI = 0.0;
        static final double TAU = 0.0;
        //@NotModified[T]
        static double sin(double a) { return 0.0; }

        //@NotModified[T]
        static double cos(double a) { return 0.0; }

        //@NotModified[T]
        static double tan(double a) { return 0.0; }

        //@NotModified[T]
        static double asin(double a) { return 0.0; }

        //@NotModified[T]
        static double acos(double a) { return 0.0; }

        //@NotModified[T]
        static double atan(double a) { return 0.0; }

        //@NotModified[T]
        static double toRadians(double angdeg) { return 0.0; }

        //@NotModified[T]
        static double toDegrees(double angrad) { return 0.0; }

        //@NotModified[T]
        static double exp(double a) { return 0.0; }

        //@NotModified[T]
        static double log(double a) { return 0.0; }

        //@NotModified[T]
        static double log10(double a) { return 0.0; }

        //@NotModified[T]
        static double sqrt(double a) { return 0.0; }

        //@NotModified[T]
        static double cbrt(double a) { return 0.0; }

        //@NotModified[T]
        static double IEEEremainder(double f1, double d) { return 0.0; }

        //@NotModified[T]
        static double ceil(double a) { return 0.0; }

        //@NotModified[T]
        static double floor(double a) { return 0.0; }

        //@NotModified[T]
        static double rint(double a) { return 0.0; }

        //@NotModified[T]
        static double atan2(double y, double d) { return 0.0; }

        //@NotModified[T]
        static double pow(double a, double d) { return 0.0; }

        //@NotModified[T]
        static int round(float a) { return 0; }

        //@NotModified[T]
        static long round(double a) { return 0L; }

        //@NotModified[T]
        static double random() { return 0.0; }

        //@NotModified[T]
        static int addExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long addExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int subtractExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long subtractExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int multiplyExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long multiplyExact(long x, int i) { return 0L; }

        //@NotModified[T]
        static long multiplyExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int divideExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long divideExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int floorDivExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long floorDivExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int ceilDivExact(int x, int y) { return 0; }

        //@NotModified[T]
        static long ceilDivExact(long x, long l) { return 0L; }

        //@NotModified[T]
        static int incrementExact(int a) { return 0; }

        //@NotModified[T]
        static long incrementExact(long a) { return 0L; }

        //@NotModified[T]
        static int decrementExact(int a) { return 0; }

        //@NotModified[T]
        static long decrementExact(long a) { return 0L; }

        //@NotModified[T]
        static int negateExact(int a) { return 0; }

        //@NotModified[T]
        static long negateExact(long a) { return 0L; }

        //@NotModified[T]
        static int toIntExact(long value) { return 0; }

        //@NotModified[T]
        static long multiplyFull(int x, int y) { return 0L; }

        //@NotModified[T]
        static long multiplyHigh(long x, long l) { return 0L; }

        //@NotModified[T]
        static long unsignedMultiplyHigh(long x, long l) { return 0L; }

        //@NotModified[T]
        static int floorDiv(int x, int y) { return 0; }

        //@NotModified[T]
        static long floorDiv(long x, int i) { return 0L; }

        //@NotModified[T]
        static long floorDiv(long x, long l) { return 0L; }

        //@NotModified[T]
        static int floorMod(int x, int y) { return 0; }

        //@NotModified[T]
        static int floorMod(long x, int i) { return 0; }

        //@NotModified[T]
        static long floorMod(long x, long l) { return 0L; }

        //@NotModified[T]
        static int ceilDiv(int x, int y) { return 0; }

        //@NotModified[T]
        static long ceilDiv(long x, int i) { return 0L; }

        //@NotModified[T]
        static long ceilDiv(long x, long l) { return 0L; }

        //@NotModified[T]
        static int ceilMod(int x, int y) { return 0; }

        //@NotModified[T]
        static int ceilMod(long x, int i) { return 0; }

        //@NotModified[T]
        static long ceilMod(long x, long l) { return 0L; }

        //@NotModified[T]
        static int abs(int a) { return 0; }

        //@NotModified[T]
        static int absExact(int a) { return 0; }

        //frequency 1
        //@NotModified[T]
        static long abs(long a) { return 0L; }

        //@NotModified[T]
        static long absExact(long a) { return 0L; }

        //@NotModified[T]
        static float abs(float a) { return 0.0F; }

        //@NotModified[T]
        static double abs(double a) { return 0.0; }

        //frequency 2
        //@NotModified[T]
        static int max(int a, int b) { return 0; }

        //@NotModified[T]
        static long max(long a, long l) { return 0L; }

        //@NotModified[T]
        static float max(float a, float b) { return 0.0F; }

        //@NotModified[T]
        static double max(double a, double d) { return 0.0; }

        //frequency 3
        //@NotModified[T]
        static int min(int a, int b) { return 0; }

        //@NotModified[T]
        static long min(long a, long l) { return 0L; }

        //@NotModified[T]
        static float min(float a, float b) { return 0.0F; }

        //@NotModified[T]
        static double min(double a, double d) { return 0.0; }

        //@NotModified[T]
        static int clamp(long value, int i, int min) { return 0; }

        //@NotModified[T]
        static long clamp(long value, long l, long min) { return 0L; }

        //@NotModified[T]
        static double clamp(double value, double d, double min) { return 0.0; }

        //@NotModified[T]
        static float clamp(float value, float min, float max) { return 0.0F; }

        //@NotModified[T]
        static double fma(double a, double d, double b) { return 0.0; }

        //@NotModified[T]
        static float fma(float a, float b, float c) { return 0.0F; }

        //@NotModified[T]
        static double ulp(double d) { return 0.0; }

        //@NotModified[T]
        static float ulp(float f) { return 0.0F; }

        //@NotModified[T]
        static double signum(double d) { return 0.0; }

        //@NotModified[T]
        static float signum(float f) { return 0.0F; }

        //@NotModified[T]
        static double sinh(double x) { return 0.0; }

        //@NotModified[T]
        static double cosh(double x) { return 0.0; }

        //@NotModified[T]
        static double tanh(double x) { return 0.0; }

        //@NotModified[T]
        static double hypot(double x, double d) { return 0.0; }

        //@NotModified[T]
        static double expm1(double x) { return 0.0; }

        //@NotModified[T]
        static double log1p(double x) { return 0.0; }

        //@NotModified[T]
        static double copySign(double magnitude, double d) { return 0.0; }

        //@NotModified[T]
        static float copySign(float magnitude, float sign) { return 0.0F; }

        //@NotModified[T]
        static int getExponent(float f) { return 0; }

        //@NotModified[T]
        static int getExponent(double d) { return 0; }

        //@NotModified[T]
        static double nextAfter(double start, double d) { return 0.0; }

        //@NotModified[T]
        static float nextAfter(float start, double direction) { return 0.0F; }

        //@NotModified[T]
        static double nextUp(double d) { return 0.0; }

        //@NotModified[T]
        static float nextUp(float f) { return 0.0F; }

        //@NotModified[T]
        static double nextDown(double d) { return 0.0; }

        //@NotModified[T]
        static float nextDown(float f) { return 0.0F; }

        //@NotModified[T]
        static double scalb(double d, int i) { return 0.0; }

        //@NotModified[T]
        static float scalb(float f, int scaleFactor) { return 0.0F; }
    }

    //public abstract class Number implements Serializable
    @ImmutableContainer(hc = true)
    class Number$ {
        //override has frequency 2
        Number$() { }

        //override has frequency 3
        //@NotModified[T]
        int intValue() { return 0; }

        //override has frequency 1
        //@NotModified[T]
        long longValue() { return 0L; }

        //@NotModified[T]
        float floatValue() { return 0.0F; }

        //@NotModified[T]
        double doubleValue() { return 0.0; }

        //@NotModified[T]
        byte byteValue() { return 0; }

        //@NotModified[T]
        short shortValue() { return 0; }
    }

    //public class Object
    @ImmutableContainer(hc = true)
    @Independent
    class Object$ {
        //override has frequency 1
        Object$() { }

        //frequency 242
        //@NotModified[T]
        public Class<?> getClass$() { return null; }

        //frequency 113
        //@NotModified[T]
        public int hashCode() { return 0; }

        //frequency 1
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object obj) { return false; }

        //frequency 10
        //@NotModified[T]
        @NotNull
        public String toString() { return null; }

        //@NotModified[T]
        @IgnoreModifications
        public void notify$() { }

        //@NotModified[T]
        @IgnoreModifications
        public void notifyAll$() { }

        //@NotModified[T]
        @IgnoreModifications
        public void wait$() { }

        //@NotModified[T]
        @IgnoreModifications
        public void wait$(long timeoutMillis) { }

        //@NotModified[T]
        @IgnoreModifications
        public void wait$(long timeoutMillis, int i) { }
    }

    //public abstract class Record
    // The implicit supertype of every record. It declares no state and only the abstract Object contract methods,
    // so like Object it is a hidden-content-immutable, independent base. Without this stub records extending it
    // cannot compute their own INDEPENDENT_TYPE (it stays null), and immutability stalls at @FinalFields.
    @ImmutableContainer(hc = true)
    @Independent
    class Record$ {
        Record$() { }

        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        @NotNull
        public String toString() { return null; }
    }

    //public interface ProcessHandle implements Comparable<ProcessHandle>
    @Container
    class ProcessHandle$ {
        //public interface Info
        //@Container[M]
        class Info {
            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<String> command() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<String> commandLine() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<String []> arguments() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            @Modified Optional<Instant> startInstant() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<Duration> totalCpuDuration() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<String> user() { return null; }
        }
        @NotModified long pid() { return 0L; }
        //@Immutable(hc=true)[T] @Independent[T]
        static Optional<ProcessHandle> of(long pid) { return null; }
        static ProcessHandle current() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Optional<ProcessHandle> parent() { return null; }
        @NotModified Stream<ProcessHandle> children() { return null; }
        @NotModified Stream<ProcessHandle> descendants() { return null; }
        //@Independent[T]
        static Stream<ProcessHandle> allProcesses() { return null; }
        @NotModified ProcessHandle.Info info() { return null; }
        @NotModified CompletableFuture<ProcessHandle> onExit() { return null; }
        @NotModified boolean supportsNormalTermination() { return false; }
        boolean destroy() { return false; }
        boolean destroyForcibly() { return false; }
        @NotModified boolean isAlive() { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ProcessHandle processHandle) { return 0; }
    }

    //public interface Readable
    @Independent
    class Readable$ {@Modified int read(@NotNull CharBuffer charBuffer) { return 0; } }

    //public interface Runnable
    //@Container[M]
    @Independent(hc = true) class Runnable$ {void run() { } }

    //public class RuntimeException extends Exception
    class RuntimeException$ {
        //frequency 1
        RuntimeException$() { }

        //frequency 8
        RuntimeException$(@Nullable String message) { }

        //override has frequency 1
        RuntimeException$(@Nullable String message, @Nullable @NotModified Throwable cause) { }

        //frequency 3
        RuntimeException$(@Nullable @NotModified Throwable cause) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        RuntimeException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1, boolean p2, boolean p3) { }
    }

    //public final class String implements Serializable, Comparable<String>, CharSequence, Constable, ConstantDesc
    @Immutable
    class String$ {
        //@Immutable(hc=true)[T] @NotModified[T]
        static final Comparator<String> CASE_INSENSITIVE_ORDER = null;
        String$() { }
        String$(@NotNull String original) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified char [] value) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified char [] value, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified int [] codePoints, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] ascii, int hibyte, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] ascii, int hibyte) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] bytes, int offset, int length, @NotNull String charsetName) { }
        String$(
            /*@Independent[T]*/ @NotNull @NotModified byte [] bytes,
            int offset,
            int length,
            /*@Independent[T]*/ @NotNull Charset charset) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] bytes, @NotNull String charsetName) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] bytes, /*@Independent[T]*/ @NotNull Charset charset) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] bytes, int offset, int length) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified byte [] bytes) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified StringBuffer buffer) { }
        String$(/*@Independent[T]*/ @NotNull @NotModified StringBuilder builder) { }
        //override from java.lang.CharSequence
        //frequency 17
        //@NotModified[T]
        int length() { return 0; }

        //override from java.lang.CharSequence
        //frequency 21
        //@NotModified[T]
        boolean isEmpty() { return false; }

        //override from java.lang.CharSequence
        //@NotModified[T]
        char charAt(int index) { return '\0'; }

        //@NotModified[T]
        int codePointAt(int index) { return 0; }

        //@NotModified[T]
        int codePointBefore(int index) { return 0; }

        //@NotModified[T]
        int codePointCount(int beginIndex, int endIndex) { return 0; }

        //@NotModified[T]
        int offsetByCodePoints(int index, int codePointOffset) { return 0; }

        //@NotModified[T]
        void getChars(int srcBegin, int srcEnd, /*@Independent[M]*/ @NotNull char [] dst, int dstBegin) { }

        //@NotModified[T]
        void getBytes(int srcBegin, int srcEnd, /*@Independent[M]*/ @NotNull byte [] dst, int dstBegin) { }

        //@Independent[O] @NotModified[T]
        byte [] getBytes(@NotNull String charsetName) { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        byte [] getBytes(/*@Independent[M]*/ @NotNull Charset charset) { return null; }

        //frequency 2
        //@Independent[O] @NotModified[T]
        byte [] getBytes() { return null; }

        //override from java.lang.Object
        //frequency 40
        //@NotModified[T]

        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object anObject) {
            return false;
        }

        //@NotModified[T]
        boolean contentEquals(@NotModified /*@Independent[M]*/ @NotNull StringBuffer sb) { return false; }

        //@NotModified[T]
        boolean contentEquals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull CharSequence cs) {
            return false;
        }

        //frequency 13
        //@NotModified[T]
        boolean equalsIgnoreCase(@Nullable String anotherString) { return false; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@NotNull[H]*/ @NotNull String anotherString) { return 0; }

        //@NotModified[T]
        int compareToIgnoreCase(@NotNull String str) { return 0; }

        //@NotModified[T]
        boolean regionMatches(int toffset, @NotNull String other, int ooffset, int len) { return false; }

        //@NotModified[T]
        boolean regionMatches(boolean ignoreCase, int toffset, @NotNull String other, int ooffset, int len) { return false; }

        //@NotModified[T]
        boolean startsWith(@NotNull String prefix, int toffset) { return false; }

        //frequency 4
        //@NotModified[T]
        boolean startsWith(@NotNull String prefix) { return false; }

        //frequency 2
        //@NotModified[T]
        boolean endsWith(@NotNull String suffix) { return false; }

        //override from java.lang.Object
        //frequency 3
        //@NotModified[T]
        public int hashCode() { return 0; }

        //@NotModified[T]
        int indexOf(int ch) { return 0; }

        //@NotModified[T]
        int indexOf(int ch, int fromIndex) { return 0; }

        //@NotModified[T]
        int indexOf(int ch, int beginIndex, int endIndex) { return 0; }

        //@NotModified[T]
        int lastIndexOf(int ch) { return 0; }

        //@NotModified[T]
        int lastIndexOf(int ch, int fromIndex) { return 0; }

        //frequency 1
        //@NotModified[T]
        int indexOf(@NotNull String str) { return 0; }

        //@NotModified[T]
        int indexOf(@NotNull String str, int fromIndex) { return 0; }

        //@NotModified[T]
        int indexOf(@NotNull String str, int beginIndex, int endIndex) { return 0; }

        //@NotModified[T]
        int lastIndexOf(@NotNull String str) { return 0; }

        //@NotModified[T]
        int lastIndexOf(@NotNull String str, int fromIndex) { return 0; }

        //frequency 2
        //@NotModified[T]
        String substring(int beginIndex) { return null; }

        //frequency 6
        //@NotModified[T]
        String substring(int beginIndex, int endIndex) { return null; }

        //override from java.lang.CharSequence
        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        CharSequence subSequence(int beginIndex, int endIndex) { return null; }

        //frequency 1
        //@NotModified[T]
        String concat(@NotNull String str) { return null; }

        //frequency 2
        //@NotModified[T]
        String replace(char oldChar, char newChar) { return null; }

        //frequency 1
        //@NotModified[T]
        boolean matches(@NotNull String regex) { return false; }

        //frequency 6
        //@NotModified[T]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull CharSequence s) { return false; }

        //@NotModified[T]
        String replaceFirst(@NotNull String regex, @NotNull String replacement) { return null; }

        //frequency 7
        //@NotModified[T]
        String replaceAll(@NotNull String regex, @NotNull String replacement) { return null; }

        //frequency 8
        //@NotModified[T]

        String replace(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull CharSequence target,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull CharSequence replacement) { return null; }

        //@Independent[O] @NotModified[T]
        String [] split(@NotNull String regex, int limit) { return null; }

        //@Independent[O] @NotModified[T]
        String [] splitWithDelimiters(@NotNull String regex, int limit) { return null; }

        //frequency 9
        //@Independent[O] @NotModified[T]
        String [] split(@NotNull String regex) { return null; }

        //@NotModified[T]
        static String join(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ @NotNull CharSequence delimiter,
            /*@Independent[T]*/ @NotNull(nullableArguments = 0) @NotModified CharSequence ... elements) { return null; }

        // join only iterates its elements: @NotModified is a contract here, not the type's default, since Iterable is
        // a @Container (a Java field passed to join must stay unmodified: TestKotlinCollectionReadsVsJava's control row)
        //@NotModified[T]
        static String join(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ @NotNull CharSequence delimiter,
            /*@Independent[T]*/ @NotNull(nullableArguments = 0) @NotModified Iterable<? extends CharSequence> elements) { return null; }

        //frequency 3
        //@NotModified[T]
        String toLowerCase(/*@Independent[M]*/ @NotNull Locale locale) { return null; }

        //frequency 36
        //@NotModified[T]
        String toLowerCase() { return null; }

        //@NotModified[T]
        String toUpperCase(/*@Independent[M]*/ @NotNull Locale locale) { return null; }

        //frequency 3
        //@NotModified[T]
        String toUpperCase() { return null; }

        //frequency 14
        //@NotModified[T]
        String trim() { return null; }

        //@NotModified[T]
        String strip() { return null; }

        //@NotModified[T]
        String stripLeading() { return null; }

        //@NotModified[T]
        String stripTrailing() { return null; }

        //frequency 8
        //@NotModified[T]
        boolean isBlank() { return false; }

        //@Independent[O] @NotModified[T]
        Stream<String> lines() { return null; }

        //@NotModified[T]
        String indent(int n) { return null; }

        //@NotModified[T]
        String stripIndent() { return null; }

        //@NotModified[T]
        String translateEscapes() { return null; }

        //@Independent[O] @NotModified[T]
        <R> R transform(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull Function<? super String, ? extends R> f) {
            return null;
        }

        //override from java.lang.CharSequence, java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.CharSequence
        //@Independent[O] @NotModified[T]
        IntStream chars() { return null; }

        //override from java.lang.CharSequence
        //@Independent[O] @NotModified[T]
        IntStream codePoints() { return null; }

        //@Independent[O] @NotModified[T]
        char [] toCharArray() { return null; }

        //@NotModified[T]
        static String format(@NotNull String format, @NotNull(nullableArguments = 0) @NotModified /*@Independent[T]*/ Object ... args) { return null; }

        //@NotModified[T]
        static String format(/*@Independent[T]*/ @Nullable Locale l, @NotNull String format,
            @NotNull(nullableArguments = 0) @NotModified /*@Independent[T]*/ Object ... args) {
            return null;
        }

        //frequency 37
        //@NotModified[T]
        String formatted(@NotNull(nullableArguments = 0) @NotModified /*@Independent[M]*/ Object ... args) { return null; }

        //frequency 1
        //@NotModified[T]
        static String valueOf(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ @Nullable Object obj) { return null; }

        //@NotModified[T]
        static String valueOf(@NotModified /*@Independent[T]*/ @NotNull char [] data) { return null; }

        //@NotModified[T]
        static String valueOf(@NotModified /*@Independent[T]*/ @NotNull char [] data, int offset, int count) { return null; }

        //@NotModified[T]
        static String copyValueOf(@NotModified /*@Independent[T]*/ @NotNull char [] data, int offset, int count) { return null; }

        //@NotModified[T]
        static String copyValueOf(@NotModified /*@Independent[T]*/ @NotNull char [] data) { return null; }

        //frequency 3
        //@NotModified[T]
        static String valueOf(boolean b) { return null; }

        //@NotModified[T]
        static String valueOf(char c) { return null; }

        //frequency 15
        //@NotModified[T]
        static String valueOf(int i) { return null; }

        //frequency 6
        //@NotModified[T]
        static String valueOf(long l) { return null; }

        //frequency 7
        //@NotModified[T]
        static String valueOf(float f) { return null; }

        //@NotModified[T]
        static String valueOf(double d) { return null; }

        //@NotModified[T]
        String intern() { return null; }

        //@NotModified[T]
        String repeat(int count) { return null; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<String> describeConstable() { return null; }

        //override from java.lang.constant.ConstantDesc
        //@NotModified[T]
        String resolveConstantDesc(/*@Independent[M]*/ @NotNull MethodHandles.Lookup lookup) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int compareToFoldCase(@NotNull java.lang.String p0) { return 0; }
        int encodedLength(@NotNull java.nio.charset.Charset p0) { return 0; }
        boolean equalsFoldCase(@Nullable java.lang.String p0) { return false; }
    }

    //public final class Short extends Number implements Comparable<Short>, Constable
    @ImmutableContainer
    class Short$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Short$(@NotNull java.lang.String p0) { }
        int compareTo(@NotNull java.lang.Short p0) { return 0; }
        static java.lang.Short decode(@NotNull java.lang.String p0) { return null; }
        static short parseShort(@NotNull java.lang.String p0) { return 0; }
        static short parseShort(@NotNull java.lang.String p0, int p1) { return 0; }
        static java.lang.Short valueOf(@NotNull java.lang.String p0) { return null; }
        static java.lang.Short valueOf(@NotNull java.lang.String p0, int p1) { return null; }
    }

    //public final class StackTraceElement implements Serializable
    @ImmutableContainer
    class StackTraceElement$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StackTraceElement$(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @NotNull java.lang.String p3, @NotNull java.lang.String p4, @Nullable java.lang.String p5, int p6) { }
        StackTraceElement$(@NotNull java.lang.String p0, @NotNull java.lang.String p1, @Nullable java.lang.String p2, int p3) { }
        @Nullable java.lang.String getClassLoaderName() { return null; }
        @Nullable java.lang.String getFileName() { return null; }
        @Nullable java.lang.String getModuleName() { return null; }
        @Nullable java.lang.String getModuleVersion() { return null; }
    }

    //public final class StringBuffer extends AbstractStringBuilder implements Appendable, Serializable, Comparable<StringBuffer>, CharSequence
    @Independent
    class StringBuffer$ {
        //frequency 3
        StringBuffer$() { }
        StringBuffer$(int capacity) { }
        StringBuffer$(@NotNull String str) { }
        StringBuffer$(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ @NotNull @NotModified CharSequence seq) { }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[O] @NotNull[H]*/ @NotNull @NotModified StringBuffer another) { return 0; }

        //override from java.lang.AbstractStringBuilder
        //frequency 3
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified Object obj) { return null; }

        //override from java.lang.AbstractStringBuilder
        //frequency 6
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(@Nullable String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(/*@Independent[H] @NotModified[O]*/ @Nullable @NotModified StringBuffer sb) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s,
            int start,
            int end) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(/*@Independent[H] @NotModified[O] @NotNull[H]*/ @NotNull @NotModified char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(/*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str, int offset, int len) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(boolean b) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(char c) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(int i) { return null; }

        //override from java.lang.AbstractStringBuilder
        //frequency 1
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(long lng) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(float f) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer append(double d) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer appendCodePoint(int codePoint) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer delete(int start, int end) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer deleteCharAt(int index) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer replace(int start, int end, @NotNull String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer insert(int index, /*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str, int offset, int len) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer insert(int offset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified Object obj) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, @Nullable String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, /*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer insert(int dstOffset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer insert(
            int dstOffset,
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s,
            int start,
            int end) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, boolean b) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, char c) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, int i) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, long l) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, float f) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer insert(int offset, double d) { return null; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int indexOf(@NotNull String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int indexOf(@NotNull String str, int fromIndex) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int lastIndexOf(@NotNull String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int lastIndexOf(@NotNull String str, int fromIndex) { return 0; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer reverse() { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuffer repeat(int codePoint, int count) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuffer repeat(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence cs, int count) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder, java.lang.CharSequence, java.lang.Object
        //frequency 3
        //@NotNull[H]
        @NotModified
        public String toString() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void getChars(int p0, int p1, @NotNull char[] p2, int p3) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified int capacity() { return 0; }
        @NotModified int codePointAt(int p0) { return 0; }
        @NotModified int codePointBefore(int p0) { return 0; }
        @NotModified int codePointCount(int p0, int p1) { return 0; }
        @NotModified int offsetByCodePoints(int p0, int p1) { return 0; }
        @NotModified java.lang.String substring(int p0) { return null; }
        @NotModified java.lang.String substring(int p0, int p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void ensureCapacity(int p0) { }
        @Modified void setCharAt(int p0, char p1) { }
        @Modified void setLength(int p0) { }
        @Modified void trimToSize() { }
    }

    //public final class StringBuilder extends AbstractStringBuilder implements Appendable, Serializable, Comparable<StringBuilder>, CharSequence
    @Independent
    class StringBuilder$ {
        //frequency 3
        StringBuilder$() { }
        StringBuilder$(int capacity) { }
        StringBuilder$(@NotNull String str) { }
        StringBuilder$(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ @NotNull @NotModified CharSequence seq) { }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[O] @NotNull[H]*/ @NotNull @NotModified StringBuilder another) { return 0; }

        //override from java.lang.AbstractStringBuilder
        //frequency 3
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified Object obj) { return null; }

        //override from java.lang.AbstractStringBuilder
        //frequency 6
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(@Nullable String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O]*/ @Nullable @NotModified StringBuffer sb) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s,
            int start,
            int end) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O] @NotNull[H]*/ @NotNull @NotModified char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str, int offset, int len) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(boolean b) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(char c) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(int i) { return null; }

        //override from java.lang.AbstractStringBuilder
        //frequency 1
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(long lng) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(float f) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(double d) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder appendCodePoint(int codePoint) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder delete(int start, int end) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder deleteCharAt(int index) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder replace(int start, int end, @NotNull String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int index, /*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str, int offset, int len) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int offset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified Object obj) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, @Nullable String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, /*@Independent[H] @NotModified[O]*/ @NotNull @NotModified char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int dstOffset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(
            int dstOffset,
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence s,
            int start,
            int end) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, boolean b) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, char c) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, int i) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, long l) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, float f) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, double d) { return null; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int indexOf(@NotNull String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int indexOf(@NotNull String str, int fromIndex) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int lastIndexOf(@NotNull String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        @NotModified int lastIndexOf(@NotNull String str, int fromIndex) { return 0; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder reverse() { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder repeat(int codePoint, int count) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder repeat(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ @Nullable @NotModified CharSequence cs, int count) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder, java.lang.CharSequence, java.lang.Object
        //frequency 3
        //@NotNull[H]
        @NotModified
        public String toString() { return null; }
    }

    //public final class System
    @Independent
    class System$ {
        @IgnoreModifications @NotNull static final InputStream in = null;
        @IgnoreModifications @NotNull static final PrintStream out = null;
        @IgnoreModifications @NotNull static final PrintStream err = null;
        //public interface Logger
        // a @Container, as slf4j's Logger (libs/log/OrgSlf4j): log calls read their arguments; the calls modify, and
        // carry @IgnoreModifications
        @Container @Independent
        class Logger {
            //public enum Level extends Enum<Level>
            class Level {
                //@NotNull[O]
                static final System.Logger.Level ALL = null;

                //@NotNull[O]
                static final System.Logger.Level TRACE = null;

                //@NotNull[O]
                static final System.Logger.Level DEBUG = null;

                //@NotNull[O]
                static final System.Logger.Level INFO = null;

                //@NotNull[O]
                static final System.Logger.Level WARNING = null;

                //@NotNull[O]
                static final System.Logger.Level ERROR = null;

                //@NotNull[O]
                static final System.Logger.Level OFF = null;
                static System.Logger.Level [] values() { return null; }
                static System.Logger.Level valueOf(String name) { return null; }
                String getName() { return null; }
                int getSeverity() { return 0; }
            }
            @NotModified String getName() { return null; }
            @NotModified boolean isLoggable(/*@Independent[M]*/ @NotNull System.Logger.Level level) { return false; }
            @IgnoreModifications void log(/*@Independent[T]*/ @NotNull System.Logger.Level level, @NotNull String msg) { }
            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                /*@IgnoreModifications[T] @Independent[T]*/ @NotNull Supplier<String> msgSupplier) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ @NotNull Object obj) { }

            @IgnoreModifications
            void log(/*@Independent[T]*/ @NotNull System.Logger.Level level, @NotNull String msg, /*@Independent[T]*/ @NotNull Throwable thrown) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                /*@IgnoreModifications[T] @Independent[T]*/ @NotNull Supplier<String> msgSupplier,
                /*@Independent[T]*/ @NotNull Throwable thrown) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                @NotNull String format,
                @NotNull @NotModified /*@Independent[T]*/ Object ... params) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                /*@Independent[T]*/ @NotNull ResourceBundle resourceBundle,
                @NotNull String string,
                /*@Independent[T]*/ @NotNull Throwable throwable) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ @NotNull System.Logger.Level level,
                /*@Independent[T]*/ @NotNull ResourceBundle resourceBundle,
                @NotNull String string,
                @NotNull @NotModified /*@Independent[T]*/ Object ... object) { }
        }

        //public abstract static class LoggerFinder
        class LoggerFinder {
            System.Logger getLogger(@NotNull String string, @NotNull Module module) { return null; }
            System.Logger getLocalizedLogger(@NotNull String name, @NotNull ResourceBundle bundle, @NotNull Module module) { return null; }
            static System.LoggerFinder getLoggerFinder() { return null; }
        }
        // reconfiguring the process-wide streams is a static side effect (road-to-immutability section 050): the
        // effect (replacing System.out/err/in) is invisible from JDK source, so it is contracted here on the safe
        // surface. A caller inherits the static side effect, which the @IgnoreModifications containment guard uses
        // to tell "reconfiguring the mechanics" (an escape) from "just using System.out" (confined).
        @StaticSideEffects static void setIn(@NotModified /*@Independent[T]*/ @NotNull InputStream in) { }
        @StaticSideEffects static void setOut(@NotModified /*@Independent[T]*/ @NotNull PrintStream out) { }
        @StaticSideEffects static void setErr(@NotModified /*@Independent[T]*/ @NotNull PrintStream err) { }
        //@Independent[O]
        @Nullable static Console console() { return null; }

        //@Independent[O]
        @Nullable static Channel inheritedChannel() { return null; }
        static void setSecurityManager(@NotModified /*@Independent[T]*/ @Nullable SecurityManager sm) { }
        //@Independent[O]
        @Nullable static SecurityManager getSecurityManager() { return null; }

        //frequency 4
        @IgnoreModifications
        static long currentTimeMillis() { return 0L; }
        static long nanoTime() { return 0L; }
        static void arraycopy(
            /*@Immutable(hc=true)[T] @Independent[T]*/ @Independent(hcParameters = { 2 }) @NotModified @NotNull Object object,
            int i,
            /*@Immutable(hc=true)[T] @NotModified[T]*/ @NotNull Object object1,
            int i1,
            int i2) { }

        static int identityHashCode(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ @Nullable Object object) {
            return 0;
        }

        //@Independent[O]
        static Properties getProperties() { return null; }
        static String lineSeparator() { return null; }
        static void setProperties(@NotModified /*@Independent[T]*/ @Nullable Properties props) { }
        //frequency 2
        @NotModified
        @Nullable static String getProperty(@NotNull String key) { return null; }
        static String getProperty(@NotNull String key, @Nullable String def) { return null; }
        @Nullable static String setProperty(@NotNull String key, @NotNull String value) { return null; }
        @Nullable static String clearProperty(@NotNull String key) { return null; }
        //frequency 1
        @NotModified
        @Nullable static String getenv(@NotNull String name) { return null; }

        //@Independent[O]
        static Map<String, String> getenv() { return null; }

        //@Independent[O]
        static System.Logger getLogger(@NotNull String name) { return null; }

        //@Independent[O]
        static System.Logger getLogger(@NotNull String name, @NotModified /*@Independent[T]*/ @NotNull ResourceBundle bundle) { return null; }

        //frequency 1
        @IgnoreModifications
        static void exit(int status) { }
        static void gc() { }
        static void runFinalization() { }
        static void load(@NotNull String filename) { }
        static void loadLibrary(@NotNull String libname) { }
        static String mapLibraryName(@NotNull String string) { return null; }
    }

    //public class Thread implements Runnable
    class Thread$ {
        static final int MIN_PRIORITY = 0;
        static final int NORM_PRIORITY = 0;
        static final int MAX_PRIORITY = 0;
        //public interface Builder
        class Builder {
            //public interface OfPlatform implements Thread.Builder
            class OfPlatform {
                Thread.Builder.OfPlatform name(String string) { return null; }
                Thread.Builder.OfPlatform name(String string, long l) { return null; }
                Thread.Builder.OfPlatform inheritInheritableThreadLocals(boolean b) { return null; }
                Thread.Builder.OfPlatform uncaughtExceptionHandler(
                    Thread.UncaughtExceptionHandler uncaughtExceptionHandler) { return null; }
                Thread.Builder.OfPlatform group(ThreadGroup threadGroup) { return null; }
                Thread.Builder.OfPlatform daemon(boolean b) { return null; }
                Thread.Builder.OfPlatform daemon() { return null; }
                Thread.Builder.OfPlatform priority(int i) { return null; }
                Thread.Builder.OfPlatform stackSize(long l) { return null; }
            }

            //public interface OfVirtual implements Thread.Builder
            class OfVirtual {
                Thread.Builder.OfVirtual name(String string) { return null; }
                Thread.Builder.OfVirtual name(String string, long l) { return null; }
                Thread.Builder.OfVirtual inheritInheritableThreadLocals(boolean b) { return null; }
                Thread.Builder.OfVirtual uncaughtExceptionHandler(
                    Thread.UncaughtExceptionHandler uncaughtExceptionHandler) { return null; }
            }
            Thread.Builder name(@NotNull String string) { return null; }
            Thread.Builder name(@NotNull String string, long l) { return null; }
            Thread.Builder inheritInheritableThreadLocals(boolean b) { return null; }
            Thread.Builder uncaughtExceptionHandler(@NotNull Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
                return null;
            }
            Thread unstarted(@NotNull Runnable runnable) { return null; }
            @Modified Thread start(@NotNull Runnable runnable) { return null; }
            ThreadFactory factory() { return null; }
        }

        //public enum State extends Enum<State>
        class State {
            //@NotNull[O]
            static final Thread.State NEW = null;

            //@NotNull[O]
            static final Thread.State RUNNABLE = null;

            //@NotNull[O]
            static final Thread.State BLOCKED = null;

            //@NotNull[O]
            static final Thread.State WAITING = null;

            //@NotNull[O]
            static final Thread.State TIMED_WAITING = null;

            //@NotNull[O]
            static final Thread.State TERMINATED = null;
            static Thread.State [] values() { return null; }
            static Thread.State valueOf(String name) { return null; }
        }

        //public interface UncaughtExceptionHandler
        class UncaughtExceptionHandler {void uncaughtException(@NotNull Thread thread, @NotNull Throwable throwable) { } }
        Thread$() { }
        Thread$(@Nullable Runnable task) { }
        Thread$(@Nullable ThreadGroup group, @Nullable Runnable task) { }
        Thread$(@NotNull String name) { }
        Thread$(@Nullable ThreadGroup group, @NotNull String name) { }
        Thread$(@Nullable Runnable task, @NotNull String name) { }
        Thread$(@Nullable ThreadGroup group, @Nullable Runnable task, @NotNull String name) { }
        Thread$(@Nullable ThreadGroup group, @Nullable Runnable task, @NotNull String name, long stackSize) { }
        Thread$(@Nullable ThreadGroup group, @Nullable Runnable task, @NotNull String name, long stackSize, boolean b) { }
        //frequency 3
        @NotModified
        static Thread currentThread() { return null; }
        static void yield() { }
        //frequency 2
        @IgnoreModifications
        static void sleep(long millis) { }
        static void sleep(long millis, int i) { }
        static void sleep(@NotNull Duration duration) { }
        static void onSpinWait() { }
        static Thread.Builder.OfPlatform ofPlatform() { return null; }
        static Thread.Builder.OfVirtual ofVirtual() { return null; }
        static Thread startVirtualThread(@NotNull Runnable task) { return null; }
        @NotModified boolean isVirtual() { return false; }
        @Modified void start() { }
        //override from java.lang.Runnable
        void run() { }
        void stop() { }
        //frequency 3
        @Modified @IgnoreModifications
        void interrupt() { }
        static boolean interrupted() { return false; }
        @NotModified boolean isInterrupted() { return false; }
        @NotModified boolean isAlive() { return false; }
        @Modified void setPriority(int newPriority) { }
        @NotModified int getPriority() { return 0; }
        @Modified void setName(@NotNull String name) { }
        @NotModified String getName() { return null; }
        @Nullable @NotModified ThreadGroup getThreadGroup() { return null; }
        static int activeCount() { return 0; }
        static int enumerate(@NotNull(nullableArguments = 0) Thread [] tarray) { return 0; }
        @NotModified void join(long millis) { }
        @NotModified void join(long millis, int i) { }
        @NotModified void join() { }
        @NotModified boolean join(@NotNull Duration duration) { return false; }
        static void dumpStack() { }
        @Modified void setDaemon(boolean on) { }
        @NotModified boolean isDaemon() { return false; }
        @NotModified void checkAccess() { }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @Nullable @NotModified ClassLoader getContextClassLoader() { return null; }
        @Modified void setContextClassLoader(@Nullable @NotModified ClassLoader cl) { }
        static boolean holdsLock(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object) {
            return false;
        }
        @NotModified StackTraceElement [] getStackTrace() { return null; }
        //@Independent[T]
        @NotModified static Map<Thread, StackTraceElement []> getAllStackTraces() { return null; }
        @NotModified long getId() { return 0L; }
        @NotModified long threadId() { return 0L; }
        @NotModified Thread.State getState() { return null; }
        static void setDefaultUncaughtExceptionHandler(@NotModified @Nullable Thread.UncaughtExceptionHandler ueh) { }
        @Nullable @NotModified static Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() { return null; }
        @Nullable @NotModified Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() { return null; }
        @Modified void setUncaughtExceptionHandler(@NotModified @Nullable Thread.UncaughtExceptionHandler ueh) { }
    }

    //public class Throwable implements Serializable
    class Throwable$ {
        //override has frequency 1
        Throwable$() { }

        //override has frequency 8
        Throwable$(@Nullable String message) { }

        //override has frequency 1
        Throwable$(@Nullable String message, @Nullable @NotModified Throwable cause) { }

        //override has frequency 4
        Throwable$(@Nullable @NotModified Throwable cause) { }

        //frequency 17
        @NotModified
        @Nullable String getMessage() { return null; }
        @NotModified @Nullable String getLocalizedMessage() { return null; }
        //frequency 7
        @NotModified
        @Nullable Throwable getCause() { return null; }
        @Modified Throwable initCause(@Nullable @NotModified Throwable cause) { return null; }
        public String toString() { return null; }
        @IgnoreModifications void printStackTrace() { }
        @NotModified void printStackTrace(/*@Independent[M]*/ @NotNull PrintStream s) { }
        @NotModified void printStackTrace(/*@Independent[M]*/ @NotNull PrintWriter s) { }
        @Modified Throwable fillInStackTrace() { return null; }
        @NotModified StackTraceElement [] getStackTrace() { return null; }
        @Modified void setStackTrace(@NotModified @NotNull StackTraceElement [] stackTrace) { }
        @Modified void addSuppressed(@NotNull @NotModified Throwable exception) { }
        @NotModified Throwable [] getSuppressed() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Throwable$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1, boolean p2, boolean p3) { }
    }

    // null contracts (docs/design/nullability.md M2), formerly the analyzer's LibraryNullness
    class ThreadLocal$<T> {
        @NotModified @Nullable T get() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <S> java.lang.ThreadLocal<S> withInitial(@NotNull java.util.function.Supplier<? extends S> p0) { return null; }

        // get() initializes the current thread's value on first use, in the thread's map: no change a caller of
        // this object can observe. set and remove are the changes.
        @Modified void set(T p0) { }
        @Modified void remove() { }
    }

    //public java.lang.AbstractMethodError (shadow created for its nullability, generated)
    class AbstractMethodError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        AbstractMethodError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ArithmeticException (shadow created for its nullability, generated)
    class ArithmeticException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ArithmeticException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ArrayIndexOutOfBoundsException (shadow created for its nullability, generated)
    class ArrayIndexOutOfBoundsException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ArrayIndexOutOfBoundsException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ArrayStoreException (shadow created for its nullability, generated)
    class ArrayStoreException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ArrayStoreException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ClassCastException (shadow created for its nullability, generated)
    class ClassCastException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ClassCastException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ClassCircularityError (shadow created for its nullability, generated)
    class ClassCircularityError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ClassCircularityError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ClassFormatError (shadow created for its nullability, generated)
    class ClassFormatError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ClassFormatError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ClassLoader (shadow created for its nullability, generated)
    class ClassLoader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ClassLoader$(@Nullable java.lang.ClassLoader p0) { }
        ClassLoader$(@Nullable java.lang.String p0, @Nullable java.lang.ClassLoader p1) { }
        java.lang.Class<?> defineClass(@Nullable java.lang.String p0, @NotNull java.nio.ByteBuffer p1, @Nullable java.security.ProtectionDomain p2) { return null; }
        java.lang.Class<?> defineClass(@Nullable java.lang.String p0, @NotNull byte[] p1, int p2, int p3) { return null; }
        java.lang.Class<?> defineClass(@Nullable java.lang.String p0, @NotNull byte[] p1, int p2, int p3, @Nullable java.security.ProtectionDomain p4) { return null; }
        java.lang.Class<?> defineClass(@NotNull byte[] p0, int p1, int p2) { return null; }
        java.lang.Package definePackage(@NotNull java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.String p5, @Nullable java.lang.String p6, @Nullable java.net.URL p7) { return null; }
        java.lang.Class<?> findClass(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.Class<?> findClass(@Nullable java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        @Nullable java.lang.String findLibrary(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.Class<?> findLoadedClass(@NotNull java.lang.String p0) { return null; }
        @Nullable java.net.URL findResource(@NotNull java.lang.String p0) { return null; }
        @Nullable java.net.URL findResource(@Nullable java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        java.util.Enumeration<java.net.URL> findResources(@NotNull java.lang.String p0) { return null; }
        java.lang.Class<?> findSystemClass(@NotNull java.lang.String p0) { return null; }
        java.lang.Object getClassLoadingLock(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.lang.Package getDefinedPackage(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.lang.String getName() { return null; }
        @Nullable java.lang.Package getPackage(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.lang.ClassLoader getParent() { return null; }
        @NotModified @Nullable java.net.URL getResource(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.io.InputStream getResourceAsStream(@NotNull java.lang.String p0) { return null; }
        @NotModified java.util.Enumeration<java.net.URL> getResources(@NotNull java.lang.String p0) { return null; }
        @Nullable static java.net.URL getSystemResource(@NotNull java.lang.String p0) { return null; }
        @Nullable static java.io.InputStream getSystemResourceAsStream(@NotNull java.lang.String p0) { return null; }
        static java.util.Enumeration<java.net.URL> getSystemResources(@NotNull java.lang.String p0) { return null; }
        @NotModified java.lang.Class<?> loadClass(@NotNull java.lang.String p0) { return null; }
        @NotModified java.lang.Class<?> loadClass(@NotNull java.lang.String p0, boolean p1) { return null; }
        void resolveClass(@NotNull java.lang.Class<?> p0) { }
        @NotModified java.util.stream.Stream<java.net.URL> resources(@NotNull java.lang.String p0) { return null; }
        @Modified void setClassAssertionStatus(@NotNull java.lang.String p0, boolean p1) { }
        @Modified void setPackageAssertionStatus(@Nullable java.lang.String p0, boolean p1) { }
        void setSigners(@NotNull java.lang.Class<?> p0, @NotNull java.lang.Object[] p1) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.Package[] getDefinedPackages() { return null; }
        @NotModified java.lang.Module getUnnamedModule() { return null; }
        @NotModified boolean isRegisteredAsParallelCapable() { return false; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void clearAssertionStatus() { }
        @Modified void setDefaultAssertionStatus(boolean p0) { }
    }

    //public java.lang.ClassNotFoundException (shadow created for its nullability, generated)
    class ClassNotFoundException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ClassNotFoundException$(@Nullable java.lang.String p0) { }
        ClassNotFoundException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.Throwable getException() { return null; }
    }

    //public java.lang.ClassValue (shadow created for its nullability, generated)
    class ClassValue$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        T computeValue(@NotNull java.lang.Class<?> p0) { return null; }
        @NotModified T get(@NotNull java.lang.Class<?> p0) { return null; }
        @Modified void remove(@NotNull java.lang.Class<?> p0) { }
    }

    //public java.lang.CloneNotSupportedException (shadow created for its nullability, generated)
    class CloneNotSupportedException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CloneNotSupportedException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.EnumConstantNotPresentException (shadow created for its nullability, generated)
    class EnumConstantNotPresentException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        EnumConstantNotPresentException$(@NotNull java.lang.Class<? extends java.lang.Enum> p0, @NotNull java.lang.String p1) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.String constantName() { return null; }
        @NotModified java.lang.Class<? extends java.lang.Enum> enumType() { return null; }
    }

    //public java.lang.ExceptionInInitializerError (shadow created for its nullability, generated)
    class ExceptionInInitializerError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ExceptionInInitializerError$(@Nullable java.lang.String p0) { }
        ExceptionInInitializerError$(@NotModified @Nullable java.lang.Throwable p0) { }
        @NotModified @Nullable java.lang.Throwable getException() { return null; }
    }

    //public java.lang.IO (shadow created for its nullability, generated)
    class IO$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static void print(@Nullable java.lang.Object p0) { }
        static void println(@Nullable java.lang.Object p0) { }
        @Nullable static java.lang.String readln() { return null; }
        @Nullable static java.lang.String readln(@Nullable java.lang.String p0) { return null; }
    }

    //public java.lang.IllegalAccessError (shadow created for its nullability, generated)
    class IllegalAccessError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IllegalAccessError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.IllegalAccessException (shadow created for its nullability, generated)
    class IllegalAccessException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IllegalAccessException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.IllegalCallerException (shadow created for its nullability, generated)
    class IllegalCallerException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IllegalCallerException$(@Nullable java.lang.String p0) { }
        IllegalCallerException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        IllegalCallerException$(@NotModified @Nullable java.lang.Throwable p0) { }
    }

    //public java.lang.IllegalMonitorStateException (shadow created for its nullability, generated)
    class IllegalMonitorStateException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IllegalMonitorStateException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.IllegalThreadStateException (shadow created for its nullability, generated)
    class IllegalThreadStateException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IllegalThreadStateException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.IncompatibleClassChangeError (shadow created for its nullability, generated)
    class IncompatibleClassChangeError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IncompatibleClassChangeError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.IndexOutOfBoundsException (shadow created for its nullability, generated)
    class IndexOutOfBoundsException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IndexOutOfBoundsException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.InstantiationError (shadow created for its nullability, generated)
    class InstantiationError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InstantiationError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.InstantiationException (shadow created for its nullability, generated)
    class InstantiationException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InstantiationException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.InternalError (shadow created for its nullability, generated)
    class InternalError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InternalError$(@Nullable java.lang.String p0) { }
        InternalError$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        InternalError$(@NotModified @Nullable java.lang.Throwable p0) { }
    }

    //public java.lang.InterruptedException (shadow created for its nullability, generated)
    class InterruptedException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InterruptedException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.LinkageError (shadow created for its nullability, generated)
    class LinkageError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LinkageError$(@Nullable java.lang.String p0) { }
        LinkageError$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
    }

    //public java.lang.MatchException (shadow created for its nullability, generated)
    class MatchException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MatchException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
    }

    //public java.lang.Module (shadow created for its nullability, generated)
    class Module$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified java.lang.Module addExports(@NotNull java.lang.String p0, @NotModified @NotNull java.lang.Module p1) { return null; }
        @Modified java.lang.Module addOpens(@NotNull java.lang.String p0, @NotModified @NotNull java.lang.Module p1) { return null; }
        @Modified java.lang.Module addReads(@NotModified @NotNull java.lang.Module p0) { return null; }
        @Modified java.lang.Module addUses(@NotNull java.lang.Class<?> p0) { return null; }
        @NotModified boolean canRead(@NotModified @NotNull java.lang.Module p0) { return false; }
        @NotModified boolean canUse(@NotNull java.lang.Class<?> p0) { return false; }
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        @NotModified @Nullable java.lang.ClassLoader getClassLoader() { return null; }
        @NotModified @Nullable java.lang.module.ModuleDescriptor getDescriptor() { return null; }
        @NotModified @Nullable java.lang.ModuleLayer getLayer() { return null; }
        @NotModified @Nullable java.lang.String getName() { return null; }
        @NotModified @Nullable java.io.InputStream getResourceAsStream(@NotNull java.lang.String p0) { return null; }
        @NotModified boolean isExported(@NotNull java.lang.String p0) { return false; }
        @NotModified boolean isExported(@NotNull java.lang.String p0, @NotModified @NotNull java.lang.Module p1) { return false; }
        @NotModified boolean isOpen(@NotNull java.lang.String p0) { return false; }
        @NotModified boolean isOpen(@NotNull java.lang.String p0, @NotModified @NotNull java.lang.Module p1) { return false; }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.util.Set<java.lang.String> getPackages() { return null; }
        @NotModified boolean isNamed() { return false; }
        @NotModified boolean isNativeAccessEnabled() { return false; }
    }

    //public java.lang.ModuleLayer (shadow created for its nullability, generated)
    class ModuleLayer$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static java.lang.ModuleLayer boot() { return null; }
        @NotModified java.lang.ModuleLayer defineModules(@NotNull java.lang.module.Configuration p0, @NotNull java.util.function.Function<java.lang.String, java.lang.ClassLoader> p1) { return null; }
        static java.lang.ModuleLayer.Controller defineModules(@NotNull java.lang.module.Configuration p0, @NotNull java.util.List<java.lang.ModuleLayer> p1, @NotNull java.util.function.Function<java.lang.String, java.lang.ClassLoader> p2) { return null; }
        @NotModified java.lang.ModuleLayer defineModulesWithManyLoaders(@NotNull java.lang.module.Configuration p0, @Nullable java.lang.ClassLoader p1) { return null; }
        static java.lang.ModuleLayer.Controller defineModulesWithManyLoaders(@NotNull java.lang.module.Configuration p0, @NotNull java.util.List<java.lang.ModuleLayer> p1, @Nullable java.lang.ClassLoader p2) { return null; }
        @NotModified java.lang.ModuleLayer defineModulesWithOneLoader(@NotNull java.lang.module.Configuration p0, @Nullable java.lang.ClassLoader p1) { return null; }
        static java.lang.ModuleLayer.Controller defineModulesWithOneLoader(@NotNull java.lang.module.Configuration p0, @NotNull java.util.List<java.lang.ModuleLayer> p1, @Nullable java.lang.ClassLoader p2) { return null; }
        @NotModified @Nullable java.lang.ClassLoader findLoader(@NotNull java.lang.String p0) { return null; }
        @NotModified java.util.Optional<java.lang.Module> findModule(@NotNull java.lang.String p0) { return null; }

        //public java.lang.ModuleLayer.Controller (shadow created for its nullability, generated)
        class Controller {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Modified java.lang.ModuleLayer.Controller addExports(@NotNull java.lang.Module p0, @NotNull java.lang.String p1, @NotNull java.lang.Module p2) { return null; }
            @Modified java.lang.ModuleLayer.Controller addOpens(@NotNull java.lang.Module p0, @NotNull java.lang.String p1, @NotNull java.lang.Module p2) { return null; }
            @Modified java.lang.ModuleLayer.Controller addReads(@NotNull java.lang.Module p0, @NotNull java.lang.Module p1) { return null; }
            java.lang.ModuleLayer.Controller enableNativeAccess(@NotNull java.lang.Module p0) { return null; }
        }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.module.Configuration configuration() { return null; }
        @NotModified java.util.Set<java.lang.Module> modules() { return null; }
        @NotModified java.util.List<java.lang.ModuleLayer> parents() { return null; }
    }

    //public java.lang.NegativeArraySizeException (shadow created for its nullability, generated)
    class NegativeArraySizeException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NegativeArraySizeException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NoClassDefFoundError (shadow created for its nullability, generated)
    class NoClassDefFoundError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoClassDefFoundError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NoSuchFieldError (shadow created for its nullability, generated)
    class NoSuchFieldError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoSuchFieldError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NoSuchFieldException (shadow created for its nullability, generated)
    class NoSuchFieldException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoSuchFieldException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NoSuchMethodError (shadow created for its nullability, generated)
    class NoSuchMethodError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoSuchMethodError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NoSuchMethodException (shadow created for its nullability, generated)
    class NoSuchMethodException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoSuchMethodException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.NullPointerException (shadow created for its nullability, generated)
    class NullPointerException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NullPointerException$(@Nullable java.lang.String p0) { }
        @Nullable java.lang.String getMessage() { return null; }
    }

    //public java.lang.NumberFormatException (shadow created for its nullability, generated)
    class NumberFormatException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NumberFormatException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.OutOfMemoryError (shadow created for its nullability, generated)
    class OutOfMemoryError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        OutOfMemoryError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.Package (shadow created for its nullability, generated)
    class Package$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <A extends java.lang.annotation.Annotation> A getAnnotation(@NotNull java.lang.Class<A> p0) { return null; }
        <A extends java.lang.annotation.Annotation> A[] getAnnotationsByType(@NotNull java.lang.Class<A> p0) { return null; }
        @Nullable <A extends java.lang.annotation.Annotation> A getDeclaredAnnotation(@NotNull java.lang.Class<A> p0) { return null; }
        <A extends java.lang.annotation.Annotation> A[] getDeclaredAnnotationsByType(@NotNull java.lang.Class<A> p0) { return null; }
        @NotModified @Nullable java.lang.String getImplementationTitle() { return null; }
        @NotModified @Nullable java.lang.String getImplementationVendor() { return null; }
        @NotModified @Nullable java.lang.String getImplementationVersion() { return null; }
        @Nullable static java.lang.Package getPackage(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.lang.String getSpecificationTitle() { return null; }
        @NotModified @Nullable java.lang.String getSpecificationVendor() { return null; }
        @NotModified @Nullable java.lang.String getSpecificationVersion() { return null; }
        boolean isAnnotationPresent(@NotNull java.lang.Class<? extends java.lang.annotation.Annotation> p0) { return false; }
        @NotModified boolean isCompatibleWith(@NotNull java.lang.String p0) { return false; }
        @NotModified boolean isSealed(@NotNull java.net.URL p0) { return false; }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.String getName() { return null; }
        @NotModified boolean isSealed() { return false; }
    }

    //public java.lang.Process (shadow created for its nullability, generated)
    class Process$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified java.io.BufferedReader errorReader(@NotNull java.nio.charset.Charset p0) { return null; }
        @NotModified java.io.BufferedReader inputReader(@NotNull java.nio.charset.Charset p0) { return null; }
        @NotModified java.io.BufferedWriter outputWriter(@NotNull java.nio.charset.Charset p0) { return null; }
        @NotModified boolean waitFor(@NotNull java.time.Duration p0) { return false; }
        @NotModified boolean waitFor(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return false; }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.util.stream.Stream<java.lang.ProcessHandle> children() { return null; }
        @NotModified java.util.stream.Stream<java.lang.ProcessHandle> descendants() { return null; }
        @NotModified java.io.InputStream getErrorStream() { return null; }
        @NotModified java.io.InputStream getInputStream() { return null; }
        @NotModified java.io.OutputStream getOutputStream() { return null; }
        @NotModified java.lang.ProcessHandle.Info info() { return null; }
        @NotModified boolean isAlive() { return false; }
        @NotModified long pid() { return 0; }
        @NotModified boolean supportsNormalTermination() { return false; }
        @NotModified java.lang.ProcessHandle toHandle() { return null; }
        @NotModified int exitValue() { return 0; }
        @NotModified java.util.concurrent.CompletableFuture<java.lang.Process> onExit() { return null; }
        @NotModified java.io.BufferedReader errorReader() { return null; }
        @NotModified java.io.BufferedReader inputReader() { return null; }
        @NotModified java.io.BufferedWriter outputWriter() { return null; }
        @NotModified int waitFor() { return 0; }
    }

    //public java.lang.ProcessBuilder (shadow created for its nullability, generated)
    class ProcessBuilder$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ProcessBuilder$(@NotModified @NotNull java.util.List<java.lang.String> p0) { }
        ProcessBuilder$(@NotNull @NotModified java.lang.String... p0) { }
        java.lang.ProcessBuilder command(@NotModified @NotNull java.util.List<java.lang.String> p0) { return null; }
        java.lang.ProcessBuilder command(@NotNull @NotModified java.lang.String... p0) { return null; }
        @NotModified @Nullable java.io.File directory() { return null; }
        java.lang.ProcessBuilder directory(@Nullable java.io.File p0) { return null; }
        java.lang.ProcessBuilder redirectError(@NotNull java.io.File p0) { return null; }
        java.lang.ProcessBuilder redirectError(@NotModified @NotNull java.lang.ProcessBuilder.Redirect p0) { return null; }
        java.lang.ProcessBuilder redirectInput(@NotNull java.io.File p0) { return null; }
        java.lang.ProcessBuilder redirectInput(@NotModified @NotNull java.lang.ProcessBuilder.Redirect p0) { return null; }
        java.lang.ProcessBuilder redirectOutput(@NotNull java.io.File p0) { return null; }
        java.lang.ProcessBuilder redirectOutput(@NotModified @NotNull java.lang.ProcessBuilder.Redirect p0) { return null; }
        static java.util.List<java.lang.Process> startPipeline(@NotModified @NotNull java.util.List<java.lang.ProcessBuilder> p0) { return null; }

        //public java.lang.ProcessBuilder.Redirect (shadow created for its nullability, generated)
        class Redirect {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            static java.lang.ProcessBuilder.Redirect appendTo(@NotNull java.io.File p0) { return null; }
            @Nullable java.io.File file() { return null; }
            static java.lang.ProcessBuilder.Redirect from(@NotNull java.io.File p0) { return null; }
            static java.lang.ProcessBuilder.Redirect to(@NotNull java.io.File p0) { return null; }
        }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.Process start() { return null; }
        @NotModified boolean redirectErrorStream() { return false; }
        @NotModified java.util.Map<java.lang.String, java.lang.String> environment() { return null; }
        @NotModified java.util.List<java.lang.String> command() { return null; }
        @NotModified java.lang.ProcessBuilder.Redirect redirectError() { return null; }
        @NotModified java.lang.ProcessBuilder.Redirect redirectInput() { return null; }
        @NotModified java.lang.ProcessBuilder.Redirect redirectOutput() { return null; }
    }

    //public java.lang.ReflectiveOperationException (shadow created for its nullability, generated)
    class ReflectiveOperationException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ReflectiveOperationException$(@Nullable java.lang.String p0) { }
        ReflectiveOperationException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        ReflectiveOperationException$(@NotModified @Nullable java.lang.Throwable p0) { }
    }

    //public java.lang.Runtime (shadow created for its nullability, generated)
    class Runtime$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void addShutdownHook(@NotNull java.lang.Thread p0) { }
        @NotModified java.lang.Process exec(@NotNull java.lang.String p0) { return null; }
        @NotModified java.lang.Process exec(@NotNull java.lang.String p0, @NotModified @Nullable java.lang.String[] p1) { return null; }
        @NotModified java.lang.Process exec(@NotNull java.lang.String p0, @NotModified @Nullable java.lang.String[] p1, @Nullable java.io.File p2) { return null; }
        @NotModified java.lang.Process exec(@NotModified @NotNull java.lang.String[] p0) { return null; }
        @NotModified java.lang.Process exec(@NotModified @NotNull java.lang.String[] p0, @NotModified @Nullable java.lang.String[] p1) { return null; }
        @NotModified java.lang.Process exec(@NotModified @NotNull java.lang.String[] p0, @NotModified @Nullable java.lang.String[] p1, @Nullable java.io.File p2) { return null; }
        @Modified void load(@NotNull java.lang.String p0) { }
        @Modified void loadLibrary(@NotNull java.lang.String p0) { }
        @Modified boolean removeShutdownHook(@NotNull java.lang.Thread p0) { return false; }

        //public java.lang.Runtime.Version (shadow created for its nullability, generated)
        @ImmutableContainer
        class Version {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            int compareTo(@NotNull java.lang.Runtime.Version p0) { return 0; }
            int compareToIgnoreOptional(@NotNull java.lang.Runtime.Version p0) { return 0; }
            boolean equalsIgnoreOptional(@NotNull java.lang.Object p0) { return false; }
            static java.lang.Runtime.Version parse(@NotNull java.lang.String p0) { return null; }
        }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified int availableProcessors() { return 0; }
        @NotModified long freeMemory() { return 0; }
        @NotModified long maxMemory() { return 0; }
        @NotModified long totalMemory() { return 0; }
    }

    //public java.lang.RuntimePermission (shadow created for its nullability, generated)
    class RuntimePermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        RuntimePermission$(@NotNull java.lang.String p0) { }
        RuntimePermission$(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { }
    }

    //public java.lang.SecurityException (shadow created for its nullability, generated)
    class SecurityException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SecurityException$(@Nullable java.lang.String p0) { }
        SecurityException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        SecurityException$(@NotModified @Nullable java.lang.Throwable p0) { }
    }

    //public java.lang.SecurityManager (shadow created for its nullability, generated)
    class SecurityManager$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified void checkAccept(@NotNull java.lang.String p0, int p1) { }
        @NotModified void checkAccess(@NotModified @NotNull java.lang.Thread p0) { }
        @NotModified void checkAccess(@NotModified @NotNull java.lang.ThreadGroup p0) { }
        @NotModified void checkConnect(@NotNull java.lang.String p0, int p1) { }
        @NotModified void checkConnect(@NotNull java.lang.String p0, int p1, @NotNull java.lang.Object p2) { }
        @NotModified void checkDelete(@NotNull java.lang.String p0) { }
        @NotModified void checkExec(@NotNull java.lang.String p0) { }
        @NotModified void checkLink(@NotNull java.lang.String p0) { }
        @NotModified void checkMulticast(@NotNull java.net.InetAddress p0) { }
        @NotModified void checkMulticast(@NotNull java.net.InetAddress p0, byte p1) { }
        @NotModified void checkPackageAccess(@NotNull java.lang.String p0) { }
        @NotModified void checkPackageDefinition(@NotNull java.lang.String p0) { }
        @NotModified void checkPermission(@NotModified @NotNull java.security.Permission p0) { }
        @NotModified void checkPermission(@NotModified @NotNull java.security.Permission p0, @NotNull java.lang.Object p1) { }
        @NotModified void checkPropertyAccess(@NotNull java.lang.String p0) { }
        @NotModified void checkRead(@NotModified @NotNull java.io.FileDescriptor p0) { }
        @NotModified void checkRead(@NotNull java.lang.String p0) { }
        @NotModified void checkRead(@NotNull java.lang.String p0, @NotNull java.lang.Object p1) { }
        @NotModified void checkSecurityAccess(@NotNull java.lang.String p0) { }
        @NotModified void checkWrite(@NotModified @NotNull java.io.FileDescriptor p0) { }
        @NotModified void checkWrite(@NotNull java.lang.String p0) { }
    }

    //public java.lang.StackOverflowError (shadow created for its nullability, generated)
    class StackOverflowError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StackOverflowError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.StackWalker (shadow created for its nullability, generated)
    class StackWalker$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified void forEach(@NotNull java.util.function.Consumer<? super java.lang.StackWalker.StackFrame> p0) { }
        static java.lang.StackWalker getInstance(@NotNull java.lang.StackWalker.Option p0) { return null; }
        static java.lang.StackWalker getInstance(@NotNull java.util.Set<java.lang.StackWalker.Option> p0) { return null; }
        static java.lang.StackWalker getInstance(@NotNull java.util.Set<java.lang.StackWalker.Option> p0, int p1) { return null; }
        @NotModified <T> T walk(@NotNull java.util.function.Function<? super java.util.stream.Stream<java.lang.StackWalker.StackFrame>, ? extends T> p0) { return null; }

        //public java.lang.StackWalker.StackFrame (shadow created for its nullability, generated)
        class StackFrame {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.lang.String getFileName() { return null; }
        }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.Class<?> getCallerClass() { return null; }
    }

    //public java.lang.StringIndexOutOfBoundsException (shadow created for its nullability, generated)
    class StringIndexOutOfBoundsException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringIndexOutOfBoundsException$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.ThreadGroup (shadow created for its nullability, generated)
    class ThreadGroup$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ThreadGroup$(@Nullable java.lang.String p0) { }
        ThreadGroup$(@NotNull java.lang.ThreadGroup p0, @Nullable java.lang.String p1) { }
        @NotModified int enumerate(@NotNull(nullableArguments = 0) java.lang.ThreadGroup[] p0) { return 0; }
        @NotModified int enumerate(@NotNull(nullableArguments = 0) java.lang.ThreadGroup[] p0, boolean p1) { return 0; }
        @NotModified int enumerate(@NotNull(nullableArguments = 0) java.lang.Thread[] p0) { return 0; }
        @NotModified int enumerate(@NotNull(nullableArguments = 0) java.lang.Thread[] p0, boolean p1) { return 0; }
        @NotModified @Nullable java.lang.String getName() { return null; }
        @NotModified @Nullable java.lang.ThreadGroup getParent() { return null; }
        @NotModified boolean parentOf(@NotModified @NotNull java.lang.ThreadGroup p0) { return false; }
        @NotModified void uncaughtException(@NotModified @NotNull java.lang.Thread p0, @NotModified @NotNull java.lang.Throwable p1) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified int activeCount() { return 0; }
        @NotModified int activeGroupCount() { return 0; }
        @NotModified void checkAccess() { }
        @NotModified int getMaxPriority() { return 0; }
        @NotModified boolean isDaemon() { return false; }
        @NotModified boolean isDestroyed() { return false; }
        @NotModified void list() { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void interrupt() { }
        @Modified void setDaemon(boolean p0) { }
        @Modified void setMaxPriority(int p0) { }
    }

    //public java.lang.TypeNotPresentException (shadow created for its nullability, generated)
    class TypeNotPresentException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        TypeNotPresentException$(@NotNull java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }

        // a query of this object, not a change of it (java.lang audit, 2026-10-29: the unannotated default is modifying)
        @NotModified java.lang.String typeName() { return null; }
    }

    //public java.lang.UnknownError (shadow created for its nullability, generated)
    class UnknownError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnknownError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.UnsatisfiedLinkError (shadow created for its nullability, generated)
    class UnsatisfiedLinkError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsatisfiedLinkError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.UnsupportedClassVersionError (shadow created for its nullability, generated)
    class UnsupportedClassVersionError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsupportedClassVersionError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.UnsupportedOperationException (shadow created for its nullability, generated)
    class UnsupportedOperationException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsupportedOperationException$(@Nullable java.lang.String p0) { }
        UnsupportedOperationException$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        UnsupportedOperationException$(@NotModified @Nullable java.lang.Throwable p0) { }
    }

    //public java.lang.VerifyError (shadow created for its nullability, generated)
    class VerifyError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        VerifyError$(@Nullable java.lang.String p0) { }
    }

    //public java.lang.VirtualMachineError (shadow created for its nullability, generated)
    class VirtualMachineError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        VirtualMachineError$(@Nullable java.lang.String p0) { }
        VirtualMachineError$(@Nullable java.lang.String p0, @NotModified @Nullable java.lang.Throwable p1) { }
        VirtualMachineError$(@NotModified @Nullable java.lang.Throwable p0) { }
    }
}
