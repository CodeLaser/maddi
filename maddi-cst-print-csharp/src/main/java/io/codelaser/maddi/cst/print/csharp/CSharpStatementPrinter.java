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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.EmptyExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.*;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Prints a {@link Statement} as C#. Most of Java's statements are C#'s; where they differ:
 * <ul>
 *   <li>{@code for (T x : xs)} is {@code foreach (T x in xs)}; {@code synchronized (o)} is {@code lock (o)};
 *   {@code assert c : m} is {@code Debug.Assert(c, m)};</li>
 *   <li>a labelled {@code break} or {@code continue} is a {@code goto}: {@code break outer} jumps to
 *   {@code outer_break:} after the loop, {@code continue outer} to {@code outer_continue:} at the end of its body;</li>
 *   <li>a C# switch section may not fall through: a Java section that does ends in {@code goto case …} (or
 *   {@code goto default}) to the next one, and the last section in {@code break};</li>
 *   <li>a switch expression is C#'s {@code selector switch { A or B => x, _ => y }}; an arm that is a block with
 *   {@code yield} is a lambda that is called at once: {@code new Func<T>(() => { …; return v; })()};</li>
 *   <li>try-with-resources is {@code using (T r = …)} per resource; a multi-catch is one {@code catch} with a
 *   {@code when} filter;</li>
 *   <li>a local is declared {@code var} where Java says {@code var}, or where its initializer constructs exactly the
 *   declared type ({@code var list = new List<int>()}); with its type otherwise.</li>
 * </ul>
 */
public final class CSharpStatementPrinter {

    private CSharpStatementPrinter() {
    }

    public static OutputBuilder print(Statement s, Qualification q) {
        String label = s.label();
        if (label == null || s instanceof ExplicitConstructorInvocation) return printStatement(s, q);
        // a labelled statement: goto targets for the labelled break and continue statements aimed at it
        boolean breaks = jumpsTo(s, label, BreakStatement.class);
        boolean continues = jumpsTo(s, label, ContinueStatement.class);
        OutputBuilder b = printStatement(s, q, continues ? label : null);
        if (breaks) b.add(SpaceEnum.NEWLINE).add(text(breakLabel(label) + ":")).add(SpaceEnum.ONE).add(SymbolEnum.SEMICOLON);
        return b;
    }

    private static OutputBuilder printStatement(Statement s, Qualification q) {
        return printStatement(s, q, null);
    }

    /** {@code continueLabel}: a loop's label, when a {@code continue} jumps to the end of its body. */
    private static OutputBuilder printStatement(Statement s, Qualification q, String continueLabel) {
        return switch (s) {
            case Block block -> block(block, q);
            case ReturnStatement rs -> rs.hasNoValue()
                    ? new OutputBuilderImpl().add(KeywordImpl.RETURN).add(SymbolEnum.SEMICOLON)
                    : new OutputBuilderImpl().add(KeywordImpl.RETURN).add(SpaceEnum.ONE)
                            .add(CSharpExpressionPrinter.print(rs.expression(), q)).add(SymbolEnum.SEMICOLON);
            // only in a switch expression's block arm, which is a lambda: its value is the lambda's
            case YieldStatement ys -> new OutputBuilderImpl().add(KeywordImpl.RETURN).add(SpaceEnum.ONE)
                    .add(CSharpExpressionPrinter.print(ys.expression(), q)).add(SymbolEnum.SEMICOLON);
            case ExpressionAsStatement es -> new OutputBuilderImpl()
                    .add(CSharpExpressionPrinter.print(es.expression(), q)).add(SymbolEnum.SEMICOLON);
            case LocalVariableCreation lvc -> new OutputBuilderImpl().add(declaration(lvc, q)).add(SymbolEnum.SEMICOLON);
            case IfElseStatement ife -> ifElse(ife, q);
            case ThrowStatement ts -> new OutputBuilderImpl().add(KeywordImpl.THROW).add(SpaceEnum.ONE)
                    .add(CSharpExpressionPrinter.print(ts.expression(), q)).add(SymbolEnum.SEMICOLON);
            case WhileStatement ws -> new OutputBuilderImpl().add(KeywordImpl.WHILE).add(SpaceEnum.ONE)
                    .add(parenthesized(ws.expression(), q)).add(SpaceEnum.ONE).add(loopBody(ws.block(), continueLabel, q));
            case DoStatement ds -> new OutputBuilderImpl().add(KeywordImpl.DO).add(SpaceEnum.ONE)
                    .add(loopBody(ds.block(), continueLabel, q)).add(SpaceEnum.ONE).add(KeywordImpl.WHILE)
                    .add(SpaceEnum.ONE).add(parenthesized(ds.expression(), q)).add(SymbolEnum.SEMICOLON);
            case ForEachStatement fe -> new OutputBuilderImpl().add(CSharpKeyword.FOREACH).add(SpaceEnum.ONE)
                    .add(SymbolEnum.LEFT_PARENTHESIS).add(forEachVariable(fe, q)).add(SpaceEnum.ONE)
                    .add(CSharpKeyword.IN).add(SpaceEnum.ONE).add(CSharpExpressionPrinter.print(fe.expression(), q))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(loopBody(fe.block(), continueLabel, q));
            case ForStatement fs -> forStatement(fs, continueLabel, q);
            case SwitchStatementOldStyle sw -> switchOldStyle(sw, q);
            case SwitchStatementNewStyle sw -> switchNewStyle(sw, q);
            case TryStatement ts -> tryStatement(ts, q);
            case BreakStatement bs -> bs.goToLabel() == null
                    ? new OutputBuilderImpl().add(KeywordImpl.BREAK).add(SymbolEnum.SEMICOLON)
                    : jump(breakLabel(bs.goToLabel()));
            case ContinueStatement cs -> cs.goToLabel() == null
                    ? new OutputBuilderImpl().add(KeywordImpl.CONTINUE).add(SymbolEnum.SEMICOLON)
                    : jump(continueLabel(cs.goToLabel()));
            case EmptyStatement es -> new OutputBuilderImpl().add(SymbolEnum.SEMICOLON);
            case SynchronizedStatement ss -> new OutputBuilderImpl().add(CSharpKeyword.LOCK).add(SpaceEnum.ONE)
                    .add(parenthesized(ss.expression(), q)).add(SpaceEnum.ONE).add(block(ss.block(), q));
            case AssertStatement as -> assertStatement(as, q);
            case LocalTypeDeclaration ltd -> {
                CSharpContext.message(CSharpPrintMessage.Code.LOCAL_CLASS, ltd, ltd.typeInfo().simpleName());
                yield new OutputBuilderImpl();
            }
            default -> {
                CSharpContext.message(CSharpPrintMessage.Code.JAVA_FALLBACK, s, s.getClass().getSimpleName());
                yield s.print(q);
            }
        };
    }

    // ---------------------------------------------------------------- blocks

    static OutputBuilder block(Block block, Qualification q) {
        return block(block.statements(), q);
    }

    static OutputBuilder block(List<Statement> statements, Qualification q) {
        return braces(statements.stream().filter(st -> !st.isSynthetic()).map(st -> print(st, q)).toList());
    }

    static OutputBuilder braces(List<OutputBuilder> printed) {
        List<OutputBuilder> nonEmpty = printed.stream().filter(o -> !o.isEmpty()).toList();
        if (nonEmpty.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        return nonEmpty.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, SymbolEnum.LEFT_BRACE,
                SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock()));
    }

    /** A loop body; with the target of a labelled {@code continue} as its last statement. */
    private static OutputBuilder loopBody(Block body, String continueLabel, Qualification q) {
        if (continueLabel == null) return block(body, q);
        List<OutputBuilder> printed = new ArrayList<>(body.statements().stream().filter(st -> !st.isSynthetic())
                .map(st -> print(st, q)).toList());
        printed.add(new OutputBuilderImpl().add(text(continueLabel(continueLabel) + ":")).add(SpaceEnum.ONE)
                .add(SymbolEnum.SEMICOLON));
        return braces(printed);
    }

    private static OutputBuilder ifElse(IfElseStatement ife, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.IF).add(SpaceEnum.ONE)
                .add(parenthesized(ife.expression(), q)).add(SpaceEnum.ONE).add(block(ife.block(), q));
        if (ife.elseBlock() != null && !ife.elseBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KeywordImpl.ELSE).add(SpaceEnum.ONE);
            List<Statement> elseStatements = ife.elseBlock().statements().stream().filter(x -> !x.isSynthetic()).toList();
            // else { if … } is else if …
            if (elseStatements.size() == 1 && elseStatements.getFirst() instanceof IfElseStatement chained
                && chained.label() == null) {
                b.add(ifElse(chained, q));
            } else {
                b.add(block(ife.elseBlock(), q));
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- locals and loops

    /** {@code int a = 1, b}, {@code var list = new List<int>()}: a declaration without its semicolon. */
    static OutputBuilder declaration(LocalVariableCreation lvc, Qualification q) {
        LocalVariable lv = lvc.localVariable();
        OutputBuilder b = new OutputBuilderImpl();
        b.add(text(implicitlyTyped(lvc) ? "var" : CSharpTypeName.of(lv.parameterizedType(), q))).add(SpaceEnum.ONE);
        return b.add(lvc.localVariableStream().map(v -> {
            OutputBuilder d = new OutputBuilderImpl().add(text(CSharpNames.name(v.simpleName())));
            Expression init = v.assignmentExpression();
            if (init != null && !init.isEmpty()) {
                d.add(SymbolEnum.assignment("=")).add(CSharpExpressionPrinter.print(init, q));
            }
            return d;
        }).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
    }

    private static boolean implicitlyTyped(LocalVariableCreation lvc) {
        if (!lvc.otherLocalVariables().isEmpty()) return false;
        LocalVariable lv = lvc.localVariable();
        Expression init = lv.assignmentExpression();
        if (init == null || init.isEmpty() || init instanceof EmptyExpression) return false;
        if (lvc.isVar()) return true;
        return CSharpExpressionPrinter.unwrap(init) instanceof ConstructorCall cc && cc.anonymousClass() == null
               && cc.parameterizedType().arrays() == 0
               && Objects.equals(cc.parameterizedType(), lv.parameterizedType());
    }

    private static OutputBuilder forEachVariable(ForEachStatement fe, Qualification q) {
        LocalVariable lv = fe.initializer().localVariable();
        String type = fe.initializer().isVar() ? "var" : CSharpTypeName.of(lv.parameterizedType(), q);
        return text(type + " " + CSharpNames.name(lv.simpleName()));
    }

    private static OutputBuilder forStatement(ForStatement fs, String continueLabel, Qualification q) {
        OutputBuilder init = fs.initializers().stream().map(e -> switch (e) {
            case LocalVariableCreation lvc -> declaration(lvc, q);
            case Expression x -> CSharpExpressionPrinter.print(x, q);
            default -> text(e.toString());
        }).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
        OutputBuilder updates = fs.updaters().stream().map(e -> CSharpExpressionPrinter.print(e, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
        OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.FOR).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_PARENTHESIS).add(init).add(SymbolEnum.SEMICOLON);
        if (fs.expression() != null && !fs.expression().isEmpty()) {
            b.add(CSharpExpressionPrinter.print(fs.expression(), q));
        }
        return b.add(SymbolEnum.SEMICOLON).add(updates).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                .add(loopBody(fs.block(), continueLabel, q));
    }

    // ---------------------------------------------------------------- labels

    private static String breakLabel(String label) {
        return label + "_break";
    }

    private static String continueLabel(String label) {
        return label + "_continue";
    }

    private static OutputBuilder jump(String target) {
        return new OutputBuilderImpl().add(KeywordImpl.GOTO).add(SpaceEnum.ONE).add(text(target)).add(SymbolEnum.SEMICOLON);
    }

    private static boolean jumpsTo(Statement s, String label, Class<? extends BreakOrContinueStatement> kind) {
        boolean[] found = {false};
        s.visit((Element e) -> {
            if (kind.isInstance(e) && label.equals(((BreakOrContinueStatement) e).goToLabel())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    // ---------------------------------------------------------------- switch

    /**
     * Java's sections fall through; C#'s may not. A section whose end can be reached jumps to the next one,
     * {@code goto case X;}, and the last one breaks.
     */
    private static OutputBuilder switchOldStyle(SwitchStatementOldStyle sw, Qualification q) {
        List<Statement> statements = sw.block().statements();
        TreeMap<Integer, List<SwitchStatementOldStyle.SwitchLabel>> byStart = new TreeMap<>();
        for (SwitchStatementOldStyle.SwitchLabel l : sw.switchLabels()) {
            byStart.computeIfAbsent(l.startFromPosition(), _ -> new ArrayList<>()).add(l);
        }
        List<Integer> starts = new ArrayList<>(byStart.keySet());
        List<OutputBuilder> sections = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int from = Math.min(starts.get(i), statements.size());
            int to = i + 1 < starts.size() ? Math.min(starts.get(i + 1), statements.size()) : statements.size();
            List<Statement> own = statements.subList(from, to).stream().filter(s -> !s.isSynthetic()).toList();
            List<OutputBuilder> labels = new ArrayList<>();
            byStart.get(starts.get(i)).forEach(l -> labels.add(caseLabel(l.literal(), l.patternVariable(),
                    l.whenExpression(), q)));
            List<OutputBuilder> lines = new ArrayList<>();
            own.forEach(s -> lines.add(print(s, q)));
            if (!endsInJump(own)) {
                if (i + 1 < starts.size()) {
                    SwitchStatementOldStyle.SwitchLabel next = byStart.get(starts.get(i + 1)).getFirst();
                    lines.add(isDefault(next.literal())
                            ? new OutputBuilderImpl().add(KeywordImpl.GOTO).add(SpaceEnum.ONE).add(KeywordImpl.DEFAULT)
                                    .add(SymbolEnum.SEMICOLON)
                            : new OutputBuilderImpl().add(KeywordImpl.GOTO).add(SpaceEnum.ONE).add(KeywordImpl.CASE)
                                    .add(SpaceEnum.ONE).add(caseConstant(next.literal(), q)).add(SymbolEnum.SEMICOLON));
                } else {
                    lines.add(new OutputBuilderImpl().add(KeywordImpl.BREAK).add(SymbolEnum.SEMICOLON));
                }
            }
            sections.add(section(labels, lines));
        }
        return switchStatement(sw.expression(), sections, q);
    }

    private static OutputBuilder switchNewStyle(SwitchStatementNewStyle sw, Qualification q) {
        List<OutputBuilder> sections = new ArrayList<>();
        for (SwitchEntry entry : sw.entries()) {
            Statement body = entry.statement();
            // a block arm's statements are the section's
            List<Statement> statements = body instanceof Block block
                    ? block.statements().stream().filter(s -> !s.isSynthetic()).toList() : List.of(body);
            List<OutputBuilder> lines = new ArrayList<>(statements.stream().map(s -> print(s, q)).toList());
            if (!endsInJump(statements)) lines.add(new OutputBuilderImpl().add(KeywordImpl.BREAK).add(SymbolEnum.SEMICOLON));
            sections.add(section(entryLabels(entry, q), lines));
        }
        return switchStatement(sw.expression(), sections, q);
    }

    /** The labels, and the statements indented under them. */
    private static OutputBuilder section(List<OutputBuilder> labels, List<OutputBuilder> statements) {
        OutputBuilder b = labels.stream().collect(OutputBuilderImpl.joining(SpaceEnum.ONE));
        List<OutputBuilder> nonEmpty = statements.stream().filter(o -> !o.isEmpty()).toList();
        if (nonEmpty.isEmpty()) return b;
        return b.add(SpaceEnum.ONE).add(nonEmpty.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock())));
    }

    private static OutputBuilder switchStatement(Expression selector, List<OutputBuilder> sections, Qualification q) {
        return new OutputBuilderImpl().add(KeywordImpl.SWITCH).add(SpaceEnum.ONE).add(parenthesized(selector, q))
                .add(SpaceEnum.ONE).add(sections.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                        SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
    }

    private static List<OutputBuilder> entryLabels(SwitchEntry entry, Qualification q) {
        List<Expression> conditions = entry.conditions();
        if (conditions.isEmpty() || conditions.stream().allMatch(CSharpStatementPrinter::isDefault)) {
            return List.of(new OutputBuilderImpl().add(KeywordImpl.DEFAULT).add(SymbolEnum.COLON_LABEL));
        }
        return conditions.stream().map(c -> caseLabel(c, entry.patternVariable(), entry.whenExpression(), q)).toList();
    }

    /** {@code case X:}, {@code case T t when (c):}, {@code default:}. */
    private static OutputBuilder caseLabel(Expression literal, RecordPattern pattern, Expression when, Qualification q) {
        if (isDefault(literal) && pattern == null) {
            return new OutputBuilderImpl().add(KeywordImpl.DEFAULT).add(SymbolEnum.COLON_LABEL);
        }
        OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.CASE).add(SpaceEnum.ONE)
                .add(pattern != null ? pattern(pattern, q) : caseConstant(literal, q));
        if (when != null && !when.isEmpty()) {
            b.add(SpaceEnum.ONE).add(CSharpKeyword.WHEN).add(SpaceEnum.ONE).add(CSharpExpressionPrinter.print(when, q));
        }
        return b.add(SymbolEnum.COLON_LABEL);
    }

    private static OutputBuilder pattern(RecordPattern pattern, Qualification q) {
        if (pattern.localVariable() != null) {
            return text(CSharpTypeName.of(pattern.localVariable().parameterizedType(), q) + " "
                        + CSharpNames.name(pattern.localVariable().simpleName()));
        }
        CSharpContext.message(CSharpPrintMessage.Code.SWITCH_FORM, null, "record pattern");
        return text(CSharpTypeName.of(pattern.parameterizedType(), q));
    }

    /** A case constant; Java's unqualified enum constant {@code RED} is C#'s {@code Color.Red}. */
    private static OutputBuilder caseConstant(Expression literal, Qualification q) {
        if (literal instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
            && CSharpNames.isEnumConstant(fr.fieldInfo())) {
            return text(CSharpTypeName.name(fr.fieldInfo().owner(), q) + "." + CSharpNames.field(fr.fieldInfo()));
        }
        if (literal instanceof TypeExpression te) {
            // case String s -> without a variable: a type pattern
            return text(CSharpTypeName.of(te.parameterizedType(), q));
        }
        return CSharpExpressionPrinter.print(literal, q);
    }

    private static boolean isDefault(Expression literal) {
        return literal == null || literal.isEmpty() || literal instanceof EmptyExpression;
    }

    /** Control cannot reach the end of these statements. Conservative: a {@code break;} too many is only a warning. */
    private static boolean endsInJump(List<Statement> statements) {
        if (statements.isEmpty()) return false;
        Statement last = statements.getLast();
        return last instanceof BreakStatement || last instanceof ContinueStatement || last instanceof ReturnStatement
               || last instanceof ThrowStatement || last instanceof YieldStatement
               || last instanceof Block b && b.label() == null && endsInJump(b.statements())
               || last instanceof IfElseStatement ife && ife.elseBlock() != null && !ife.elseBlock().isEmpty()
                  && endsInJump(ife.block().statements()) && endsInJump(ife.elseBlock().statements());
    }

    /** {@code selector switch { A or B => x, _ => y }}. */
    static OutputBuilder switchExpression(SwitchExpression se, Qualification q) {
        List<OutputBuilder> arms = new ArrayList<>();
        for (SwitchEntry entry : se.entries()) {
            OutputBuilder head;
            List<Expression> conditions = entry.conditions();
            if (conditions.isEmpty() || conditions.stream().allMatch(CSharpStatementPrinter::isDefault)) {
                head = text("_");
            } else if (entry.patternVariable() != null) {
                head = pattern(entry.patternVariable(), q);
            } else {
                head = joinOr(conditions, q);
            }
            if (entry.whenExpression() != null && !entry.whenExpression().isEmpty()) {
                head.add(SpaceEnum.ONE).add(CSharpKeyword.WHEN).add(SpaceEnum.ONE)
                        .add(CSharpExpressionPrinter.print(entry.whenExpression(), q));
            }
            arms.add(new OutputBuilderImpl().add(head).add(SymbolEnum.binaryOperator("=>"))
                    .add(armValue(entry.statement(), se.parameterizedType(), q)));
        }
        return new OutputBuilderImpl().add(CSharpExpressionPrinter.receiver(se.selector(), q)).add(SpaceEnum.ONE)
                .add(KeywordImpl.SWITCH).add(SpaceEnum.ONE).add(arms.stream().collect(OutputBuilderImpl.joining(
                        SymbolEnum.COMMA, SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
    }

    private static OutputBuilder joinOr(List<Expression> conditions, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) b.add(SpaceEnum.ONE).add(text("or")).add(SpaceEnum.ONE);
            b.add(caseConstant(conditions.get(i), q));
        }
        return b;
    }

    /** An arm's value: the expression, a throw expression, or a block as a lambda called at once. */
    private static OutputBuilder armValue(Statement statement, ParameterizedType type, Qualification q) {
        if (statement instanceof ExpressionAsStatement eas) return CSharpExpressionPrinter.print(eas.expression(), q);
        if (statement instanceof ThrowStatement ts) {
            return new OutputBuilderImpl().add(KeywordImpl.THROW).add(SpaceEnum.ONE)
                    .add(CSharpExpressionPrinter.print(ts.expression(), q));
        }
        if (statement instanceof Block block) {
            List<Statement> statements = block.statements().stream().filter(s -> !s.isSynthetic()).toList();
            if (statements.size() == 1 && statements.getFirst() instanceof YieldStatement ys) {
                return CSharpExpressionPrinter.print(ys.expression(), q);
            }
            if (statements.size() == 1 && statements.getFirst() instanceof ThrowStatement ts) {
                return armValue(ts, type, q);
            }
            CSharpContext.using("System");
            return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(text("Func<" + CSharpTypeName.of(type, q) + ">")).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS).add(SymbolEnum.binaryOperator("=>"))
                    .add(block(statements, q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        CSharpContext.message(CSharpPrintMessage.Code.SWITCH_FORM, statement, CSharpContext.describe(statement));
        return statement.print(q);
    }

    // ---------------------------------------------------------------- try

    private static OutputBuilder tryStatement(TryStatement ts, Qualification q) {
        OutputBuilder body = ts.resources().isEmpty() ? block(ts.block(), q) : using(ts, q);
        boolean plain = ts.catchClauses().isEmpty() && (ts.finallyBlock() == null || ts.finallyBlock().isEmpty());
        if (!ts.resources().isEmpty() && plain) return body;
        if (!ts.resources().isEmpty()) body = braces(List.of(body));

        OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.TRY).add(SpaceEnum.ONE).add(body);
        for (TryStatement.CatchClause cc : ts.catchClauses()) {
            String variable = CSharpNames.name(cc.catchVariable().simpleName());
            List<ParameterizedType> types = cc.exceptionTypes();
            b.add(SpaceEnum.ONE).add(KeywordImpl.CATCH).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS);
            if (types.size() == 1) {
                b.add(text(CSharpTypeName.of(types.getFirst(), q) + " " + variable)).add(SymbolEnum.RIGHT_PARENTHESIS);
            } else {
                // catch (A | B e): one clause, filtered
                CSharpContext.using("System");
                b.add(text("Exception " + variable)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                        .add(CSharpKeyword.WHEN).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                        .add(text(types.stream().map(t -> variable + " is " + CSharpTypeName.of(t, q))
                                .reduce((x, y) -> x + " || " + y).orElse("true")))
                        .add(SymbolEnum.RIGHT_PARENTHESIS);
            }
            b.add(SpaceEnum.ONE).add(block(cc.block(), q));
        }
        if (ts.finallyBlock() != null && !ts.finallyBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KeywordImpl.FINALLY).add(SpaceEnum.ONE).add(block(ts.finallyBlock(), q));
        }
        return b;
    }

    /** {@code using (T a = …) using (U b = …) { body }}: disposed in reverse order, as Java closes its resources. */
    private static OutputBuilder using(TryStatement ts, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        for (Statement resource : ts.resources()) {
            b.add(CSharpKeyword.USING).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS);
            if (resource instanceof LocalVariableCreation lvc) b.add(declaration(lvc, q));
            else if (resource instanceof ExpressionAsStatement eas) b.add(CSharpExpressionPrinter.print(eas.expression(), q));
            else b.add(print(resource, q));
            b.add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.NEWLINE);
        }
        return b.add(block(ts.block(), q));
    }

    // ---------------------------------------------------------------- the rest

    private static OutputBuilder assertStatement(AssertStatement as, Qualification q) {
        CSharpContext.using("System.Diagnostics");
        OutputBuilder args = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(CSharpExpressionPrinter.print(as.expression(), q));
        Expression message = as.message();
        if (message != null && !message.isEmpty()) {
            args.add(SymbolEnum.COMMA);
            ParameterizedType type = message.parameterizedType();
            if (type != null && type.arrays() == 0 && type.isJavaLangString()) {
                args.add(CSharpExpressionPrinter.print(message, q));
            } else {
                // Java's message is any value, String.valueOf'd
                CSharpContext.using("System");
                args.add(text("Convert.ToString")).add(SymbolEnum.LEFT_PARENTHESIS)
                        .add(CSharpExpressionPrinter.print(message, q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            }
        }
        return new OutputBuilderImpl().add(text("Debug.Assert")).add(args.add(SymbolEnum.RIGHT_PARENTHESIS))
                .add(SymbolEnum.SEMICOLON);
    }

    private static OutputBuilder parenthesized(Expression e, Qualification q) {
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter.print(e, q))
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    private static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }
}
