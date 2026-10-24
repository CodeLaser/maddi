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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.*;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.print.rust.RustFacts.Passing;
import io.codelaser.maddi.cst.print.rust.RustFacts.Receiver;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A method body as Rust statements. Every use of a value is adapted to what its destination expects, as the facts
 * say: an argument for a {@code &T} parameter is borrowed, one for {@code &mut T} borrowed exclusively, one for a
 * {@code T} parameter moved. Nothing is cloned to make the borrow checker happy; a string is the one exception
 * ({@code .to_string()} where an owned {@code String} is stored), because it is immutable in Java and the copy cannot
 * change behaviour.
 */
final class RustBodyPrinter {

    /** How an expression's value is held, which decides how it is passed on. */
    enum Held {
        /** An owned value or a copy. */
        VALUE,
        /** A place: a field or an element, which a borrow refers to and a move takes out of. */
        PLACE,
        /** {@code &T}. */
        SHARED,
        /** {@code &mut T}. */
        EXCLUSIVE,
        /** {@code &str}: a string literal, or a string parameter. */
        STR
    }

    record Printed(String text, Held held, boolean nullable) {
    }

    private static final String INDENT = "    ";

    private final RustContext context;
    /** How each local of the method is held, from its declaration. */
    private final Map<LocalVariable, Held> locals = new HashMap<>();
    private final Map<LocalVariable, Boolean> nullableLocals = new HashMap<>();
    /** the methods of the lambdas printed as closures: their parameters are an element or a reference to one */
    private final Set<MethodInfo> closures = new HashSet<>();
    /** Variables printed as another name: in {@code eq}, Java's parameter and its cast or pattern are {@code other}. */
    private final Map<Variable, String> aliases = new HashMap<>();

    RustBodyPrinter(RustContext context) {
        this.context = context;
    }

    // ---- statements

    /** The statements of {@code block}, each line indented by {@code depth} levels. */
    String statements(Block block, int depth) {
        StringBuilder sb = new StringBuilder();
        for (Statement statement : block.statements()) sb.append(statement(statement, depth));
        return sb.toString();
    }

    String statement(Statement statement, int depth) {
        String pad = INDENT.repeat(depth);
        String label = statement.label() == null ? "" : "'" + RustNames.snake(statement.label()) + ": ";
        return switch (statement) {
            case ExpressionAsStatement es -> pad + expressionStatement(es.expression(), pad) + ";\n";
            case LocalVariableCreation lvc -> localVariables(lvc, pad);
            case ReturnStatement rs -> pad + returnStatement(rs) + ";\n";
            case IfElseStatement ife -> pad + ifElse(ife, depth) + "\n";
            case WhileStatement ws -> pad + label + "while " + condition(ws.expression()) + " {\n"
                                      + statements(ws.block(), depth + 1) + pad + "}\n";
            case DoStatement ds -> pad + label + "loop {\n" + statements(ds.block(), depth + 1)
                                   + pad + INDENT + "if !(" + condition(ds.expression()) + ") {\n"
                                   + pad + INDENT + INDENT + "break;\n" + pad + INDENT + "}\n" + pad + "}\n";
            case ForEachStatement fe -> forEach(fe, label, depth);
            case ForStatement fs -> forStatement(fs, label, depth);
            case Block b -> pad + label + "{\n" + statements(b, depth + 1) + pad + "}\n";
            case ThrowStatement ts -> pad + throwStatement(ts) + ";\n";
            case BreakStatement bs -> pad + "break" + (bs.goToLabel() == null ? ""
                    : " '" + RustNames.snake(bs.goToLabel())) + ";\n";
            case ContinueStatement cs -> pad + "continue" + (cs.goToLabel() == null ? ""
                    : " '" + RustNames.snake(cs.goToLabel())) + ";\n";
            case EmptyStatement ignored -> "";
            case AssertStatement as -> pad + "assert!(" + condition(as.expression()) + ");\n";
            case TryStatement ts -> {
                context.message(RustPrintCode.TRY_BODY_ONLY, ts, null);
                yield pad + "{\n" + statements(ts.block(), depth + 1) + pad + "}\n";
            }
            case SwitchStatementNewStyle sw -> pad + label + switchNewStyle(sw, depth) + "\n";
            case SwitchStatementOldStyle sw -> pad + label + switchOldStyle(sw, depth) + "\n";
            case SynchronizedStatement ss -> {
                context.message(RustPrintCode.SYNCHRONIZED, ss, null);
                yield pad + "{\n" + statements(ss.block(), depth + 1) + pad + "}\n";
            }
            default -> pad + unsupported(statement, statement.getClass().getSimpleName()) + ";\n";
        };
    }

    private String expressionStatement(Expression e) {
        return expressionStatement(e, INDENT);
    }

    private String expressionStatement(Expression e, String pad) {
        if (e instanceof Assignment a) return assignment(a, true);
        List<MethodCall> appends = appendChain(e);
        if (appends != null) {
            // sb.append(a).append(b): Java returns the builder, Rust's push_str nothing; one statement per append
            String target = expression(appends.getFirst().object()).text;
            return appends.stream().map(mc -> RustJdk.append(target, mc.parameterExpressions().getFirst(), this))
                    .collect(Collectors.joining(";\n" + pad));
        }
        return expression(e).text;
    }

    /** {@code sb.append(a).append(b)…} on a {@code StringBuilder}, innermost first; {@code null} otherwise. */
    private static List<MethodCall> appendChain(Expression e) {
        List<MethodCall> chain = new ArrayList<>();
        Expression current = e;
        while (current instanceof MethodCall mc && "append".equals(mc.methodInfo().name())
               && mc.parameterExpressions().size() == 1
               && "java.lang.StringBuilder".equals(mc.methodInfo().typeInfo().fullyQualifiedName())) {
            chain.addFirst(mc);
            current = mc.object();
        }
        return chain.size() > 1 ? chain : null;
    }

    private String localVariables(LocalVariableCreation lvc, String pad) {
        StringBuilder sb = new StringBuilder();
        lvc.localVariableStream().forEach(lv -> sb.append(pad).append(local(lvc, lv)).append(";\n"));
        return sb.toString();
    }

    private String local(LocalVariableCreation lvc, LocalVariable lv) {
        String name = RustNames.snake(lv.simpleName());
        boolean nullable = context.method != null
                           && RustFacts.nullable(context.facts.local(context.method, lvc, lv))
                           || lv.assignmentExpression() instanceof NullConstant;
        boolean reassigned = reassigned(lv);
        Expression init = lv.assignmentExpression();
        if (init == null || init instanceof EmptyExpression) {
            nullableLocals.put(lv, nullable);
            locals.put(lv, Held.VALUE);
            String type = RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), nullable);
            return "let " + (reassigned ? "mut " : "") + name + ": " + type;
        }
        Printed value = widen(expression(init), init, lv.parameterizedType());
        nullable |= value.nullable;
        nullableLocals.put(lv, nullable);
        boolean mutated = mutated(lv);
        String text = value.text;
        Held held = value.held;
        if (held == Held.PLACE && (RustTypes.isString(lv.parameterizedType())
                                   || RustTypes.isImmutableValue(lv.parameterizedType(), context.facts))) {
            // an immutable value: the local is a copy, so that the field may be assigned while it lives
            text = text + ".clone()";
            held = Held.VALUE;
        } else if (held == Held.SHARED && RustTypes.isString(lv.parameterizedType()) && !(init instanceof NullConstant)
                   && !(CstQueries.unwrap(init) instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo)) {
            // a string borrowed out of a lookup ('(String) b.get("sku")'): a copy, which ends the borrow of what holds it
            text = text + (value.nullable ? ".cloned()" : ".clone()");
            held = Held.VALUE;
        } else if (held == Held.PLACE && !RustTypes.isCopy(lv.parameterizedType())) {
            // an element or a field: the local refers to it, as Java's reference does
            text = (mutated ? "&mut " : "&") + text;
            held = mutated ? Held.EXCLUSIVE : Held.SHARED;
        } else if (held == Held.PLACE) {
            held = Held.VALUE;
        }
        if (mutated && held == Held.SHARED && text.contains(".get(")) {
            // map.get(k) modified through the local: the mutable lookup
            int at = text.lastIndexOf(".get(");
            text = text.substring(0, at) + ".get_mut(" + text.substring(at + ".get(".length());
            held = Held.EXCLUSIVE;
        }
        if (nullable && !value.nullable && !(init instanceof NullConstant)) text = "Some(" + text + ")";
        locals.put(lv, held);
        // an Option is unwrapped with as_mut() to modify what it holds, which needs a mutable binding
        boolean mut = reassigned || mutated && (held == Held.VALUE || nullable);
        String type = init instanceof NullConstant
                ? ": " + RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), true)
                : mentionsObject(lv.parameterizedType()) && held == Held.VALUE
                ? ": " + RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), nullable) : "";
        return "let " + (mut ? "mut " : "") + name + type + " = " + text;
    }

    private String returnStatement(ReturnStatement rs) {
        Expression e = rs.expression();
        if (e == null || e instanceof EmptyExpression) return "return";
        MethodInfo method = context.method;
        boolean nullable = RustFacts.nullable(context.facts.returnType(method));
        boolean borrows = context.facts.resultBorrowsReceiver(method)
                          && context.facts.receiver(method) != Receiver.NONE;
        Printed value = widen(expression(e), e, method.returnType());
        String text;
        if (RustTypes.isCopy(method.returnType())) {
            text = value.text;
        } else if (borrows) {
            text = adapt(value, RustTypes.isString(method.returnType()) ? Held.STR : Held.SHARED);
        } else {
            text = owned(value, method.returnType());
        }
        if (nullable && !value.nullable && !(e instanceof NullConstant)) text = "Some(" + text + ")";
        if (!nullable && value.nullable) {
            // the verdict says the result is never null: Java's value is used as it is, Rust's unwrapped
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, value.text);
            text = text + ".unwrap()";
        }
        return "return " + text;
    }

    private String ifElse(IfElseStatement ife, int depth) {
        String pad = INDENT.repeat(depth);
        StringBuilder sb = new StringBuilder("if " + condition(ife.expression()) + " {\n"
                                             + statements(ife.block(), depth + 1) + pad + "}");
        Block elseBlock = ife.elseBlock();
        if (elseBlock != null && !elseBlock.statements().isEmpty()) {
            if (elseBlock.statements().size() == 1 && elseBlock.statements().getFirst() instanceof IfElseStatement nested) {
                sb.append(" else ").append(ifElse(nested, depth));
            } else {
                sb.append(" else {\n").append(statements(elseBlock, depth + 1)).append(pad).append("}");
            }
        }
        return sb.toString();
    }

    private String forEach(ForEachStatement fe, String label, int depth) {
        String pad = INDENT.repeat(depth);
        LocalVariable lv = fe.initializer().localVariable();
        String name = RustNames.snake(lv.simpleName());
        Printed collection = expression(fe.expression());
        boolean mutated = mutated(lv);
        String iterator;
        if (RustJdk.isIterator(collection.text)) {
            iterator = collection.text;
        } else {
            iterator = collection.text + (mutated ? ".iter_mut()" : ".iter()");
        }
        boolean copy = RustTypes.isCopy(lv.parameterizedType());
        locals.put(lv, copy ? Held.VALUE : mutated ? Held.EXCLUSIVE : Held.SHARED);
        String pattern = copy && !mutated ? "&" + name : name;
        return pad + label + "for " + pattern + " in " + iterator + " {\n" + statements(fe.block(), depth + 1)
               + pad + "}\n";
    }

    /** {@code for (int i = a; i < b; i++)} as {@code for i in a..b}; anything else as a {@code while}. */
    private String forStatement(ForStatement fs, String label, int depth) {
        String pad = INDENT.repeat(depth);
        String range = range(fs);
        if (range != null) {
            return pad + label + "for " + range + " {\n" + statements(fs.block(), depth + 1) + pad + "}\n";
        }
        StringBuilder sb = new StringBuilder(pad + "{\n");
        for (Element init : fs.initializers()) {
            if (init instanceof LocalVariableCreation lvc) sb.append(localVariables(lvc, pad + INDENT));
            else if (init instanceof Expression e) sb.append(pad).append(INDENT).append(expressionStatement(e)).append(";\n");
        }
        if (containsContinue(fs.block())) context.message(RustPrintCode.UNSUPPORTED, fs, "continue in a for loop");
        sb.append(pad).append(INDENT).append(label).append("while ").append(condition(fs.expression())).append(" {\n");
        sb.append(statements(fs.block(), depth + 2));
        for (Expression update : fs.updaters()) {
            sb.append(pad).append(INDENT).append(INDENT).append(expressionStatement(update)).append(";\n");
        }
        return sb.append(pad).append(INDENT).append("}\n").append(pad).append("}\n").toString();
    }

    private String range(ForStatement fs) {
        if (fs.initializers().size() != 1 || fs.updaters().size() != 1) return null;
        if (!(fs.initializers().getFirst() instanceof LocalVariableCreation lvc)) return null;
        LocalVariable i = lvc.localVariable();
        if (!lvc.otherLocalVariables().isEmpty() || i.assignmentExpression() == null) return null;
        if (!(fs.updaters().getFirst() instanceof Assignment a) || !isIncrement(a, i)) return null;
        if (!(fs.expression() instanceof BinaryOperator bo) || bo.operator() == null) return null;
        if (!(bo.lhs() instanceof VariableExpression ve) || !i.equals(ve.variable())) return null;
        String op = bo.operator().name();
        if (!"<".equals(op) && !"<=".equals(op)) return null;
        if (assignedIn(fs.block(), i)) return null;
        locals.put(i, Held.VALUE);
        return RustNames.snake(i.simpleName()) + " in " + expression(i.assignmentExpression()).text
               + ("<".equals(op) ? ".." : "..=") + operand(bo.rhs());
    }

    private static boolean isIncrement(Assignment a, LocalVariable i) {
        if (!i.equals(a.variableTarget())) return false;
        if (a.prefixPrimitiveOperator() != null) return a.assignmentOperatorIsPlus();
        return a.assignmentOperator() != null && "+=".equals(a.assignmentOperator().name())
               && a.value() instanceof IntConstant ic && ic.constant() == 1;
    }

    private String throwStatement(ThrowStatement ts) {
        context.message(RustPrintCode.THROW_AS_PANIC, ts, null);
        if (ts.expression() instanceof ConstructorCall cc && cc.constructor() != null) {
            String exception = cc.constructor().typeInfo().simpleName();
            if (cc.parameterExpressions().isEmpty()) return "panic!(\"" + exception + "\")";
            return "panic!(\"" + exception + ": {}\", " + expression(cc.parameterExpressions().getFirst()).text + ")";
        }
        return "panic!(\"{:?}\", " + expression(ts.expression()).text + ")";
    }

    private String condition(Expression e) {
        return expression(e).text;
    }

    // ---- equals as PartialEq

    /**
     * The body of {@code equals(Object o)} as {@code eq(&self, other: &Self)}. Rust's types already guarantee what
     * the usual preamble tests ({@code this == o}, {@code o == null}, {@code getClass()}, {@code instanceof}): those
     * statements are left out, and {@code o}, its pattern variable and its cast are {@code other}.
     */
    String equalsBody(MethodInfo equals, int depth) {
        ParameterInfo o = equals.parameters().getFirst();
        aliases.put(o, "other");
        StringBuilder sb = new StringBuilder();
        for (Statement s : equals.methodBody().statements()) {
            if (s instanceof IfElseStatement ife && preamble(ife, o)) continue;
            if (s instanceof IfElseStatement ife && ife.expression() instanceof InstanceOf io && pattern(io, o)) {
                // if (o instanceof T t) { return …; } return false;
                sb.append(statements(ife.block(), depth));
                continue;
            }
            if (s instanceof LocalVariableCreation lvc && lvc.otherLocalVariables().isEmpty()
                && CstQueries.unwrap(lvc.localVariable().assignmentExpression()) instanceof Cast c
                && isVariable(c.expression(), o)) {
                aliases.put(lvc.localVariable(), "other");
                continue;
            }
            sb.append(statement(s, depth));
        }
        return sb.toString();
    }

    /** An {@code if} without else whose block only returns a constant, and whose test only looks at identity and type. */
    private boolean preamble(IfElseStatement ife, ParameterInfo o) {
        if (ife.elseBlock() != null && !ife.elseBlock().statements().isEmpty()) return false;
        List<Statement> then = ife.block().statements();
        if (then.size() != 1 || !(then.getFirst() instanceof ReturnStatement rs)
            || !(rs.expression() instanceof BooleanConstant)) {
            return false;
        }
        boolean[] other = {false};
        ife.expression().visit(e -> {
            switch (e) {
                case InstanceOf io -> pattern(io, o);
                case MethodCall mc when !"getClass".equals(mc.methodInfo().name()) -> other[0] = true;
                case VariableExpression ve when !(ve.variable() instanceof This) && !o.equals(ve.variable()) ->
                        other[0] = true;
                default -> {
                }
            }
            return !other[0];
        });
        return !other[0];
    }

    /** {@code o instanceof T t}: {@code t} is {@code other}. */
    private boolean pattern(InstanceOf io, ParameterInfo o) {
        if (!isVariable(io.expression(), o)) return false;
        if (io.patternVariable() != null && io.patternVariable().localVariable() != null) {
            aliases.put(io.patternVariable().localVariable(), "other");
        }
        return true;
    }

    private static boolean isVariable(Expression e, Variable v) {
        return CstQueries.unwrap(e) instanceof VariableExpression ve && v.equals(ve.variable());
    }

    // ---- switch as match

    /** {@code match selector { A | B => { … } _ => {} }}: a switch statement does nothing for a missing case. */
    private String switchNewStyle(SwitchStatementNewStyle sw, int depth) {
        String pad = INDENT.repeat(depth);
        StringBuilder sb = new StringBuilder("match " + selector(sw.expression()) + " {\n");
        List<Expression> labels = new ArrayList<>();
        boolean hasDefault = false;
        for (SwitchEntry entry : sw.entries()) {
            if (entry.patternVariable() != null) return unsupported(entry, "pattern in a switch") + ";";
            boolean isDefault = isDefault(entry.conditions());
            hasDefault |= isDefault;
            entry.conditions().stream().filter(c -> !c.isEmpty()).forEach(labels::add);
            String head = isDefault ? "_" : caseLabels(entry.conditions());
            List<Statement> body = entry.statement() instanceof Block b ? CstQueries.own(b.statements())
                    : List.of(entry.statement());
            sb.append(pad).append(INDENT).append(head).append(" => ").append(arm(withoutFinalBreak(body), depth + 1));
        }
        if (!hasDefault && !exhaustive(sw.expression(), labels)) sb.append(pad).append(INDENT).append("_ => {}\n");
        return sb.append(pad).append("}").toString();
    }

    /**
     * Java's groups of labels, each with its statements up to the next group; a group that does not end in a jump
     * falls through, and gets the following groups' statements copied up to the first that does.
     */
    private String switchOldStyle(SwitchStatementOldStyle sw, int depth) {
        String pad = INDENT.repeat(depth);
        List<CstQueries.SwitchGroup> groups = CstQueries.switchGroups(sw);
        StringBuilder sb = new StringBuilder("match " + selector(sw.expression()) + " {\n");
        List<Expression> labels = new ArrayList<>();
        boolean hasDefault = false;
        for (int i = 0; i < groups.size(); i++) {
            List<Statement> arm = new ArrayList<>(groups.get(i).own());
            for (int j = i + 1; j < groups.size() && !endsInJump(arm); j++) arm.addAll(groups.get(j).own());
            List<Expression> conditions = groups.get(i).labels().stream()
                    .map(SwitchStatementOldStyle.SwitchLabel::literal).toList();
            boolean isDefault = conditions.stream().anyMatch(c -> c == null || c.isEmpty());
            hasDefault |= isDefault;
            if (!isDefault) labels.addAll(conditions);
            String head = isDefault ? "_" : caseLabels(conditions);
            sb.append(pad).append(INDENT).append(head).append(" => ").append(arm(withoutFinalBreak(arm), depth + 1));
        }
        if (!hasDefault && !exhaustive(sw.expression(), labels)) sb.append(pad).append(INDENT).append("_ => {}\n");
        return sb.append(pad).append("}").toString();
    }

    /** The statements of an arm, in braces. A {@code break} inside would leave the enclosing loop in Rust. */
    private String arm(List<Statement> body, int depth) {
        String pad = INDENT.repeat(depth);
        StringBuilder sb = new StringBuilder("{\n");
        for (Statement s : body) {
            if (breaksOutOfSwitch(s)) context.message(RustPrintCode.UNSUPPORTED, s, "break inside a switch arm");
            sb.append(statement(s, depth + 1));
        }
        return sb.append(pad).append("}\n").toString();
    }

    /** {@code match selector { A => value, … }}: Java's switch expression is exhaustive. */
    private Printed switchExpression(SwitchExpression se) {
        int depth = 2;
        String pad = INDENT.repeat(depth);
        StringBuilder sb = new StringBuilder("match " + selector(se.selector()) + " {\n");
        List<Expression> labels = new ArrayList<>();
        boolean hasDefault = false;
        for (SwitchEntry entry : se.entries()) {
            if (entry.patternVariable() != null) return value(unsupported(entry, "pattern in a switch"));
            boolean isDefault = isDefault(entry.conditions());
            hasDefault |= isDefault;
            entry.conditions().stream().filter(c -> !c.isEmpty()).forEach(labels::add);
            String head = isDefault ? "_" : caseLabels(entry.conditions());
            sb.append(pad).append(INDENT).append(head).append(" => ")
                    .append(armValue(entry.statement(), se.parameterizedType(), depth + 1)).append(",\n");
        }
        if (!hasDefault && !exhaustive(se.selector(), labels)) {
            sb.append(pad).append(INDENT).append("_ => unreachable!(),\n");
        }
        return value(sb.append(pad).append("}").toString());
    }

    /** The value of an arm of a switch expression: its expression, or a block whose last statement yields. */
    private String armValue(Statement s, ParameterizedType type, int depth) {
        switch (s) {
            case ExpressionAsStatement es -> {
                return armExpression(es.expression(), type);
            }
            case YieldStatement ys -> {
                return armExpression(ys.expression(), type);
            }
            case ThrowStatement ts -> {
                return throwStatement(ts);
            }
            case Block b -> {
                List<Statement> body = CstQueries.own(b.statements());
                if (body.size() == 1) return armValue(body.getFirst(), type, depth);
                String pad = INDENT.repeat(depth);
                StringBuilder sb = new StringBuilder("{\n");
                for (int i = 0; i < body.size(); i++) {
                    Statement st = body.get(i);
                    if (i == body.size() - 1 && st instanceof YieldStatement ys) {
                        sb.append(pad).append(INDENT).append(armExpression(ys.expression(), type)).append("\n");
                    } else {
                        sb.append(statement(st, depth + 1));
                    }
                }
                return sb.append(pad).append("}").toString();
            }
            default -> {
                return unsupported(s, s.getClass().getSimpleName());
            }
        }
    }

    private String armExpression(Expression e, ParameterizedType type) {
        Printed p = widen(expression(e), e, type);
        return RustTypes.isCopy(type) ? p.text : owned(p, type);
    }

    /** The selector: a string is matched as a {@code &str}, a nullable value unwrapped as Java would throw. */
    private String selector(Expression selector) {
        String text = operand(selector);
        return RustTypes.isString(selector.parameterizedType()) ? text + ".as_str()" : text;
    }

    private String caseLabels(List<Expression> conditions) {
        return conditions.stream().filter(c -> c != null && !c.isEmpty()).map(this::caseLabel)
                .collect(Collectors.joining(" | "));
    }

    /** A case label: an enum constant, written unqualified in Java, is the variant's path in Rust. */
    private String caseLabel(Expression c) {
        if (c instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr && fr.fieldInfo().isStatic()
            && fr.fieldInfo().owner().typeNature().isEnum()) {
            return field(fr).text;
        }
        return expression(c).text;
    }

    private static boolean isDefault(List<Expression> conditions) {
        return conditions.isEmpty() || conditions.stream().anyMatch(Expression::isEmpty);
    }

    /** Rust wants every value matched; a switch over all the constants of an enum needs no wildcard. */
    private static boolean exhaustive(Expression selector, List<Expression> labels) {
        TypeInfo ti = selector.parameterizedType().typeInfo();
        if (ti == null || !ti.typeNature().isEnum()) return false;
        long constants = ti.fields().stream().filter(f -> f.isStatic() && f.type().typeInfo() == ti).count();
        return labels.size() >= constants;
    }

    private static List<Statement> withoutFinalBreak(List<Statement> statements) {
        if (!statements.isEmpty() && statements.getLast() instanceof BreakStatement bs && bs.goToLabel() == null) {
            return statements.subList(0, statements.size() - 1);
        }
        return statements;
    }

    private static boolean endsInJump(List<Statement> statements) {
        if (statements.isEmpty()) return false;
        Statement last = statements.getLast();
        return last instanceof BreakStatement || last instanceof ContinueStatement || last instanceof ReturnStatement
               || last instanceof ThrowStatement || last instanceof Block b && endsInJump(b.statements());
    }

    /** An unlabeled {@code break} that leaves the switch, not a loop inside the arm. */
    private static boolean breaksOutOfSwitch(Statement s) {
        boolean[] found = {false};
        s.visit(e -> {
            if (e instanceof BreakStatement bs && bs.goToLabel() == null) found[0] = true;
            return !found[0] && !(e instanceof LoopStatement) && !(e instanceof SwitchStatementNewStyle)
                   && !(e instanceof SwitchStatementOldStyle) && !(e instanceof Lambda);
        });
        return found[0];
    }

    // ---- expressions

    private Printed expression(Expression e) {
        return switch (e) {
            case IntConstant ic -> value(Integer.toString(ic.constant()));
            case LongConstant lc -> value(lc.constant() + "_i64");
            case ShortConstant sc -> value(sc.constant() + "_i16");
            case ByteConstant bc -> value(bc.constant() + "_i8");
            case DoubleConstant dc -> value(doubleLiteral(dc.constant()));
            case FloatConstant fc -> value(doubleLiteral(fc.constant()) + "_f32");
            case BooleanConstant bc -> value(Boolean.toString(bc.constant()));
            case CharConstant cc -> value(charLiteral(cc.constant()));
            case StringConstant sc -> new Printed(stringLiteral(sc.constant()), Held.STR, false);
            case NullConstant ignored -> new Printed("None", Held.VALUE, true);
            case EnclosedExpression ee -> {
                Printed inner = expression(ee.inner());
                yield new Printed("(" + inner.text + ")", inner.held, inner.nullable);
            }
            case VariableExpression ve -> variable(ve.variable(), ve);
            case Assignment a -> value(assignment(a, false));
            case InlineConditional ic -> inlineConditional(ic);
            case MethodCall mc -> methodCall(mc);
            case ConstructorCall cc -> constructorCall(cc);
            case BinaryOperator bo when bo.operator() != null -> binaryOperator(bo);
            case UnaryOperator uo when uo.operator() != null -> unaryOperator(uo);
            case Cast c -> cast(c);
            case ArrayLength al -> value(expression(al.scope()).text + ".len() as i32");
            case Lambda lambda -> lambda(lambda);
            case SwitchExpression se -> switchExpression(se);
            case And and -> value(and.expressions().stream().map(this::operand).collect(Collectors.joining(" && ")));
            case Or or -> value(or.expressions().stream().map(this::operand).collect(Collectors.joining(" || ")));
            case TypeExpression te -> value(RustTypes.owned(te.parameterizedType(), context, te));
            case MethodReference mr -> methodReference(mr);
            default -> value(unsupported(e, e.getClass().getSimpleName()));
        };
    }

    private static Printed value(String text) {
        return new Printed(text, Held.VALUE, false);
    }

    private Printed variable(Variable variable, Element where) {
        String alias = aliases.get(variable);
        if (alias != null) return new Printed(alias, Held.SHARED, false);
        return switch (variable) {
            case This ignored -> {
                Receiver receiver = context.method == null ? Receiver.EXCLUSIVE : context.facts.receiver(context.method);
                yield new Printed(context.receiver, context.receiver.equals("this") ? Held.VALUE
                        : receiver == Receiver.SHARED ? Held.SHARED : receiver == Receiver.OWNED ? Held.VALUE
                        : Held.EXCLUSIVE, false);
            }
            case ParameterInfo pi -> parameter(pi);
            case LocalVariable lv -> new Printed(RustNames.snake(lv.simpleName()), locals.getOrDefault(lv, Held.VALUE),
                    nullableLocals.getOrDefault(lv, false));
            case FieldReference fr -> field(fr);
            case DependentVariable dv -> {
                if (dv.arrayExpression() == null) yield value(unsupported(where, "array element"));
                yield new Printed(expression(dv.arrayExpression()).text + "[" + index(dv.indexExpression()) + "]",
                        Held.PLACE, false);
            }
            default -> value(unsupported(where, variable.getClass().getSimpleName()));
        };
    }

    private Printed parameter(ParameterInfo pi) {
        String name = RustNames.snake(pi.name());
        boolean nullable = RustFacts.nullable(context.facts.parameter(pi));
        if (RustTypes.isCopy(pi.parameterizedType())) return new Printed(name, Held.VALUE, nullable);
        // Rust hands a closure the element ('map' over '.cloned()') or a reference to it ('filter'): a place, which
        // '&x' borrows and '.clone()' copies either way (deref coercion, auto-deref)
        if (closures.contains(pi.methodInfo())) return new Printed(name, Held.PLACE, nullable);
        Passing passing = context.facts.passing(pi);
        Held held = switch (passing) {
            case SHARED -> RustTypes.isString(pi.parameterizedType()) ? Held.STR : Held.SHARED;
            case EXCLUSIVE -> Held.EXCLUSIVE;
            case OWNED -> Held.VALUE;
        };
        return new Printed(name, held, nullable);
    }

    private Printed field(FieldReference fr) {
        FieldInfo f = fr.fieldInfo();
        boolean nullable = RustFacts.nullable(context.facts.field(f));
        if (f.isStatic()) {
            TypeInfo owner = f.owner();
            String jdk = RustJdk.constant(f);
            if (jdk != null) return value(jdk);
            String prefix = owner == context.type ? "Self" : RustNames.type(owner);
            if (owner != context.type && !owner.compilationUnit().externalLibrary()) {
                String module = RustNames.module(owner.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(owner));
            }
            if (owner.typeNature().isEnum() && f.type().typeInfo() == owner) {
                return value(prefix + "::" + RustNames.variant(f.name()));
            }
            return new Printed(prefix + "::" + RustNames.constant(f.name()), Held.VALUE, nullable);
        }
        boolean own = fr.scopeIsThis() || fr.isDefaultScope();
        String scope = own ? context.receiver : dereference(expression(fr.scope()));
        TypeInfo scopeType = own ? context.type : fr.scope().parameterizedType().typeInfo();
        // an inherited field is in the base
        String path = scopeType == null ? "" : RustTypePrinter.basePath(scopeType, f.owner());
        return new Printed(scope + path + "." + RustNames.snake(f.name()), Held.PLACE, nullable);
    }

    /** A nullable value dereferenced: Java would throw a NullPointerException, Rust panics in {@code unwrap()}. */
    private String dereference(Printed scope) {
        if (!scope.nullable) return scope.text;
        context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, null, scope.text);
        return scope.text + ".as_ref().unwrap()";
    }

    private String assignment(Assignment a, boolean statement) {
        String target = expression(a.target()).text;
        if (a.prefixPrimitiveOperator() != null || a.binaryOperator() != null && a.value() == null) {
            String op = a.assignmentOperatorIsPlus() ? " += 1" : " -= 1";
            if (statement) return target + op;
            // i++ as a value: a block that increments and gives the old value (++i: the new one)
            if (Boolean.TRUE.equals(a.prefixPrimitiveOperator())) return "{ " + target + op + "; " + target + " }";
            return "{ let v = " + target + "; " + target + op + "; v }";
        }
        String operator = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        Printed value = widen(expression(a.value()), a.value(), a.target().parameterizedType());
        if (!statement) return unsupported(a, "assignment inside an expression");
        ParameterizedType targetType = a.target().parameterizedType();
        if ("+=".equals(operator) && RustTypes.isString(targetType)) {
            return target + ".push_str(" + adapt(value, Held.STR) + ")";
        }
        String text = RustTypes.isCopy(targetType) ? value.text : owned(value, targetType);
        if (targetNullable(a.variableTarget()) && !value.nullable && !(a.value() instanceof NullConstant)) {
            text = "Some(" + text + ")";
        }
        return target + " " + operator + " " + text;
    }

    private boolean targetNullable(Variable v) {
        return switch (v) {
            case FieldReference fr -> RustFacts.nullable(context.facts.field(fr.fieldInfo()));
            case LocalVariable lv -> nullableLocals.getOrDefault(lv, false);
            case ParameterInfo pi -> RustFacts.nullable(context.facts.parameter(pi));
            default -> false;
        };
    }

    private Printed binaryOperator(BinaryOperator bo) {
        String op = bo.operator().name();
        if ("+".equals(op) && RustTypes.isString(bo.parameterizedType())) return stringConcat(bo);
        if (("==".equals(op) || "!=".equals(op))
            && (bo.lhs() instanceof NullConstant || bo.rhs() instanceof NullConstant)) {
            Expression other = bo.lhs() instanceof NullConstant ? bo.rhs() : bo.lhs();
            Printed p = expression(other);
            if (!p.nullable && !(other instanceof NullConstant) && !other.parameterizedType().isPrimitiveExcludingVoid()) {
                // a defensive check of a value the verdicts call non-null: its outcome under them
                context.message(RustPrintCode.NULL_CHECK_OF_NON_NULL, bo, p.text);
                return value("==".equals(op) ? "false" : "true");
            }
            return value(p.text + ("==".equals(op) ? ".is_none()" : ".is_some()"));
        }
        if (">>>".equals(op)) {
            context.message(RustPrintCode.UNSIGNED_SHIFT, bo, null);
            String type = RustTypes.primitive(bo.parameterizedType().typeInfo());
            return value("(((" + operand(bo.lhs()) + ") as u" + type.substring(1) + ") >> " + operand(bo.rhs())
                         + ") as " + type);
        }
        ParameterizedType common = promoted(bo, op);
        String lhs = promotedOperand(bo.lhs(), common);
        // 'n as i32 < 3' reads as the start of generic arguments
        if (op.startsWith("<") && AS_CAST.matcher(lhs).find()) lhs = "(" + lhs + ")";
        return value(lhs + " " + op + " " + promotedOperand(bo.rhs(), common));
    }

    /**
     * The type Java computes an arithmetic operation or comparison in (JLS 5.6.2): the operation's own type for
     * arithmetic, the wider operand's for a comparison. {@code null} when it is not numeric.
     */
    private static ParameterizedType promoted(BinaryOperator bo, String op) {
        boolean comparison = switch (op) {
            case "<", "<=", ">", ">=", "==", "!=" -> true;
            default -> false;
        };
        if (!comparison) return numeric(bo.parameterizedType()) ? bo.parameterizedType() : null;
        ParameterizedType l = bo.lhs().parameterizedType();
        ParameterizedType r = bo.rhs().parameterizedType();
        if (!numeric(l) || !numeric(r)) return null;
        return rank(l) >= rank(r) ? l : r;
    }

    private String promotedOperand(Expression e, ParameterizedType common) {
        if (common == null) return operand(e);
        Printed p = widen(new Printed(operand(e), Held.VALUE, false), e, common);
        return p.text;
    }

    /**
     * Java's implicit widening of a primitive ({@code int} to {@code long} or {@code double}, …), which Rust spells
     * out: an integer literal gets the target's form, anything else an {@code as}.
     */
    private Printed widen(Printed p, Expression e, ParameterizedType target) {
        ParameterizedType from = e.parameterizedType();
        if (!numeric(from) || !numeric(target) || rank(from) >= rank(target)) return p;
        String to = primitive(target);
        Expression inner = CstQueries.unwrap(e);
        if (inner instanceof IntConstant ic) {
            String literal = Integer.toString(ic.constant());
            if (to.startsWith("f")) return new Printed(literal + ".0", p.held, p.nullable);
            return new Printed(literal + "_" + to, p.held, p.nullable);
        }
        String text = isName(p.text) ? p.text : "(" + p.text + ")";
        return new Printed(text + " as " + to, p.held, p.nullable);
    }

    /** The Rust primitive of a Java primitive or its box; {@code null} for any other type. */
    private static String primitive(ParameterizedType type) {
        if (type == null || type.arrays() > 0 || type.typeInfo() == null) return null;
        TypeInfo ti = type.typeInfo();
        if (ti.isPrimitiveExcludingVoid()) return RustTypes.primitive(ti);
        return ti.isBoxedExcludingVoid() ? RustTypes.mapped(ti) : null;
    }

    private static boolean numeric(ParameterizedType type) {
        String p = primitive(type);
        return p != null && !"bool".equals(p);
    }

    /** Java's widening order: byte, short and char, int, long, float, double. */
    private static int rank(ParameterizedType type) {
        return switch (primitive(type)) {
            case "f64" -> 5;
            case "f32" -> 4;
            case "i64" -> 3;
            case "i32" -> 2;
            case null -> -1;
            default -> 1;
        };
    }

    /** An operand of an operator: a borrowed value compares and computes as the value itself. */
    private static final java.util.regex.Pattern AS_CAST = java.util.regex.Pattern.compile(" as [\\w:<>]+$");

    private static boolean isObject(ParameterizedType type) {
        return type.typeInfo() != null && type.typeInfo().isJavaLangObject() && type.arrays() == 0;
    }

    /** Whether {@code text} has an operator outside parentheses, brackets, braces, closures and string literals. */
    static boolean topLevelOperator(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
                continue;
            }
            switch (c) {
                case '"' -> inString = true;
                case '(', '[', '{' -> depth++;
                case ')', ']', '}' -> depth--;
                case '|' -> {
                    if (depth == 0) return true;
                }
                case ' ' -> {
                    if (depth == 0 && i + 1 < text.length() && "=!<>&|+-*/%^".indexOf(text.charAt(i + 1)) >= 0) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }
        return false;
    }

    private String operand(Expression e) {
        Printed p = expression(e);
        boolean binary = e instanceof BinaryOperator || e instanceof InlineConditional || e instanceof Assignment
                         || e instanceof And || e instanceof Or || topLevelOperator(p.text);
        String text = binary ? "(" + p.text + ")" : p.text;
        if (p.nullable) {
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
            return text + ".unwrap()";
        }
        return text;
    }

    /** Both branches of the same type: when one may be null, the other is wrapped in {@code Some}. */
    private Printed inlineConditional(InlineConditional ic) {
        boolean string = RustTypes.isString(ic.parameterizedType());
        Printed pt = expression(ic.ifTrue());
        Printed pf = expression(ic.ifFalse());
        String t = string ? owned(pt, ic.parameterizedType()) : pt.text;
        String f = string ? owned(pf, ic.parameterizedType()) : pf.text;
        boolean nullable = pt.nullable || pf.nullable;
        if (nullable && !pt.nullable) t = "Some(" + t + ")";
        if (nullable && !pf.nullable) f = "Some(" + f + ")";
        return new Printed("if " + condition(ic.condition()) + " { " + t + " } else { " + f + " }", Held.VALUE, nullable);
    }

    private Printed stringConcat(BinaryOperator bo) {
        List<Expression> parts = new ArrayList<>();
        flattenConcat(bo, parts);
        StringBuilder format = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (Expression part : parts) {
            if (part instanceof StringConstant sc) {
                format.append(sc.constant().replace("{", "{{").replace("}", "}}"));
            } else {
                String[] d = display(part);
                format.append(d[0]);
                args.add(d[1]);
            }
        }
        return value("format!(" + stringLiteral(format.toString())
                     + (args.isEmpty() ? "" : ", " + String.join(", ", args)) + ")");
    }

    /**
     * A value in a format string, as Java's string conversion prints it as far as Rust can: {@code "null"} for a
     * null, an enum's name, Debug for what has no Display (a collection, a SystemTime), a class's address when it
     * has no {@code toString()}. {@code {spec, text}}.
     */
    String[] display(Expression e) {
        Printed p = expression(e);
        ParameterizedType type = e.parameterizedType();
        if (p.nullable) {
            return new String[]{"{}", p.text + ".as_ref().map(|v| v.to_string()).unwrap_or_else(|| \"null\".to_string())"};
        }
        TypeInfo ti = type.typeInfo();
        if (isObject(type)) return new String[]{"{}", "crate::jrt::to_string(" + adapt(p, Held.SHARED) + ")"};
        if (ti == null || type.isPrimitiveExcludingVoid()) return new String[]{"{}", p.text};
        if (type.arrays() > 0) return new String[]{"{:?}", p.text};
        if (ti.typeNature().isEnum() && RustTypePrinter.plainEnum(ti)) return new String[]{"{}", p.text + ".name()"};
        String mapped = RustTypes.mapped(ti);
        if (mapped != null) {
            boolean debug = java.util.Set.of("Vec", "HashSet", "BTreeSet", "HashMap", "BTreeMap", "VecDeque", "Option",
                    "SystemTime").contains(mapped);
            return new String[]{debug ? "{:?}" : "{}", p.text};
        }
        if (!ti.compilationUnit().externalLibrary() && !ti.isInterface()) {
            if (ti.typeNature().isRecord()) return new String[]{"{:?}", p.text};
            if (!RustTypePrinter.hasToString(ti)) return new String[]{"{:p}", adapt(p, Held.SHARED)};
        }
        return new String[]{"{}", p.text};
    }

    private static void flattenConcat(Expression e, List<Expression> parts) {
        if (e instanceof BinaryOperator bo && bo.operator() != null && "+".equals(bo.operator().name())
            && RustTypes.isString(bo.parameterizedType())) {
            flattenConcat(bo.lhs(), parts);
            flattenConcat(bo.rhs(), parts);
        } else {
            parts.add(e instanceof EnclosedExpression ee ? ee.inner() : e);
        }
    }

    private Printed unaryOperator(UnaryOperator uo) {
        String op = uo.operator().name();
        String operand = operand(uo.expression());
        return value(("~".equals(op) ? "!" : op) + operand);
    }

    private Printed cast(Cast c) {
        ParameterizedType to = c.parameterizedType();
        Printed inner = expression(c.expression());
        ParameterizedType from = c.expression().parameterizedType();
        if (from.typeInfo() != null && from.typeInfo().isJavaLangObject() && from.arrays() == 0) {
            // a Box<dyn Any>: downcast, a panic where Java throws
            context.message(RustPrintCode.DOWNCAST, c, to.fullyQualifiedName());
            String target = RustTypes.owned(to, context, c);
            String text = inner.text + (inner.nullable ? ".unwrap()" : "") + ".downcast_ref::<" + target + ">().unwrap()";
            return RustTypes.isCopy(to) ? value("*" + text) : new Printed(text, Held.SHARED, false);
        }
        if (to.isPrimitiveExcludingVoid() && to.arrays() == 0) {
            if (to.typeInfo().isChar()) {
                return value("char::from_u32((" + inner.text + ") as u32).unwrap()");
            }
            return value("(" + inner.text + ") as " + RustTypes.primitive(to.typeInfo()));
        }
        context.message(RustPrintCode.REFERENCE_CAST, c, to.fullyQualifiedName());
        return inner;
    }

    private Printed lambda(Lambda lambda) {
        MethodInfo m = lambda.methodInfo();
        String params = m.parameters().stream().map(p -> RustNames.snake(p.name())).collect(Collectors.joining(", "));
        Expression single = lambda.singleExpression();
        MethodInfo outer = context.method;
        closures.add(m);
        try {
            context.method = m;
            if (single != null) {
                // a Consumer's body is a statement: 'map.put(k, v)' returns a value Rust's 'for_each' does not take
                String body = expression(single).text;
                return value("|" + params + "| " + (m.noReturnValue() ? "{ " + body + "; }" : body));
            }
            return value("|" + params + "| {\n" + statements(m.methodBody(), 1) + "}");
        } finally {
            context.method = outer;
        }
    }

    /**
     * A method reference as a closure: {@code Order::customer} is {@code |x| x.customer()}, {@code Type::of} is
     * {@code |x| Type::of(x)}, {@code this::add} is {@code |x| self.add(x)}.
     */
    private Printed methodReference(MethodReference mr) {
        MethodInfo m = mr.methodInfo();
        int n = mr.concreteParameterTypes().size();
        List<String> params = new ArrayList<>();
        for (int i = 0; i < n; i++) params.add(n == 1 ? "x" : "x" + (i + 1));
        String closure = "|" + String.join(", ", params) + "| ";
        if (m.typeInfo().compilationUnit().externalLibrary()) {
            String jdk = RustJdk.reference(m, params);
            // an entry's key or value is a reference into the map: a copy, or a clone for a string
            if (jdk != null && (jdk.endsWith(".0") || jdk.endsWith(".1"))) {
                ParameterizedType result = mr.concreteReturnType();
                if (RustTypes.isCopy(result)) jdk = "*" + jdk;
                else if (RustTypes.isString(result) || RustTypes.isImmutableValue(result, context.facts)) jdk += ".clone()";
            }
            if (jdk != null) return value(closure + jdk);
            return value(unsupported(mr, "method reference " + m.fullyQualifiedName()));
        }
        String owner = m.typeInfo() == context.type ? "Self" : RustNames.type(m.typeInfo());
        if (m.typeInfo() != context.type) {
            String module = RustNames.module(m.typeInfo().packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(m.typeInfo()));
        }
        String name = RustNames.method(m);
        if (m.isConstructor() || m.isStatic()) {
            return value(closure + owner + "::" + name + "(" + passed(params, m) + ")");
        }
        if (mr.scope() instanceof TypeExpression) {
            // unbound: the first parameter is the object
            if (params.isEmpty()) return value(unsupported(mr, "method reference without parameters"));
            return value(closure + params.getFirst() + "." + name + "(" + passed(params.subList(1, n), m) + ")");
        }
        return value(closure + expression(mr.scope()).text + "." + name + "(" + passed(params, m) + ")");
    }

    // ---- calls

    private Printed methodCall(MethodCall mc) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary()) {
            Printed mapped = RustJdk.call(mc, this);
            if (mapped != null) return mapped;
            context.message(RustPrintCode.UNMAPPED_JDK_CALL, mc, callee.fullyQualifiedName());
            if (callee.isStatic()) {
                return value(RustTypes.owned(callee.typeInfo().asSimpleParameterizedType(), context, mc) + "::"
                             + RustNames.snake(callee.name()) + "(" + arguments(mc.parameterExpressions(), null) + ")");
            }
            String object = mc.object() == null || mc.objectIsImplicit() ? context.receiver : expression(mc.object()).text;
            return value(object + "." + RustNames.snake(callee.name()) + "(" + arguments(mc.parameterExpressions(), null)
                         + ")");
        }
        if (RustTypePrinter.isEquals(callee) && mc.object() != null && !mc.objectIsImplicit()) {
            return value(adapt(expression(mc.object()), Held.SHARED) + " == "
                         + adapt(expression(mc.parameterExpressions().getFirst()), Held.SHARED));
        }
        String name = RustNames.method(callee);
        boolean exclusive = !callee.isStatic() && context.facts.receiver(callee) == Receiver.EXCLUSIVE;
        String args = exclusive ? withoutSelfBorrows(mc.parameterExpressions(), callee)
                : arguments(mc.parameterExpressions(), callee);
        String call;
        if (callee.isStatic()) {
            TypeInfo owner = callee.typeInfo();
            String prefix = owner == context.type ? "Self" : RustNames.type(owner);
            if (owner != context.type) {
                String module = RustNames.module(owner.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(owner));
            }
            call = prefix + "::" + name + "(" + args + ")";
        } else {
            // a trait's method is callable where the trait is in scope
            TypeInfo trait = callee.typeInfo().isInterface() ? callee.typeInfo() : RustTypePrinter.implementedTrait(callee);
            if (trait != null) {
                String module = RustNames.module(trait.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(trait));
            }
            boolean own = mc.object() == null || mc.objectIsImplicit()
                          || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This;
            String object = own ? context.receiver : dereference(expression(mc.object()));
            TypeInfo objectType = own ? context.type : mc.object().parameterizedType().typeInfo();
            // an inherited method is the base's; super.m() too
            String path = objectType == null ? "" : RustTypePrinter.basePath(objectType, callee.typeInfo());
            call = object + path + "." + name + "(" + args + ")";
        }
        boolean nullable = !callee.isVoid() && RustFacts.nullable(context.facts.returnType(callee));
        if (callee.isVoid() || RustTypes.isCopy(callee.returnType())) return new Printed(call, Held.VALUE, nullable);
        boolean borrows = context.facts.resultBorrowsReceiver(callee) && context.facts.receiver(callee) != Receiver.NONE;
        Held held = !borrows ? Held.VALUE : RustTypes.isString(callee.returnType()) ? Held.STR : Held.SHARED;
        return new Printed(call, held, nullable);
    }

    private Printed constructorCall(ConstructorCall cc) {
        if (cc.anonymousClass() != null) return value(unsupported(cc, "anonymous class"));
        ParameterizedType type = cc.parameterizedType();
        if (type.arrays() > 0) {
            if (cc.arrayInitializer() != null) {
                return value("vec![" + cc.arrayInitializer().expressions().stream().map(x -> expression(x).text)
                        .collect(Collectors.joining(", ")) + "]");
            }
            if (cc.parameterExpressions().size() == 1) {
                String element = RustTypes.empty(type.componentType(), false, context, cc);
                return value("vec![" + element + "; " + index(cc.parameterExpressions().getFirst()) + "]");
            }
            return value(unsupported(cc, "multi-dimensional array"));
        }
        MethodInfo constructor = cc.constructor();
        TypeInfo ti = type.typeInfo();
        if (ti.compilationUnit().externalLibrary()) {
            Printed mapped = RustJdk.construct(cc, this);
            if (mapped != null) return mapped;
            context.message(RustPrintCode.UNMAPPED_JDK_CALL, cc, ti.fullyQualifiedName());
            return value(ti.simpleName() + "::new(" + arguments(cc.parameterExpressions(), null) + ")");
        }
        String owner = ti == context.type ? "Self" : RustNames.type(ti);
        if (ti != context.type) {
            String module = RustNames.module(ti.packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(ti));
        }
        String name = constructor == null ? "new" : RustNames.method(constructor);
        return value(owner + "::" + name + "(" + arguments(cc.parameterExpressions(), constructor) + ")");
    }

    /**
     * The arguments of a call that borrows {@code self} exclusively: a string read from {@code self} is passed as a
     * copy, as a borrow of it could not live beside the exclusive one (Java shares an immutable string freely).
     */
    private String withoutSelfBorrows(List<Expression> arguments, MethodInfo callee) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Expression a = arguments.get(i);
            Printed p = expression(a);
            ParameterInfo pi = i < callee.parameters().size() ? callee.parameters().get(i) : null;
            boolean fromSelf = p.text.startsWith(context.receiver + ".") && RustTypes.isString(a.parameterizedType())
                               && !p.text.contains("(");
            // a string borrowed from a local or a lookup may be borrowed from self too ('let sku = self.m.get(k)'):
            // copied, as an immutable string can be
            boolean borrowed = p.held == Held.SHARED && RustTypes.isString(a.parameterizedType())
                               && !(a instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo);
            if (pi != null && (fromSelf || borrowed) && context.facts.passing(pi) == Passing.SHARED) {
                String text = "&" + p.text + ".clone()";
                boolean nullable = RustFacts.nullable(context.facts.parameter(pi));
                if (p.nullable) text = p.text + (fromSelf ? ".clone().as_deref()" : ".cloned().as_deref()");
                out.add(nullable && !p.nullable ? "Some(" + text + ")" : text);
            } else {
                out.add(pi == null ? p.text : argument(p, pi, a));
            }
        }
        return String.join(", ", out);
    }

    /** A method reference's closure parameters, each a place (see {@link #parameter}), adapted to the callee's. */
    private String passed(List<String> names, MethodInfo callee) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            if (i >= callee.parameters().size()) {
                out.add(names.get(i));
                continue;
            }
            ParameterInfo pi = callee.parameters().get(i);
            Printed p = new Printed(names.get(i), Held.PLACE, false);
            ParameterizedType type = pi.parameterizedType();
            String text = RustTypes.isCopy(type) ? p.text : switch (context.facts.passing(pi)) {
                case SHARED -> adapt(p, RustTypes.isString(type) ? Held.STR : Held.SHARED);
                case EXCLUSIVE -> adapt(p, Held.EXCLUSIVE);
                case OWNED -> owned(p, type);
            };
            out.add(RustFacts.nullable(context.facts.parameter(pi)) ? "Some(" + text + ")" : text);
        }
        return String.join(", ", out);
    }

    /** The arguments, each adapted to the parameter it is passed to. Without a callee: as they are. */
    String arguments(List<Expression> arguments, MethodInfo callee) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Printed p = expression(arguments.get(i));
            if (callee == null || i >= callee.parameters().size()) {
                out.add(p.text);
                continue;
            }
            ParameterInfo pi = callee.parameters().get(Math.min(i, callee.parameters().size() - 1));
            out.add(argument(p, pi, arguments.get(i)));
        }
        return String.join(", ", out);
    }

    private String argument(Printed p, ParameterInfo pi, Expression argument) {
        ParameterizedType type = pi.parameterizedType();
        p = widen(p, argument, type);
        boolean nullable = RustFacts.nullable(context.facts.parameter(pi));
        String text;
        if (RustTypes.isCopy(type)) {
            text = p.text;
        } else {
            text = switch (context.facts.passing(pi)) {
                case SHARED -> adapt(p, RustTypes.isString(type) ? Held.STR : Held.SHARED);
                case EXCLUSIVE -> adapt(p, Held.EXCLUSIVE);
                case OWNED -> owned(p, type);
            };
        }
        if (nullable && !p.nullable && !(argument instanceof NullConstant)) return "Some(" + text + ")";
        if (!nullable && p.nullable) return text + ".unwrap()";
        if (nullable && p.nullable && !RustTypes.isCopy(type) && context.facts.passing(pi) == Passing.SHARED) {
            return nullableBorrow(p, argument, RustTypes.isString(type));
        }
        return text;
    }

    /**
     * A nullable value where an {@code Option<&T>} ({@code Option<&str>}) is expected: {@code None} for null, a borrow
     * of an owned {@code Option<T>}'s content, a borrowed string's {@code &String} as {@code &str}.
     */
    private static String nullableBorrow(Printed p, Expression argument, boolean string) {
        if (argument instanceof NullConstant || "None".equals(p.text)) return "None";
        return switch (p.held) {
            case STR -> p.text;
            case VALUE, PLACE -> p.text + (string ? ".as_deref()" : ".as_ref()");
            case SHARED, EXCLUSIVE -> string ? p.text + ".map(|s| s.as_str())" : p.text;
        };
    }

    /** {@code p} where {@code target} is expected: a value or place is borrowed; a borrow is passed as it is. */
    static String adapt(Printed p, Held target) {
        return switch (target) {
            case SHARED, STR -> p.held == Held.VALUE || p.held == Held.PLACE ? "&" + p.text : p.text;
            case EXCLUSIVE -> p.held == Held.VALUE || p.held == Held.PLACE ? "&mut " + p.text : p.text;
            default -> p.text;
        };
    }

    /** {@code p} where an owned value is expected: moved, except that a borrowed string becomes a new one. */
    private String owned(Printed p, ParameterizedType type) {
        if ("None".equals(p.text)) return p.text;
        if (RustTypes.isString(type) && p.nullable && (p.held == Held.STR || p.held == Held.SHARED)) {
            return p.text + ".map(|s| s.to_string())";
        }
        if (RustTypes.isString(type) && (p.held == Held.STR || p.held == Held.SHARED)) return p.text + ".to_string()";
        // a string is immutable in Java: sharing it and copying it cannot be told apart, so a named one is cloned
        // rather than moved, and its later uses stay valid
        if (RustTypes.isString(type) && (p.held == Held.PLACE || p.held == Held.VALUE && isName(p.text))) {
            return p.text + ".clone()";
        }
        // an @Immutable class or record (no hidden content) derives Clone, and a copy cannot be told from the shared
        // object: a borrowed one is copied where an owned one is expected. Any other type stays a mismatch for rustc.
        if (RustTypes.isImmutableValue(type, context.facts) && (p.held == Held.SHARED || p.held == Held.PLACE
                                                               || p.held == Held.VALUE && isName(p.text))) {
            return p.text + (p.nullable ? ".cloned()" : ".clone()");
        }
        return p.text;
    }

    /**
     * {@code Map<String, Object>}: Rust infers a collection's element type from its first insert ({@code Box<f64>}),
     * so the declaration states it ({@code Box<dyn Any>}).
     */
    private static boolean mentionsObject(ParameterizedType type) {
        if (type.typeInfo() != null && type.typeInfo().isJavaLangObject()) return true;
        return type.parameters().stream().anyMatch(RustBodyPrinter::mentionsObject);
    }

    private static boolean isName(String text) {
        return text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '.');
    }

    // ---- what RustJdk needs

    String text(Expression e) {
        return expression(e).text;
    }

    Printed printed(Expression e) {
        return expression(e);
    }

    /** The object of a call: a nullable one unwrapped, as Java would throw a NullPointerException. */
    String receiver(Expression e, boolean modifying) {
        if (modifying) {
            String inPlace = RustJdk.getOrDefaultMut(e, this);
            if (inPlace != null) return inPlace;
        }
        Printed p = expression(e);
        if (!p.nullable) return p.text;
        context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
        return p.text + (modifying ? ".as_mut().unwrap()" : ".as_ref().unwrap()");
    }

    /** A two-parameter lambda over a map's entries: Rust's iterator gives one {@code (k, v)} tuple. */
    String tupleLambda(Expression e) {
        String text = expression(e).text;
        if (e instanceof Lambda l && l.methodInfo().parameters().size() == 2) {
            return text.replaceFirst("^\\|([^|]*)\\|", "|($1)|");
        }
        return text;
    }

    String borrowed(Expression e) {
        Printed p = expression(e);
        return RustTypes.isCopy(e.parameterizedType()) ? p.text : adapt(p, Held.SHARED);
    }

    /** Always a borrow, {@code &5} included: what {@code contains} and {@code get} take. */
    String reference(Expression e) {
        return adapt(expression(e), Held.SHARED);
    }

    String ownedText(Expression e) {
        return owned(expression(e), e.parameterizedType());
    }

    /**
     * {@code e} stored where a {@code target} is held: owned, and boxed when the target is {@code Object}
     * ({@code Box<dyn Any>}) and the value is not.
     */
    String ownedAs(Expression e, ParameterizedType target) {
        String text = ownedText(e);
        boolean toObject = target != null && target.typeInfo() != null && target.typeInfo().isJavaLangObject()
                           && target.arrays() == 0;
        boolean fromObject = e.parameterizedType().typeInfo() != null && e.parameterizedType().typeInfo().isJavaLangObject();
        return toObject && !fromObject && !(e instanceof NullConstant) ? "Box::new(" + text + ")" : text;
    }

    /** What a field of {@code type} is initialized with: owned, and in {@code Some} when the field is nullable. */
    String fieldValue(Expression e, ParameterizedType type, boolean nullable) {
        Printed p = expression(e);
        String text = RustTypes.isCopy(type) ? p.text : owned(p, type);
        return nullable && !p.nullable && !(e instanceof NullConstant) ? "Some(" + text + ")" : text;
    }

    String index(Expression e) {
        if (e instanceof IntConstant ic) return Integer.toString(ic.constant());
        String text = operand(e);
        // a size, which went to i32 for Java's arithmetic, is a usize already
        if (text.endsWith(".len() as i32")) return text.substring(0, text.length() - " as i32".length());
        return text + " as usize";
    }

    static Printed printed(String text, Held held, boolean nullable) {
        return new Printed(text, held, nullable);
    }

    RustContext context() {
        return context;
    }

    // ---- scans

    private boolean reassigned(LocalVariable lv) {
        return context.method != null && context.method.methodBody() != null
               && assignedIn(context.method.methodBody(), lv);
    }

    private static boolean assignedIn(Element element, Variable variable) {
        boolean[] found = {false};
        element.visit(e -> {
            if (e instanceof Assignment a && variable.equals(a.variableTarget())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    private static boolean containsContinue(Element element) {
        boolean[] found = {false};
        element.visit(e -> {
            if (e instanceof ContinueStatement) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    /** The local is the object of a modifying call, or passed where a {@code &mut} is expected. */
    private boolean mutated(LocalVariable lv) {
        if (context.method == null || context.method.methodBody() == null) return false;
        boolean[] found = {false};
        context.method.methodBody().visit(e -> {
            if (found[0]) return false;
            if (e instanceof MethodCall mc) {
                if (isLocal(mc.object(), lv) && modifies(mc)) found[0] = true;
                List<Expression> args = mc.parameterExpressions();
                MethodInfo callee = mc.methodInfo();
                for (int i = 0; i < args.size() && i < callee.parameters().size(); i++) {
                    if (isLocal(args.get(i), lv) && !callee.typeInfo().compilationUnit().externalLibrary()
                        && context.facts.passing(callee.parameters().get(i)) == Passing.EXCLUSIVE) {
                        found[0] = true;
                    }
                }
            } else if (e instanceof Assignment a && a.target().variable() instanceof FieldReference fr
                       && isLocal(fr.scope(), lv)) {
                found[0] = true;
            } else if (e instanceof Assignment a && a.target().variable() instanceof DependentVariable dv
                       && isLocal(dv.arrayExpression(), lv)) {
                found[0] = true;
            }
            return !found[0];
        });
        return found[0];
    }

    private boolean modifies(MethodCall mc) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary()) return RustJdk.modifies(mc);
        return context.facts.receiver(callee) == Receiver.EXCLUSIVE;
    }

    private static boolean isLocal(Expression e, LocalVariable lv) {
        return e instanceof VariableExpression ve && lv.equals(ve.variable());
    }

    // ---- literals

    private String unsupported(Element element, String what) {
        context.message(RustPrintCode.UNSUPPORTED, element, what);
        return "todo!(\"" + what.replace("\"", "'") + "\")";
    }

    static String stringLiteral(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u{%x}", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    private static String charLiteral(char c) {
        return switch (c) {
            case '\'' -> "'\\''";
            case '\\' -> "'\\\\'";
            case '\n' -> "'\\n'";
            case '\r' -> "'\\r'";
            case '\t' -> "'\\t'";
            case '\0' -> "'\\0'";
            default -> c < 0x20 ? String.format("'\\u{%x}'", (int) c) : "'" + c + "'";
        };
    }

    private static String doubleLiteral(double d) {
        if (Double.isNaN(d)) return "f64::NAN";
        if (Double.isInfinite(d)) return d > 0 ? "f64::INFINITY" : "f64::NEG_INFINITY";
        String s = Double.toString(d);
        return s.contains("E") ? s.replace("E", "e") : s;
    }
}
