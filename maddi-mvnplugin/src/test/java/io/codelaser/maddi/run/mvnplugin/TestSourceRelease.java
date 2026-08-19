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

package io.codelaser.maddi.run.mvnplugin;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Where the module states the Java API level it is compiled against.
 *
 * <p>⛔ Getting this wrong is not a cosmetic loss: the parse then runs on whatever JDK maddi happens to be, and
 * every API removed since the target release reads as "cannot find symbol", drops the compilation unit, and can
 * cost the whole {@code ParseResult}.
 */
public class TestSourceRelease {

    /**
     * ⛔⛔ THE PLUGIN'S OWN {@code <configuration>}, WHICH IS THE MAJORITY SPELLING.
     *
     * <p>⚠ COUNTED over the corpus (2026-08-19), poms setting the property against poms putting it in
     * {@code maven-compiler-plugin}'s configuration: activemq 0/8, guava 0/10, jenkins 0/1, camel 12/19. On four
     * of six projects the property is never used at all, so a properties-only reader abstains on the entire
     * project.
     */
    @Test
    public void readsThePluginConfiguration() {
        MavenProject project = project(compilerPlugin(configuration("release", "17")), null);
        assertEquals(17, ComputeSourceSets.sourceRelease(project, false));
        assertEquals(17, ComputeSourceSets.sourceRelease(project, true), "test falls back to the main release");
    }

    /**
     * A multi-module build normally states this once, in the parent's {@code <pluginManagement>}; the module's
     * own {@code <plugins>} then need not mention the compiler plugin at all, and the lifecycle-injected
     * execution still picks the managed configuration up.
     */
    @Test
    public void readsPluginManagement() {
        MavenProject project = project(null, compilerPlugin(configuration("source", "1.8")));
        assertEquals(8, ComputeSourceSets.sourceRelease(project, false), "the 1.N spelling stops at 1.8");
    }

    /** An execution may carry its own, and Maven merges executions over the plugin-level block. */
    @Test
    public void readsAnExecutionConfiguration() {
        Plugin plugin = compilerPlugin(null);
        PluginExecution execution = new PluginExecution();
        execution.setId("default-testCompile");
        execution.setConfiguration(configuration("testRelease", "21"));
        plugin.setExecutions(List.of(execution));

        MavenProject project = project(plugin, null);
        assertEquals(21, ComputeSourceSets.sourceRelease(project, true));
        assertEquals(0, ComputeSourceSets.sourceRelease(project, false),
                "testRelease says nothing about main, and a guess is worse than an abstention");
    }

    /**
     * ⚠ Plugin configuration is NOT interpolated in the effective model -- Maven resolves the expression when it
     * injects the parameter into the mojo, which never happens for a plugin we only read about. Unresolved, the
     * value is the literal {@code ${java.version}} and {@code parseRelease} answers 0.
     */
    @Test
    public void resolvesAPropertyExpression() {
        MavenProject project = project(compilerPlugin(configuration("release", "${java.version}")), null);
        project.getProperties().setProperty("java.version", "17");
        assertEquals(17, ComputeSourceSets.sourceRelease(project, false));
    }

    /**
     * ⛔⛔ A CUSTOM EXECUTION IS NOT THE MODULE'S RELEASE.
     *
     * <p>⚠ MEASURED on activemq (2026-08-19): {@code activemq-broker} declares an execution
     * {@code java24-compile} compiling {@code src/main/java24} into a multi-release jar at
     * {@code <release>24</release>}, and nothing else. Reading the first execution found gave the whole module
     * release 24 -- a WRONG release, which is worse than the abstention it replaced. Nothing but a corpus run
     * would have shown it: the shape is perfectly ordinary Maven.
     */
    @Test
    public void ignoresAnExecutionThatIsNotTheLifecycleCompile() {
        Plugin plugin = compilerPlugin(null);
        PluginExecution mrjar = new PluginExecution();
        mrjar.setId("java24-compile");
        mrjar.setConfiguration(configuration("release", "24"));
        plugin.setExecutions(List.of(mrjar));

        assertEquals(0, ComputeSourceSets.sourceRelease(project(plugin, null), false),
                "src/main/java24 is a different compilation; the module says nothing about its own");
    }

    /**
     * Configuration over property, which is {@code maven-compiler-plugin}'s own precedence: the property supplies
     * the DEFAULT for its {@code release} parameter, and an explicit {@code <release>} overrides it.
     */
    @Test
    public void configurationBeatsTheProperty() {
        MavenProject project = project(compilerPlugin(configuration("release", "17")), null);
        project.getProperties().setProperty("maven.compiler.release", "11");
        assertEquals(17, ComputeSourceSets.sourceRelease(project, false));
    }

    /** The property still answers when nothing else does -- langchain4j, and 12 of camel's poms. */
    @Test
    public void fallsBackToTheProperty() {
        MavenProject project = project(null, null);
        project.getProperties().setProperty("maven.compiler.release", "17");
        assertEquals(17, ComputeSourceSets.sourceRelease(project, false));
    }

    /** ⛔ AND ABSTAINS WHEN THE MODEL SAYS NOTHING. A wrong release is worse than none. */
    @Test
    public void abstainsWhenNothingIsStated() {
        assertEquals(0, ComputeSourceSets.sourceRelease(project(null, null), false));
    }

    // ------------------------------------------------------------------ fixture

    private static MavenProject project(Plugin inPlugins, Plugin inPluginManagement) {
        Build build = new Build();
        if (inPlugins != null) build.setPlugins(List.of(inPlugins));
        if (inPluginManagement != null) {
            PluginManagement management = new PluginManagement();
            management.setPlugins(List.of(inPluginManagement));
            build.setPluginManagement(management);
        }
        Model model = new Model();
        model.setBuild(build);
        return new MavenProject(model);
    }

    private static Plugin compilerPlugin(Xpp3Dom configuration) {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setConfiguration(configuration);
        return plugin;
    }

    private static Xpp3Dom configuration(String key, String value) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom child = new Xpp3Dom(key);
        child.setValue(value);
        configuration.addChild(child);
        return configuration;
    }
}
