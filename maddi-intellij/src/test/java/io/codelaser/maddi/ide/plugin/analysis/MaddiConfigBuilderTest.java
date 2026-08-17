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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.e2immu.analyzer.ide.client.AnalysisModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives {@link MaddiConfigBuilder} against a real (light) IntelliJ project model and asserts the mapping
 * into the daemon's {@code AnalyzeConfig}. The load-bearing behavior is <b>compiler output dirs → classpath</b>
 * (the "hot class files" maddi's parser resolves against) and <b>libraries → classpath</b>; source roots
 * carry their test/main scope across.
 */
public class MaddiConfigBuilderTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String JDK_HOME = "/opt/jdk-25";

    // Tests that add source roots / output dirs / libraries create temp VFS dirs; deleting them at teardown
    // fires VFS events into a bundled IDEA-Ultimate async listener (Vue) that throws during init. That logged
    // error is unrelated to the config builder, so swallow ONLY it and keep every other logged error fatal.
    private AccessToken loggedErrorGuard;

    private static final LoggedErrorProcessor SWALLOW_BUNDLED_ASYNC_LISTENER = new LoggedErrorProcessor() {
        @Override
        public Set<Action> processError(String category, String message, String[] details, Throwable t) {
            return fromVueListener(t) ? Action.NONE : Action.ALL;
        }
    };

    private static boolean fromVueListener(Throwable t) {
        for (Throwable e = t; e != null; e = e.getCause()) {
            if (e.getClass().getName().contains("Vue")) return true;
            for (StackTraceElement s : e.getStackTrace()) {
                if (s.getClassName().contains("Vue")) return true;
            }
        }
        return false;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        loggedErrorGuard = LoggedErrorProcessor.executeWith(SWALLOW_BUNDLED_ASYNC_LISTENER);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown(); // deletes temp roots → fires the bundled listener; guard still installed
        } finally {
            if (loggedErrorGuard != null) loggedErrorGuard.finish();
        }
    }

    private AnalysisModel.AnalyzeConfig build() {
        return ReadAction.compute(() -> new MaddiConfigBuilder().build(getProject(), JDK_HOME, false));
    }

    private Module module() {
        return ModuleManager.getInstance(getProject()).getModules()[0];
    }

    /** path -> scope over the classpath entries, so tests can look an entry up directly. */
    private static Map<String, String> classpath(AnalysisModel.AnalyzeConfig config) {
        Map<String, String> map = new LinkedHashMap<>();
        for (AnalysisModel.ClasspathEntry e : config.classpath()) map.put(e.path(), e.scope());
        return map;
    }

    /** Scalar fields come straight through; the maddi JDK is the analysis SDK, not the project SDK. */
    public void testStructuralFields() {
        AnalysisModel.AnalyzeConfig config = build();
        assertEquals(getProject().getBasePath(), config.workingDirectory());
        assertEquals(JDK_HOME, config.sdkHome());
        assertEquals("UTF-8", config.sourceEncoding());
        assertTrue("java.base must always be loaded", config.jmods().contains("java.base"));
        assertTrue("common enterprise jmods expected", config.jmods().contains("java.sql"));
        assertTrue("whole-project analysis runs in parallel", config.parallel());
        assertTrue("no package restriction for whole-project", config.restrictToPackages().isEmpty());
    }

    /** The module's source root is reported as a main (non-test) source. */
    public void testMainSourceRootIsPresent() {
        AnalysisModel.AnalyzeConfig config = build();
        assertFalse("the light module has a source root", config.sources().isEmpty());
        assertTrue("the source root is a main source (test=false)",
                config.sources().stream().anyMatch(s -> !s.test()));
        // name == path for IntelliJ-derived roots (the daemon keys source sets by path)
        assertTrue(config.sources().stream().allMatch(s -> s.name().equals(s.path())));
    }

    /** A test source root is carried across with test=true. */
    public void testTestSourceRootIsScopedAsTest() throws Exception {
        VirtualFile testDir = myFixture.getTempDirFixture().findOrCreateDir("tst");
        PsiTestUtil.addSourceRoot(module(), testDir, true);

        AnalysisModel.AnalyzeConfig config = build();
        assertTrue("the added test source root should be reported with test=true",
                config.sources().stream().anyMatch(s -> s.test() && s.path().equals(testDir.getPath())));
    }

    /** Compiler output dirs — the hot class files — become classpath entries, main as compile, tests as test. */
    public void testCompilerOutputBecomesClasspath() throws Exception {
        VirtualFile out = myFixture.getTempDirFixture().findOrCreateDir("out");
        VirtualFile testOut = myFixture.getTempDirFixture().findOrCreateDir("out-test");
        PsiTestUtil.setCompilerOutputPath(module(), out.getUrl(), false);
        PsiTestUtil.setCompilerOutputPath(module(), testOut.getUrl(), true);

        Map<String, String> classpath = classpath(build());
        assertEquals("main output is a compile-scope classpath entry", "compile", classpath.get(out.getPath()));
        assertEquals("test output is a test-scope classpath entry", "test", classpath.get(testOut.getPath()));
    }

    /** Library class roots land on the classpath as compile-scope entries. */
    public void testLibraryClassesOnClasspath() throws Exception {
        VirtualFile libClasses = myFixture.getTempDirFixture().findOrCreateDir("lib-classes");
        PsiTestUtil.addProjectLibrary(module(), "maddi-test-lib", libClasses);

        Map<String, String> classpath = classpath(build());
        assertEquals("library classes should be a compile-scope classpath entry",
                "compile", classpath.get(libClasses.getPath()));
    }
}
