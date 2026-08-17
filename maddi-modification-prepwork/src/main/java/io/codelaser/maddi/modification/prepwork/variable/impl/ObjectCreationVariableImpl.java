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

package io.codelaser.maddi.modification.prepwork.variable.impl;

import io.codelaser.maddi.modification.prepwork.variable.ObjectCreationVariable;
import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.element.SourceImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class ObjectCreationVariableImpl implements ObjectCreationVariable {
    public final ParameterizedType returnType;
    public final String index;
    public final String fqn;
    private final MethodInfo methodInfo;

    public ObjectCreationVariableImpl(MethodInfo methodInfo, String index, ParameterizedType parameterizedType) {
        this.returnType = parameterizedType;
        this.index = index;
        fqn = "oc:" + index + ":" + parameterizedType.detailedString();
        this.methodInfo = methodInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectCreationVariable that = (ObjectCreationVariable) o;
        return returnType.equals(that.parameterizedType()) &&
               fqn.equals(that.fullyQualifiedName());
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    public TypeInfo getOwningType() {
        return methodInfo.typeInfo();
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, fqn);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return returnType;
    }

    @Override
    public String simpleName() {
        return "oc:" + index;
    }

    @Override
    public String fullyQualifiedName() {
        return fqn;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(simpleName()));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return Stream.of(this);
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate, DetailedSources detailedSources) {
        return Stream.of(new ElementImpl.TypeReference(parameterizedType().typeInfo(),
                TypeReferenceNature.IMPLICIT, null));
    }

    @Override
    public String toString() {
        return simpleName();
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public Source source() {
        return SourceImpl.NO_SOURCE;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeVariable(this);
        visitor.afterVariable(this);
    }

    @Override
    public Variable rewire(InfoMapView infoMap) {
        return new ObjectCreationVariableImpl(infoMap.methodInfo(methodInfo),
                index, parameterizedType().rewire(infoMap));
    }
}
