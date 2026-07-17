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

import org.e2immu.analyzer.ide.client.AnalysisModel;

/**
 * Which computed annotations appear as inline hints (the gutter always shows the full set). Applied per
 * annotation using its polarity and context-default flag.
 */
public enum InlineHintsMode {
    HIDE_CONTEXT_DEFAULTS("Hide context defaults"),
    ALL("Show all"),
    POSITIVE_ONLY("Positive only (safe: @NotModified, @Immutable, …)"),
    NEGATIVE_ONLY("Negative only (@Modified, @Mutable, …)"),
    NONE("Off (gutter only)");

    private final String label;

    InlineHintsMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label; // rendered in the settings combo box
    }

    /** Whether an annotation should be shown inline under this mode. Neutral annotations are always kept. */
    public boolean shows(AnalysisModel.Annotation a) {
        return switch (this) {
            case ALL -> true;
            case NONE -> false;
            case HIDE_CONTEXT_DEFAULTS -> !a.contextDefault();
            case POSITIVE_ONLY -> !"NEGATIVE".equals(a.polarity());
            case NEGATIVE_ONLY -> !"POSITIVE".equals(a.polarity());
        };
    }
}
