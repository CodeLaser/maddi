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

package org.e2immu.analyzer.modification.analyzer;

import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;

import java.util.Set;

/*
Level 2:

is a field @Modified?

- we need to know the @Modified value for the field in each method of the primary type

Is a field @Independent?

- we need to know if the field links to any of the parameters of each method in the primary type.
- so we compute this linking first, then write the @Independent property if all data is present

Independence and modification of parameters is directly influenced by the values computed here,
but the actual computation for parameters is done in the next phase.

This analyzer does not concern itself with solving internal cycles.
It writes out results, if any, in the field's analysis() object.
 */
public interface FieldAnalyzer {

    void go(@Modified FieldInfo fieldInfo, boolean cycleBreakingActive);
}
