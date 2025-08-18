package org.e2immu.analyzer.aapi.archive.v2.jdk;
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
import org.e2immu.annotation.Container;
import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;
import org.e2immu.annotation.rare.IgnoreModifications;
import org.e2immu.annotation.type.UtilityClass;
public class JavaLang {
    public static final String PACKAGE_NAME = "java.lang";
    //public interface Appendable
    @Container
    @Independent
    class Appendable$ {
        //@Independent[O]
        Appendable append(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ CharSequence charSequence) {
            return null;
        }

        //@Independent[O]
        Appendable append(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ CharSequence charSequence,
            int i,
            int i1) { return null; }

        //@Independent[O]
        Appendable append(char c) { return null; }
    }

    //public class AssertionError extends Error
    class AssertionError$ {
        AssertionError$() { }
        AssertionError$(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object detailMessage) { }
        AssertionError$(boolean detailMessage) { }
        AssertionError$(char detailMessage) { }
        AssertionError$(int detailMessage) { }
        AssertionError$(long detailMessage) { }
        AssertionError$(float detailMessage) { }
        AssertionError$(double detailMessage) { }
        AssertionError$(String message, Throwable cause) { }
    }

    //public interface AutoCloseable
    //@Container[M]
    @Independent class AutoCloseable$ {void close() { } }

    //public final class Boolean implements Serializable, Comparable<Boolean>, Constable
    @ImmutableContainer
    class Boolean$ {
        //@Immutable[T] @Independent[T] @NotModified[T]
        static final Boolean TRUE = null;

        //@Immutable[T] @Independent[T] @NotModified[T]
        static final Boolean FALSE = null;
        static final Class<Boolean> TYPE = null;
        Boolean$(boolean value) { }
        Boolean$(String s) { }
        //frequency 1
        //@NotModified[T]
        static boolean parseBoolean(String s) { return false; }

        //@NotModified[T]
        boolean booleanValue() { return false; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Boolean valueOf(boolean b) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Boolean valueOf(String s) { return null; }

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
        static boolean getBoolean(String name) { return false; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ Boolean b) { return 0; }

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

    //public interface CharSequence
    @ImmutableContainer(hc = true)
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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence cs1,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence cs2) { return 0; }
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
        static Class<?> forName(String className) { return null; }

        //@NotModified[T]
        static Class<?> forName(String name, boolean initialize, /*@Independent[T] @NotModified[O]*/ ClassLoader loader) {
            return null;
        }

        //@NotModified[T]
        static Class<?> forName(/*@Independent[T] @NotModified[O]*/ Module module, String name) { return null; }

        //@NotModified[T]
        static Class<?> forPrimitiveName(String primitiveName) { return null; }

        //@Independent[O] @NotModified[T]
        T newInstance() { return null; }

        //@NotModified[T]
        boolean isInstance(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //@NotModified[T]
        boolean isAssignableFrom(Class<?> class0) { return false; }

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
        ClassLoader getClassLoader() { return null; }

        //@Independent[O] @NotModified[T]
        Module getModule() { return null; }

        //override from java.lang.reflect.GenericDeclaration
        //@Independent[O] @NotModified[T]
        TypeVariable<Class<T>> [] getTypeParameters() { return null; }

        //@NotModified[T]
        Class<? super T> getSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        Type getGenericSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        Package getPackage() { return null; }

        //@NotModified[T]
        String getPackageName() { return null; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getInterfaces() { return null; }

        //@Independent[O] @NotModified[T]
        Type [] getGenericInterfaces() { return null; }

        //@NotModified[T]
        Class<?> getComponentType() { return null; }

        //@NotModified[T]
        int getModifiers() { return 0; }

        //@Independent[O] @NotModified[T]
        Set<AccessFlag> accessFlags() { return null; }

        //@Independent[O] @NotModified[T]
        Object [] getSigners() { return null; }

        //@Independent[O] @NotModified[T]
        Method getEnclosingMethod() { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<?> getEnclosingConstructor() { return null; }

        //@NotModified[T]
        Class<?> getDeclaringClass() { return null; }

        //@NotModified[T]
        Class<?> getEnclosingClass() { return null; }

        //@NotModified[T]
        String getSimpleName() { return null; }

        //override from java.lang.reflect.Type
        //@NotModified[T]
        String getTypeName() { return null; }

        //@NotModified[T]
        String getCanonicalName() { return null; }

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
        Field getField(String name) { return null; }

        //@Independent[O] @NotModified[T]
        Method getMethod(String name, /*@Independent[M] @NotModified[O]*/ Class<?> ... parameterTypes) { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<T> getConstructor(/*@Independent[M] @NotModified[O]*/ Class<?> ... parameterTypes) { return null; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getDeclaredClasses() { return null; }

        //@Independent[O] @NotModified[T]
        Field [] getDeclaredFields() { return null; }

        //@Independent[O] @NotModified[T]
        RecordComponent [] getRecordComponents() { return null; }

        //@Independent[O] @NotModified[T]
        Method [] getDeclaredMethods() { return null; }

        //@Independent[O] @NotModified[T]
        Constructor<?> [] getDeclaredConstructors() { return null; }

        //@Independent[O] @NotModified[T]
        Field getDeclaredField(String name) { return null; }

        //@Independent[O] @NotModified[T]
        Method getDeclaredMethod(String name, /*@Independent[M] @NotModified[O]*/ Class<?> ... parameterTypes) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        Constructor<T> getDeclaredConstructor(/*@Independent[M] @NotModified[O]*/ Class<?> ... parameterTypes) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        InputStream getResourceAsStream(String name) { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        URL getResource(String name) { return null; }

        //@Independent[O] @NotModified[T]
        ProtectionDomain getProtectionDomain() { return null; }

        //@NotModified[T]
        boolean desiredAssertionStatus() { return false; }

        //@NotModified[T]
        boolean isEnum() { return false; }

        //@NotModified[T]
        boolean isRecord() { return false; }

        //@Independent[O] @NotModified[T]
        T [] getEnumConstants() { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        T cast(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return null; }

        //@NotModified[T]
        <U> Class<? extends U> asSubclass(Class<U> clazz) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A getAnnotation(Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@NotModified[T]
        boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) { return false; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A [] getAnnotationsByType(Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        Annotation [] getAnnotations() { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        <A extends Annotation> A [] getDeclaredAnnotationsByType(Class<A> annotationClass) { return null; }

        //override from java.lang.reflect.AnnotatedElement
        //@Independent[O] @NotModified[T]
        Annotation [] getDeclaredAnnotations() { return null; }

        //@Independent[O] @NotModified[T]
        AnnotatedType getAnnotatedSuperclass() { return null; }

        //@Independent[O] @NotModified[T]
        AnnotatedType [] getAnnotatedInterfaces() { return null; }

        //@NotModified[T]
        Class<?> getNestHost() { return null; }

        //@NotModified[T]
        boolean isNestmateOf(Class<?> c) { return false; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getNestMembers() { return null; }

        //override from java.lang.invoke.TypeDescriptor
        //@NotModified[T]
        String descriptorString() { return null; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        Class<?> componentType() { return null; }

        //override from java.lang.invoke.TypeDescriptor.OfField
        //@NotModified[T]
        Class<?> arrayType() { return null; }

        //override from java.lang.constant.Constable
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        Optional<ClassDesc> describeConstable() { return null; }

        //@NotModified[T]
        boolean isHidden() { return false; }

        //@Independent[O] @NotModified[T]
        Class<?> [] getPermittedSubclasses() { return null; }

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
        Double$(String s) { }
        //@NotModified[T]
        static String toString(double d) { return null; }

        //@NotModified[T]
        static String toHexString(double d) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Double valueOf(String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Double valueOf(double d) { return null; }

        //@NotModified[T]
        static double parseDouble(String s) { return 0.0; }

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
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ Double anotherDouble) { return 0; }

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
        Double resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ MethodHandles.Lookup lookup) { return null; }
    }

    //public abstract class Enum implements Constable, Comparable<E>, Serializable
    @ImmutableContainer(hc = true)
    class Enum$<E extends Enum<E>> {
        //public static final class EnumDesc extends DynamicConstantDesc<E>
        class EnumDesc<E extends Enum<E>> {
            static <E extends Enum<E>> Enum.EnumDesc<E> of(ClassDesc enumClass, String constantName) { return null; }
            //override from java.lang.constant.ConstantDesc, java.lang.constant.DynamicConstantDesc
            //@Immutable(hc=true)[T] @Independent(hc=true)[H]
            E resolveConstantDesc(MethodHandles.Lookup lookup) { return null; }

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
        static <T extends Enum<T>> T valueOf(Class<T> enumClass, String name) { return null; }
    }

    //public class Error extends Throwable
    class Error$ {
        Error$() { }
        //override has frequency 7
        Error$(String message) { }
        Error$(String message, Throwable cause) { }
        Error$(Throwable cause) { }
    }

    //public class Exception extends Throwable
    class Exception$ {
        //override has frequency 1
        Exception$() { }

        //override has frequency 8
        Exception$(String message) { }

        //override has frequency 1
        Exception$(String message, Throwable cause) { }

        //override has frequency 4
        Exception$(Throwable cause) { }
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
        Float$(String s) { }
        //@NotModified[T]
        static String toString(float f) { return null; }

        //@NotModified[T]
        static String toHexString(float f) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Float valueOf(String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Float valueOf(float f) { return null; }

        //frequency 2
        //@NotModified[T]
        static float parseFloat(String s) { return 0.0F; }

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
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ Float anotherFloat) { return 0; }

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
        Float resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ MethodHandles.Lookup lookup) { return null; }
    }

    //public class IllegalArgumentException extends RuntimeException
    class IllegalArgumentException$ {
        IllegalArgumentException$() { }
        //frequency 12
        IllegalArgumentException$(String s) { }

        //frequency 2
        IllegalArgumentException$(String message, Throwable cause) { }
        IllegalArgumentException$(Throwable cause) { }
    }

    //public class IllegalStateException extends RuntimeException
    class IllegalStateException$ {
        IllegalStateException$() { }
        //frequency 9
        IllegalStateException$(String s) { }

        //frequency 1
        IllegalStateException$(String message, Throwable cause) { }

        //frequency 4
        IllegalStateException$(Throwable cause) { }
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
        Integer$(String s) { }
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
        static int parseInt(String s, int radix) { return 0; }

        //@NotModified[T]
        static int parseInt(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0; }

        //frequency 13
        //@NotModified[T]
        static int parseInt(String s) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(String s, int radix) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0; }

        //@NotModified[T]
        static int parseUnsignedInt(String s) { return 0; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer valueOf(String s, int radix) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer valueOf(String s) { return null; }

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
        static Integer getInteger(String nm) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer getInteger(String nm, int val) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer getInteger(String nm, /*@Immutable[T] @Independent[T] @NotModified[O]*/ Integer val) {
            return null;
        }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Integer decode(String nm) { return null; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ Integer anotherInteger) { return 0; }

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
        Integer resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ MethodHandles.Lookup lookup) { return null; }
    }

    //public interface Iterable
    @Container
    class Iterable$<T> {
        //override has frequency 2
        @NotModified @NotNull
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
        Long$(String s) { }
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
        static long parseLong(String s, int radix) { return 0L; }

        //@NotModified[T]
        static long parseLong(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0L; }

        //frequency 7
        //@NotModified[T]
        static long parseLong(String s) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(String s, int radix) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ CharSequence s,
            int beginIndex,
            int endIndex,
            int radix) { return 0L; }

        //@NotModified[T]
        static long parseUnsignedLong(String s) { return 0L; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(String s, int radix) { return null; }

        //frequency 1
        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(String s) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long valueOf(long l) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long decode(String nm) { return null; }

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
        static Long getLong(String nm) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long getLong(String nm, long val) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static Long getLong(String nm, /*@Immutable[T] @Independent[T] @NotModified[O]*/ Long val) { return null; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ Long anotherLong) { return 0; }

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
        Long resolveConstantDesc(/*@Independent[M] @NotModified[O]*/ MethodHandles.Lookup lookup) { return null; }
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
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

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
            Optional<Instant> startInstant() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<Duration> totalCpuDuration() { return null; }

            //@Immutable(hc=true)[T] @Independent(hc=true)[T]
            Optional<String> user() { return null; }
        }
        long pid() { return 0L; }
        //@Immutable(hc=true)[T] @Independent[T]
        static Optional<ProcessHandle> of(long pid) { return null; }
        static ProcessHandle current() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Optional<ProcessHandle> parent() { return null; }
        Stream<ProcessHandle> children() { return null; }
        Stream<ProcessHandle> descendants() { return null; }
        //@Independent[T]
        static Stream<ProcessHandle> allProcesses() { return null; }
        ProcessHandle.Info info() { return null; }
        CompletableFuture<ProcessHandle> onExit() { return null; }
        boolean supportsNormalTermination() { return false; }
        boolean destroy() { return false; }
        boolean destroyForcibly() { return false; }
        boolean isAlive() { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ProcessHandle processHandle) { return 0; }
    }

    //public interface Readable
    @Independent
    class Readable$ {int read(@NotNull CharBuffer charBuffer) { return 0; } }

    //public interface Runnable
    //@Container[M]
    @Independent(hc = true) class Runnable$ {void run() { } }

    //public class RuntimeException extends Exception
    class RuntimeException$ {
        //frequency 1
        RuntimeException$() { }

        //frequency 8
        RuntimeException$(String message) { }

        //override has frequency 1
        RuntimeException$(String message, Throwable cause) { }

        //frequency 3
        RuntimeException$(Throwable cause) { }
    }

    //public final class String implements Serializable, Comparable<String>, CharSequence, Constable, ConstantDesc
    @Immutable
    class String$ {
        //@Immutable(hc=true)[T] @NotModified[T]
        static final Comparator<String> CASE_INSENSITIVE_ORDER = null;
        String$() { }
        String$(String original) { }
        String$(/*@Independent[T]*/ @NotModified char [] value) { }
        String$(/*@Independent[T]*/ @NotModified char [] value, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotModified int [] codePoints, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotModified byte [] ascii, int hibyte, int offset, int count) { }
        String$(/*@Independent[T]*/ @NotModified byte [] ascii, int hibyte) { }
        String$(/*@Independent[T]*/ @NotModified byte [] bytes, int offset, int length, String charsetName) { }
        String$(
            /*@Independent[T]*/ @NotModified byte [] bytes,
            int offset,
            int length,
            /*@Independent[T]*/ Charset charset) { }
        String$(/*@Independent[T]*/ @NotModified byte [] bytes, String charsetName) { }
        String$(/*@Independent[T]*/ @NotModified byte [] bytes, /*@Independent[T]*/ Charset charset) { }
        String$(/*@Independent[T]*/ @NotModified byte [] bytes, int offset, int length) { }
        String$(/*@Independent[T]*/ @NotModified byte [] bytes) { }
        String$(/*@Independent[T]*/ @NotModified StringBuffer buffer) { }
        String$(/*@Independent[T]*/ @NotModified StringBuilder builder) { }
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
        void getChars(int srcBegin, int srcEnd, /*@Independent[M]*/ char [] dst, int dstBegin) { }

        //@NotModified[T]
        void getBytes(int srcBegin, int srcEnd, /*@Independent[M]*/ byte [] dst, int dstBegin) { }

        //@Independent[O] @NotModified[T]
        byte [] getBytes(String charsetName) { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        byte [] getBytes(/*@Independent[M]*/ Charset charset) { return null; }

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
        boolean contentEquals(/*@Independent[M]*/ StringBuffer sb) { return false; }

        //@NotModified[T]
        boolean contentEquals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ CharSequence cs) {
            return false;
        }

        //frequency 13
        //@NotModified[T]
        boolean equalsIgnoreCase(String anotherString) { return false; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@NotNull[H]*/ String anotherString) { return 0; }

        //@NotModified[T]
        int compareToIgnoreCase(String str) { return 0; }

        //@NotModified[T]
        boolean regionMatches(int toffset, String other, int ooffset, int len) { return false; }

        //@NotModified[T]
        boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) { return false; }

        //@NotModified[T]
        boolean startsWith(String prefix, int toffset) { return false; }

        //frequency 4
        //@NotModified[T]
        boolean startsWith(String prefix) { return false; }

        //frequency 2
        //@NotModified[T]
        boolean endsWith(String suffix) { return false; }

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
        int indexOf(String str) { return 0; }

        //@NotModified[T]
        int indexOf(String str, int fromIndex) { return 0; }

        //@NotModified[T]
        int indexOf(String str, int beginIndex, int endIndex) { return 0; }

        //@NotModified[T]
        int lastIndexOf(String str) { return 0; }

        //@NotModified[T]
        int lastIndexOf(String str, int fromIndex) { return 0; }

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
        String concat(String str) { return null; }

        //frequency 2
        //@NotModified[T]
        String replace(char oldChar, char newChar) { return null; }

        //frequency 1
        //@NotModified[T]
        boolean matches(String regex) { return false; }

        //frequency 6
        //@NotModified[T]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ CharSequence s) { return false; }

        //@NotModified[T]
        String replaceFirst(String regex, String replacement) { return null; }

        //frequency 7
        //@NotModified[T]
        String replaceAll(String regex, String replacement) { return null; }

        //frequency 8
        //@NotModified[T]

        String replace(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ CharSequence target,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ CharSequence replacement) { return null; }

        //@Independent[O] @NotModified[T]
        String [] split(String regex, int limit) { return null; }

        //@Independent[O] @NotModified[T]
        String [] splitWithDelimiters(String regex, int limit) { return null; }

        //frequency 9
        //@Independent[O] @NotModified[T]
        String [] split(String regex) { return null; }

        //@NotModified[T]
        static String join(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ CharSequence delimiter,
            /*@Independent[T]*/ CharSequence ... elements) { return null; }

        //@NotModified[T]
        static String join(
            /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ CharSequence delimiter,
            /*@Independent[T]*/ Iterable<? extends CharSequence> elements) { return null; }

        //frequency 3
        //@NotModified[T]
        String toLowerCase(/*@Independent[M]*/ Locale locale) { return null; }

        //frequency 36
        //@NotModified[T]
        String toLowerCase() { return null; }

        //@NotModified[T]
        String toUpperCase(/*@Independent[M]*/ Locale locale) { return null; }

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
        <R> R transform(/*@IgnoreModifications[T] @Independent[M]*/ Function<? super String, ? extends R> f) {
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
        static String format(String format, /*@Independent[T]*/ Object ... args) { return null; }

        //@NotModified[T]
        static String format(/*@Independent[T]*/ Locale l, String format, /*@Independent[T]*/ Object ... args) {
            return null;
        }

        //frequency 37
        //@NotModified[T]
        String formatted(/*@Independent[M]*/ Object ... args) { return null; }

        //frequency 1
        //@NotModified[T]
        static String valueOf(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ Object obj) { return null; }

        //@NotModified[T]
        static String valueOf(/*@Independent[T]*/ char [] data) { return null; }

        //@NotModified[T]
        static String valueOf(/*@Independent[T]*/ char [] data, int offset, int count) { return null; }

        //@NotModified[T]
        static String copyValueOf(/*@Independent[T]*/ char [] data, int offset, int count) { return null; }

        //@NotModified[T]
        static String copyValueOf(/*@Independent[T]*/ char [] data) { return null; }

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
        String resolveConstantDesc(/*@Independent[M]*/ MethodHandles.Lookup lookup) { return null; }
    }

    //public final class StringBuilder extends AbstractStringBuilder implements Appendable, Serializable, Comparable<StringBuilder>, CharSequence
    @Container
    @Independent
    class StringBuilder$ {
        //frequency 3
        StringBuilder$() { }
        StringBuilder$(int capacity) { }
        StringBuilder$(String str) { }
        StringBuilder$(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[O]*/ CharSequence seq) { }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[O] @NotNull[H]*/ StringBuilder another) { return 0; }

        //override from java.lang.AbstractStringBuilder
        //frequency 3
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ Object obj) { return null; }

        //override from java.lang.AbstractStringBuilder
        //frequency 6
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O]*/ StringBuffer sb) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ CharSequence s) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ CharSequence s,
            int start,
            int end) { return null; }

        //override from java.lang.AbstractStringBuilder, java.lang.Appendable
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O] @NotNull[H]*/ char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder append(/*@Independent[H] @NotModified[O]*/ char [] str, int offset, int len) { return null; }

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
        StringBuilder replace(int start, int end, String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int index, /*@Independent[H] @NotModified[O]*/ char [] str, int offset, int len) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int offset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ Object obj) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, String str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent
        StringBuilder insert(int offset, /*@Independent[H] @NotModified[O]*/ char [] str) { return null; }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(int dstOffset, /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ CharSequence s) {
            return null;
        }

        //override from java.lang.AbstractStringBuilder
        //@Independent[O] @NotNull[M]
        @Fluent

        StringBuilder insert(
            int dstOffset,
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ CharSequence s,
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
        int indexOf(String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        int indexOf(String str, int fromIndex) { return 0; }

        //override from java.lang.AbstractStringBuilder
        int lastIndexOf(String str) { return 0; }

        //override from java.lang.AbstractStringBuilder
        int lastIndexOf(String str, int fromIndex) { return 0; }

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

        StringBuilder repeat(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[O]*/ CharSequence cs, int count) {
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
        @Independent
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
            @NotModified boolean isLoggable(/*@Independent[M]*/ System.Logger.Level level) { return false; }
            @IgnoreModifications void log(/*@Independent[T]*/ System.Logger.Level level, String msg) { }
            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                /*@IgnoreModifications[T] @Independent[T]*/ Supplier<String> msgSupplier) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                /*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ Object obj) { }

            @IgnoreModifications
            void log(/*@Independent[T]*/ System.Logger.Level level, String msg, /*@Independent[T]*/ Throwable thrown) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                /*@IgnoreModifications[T] @Independent[T]*/ Supplier<String> msgSupplier,
                /*@Independent[T]*/ Throwable thrown) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                String format,
                /*@Independent[T]*/ Object ... params) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                /*@Independent[T]*/ ResourceBundle resourceBundle,
                String string,
                /*@Independent[T]*/ Throwable throwable) { }

            @IgnoreModifications
            void log(
                /*@Independent[T]*/ System.Logger.Level level,
                /*@Independent[T]*/ ResourceBundle resourceBundle,
                String string,
                /*@Independent[T]*/ Object ... object) { }
        }

        //public abstract static class LoggerFinder
        class LoggerFinder {
            System.Logger getLogger(String string, Module module) { return null; }
            System.Logger getLocalizedLogger(String name, ResourceBundle bundle, Module module) { return null; }
            static System.LoggerFinder getLoggerFinder() { return null; }
        }
        static void setIn(/*@Independent[T]*/ InputStream in) { }
        static void setOut(/*@Independent[T]*/ PrintStream out) { }
        static void setErr(/*@Independent[T]*/ PrintStream err) { }
        //@Independent[O]
        static Console console() { return null; }

        //@Independent[O]
        static Channel inheritedChannel() { return null; }
        static void setSecurityManager(/*@Independent[T]*/ SecurityManager sm) { }
        //@Independent[O]
        static SecurityManager getSecurityManager() { return null; }

        //frequency 4
        @IgnoreModifications
        static long currentTimeMillis() { return 0L; }
        static long nanoTime() { return 0L; }
        static void arraycopy(
            /*@Immutable(hc=true)[T] @Independent[T]*/ @NotModified @NotNull Object object,
            int i,
            /*@Immutable(hc=true)[T] @NotModified[T]*/ @Independent(hcParameters = { 0 }) @NotNull Object object1,
            int i1,
            int i2) { }

        static int identityHashCode(/*@Immutable(hc=true)[T] @Independent[T] @NotModified[T]*/ Object object) {
            return 0;
        }

        //@Independent[O]
        static Properties getProperties() { return null; }
        static String lineSeparator() { return null; }
        static void setProperties(/*@Independent[T]*/ Properties props) { }
        //frequency 2
        @NotModified
        static String getProperty(String key) { return null; }
        static String getProperty(String key, String def) { return null; }
        static String setProperty(String key, String value) { return null; }
        static String clearProperty(String key) { return null; }
        //frequency 1
        @NotModified
        static String getenv(String name) { return null; }

        //@Independent[O]
        static Map<String, String> getenv() { return null; }

        //@Independent[O]
        static System.Logger getLogger(String name) { return null; }

        //@Independent[O]
        static System.Logger getLogger(String name, /*@Independent[T]*/ ResourceBundle bundle) { return null; }

        //frequency 1
        @IgnoreModifications
        static void exit(int status) { }
        static void gc() { }
        static void runFinalization() { }
        static void load(String filename) { }
        static void loadLibrary(String libname) { }
        static String mapLibraryName(String string) { return null; }
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
            Thread.Builder name(String string) { return null; }
            Thread.Builder name(String string, long l) { return null; }
            Thread.Builder inheritInheritableThreadLocals(boolean b) { return null; }
            Thread.Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
                return null;
            }
            Thread unstarted(Runnable runnable) { return null; }
            Thread start(Runnable runnable) { return null; }
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
        class UncaughtExceptionHandler {void uncaughtException(Thread thread, Throwable throwable) { } }
        Thread$() { }
        Thread$(Runnable task) { }
        Thread$(ThreadGroup group, Runnable task) { }
        Thread$(String name) { }
        Thread$(ThreadGroup group, String name) { }
        Thread$(Runnable task, String name) { }
        Thread$(ThreadGroup group, Runnable task, String name) { }
        Thread$(ThreadGroup group, Runnable task, String name, long stackSize) { }
        Thread$(ThreadGroup group, Runnable task, String name, long stackSize, boolean b) { }
        //frequency 3
        @NotModified
        static Thread currentThread() { return null; }
        static void yield() { }
        //frequency 2
        @IgnoreModifications
        static void sleep(long millis) { }
        static void sleep(long millis, int i) { }
        static void sleep(Duration duration) { }
        static void onSpinWait() { }
        static Thread.Builder.OfPlatform ofPlatform() { return null; }
        static Thread.Builder.OfVirtual ofVirtual() { return null; }
        static Thread startVirtualThread(Runnable task) { return null; }
        boolean isVirtual() { return false; }
        void start() { }
        //override from java.lang.Runnable
        void run() { }
        void stop() { }
        //frequency 3
        @IgnoreModifications
        void interrupt() { }
        static boolean interrupted() { return false; }
        boolean isInterrupted() { return false; }
        boolean isAlive() { return false; }
        void setPriority(int newPriority) { }
        int getPriority() { return 0; }
        void setName(String name) { }
        String getName() { return null; }
        ThreadGroup getThreadGroup() { return null; }
        static int activeCount() { return 0; }
        static int enumerate(Thread [] tarray) { return 0; }
        void join(long millis) { }
        void join(long millis, int i) { }
        void join() { }
        boolean join(Duration duration) { return false; }
        static void dumpStack() { }
        void setDaemon(boolean on) { }
        boolean isDaemon() { return false; }
        void checkAccess() { }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        ClassLoader getContextClassLoader() { return null; }
        void setContextClassLoader(ClassLoader cl) { }
        static boolean holdsLock(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) {
            return false;
        }
        StackTraceElement [] getStackTrace() { return null; }
        //@Independent[T]
        static Map<Thread, StackTraceElement []> getAllStackTraces() { return null; }
        long getId() { return 0L; }
        long threadId() { return 0L; }
        Thread.State getState() { return null; }
        static void setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh) { }
        static Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() { return null; }
        Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() { return null; }
        void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh) { }
    }

    //public class Throwable implements Serializable
    class Throwable$ {
        //override has frequency 1
        Throwable$() { }

        //override has frequency 8
        Throwable$(String message) { }

        //override has frequency 1
        Throwable$(String message, Throwable cause) { }

        //override has frequency 4
        Throwable$(Throwable cause) { }

        //frequency 17
        @NotModified
        String getMessage() { return null; }
        String getLocalizedMessage() { return null; }
        //frequency 7
        @NotModified
        Throwable getCause() { return null; }
        Throwable initCause(Throwable cause) { return null; }
        public String toString() { return null; }
        @IgnoreModifications void printStackTrace() { }
        @NotModified void printStackTrace(/*@Independent[M]*/ PrintStream s) { }
        void printStackTrace(PrintWriter s) { }
        Throwable fillInStackTrace() { return null; }
        StackTraceElement [] getStackTrace() { return null; }
        void setStackTrace(StackTraceElement [] stackTrace) { }
        void addSuppressed(Throwable exception) { }
        Throwable [] getSuppressed() { return null; }
    }
}
