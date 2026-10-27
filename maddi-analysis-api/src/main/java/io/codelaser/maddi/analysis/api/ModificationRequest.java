/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
