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

package io.codelaser.maddi.cst.api.output;

import io.codelaser.maddi.cst.api.output.element.Symbol;

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
