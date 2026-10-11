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

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * The library mappings of a translation, in order: the first that answers wins. A client puts its own overrides in
 * front, and leaves out the mapping of a library it does not want translated; the JDK's is normally last.
 */
public final class CSharpLibraries implements LibraryMapping {

    public static final CSharpLibraries DEFAULT = new CSharpLibraries(List.of(CSharpBcl.JDK));

    private final List<LibraryMapping> mappings;
    private final Set<String> typeNames;

    public CSharpLibraries(List<LibraryMapping> mappings) {
        this.mappings = List.copyOf(mappings);
        Set<String> names = new HashSet<>();
        this.mappings.forEach(m -> names.addAll(m.typeNames()));
        this.typeNames = Set.copyOf(names);
    }

    public static CSharpLibraries of(LibraryMapping... mappings) {
        return new CSharpLibraries(List.of(mappings));
    }

    /** These mappings, with {@code first} in front of them: its answers win. */
    public CSharpLibraries withFirst(LibraryMapping first) {
        return new CSharpLibraries(java.util.stream.Stream.concat(java.util.stream.Stream.of(first), mappings.stream())
                .toList());
    }

    public List<LibraryMapping> mappings() {
        return mappings;
    }

    @Override
    public String id() {
        return String.join(",", mappings.stream().map(LibraryMapping::id).toList());
    }

    private <T> T first(Function<LibraryMapping, T> ask) {
        return mappings.stream().map(ask).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public TypeMapping type(TypeInfo typeInfo) {
        return first(m -> m.type(typeInfo));
    }

    @Override
    public Rule call(MethodInfo method, MethodCall call) {
        return first(m -> m.call(method, call));
    }

    @Override
    public Rule constructor(MethodInfo constructor) {
        return first(m -> m.constructor(constructor));
    }

    @Override
    public FieldMapping field(FieldInfo fieldInfo) {
        return first(m -> m.field(fieldInfo));
    }

    @Override
    public PatternMapping pattern(ParameterizedType tested, ParameterizedType expression) {
        return first(m -> m.pattern(tested, expression));
    }

    @Override
    public String overrideName(MethodInfo method) {
        return first(m -> m.overrideName(method));
    }

    @Override
    public Set<String> typeNames() {
        return typeNames;
    }
}
