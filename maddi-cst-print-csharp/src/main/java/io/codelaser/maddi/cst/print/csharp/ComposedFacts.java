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

import java.util.Objects;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.impl.analysis.ForwardingNullabilityFacts;
import io.codelaser.maddi.cst.impl.analysis.ModificationFacts;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

/** {@link CSharpFacts} of a {@link NullabilityFacts} and a {@link ModificationFacts}: see {@link CSharpFacts#of}. */
final class ComposedFacts extends ForwardingNullabilityFacts implements CSharpFacts {
    private final ModificationFacts modification;

    ComposedFacts(NullabilityFacts nullability, ModificationFacts modification) {
        super(nullability);
        this.modification = Objects.requireNonNull(modification);
    }

    @Override
    public boolean unmodified(ParameterInfo parameter) {
        return modification.unmodified(parameter);
    }

    @Override
    public boolean structurallyUnmodified(FieldInfo field) {
        return modification.structurallyUnmodified(field);
    }

    @Override
    public boolean structurallyNonModifying(MethodInfo method) {
        return modification.structurallyNonModifying(method);
    }
}
