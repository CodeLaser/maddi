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

/**
 * Where a declaration's computed annotations are drawn, mirroring the IntelliJ plugin's setting of the same
 * name. {@link #ABOVE_DECLARATION} is the default: it reads like hand-written annotated API source, and a
 * full annotation list is long enough that putting it in front of {@code public class Foo} pushes the
 * declaration off to the right.
 * <p>
 * Parameters are always inline whatever this says — a parameter's annotation belongs next to the parameter.
 * <p>
 * In Eclipse the choice has a second consequence: {@link #INLINE} uses a line-content code mining, which has
 * to borrow a character's glyph box and can leave that character showing the mining's own styling
 * (Eclipse bug 531769). {@link #ABOVE_DECLARATION} uses a line-header mining and is free of it. See
 * {@link MaddiHintLineHeaderCodeMining}.
 */
public enum HintPlacement {

    ABOVE_DECLARATION("Above the declaration"),
    INLINE("Inline, at the declaration");

    private final String label;

    HintPlacement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
