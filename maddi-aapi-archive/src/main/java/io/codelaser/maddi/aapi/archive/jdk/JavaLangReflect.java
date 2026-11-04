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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.type.UtilityClass;

public class JavaLangReflect {
    public static final String PACKAGE_NAME = "java.lang.reflect";


    @ImmutableContainer
    interface GenericDeclaration$ {
    }

    @ImmutableContainer
    interface AnnotatedElement$ {
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable default <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        default <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> p0) { return null; }
        @Nullable default <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> p0) { return null; }
        default <T extends Annotation> T[] getDeclaredAnnotationsByType(@NotNull Class<T> p0) { return null; }
        default boolean isAnnotationPresent(@NotNull Class<? extends Annotation> p0) { return false; }
    }

    @ImmutableContainer
    interface AnnotatedType$ {
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable default AnnotatedType getAnnotatedOwnerType() { return null; }
    }

    @ImmutableContainer
    @Independent
    interface Type$ {
    }

    @Independent(hc = true)
    interface AccessibleObject$ {

    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified default boolean canAccess(@Nullable Object p0) { return false; }
        @Nullable default <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        default <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> p0) { return null; }
        @Nullable default <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> p0) { return null; }
        default <T extends Annotation> T[] getDeclaredAnnotationsByType(@NotNull Class<T> p0) { return null; }
        default boolean isAnnotationPresent(@NotNull Class<? extends Annotation> p0) { return false; }
        static void setAccessible(@NotNull AccessibleObject[] p0, boolean p1) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified default void setAccessible(boolean p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified default boolean isAccessible() { return false; }
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
        @Nullable static Object invokeDefault(@NotNull Object p0, @NotNull Method p1, @Nullable(nullableArguments = 0) Object... p2) { return null; }
    }

    //public java.lang.reflect.ClassFileFormatVersion (shadow created for its nullability, generated)
    class ClassFileFormatVersion$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable Runtime.Version runtimeVersion() { return null; }
    }

    //public java.lang.reflect.Constructor (shadow created for its nullability, generated)
    class Constructor$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable AnnotatedType getAnnotatedReceiverType() { return null; }
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        T newInstance(@Nullable(nullableArguments = 0) Object... p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified AnnotatedType getAnnotatedReturnType() { return null; }
        @NotModified Class<T> getDeclaringClass() { return null; }
        @NotModified Class<?>[] getExceptionTypes() { return null; }
        @NotModified Type[] getGenericExceptionTypes() { return null; }
        @NotModified Type[] getGenericParameterTypes() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Annotation[][] getParameterAnnotations() { return null; }
        @NotModified int getParameterCount() { return 0; }
        @NotModified Class<?>[] getParameterTypes() { return null; }
        @NotModified boolean isSynthetic() { return false; }
        @NotModified boolean isVarArgs() { return false; }
        @NotModified String toGenericString() { return null; }
    }

    //public java.lang.reflect.Executable (shadow created for its nullability, generated)
    class Executable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable AnnotatedType getAnnotatedReceiverType() { return null; }
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified AnnotatedType[] getAnnotatedExceptionTypes() { return null; }
        @NotModified AnnotatedType[] getAnnotatedParameterTypes() { return null; }
        @NotModified AnnotatedType getAnnotatedReturnType() { return null; }
        @NotModified Class<?> getDeclaringClass() { return null; }
        @NotModified Class<?>[] getExceptionTypes() { return null; }
        @NotModified Type[] getGenericExceptionTypes() { return null; }
        @NotModified Type[] getGenericParameterTypes() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Annotation[][] getParameterAnnotations() { return null; }
        @NotModified int getParameterCount() { return 0; }
        @NotModified Class<?>[] getParameterTypes() { return null; }
        @NotModified Parameter[] getParameters() { return null; }
        @NotModified boolean isSynthetic() { return false; }
        @NotModified boolean isVarArgs() { return false; }
        @NotModified String toGenericString() { return null; }
    }

    //public java.lang.reflect.Field (shadow created for its nullability, generated)
    class Field$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable Object get(@Nullable Object p0) { return null; }
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> p0) { return null; }
        @NotModified boolean getBoolean(@Nullable Object p0) { return false; }
        @NotModified byte getByte(@Nullable Object p0) { return 0; }
        @NotModified char getChar(@Nullable Object p0) { return 0; }
        @NotModified double getDouble(@Nullable Object p0) { return 0; }
        @NotModified float getFloat(@Nullable Object p0) { return 0; }
        @NotModified int getInt(@Nullable Object p0) { return 0; }
        @NotModified long getLong(@Nullable Object p0) { return 0; }
        @NotModified short getShort(@Nullable Object p0) { return 0; }
        @Modified void set(@Nullable Object p0, @Nullable Object p1) { }
        @Modified void setBoolean(@Nullable Object p0, boolean p1) { }
        @Modified void setByte(@Nullable Object p0, byte p1) { }
        @Modified void setChar(@Nullable Object p0, char p1) { }
        @Modified void setDouble(@Nullable Object p0, double p1) { }
        @Modified void setFloat(@Nullable Object p0, float p1) { }
        @Modified void setInt(@Nullable Object p0, int p1) { }
        @Modified void setLong(@Nullable Object p0, long p1) { }
        @Modified void setShort(@Nullable Object p0, short p1) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified AnnotatedType getAnnotatedType() { return null; }
        @NotModified Class<?> getDeclaringClass() { return null; }
        @NotModified Type getGenericType() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Class<?> getType() { return null; }
        @NotModified boolean isEnumConstant() { return false; }
        @NotModified boolean isSynthetic() { return false; }
        @NotModified String toGenericString() { return null; }
    }

    //public java.lang.reflect.InvocationTargetException (shadow created for its nullability, generated)
    class InvocationTargetException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InvocationTargetException$(@Nullable Throwable p0) { }
        InvocationTargetException$(@Nullable Throwable p0, @Nullable String p1) { }
        @Nullable Throwable getCause() { return null; }
        @NotModified @Nullable Throwable getTargetException() { return null; }
    }

    //public java.lang.reflect.Method (shadow created for its nullability, generated)
    class Method$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        @NotModified @Nullable Object getDefaultValue() { return null; }
        @Nullable Object invoke(@Nullable Object p0, @Nullable(nullableArguments = 0) Object... p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAccessible(boolean p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified AnnotatedType getAnnotatedReturnType() { return null; }
        @NotModified Class<?> getDeclaringClass() { return null; }
        @NotModified Class<?>[] getExceptionTypes() { return null; }
        @NotModified Type[] getGenericExceptionTypes() { return null; }
        @NotModified Type[] getGenericParameterTypes() { return null; }
        @NotModified Type getGenericReturnType() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Annotation[][] getParameterAnnotations() { return null; }
        @NotModified int getParameterCount() { return 0; }
        @NotModified Class<?>[] getParameterTypes() { return null; }
        @NotModified Class<?> getReturnType() { return null; }
        @NotModified boolean isBridge() { return false; }
        @NotModified boolean isDefault() { return false; }
        @NotModified boolean isSynthetic() { return false; }
        @NotModified boolean isVarArgs() { return false; }
        @NotModified String toGenericString() { return null; }
    }

    //public java.lang.reflect.Parameter (shadow created for its nullability, generated)
    class Parameter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> p0) { return null; }
        @Nullable <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> p0) { return null; }
        <T extends Annotation> T[] getDeclaredAnnotationsByType(@NotNull Class<T> p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified AnnotatedType getAnnotatedType() { return null; }
        @NotModified Executable getDeclaringExecutable() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Type getParameterizedType() { return null; }
        @NotModified Class<?> getType() { return null; }
        @NotModified boolean isImplicit() { return false; }
        @NotModified boolean isNamePresent() { return false; }
        @NotModified boolean isSynthetic() { return false; }
        @NotModified boolean isVarArgs() { return false; }
    }

    //public java.lang.reflect.ParameterizedType (shadow created for its nullability, generated)
    class ParameterizedType$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable Type getOwnerType() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Type[] getActualTypeArguments() { return null; }
        @NotModified Type getRawType() { return null; }
    }

    //public java.lang.reflect.Proxy (shadow created for its nullability, generated)
    class Proxy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Proxy$(@NotNull InvocationHandler p0) { }
        static InvocationHandler getInvocationHandler(@NotNull Object p0) { return null; }
        static Class<?> getProxyClass(@Nullable ClassLoader p0, @NotNull Class<?>... p1) { return null; }
        static boolean isProxyClass(@NotNull Class<?> p0) { return false; }
        static Object newProxyInstance(@Nullable ClassLoader p0, @NotNull Class<?>[] p1, @NotNull InvocationHandler p2) { return null; }
    }

    //public java.lang.reflect.RecordComponent (shadow created for its nullability, generated)
    class RecordComponent$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable <T extends Annotation> T getAnnotation(@NotNull Class<T> p0) { return null; }
        @NotModified @Nullable String getGenericSignature() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Method getAccessor() { return null; }
        @NotModified AnnotatedType getAnnotatedType() { return null; }
        @NotModified Class<?> getDeclaringRecord() { return null; }
        @NotModified Type getGenericType() { return null; }
        @NotModified String getName() { return null; }
        @NotModified Class<?> getType() { return null; }
    }

    class Member$ {
        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Class<?> getDeclaringClass() { return null; }
        @NotModified int getModifiers() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified boolean isSynthetic() { return false; }
    }
}
