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

package org.e2immu.analyzer.ide.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Application-level persisted settings. The most important field is {@link State#jdkHome}: maddi needs a
 * <b>JDK 25+</b> both to run the daemon and to read {@code java.base} (a JBR 21 / older SDK triggers
 * internal failures), so this is a dedicated "maddi JDK", not the analyzed project's SDK (which may
 * target 17/21). The settings UI ({@code Configurable}) is added in M4.
 */
@Service(Service.Level.APP)
@State(name = "MaddiSettings", storages = @Storage("maddi.xml"))
public final class MaddiSettings implements PersistentStateComponent<MaddiSettings.State> {

    public static final class State {
        /** Home of a JDK 25+ to run the daemon on and to analyze against. Required. */
        public String jdkHome = "";
        /** Override for the daemon distribution dir; empty = use the copy bundled in the plugin. */
        public String daemonInstallDir = "";
        /** Max heap for the daemon JVM, in MB. */
        public int daemonXmxMb = 4096;
        public boolean autoAnalyzeOnBuild = true;
        public boolean showGuardFindings = true;
        public InlineHintsMode inlineHintsMode = InlineHintsMode.HIDE_CONTEXT_DEFAULTS;
        public HintPlacement hintPlacement = HintPlacement.ABOVE_DECLARATION;
        public boolean showGutterIcons = true;
        /** Advisory "one member away from @Container/@Immutable/…" warnings; off, as on the CLI. */
        public boolean warnNearMisses = false;
    }

    private State state = new State();

    public static MaddiSettings getInstance() {
        return ApplicationManager.getApplication().getService(MaddiSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State toLoad) {
        XmlSerializerUtil.copyBean(toLoad, this.state);
    }
}
