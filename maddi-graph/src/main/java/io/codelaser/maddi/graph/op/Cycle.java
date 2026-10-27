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

package io.codelaser.maddi.graph.op;

import io.codelaser.maddi.graph.V;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Cycle<T>(Set<V<T>> vertices) {
    public int size() {
        return vertices().size();
    }

    @Override
    public String toString() {
        return vertices.stream().map(V::toString).sorted().collect(Collectors.joining(", "));
    }

    public Stream<T> sortedStream(Comparator<T> comparator) {
        return vertices.stream().map(V::t).sorted(comparator);
    }

    public T first(Comparator<T> comparator) {
        return vertices().stream().map(V::t).min(comparator).orElseThrow();
    }

    public boolean contains(V<T> v) {
        return vertices.contains(v);
    }
}
