package io.codelaser.maddi.java.openjdk.initial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.java.openjdk.InMemoryJavaFileObject;

/**
 * A source set's name is free text; a Maven reactor's reads "Project :: Module/main". The in-memory URI quotes
 * it, gives it back unchanged, and is unchanged itself for a plain name.
 */
public class TestInMemoryUri {

    @Test
    public void plainNameIsTheUriItAlwaysWas() {
        assertEquals(URI.create("mem:///main/a/b/C.java"), InMemoryJavaFileObject.uri("main", "a.b.C"));
    }

    @Test
    public void nameOfABuildToolIsQuoted() {
        URI uri = InMemoryJavaFileObject.uri("Project :: Core Model/main", "a.b.C");
        assertEquals("mem", uri.getScheme());
        assertEquals("/Project :: Core Model/main/a/b/C.java", uri.getPath());
        // the file object itself is constructed, which is where URI.create used to throw
        assertEquals(uri, new InMemoryJavaFileObject("Project :: Core Model/main", "a.b.C", "").toUri());
    }
}
