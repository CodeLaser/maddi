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

/**
 * Which computed annotations appear as gutter hints, mirroring the IntelliJ plugin's inline-hints filter.
 * maddi's decorator collapses "baseline" and "negative polarity" onto the same boundary, so the choices are
 * two axes: <b>context-default-ness</b> (is this annotation already implied by the enclosing declaration?)
 * and <b>polarity</b> (proven-positive vs. baseline-negative).
 */
public enum HintFilter {

    /** Default: hide annotations the enclosing declaration already implies; keep the surprises. */
    HIDE_CONTEXT_DEFAULTS("Hide implied defaults"),
    ALL("Everything"),
    POSITIVE_ONLY("Only positive (immutable, not-modified, …)"),
    NEGATIVE_ONLY("Only negative (mutable, modifying, …)"),
    NONE("Nothing");

    private final String label;

    HintFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean shows(AnalysisModel.Annotation annotation) {
        return switch (this) {
            case ALL -> true;
            case NONE -> false;
            case HIDE_CONTEXT_DEFAULTS -> !annotation.contextDefault();
            case POSITIVE_ONLY -> !"NEGATIVE".equals(annotation.polarity());
            case NEGATIVE_ONLY -> !"POSITIVE".equals(annotation.polarity());
        };
    }

    /**
     * What this element should read as under this filter: the shown annotations joined, or empty for
     * "show nothing here". Shared by every surface (gutter markers, inline code minings) so one filter
     * setting means the same thing everywhere.
     */
    public String textFor(AnalysisModel.ElementAnnotation element) {
        List<AnalysisModel.Annotation> annotations = element.annotations();
        if (annotations == null || annotations.isEmpty()) {
            // no tagged annotations to filter on; fall back to the full display set unless suppressed entirely
            return this == NONE || element.displayAnnotations() == null
                    ? "" : String.join(" ", element.displayAnnotations());
        }
        StringBuilder sb = new StringBuilder();
        for (AnalysisModel.Annotation annotation : annotations) {
            if (!shows(annotation)) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(annotation.text());
        }
        return sb.toString();
    }
}
