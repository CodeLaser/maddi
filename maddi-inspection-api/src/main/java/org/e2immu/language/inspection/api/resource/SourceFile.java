package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;

import java.net.URI;
import java.util.Objects;

public record SourceFile(String path, URI uri, SourceSet sourceSet, FingerPrint fingerPrint) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SourceFile that)) return false;
        return Objects.equals(uri(), that.uri()) && Objects.equals(sourceSet(), that.sourceSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri(), sourceSet());
    }

    public static String ensureDotClass(String substring) {
        if (!substring.endsWith(".class")) {
            return substring + ".class";
        }
        return substring;
    }

    public String stripDotClass() {
        return Resources.stripDotClass(path);
    }

    public SourceFile withFingerprint(FingerPrint fingerPrint) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }

    public SourceFile withPath(String path) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }

    public SourceFile withURI(URI uri) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }
}
