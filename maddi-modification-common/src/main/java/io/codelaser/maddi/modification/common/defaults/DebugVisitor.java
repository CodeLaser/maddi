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

package io.codelaser.maddi.modification.common.defaults;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;

import java.util.List;
import java.util.Map;

public interface DebugVisitor {

    default void allTypes(List<TypeInfo> allTypes) {
    }

    default void dataMapAfterFieldMethodAnalyzer(Map<Element, ShallowAnalyzer.InfoData> dataMap) {
    }

    default void dataMapAfterTypeAnalyzer(Map<Element, ShallowAnalyzer.InfoData> dataMap) {
    }

    default void inputTypes(List<TypeInfo> types) {
    }

    default void sortedLinearized(List<TypeInfo> sorted) {
    }

    default void typeGraph(G<TypeInfo> typeGraph) {
    }
}
