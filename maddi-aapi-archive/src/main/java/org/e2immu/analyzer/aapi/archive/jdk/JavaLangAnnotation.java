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

package org.e2immu.analyzer.aapi.archive.jdk;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotModified;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

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
        @NotModified Method element() { return null; }
        @NotModified String foundType() { return null; }
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
    }

    //public enum RetentionPolicy extends Enum<RetentionPolicy>
    @ImmutableContainer
    class RetentionPolicy$ {
        @NotModified static RetentionPolicy [] values() { return null; }
        @NotModified static RetentionPolicy valueOf(String name) { return null; }
    }
}
