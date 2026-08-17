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

package org.e2immu.analyzer.ide.daemon;

import org.e2immu.annotation.Immutable;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.util.List;

/**
 * Turns the plain-JSON {@link DaemonProtocol.AnalyzeConfig} the plugin sends into a maddi
 * {@link InputConfiguration}, using the string-based builder (Style A). {@code Builder.build()}
 * auto-wires each source set's dependencies to all classpath parts and all earlier source sets, so
 * the daemon does not have to compute the inter-source-set graph itself — correct for whole-project
 * analysis. Mirrors {@code Main.inputConfiguration} / {@code AnalyzerPropertyComputer}.
 */
public class InputConfigurationAssembler {

    public InputConfiguration build(DaemonProtocol.AnalyzeConfig config) {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();

        if (notBlank(config.workingDirectory())) builder.setWorkingDirectory(config.workingDirectory());
        if (notBlank(config.sdkHome())) builder.setAlternativeJREDirectory(config.sdkHome());
        if (notBlank(config.sourceEncoding())) builder.setSourceEncoding(config.sourceEncoding());

        if (config.sources() != null) {
            for (DaemonProtocol.SourceRoot root : config.sources()) {
                if (root.test()) {
                    builder.addTestSource(root.name(), root.path());
                } else {
                    builder.addSource(root.name(), root.path());
                }
            }
        }

        if (config.classpath() != null) {
            for (DaemonProtocol.ClasspathEntry entry : config.classpath()) {
                String scope = entry.scope() == null ? "compile" : entry.scope();
                switch (scope) {
                    case "test" -> builder.addTestClassPath(entry.path());
                    case "runtime" -> builder.addRuntimeClassPath(entry.path());
                    case "test-runtime" -> builder.addTestRuntimeClassPath(entry.path());
                    default -> builder.addClassPath(entry.path()); // "compile"
                }
            }
        }

        List<String> jmods = config.jmods();
        if (jmods != null) {
            for (String jmod : jmods) {
                if (notBlank(jmod)) builder.addJmodToClassPath(jmod);
            }
        }

        List<String> restrict = config.restrictToPackages();
        if (restrict != null && !restrict.isEmpty()) {
            builder.addRestrictSourceToPackages(restrict.toArray(new String[0]));
        }

        // Supply the e2immu annotation types (@Immutable, @Container, @NotModified, …) as a real classpath part,
        // located from the daemon's own classpath (the distribution bundles maddi-support). Required so
        // DecoratorImpl resolves them, so projects that do NOT depend on the annotations still get hints, and so
        // a project that DOES use them parses those imports. (The openjdk inspector does not support the
        // jar-on-classpath: scheme that withE2ImmuSupportFromClasspath() uses.)
        builder.addClassPathParts(SourceSetImpl.sourceSetOf(Immutable.class));
        // Provide java.base as a first-class source set (like the analyzer's test harness) so the runtime
        // registers its types for hint resolution — otherwise LoadAnalysisResults reports "type not on the
        // classpath" even though the openjdk parser preloaded them.
        builder.addClassPathParts(SourceSetImpl.javaBase());
        return builder.build();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
