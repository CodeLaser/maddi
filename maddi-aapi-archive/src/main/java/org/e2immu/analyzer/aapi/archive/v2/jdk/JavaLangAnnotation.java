package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotModified;

import java.lang.annotation.Annotation;

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
}
