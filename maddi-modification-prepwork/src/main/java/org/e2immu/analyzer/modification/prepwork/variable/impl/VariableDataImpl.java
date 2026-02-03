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

package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Stream;

public class VariableDataImpl implements VariableData {
    public static final PropertyImpl VARIABLE_DATA = new PropertyImpl("variableData",
            new VariableDataImpl(new LinkedHashMap<>()));

    public static class Builder implements VariableData {
        // we employ a linkedHashMap to keep the order of creation, with this first, then fields, then parameters,
        // followed by local variables
        private final LinkedHashMap<String, VariableInfoContainer> vicByFqn = new LinkedHashMap<>();

        @Override
        public Set<String> knownVariableNames() {
            return Set.copyOf(vicByFqn.keySet());
        }

        public boolean isKnown(String fqn) {
            return vicByFqn.containsKey(fqn);
        }

        @Override
        public boolean isDefault() {
            return vicByFqn.isEmpty();
        }

        public void put(Variable v, VariableInfoContainer vic) {
            vicByFqn.put(v.fullyQualifiedName(), vic);
        }

        public VariableData build() {
            return new VariableDataImpl(vicByFqn);
        }

        @Override
        public VariableInfoContainer variableInfoContainerOrNull(String fullyQualifiedName) {
            return vicByFqn.get(fullyQualifiedName);
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            throw new UnsupportedOperationException("Builder cannot be encoded, must be built first");
        }

        @Override
        public Stream<VariableInfoContainer> variableInfoContainerStream() {
            return vicByFqn.values().stream();
        }
    }

    private final SequencedMap<String, VariableInfoContainer> vicByFqn;

    private VariableDataImpl(LinkedHashMap<String, VariableInfoContainer> map) {
        this.vicByFqn = Collections.unmodifiableSequencedMap(map);
    }

    @Override
    public boolean isDefault() {
        return vicByFqn.isEmpty();
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        return null;// not yet streamed
    }

    @Override
    public boolean isKnown(String fullyQualifiedName) {
        return vicByFqn.containsKey(fullyQualifiedName);
    }

    @Override
    public VariableInfoContainer variableInfoContainerOrNull(String fullyQualifiedName) {
        return vicByFqn.get(fullyQualifiedName);
    }

    @Override
    public Stream<VariableInfoContainer> variableInfoContainerStream() {
        return vicByFqn.values().stream();
    }

    @Override
    public Set<String> knownVariableNames() {
        return vicByFqn.keySet();
    }

    public static VariableData of(Element element) {
        return element.analysis().getOrNull(VARIABLE_DATA, VariableDataImpl.class);
    }
}
