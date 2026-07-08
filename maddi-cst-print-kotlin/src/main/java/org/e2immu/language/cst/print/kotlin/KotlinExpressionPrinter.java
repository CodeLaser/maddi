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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.impl.output.*;

/**
 * Prints an {@link Expression} as Kotlin, translating the Java-only forms and recursing into their
 * sub-expressions: `new Foo(a)`→`Foo(a)`, `(T) x`→`x as T`, `x instanceof T`→`x is T`, `c ? t : f`→
 * `if (c) t else f`. A method call recurses into its receiver and arguments. Everything else (constants,
 * variable references, binary/unary operators, …) is delegated to the shared Java {@link Expression#print} —
 * identical syntax for those forms. First increment: a Java-only construct nested inside a delegated form (e.g.
 * a cast inside a `+`) is not yet translated; that needs recursion through the operator families too.
 */
public class KotlinExpressionPrinter {

    public static OutputBuilder print(Expression e, Qualification q) {
        return switch (e) {
            case ConstructorCall cc when cc.constructor() != null -> constructorCall(cc, q);
            case Cast cast -> new OutputBuilderImpl().add(print(cast.expression(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(cast.parameterizedType())));
            case InstanceOf io -> new OutputBuilderImpl().add(print(io.expression(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.IS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(io.testType())));
            case InlineConditional ic -> new OutputBuilderImpl()
                    .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ic.condition(), q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                    .add(print(ic.ifTrue(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.ELSE).add(SpaceEnum.ONE).add(print(ic.ifFalse(), q));
            case MethodCall mc -> methodCall(mc, q);
            case EnclosedExpression ee -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ee.inner(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            default -> e.print(q); // constants, variables, operators, …: identical to Java
        };
    }

    private static OutputBuilder constructorCall(ConstructorCall cc, Qualification q) {
        // Kotlin has no `new`: Type(args)
        return new OutputBuilderImpl()
                .add(new TextImpl(KotlinTypeName.of(cc.parameterizedType())))
                .add(arguments(cc.parameterExpressions(), q));
    }

    private static OutputBuilder methodCall(MethodCall mc, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        Expression object = mc.object();
        boolean implicitThis = object instanceof VariableExpression ve && ve.variable() instanceof This;
        if (object != null && !implicitThis) {
            b.add(print(object, q)).add(SymbolEnum.DOT);
        }
        b.add(new TextImpl(mc.methodInfo().name())).add(arguments(mc.parameterExpressions(), q));
        return b;
    }

    private static OutputBuilder arguments(java.util.List<Expression> args, Qualification q) {
        if (args.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        return args.stream().map(a -> print(a, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }
}
