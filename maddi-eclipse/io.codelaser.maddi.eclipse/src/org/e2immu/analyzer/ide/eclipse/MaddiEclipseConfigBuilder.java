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

package org.e2immu.analyzer.ide.eclipse;

import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the daemon's {@code AnalyzeConfig} from JDT's project model — the Eclipse analog of the IntelliJ
 * {@code MaddiConfigBuilder}. The crucial mapping is the same: <b>compiler output dirs → classpath</b>, the
 * "hot class files" maddi's parser resolves cross-source-set references against, kept fresh by JDT's
 * incremental builder. Library jars come across too; the JDK does NOT (maddi loads {@code jmods} from its
 * own configured JDK 25+, so the project's JRE container is skipped).
 */
public final class MaddiEclipseConfigBuilder {

    /** JDK modules loaded by default; mirrors the IntelliJ builder. */
    private static final List<String> DEFAULT_JMODS = List.of(
            "java.base", "java.logging", "java.xml", "java.sql", "java.naming",
            "java.desktop", "java.management", "java.net.http", "java.compiler");

    private static final String JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER";

    /**
     * @param javaProject the JDT project to analyze
     * @param jdkHome     home of the maddi JDK (25+); the analysis SDK AND the daemon's run JDK — NOT the
     *                    project's JRE, which may target an older release maddi cannot read java.base from
     */
    public AnalysisModel.AnalyzeConfig build(IJavaProject javaProject, String jdkHome) throws CoreException {
        IProject project = javaProject.getProject();

        List<AnalysisModel.SourceRoot> sources = new ArrayList<>();
        // path -> scope, deduplicated and order-preserving
        Map<String, String> classpath = new LinkedHashMap<>();

        String defaultOutput = absolute(javaProject.getOutputLocation());
        if (defaultOutput != null) classpath.putIfAbsent(defaultOutput, "compile");

        for (IClasspathEntry raw : javaProject.getRawClasspath()) {
            addEntry(javaProject, raw, defaultOutput, sources, classpath);
        }

        List<AnalysisModel.ClasspathEntry> classpathEntries = new ArrayList<>();
        classpath.forEach((path, scope) -> classpathEntries.add(new AnalysisModel.ClasspathEntry(path, scope)));

        String workingDirectory = project.getLocation() == null ? null : project.getLocation().toOSString();
        return new AnalysisModel.AnalyzeConfig(
                workingDirectory,
                jdkHome,
                "UTF-8",
                DEFAULT_JMODS,
                sources,
                classpathEntries,
                List.of(),
                true);
    }

    private void addEntry(IJavaProject javaProject, IClasspathEntry entry, String defaultOutput,
                          List<AnalysisModel.SourceRoot> sources, Map<String, String> classpath)
            throws CoreException {
        boolean test = entry.isTest();
        switch (entry.getEntryKind()) {
            case IClasspathEntry.CPE_SOURCE -> {
                String src = absolute(entry.getPath());
                if (src != null) sources.add(new AnalysisModel.SourceRoot(src, src, test));
                // a per-source output overrides the project default (e.g. test sources → bin/test)
                String out = entry.getOutputLocation() != null ? absolute(entry.getOutputLocation()) : defaultOutput;
                if (out != null) classpath.putIfAbsent(out, test ? "test" : "compile");
            }
            case IClasspathEntry.CPE_LIBRARY -> {
                String lib = absolute(entry.getPath());
                if (lib != null) classpath.putIfAbsent(lib, test ? "test" : "compile");
            }
            case IClasspathEntry.CPE_PROJECT -> {
                // a required project's OWN output dir is more hot class files
                IProject dep = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().lastSegment());
                if (dep.exists() && dep.isOpen()) {
                    IJavaProject depJava = JavaCore.create(dep);
                    String out = absolute(depJava.getOutputLocation());
                    if (out != null) classpath.putIfAbsent(out, test ? "test" : "compile");
                }
            }
            case IClasspathEntry.CPE_CONTAINER -> {
                if (JRE_CONTAINER.equals(entry.getPath().segment(0))) return; // JDK comes from jmods
                IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
                if (container != null) {
                    for (IClasspathEntry resolved : container.getClasspathEntries()) {
                        if (resolved.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                            String lib = absolute(resolved.getPath());
                            if (lib != null) classpath.putIfAbsent(lib, test ? "test" : "compile");
                        }
                    }
                }
            }
            case IClasspathEntry.CPE_VARIABLE -> {
                IClasspathEntry resolved = JavaCore.getResolvedClasspathEntry(entry);
                if (resolved != null && resolved.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    String lib = absolute(resolved.getPath());
                    if (lib != null) classpath.putIfAbsent(lib, test ? "test" : "compile");
                }
            }
            default -> {
                // nothing
            }
        }
    }

    /**
     * A JDT {@link IPath} is workspace-relative for resources inside the workspace (e.g. {@code /Proj/bin})
     * and absolute for external artifacts (an out-of-workspace jar). Resolve to a real filesystem path.
     */
    private static String absolute(IPath path) {
        if (path == null) return null;
        IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (res != null && res.getLocation() != null) return res.getLocation().toOSString();
        return path.toOSString(); // already filesystem-absolute (external jar / library)
    }
}
