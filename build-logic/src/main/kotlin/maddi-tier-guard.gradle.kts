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

// The compile-time wall of the dist tier (docs/roadmap/split-maddi-into-three-repositories.md §1): a dist module
// carries the modification analysis (maddi-mod) at RUN time and never COMPILES against it. The dist build includes
// maddi-mod to bundle it, so nothing physical stops an `implementation("io.codelaser:maddi-…")` of a mod module;
// this plugin does. It judges what javac actually sees: every compile class path and annotation-processor path of
// the module, AFTER resolution, so a transitive leak or a declaration in any syntax is caught as well as a direct
// one. A module is recognised by its name, which is the same as a project in this build, as a project of an
// included build and as a published coordinate, so the rule holds from source and pinned alike.
// The tiers are read from tiers.txt next to this plugin: the one list tools/tiers/check_tiers.py reads too.
// Base and mod need no guard of this kind: after the split, neither build includes a tier above it.

val tiers: Map<String, String> = io.codelaser.maddi.buildlogic.Tiers.byModule

if (tiers[project.name] == "dist") {
    val modModules = tiers.filterValues { it == "mod" }.keys
    val projectPath = project.path
    fun isCompileSide(name: String) = name == "compileClasspath" || name.endsWith("CompileClasspath")
            || name == "annotationProcessor" || name.endsWith("AnnotationProcessor")
    configurations.matching { it.isCanBeResolved && isCompileSide(it.name) }.configureEach {
        val configurationName = name
        incoming.afterResolve {
            val leaks = resolutionResult.allComponents
                .filter { it.moduleVersion?.name in modModules }
                .map { component ->
                    val via = component.dependents.mapNotNull { it.from.moduleVersion?.name }.distinct().sorted()
                    component.moduleVersion!!.name + " (required by " + via.joinToString(", ") + ")"
                }
                .sorted()
            if (leaks.isNotEmpty()) {
                throw GradleException("maddi tier rule: $projectPath is in the dist tier, which compiles against base " +
                        "only, but its $configurationName resolves maddi-mod module(s): " + leaks.joinToString("; ") +
                        ". Declare them runtimeOnly (or shadeRuntime in a plugin) and reach the analysis through " +
                        "io.codelaser.maddi.analysis.api.AnalysisEngine. See build-logic/src/main/resources/tiers.txt.")
            }
        }
    }
}
