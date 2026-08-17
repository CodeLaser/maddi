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

package org.e2immu.analyzer.ide.plugin.analysis;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the daemon's {@code AnalyzeConfig} from IntelliJ's project model. The crucial mapping is
 * <b>compiler output dirs → classpath</b>: those are the "hot class files" maddi's parser needs to
 * resolve cross-source-set references; IntelliJ's incremental compiler keeps them fresh.
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
        List<AnalysisModel.SourceRoot> sources = new ArrayList<>();
        // path -> scope, deduplicated and order-preserving
        Map<String, String> classpath = new LinkedHashMap<>();

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager roots = ModuleRootManager.getInstance(module);
            for (VirtualFile root : roots.getSourceRoots(JavaSourceRootType.SOURCE)) {
                sources.add(new AnalysisModel.SourceRoot(root.getPath(), root.getPath(), false));
            }
            for (VirtualFile root : roots.getSourceRoots(JavaSourceRootType.TEST_SOURCE)) {
                sources.add(new AnalysisModel.SourceRoot(root.getPath(), root.getPath(), true));
            }
            CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
            if (cme != null) {
                VirtualFile out = cme.getCompilerOutputPath();
                if (out != null) classpath.putIfAbsent(out.getPath(), "compile");
                VirtualFile testOut = cme.getCompilerOutputPathForTests();
                if (testOut != null) classpath.putIfAbsent(testOut.getPath(), "test");
            }
        }

        // library jars (and any classes dirs) across the whole project
        for (VirtualFile root : OrderEnumerator.orderEntries(project).librariesOnly().classes().getRoots()) {
            classpath.putIfAbsent(localPath(root), "compile");
        }

        List<AnalysisModel.ClasspathEntry> classpathEntries = new ArrayList<>();
        classpath.forEach((path, scope) -> classpathEntries.add(new AnalysisModel.ClasspathEntry(path, scope)));

        return new AnalysisModel.AnalyzeConfig(
                project.getBasePath(),
                jdkHome,
                "UTF-8",
                DEFAULT_JMODS,
                sources,
                classpathEntries,
                List.of(),
                true,
                warnNearMisses);
    }

    /** A library root VirtualFile inside a jar reports {@code /abs/foo.jar!/}; strip to the real jar path. */
    private static String localPath(VirtualFile root) {
        String path = root.getPath();
        int sep = path.indexOf("!/");
        return sep >= 0 ? path.substring(0, sep) : path;
    }
}
