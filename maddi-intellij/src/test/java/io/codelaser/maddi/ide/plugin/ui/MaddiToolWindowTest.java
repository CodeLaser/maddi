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

package io.codelaser.maddi.ide.plugin.ui;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;
import io.codelaser.maddi.ide.plugin.analysis.MaddiRunListener;

/**
 * What the tool window must say while a run is in flight, and after one that ends badly.
 * <p>
 * Each test is one thing the panel used to get wrong on screen: an error that only ever appeared in a
 * transient balloon, a mid-run display of zeroes that read as "nothing found, instantly", and no way at all to
 * see WHICH daemon was answering — the last of which cost two misdiagnoses of a stale bundled daemon as an
 * analyzer regression.
 */
public class MaddiToolWindowTest extends LightJavaCodeInsightFixtureTestCase {

    private MaddiFindingsPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new MaddiFindingsPanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
    }

    private MaddiRunListener publisher() {
        return getProject().getMessageBus().syncPublisher(MaddiRunListener.TOPIC);
    }

    /** #29: a run that fails must leave the failure on screen, not only in a balloon that fades. */
    public void testFailedRunIsVisibleInThePanel() {
        publisher().runStarted("req-1", "/tmp/install", "1d583c80d");
        publisher().runFailed("analyze", "Cannot invoke Access.isPrivate() because access() is null");

        assertTrue("the header must state the failure; got:\n" + panel.headerText(),
                panel.headerText().contains("FAILED") && panel.headerText().contains("isPrivate"));
        assertTrue("the log must keep the error; got:\n" + panel.logText(),
                panel.logText().contains("ERROR [analyze]"));
    }

    /** Mid-run the panel must report progress, and must NOT present unknown numbers as zeroes. */
    public void testRunningStateShowsProgressAndNotZeroes() {
        publisher().runStarted("req-1", "/tmp/install", "1d583c80d");
        publisher().statusUpdated("parse", "parsing 1792 type(s)");
        publisher().passCompleted(3, true, 18771);

        String header = panel.headerText();
        assertTrue("the header must say a run is in flight; got:\n" + header, header.contains("running"));
        assertTrue("the header must show the phase; got:\n" + header, header.contains("parse"));
        assertFalse("mid-run there is no elapsed time to report; got:\n" + header, header.contains("0 ms"));
        assertFalse("mid-run the finding count is unknown, not zero; got:\n" + header,
                header.contains("0 finding(s)"));
        assertTrue("the pass must reach the log; got:\n" + panel.logText(),
                panel.logText().contains("pass 3") && panel.logText().contains("18771"));
    }

    /** The daemon's identity belongs on screen: it is the only thing that tells two builds apart. */
    public void testDaemonBuildIsOnScreen() {
        publisher().runStarted("req-1", "/opt/maddi-ide-daemon", "1d583c80d");

        assertTrue("the header must name the daemon build; got:\n" + panel.headerText(),
                panel.logText().contains("1d583c80d") || panel.headerText().contains("1d583c80d"));
    }

    /** A finished run reports the numbers that DO exist by then, including the outcome. */
    public void testFinishedRunReportsOutcomeAndCounts() {
        publisher().runStarted("req-1", "/tmp/install", "1d583c80d");
        publisher().runFinished("CERTIFIED", 12, 23840, 0, 156594L);

        String header = panel.headerText();
        assertTrue("expected the counts; got:\n" + header,
                header.contains("12 finding(s)") && header.contains("23840"));
        assertTrue("expected the outcome; got:\n" + header, header.contains("CERTIFIED"));
        assertFalse("a finished run is no longer running; got:\n" + header, header.contains("running"));
    }

    /**
     * The tests above publish on the bus directly, which leaves the seam that MATTERS untested: the service
     * computes those events and publishes them through {@code invokeLater}. Driving one real service call
     * proves the panel is reachable from where the events are actually produced.
     */
    public void testServiceEventsReachThePanel() {
        MaddiAnalysisService.getInstance(getProject()).restartDaemon();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("a service-published event must reach the panel; got:\n" + panel.logText(),
                panel.logText().contains("daemon stopped"));
    }

    /** A partial parse is the one number a user can act on, so it must not be buried. */
    public void testParseErrorsAreReported() {
        publisher().runStarted("req-1", "/tmp/install", "1d583c80d");
        publisher().runFinished("UNKNOWN", 0, 1792, 52, 118000L);

        assertTrue("expected the parse-error count; got:\n" + panel.headerText(),
                panel.headerText().contains("52 file(s) did NOT parse"));
    }
}
