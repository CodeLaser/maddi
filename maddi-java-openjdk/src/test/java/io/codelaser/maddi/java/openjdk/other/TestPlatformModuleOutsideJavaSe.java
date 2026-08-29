package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A type from a platform module that the configuration does not list is still the JDK, and must not be attributed to
 * the source set of the project being compiled.
 * <p>
 * The build plugins' {@code jmods} option defaults to {@code JavaModules.DEFAULT_JMODS} = {@code "java.se"}, whose
 * closure is the Java SE platform: twenty {@code java.*} modules and no {@code jdk.*} module, because the
 * {@code jdk.*} modules sit outside Java SE by definition. {@code sun.misc.Unsafe} lives in {@code jdk.unsupported},
 * so the lookup in {@code ClassSymbolScanner.ensureSourceSet} missed on every JDK and every build system, and the
 * method then returned the source set of the CURRENT COMPILATION TASK — not a failure to classify the type, but a
 * positive claim that a JDK class is part of the project's own source.
 * <p>
 * Measured on QuestDB, whose {@code io.questdb.std.Unsafe} delegates to {@code sun.misc.Unsafe}. With that class
 * counted as project code, handing a value to it read as a pass-through to another project method rather than as a
 * use nothing can reason about: nearly every method of that class lost its parameters and 2 832 call sites were
 * rewritten to pass no arguments.
 */
public class TestPlatformModuleOutsideJavaSe extends CommonTest {

    @Test
    public void aTypeFromJdkUnsupportedIsPartOfTheJdk() {
        // the reference is what makes javac resolve it; jdk.unsupported exports sun.misc without any --add-exports
        scan("a.b.X", "package a.b; class X { sun.misc.Unsafe u; }");

        TypeInfo unsafe = classSymbolScanner.getType("sun.misc.Unsafe");
        assertNotNull(unsafe, "sun.misc.Unsafe resolves: it is in jdk.unsupported, a module of every JDK");

        SourceSet set = unsafe.compilationUnit().sourceSet();
        assertNotNull(set, "a resolved type has a source set");
        assertTrue(set.partOfJdk(),
                "sun.misc.Unsafe is the JDK, whatever the configuration lists. It was attributed to the source set"
                + " of the compilation task instead: " + set.name());
        assertTrue(set.externalLibrary(),
                "and therefore not project source: " + set.name());
    }
}
