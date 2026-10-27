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

package io.codelaser.maddi.bytecode.java.asm;


import io.codelaser.maddi.bytecode.java.ExpressionFactory;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.resource.ByteCodeInspector;
import org.objectweb.asm.AnnotationVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.objectweb.asm.Opcodes.ASM9;


public class MyAnnotationVisitor<T extends Info.Builder<? extends Info.Builder<T>>> extends AnnotationVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyAnnotationVisitor.class);

    private final Runtime runtime;
    private final LocalTypeMap localTypeMap;
    private final Info.Builder<T> inspectionBuilder;
    private final AnnotationExpression.Builder expressionBuilder;
    private final SourceSet sourceSetOfRequest;
    private final SourceSet nearestSourceSet;

    public MyAnnotationVisitor(Runtime runtime,
                               SourceSet sourceSetOfRequest,
                               SourceSet nearestSourceSet,
                               ByteCodeInspector.TypeParameterContext typeParameterContext,
                               LocalTypeMap localTypeMap,
                               String descriptor,
                               Info.Builder<T> inspectionBuilder) {
        super(ASM9);
        this.runtime = runtime;
        this.localTypeMap = localTypeMap;
        this.inspectionBuilder = Objects.requireNonNull(inspectionBuilder);
        this.sourceSetOfRequest = sourceSetOfRequest;
        this.nearestSourceSet = nearestSourceSet;

        LOGGER.debug("My annotation visitor: {}", descriptor);

        ParameterizedTypeFactory.Result from = ParameterizedTypeFactory.from(runtime, sourceSetOfRequest,
                nearestSourceSet, typeParameterContext,
                localTypeMap, LocalTypeMap.LoadMode.TRIGGER, descriptor, false);
        if (from == null) {
            expressionBuilder = null;
        } else {
            ParameterizedType type = from.parameterizedType;
            expressionBuilder = runtime.newAnnotationExpressionBuilder().setTypeInfo(type.typeInfo());
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        LOGGER.debug("Annotation again: {}, {}", name, descriptor);
        return null;
    }

    @Override
    public void visit(String name, Object value) {
        if (expressionBuilder != null) {
            LOGGER.debug("Assignment: {} to {}", name, value);
            Expression expression = ExpressionFactory.from(runtime, sourceSetOfRequest, nearestSourceSet,
                    localTypeMap, value);
            if (!expression.isEmpty()) {
                expressionBuilder.addKeyValuePair(name, expression);
            } else {
                LOGGER.warn("Ignoring unparsed annotation expression of type {}", value.getClass());
            }
        }// else: jdk/ annotation
    }

    @Override
    public void visitEnd() {
        if (expressionBuilder != null) {
            inspectionBuilder.addAnnotation(expressionBuilder.build());
        } // else: jdk/ annotation
    }
}
