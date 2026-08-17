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

package io.codelaser.maddi.inspection.resource;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves the service types a module descriptor names in its {@code uses} and {@code provides} directives, so
 * that {@code ComputeCallGraph} can turn them into module→type edges. Gap ledger {@code #201}.
 *
 * <p>⛔⛔ <b>WHAT A DIRECTIVE HOLDS IS THE WRITTEN TEXT, NOT AN FQN.</b> {@code ParseModuleInfo} stores
 * {@code apiNode.getSource()} — deliberately, because a refactoring has to rewrite a directive the way its
 * author wrote it. Resolution therefore has to go through the descriptor's own imports: a module declaration has
 * no package, so a short name can come from nowhere else (JLS 7.7). Measured on Elasticsearch's 119 descriptors:
 * <b>37 of the names are written short</b> — 2 of 19 {@code uses}, 17 of 70 {@code provides} apis, 18 of 126
 * implementations — and every single one is resolvable from its own file.
 *
 * <p>⛔⛔ <b>AND THIS EXISTS AS A SHARED CLASS BECAUSE ONE OF THE TWO INSPECTORS DID NOT RESOLVE AT ALL.</b> The
 * congocc inspector resolved (by FQN, hence the bug above); the <b>openjdk</b> inspector — the one every real
 * run uses — never called {@code setApiResolved}, so {@code apiResolved()} was null for <em>every</em>
 * directive, short or qualified, and the call graph lost <em>every</em> module→service edge. Neither the
 * consumer nor anything else could see it: {@code ComputeCallGraph} skips a null api, and
 * {@code implementationsResolved()} defaults to an empty list. ▶ <b>A DEFECT FIXED IN ONE READER IS NOT
 * FIXED</b>, and here one reader had never been written.
 *
 * <p>⚠ <b>THE COUNTS COME BACK WITH THEIR DENOMINATORS.</b> "0 resolved through an import" and "the import rule
 * was never reached" are the same log line otherwise ({@code #188}).
 */
public class ResolveModuleDirectives {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolveModuleDirectives.class);

    private ResolveModuleDirectives() {
    }

    /**
     * @param descriptors how many module descriptors were seen at all — 0 means the question never arose
     * @param named       every service type named by a {@code uses} or {@code provides}, api and implementations
     * @param resolved    those that were found on the source set's class path
     * @param viaImport   those that resolved only because the descriptor's own imports were consulted
     */
    public record Counts(int descriptors, int named, int resolved, int viaImport) {
    }

    /**
     * Resolves every directive of every descriptor in {@code summary}. Idempotent per directive it has already
     * resolved is <b>not</b> guaranteed — {@code setApiResolved} is commit-once and throws on a second call — so
     * call this exactly once, after all source sets are parsed and their types known.
     */
    public static Counts go(Summary summary, CompiledTypesManager compiledTypesManager) {
        int named = 0;
        int resolved = 0;
        int viaImport = 0;
        Map<SourceSet, ModuleInfo> map = summary.sourceSetToModuleInfoMap();
        for (Map.Entry<SourceSet, ModuleInfo> entry : map.entrySet()) {
            SourceSet sourceSet = entry.getKey();
            ModuleInfo moduleInfo = entry.getValue();
            Map<String, String> shortNames = moduleInfo.importedShortNames();
            for (ModuleInfo.Uses uses : moduleInfo.uses()) {
                ++named;
                String fqn = ModuleInfo.resolveDirectiveName(uses.api(), shortNames);
                TypeInfo r = compiledTypesManager.typeIfLoaded(fqn, sourceSet);
                if (r != null) {
                    uses.setApiResolved(r);
                    ++resolved;
                    if (!fqn.equals(uses.api())) ++viaImport;
                }
            }
            for (ModuleInfo.Provides provides : moduleInfo.provides()) {
                ++named;
                String apiFqn = ModuleInfo.resolveDirectiveName(provides.api(), shortNames);
                TypeInfo r0 = compiledTypesManager.typeIfLoaded(apiFqn, sourceSet);
                if (r0 != null) {
                    provides.setApiResolved(r0);
                    ++resolved;
                    if (!apiFqn.equals(provides.api())) ++viaImport;
                }
                List<TypeInfo> implementationsResolved = new ArrayList<>();
                for (String implementation : provides.implementations()) {
                    ++named;
                    String implFqn = ModuleInfo.resolveDirectiveName(implementation, shortNames);
                    TypeInfo r1 = compiledTypesManager.typeIfLoaded(implFqn, sourceSet);
                    if (r1 != null) {
                        implementationsResolved.add(r1);
                        ++resolved;
                        if (!implFqn.equals(implementation)) ++viaImport;
                    }
                }
                provides.setImplementationsResolved(implementationsResolved);
            }
        }
        Counts counts = new Counts(map.size(), named, resolved, viaImport);
        if (named > 0) {
            LOGGER.info("Module directives in {} descriptor(s): {} service type(s) named, {} resolved, of which"
                        + " {} through the descriptor's own imports (a short name resolves no other way)",
                    counts.descriptors(), counts.named(), counts.resolved(), counts.viaImport());
        } else {
            LOGGER.debug("Module directives: {} descriptor(s), none naming a service type", counts.descriptors());
        }
        return counts;
    }
}
