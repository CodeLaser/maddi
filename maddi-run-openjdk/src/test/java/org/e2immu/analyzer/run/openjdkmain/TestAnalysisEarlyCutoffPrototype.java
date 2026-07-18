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

package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prototype of the early-cutoff <em>compare half</em> (analysis-rewiring.md). We analyse a tiny project, snapshot the
 * per-type analysisFingerprint, edit a source file, analyse again, and diff the snapshots. The diff is exactly the
 * set of types whose analyzer output actually moved — the blast radius a real incremental run would have to
 * recompute; everything else is what the cutoff would spare.
 * <p>
 * This demonstrates the <em>decision</em> (which types changed), not yet the saving: both runs are full, clean
 * analyses on a fresh inspector. Wiring the decision into the reload flow so unchanged types are not re-analysed at
 * all is the next step, and it needs the carry so a spared type returns with its analysis rather than empty.
 * <p>
 * Lives in {@code run-openjdk} because that is the only module with the analyzer, the openjdk front-end, and the
 * fingerprint on one path ({@code run-rewire} cannot see {@code modification.analyzer}).
 */
public class TestAnalysisEarlyCutoffPrototype {

    // Data's immutability feeds User (User holds a Data field and hands it out), so a change to Data's verdicts
    // should propagate to User — a two-type blast radius.
    private static final String DATA_V1 = """
            package x;
            public class Data {
                private final int v;
                public Data(int v) { this.v = v; }
                public int v() { return v; }
            }
            """;
    private static final String DATA_MUTABLE = """
            package x;
            public class Data {
                private int v;
                public Data(int v) { this.v = v; }
                public int v() { return v; }
                public void set(int v) { this.v = v; }
            }
            """;
    // User exposes the Data field via a getter whether Data is mutable or not, so its verdicts do NOT depend on
    // Data's mutability: the firewall should SPARE it even though it structurally uses Data.
    private static final String USER = """
            package x;
            public class User {
                private final Data data;
                public User(Data d) { this.data = d; }
                public Data get() { return data; }
            }
            """;
    // Holder's immutability DOES depend on Data's: a public field of an immutable type keeps Holder immutable, but a
    // public field of a mutable type breaks rule 2/3 -- so Data's change must propagate to Holder.
    private static final String HOLDER = """
            package x;
            public class Holder {
                public final Data data;
                public Holder(Data d) { this.data = d; }
            }
            """;

    @TempDir
    Path sourceDir;

    private InputConfiguration inputConfiguration() {
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName("main")
                .setSourceDirectories(List.of(sourceDir))
                .setUri(sourceDir.toUri())
                .build();
        return new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
    }

    /** A full clean analysis on a fresh inspector; returns fqn -> analysisFingerprint for every primary type. */
    private TreeMap<String, FingerPrint> analyzeAndFingerprint() throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr = javaInspector.parse(java.util.Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        PrepAnalyzer prep = new PrepAnalyzer(javaInspector.runtime(),
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        ComputeCallGraph ccg = prep.doPrimaryTypesReturnComputeCallGraph(Set.copyOf(pr.primaryTypes()),
                pr.sourceSetToModuleInfoMap().values(), t -> false, false);
        List<Info> order = new ComputeAnalysisOrder().go(ccg.graph(), false);
        new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(20)
                        .setStopWhenCycleDetectedAndNoImprovements(true).build())
                .analyze(order, ccg.graph());
        TreeMap<String, FingerPrint> snapshot = new TreeMap<>();
        for (TypeInfo pt : pr.primaryTypes()) {
            snapshot.put(pt.fullyQualifiedName(), AnalysisFingerprint.of(javaInspector.runtime(), pt));
        }
        return snapshot;
    }

    /** The compare-half decision: which types' analyzer output changed between two runs. */
    private static Set<String> changedTypes(TreeMap<String, FingerPrint> before, TreeMap<String, FingerPrint> after) {
        Set<String> changed = new TreeSet<>();
        for (String fqn : before.keySet()) {
            if (!before.get(fqn).equals(after.get(fqn))) changed.add(fqn);
        }
        return changed;
    }

    private void write(String data) throws IOException {
        Path pkg = Files.createDirectories(sourceDir.resolve("x"));
        Files.writeString(pkg.resolve("Data.java"), data);
        Files.writeString(pkg.resolve("User.java"), USER);
        Files.writeString(pkg.resolve("Holder.java"), HOLDER);
    }

    @DisplayName("a comment-only edit changes no analyzer output: the whole run would be cut off")
    @Test
    public void testCommentEditIsFullyCutOff() throws IOException {
        write(DATA_V1);
        TreeMap<String, FingerPrint> before = analyzeAndFingerprint();
        assertEquals(Set.of("x.Data", "x.User", "x.Holder"), before.keySet());

        // a comment edit that shifts every line, exactly like RunRewireTests' edit
        write("// a leading comment\n\n" + DATA_V1);
        TreeMap<String, FingerPrint> after = analyzeAndFingerprint();

        Set<String> changed = changedTypes(before, after);
        assertTrue(changed.isEmpty(),
                "a comment edit changes no verdict; the cutoff would spare every type, but these moved: " + changed);
    }

    @DisplayName("a semantic edit (Data becomes mutable) moves Data — and propagates to its dependent User")
    @Test
    public void testSemanticEditBlastRadius() throws IOException {
        write(DATA_V1);
        TreeMap<String, FingerPrint> before = analyzeAndFingerprint();

        write(DATA_MUTABLE);
        TreeMap<String, FingerPrint> after = analyzeAndFingerprint();

        Set<String> changed = changedTypes(before, after);
        System.out.println("=== compare-half blast radius (types whose analyzer output moved) === " + changed);
        assertTrue(changed.contains("x.Data"), "the edited type's own verdicts must move");
        // The firewall: User and Holder both structurally use Data, yet neither's *output* moved (User exposes the
        // field either way; Holder's verdicts didn't change on this analyzer), so the cutoff spares them. This is the
        // whole point of an output fingerprint over structural reachability — 'reaches Data' would recompute both.
        assertFalse(changed.contains("x.User"), "User's output is unchanged, so the cutoff must spare it: " + changed);
        assertFalse(changed.contains("x.Holder"), "Holder's output is unchanged, so the cutoff must spare it: " + changed);
    }
}
