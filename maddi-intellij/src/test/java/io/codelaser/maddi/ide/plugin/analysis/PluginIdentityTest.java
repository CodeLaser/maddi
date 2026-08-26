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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin's identity on JetBrains Marketplace, guarded mechanically because none of it can be corrected
 * after a public release: the id "cannot be changed later after public release", and a logo is required at
 * upload. See {@code PUBLISHING.md}, "Package 2 — the IDE plugins".
 */
public class PluginIdentityTest {

    /**
     * ⛔ THE TWO MUST NOT DRIFT. {@code MaddiAnalysisService.resolveInstallDir} asks
     * {@code PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))} where the plugin was installed, so that
     * it can launch the daemon bundled at {@code <plugin>/daemon}. A constant that no longer matches
     * {@code plugin.xml} yields a null descriptor, i.e. "no bundled daemon" — which reads as a packaging
     * problem, not as a typo.
     */
    @DisplayName("the constant the daemon is located with equals the descriptor's id")
    @Test
    public void constantMatchesDescriptor() {
        assertEquals(idInPluginXml(), MaddiAnalysisService.PLUGIN_ID);
    }

    /**
     * The Marketplace's own plugin-structure library refuses an id containing "intellij"
     * ({@code verifyPluginStructure} reports it), and the id is immutable after the first upload — so this
     * is a release blocker that only shows up at upload time.
     */
    @DisplayName("the id carries no word the Marketplace refuses")
    @Test
    public void idIsAcceptableToTheMarketplace() {
        String id = idInPluginXml();
        assertFalse(id.toLowerCase().contains("intellij"), "the id must not contain 'intellij': " + id);
        assertFalse(id.toLowerCase().contains("jetbrains"), "the id must not contain 'jetbrains': " + id);
    }

    /** A 40x40 SVG logo is required at upload, and must not be the IntelliJ template's default. */
    @DisplayName("both theme variants of the logo ship in the jar")
    @Test
    public void logoIsBundled() {
        assertNotNull(PluginIdentityTest.class.getResource("/META-INF/pluginIcon.svg"));
        assertNotNull(PluginIdentityTest.class.getResource("/META-INF/pluginIcon_dark.svg"));
    }

    private static String idInPluginXml() {
        String xml = resource("/META-INF/plugin.xml");
        Matcher m = Pattern.compile("<id>([^<]+)</id>").matcher(xml);
        assertTrue(m.find(), "plugin.xml declares no <id>");
        return m.group(1).trim();
    }

    private static String resource(String path) {
        try (InputStream in = PluginIdentityTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is not on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
