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

package io.codelaser.maddi.aapi.archive.jdk;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaLangAnnotation {
    public static final String PACKAGE_NAME = "java.lang.annotation";
    //public interface Annotation
    @ImmutableContainer(hc = true)
    class Annotation$ {
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        Class<? extends Annotation> annotationType() { return null; }
    }

    //public class AnnotationTypeMismatchException extends RuntimeException
    // constructor params (Method, String) are immutable -> @Independent/@NotModified implied, so omitted
    class AnnotationTypeMismatchException$ {
        @Nullable @NotModified Method element() { return null; }
        @Nullable @NotModified String foundType() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        AnnotationTypeMismatchException$(@Nullable java.lang.reflect.Method p0, @Nullable java.lang.String p1) { }
    }

    //public enum ElementType extends Enum<ElementType>
    @ImmutableContainer
    class ElementType$ {
        @NotModified static ElementType [] values() { return null; }
        @NotModified static ElementType valueOf(String name) { return null; }
    }

    //public class IncompleteAnnotationException extends RuntimeException
    // constructor params (Class, String) are immutable -> @Independent/@NotModified implied, so omitted
    class IncompleteAnnotationException$ {
        @NotModified Class<? extends Annotation> annotationType() { return null; }
        @NotModified String elementName() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        IncompleteAnnotationException$(@NotNull java.lang.Class<? extends java.lang.annotation.Annotation> p0, @NotNull java.lang.String p1) { }
    }

    //public enum RetentionPolicy extends Enum<RetentionPolicy>
    @ImmutableContainer
    class RetentionPolicy$ {
        @NotModified static RetentionPolicy [] values() { return null; }
        @NotModified static RetentionPolicy valueOf(String name) { return null; }
    }

    //public java.lang.annotation.AnnotationFormatError (shadow created for its nullability, generated)
    class AnnotationFormatError$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        AnnotationFormatError$(@Nullable java.lang.String p0) { }
        AnnotationFormatError$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
        AnnotationFormatError$(@Nullable java.lang.Throwable p0) { }
    }
}
