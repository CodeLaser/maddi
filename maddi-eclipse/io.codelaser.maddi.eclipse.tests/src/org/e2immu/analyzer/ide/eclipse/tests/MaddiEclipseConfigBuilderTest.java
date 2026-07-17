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

package org.e2immu.analyzer.ide.eclipse.tests;

import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.eclipse.MaddiEclipseConfigBuilder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Live (headless) check that {@link MaddiEclipseConfigBuilder} maps a real JDT project model to an
 * {@code AnalyzeConfig}. Runs inside an Equinox runtime with a real workspace; creates a Java project with a
 * source folder + output dir, then asserts the mapping — the Eclipse analog of the IntelliJ
 * {@code MaddiConfigBuilderTest}. Confirms the plugin's JDT integration works, not just compiles.
 */
public class MaddiEclipseConfigBuilderTest {

    private static final String JDK_HOME = "/opt/jdk-25";

    @Test
    public void sourceRootAndOutputDirBecomeConfig() throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("MaddiSample");
        if (!project.exists()) project.create(null);
        project.open(null);
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[]{JavaCore.NATURE_ID});
        project.setDescription(description, null);

        IJavaProject javaProject = JavaCore.create(project);
        IFolder src = project.getFolder("src");
        if (!src.exists()) src.create(true, true, null);
        IFolder bin = project.getFolder("bin");
        if (!bin.exists()) bin.create(true, true, null);
        javaProject.setRawClasspath(new IClasspathEntry[]{JavaCore.newSourceEntry(src.getFullPath())}, null);
        javaProject.setOutputLocation(bin.getFullPath(), null);

        AnalysisModel.AnalyzeConfig config = new MaddiEclipseConfigBuilder().build(javaProject, JDK_HOME);

        assertEquals("the maddi JDK is the analysis SDK", JDK_HOME, config.sdkHome());
        assertEquals("UTF-8", config.sourceEncoding());
        assertTrue("java.base is always loaded", config.jmods().contains("java.base"));
        assertTrue("whole-project analysis runs in parallel", config.parallel());

        String srcAbs = src.getLocation().toOSString();
        assertTrue("the source folder maps to a main (non-test) source root",
                config.sources().stream().anyMatch(s -> s.path().equals(srcAbs) && !s.test()));

        String binAbs = bin.getLocation().toOSString();
        assertTrue("the compiler output dir (hot class files) is a compile-scope classpath entry",
                config.classpath().stream()
                        .anyMatch(c -> c.path().equals(binAbs) && "compile".equals(c.scope())));
    }
}
