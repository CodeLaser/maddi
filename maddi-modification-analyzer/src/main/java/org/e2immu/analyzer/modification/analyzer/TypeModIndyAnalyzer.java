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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Map;
import java.util.Set;

/*
Phase 3:

Given the modification and linking of methods and fields,
compute independence of methods, fields, parameters, and primary type,
and forward the modification of fields to the parameters linked to it.
 */
public interface TypeModIndyAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {
        boolean resolvedInternalCycles();

        Map<MethodInfo, Set<MethodInfo>> waitForMethodModifications();

        Map<MethodInfo, Set<TypeInfo>> waitForTypeIndependence();

    }

    Output go(TypeInfo primaryType, Map<MethodInfo, Set<MethodInfo>> methodsWaitFor, boolean cycleBreakingActive);
}
