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
