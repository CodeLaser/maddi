/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.bytecode.java.asm;


import org.e2immu.bytecode.java.ExpressionFactory;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
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

    public MyAnnotationVisitor(Runtime runtime,
                               TypeParameterContext typeParameterContext,
                               LocalTypeMap localTypeMap,
                               String descriptor,
                               Info.Builder<T> inspectionBuilder) {
        super(ASM9);
        this.runtime = runtime;
        this.localTypeMap = localTypeMap;
        this.inspectionBuilder = Objects.requireNonNull(inspectionBuilder);
        LOGGER.debug("My annotation visitor: {}", descriptor);

        ParameterizedTypeFactory.Result from = ParameterizedTypeFactory.from(runtime, typeParameterContext, localTypeMap,
                LocalTypeMap.LoadMode.TRIGGER, descriptor, false);
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
            Expression expression = ExpressionFactory.from(runtime, localTypeMap, value);
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
