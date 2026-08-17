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

package org.e2immu.language.cst.api.output;

import org.e2immu.language.cst.api.output.element.Symbol;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interface to hold the output that's being accumulated while printing.
 */
public interface OutputBuilder {

    default OutputBuilder addIfNotNull(OutputBuilder outputBuilder) {
        return outputBuilder != null ? add(outputBuilder) : this;
    }

    OutputBuilder add(OutputElement... outputElements);

    OutputBuilder add(OutputBuilder... outputBuilders);

    List<OutputElement> list();

    // remove the first
    void removeLast();

    Stream<OutputElement> stream();

    boolean isEmpty();

    boolean notStart();

    default String generateJavaForDebugging() {
        return list().stream().map(OutputElement::generateJavaForDebugging).collect(Collectors.joining("\n"));
    }
}
