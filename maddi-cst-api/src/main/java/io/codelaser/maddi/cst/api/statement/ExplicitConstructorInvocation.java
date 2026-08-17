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

package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;

import java.util.List;

/**
 * An explicit constructor invocation as the first statement of a constructor body: {@code this(...)} or
 * {@code super(...)}. {@link #isSuper()} distinguishes the two, {@link #methodInfo()} is the invoked
 * constructor, and {@link #parameterExpressions()} are its arguments.
 */
public interface ExplicitConstructorInvocation extends Statement {

    /**
     * @return {@code true} for {@code super(...)}, {@code false} for {@code this(...)}.
     */
    boolean isSuper();

    /**
     * @return the constructor being invoked.
     */
    MethodInfo methodInfo();

    /**
     * @return the argument expressions passed to the constructor, in order.
     */
    List<Expression> parameterExpressions();

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    ExplicitConstructorInvocation withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setIsSuper(boolean isSuper);

        @Fluent
        Builder setSynthetic(boolean synthetic);

        @Fluent
        Builder setMethodInfo(MethodInfo methodInfo);

        @Fluent
        Builder setParameterExpressions(List<Expression> parameterExpressions);

        ExplicitConstructorInvocation build();
    }

    String NAME = "explicitConstructorInvocation";

    @Override
    default String name() {
        return NAME;
    }
}
