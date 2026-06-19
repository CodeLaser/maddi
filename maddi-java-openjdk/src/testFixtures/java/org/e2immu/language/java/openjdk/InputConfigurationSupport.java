package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class InputConfigurationSupport {

    public static SourceSet javaBase() {
        return new SourceSetImpl.Builder().setName("java.base").setUri(URI.create("file:/"))
                .setLibrary(true)
                .setExternalLibrary(true).setPartOfJdk(true).setModule(true).build();
    }

    public static String tail(URI uri) {
        String toString = uri.toString();
        int last = toString.lastIndexOf('/');
        String name = toString.substring(last + 1);
        assert name.endsWith(".jar");
        return name;
    }

    public static SourceSet sourceSetOf(Class<?> clazz, SourceSet... dependencies) throws URISyntaxException {
        return sourceSetOf(clazz, false, dependencies);
    }

    public static SourceSet sourceSetModuleOf(Class<?> clazz, SourceSet... dependencies) throws URISyntaxException {
        return sourceSetOf(clazz, true, dependencies);
    }

    private static SourceSet sourceSetOf(Class<?> clazz, boolean isModule, SourceSet... dependencies) throws URISyntaxException {
        URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
        return new SourceSetImpl.Builder().setName(tail(uri)).setUri(uri)
                .setModule(isModule)
                .setLibrary(true)
                .setExternalLibrary(true)
                .setDependencies(Arrays.stream(dependencies).toList())
                .build();
    }
}
