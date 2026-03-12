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

package org.e2immu.language.cst.impl.variable;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.OutputElement;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.TypeNameRequired;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.*;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.e2immu.language.cst.api.element.Element.TypeReferenceNature.*;

public class ThisImpl extends VariableImpl implements This {

    private final boolean writeSuper;
    private final TypeInfo explicitlyWriteType;
    private final String fullyQualifiedName;

    public ThisImpl(ParameterizedType parameterizedType) {
        this(parameterizedType, null, false);
    }

    public ThisImpl(ParameterizedType parameterizedType, TypeInfo explicitlyWriteType, boolean writeSuper) {
        super(parameterizedType);
        this.writeSuper = writeSuper;
        this.explicitlyWriteType = explicitlyWriteType;
        this.fullyQualifiedName = parameterizedType.typeInfo().fullyQualifiedName() + ".this";
    }

    @Override
    public TypeInfo typeInfo() {
        return parameterizedType().typeInfo();
    }

    @Override
    public TypeInfo explicitlyWriteType() {
        return explicitlyWriteType;
    }

    @Override
    public boolean writeSuper() {
        return writeSuper;
    }

    @Override
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public String simpleName() {
        String superOrThis = writeSuper ? "super" : "this";
        if (explicitlyWriteType != null) return explicitlyWriteType.simpleName() + "." + superOrThis;
        return superOrThis;
    }

    @Override
    public int complexity() {
        return 1;
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
    public OutputBuilder print(Qualification qualification) {
        String name = writeSuper ? "super" : "this";
        OutputElement oe;
        if (qualification.qualifierRequired(this) || explicitlyWriteType != null) {
            TypeInfo ti = explicitlyWriteType == null ? typeInfo() : explicitlyWriteType;
            TypeNameRequired typeNameRequired = qualification.qualifierRequired(ti);
            oe = new QualifiedNameImpl(name,
                    // what should the qualifier look like?
                    TypeNameImpl.typeName(ti, typeNameRequired, false),
                    // shall we write the qualifier?
                    QualifiedNameImpl.Required.YES);
        } else {
            oe = new TextImpl(name);
        }
        return new OutputBuilderImpl().add(oe);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> test, DetailedSources detailedSources) {
        if (explicitlyWriteType == null) {
            return Stream.of(new ElementImpl.TypeReference(typeInfo(), IMPLICIT));
        }
        TypeReferenceNature nature = detailedSources != null && detailedSources.isFullyQualified(parameterizedType())
                ? FULLY_QUALIFIED : EXPLICIT;
        return Stream.of(new ElementImpl.TypeReference(typeInfo(), IMPLICIT),
                new ElementImpl.TypeReference(explicitlyWriteType, nature));
    }

    @Override
    public Variable rewire(InfoMap infoMap) {
        return new ThisImpl(parameterizedType().rewire(infoMap),
                explicitlyWriteType == null ? null : infoMap.typeInfo(explicitlyWriteType),
                writeSuper);
    }
}
