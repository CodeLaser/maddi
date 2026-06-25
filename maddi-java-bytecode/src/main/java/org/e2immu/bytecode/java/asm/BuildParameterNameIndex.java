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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Offline driver that builds a {@link ParameterNameIndex} for a module/jar: it enumerates the primary types in
 * the given {@link Resources}, loads each through the (faithful, LocalVariableTable-reading) ASM loader, and
 * records the parameter names. Run occasionally to (re)generate the index files shipped to the javac-based loader.
 */
public class BuildParameterNameIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildParameterNameIndex.class);

    public static ParameterNameIndex build(CompiledTypesManager compiledTypesManager, Resources resources,
                                           SourceSet sourceSet, Predicate<SourceFile> filter) {
        Set<String> primaryFqns = new TreeSet<>();
        resources.visit(new String[0], (_, sourceFiles) -> {
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile.path().endsWith(".class") && filter.test(sourceFile)) {
                    String fqn = sourceFile.fullyQualifiedNameFromPath();
                    // primary types only; subtypes are reached via ParameterNameIndex.putRecursively
                    if (fqn.indexOf('$') < 0
                        && !fqn.endsWith("module-info") && !fqn.endsWith("package-info")) {
                        primaryFqns.add(fqn);
                    }
                }
            }
        });
        ParameterNameIndex index = new ParameterNameIndex();
        int failed = 0;
        for (String fqn : primaryFqns) {
            try {
                TypeInfo typeInfo = compiledTypesManager.getOrLoad(fqn, sourceSet);
                if (typeInfo != null) index.putRecursively(typeInfo);
            } catch (RuntimeException re) {
                failed++;
                LOGGER.warn("Skipping {} while indexing parameter names: {}", fqn, re.toString());
            }
        }
        LOGGER.info("Parameter name index: {} methods from {} primary types ({} failed to load)",
                index.size(), primaryFqns.size(), failed);
        return index;
    }

    public static ParameterNameIndex build(CompiledTypesManager compiledTypesManager, Resources resources,
                                           SourceSet sourceSet) {
        return build(compiledTypesManager, resources, sourceSet, _ -> true);
    }
}
