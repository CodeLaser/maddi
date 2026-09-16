package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A source set compiled with {@code --release 8} still resolves JDK types to the JDK.
 * <p>
 * At release 8 there is no module system: javac reads the platform from its release table ({@code ct.sym}) and every
 * JDK package's module is the UNNAMED module. {@code ClassSymbolScanner.ensureSourceSet} places a compiled type by
 * its jar, its class directory or its named module, so a release-8 JDK type matched none of them. Until 0b873b335 it
 * fell through to the source set of the compilation task (wrong, but mapped); since then it is a miss, and the parse
 * refuses: {@code Cannot map javac's type 'java.nio.charset.UnsupportedCharsetException' ... onto a TypeInfo}.
 * <p>
 * Measured on OpenSearch, whose {@code client/rest} source sets state release 8: five such errors in four units
 * stopped the whole project from parsing (2026-09-15; it had parsed on 2026-08-28, before that commit).
 */
public class TestReleaseEightPlatformTypes extends CommonTest {

    @Test
    public void aJavaBaseTypeAtRelease8IsJavaBase() {
        release = 8;
        scan("a.b.X", """
                package a.b;
                class X {
                    java.nio.charset.Charset pick(String name) throws java.nio.charset.UnsupportedCharsetException {
                        return java.nio.charset.Charset.forName(name);
                    }
                }
                """);

        TypeInfo unsupported = classSymbolScanner.getType("java.nio.charset.UnsupportedCharsetException");
        assertNotNull(unsupported, "a java.base type resolves at release 8");
        SourceSet set = unsupported.compilationUnit().sourceSet();
        assertNotNull(set, "and has a source set");
        assertTrue(set.partOfJdk(), "it is the JDK: " + set.name());
        assertEquals("java.base", set.name(), "the module that holds java.nio.charset, as on any later release");
    }
}
