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

package io.codelaser.maddi.parser.java.erasure;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.MethodResolution;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LambdaErasure extends ErasureExpressionImpl {
    private final Set<MethodResolution.Count> counts;

    public LambdaErasure(Runtime runtime, Set<MethodResolution.Count> counts, Source source) {
        super(runtime, source);
        Objects.requireNonNull(counts);
        Objects.requireNonNull(source);
        this.counts = counts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LambdaErasure that)) return false;
        return Objects.equals(counts, that.counts) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts, source);
    }

    @Override
    public Expression withSource(Source source) {
        return new LambdaErasure(runtime, counts, source);
    }

    @Override
    public Set<ParameterizedType> erasureTypes() {
        return counts.stream()
                .map(count -> runtime.syntheticFunctionalType(count.parameters(), !count.isVoid()))
                .map(NamedType::asParameterizedType)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Precedence precedence() {
        return runtime.precedenceBottom();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return runtime.newOutputBuilder().add(runtime.newText("<Lambda Erasure at " + source + ": " + counts + ">"));
    }

    public Set<MethodResolution.Count> counts() {
        return counts;
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException();
    }
}
