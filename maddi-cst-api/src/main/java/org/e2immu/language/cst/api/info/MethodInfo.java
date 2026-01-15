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

package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.util.ParSeq;

import java.util.*;
import java.util.stream.Stream;

public interface MethodInfo extends Info {
    String CONSTRUCTOR_NAME = "<init>";

    List<ParameterizedType> exceptionTypes();

    boolean isFactoryMethod();

    boolean isFinalizer();

    boolean isSyntheticArrayConstructor();

    Set<MethodModifier> methodModifiers();

    boolean noReturnValue();

    boolean explicitlyEmptyMethod();

    boolean isConstructor();

    boolean isFinal();

    boolean isOverloadOfJLOMethod();

    boolean isSynchronized();

    MethodInfo.MethodType methodType();

    TypeInfo primaryType();

    boolean isDefault();

    boolean isVoid();

    Stream<TypeReference> typesReferenced(boolean includeBody);

    boolean complexityGreaterThanCOMPLEXITY_METHOD_WITHOUT_CODE();

    default boolean hasReturnValue() {
        return !isVoid() && !isConstructor();
    }

    boolean isPostfix();

    boolean isInfix();

    boolean isAbstract();

    List<MethodInfo> translate(TranslationMap translationMap);

    void rewirePhase3(InfoMap infoMap);

    ParameterizedType typeOfParameterHandleVarargs(int index);

    /**
     * Make an exact copy of the object, but with a different method body
     *
     * @param newBody the new method body
     * @return a new MethodInfo instance
     */
    MethodInfo withMethodBody(Block newBody);

    MethodInfo withMethodType(MethodType methodType);

    MethodInfo withSynthetic(boolean synthetic);

    interface MethodType {
        boolean isCompactConstructor();

        boolean isStatic();

        boolean isConstructor();

        boolean isAbstract();

        boolean isDefault();

        boolean isStaticInitializer();
    }

    interface MissingData {
        boolean methodBody();

        boolean overrides();
    }

    String name();

    boolean isStatic();

    ParameterizedType returnType();

    Set<MethodInfo> topOfOverloadingHierarchy();

    // SAM = single abstract method
    default boolean isSAMOfStandardFunctionalInterface() {
        return this == typeInfo().singleAbstractMethod()
               && ("java.util.function".equals(typeInfo().packageName())
                   || Runnable.class.getCanonicalName().equals(typeInfo().fullyQualifiedName())
                   || typeInfo().isSynthetic());
    }
    // from inspection

    List<ParameterInfo> parameters();

    List<TypeParameter> typeParameters();

    Block methodBody();

    // from resolution

    boolean isOverloadOf(MethodInfo methodInfo);

    Set<MethodInfo> overrides();

    boolean allowsInterrupts();

    // with inspection

    boolean isPublic();

    boolean isPubliclyAccessible();

    boolean isOverloadOfJLOEquals();

    boolean isCompactConstructor();

    boolean isSyntheticConstructor();

    boolean isStaticInitializer();

    boolean isInstanceInitializer();

    // from analysis

    default boolean isModifying() {
        return !isNonModifying();
    }

    boolean isNonModifying();

    boolean isFluent();

    boolean isIdentity();

    boolean isIgnoreModification();

    MissingData missingData();

    Map<FieldInfo, Boolean> areOwnFieldsReadModified();

    // related to the commutation of methods

    Value.CommutableData commutableData();

    // related to the commutation of parameters

    Value.ParameterParSeq getParallelGroups();

    default boolean hasParallelGroups() {
        ParSeq<ParameterInfo> parSeq = getParallelGroups().parSeq();
        return parSeq != null && parSeq.containsParallels();
    }

    default List<Expression> sortAccordingToParallelGroupsAndNaturalOrder(List<Expression> parameterExpressions) {
        return getParallelGroups().parSeq().sortParallels(parameterExpressions, Comparator.naturalOrder());
    }

    // this method acts as a getter or setter for this field
    Value.FieldValue getSetField();

    // there is another method without these parameters; they can also be set with setters
    Value.GetSetEquivalent getSetEquivalents();

    Value.PostConditions postConditions();

    Value.Precondition precondition();

    /*
       Many throw and assert statements find their way into a pre- or post-condition.
       Some, however, do not. We register them here.
     */
    Value.IndicesOfEscapes indicesOfEscapesNotInPreOrPostConditions();

    Builder builder();

    boolean isPropertyNotNull();

    boolean isPropertyNullable();

    default CompilationUnit compilationUnit() {
        return typeInfo().compilationUnit();
    }

    interface Builder extends Info.Builder<Builder> {

        /**
         * Intermediate step: the fully qualified name can now be computed, because all
         * parameters are known.
         *
         * @return the builder
         */
        @Fluent
        Builder commitParameters();

        @Fluent
        Builder setMethodBody(Block block);

        @Fluent
        Builder addMethodModifier(MethodModifier methodModifier);

        /**
         * This method directly commits the builder, without any changes.
         */
        @Fluent
        Builder addAndCommitParameter(String name, ParameterizedType type);

        @Fluent
        Builder setReturnType(ParameterizedType returnType);

        ParameterInfo addParameter(String name, ParameterizedType type);

        ParameterInfo addUnnamedParameter(ParameterizedType type);

        @Fluent
        Builder addTypeParameter(TypeParameter typeParameter);

        @Fluent
        Builder addExceptionType(ParameterizedType exceptionType);

        @Fluent
        Builder addOverrides(Collection<MethodInfo> overrides);

        // used for translations
        @Fluent
        Builder addParameter(ParameterInfo parameterInfo);

        @Fluent
        Builder setMissingData(MissingData missingData);

        List<ParameterInfo> parameters();

        List<TypeParameter> typeParameters();
    }

    default boolean isVarargs() {
        if (parameters().isEmpty()) return false;
        return parameters().get(parameters().size() - 1).isVarArgs();
    }
}

