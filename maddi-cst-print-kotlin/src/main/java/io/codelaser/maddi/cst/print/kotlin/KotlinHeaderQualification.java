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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.output.TypeNameRequired;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;

/**
 * The qualification of a class header: its type parameters' bounds and its supertypes. Kotlin resolves them outside
 * the class's own scope, so a type nested in the class is named from the primary type there:
 * {@code class MapMakerInternalMap<K, V, E : MapMakerInternalMap.InternalEntry<K, V, E>, …>} (guava), where Java
 * writes {@code InternalEntry}. Everything else is the body's qualification.
 */
record KotlinHeaderQualification(Qualification inside, TypeInfo declared) implements Qualification {

    @Override
    public TypeNameRequired qualifierRequired(TypeInfo typeInfo) {
        if (typeInfo != declared && !typeInfo.isPrimaryType()
            && declared.recursiveSubTypeStream().anyMatch(t -> t == typeInfo)) {
            return TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE;
        }
        return inside.qualifierRequired(typeInfo);
    }

    @Override
    public boolean doNotQualifyImplicit() {
        return inside.doNotQualifyImplicit();
    }

    @Override
    public boolean isFullyQualifiedNames() {
        return inside.isFullyQualifiedNames();
    }

    @Override
    public boolean isSimpleOnly() {
        return inside.isSimpleOnly();
    }

    @Override
    public boolean qualifierRequired(MethodInfo methodInfo) {
        return inside.qualifierRequired(methodInfo);
    }

    @Override
    public boolean qualifierRequired(Variable variable) {
        return inside.qualifierRequired(variable);
    }

    @Override
    public TypeNameRequired typeNameRequired() {
        return inside.typeNameRequired();
    }

    @Override
    public Decorator decorator() {
        return inside.decorator();
    }

    @Override
    public void addField(FieldInfo fieldInfo) {
        inside.addField(fieldInfo);
    }

    @Override
    public void addUnqualifiedType(TypeInfo typeInfo) {
        inside.addUnqualifiedType(typeInfo);
    }

    @Override
    public void addThis(This thisVar) {
        inside.addThis(thisVar);
    }

    @Override
    public void addMethodUnlessOverride(MethodInfo methodInfo) {
        inside.addMethodUnlessOverride(methodInfo);
    }
}
