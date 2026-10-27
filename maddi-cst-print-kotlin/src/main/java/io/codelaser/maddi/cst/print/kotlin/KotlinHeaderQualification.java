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
