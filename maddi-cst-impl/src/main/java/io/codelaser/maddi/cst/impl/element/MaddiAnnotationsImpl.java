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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.annotation.*;
import io.codelaser.maddi.annotation.eventual.BeforeMark;
import io.codelaser.maddi.annotation.eventual.Mark;
import io.codelaser.maddi.annotation.eventual.Only;
import io.codelaser.maddi.annotation.eventual.TestMark;
import io.codelaser.maddi.annotation.method.GetSet;
import io.codelaser.maddi.annotation.rare.AllowsInterrupt;
import io.codelaser.maddi.annotation.rare.Finalizer;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.annotation.type.ExtensionClass;
import io.codelaser.maddi.annotation.type.Singleton;
import io.codelaser.maddi.annotation.type.UtilityClass;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.impl.expression.AnnotationExpressionImpl;
import io.codelaser.maddi.cst.impl.info.TypeInfoImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MaddiAnnotationsImpl {
    public final static String IMPLIED = "implied";
    public final static String ABSENT = "absent";
    public final static String AFTER = "after";
    public final static String BEFORE = "before";
    public final static String CONSTRUCTION = "construction";
    public final static String CONTENT = "content";
    public final static String CONTRACT = "contract";
    public final static String HIDDEN_CONTENT = "hc";
    public final static String INCONCLUSIVE = "inconclusive";
    public final static String HC_PARAMETERS = "hcParameters";
    public final static String VALUE = "value";
    public final static String PAR = "par";
    public final static String SEQ = "seq";
    public final static String MULTI = "multi";

    public final AnnotationExpression allowsInterrupt = create(AllowsInterrupt.class);
    public final AnnotationExpression beforeMark = create(BeforeMark.class);
    public final AnnotationExpression commutable = create(Commutable.class);
    public final AnnotationExpression container = create(Container.class);
    public final AnnotationExpression extensionClass = create(ExtensionClass.class);
    public final AnnotationExpression effectivelyFinal = create(Final.class);
    public final AnnotationExpression finalFields = create(FinalFields.class);
    public final AnnotationExpression finalizer = create(Finalizer.class);
    public final AnnotationExpression fluent = create(Fluent.class);
    public final AnnotationExpression getSet = create(GetSet.class);
    public final AnnotationExpression identity = create(Identity.class);
    public final AnnotationExpression ignoreModifications = create(IgnoreModifications.class);
    public final AnnotationExpression immutable = create(Immutable.class);
    public final AnnotationExpression immutableContainer = create(ImmutableContainer.class);
    public final AnnotationExpression independent = create(Independent.class);
    public final AnnotationExpression mark = create(Mark.class);
    public final AnnotationExpression modified = create(Modified.class);
    public final AnnotationExpression notModified = create(NotModified.class);
    public final AnnotationExpression notNull = create(NotNull.class);
    public final AnnotationExpression nullable = create(Nullable.class);
    public final AnnotationExpression only = create(Only.class);
    public final AnnotationExpression singleton = create(Singleton.class);
    public final AnnotationExpression testMark = create(TestMark.class);
    public final AnnotationExpression unmodifiableView = create(UnmodifiableView.class);
    public final AnnotationExpression utilityClass = create(UtilityClass.class);

    @ImmutableContainer // result of Map.copyOf
    private final Map<String, AnnotationExpression> annotationTypes;

    public MaddiAnnotationsImpl() {
        Map<String, AnnotationExpression> builder = new HashMap<>();
        add(builder, allowsInterrupt, beforeMark, commutable, container, independent,
                immutableContainer, extensionClass, finalFields, getSet, immutable,
                effectivelyFinal, fluent, finalizer, identity, ignoreModifications, mark, modified);
        add(builder, notModified, notNull, nullable, only, singleton, testMark, unmodifiableView, utilityClass);
        annotationTypes = Map.copyOf(builder);
    }

    private static void add(Map<String, AnnotationExpression> builder, AnnotationExpression... aes) {
        for (AnnotationExpression ae : aes) {
            builder.put(ae.typeInfo().fullyQualifiedName(), ae);
        }
    }

    /**
     * create an annotation for a given class, without parameters (contract=false, absent=false)
     *
     * @param clazz must have a method called type of Enum type AnnotationType
     * @return an annotation expression
     */
    private AnnotationExpression create(Class<?> clazz) {
        CompilationUnit cu = new CompilationUnitImpl.Builder().setPackageName(clazz.getPackageName()).build();
        return new AnnotationExpressionImpl(List.of(), null, new TypeInfoImpl(cu, clazz.getSimpleName()), List.of());
    }

    public AnnotationExpression get(String name) {
        return Objects.requireNonNull(annotationTypes.get(name), name);
    }

    public Stream<AnnotationExpression> streamTypes() {
        return annotationTypes.values().stream();
    }
}
