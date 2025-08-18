package org.e2immu.analyzer.aapi.archive.v2.jdk;

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.type.UtilityClass;

public class JavaLangReflect {
    public static final String PACKAGE_NAME = "java.lang.reflect";


    @ImmutableContainer
    interface GenericDeclaration$ {
    }

    @ImmutableContainer
    interface AnnotatedElement$ {
    }

    @ImmutableContainer
    interface AnnotatedType$ {
    }

    @ImmutableContainer
    @Independent
    interface Type$ {
    }

    @Independent(hc = true)
    interface AccessibleObject$ {

    }

    //public final class Array
    @UtilityClass
    class Array$ {
        //@Immutable(hc=true)[T] @Independent[T]
        static Object newInstance(Class<?> componentType, int length) { return null; }

        //@Immutable(hc=true)[T] @Independent[T]
        static Object newInstance(Class<?> componentType, int ... dimensions) { return null; }

        static int getLength(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) {
            return 0;
        }

        //@Immutable(hc=true)[T] @Independent[T]
        static Object get(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return null;
        }

        static boolean getBoolean(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            int i) { return false; }

        static byte getByte(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return 0;
        }

        static char getChar(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return '\0';
        }

        static short getShort(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return 0;
        }

        static int getInt(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return 0;
        }

        static long getLong(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return 0L;
        }

        static float getFloat(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object, int i) {
            return 0.0F;
        }

        static double getDouble(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            int i) { return 0.0; }

        static void set(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }

        static void setBoolean(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            boolean b) { }

        static void setByte(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            byte b) { }

        static void setChar(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            char c) { }

        static void setShort(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            short s) { }

        static void setInt(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            int i1) { }

        static void setLong(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            long l) { }

        static void setFloat(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Modified Object object,
            int i,
            float f) { }

        static void setDouble(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/@Modified Object object,
            int i,
            double d) { }
    }
}
