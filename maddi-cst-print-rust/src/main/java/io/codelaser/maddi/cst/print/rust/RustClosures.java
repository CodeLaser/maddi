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


package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A JDK function returning a value is a {@code dyn Fn}: called through {@code &}, its closure only reads what it
 * captures. A closure passed for it that writes what it captures ({@code this::parse} assigning {@code this.j}, a
 * lambda adding to a field) is an {@code FnMut}, rustc's E0596 in a {@code Fn}. So the parameters of the program's own
 * methods that some call site passes such a closure are {@code dyn FnMut}, borrowed {@code &mut}.
 */
final class RustClosures {

    private RustClosures() {
    }

    /** The parameters of {@code types}' methods that a call site in them passes a closure writing what it captures. */
    static Set<ParameterInfo> mutating(List<TypeInfo> types, RustFacts facts) {
        Set<ParameterInfo> out = new HashSet<>();
        for (TypeInfo type : types) visit(type, facts, out);
        return out;
    }

    private static void visit(TypeInfo type, RustFacts facts, Set<ParameterInfo> out) {
        type.constructorAndMethodStream().filter(m -> m.methodBody() != null).forEach(m -> m.methodBody().visit(e -> {
            switch (e) {
                case MethodCall mc -> arguments(mc.methodInfo(), mc.parameterExpressions(), facts, out);
                case ConstructorCall cc when cc.constructor() != null ->
                        arguments(cc.constructor(), cc.parameterExpressions(), facts, out);
                default -> { }
            }
            return true;
        }));
        type.subTypes().forEach(sub -> visit(sub, facts, out));
    }

    private static void arguments(MethodInfo callee, List<Expression> args, RustFacts facts, Set<ParameterInfo> out) {
        if (callee.typeInfo().compilationUnit().externalLibrary()) return;
        List<ParameterInfo> formals = callee.parameters();
        for (int i = 0; i < args.size() && i < formals.size(); i++) {
            ParameterInfo formal = formals.get(i);
            if (RustTypes.isFn(formal.parameterizedType()) && writesCaptured(args.get(i), facts)) out.add(formal);
        }
    }

    /** The closure {@code e} writes a variable or an object it captures. */
    static boolean writesCaptured(Expression e, RustFacts facts) {
        Expression f = CstQueries.unwrap(e);
        if (f instanceof MethodReference mr) {
            // a bound receiver ('this::parse', 'list::add') the referenced method modifies
            return mr.scope() instanceof VariableExpression && modifying(mr.methodInfo(), facts);
        }
        if (!(f instanceof Lambda lambda)) return false;
        MethodInfo own = lambda.methodInfo();
        boolean[] writes = {false};
        own.methodBody().visit(x -> {
            switch (x) {
                case Assignment a when captured(a.variableTarget(), own) -> writes[0] = true;
                case MethodCall mc when modifying(mc.methodInfo(), facts)
                                        && (mc.object() == null || mc.objectIsImplicit()
                                            || CstQueries.unwrap(mc.object()) instanceof VariableExpression ve
                                               && captured(ve.variable(), own)) -> writes[0] = true;
                default -> { }
            }
            return !writes[0];
        });
        return writes[0];
    }

    // a variable from outside the closure: a field, this, or a local or parameter of the enclosing method
    private static boolean captured(Variable v, MethodInfo lambda) {
        return switch (v) {
            case FieldReference fr -> true;
            case This t -> true;
            case ParameterInfo pi -> pi.methodInfo() != lambda;
            case LocalVariable lv -> false; // a local of the lambda's own body; Java's captured locals are final
            case DependentVariable dv -> true; // an element of an array the closure captures
            default -> false;
        };
    }

    private static boolean modifying(MethodInfo m, RustFacts facts) {
        if (m.isStatic() || m.isConstructor()) return false;
        if (m.typeInfo().compilationUnit().externalLibrary()) return !m.isNonModifying();
        return facts.receiver(m) == RustFacts.Receiver.EXCLUSIVE;
    }
}
