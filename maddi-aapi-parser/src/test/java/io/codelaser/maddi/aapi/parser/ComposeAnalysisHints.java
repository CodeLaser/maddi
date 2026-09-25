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

package io.codelaser.maddi.aapi.parser;

import ch.qos.logback.classic.Level;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.prepwork.io.DecoratorImpl;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Writes first-cut analysis hint sources for a whole library, from its JAR: one {@code PACKAGE_NAME} class per
 * library package, one {@code $}-suffixed shadow per public type (use case 3, but jar-driven rather than
 * call-frequency driven). Run as {@code ./gradlew :maddi-aapi-parser:composeAnalysisHints} with
 * <ul>
 *     <li>{@code -Pmaddi.compose.anchor=io.vavr.Value,io.vavr.match.annotation.Patterns} -- a class of the
 *     library, then one class of each jar its class files refer to; the jars are found on this module's test class
 *     path (add the dependency as {@code testImplementation} first);</li>
 *     <li>{@code -Pmaddi.compose.packages=io.vavr} -- the package prefix to compose;</li>
 *     <li>{@code -Pmaddi.compose.target=io.codelaser.maddi.aapi.archive.libs.vavr} -- the hints package;</li>
 *     <li>{@code -Pmaddi.compose.out=../maddi-aapi-archive/src/main/java} -- the source root to write into;</li>
 *     <li>optionally {@code -Pmaddi.compose.preload=dir1,dir2} -- analysis results (e.g. from a SOURCE run of the
 *     library, {@code --analysis-results-dir}) to load onto the jar's types first, so that the shadows carry the
 *     COMPUTED verdicts as their starting annotations;</li>
 *     <li>optionally {@code -Pmaddi.compose.notes=../maddi-aapi-archive/.../libs/vavr/VAVR.md} -- the library report,
 *     whose type table ({@code | `type` | computed | expected | gap |}) is written into each shadow as an
 *     {@code // EXPECTED ...} comment.</li>
 * </ul>
 * The output is a starting point to curate, not a finished hint file: see the library's report next to it.
 */
public class ComposeAnalysisHints {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeAnalysisHints.class);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        String anchor = required("maddi.compose.anchor");
        String packagePrefix = required("maddi.compose.packages");
        String target = required("maddi.compose.target");
        String out = required("maddi.compose.out");
        String preload = System.getProperty("maddi.compose.preload");

        // sourceSetOf, never a hand-rolled Builder: a loaded class file is attributed to its source set by the JAR's
        // file name (see CompileAnalysisHints.kotlinJavaInspectorFactory)
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiAnnotation = SourceSetImpl.sourceSetOf(Immutable.class);
        // comma-separated: the library's own class first, then one class of each jar its class files refer to
        // (vavr: io.vavr.Value,io.vavr.match.annotation.Patterns)
        List<SourceSet> libraries = new java.util.ArrayList<>();
        for (String a : anchor.split(",")) libraries.add(SourceSetImpl.sourceSetOf(Class.forName(a.trim())));

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.preload("java.base::java.util.");
        javaInspector.preload("io.codelaser.maddi.annotation.");
        javaInspector.preload(packagePrefix);
        var inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPathParts(javaBase, maddiAnnotation)
                .addClassPathParts(libraries.toArray(new SourceSet[0]))
                .build();
        javaInspector.initialize(inputConfiguration);
        inputConfiguration.classPathParts().forEach(SourceSet::computePriorityDependencies);
        javaInspector.onlyPreload();

        if (preload != null && !preload.isBlank()) {
            // only the published verdicts: a source run's results also carry internal properties the prepwork codec
            // cannot decode (links, methodLinks, ...), and downcastParameter's decoding needs types still loading
            int loaded = new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources())
                    .setPropertyKeyFilter(PUBLISHED_KEYS::contains)
                    .go(Arrays.asList(preload.split(",")));
            LOGGER.info("Loaded {} analysis result(s) from {}", loaded, preload);
        }

        List<TypeInfo> primaryTypes = javaInspector.compiledTypesManager().typesLoaded(true).stream()
                .filter(TypeInfo::isPrimaryType)
                .filter(t -> t.packageName() != null && (t.packageName().equals(packagePrefix)
                                                         || t.packageName().startsWith(packagePrefix + ".")))
                .toList();
        LOGGER.info("Composing hints for {} primary type(s) under {}", primaryTypes.size(), packagePrefix);

        AnalysisHintsComposer composer = new AnalysisHintsComposer(javaInspector, _ -> target,
                info -> info.access().isPublic());
        String notes = System.getProperty("maddi.compose.notes");
        if (notes != null && !notes.isBlank()) {
            List<ExpectedRow> rows = readExpectedRows(java.nio.file.Path.of(notes));
            LOGGER.info("Read {} EXPECTED row(s) from {}", rows.size(), notes);
            String report = java.nio.file.Path.of(notes).getFileName().toString();
            composer.setTypeNotes(typeInfo -> rows.stream()
                    .filter(r -> r.matches(typeInfo.fullyQualifiedName()))
                    .findFirst()
                    .map(r -> List.of("EXPECTED " + r.expected() + " -- computed " + r.computed() + " -- " + r.gap()
                                      + " (" + report + ")"))
                    .orElse(List.of()));
        }
        Collection<TypeInfo> apiTypes = composer.compose(primaryTypes);
        Map<Element, Element> dollarMap = composer.translateFromDollarToReal();
        // imports resolved from the main source set, which sees the whole class path: the jar's own source set sees
        // nothing, and star-import clashes (java.util.* vs io.vavr.collection.*) would go undetected
        // EXPLICIT decoration: hints written from a computed analysis must round-trip to it (see DecoratorImpl)
        composer.write(apiTypes, new java.io.File(out), new DecoratorImpl(javaInspector.runtime(),
                javaInspector.mainSources(), dollarMap, preload != null && !preload.isBlank()),
                javaInspector.mainSources());
    }

    /*
     The keys the hint decorator prints (DecoratorImpl), minus downcastParameter. Everything else in a source run's
     results is internal to the analysis.
     */
    private static final java.util.Set<String> PUBLISHED_KEYS = java.util.Set.of("analyzerError", "commutableMethods",
            "containerType", "eventualMethod", "eventualParameter", "eventuallyFinalField", "eventuallyImmutableType",
            "eventuallyNonModifyingMethod", "eventuallyUnmodifiedParameter", "finalField", "finalizerMethod",
            "fluentMethod", "getSetEquivalent", "getSetField", "identityMethod", "ignoreModMethod",
            "ignoreModificationsField", "ignoreModsParameter", "immutableField", "immutableMethod",
            "immutableParameter", "immutableType", "independentField", "independentMethod", "independentParameter",
            "independentType", "independentTypeParameter", "methodAllowsInterrupts", "nonModifyingMethod",
            "notNullField", "notNullMethod", "notNullParameter", "staticSideEffectsMethod", "unmodifiedField",
            "unmodifiedParameter", "utilityClass");

    /**
     * One row of a library report's type table: {@code | `pattern` | computed | expected | gap |}. The pattern is a
     * fully qualified type name, optionally ending in {@code *} (a prefix). The first matching row wins, so list
     * specific types before families. Cells may contain backticks; they are stripped.
     */
    record ExpectedRow(String pattern, String computed, String expected, String gap) {
        boolean matches(String fqn) {
            return pattern.endsWith("*") ? fqn.startsWith(pattern.substring(0, pattern.length() - 1))
                    : pattern.equals(fqn);
        }
    }

    static List<ExpectedRow> readExpectedRows(java.nio.file.Path report) throws IOException {
        List<ExpectedRow> rows = new java.util.ArrayList<>();
        for (String line : java.nio.file.Files.readAllLines(report)) {
            if (!line.startsWith("| `")) continue;
            String[] cells = line.split("\\|");
            if (cells.length < 5) continue;
            String pattern = cells[1].trim().replace("`", "");
            if (!pattern.contains(".")) continue;
            rows.add(new ExpectedRow(pattern, clean(cells[2]), clean(cells[3]), clean(cells[4])));
        }
        return rows;
    }

    private static String clean(String cell) {
        return cell.trim().replace("`", "");
    }

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing -P" + key);
        return value;
    }
}
