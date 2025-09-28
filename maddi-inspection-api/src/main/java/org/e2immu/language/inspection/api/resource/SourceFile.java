package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.util.internal.util.StringUtil;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SourceFile(String path, URI uri, SourceSet sourceSet, FingerPrint fingerPrint) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SourceFile that)) return false;
        return Objects.equals(uri(), that.uri()) && Objects.equals(sourceSet(), that.sourceSet());
    }

    private static final Pattern PATTERN = Pattern.compile(".+(!/|:)(.+)\\.class");

    public String fullyQualifiedName() {
        String uri  = uri().toString();
        // go bock until !/ or :
        Matcher m = PATTERN.matcher(uri);
        if(m.matches()) {
            String pathWithoutDotClass = m.group(2);
            return StringUtil.replaceSlashDollar(pathWithoutDotClass);
        }
        throw new UnsupportedOperationException();
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
