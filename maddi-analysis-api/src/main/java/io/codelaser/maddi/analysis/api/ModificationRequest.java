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

package io.codelaser.maddi.analysis.api;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;

import java.util.Collection;
import java.util.List;

/**
 * @param callGraph    null: analyze the order without the graph (no worklist narrowing)
 * @param primaryTypes what the environment gates' incremental state is computed over; may be empty otherwise
 * @param valueFeed    null: none
 */
public record ModificationRequest(JavaInspector javaInspector,
                                  List<Info> order,
                                  G<Info> callGraph,
                                  Collection<TypeInfo> primaryTypes,
                                  ModificationOptions options,
                                  AnalysisValueFeed valueFeed) {
}
