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

package org.e2immu.analyzer.aapi.parser;

import java.util.List;

public interface AnalysisHintsConfiguration {

    // use case 1: input for normal analyzer, and for use cases 2, 3

    List<String> preloadAnalysisResultsDirs();

    // use case 2: read analysis hints, write analysis results

    String analysisResultsTargetDir();

    // use case 3: read source code or byte code, write analysis hints skeleton

    String updatedHintsDir();

    List<String> hintsPackages();

    String updatedHintsPackage();
}
