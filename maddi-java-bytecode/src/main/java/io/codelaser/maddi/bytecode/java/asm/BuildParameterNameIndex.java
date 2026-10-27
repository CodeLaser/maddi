/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.bytecode.java.asm;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import io.codelaser.maddi.inspection.api.resource.ParameterNameIndex;
import io.codelaser.maddi.inspection.api.resource.Resources;
import io.codelaser.maddi.inspection.api.resource.SourceFile;
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
                TypeInfo typeInfo = compiledTypesManager.type(fqn, sourceSet);
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
