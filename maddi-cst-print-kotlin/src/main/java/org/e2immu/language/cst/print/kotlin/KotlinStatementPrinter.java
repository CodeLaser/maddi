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

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.output.*;

import java.util.List;

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
            case ThrowStatement ts -> new OutputBuilderImpl().add(KotlinKeyword.THROW).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.print(ts.expression(), q));
            case WhileStatement ws -> new OutputBuilderImpl()
                    .add(KotlinKeyword.WHILE).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinExpressionPrinter.print(ws.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(block(ws.block(), q));
            case DoStatement ds -> new OutputBuilderImpl()
                    .add(KotlinKeyword.DO).add(SpaceEnum.ONE).add(block(ds.block(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.WHILE).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinExpressionPrinter.print(ds.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            case ForEachStatement fe -> new OutputBuilderImpl()
                    .add(KotlinKeyword.FOR).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(new TextImpl(fe.initializer().localVariable().simpleName()))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.IN).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.print(fe.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(block(fe.block(), q));
            case SwitchStatementNewStyle sw -> whenExpression(sw.expression(), sw.entries(), q);
            case YieldStatement ys -> KotlinExpressionPrinter.print(ys.expression(), q); // a `when` arm's value
            case TryStatement ts -> tryStatement(ts, q);
            default -> s.print(q); // not-yet-translated statement forms: Java rendering (valid enough)
        };
    }

    /** Render `when (selector) { conditions -> arm; … else -> arm }`; shared by switch statement and expression. */
    static OutputBuilder whenExpression(Expression selector, List<SwitchEntry> entries, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl()
                .add(KotlinKeyword.WHEN).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(KotlinExpressionPrinter.print(selector, q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE);
        return b.add(entries.stream().map(e -> whenEntry(e, q))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE,
                        GuideImpl.generatorForBlock())));
    }

    private static OutputBuilder whenEntry(SwitchEntry e, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        // the default arm has no conditions, or a single empty-expression sentinel
        boolean isElse = e.conditions().isEmpty() || e.conditions().stream().allMatch(Expression::isEmpty);
        if (isElse) {
            b.add(KotlinKeyword.ELSE_ARROW);
        } else {
            b.add(e.conditions().stream().filter(c -> !c.isEmpty()).map(c -> KotlinExpressionPrinter.print(c, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
        }
        return b.add(SymbolEnum.LAMBDA).add(arm(e.statement(), q));
    }

    /** A `when` arm: a single-statement block is unwrapped to its value (`1 -> "a"`, not `1 -> { "a" }`). */
    private static OutputBuilder arm(Statement s, Qualification q) {
        if (s instanceof Block block) {
            List<Statement> body = block.statements().stream().filter(x -> !x.isSynthetic()).toList();
            if (body.size() == 1) return print(body.getFirst(), q);
        }
        return print(s, q);
    }

    private static OutputBuilder tryStatement(TryStatement ts, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.TRY).add(SpaceEnum.ONE).add(block(ts.block(), q));
        for (TryStatement.CatchClause cc : ts.catchClauses()) {
            String type = cc.exceptionTypes().isEmpty() ? "Throwable" : KotlinTypeName.of(cc.exceptionTypes().getFirst());
            b.add(SpaceEnum.ONE).add(KotlinKeyword.CATCH).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(new TextImpl(cc.catchVariable().simpleName())).add(SymbolEnum.COLON_LABEL)
                    .add(new TextImpl(type)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(block(cc.block(), q));
        }
        if (ts.finallyBlock() != null && !ts.finallyBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinKeyword.FINALLY).add(SpaceEnum.ONE).add(block(ts.finallyBlock(), q));
        }
        return b;
    }

    /** The statements of a block without the enclosing braces (for a lambda body). */
    static OutputBuilder statementsNoBraces(Block block, Qualification q) {
        return block.statements().stream().filter(st -> !st.isSynthetic()).map(st -> print(st, q))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, GuideImpl.generatorForBlock()));
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
