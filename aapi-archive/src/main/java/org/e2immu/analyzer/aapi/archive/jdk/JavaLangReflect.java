/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.analyzer.aapi.archive.jdk;

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.type.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class JavaLangReflect {

    final static String PACKAGE_NAME = "java.lang.reflect";

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

    @UtilityClass
    class Array$ {
        static Object newInstance(Class<?> componentType, int length) { return null; }
        static Object newInstance(Class<?> componentType, int... dimensions) { return null; }
        static int getLength(Object object) { return 0; }
        static Object get(Object object, int i) { return null; }
        static boolean getBoolean(Object object, int i) { return false; }
        static byte getByte(Object object, int i) { return 0; }
        static char getChar(Object object, int i) { return '\0'; }
        static short getShort(Object object, int i) { return 0; }
        static int getInt(Object object, int i) { return 0; }
        static long getLong(Object object, int i) { return 0L; }
        static float getFloat(Object object, int i) { return 0.0F; }
        static double getDouble(Object object, int i) { return 0.0; }
        static void set(@Modified Object object, int i, Object object1) { }
        static void setBoolean(@Modified Object object, int i, boolean b) { }
        static void setByte(@Modified Object object, int i, byte b) { }
        static void setChar(@Modified Object object, int i, char c) { }
        static void setShort(@Modified Object object, int i, short s) { }
        static void setInt(@Modified Object object, int i, int i1) { }
        static void setLong(@Modified Object object, int i, long l) { }
        static void setFloat(@Modified Object object, int i, float f) { }
        static void setDouble(@Modified Object object, int i, double d) { }
    }

    class Method$ {
        @Modified
        void setAccessible(boolean flag) { }
        Class<?> getDeclaringClass() { return null; }
        String getName() { return null; }
        int getModifiers() { return 0; }
        TypeVariable<Method>[] getTypeParameters() { return null; }
        Class<?> getReturnType() { return null; }
        Type getGenericReturnType() { return null; }
        Class<?> [] getParameterTypes() { return null; }
        int getParameterCount() { return 0; }
        Type[] getGenericParameterTypes() { return null; }
        Class<?> [] getExceptionTypes() { return null; }
        Type[] getGenericExceptionTypes() { return null; }
        public boolean equals(Object obj) { return false; }
        public int hashCode() { return 0; }
        public String toString() { return null; }
        String toGenericString() { return null; }
        Object invoke(@Modified Object obj, @Modified Object... args) { return null; }
        boolean isBridge() { return false; }
        boolean isVarArgs() { return false; }
        boolean isSynthetic() { return false; }
        boolean isDefault() { return false; }
        Object getDefaultValue() { return null; }
        <T> T getAnnotation(Class<T> annotationClass) { return null; }
        Annotation[] getDeclaredAnnotations() { return null; }
        Annotation[][] getParameterAnnotations() { return null; }
        AnnotatedType getAnnotatedReturnType() { return null; }
    }
}
