package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nested-type access modifiers must be read from the source. Regression for a copy-paste bug in FlagHelper.type that
 * checked {@code Flags.PROTECTED} for the private modifier: a {@code private} nested type came out as PACKAGE, and a
 * {@code protected} nested type came out as both protected and private.
 */
public class TestNestedTypeAccess extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
                public static class Pub {}
                protected static class Prot {}
                static class Pkg {}
                private static class Priv {}
            }
            """;

    @DisplayName("each nested type carries exactly its declared access")
    @Test
    public void test() {
        TypeInfo c = scan("a.b.C", INPUT);

        TypeInfo pub = c.findSubType("Pub");
        assertTrue(pub.access().isPublic());

        TypeInfo prot = c.findSubType("Prot");
        assertTrue(prot.access().isProtected());
        assertFalse(prot.access().isPrivate(), "protected must not also be private");

        TypeInfo pkg = c.findSubType("Pkg");
        assertTrue(pkg.access().isPackage());

        TypeInfo priv = c.findSubType("Priv");
        assertTrue(priv.access().isPrivate(), "private nested type must be private, not package");
    }
}
