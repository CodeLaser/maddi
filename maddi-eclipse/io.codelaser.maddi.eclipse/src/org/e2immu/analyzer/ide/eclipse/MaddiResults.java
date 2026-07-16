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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The single latest whole-project result, shared by the display surfaces (like the IntelliJ
 * {@code MaddiAnalysisService} cache). {@link #update} is called off the UI thread by the analysis job;
 * listeners marshal to the UI thread themselves.
 */
public final class MaddiResults {

    private static final MaddiResults INSTANCE = new MaddiResults();

    private volatile AnalysisModel.Result latest;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private MaddiResults() {
    }

    public static MaddiResults get() {
        return INSTANCE;
    }

    public void update(AnalysisModel.Result result) {
        this.latest = result;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public AnalysisModel.Result latest() {
        return latest;
    }

    public List<AnalysisModel.Finding> findings() {
        AnalysisModel.Result r = latest;
        return r == null ? List.of() : r.findings();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
