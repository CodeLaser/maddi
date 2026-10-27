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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.impl.analysis.ForwardingNullabilityFacts;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

import java.util.Objects;
import java.util.function.Predicate;

/** {@link CSharpFacts#of}: the nullability facts as given, and the modification analysis's {@code unmodified}. */
final class ComposedFacts extends ForwardingNullabilityFacts implements CSharpFacts {

    private final Predicate<ParameterInfo> unmodified;

    ComposedFacts(NullabilityFacts nullability, Predicate<ParameterInfo> unmodified) {
        super(nullability);
        this.unmodified = Objects.requireNonNull(unmodified);
    }

    @Override
    public boolean unmodified(ParameterInfo parameter) {
        return unmodified.test(parameter);
    }
}
