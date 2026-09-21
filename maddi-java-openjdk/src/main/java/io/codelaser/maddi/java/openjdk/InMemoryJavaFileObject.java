package io.codelaser.maddi.java.openjdk;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class InMemoryJavaFileObject extends SimpleJavaFileObject {
    private final String source;

    public InMemoryJavaFileObject(String sourceSetName, String className, String source) {
        super(uri(sourceSetName, className), Kind.SOURCE);
        this.source = source;
    }

    /**
     * {@code mem:///<sourceSet>/<a/b/C>.java}: the canonical class name with a {@code .java} extension, below the
     * name of its source set.
     * <p>
     * The source set's name is free text, and a build tool's is not URI-safe: Maven's reads
     * {@code "Camel :: Core Model/main"}. Concatenated into {@link URI#create} that threw "Illegal character in
     * path" on the first in-memory parse of any such project. The multi-argument constructor quotes what is
     * illegal, {@link URI#getPath()} gives the name back as it was, and for a plain name the result is the very
     * URI the concatenation produced.
     */
    public static URI uri(String sourceSetName, String className) {
        try {
            return new URI("mem", "", "/" + sourceSetName + "/" + className.replace('.', '/') + ".java", null, null);
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("no in-memory URI for " + className + " in source set '"
                                               + sourceSetName + "'", e);
        }
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }
}
