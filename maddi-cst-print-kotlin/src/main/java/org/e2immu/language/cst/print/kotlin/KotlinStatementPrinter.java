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

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.output.*;

/**
 * Prints a {@link Statement} as Kotlin — no trailing semicolons; `val`/`var` local declarations; expression
 * statements and blocks via {@link KotlinExpressionPrinter}. Statement forms not yet handled fall back to the
 * shared Java {@link Statement#print}. Uses the same block guide as the Java printer, so indentation/newlines
 * come out of the shared formatter.
 */
public class KotlinStatementPrinter {

    public static OutputBuilder print(Statement s, Qualification q) {
        return switch (s) {
            case Block block -> block(block, q);
            case ReturnStatement rs -> {
                OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.RETURN);
                if (rs.expression() != null && !rs.expression().isEmpty()) {
                    b.add(SpaceEnum.ONE).add(KotlinExpressionPrinter.print(rs.expression(), q));
                }
                yield b;
            }
            case ExpressionAsStatement es -> KotlinExpressionPrinter.print(es.expression(), q);
            case LocalVariableCreation lvc -> localVariable(lvc, q);
            case IfElseStatement ife -> ifElse(ife, q);
            default -> s.print(q); // not-yet-translated statement forms: Java rendering (valid enough)
        };
    }

    static OutputBuilder block(Block block, Qualification q) {
        OutputBuilder ob = new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE);
        if (!block.statements().isEmpty()) {
            ob.add(block.statements().stream()
                    .filter(st -> !st.isSynthetic())
                    .map(st -> print(st, q))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, GuideImpl.generatorForBlock())));
        }
        return ob.add(SymbolEnum.RIGHT_BRACE);
    }

    private static OutputBuilder localVariable(LocalVariableCreation lvc, Qualification q) {
        LocalVariable lv = lvc.localVariable();
        OutputBuilder b = new OutputBuilderImpl()
                .add(lvc.isFinal() ? KotlinKeyword.VAL : KotlinKeyword.VAR).add(SpaceEnum.ONE)
                .add(new TextImpl(lv.simpleName()));
        boolean hasInitializer = lv.assignmentExpression() != null && !lv.assignmentExpression().isEmpty();
        if (hasInitializer) {
            // type is inferred from the initializer
            b.add(SpaceEnum.ONE).add(SymbolEnum.assignment("=")).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.print(lv.assignmentExpression(), q));
        } else {
            b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(KotlinTypeName.of(lv.parameterizedType())));
        }
        return b;
    }

    private static OutputBuilder ifElse(IfElseStatement ife, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl()
                .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(KotlinExpressionPrinter.print(ife.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE).add(block(ife.block(), q));
        if (ife.elseBlock() != null && !ife.elseBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinKeyword.ELSE).add(SpaceEnum.ONE).add(block(ife.elseBlock(), q));
        }
        return b;
    }
}
