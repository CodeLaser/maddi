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

package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Map;
import java.util.Set;

/*
Phase 1.
Single method analyzer.

Analyzes statements in a method, and tries to determine if the method is @Modified.
Computes linking.

While it could also write out method independence and parameter independence, this code sits in Phase 3.
 */
public interface MethodModAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {

        Set<MethodInfo> waitForMethods();

        Set<TypeInfo> waitForIndependenceOfTypes();

        Map<String, Integer> infoHistogram();
    }

    Output go(MethodInfo methodInfo, boolean activateCycleBreaking);
}
