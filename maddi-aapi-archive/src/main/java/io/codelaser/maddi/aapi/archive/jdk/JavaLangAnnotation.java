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
