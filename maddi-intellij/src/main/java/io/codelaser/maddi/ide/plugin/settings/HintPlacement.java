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

/**
 * Where a declaration's computed annotations are drawn.
 * <p>
 * {@link #ABOVE_DECLARATION} is the default because it reads like hand-written annotated API source — the
 * form these annotations take when someone writes them out — and because a full list
 * ({@code @ImmutableContainer(hc=true) @Independent}) is long enough that wedging it in front of
 * {@code public class Foo} pushes the declaration off to the right.
 * <p>
 * Parameters are always inline regardless: a parameter's annotation belongs next to the parameter, and
 * there is no sensible line of its own to put it on.
 */
public enum HintPlacement {

    /** On their own line above the declaration, indented to match it. */
    ABOVE_DECLARATION("Above the declaration"),

    /** Immediately after the declaration's name, on the same line. */
    INLINE("Inline, after the name");

    private final String label;

    HintPlacement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
