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
package org.e2immu.analyzer.run.config.compile;

import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Assembles a {@link CompileListToSourceSets.Result} into an {@link InputConfiguration}: the JDK module closure,
 * the source sets, the libraries.
 *
 * <p>⚠ THIS EXISTS BECAUSE THERE WERE TWO COPIES OF IT. {@code ParseJavacList} and {@code ParseKotlincList} each
 * carried the same twenty lines, so anything added to one silently did not hold for the other — and a defect
 * fixed in one reader is not fixed. They now both call this, which is what makes the checks below hold for both
 * front-ends BY CONSTRUCTION rather than by remembering.
 */
public class CompileListToInputConfiguration {

    /**
     * @param result     the source-set graph reconstructed from the compile invocations
     * @param extraJmods JDK modules to add on top of the {@code java.se} closure, each with its own closure
     */
    public static InputConfiguration build(CompileListToSourceSets.Result result, List<String> extraJmods) {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        Set<String> closure = new HashSet<>(JavaModules.jmodDependencyClosure("java.se"));
        if (extraJmods != null) {
            extraJmods.forEach(jm -> {
                closure.add(jm);
                closure.addAll(JavaModules.jmodDependencyClosure(jm));
            });
        }
        // sorted() so the jmod classpath parts have a deterministic order (a HashSet's iteration order is not
        // stable across runs, which otherwise shuffles the serialized InputConfiguration)
        closure.stream().sorted().forEach(jmod -> builder.addClassPathParts(
                new SourceSetImpl.Builder().setName(jmod)
                        .setSourceDirectories(List.of())
                        .setUri(URI.create("jmod:" + jmod))
                        .setLibrary(true)
                        .setExternalLibrary(true)
                        .setPartOfJdk(true)
                        .setModule(true)
                        .build()));

        List<SourceSet> sourceSets = result.jSourceSets().stream()
                .map(CompileListToSourceSets.JSourceSet::sourceSet).toList();
        // A compile classpath is not a closure over the TYPE_USE annotations its dependencies carry.
        sourceSets = new TypeUseAnnotationClosure().close(sourceSets, result.jars()).sourceSets();

        sourceSets.forEach(builder::addSourceSets);
        result.jars().forEach(builder::addClassPathParts);
        return builder.build();
    }
}
