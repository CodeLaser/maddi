/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ <b>THE SHARED {@code java.*} MODEL MUST NOT BE BUILT FROM A {@code ct.sym} BAND.</b>
 * <p>
 * {@code ScanCompilationUnits} runs the configured preloads under
 * {@code if (!runtime.objectTypeInfo().hasBeenInspected())} — the FIRST scan builds the JDK model for the whole
 * run — and it builds it through that task's file manager. A task carrying {@code --release N} points that file
 * manager at {@code ct.sym}'s band for N, so the first source set's release used to decide what every later set
 * gets. Since a committed type cannot gain a member, a set at a higher band then meets a {@code java.util.List}
 * without the method it needs, and its compilation unit is dropped.
 * <p>
 * Measured in the IDE daemon on maddi itself (2026-08-25): {@code maddi-annotation}, first and at level 17,
 * committed {@code List} from the 11–20 band; the 25 sets that followed dropped <b>391 compilation units</b> on
 * {@code List.getLast()}, and 543 analysis hints were skipped because the hint archive addresses methods by
 * position in a method list that is shorter at 17.
 * <p>
 * The probe is {@code List.getFirst()}/{@code getLast()}: added in Java 21 by JEP 431 (SequencedCollection),
 * so they are absent from the 11–20 band and present at 21+. A pass says the preload ran on the running JDK.
 */
public class TestSharedJdkRelease {

    @Language("java")
    private static final String INPUT = """
            package p;
            public class X {
            }
            """;

    @DisplayName("two releases in one configuration: the shared java.util.List is the HIGHER band's")
    @Test
    public void sharedJdkIsTheHighestBandTheConfigurationStates() throws java.io.IOException {
        int running = Runtime.version().feature();
        Assumptions.assumeTrue(running >= 21, "the probe is List.getFirst(), added in Java 21 (running on "
                                              + running + ")");

        JavaInspector javaInspector = new JavaInspectorImpl();
        // exactly the shape that broke: the set scanned FIRST states the LOWER release, and it is therefore the
        // one whose javac task used to perform the preload -- committing java.util.List from its band.
        SourceSet low = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL)
                .setUri(URI.create("file:/low"))
                .setSourceRelease(17)
                .build();
        SourceSet high = new SourceSetImpl.Builder()
                .setName("high")
                .setUri(URI.create("file:/high"))
                .setSourceRelease(21)
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(low, high)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.preload("java.base::java.util");
        javaInspector.initialize(inputConfiguration);
        // one trivial source, because the preload deliberately does not commit what it loads -- the first real
        // scan's "copy into CTM" does (see preloadPass). With no source set carrying anything to scan, nothing
        // consumes it, exactly as nothing preloaded at all before this change.
        javaInspector.parse(Map.of("p.X", INPUT), JavaInspectorImpl.DETAILED_SOURCES);

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        assertNotNull(list, "java.util.List is preloaded");
        List<String> names = list.methods().stream().map(MethodInfo::name).toList();
        // Control (2026-08-25): with the preload left on the first set's task, this is where it fails -- the
        // 11-20 band's List has neither of these, and the set at 21 can then never gain them.
        assertTrue(names.contains("getFirst"),
                "the shared java.util.List must come from the HIGHEST band stated (21), not the first set's (17);"
                + " methods seen: " + names);
        assertTrue(names.contains("getLast"), "same, for getLast(); methods seen: " + names);
    }

    /**
     * ...and the set's OWN sources keep being attributed at its own release. Fix 2 must not become fix 1: the
     * per-set {@code --release} is a separate, deliberate decision ({@code createTask}, and
     * {@link TestPerSourceSetRelease}), and only the preload pass is exempt from it.
     */
    @DisplayName("the per-set release still reaches javac for the set's own sources")
    @Test
    public void perSetReleaseSurvivesThePreloadPass() throws java.io.IOException {
        int running = Runtime.version().feature();
        Assumptions.assumeTrue(running >= 26,
                "the probe is java.applet.Applet, removed in JDK 26 (running on " + running + ")");

        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL)
                .setUri(URI.create("file:/"))
                .setSourceRelease(21)
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.preload("java.base::java.util");
        javaInspector.initialize(inputConfiguration);

        TypeInfo usesApplet = javaInspector.parse(Map.of("p.UsesApplet", """
                package p;
                import java.applet.Applet;
                public class UsesApplet {
                    Applet applet;
                }
                """), JavaInspectorImpl.DETAILED_SOURCES).parseResult().findType("p.UsesApplet");
        assertNotNull(usesApplet, "the set states release 21, where java.applet.Applet still exists");
    }
}
