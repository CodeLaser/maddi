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

import com.intellij.util.messages.Topic;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.jetbrains.annotations.NotNull;

/** Fired on the project message bus whenever a fresh analysis result is available (tool window etc. subscribe). */
public interface MaddiResultListener {
    Topic<MaddiResultListener> TOPIC = Topic.create("maddi analysis result", MaddiResultListener.class);

    void resultUpdated(@NotNull AnalysisModel.Result result);
}
