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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.*;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Prints a {@link Statement} as Kotlin: no semicolons, {@code val}/{@code var}, and every Java statement form
 * translated. A form this class does not know falls back to the Java {@link Statement#print}.
 * <p>
 * The translations that are not one-to-one:
 * <ul>
 *   <li><b>C-style {@code for}</b>: a range loop ({@code for (i in 0 until n)}) when the loop variable is a counter
 *   nobody else assigns and the bound cannot change; otherwise a {@code while} with the updates at the end of the
 *   body, or, when the body {@code continue}s, a {@code while (true)} that runs the updates at the TOP of every
 *   iteration but the first, so that a {@code continue} still updates. Loop variables live in {@code run { }}, the
 *   one block Kotlin has.</li>
 *   <li><b>old-style {@code switch}</b>: a {@code when}. A case that falls through gets the statements of the cases
 *   it falls into, copied; the {@code break} that ends a case disappears; a {@code break} in the middle of one
 *   becomes {@code return@label} out of a {@code run label@{ }} around the {@code when}, because a Kotlin
 *   {@code break} in a {@code when} leaves the enclosing loop.</li>
 *   <li><b>try-with-resources</b> (#104): {@code resource.use { r -> … }}, nested per resource, inside a
 *   {@code try} when there are {@code catch} or {@code finally} clauses (Java closes the resources before they
 *   run, and so does this).</li>
 *   <li><b>multi-catch</b>: one {@code catch} per type, the block repeated.</li>
 * </ul>
 */
public class KotlinStatementPrinter {

    public static OutputBuilder print(Statement s, Qualification q) {
        KotlinContext.pushStatement(s);
        try {
            return printStatement(s, q);
        } finally {
            KotlinContext.popStatement();
        }
    }

    private static OutputBuilder printStatement(Statement s, Qualification q) {
        return switch (s) {
            case Block block -> s.label() != null ? labelledBlock(block, s.label(), q)
                    : new OutputBuilderImpl().add(new TextImpl("run")).add(SpaceEnum.ONE).add(block(block, q));
            case ReturnStatement rs -> returnStatement(rs, q);
            case ExpressionAsStatement es -> KotlinExpressionPrinter.printStatement(es.expression(), q);
            case LocalVariableCreation lvc -> localVariables(lvc, q);
            case IfElseStatement ife -> ifElse(ife, q);
            case ThrowStatement ts -> new OutputBuilderImpl().add(KotlinKeyword.THROW).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.print(ts.expression(), q));
            case WhileStatement ws -> loop(ws, q, () -> new OutputBuilderImpl()
                    .add(KotlinKeyword.WHILE).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinExpressionPrinter.print(ws.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(block(ws.block(), q)));
            case DoStatement ds -> loop(ds, q, () -> new OutputBuilderImpl()
                    .add(KotlinKeyword.DO).add(SpaceEnum.ONE).add(block(ds.block(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.WHILE).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinExpressionPrinter.print(ds.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS));
            case ForEachStatement fe -> loop(fe, q, () -> new OutputBuilderImpl()
                    .add(KotlinKeyword.FOR).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(new TextImpl(declareForEach(fe)))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.IN).add(SpaceEnum.ONE)
                    .add(KotlinNullability.iterable(fe.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(block(fe.block(), q)));
            case ForStatement fs -> forStatement(fs, q);
            case SwitchStatementNewStyle sw -> switchNewStyle(sw, q);
            case SwitchStatementOldStyle sw -> switchOldStyle(sw, q);
            case YieldStatement ys -> KotlinExpressionPrinter.print(ys.expression(), q); // a `when` arm's value
            case TryStatement ts -> tryStatement(ts, q);
            case BreakStatement bs -> breakStatement(bs);
            case ContinueStatement cs -> new OutputBuilderImpl().add(new TextImpl("continue"
                    + (cs.goToLabel() == null ? "" : "@" + cs.goToLabel())));
            case EmptyStatement es -> new OutputBuilderImpl();
            case SynchronizedStatement ss -> new OutputBuilderImpl().add(new TextImpl("synchronized"))
                    .add(SymbolEnum.LEFT_PARENTHESIS).add(KotlinExpressionPrinter.print(ss.expression(), q))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(block(ss.block(), q));
            case AssertStatement as -> assertStatement(as, q);
            case LocalTypeDeclaration ltd -> new KotlinTypePrinter(ltd.typeInfo(), true) // a local type has no visibility
                    .print(new CompilationUnitPrinterImpl.ImportDataImpl(List.of(), q, q), true);
            default -> s.print(q); // not-yet-translated statement forms: Java rendering
        };
    }

    // ---------------------------------------------------------------- blocks

    /** The statements of a block without the enclosing braces; NEWLINE-separated (Kotlin has no `;`). */
    static OutputBuilder statementsNoBraces(Block block, Qualification q) {
        return statements(block.statements().stream().filter(st -> !st.isSynthetic()).toList(), q);
    }

    private static OutputBuilder statements(List<Statement> statements, Qualification q) {
        return statements.stream().map(st -> print(st, q))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
    }

    static OutputBuilder block(Block block, Qualification q) {
        return block(block.statements(), q);
    }

    static OutputBuilder block(List<Statement> statements, Qualification q) {
        return braces(statements.stream().filter(st -> !st.isSynthetic()).map(st -> print(st, q)).toList());
    }

    /** A block that starts with some lines of its own, the {@code var p = p} of {@link #reassignedParameters}. */
    static OutputBuilder block(List<OutputBuilder> prefix, List<Statement> statements, Qualification q) {
        List<OutputBuilder> all = new ArrayList<>(prefix);
        statements.stream().filter(st -> !st.isSynthetic()).forEach(st -> all.add(print(st, q)));
        return braces(all);
    }

    /**
     * {@code var p = p} for each parameter the body assigns: a Kotlin parameter is a val. The local shadows the
     * parameter (Kotlin warns, and accepts).
     */
    static List<OutputBuilder> reassignedParameters(List<io.codelaser.maddi.cst.api.info.ParameterInfo> parameters,
                                                    Element body) {
        if (body == null || parameters.isEmpty()) return List.of();
        List<OutputBuilder> result = new ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.ParameterInfo p : parameters) {
            if (assignedIn(body, p)) {
                String name = KotlinNames.name(p.name());
                result.add(new OutputBuilderImpl().add(KotlinKeyword.VAR).add(SpaceEnum.ONE).add(new TextImpl(name))
                        .add(KotlinSymbols.assignment("=")).add(new TextImpl(name)));
            }
        }
        return result;
    }

    private static OutputBuilder braces(List<OutputBuilder> printed) {
        List<OutputBuilder> nonEmpty = printed.stream().filter(b -> !b.isEmpty()).toList();
        if (nonEmpty.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE)
                .add(nonEmpty.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock())))
                .add(SymbolEnum.RIGHT_BRACE);
    }

    /** A block body of a lambda: the final {@code return x} becomes the value {@code x}, as a lambda's last line is. */
    static OutputBuilder lambdaBody(List<Statement> statements, Qualification q) {
        List<OutputBuilder> printed = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            Statement s = statements.get(i);
            if (i == statements.size() - 1 && s instanceof ReturnStatement rs && rs.goToLabel() == null) {
                if (!rs.hasNoValue()) printed.add(KotlinExpressionPrinter.print(rs.expression(), q));
            } else {
                printed.add(print(s, q));
            }
        }
        return printed.stream().filter(b -> !b.isEmpty())
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
    }

    /** {@code label: { … break label; }}: a {@code run label@{ }} left with {@code return@label}. */
    private static OutputBuilder labelledBlock(Block block, String label, Qualification q) {
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.SWITCH, block, label));
        try {
            return new OutputBuilderImpl().add(new TextImpl("run " + label + "@")).add(block(block, q));
        } finally {
            KotlinContext.pop();
        }
    }

    // ---------------------------------------------------------------- jumps

    /**
     * {@code return@lambda} inside a lambda (a bare {@code return} there would return from the enclosing function);
     * a label from Kotlin source is kept as it was.
     */
    private static OutputBuilder returnStatement(ReturnStatement rs, Qualification q) {
        // one token: the formatter puts a space between a keyword and the text after it
        String label = rs.goToLabel() != null ? rs.goToLabel()
                : KotlinContext.inLambda() ? KotlinContext.LAMBDA_LABEL : null;
        OutputBuilder b = new OutputBuilderImpl().add(label == null ? KotlinKeyword.RETURN : new TextImpl("return@" + label));
        if (!rs.hasNoValue()) {
            Expression value = rs.expression();
            OutputBuilder printed = KotlinExpressionPrinter.print(value, q);
            io.codelaser.maddi.cst.api.info.MethodInfo method = KotlinContext.currentMethod();
            if (label == null && method != null && !method.isConstructor()) {
                printed = KotlinNullability.toTarget(value, KotlinNullability.returnType(method),
                        KotlinNullability.translated(method.typeInfo()), printed, q);
            }
            if (value instanceof And || value instanceof Or) {
                // a long && chain is laid out one operand per line, and `return` at the end of a line returns Unit
                printed = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SymbolEnum.RIGHT_PARENTHESIS);
            }
            b.add(SpaceEnum.ONE).add(printed);
        }
        return b;
    }

    /**
     * A {@code break} that leaves a loop stays {@code break}; one that leaves a switch or a labelled block returns
     * from the {@code run label@{ }} around it.
     */
    private static OutputBuilder breakStatement(BreakStatement bs) {
        KotlinContext.Frame target = bs.goToLabel() == null ? KotlinContext.breakTarget()
                : KotlinContext.frameLabelled(bs.goToLabel());
        if (target != null && target.kind() == KotlinContext.Kind.SWITCH) {
            target.markLeftEarly();
            return new OutputBuilderImpl().add(new TextImpl("return@" + target.label()));
        }
        return new OutputBuilderImpl().add(new TextImpl("break" + (bs.goToLabel() == null ? "" : "@" + bs.goToLabel())));
    }

    // ---------------------------------------------------------------- loops

    private static OutputBuilder loop(Statement loop, Qualification q, java.util.function.Supplier<OutputBuilder> body) {
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.LOOP, loop, loop.label()));
        try {
            OutputBuilder printed = body.get();
            if (loop.label() == null) return printed;
            return new OutputBuilderImpl().add(new TextImpl(loop.label() + "@")).add(SpaceEnum.ONE).add(printed);
        } finally {
            KotlinContext.pop();
        }
    }

    private static OutputBuilder forStatement(ForStatement fs, Qualification q) {
        OutputBuilder range = rangeLoop(fs, q);
        if (range != null) return range;

        List<OutputBuilder> init = new ArrayList<>();
        boolean declares = false;
        for (Element e : fs.initializers()) {
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> init.add(localVariable(lvc, lv, q)));
                declares = true;
            } else if (e instanceof Expression x) {
                init.add(KotlinExpressionPrinter.printStatement(x, q));
            }
        }
        Expression condition = fs.expression();
        boolean hasCondition = condition != null && !condition.isEmpty();
        List<Expression> updates = fs.updaters();
        String label = fs.label();
        boolean continues = !updates.isEmpty() && continuesItself(fs);

        OutputBuilder whileLoop;
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.LOOP, fs, label));
        try {
            List<OutputBuilder> body = new ArrayList<>();
            String first = null;
            if (continues) {
                // the updates at the top of every iteration but the first, so that `continue` runs them
                first = KotlinContext.freshLabel("first");
                body.add(new OutputBuilderImpl().add(KotlinKeyword.IF).add(SpaceEnum.ONE)
                        .add(SymbolEnum.LEFT_PARENTHESIS).add(new TextImpl(first)).add(SymbolEnum.RIGHT_PARENTHESIS)
                        .add(SpaceEnum.ONE).add(new TextImpl(first)).add(KotlinSymbols.assignment("="))
                        .add(new TextImpl("false")).add(SpaceEnum.ONE).add(KotlinKeyword.ELSE).add(SpaceEnum.ONE)
                        .add(braces(updates.stream().map(u -> KotlinExpressionPrinter.printStatement(u, q)).toList())));
                if (hasCondition) {
                    body.add(new OutputBuilderImpl().add(KotlinKeyword.IF).add(SpaceEnum.ONE)
                            .add(SymbolEnum.LEFT_PARENTHESIS).add(SymbolEnum.UNARY_BOOLEAN_NOT)
                            .add(SymbolEnum.LEFT_PARENTHESIS).add(KotlinExpressionPrinter.print(condition, q))
                            .add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                            .add(new TextImpl("break" + (label == null ? "" : "@" + label))));
                }
            }
            fs.block().statements().stream().filter(st -> !st.isSynthetic()).forEach(st -> body.add(print(st, q)));
            if (!continues) updates.forEach(u -> body.add(KotlinExpressionPrinter.printStatement(u, q)));

            whileLoop = new OutputBuilderImpl();
            if (label != null) whileLoop.add(new TextImpl(label + "@")).add(SpaceEnum.ONE);
            whileLoop.add(KotlinKeyword.WHILE).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(continues || !hasCondition ? new OutputBuilderImpl().add(new TextImpl("true"))
                            : KotlinExpressionPrinter.print(condition, q))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(braces(body));
            if (first != null) {
                init.add(new OutputBuilderImpl().add(KotlinKeyword.VAR).add(SpaceEnum.ONE).add(new TextImpl(first))
                        .add(KotlinSymbols.assignment("=")).add(new TextImpl("true")));
            }
        } finally {
            KotlinContext.pop();
        }
        if (!declares && init.isEmpty()) return whileLoop;
        List<OutputBuilder> all = new ArrayList<>(init);
        all.add(whileLoop);
        if (!declares) {
            return all.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
        }
        // the loop variables are the loop's: `run { }` is the only block Kotlin has
        return new OutputBuilderImpl().add(new TextImpl("run")).add(SpaceEnum.ONE).add(braces(all));
    }

    /** A {@code continue} that targets this loop: one not inside a nested loop, or one labelled with this loop's. */
    private static boolean continuesItself(ForStatement fs) {
        boolean[] found = {false};
        fs.block().visit((Element e) -> {
            if (found[0] || e instanceof Lambda) return false;
            if (e instanceof ContinueStatement cs) {
                if (cs.goToLabel() == null || cs.goToLabel().equals(fs.label())) found[0] = true;
                return false;
            }
            if (e instanceof LoopStatement inner && inner != fs) {
                // an unlabelled continue in here is the inner loop's; only a labelled one can still be ours
                if (fs.label() != null) inner.visit((Element x) -> {
                    if (x instanceof ContinueStatement cs && fs.label().equals(cs.goToLabel())) found[0] = true;
                    return !found[0];
                });
                return false;
            }
            return true;
        });
        return found[0];
    }

    /**
     * {@code for (int i = a; i < n; i++)} as {@code for (i in a until n)}, when that is the same loop: one integral
     * counter, compared with a bound that cannot change during the loop, stepped by a positive constant in the
     * direction of the comparison, and assigned nowhere in the body (a Kotlin range variable is a val). A Kotlin
     * range evaluates its bound ONCE, which is why the bound must be stable.
     */
    private static OutputBuilder rangeLoop(ForStatement fs, Qualification q) {
        if (fs.initializers().size() != 1 || !(fs.initializers().getFirst() instanceof LocalVariableCreation lvc)
            || !lvc.hasSingleDeclaration() || fs.updaters().size() != 1) return null;
        LocalVariable v = lvc.localVariable();
        ParameterizedType type = v.parameterizedType();
        if (!(type.isInt() || type.isLong()) || v.assignmentExpression() == null || v.assignmentExpression().isEmpty()) {
            return null;
        }
        if (!(fs.expression() instanceof BinaryOperator cmp) || cmp.operator() == null
            || !(cmp.lhs() instanceof VariableExpression lhs) || !v.equals(lhs.variable())) return null;
        String op = cmp.operator().name();
        long step = step(fs.updaters().getFirst(), v);
        if (step == 0) return null;
        boolean up = step > 0;
        String range = switch (op) {
            case "<" -> up ? "until" : null;
            case "<=" -> up ? ".." : null;
            case ">", ">=" -> up ? null : "downTo";
            default -> null;
        };
        if (range == null || !stable(cmp.rhs(), fs) || assignedIn(fs.block(), v)) return null;

        OutputBuilder bound = KotlinExpressionPrinter.operand(io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum.ADDITIVE,
                cmp.rhs(), q);
        if (">".equals(op)) bound = new OutputBuilderImpl().add(bound).add(KotlinSymbols.binary("+"))
                .add(new TextImpl("1"));
        OutputBuilder from = KotlinExpressionPrinter.operand(io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum.ADDITIVE,
                v.assignmentExpression(), q);
        OutputBuilder header = new OutputBuilderImpl().add(KotlinKeyword.FOR).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_PARENTHESIS).add(new TextImpl(KotlinNames.name(v.simpleName())))
                .add(SpaceEnum.ONE).add(KotlinKeyword.IN).add(SpaceEnum.ONE).add(from);
        if ("..".equals(range)) header.add(new TextImpl("..")); else header.add(SpaceEnum.ONE).add(new TextImpl(range)).add(SpaceEnum.ONE);
        header.add(bound);
        if (Math.abs(step) != 1) {
            header.add(SpaceEnum.ONE).add(new TextImpl("step")).add(SpaceEnum.ONE).add(new TextImpl(Long.toString(Math.abs(step))));
        }
        header.add(SymbolEnum.RIGHT_PARENTHESIS);
        OutputBuilder finalHeader = header;
        return loop(fs, q, () -> new OutputBuilderImpl().add(finalHeader).add(SpaceEnum.ONE).add(block(fs.block(), q)));
    }

    /** +1/-1 for {@code i++}/{@code i--}, +c/-c for {@code i += c}/{@code i -= c} with c a positive constant; else 0. */
    private static long step(Expression update, LocalVariable v) {
        if (!(update instanceof Assignment a) || !v.equals(a.variableTarget())) return 0;
        if (a.prefixPrimitiveOperator() != null) return a.assignmentOperatorIsPlus() ? 1 : -1;
        if (a.assignmentOperator() == null) return 0;
        long c;
        if (a.value() instanceof IntConstant ic) c = ic.constant();
        else if (a.value() instanceof LongConstant lc) c = lc.constant();
        else return 0;
        if (c <= 0) return 0;
        return switch (a.assignmentOperator().name()) {
            case "+=" -> c;
            case "-=" -> -c;
            default -> 0;
        };
    }

    /** A bound the loop cannot change: a constant, a final field, a local or parameter the loop does not assign. */
    private static boolean stable(Expression bound, ForStatement fs) {
        return switch (bound) {
            case IntConstant ic -> true;
            case LongConstant lc -> true;
            case ArrayLength al -> stable(al.scope(), fs);
            case VariableExpression ve -> switch (ve.variable()) {
                case FieldReference fr -> fr.fieldInfo().isFinal()
                                          && (fr.isStatic() || fr.scope() == null || stable(fr.scope(), fs)
                                              || fr.scopeIsThis());
                case LocalVariable lv -> !assignedIn(fs.block(), lv);
                case io.codelaser.maddi.cst.api.info.ParameterInfo pi -> !assignedIn(fs.block(), pi);
                case io.codelaser.maddi.cst.api.variable.This t -> true;
                default -> false;
            };
            default -> false;
        };
    }

    private static boolean assignedIn(Element element, Variable v) {
        boolean[] found = {false};
        element.visit((Element e) -> {
            if (e instanceof Assignment a && v.equals(a.variableTarget())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    // ---------------------------------------------------------------- switch

    private static OutputBuilder switchNewStyle(SwitchStatementNewStyle sw, Qualification q) {
        String label = KotlinContext.freshLabel("switch");
        KotlinContext.Frame frame = new KotlinContext.Frame(KotlinContext.Kind.SWITCH, sw, label);
        KotlinContext.push(frame);
        OutputBuilder when;
        try {
            when = whenExpression(sw.expression(), sw.entries(), true, q);
        } finally {
            KotlinContext.pop();
        }
        return wrapIfLeftEarly(frame, when);
    }

    /** {@code when (selector) { conditions -> arm; … else -> arm }}; shared by switch statement and expression. */
    static OutputBuilder whenExpression(Expression selector, List<SwitchEntry> entries, boolean statement,
                                        Qualification q) {
        OutputBuilder b = new OutputBuilderImpl()
                .add(KotlinKeyword.WHEN).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(selector(selector, entries.stream().flatMap(e -> e.conditions().stream()).toList(), q))
                .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE);
        List<OutputBuilder> arms = new ArrayList<>(entries.stream().map(e -> whenEntry(e, q)).toList());
        boolean hasElse = entries.stream().anyMatch(e -> e.conditions().isEmpty()
                                                         || e.conditions().stream().allMatch(Expression::isEmpty));
        if (statement && !hasElse) addElseWhenRequired(selector, arms);
        return b.add(arms.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, SymbolEnum.LEFT_BRACE,
                SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
    }

    /**
     * {@code else -> {}}: Kotlin requires a {@code when} statement over an enum or a boolean to be exhaustive, where a
     * Java switch simply does nothing for the missing cases.
     */
    private static void addElseWhenRequired(Expression selector, List<OutputBuilder> arms) {
        ParameterizedType type = selector.parameterizedType();
        if (type == null || type.typeInfo() == null || type.arrays() > 0) return;
        if (type.typeInfo().typeNature().isEnum() || type.isBoolean()) {
            arms.add(new OutputBuilderImpl().add(KotlinKeyword.ELSE_ARROW).add(SymbolEnum.LAMBDA)
                    .add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE));
        }
    }

    private static OutputBuilder whenEntry(SwitchEntry e, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        // the default arm has no conditions, or a single empty-expression sentinel
        boolean isElse = e.conditions().isEmpty() || e.conditions().stream().allMatch(Expression::isEmpty);
        if (isElse) {
            b.add(KotlinKeyword.ELSE_ARROW);
        } else {
            b.add(e.conditions().stream().filter(c -> !c.isEmpty()).map(c -> condition(c, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
        }
        return b.add(SymbolEnum.LAMBDA).add(arm(e.statement(), q));
    }

    /**
     * A case label. An enum constant is written unqualified in a Java case and must be qualified in Kotlin, where
     * {@code when}'s branches are ordinary expressions.
     */
    private static OutputBuilder condition(Expression c, Qualification q) {
        if (c instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr && fr.isStatic()) {
            FieldInfo f = fr.fieldInfo();
            if (f.owner().typeNature().isEnum()) {
                return new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.name(f.owner(), q) + "."
                                                                + KotlinNames.name(f.name())));
            }
        }
        return KotlinExpressionPrinter.print(c, q);
    }

    /** A `when` arm: a single-statement block is unwrapped to its value (`1 -> "a"`, not `1 -> { "a" }`). */
    private static OutputBuilder arm(Statement s, Qualification q) {
        if (s instanceof Block block) {
            List<Statement> body = withoutFinalBreak(block.statements().stream().filter(x -> !x.isSynthetic()).toList());
            if (body.size() == 1 && !(body.getFirst() instanceof LocalVariableCreation)) return print(body.getFirst(), q);
            return braces(body.stream().map(st -> print(st, q)).toList());
        }
        if (s instanceof BreakStatement bs && bs.goToLabel() == null) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        }
        return print(s, q);
    }

    private static List<Statement> withoutFinalBreak(List<Statement> statements) {
        if (!statements.isEmpty() && statements.getLast() instanceof BreakStatement bs && bs.goToLabel() == null) {
            return statements.subList(0, statements.size() - 1);
        }
        return statements;
    }

    private static OutputBuilder wrapIfLeftEarly(KotlinContext.Frame frame, OutputBuilder when) {
        if (!frame.leftEarly()) return when;
        return new OutputBuilderImpl().add(new TextImpl("run " + frame.label() + "@")).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_BRACE).add(when).add(SymbolEnum.RIGHT_BRACE);
    }

    /**
     * Groups of case labels with the statements from their position up to the next group's; a group that does not
     * end in a jump falls through, and gets the following groups' statements copied up to the first that does.
     */
    private static OutputBuilder switchOldStyle(SwitchStatementOldStyle sw, Qualification q) {
        List<Statement> statements = sw.block().statements();
        TreeMap<Integer, List<SwitchStatementOldStyle.SwitchLabel>> byStart = new TreeMap<>();
        for (SwitchStatementOldStyle.SwitchLabel l : sw.switchLabels()) {
            byStart.computeIfAbsent(l.startFromPosition(), _ -> new ArrayList<>()).add(l);
        }
        List<Integer> starts = new ArrayList<>(byStart.keySet());
        List<List<Statement>> own = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int from = Math.min(starts.get(i), statements.size());
            int to = i + 1 < starts.size() ? Math.min(starts.get(i + 1), statements.size()) : statements.size();
            own.add(statements.subList(from, to).stream().filter(s -> !s.isSynthetic()).toList());
        }

        String label = KotlinContext.freshLabel("switch");
        KotlinContext.Frame frame = new KotlinContext.Frame(KotlinContext.Kind.SWITCH, sw, label);
        KotlinContext.push(frame);
        List<OutputBuilder> arms = new ArrayList<>();
        try {
            for (int i = 0; i < starts.size(); i++) {
                List<Statement> arm = new ArrayList<>(own.get(i));
                for (int j = i + 1; j < starts.size() && !endsInJump(arm); j++) arm.addAll(own.get(j));
                arm = withoutFinalBreak(arm);
                List<SwitchStatementOldStyle.SwitchLabel> labels = byStart.get(starts.get(i));
                OutputBuilder head;
                if (labels.stream().anyMatch(l -> l.literal() == null || l.literal().isEmpty())) {
                    head = new OutputBuilderImpl().add(KotlinKeyword.ELSE_ARROW);
                } else {
                    head = labels.stream().map(l -> condition(l.literal(), q))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
                }
                arms.add(new OutputBuilderImpl().add(head).add(SymbolEnum.LAMBDA)
                        .add(braces(arm.stream().map(st -> print(st, q)).toList())));
            }
        } finally {
            KotlinContext.pop();
        }
        if (byStart.values().stream().flatMap(List::stream).noneMatch(l -> l.literal() == null || l.literal().isEmpty())) {
            addElseWhenRequired(sw.expression(), arms);
        }
        OutputBuilder when = new OutputBuilderImpl().add(KotlinKeyword.WHEN).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_PARENTHESIS).add(selector(sw.expression(),
                        sw.switchLabels().stream().map(SwitchStatementOldStyle.SwitchLabel::literal).toList(), q))
                .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                .add(arms.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, SymbolEnum.LEFT_BRACE,
                        SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
        return wrapIfLeftEarly(frame, when);
    }

    /**
     * The subject of a {@code when}. Java compares a {@code byte}, {@code short} or {@code char} with {@code int}
     * case constants after widening; Kotlin compares only equal types, so the subject widens: {@code b.toInt()},
     * {@code c.code}.
     */
    private static OutputBuilder selector(Expression selector, List<Expression> labels, Qualification q) {
        ParameterizedType type = selector.parameterizedType();
        boolean intLabels = labels.stream().anyMatch(l -> l != null && !l.isEmpty() && l.parameterizedType() != null
                                                          && l.parameterizedType().isInt());
        if (type == null || !intLabels) return KotlinExpressionPrinter.print(selector, q);
        String widen = type.typeInfo() != null && "char".equals(type.typeInfo().fullyQualifiedName()) && type.arrays() == 0 ? "code" : type.isByte() || type.isShort() ? "toInt()" : null;
        if (widen == null) return KotlinExpressionPrinter.print(selector, q);
        return KotlinExpressionPrinter.receiver(selector, q).add(SymbolEnum.DOT).add(new TextImpl(widen));
    }

    /** Control cannot fall out of the end of these statements. */
    private static boolean endsInJump(List<Statement> statements) {
        if (statements.isEmpty()) return false;
        Statement last = statements.getLast();
        return last instanceof BreakStatement || last instanceof ContinueStatement || last instanceof ReturnStatement
               || last instanceof ThrowStatement || last instanceof YieldStatement
               || last instanceof Block b && endsInJump(b.statements());
    }

    // ---------------------------------------------------------------- try

    private static OutputBuilder tryStatement(TryStatement ts, Qualification q) {
        OutputBuilder body = ts.resources().isEmpty() ? block(ts.block(), q) : braces(List.of(use(ts, 0, q)));
        boolean plainTry = ts.catchClauses().isEmpty() && (ts.finallyBlock() == null || ts.finallyBlock().isEmpty());
        if (!ts.resources().isEmpty() && plainTry) return use(ts, 0, q);

        OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.TRY).add(SpaceEnum.ONE).add(body);
        for (TryStatement.CatchClause cc : ts.catchClauses()) {
            List<ParameterizedType> types = cc.exceptionTypes().isEmpty() ? List.of() : cc.exceptionTypes();
            List<String> names = types.isEmpty() ? List.of("Throwable")
                    : types.stream().map(t -> KotlinTypeName.of(t, q)).toList();
            for (String type : names) { // Kotlin has no multi-catch: one clause per type
                b.add(SpaceEnum.ONE).add(KotlinKeyword.CATCH).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                        .add(new TextImpl(declare(cc.catchVariable().simpleName()))).add(SymbolEnum.COLON_LABEL)
                        .add(new TextImpl(type)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                        .add(block(cc.block(), q));
            }
        }
        if (ts.finallyBlock() != null && !ts.finallyBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinKeyword.FINALLY).add(SpaceEnum.ONE).add(block(ts.finallyBlock(), q));
        }
        return b;
    }

    /**
     * ⛔ #104: {@code init.use { r -> … }} per resource, innermost the body. The resources used to be dropped, which
     * left the body naming variables nobody declared and nothing closing them.
     */
    private static OutputBuilder use(TryStatement ts, int index, Qualification q) {
        if (index == ts.resources().size()) return statementsNoBraces(ts.block(), q);
        Statement resource = ts.resources().get(index);
        OutputBuilder receiver;
        String parameter;
        if (resource instanceof LocalVariableCreation lvc) {
            receiver = KotlinExpressionPrinter.receiver(lvc.localVariable().assignmentExpression(), q);
            parameter = KotlinNames.name(lvc.localVariable().simpleName());
        } else if (resource instanceof ExpressionAsStatement eas) {
            receiver = KotlinExpressionPrinter.receiver(eas.expression(), q);
            parameter = null; // an existing variable: still in scope inside the lambda
        } else {
            receiver = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(print(resource, q))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
            parameter = null;
        }
        OutputBuilder b = receiver.add(SymbolEnum.DOT).add(new TextImpl("use")).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_BRACE);
        if (parameter != null) {
            b.add(SpaceEnum.ONE).add(new TextImpl(parameter)).add(SpaceEnum.ONE).add(SymbolEnum.LAMBDA).add(SpaceEnum.ONE);
        }
        OutputBuilder inner = use(ts, index + 1, q);
        if (inner.isEmpty()) return b.add(SymbolEnum.RIGHT_BRACE);
        return b.add(new OutputBuilderImpl().add(GuideImpl.generatorForBlock().start()).add(inner)
                .add(GuideImpl.generatorForBlock().end())).add(SymbolEnum.RIGHT_BRACE);
    }

    // ---------------------------------------------------------------- the rest

    private static OutputBuilder assertStatement(AssertStatement as, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl().add(new TextImpl("assert")).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(KotlinExpressionPrinter.print(as.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
        if (as.message() != null && !as.message().isEmpty()) {
            b.add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.print(as.message(), q)).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
        }
        return b;
    }

    /**
     * One {@code val}/{@code var} per declared variable. The type is written when there is no initializer, and when
     * it differs from the initializer's: Kotlin infers {@code Int} for {@code long x = 0}.
     */
    private static OutputBuilder localVariables(LocalVariableCreation lvc, Qualification q) {
        return lvc.localVariableStream().map(lv -> localVariable(lvc, lv, q))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
    }

    private static String declareForEach(ForEachStatement fe) {
        String name = declare(fe.initializer().localVariable().simpleName());
        KotlinNullability.localType(fe, fe.initializer().localVariable());
        return name;
    }

    /** The escaped name of a variable declared here, which from now on is not a pattern variable of that name. */
    static String declare(String name) {
        KotlinContext.declared(name);
        return KotlinNames.name(name);
    }

    private static OutputBuilder localVariable(LocalVariableCreation lvc, LocalVariable lv, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl()
                .add(lvc.isFinal() ? KotlinKeyword.VAL : KotlinKeyword.VAR).add(SpaceEnum.ONE)
                .add(new TextImpl(declare(lv.simpleName())));
        Expression init = lv.assignmentExpression();
        boolean hasInitializer = init != null && !init.isEmpty();
        ParameterizedType type = KotlinNullability.localType(lvc, lv);
        // a nullable local is typed: from a non-null initializer Kotlin would infer a type that rejects a later null
        boolean writeType = !hasInitializer || KotlinNullability.isNullable(type) && !KotlinNullability.nullableInKotlin(init)
                            || !lvc.isVar() && !Objects.equals(lv.parameterizedType(), init.parameterizedType());
        if (writeType) b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(KotlinTypeName.of(type, q)));
        if (hasInitializer && KotlinNullability.isNullable(type) && !KotlinNullability.nullableInKotlin(init)) {
            // `var x: T? = ArrayList()` does not smart-cast x to non-null, an assignment does: declare, then assign,
            // and the uses that follow need no `!!` (kotlinc 2.4; the use-site facts assume the assignment's cast)
            OutputBuilder assignment = new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(lv.simpleName())))
                    .add(KotlinSymbols.assignment("=")).add(KotlinExpressionPrinter.widened(init, type, q));
            return Stream.of(b, assignment).collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE,
                    GuideImpl.generatorForBlock()));
        }
        if (hasInitializer) {
            b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                    .add(KotlinNullability.toTarget(init, type, true, KotlinExpressionPrinter.widened(init, type, q), q));
        }
        return b;
    }

    private static OutputBuilder ifElse(IfElseStatement ife, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl()
                .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(KotlinExpressionPrinter.print(ife.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE).add(block(ife.block(), q));
        if (ife.elseBlock() != null && !ife.elseBlock().isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinKeyword.ELSE).add(SpaceEnum.ONE);
            IfElseStatement chained = soleIfElse(ife.elseBlock());
            // `else { if … }` -> idiomatic `else if …` (flatten the chain by recursing, not wrapping in a block)
            b.add(chained != null ? ifElse(chained, q) : block(ife.elseBlock(), q));
        }
        return b;
    }

    /** If a block's only (non-synthetic) statement is an if/else, return it — for `else if` chain flattening. */
    private static IfElseStatement soleIfElse(Block block) {
        List<Statement> body = block.statements().stream().filter(x -> !x.isSynthetic()).toList();
        return body.size() == 1 && body.getFirst() instanceof IfElseStatement ife && ife.label() == null ? ife : null;
    }
}
