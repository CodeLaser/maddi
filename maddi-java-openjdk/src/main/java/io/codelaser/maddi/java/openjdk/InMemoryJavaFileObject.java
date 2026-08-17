package org.e2immu.language.java.openjdk;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class InMemoryJavaFileObject extends SimpleJavaFileObject {
    private final String source;

    public InMemoryJavaFileObject(String sourceSetName, String className, String source) {
        // URI must use the canonical class name with .java extension
        // e.g. "com.example.Foo" -> "mem:///com/example/Foo.java"
        super(URI.create("mem:///" + sourceSetName + "/" + className.replace('.', '/') + ".java"),
                Kind.SOURCE);
        this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }
}
