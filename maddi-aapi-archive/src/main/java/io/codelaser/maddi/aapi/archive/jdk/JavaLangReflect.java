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

import io.codelaser.maddi.annotation.Nullable;
import java.lang.reflect.Method;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.type.UtilityClass;
import io.codelaser.maddi.annotation.NotNull;

public class JavaLangReflect {
    public static final String PACKAGE_NAME = "java.lang.reflect";


    @ImmutableContainer
    interface GenericDeclaration$ {
    }

    @ImmutableContainer
    interface AnnotatedElement$ {
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable default <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        default <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        @Nullable default <T extends java.lang.annotation.Annotation> T getDeclaredAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        default <T extends java.lang.annotation.Annotation> T[] getDeclaredAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        default boolean isAnnotationPresent(@NotNull java.lang.Class<? extends java.lang.annotation.Annotation> p0) { return false; }
    }

    @ImmutableContainer
    interface AnnotatedType$ {
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable default java.lang.reflect.AnnotatedType getAnnotatedOwnerType() { return null; }
    }

    @ImmutableContainer
    @Independent
    interface Type$ {
    }

    @Independent(hc = true)
    interface AccessibleObject$ {

    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        default boolean canAccess(@Nullable java.lang.Object p0) { return false; }
        @Nullable default <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        default <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        @Nullable default <T extends java.lang.annotation.Annotation> T getDeclaredAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        default <T extends java.lang.annotation.Annotation> T[] getDeclaredAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        default boolean isAnnotationPresent(@NotNull java.lang.Class<? extends java.lang.annotation.Annotation> p0) { return false; }
        static void setAccessible(@NotNull java.lang.reflect.AccessibleObject[] p0, boolean p1) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified default void setAccessible(boolean p0) { }
    }

    //public final class Array
    @UtilityClass
    class Array$ {
        //@Immutable(hc=true)[T] @Independent[T]
        static Object newInstance(@NotNull Class<?> componentType, int length) { return null; }

        //@Immutable(hc=true)[T] @Independent[T]
        static Object newInstance(@NotNull Class<?> componentType, @NotNull int ... dimensions) { return null; }

        static int getLength(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object) {
            return 0;
        }

        //@Immutable(hc=true)[T] @Independent[T]
        @Nullable static Object get(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return null;
        }

        static boolean getBoolean(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object,
            int i) { return false; }

        static byte getByte(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return 0;
        }

        static char getChar(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return '\0';
        }

        static short getShort(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return 0;
        }

        static int getInt(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return 0;
        }

        static long getLong(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return 0L;
        }

        static float getFloat(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object, int i) {
            return 0.0F;
        }

        static double getDouble(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object object,
            int i) { return 0.0; }

        static void set(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Nullable Object object1) { }

        static void setBoolean(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            boolean b) { }

        static void setByte(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            byte b) { }

        static void setChar(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            char c) { }

        static void setShort(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            short s) { }

        static void setInt(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            int i1) { }

        static void setLong(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            long l) { }

        static void setFloat(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull @Modified Object object,
            int i,
            float f) { }

        static void setDouble(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/@NotNull @Modified Object object,
            int i,
            double d) { }
    }

    // null contracts (docs/design/nullability.md M2), formerly the analyzer's LibraryNullness
    class InvocationHandler$ {
        @Nullable Object invoke(@NotNull Object proxy, @NotNull Method method, @Nullable(nullableArguments = 0) Object[] args) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static java.lang.Object invokeDefault(@NotNull java.lang.Object p0, @NotNull java.lang.reflect.Method p1, @Nullable(nullableArguments = 0) java.lang.Object... p2) { return null; }
    }

    //public java.lang.reflect.ClassFileFormatVersion (shadow created for its nullability, generated)
    class ClassFileFormatVersion$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Runtime.Version runtimeVersion() { return null; }
    }

    //public java.lang.reflect.Constructor (shadow created for its nullability, generated)
    class Constructor$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.reflect.AnnotatedType getAnnotatedReceiverType() { return null; }
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        T newInstance(@Nullable(nullableArguments = 0) java.lang.Object... p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }
    }

    //public java.lang.reflect.Executable (shadow created for its nullability, generated)
    class Executable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.reflect.AnnotatedType getAnnotatedReceiverType() { return null; }
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
    }

    //public java.lang.reflect.Field (shadow created for its nullability, generated)
    class Field$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Object get(@Nullable java.lang.Object p0) { return null; }
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        boolean getBoolean(@Nullable java.lang.Object p0) { return false; }
        byte getByte(@Nullable java.lang.Object p0) { return 0; }
        char getChar(@Nullable java.lang.Object p0) { return 0; }
        double getDouble(@Nullable java.lang.Object p0) { return 0; }
        float getFloat(@Nullable java.lang.Object p0) { return 0; }
        int getInt(@Nullable java.lang.Object p0) { return 0; }
        long getLong(@Nullable java.lang.Object p0) { return 0; }
        short getShort(@Nullable java.lang.Object p0) { return 0; }
        @Modified void set(@Nullable java.lang.Object p0, @Nullable java.lang.Object p1) { }
        @Modified void setBoolean(@Nullable java.lang.Object p0, boolean p1) { }
        @Modified void setByte(@Nullable java.lang.Object p0, byte p1) { }
        @Modified void setChar(@Nullable java.lang.Object p0, char p1) { }
        @Modified void setDouble(@Nullable java.lang.Object p0, double p1) { }
        @Modified void setFloat(@Nullable java.lang.Object p0, float p1) { }
        @Modified void setInt(@Nullable java.lang.Object p0, int p1) { }
        @Modified void setLong(@Nullable java.lang.Object p0, long p1) { }
        @Modified void setShort(@Nullable java.lang.Object p0, short p1) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }
    }

    //public java.lang.reflect.InvocationTargetException (shadow created for its nullability, generated)
    class InvocationTargetException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InvocationTargetException$(@Nullable java.lang.Throwable p0) { }
        InvocationTargetException$(@Nullable java.lang.Throwable p0, @Nullable java.lang.String p1) { }
        @Nullable java.lang.Throwable getCause() { return null; }
        @Nullable java.lang.Throwable getTargetException() { return null; }
    }

    //public java.lang.reflect.Method (shadow created for its nullability, generated)
    class Method$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        @Nullable java.lang.Object getDefaultValue() { return null; }
        @Nullable java.lang.Object invoke(@Nullable java.lang.Object p0, @Nullable(nullableArguments = 0) java.lang.Object... p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }
    }

    //public java.lang.reflect.Parameter (shadow created for its nullability, generated)
    class Parameter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
        @Nullable <T extends java.lang.annotation.Annotation> T getDeclaredAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        <T extends java.lang.annotation.Annotation> T[] getDeclaredAnnotationsByType(@NotNull java.lang.Class<T> p0) { return null; }
    }

    //public java.lang.reflect.ParameterizedType (shadow created for its nullability, generated)
    class ParameterizedType$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.reflect.Type getOwnerType() { return null; }
    }

    //public java.lang.reflect.Proxy (shadow created for its nullability, generated)
    class Proxy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Proxy$(@NotNull java.lang.reflect.InvocationHandler p0) { }
        static java.lang.reflect.InvocationHandler getInvocationHandler(@NotNull java.lang.Object p0) { return null; }
        static java.lang.Class<?> getProxyClass(@Nullable java.lang.ClassLoader p0, @NotNull java.lang.Class<?>... p1) { return null; }
        static boolean isProxyClass(@NotNull java.lang.Class<?> p0) { return false; }
        static java.lang.Object newProxyInstance(@Nullable java.lang.ClassLoader p0, @NotNull java.lang.Class<?>[] p1, @NotNull java.lang.reflect.InvocationHandler p2) { return null; }
    }

    //public java.lang.reflect.RecordComponent (shadow created for its nullability, generated)
    class RecordComponent$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends java.lang.annotation.Annotation> T getAnnotation(@NotNull java.lang.Class<T> p0) { return null; }
        @Nullable java.lang.String getGenericSignature() { return null; }
    }
}
