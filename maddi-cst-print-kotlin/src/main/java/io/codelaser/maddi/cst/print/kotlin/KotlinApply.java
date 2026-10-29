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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Construct, configure, return (CodeLaser/maddi#165):
 * <pre>
 * Config c = new Config(); c.setName(name); c.setTimeout(30); return c;
 * </pre>
 * is Kotlin's {@code return Config().apply { setName(name); setTimeout(30) }}. The block runs the same calls in the same
 * order, so neither escape nor modification matters; what does is name resolution inside the block, where the new
 * object is {@code this} and an unqualified name finds its members before the enclosing class's. So the arguments
 * read locals, parameters and other objects: no {@code this}, no field or static method of the enclosing class by its
 * bare name where the new object has a member of that name, no instance method of the enclosing class by its bare
 * name, no lambda or anonymous class (whose {@code this} and names would move too), no inner class created with the
 * enclosing instance. Locals and parameters are safe: Kotlin finds them before any receiver's
 * members.
 * <p>
 * Only for a type of the project's own, not the JDK's: {@code ArrayList} with {@code add}s is {@code mutableListOf},
 * {@code StringBuilder} is {@code buildString} (#140), and setters on domain objects are where the idiom reads best.
 * Only the form that ends in {@code return c}; a local used further stays as it is.
 */
final class KotlinApply {

    private KotlinApply() {
    }

    /** The statements from {@code start} on that make one {@code return T(…).apply { … }}, or null. */
    static List<Statement> at(List<Statement> statements, int start) {
        if (!KotlinContext.translatingJava() || KotlinContext.inLambda()) return null;
        if (!(statements.get(start) instanceof LocalVariableCreation lvc) || lvc.localVariableStream().count() != 1
            || !lvc.comments().isEmpty()) {
            return null;
        }
        LocalVariable local = lvc.localVariable();
        if (!(local.assignmentExpression() instanceof ConstructorCall cc) || cc.constructor() == null
            || cc.anonymousClass() != null || cc.arrayInitializer() != null || cc.object() != null) {
            return null;
        }
        TypeInfo type = cc.constructor().typeInfo();
        if (type == null || type.packageName() == null || type.packageName().startsWith("java")
            || type.packageName().startsWith("kotlin") || KotlinContext.reassigned(local)) {
            return null;
        }
        int i = start + 1;
        while (i < statements.size() && configures(statements.get(i), local, type)) i++;
        int calls = i - start - 1;
        if (calls < 2 || i >= statements.size()) return null;
        if (!(statements.get(i) instanceof ReturnStatement rs) || rs.goToLabel() != null || !rs.comments().isEmpty()
            || !(KotlinExpressionPrinter.unwrap(rs.expression()) instanceof VariableExpression ve)
            || !local.equals(ve.variable()) || !plainReturn()) {
            return null;
        }
        return statements.subList(start, i + 1);
    }

    // a return the Kotlin printer writes as 'return value': no generic override or type variable to cast to
    private static boolean plainReturn() {
        MethodInfo method = KotlinContext.currentMethod();
        return method != null && !method.isConstructor() && method.returnType().typeInfo() != null
               && KotlinMethodPrinter.genericOverridden(method) == null;
    }

    // 'local.m(arguments)' as a statement, with arguments that keep their meaning inside the block
    private static boolean configures(Statement statement, LocalVariable local, TypeInfo type) {
        if (!(statement instanceof ExpressionAsStatement es) || !es.comments().isEmpty()
            || !(KotlinExpressionPrinter.unwrap(es.expression()) instanceof MethodCall mc)
            || mc.objectIsImplicit() || mc.methodInfo() == null || mc.methodInfo().isStatic()
            || !(KotlinExpressionPrinter.unwrap(mc.object()) instanceof VariableExpression ve)
            || !local.equals(ve.variable())) {
            return false;
        }
        for (Expression argument : mc.parameterExpressions()) {
            if (!keepsMeaning(argument, local, type)) return false;
        }
        return true;
    }

    private static boolean keepsMeaning(Expression argument, LocalVariable local, TypeInfo type) {
        boolean[] ok = {true};
        argument.visit((Element e) -> {
            if (e instanceof Lambda || e instanceof MethodReference
                || e instanceof ConstructorCall c && (c.anonymousClass() != null
                                                      || c.constructor() != null && c.constructor().typeInfo().isInnerClass())
                || e instanceof VariableExpression v && (v.variable() instanceof This || local.equals(v.variable())
                                                         || v.variable() instanceof FieldReference fr && bare(fr)
                                                            && hasMember(type, fr.fieldInfo().name()))
                || e instanceof MethodCall m && m.objectIsImplicit()
                   && (!(m.object() instanceof io.codelaser.maddi.cst.api.expression.TypeExpression)
                       || hasMember(type, m.methodInfo().name()))) {
                ok[0] = false;
            }
            return ok[0];
        });
        return ok[0];
    }

    // a field read by its bare name: of this, or a static of the enclosing classes
    private static boolean bare(FieldReference fr) {
        if (fr.scopeIsRecursivelyThis()) return true;
        return fr.isStatic() && (fr.scope() == null || fr.scope() instanceof io.codelaser.maddi.cst.api.expression.TypeExpression te
                                                       && KotlinContext.typeInScope(te.parameterizedType().typeInfo()));
    }

    /**
     * Would the name, written bare inside the block, find a member of the new object: a field, a method, or the
     * property Kotlin makes of a Java getter? Private ones too, to be safe.
     */
    private static boolean hasMember(TypeInfo type, String name) {
        String capitalized = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(type), type.superTypesExcludingJavaLangObject().stream())
                .anyMatch(t -> t.fields().stream().anyMatch(f -> f.name().equals(name))
                               || t.methods().stream().anyMatch(m -> m.name().equals(name)
                                                                     || m.name().equals("get" + capitalized)
                                                                     || m.name().equals("is" + capitalized)));
    }

    /** {@code return T(…).apply { m(…) … }} for the statements {@link #at} found. */
    static OutputBuilder print(List<Statement> sequence, Qualification q) {
        LocalVariableCreation lvc = (LocalVariableCreation) sequence.getFirst();
        List<OutputBuilder> calls = new ArrayList<>();
        for (Statement s : sequence.subList(1, sequence.size() - 1)) {
            MethodCall mc = (MethodCall) KotlinExpressionPrinter.unwrap(((ExpressionAsStatement) s).expression());
            KotlinContext.pushStatement(s);
            try {
                calls.add(KotlinExpressionPrinter.print(mc.withObject(mc.object(), true), q));
            } finally {
                KotlinContext.popStatement();
            }
        }
        return new OutputBuilderImpl().add(KotlinKeyword.RETURN).add(SpaceEnum.ONE)
                .add(KotlinExpressionPrinter.print(lvc.localVariable().assignmentExpression(), q))
                .add(SymbolEnum.DOT).add(new TextImpl("apply")).add(SpaceEnum.ONE)
                .add(KotlinStatementPrinter.braces(calls));
    }
}
