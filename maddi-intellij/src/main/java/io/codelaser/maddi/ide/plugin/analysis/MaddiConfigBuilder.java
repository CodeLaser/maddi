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

package io.codelaser.maddi.ide.plugin.analysis;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.lang.JavaVersion;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.VirtualFile;
import io.codelaser.maddi.ide.client.AnalysisModel;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the daemon's {@code AnalyzeConfig} from IntelliJ's project model, as PER-MODULE source sets: each
 * carries its own class path and names its own direct dependencies, the shape the javac-log route produces.
 * <p>
 * ⛔ It used to flatten the project instead — every source root in one list, every library in the project in
 * one class path, and every module's compiler output on that class path as "hot class files". Three separate
 * failures came out of that one decision, all measured on the CodeLaser tree (2026-08-21):
 * <ul>
 *   <li>{@code InputConfiguration}'s string builder wires every set to all parts and all earlier sets, giving
 *       160 x 491 = 75,520 (set, entry) pairs where the javac-log configuration declares 5,524. javac opens a
 *       ZipFileSystem per container per source set, so preload OOMed at 12 GB and again at 24 GB.</li>
 *   <li>A project-wide union has no scope, so {@code compileOnly} / PROVIDED entries were simply absent:
 *       {@code CommonMojo extends AbstractMojo} could not resolve, and one such module used to cost the whole
 *       project its analysis.</li>
 *   <li>Every module's output on the class path made every FQN reachable as source AND bytecode, which is what
 *       produced {@code null parent class} commits under self-analysis.</li>
 * </ul>
 * A module's compiler output is still reported — as the source set's {@code outputPath}, its identity — but it
 * is never a class-path entry of another set.
 * <p>
 * ⚠ Per-module, NOT recursive: {@code SourceSetImpl.recursiveDependencies} computes the closure itself, so
 * asking IntelliJ for direct dependencies is both correct and much cheaper.
 * <p>
 * Must be called inside a read action (touches the project/roots model).
 */
public final class MaddiConfigBuilder {

    /** JDK modules loaded by default. Covers common enterprise usage; made configurable in M4. */
    private static final List<String> DEFAULT_JMODS = List.of(
            "java.base", "java.logging", "java.xml", "java.sql", "java.naming",
            "java.desktop", "java.management", "java.net.http", "java.compiler");

    /**
     * @param jdkHome        home of the maddi JDK (25+); becomes both the run JDK and the analysis SDK — NOT the
     *                       project SDK, which may target an older release maddi cannot read java.base from.
     * @param warnNearMisses ask the analyzer for advisory near-miss warnings (opt-in; noisy by nature)
     */
    public AnalysisModel.AnalyzeConfig build(Project project, String jdkHome, boolean warnNearMisses) {
        List<AnalysisModel.ModuleSourceSet> sets = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager roots = ModuleRootManager.getInstance(module);
            List<String> main = pathsOf(roots.getSourceRoots(JavaSourceRootType.SOURCE));
            List<String> test = pathsOf(roots.getSourceRoots(JavaSourceRootType.TEST_SOURCE));
            if (main.isEmpty() && test.isEmpty()) continue;   // a grouping module, or resources only

            // This module's OWN libraries, at every scope. No .recursively(): what a dependency exports is
            // reached through the module edge below, and maddi closes over that itself. Crucially no scope
            // filter either — PROVIDED (Gradle compileOnly, Maven provided) is a compile input like any other,
            // and dropping it is what left AbstractMojo unresolved.
            List<String> classPath = new ArrayList<>();
            for (VirtualFile root : OrderEnumerator.orderEntries(module)
                    .librariesOnly().withoutSdk().classes().getRoots()) {
                String path = localPath(root);
                if (!classPath.contains(path)) classPath.add(path);
            }

            // Direct module dependencies, by the name of their PRODUCTION set. A Gradle import already gives
            // one IntelliJ module per source set (…​.main / …​.test), so this is usually a single edge; a Maven
            // import gives one module with both root kinds, and the two specs below split it.
            List<String> moduleDeps = new ArrayList<>();
            for (Module dependency : roots.getDependencies()) {
                String name = mainSetName(dependency);
                if (!name.equals(mainSetName(module)) && !moduleDeps.contains(name)) moduleDeps.add(name);
            }

            CompilerModuleExtension output = CompilerModuleExtension.getInstance(module);
            int release = languageLevel(project, roots);
            if (!main.isEmpty()) {
                sets.add(new AnalysisModel.ModuleSourceSet(mainSetName(module), main,
                        pathOrNull(output == null ? null : output.getCompilerOutputPath()),
                        false, release, moduleDeps, classPath));
            }
            if (!test.isEmpty()) {
                // a test set compiles against its own production set; when IntelliJ split them into separate
                // modules that edge is already in moduleDeps, and the extra name is simply absent from the map
                List<String> testDeps = new ArrayList<>(moduleDeps);
                if (!main.isEmpty()) testDeps.add(mainSetName(module));
                sets.add(new AnalysisModel.ModuleSourceSet(testSetName(module), test,
                        pathOrNull(output == null ? null : output.getCompilerOutputPathForTests()),
                        true, release, testDeps, classPath));
            }
        }

        return new AnalysisModel.AnalyzeConfig(
                project.getBasePath(),
                jdkHome,
                "UTF-8",
                DEFAULT_JMODS,
                List.of(),      // the flat pair stays empty: sets below carry the structure
                List.of(),
                List.of(),
                true,
                warnNearMisses,
                sets);
    }

    private static String mainSetName(Module module) {
        return module.getName() + "/main";
    }

    private static String testSetName(Module module) {
        return module.getName() + "/test";
    }

    private static List<String> pathsOf(List<VirtualFile> roots) {
        List<String> paths = new ArrayList<>();
        for (VirtualFile root : roots) paths.add(root.getPath());
        return paths;
    }

    private static String pathOrNull(VirtualFile file) {
        return file == null ? null : file.getPath();
    }

    /**
     * The module's effective Java release, or 0 for "not known" — which the daemon then reads as "use the JDK
     * maddi runs on". That default is dangerous rather than merely imprecise (see {@code InputConfiguration}),
     * so report a real number whenever IntelliJ has one.
     */
    private static int languageLevel(Project project, ModuleRootManager roots) {
        // a ModuleExtension, so it comes off ModuleRootManager; there is no static getInstance(Module)
        LanguageLevelModuleExtension moduleExtension = roots.getModuleExtension(LanguageLevelModuleExtension.class);
        LanguageLevel level = moduleExtension == null ? null : moduleExtension.getLanguageLevel();
        if (level == null) {
            // null means "inherit the project level", which is the common case for an imported build
            LanguageLevelProjectExtension projectExtension = LanguageLevelProjectExtension.getInstance(project);
            level = projectExtension == null ? null : projectExtension.getLanguageLevel();
        }
        JavaVersion version = level == null ? null : level.toJavaVersion();
        return version == null ? 0 : version.feature;
    }

    /** A library root VirtualFile inside a jar reports {@code /abs/foo.jar!/}; strip to the real jar path. */
    private static String localPath(VirtualFile root) {
        String path = root.getPath();
        int sep = path.indexOf("!/");
        return sep >= 0 ? path.substring(0, sep) : path;
    }
}
