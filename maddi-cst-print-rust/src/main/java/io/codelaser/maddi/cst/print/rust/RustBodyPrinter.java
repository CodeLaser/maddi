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

package io.codelaser.maddi.cst.print.rust;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

/**
 * A method body as Rust statements. Every use of a value is adapted to what its destination expects, as the facts
 * say: an argument for a {@code &T} parameter is borrowed, one for {@code &mut T} borrowed exclusively, one for a
 * {@code T} parameter moved. Nothing is cloned to make the borrow checker happy; a string is the one exception
 * ({@code .to_string()} where an owned {@code String} is stored), because it is immutable in Java and the copy cannot
 * change behaviour.
 */
final class RustBodyPrinter {
    // printing the place an assignment or a modifying call writes through: a nullable scope is unwrapped as_mut(),
    // a map's value reached with get_mut() (RustJdk)
    private boolean mutatingPlace;
    // the parameters movable() lets their one read take, by name, and its memo
    private final Set<String> moved = new HashSet<>();
    /* closure parameters typed '&str' (a source functional interface's String parameter): not a place to clone */
    private final Set<ParameterInfo> strClosureParameters = new HashSet<>();
    // a closure's parameters typed Option<…> by the functional interface they implement: how each holds its value
    private final Map<ParameterInfo, Held> optionClosureParameters = new HashMap<>();
    private final Map<ParameterInfo, Boolean> movable = new HashMap<>();

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
    /**
     * The functional interface the closure being printed implements, when it is passed, stored or assigned as one:
     * its parameters are typed, which rustc cannot infer for a {@code dyn} target.
     */
    private ParameterizedType closureTarget;
    /** The lambda being printed is Optional.map's function: a nullable result stays an Option ({@code and_then}). */
    private boolean optionalResult;
    /** The lambda being printed is Optional.orElseGet's supplier: its result is a value of its own, copied. */
    private boolean copiedResult;
    /** The switch expression being printed is a method's result: each arm's value is converted as the result is. */
    private Function<Expression, String> armResult;
    /** The labeled blocks of the switch arms being printed, innermost first: a yield is a break out of one. */
    private final Deque<YieldTarget> yieldTargets = new ArrayDeque<>();
    private int yieldCount;

    private record YieldTarget(String label, Function<Expression, String> value) {
    }
    /** Variables printed as another name: in {@code eq}, Java's parameter and its cast or pattern are {@code other}. */
    private final Map<Variable, String> aliases = new HashMap<>();
    // the parameter of the equals being printed as PartialEq::eq, whose other side is a &Self
    private ParameterInfo equalsParameter;
    // the expression of the expression statement being printed: its value is dropped
    private Expression statement;

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
            case YieldStatement ys when !yieldTargets.isEmpty() -> pad + "break " + yieldTargets.peek().label() + " "
                    + yieldTargets.peek().value().apply(ys.expression()) + ";\n";
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
        if (e instanceof MethodCall mc && context.facts.side(mc.methodInfo()) == RustFacts.Side.MARK
            && RustTypestate.typestate(mc.methodInfo().typeInfo(), context)) {
            // the mark of a typestate consumes the object and returns it in its after state: the local is rebound
            if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
                return "let mut " + RustNames.snake(lv.simpleName()) + " = " + expression(e).text;
            }
            context.message(RustPrintCode.UNSUPPORTED, e, "the mark of a typestate on anything but a local variable");
        }
        List<MethodCall> appends = appendChain(e);
        if (appends != null) {
            // sb.append(a).append(b): Java returns the builder, Rust's push_str nothing; one statement per append
            String target = expression(appends.getFirst().object()).text;
            return appends.stream().map(mc -> RustJdk.append(target, mc.parameterExpressions().getFirst(), this))
                    .collect(Collectors.joining(";\n" + pad));
        }
        Expression outer = statement;
        statement = e;
        try {
            return expression(e).text;
        } finally {
            statement = outer;
        }
    }

    /** {@code e} is an expression statement's: Java drops its value ({@code list.add(x);}). */
    boolean isStatement(Expression e) {
        return e == statement;
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
        lvc.localVariableStream().forEach(lv -> {
            String bound = fluentOnNew(lv, pad);
            sb.append(bound != null ? bound : pad + local(lvc, lv) + ";\n");
        });
        return sb.toString();
    }

    /**
     * {@code Builder b = new Builder().setJ(jp);}: a fluent call hands back a borrow of its receiver, here a temporary
     * that is gone at the end of the statement (E0716). The new object is bound first, and the calls run on the
     * binding: {@code let mut b = Builder::new(); b.set_j(jp);}. {@code null} for another initializer.
     */
    private String fluentOnNew(LocalVariable lv, String pad) {
        Expression init = lv.assignmentExpression();
        if (init == null) return null;
        Expression e = CstQueries.unwrap(init);
        boolean fluent = false;
        while (e instanceof MethodCall mc && mc.object() != null && context.facts.fluent(mc.methodInfo())) {
            fluent = true;
            e = CstQueries.unwrap(mc.object());
        }
        if (!fluent || !(e instanceof ConstructorCall)) return null;
        String root = expression(e).text;
        String all = expression(init).text;
        if (!all.startsWith(root)) return null;
        String name = RustNames.snake(lv.simpleName());
        locals.put(lv, Held.VALUE);
        nullableLocals.put(lv, false);
        return pad + "let mut " + name + " = " + root + ";\n" + pad + name + all.substring(root.length()) + ";\n";
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
        boolean mutated = mutated(lv);
        // a cast of an Object the local modifies: downcast_mut (cast); a getter's result it modifies: its exclusive
        // twin (RustGetters)
        boolean mutableCast = mutated && (CstQueries.unwrap(init) instanceof Cast
                                          || CstQueries.unwrap(init) instanceof MethodCall g
                                             && context.mutableGetters.contains(g.methodInfo())
                                             // a shared object's local is a handle of its own instead
                                             && !RustShared.current().isShared(lv.parameterizedType()));
        Printed value = isClosure(init) && RustTypes.isFunctional(lv.parameterizedType())
                ? closure(init, lv.parameterizedType())
                : widen(mutableCast ? mutable(init) : expression(init), init, lv.parameterizedType());
        nullable |= value.nullable;
        nullableLocals.put(lv, nullable);
        String text = value.text;
        Held held = value.held;
        if (reassigned && (held == Held.SHARED || held == Held.STR) && RustTypes.isString(lv.parameterizedType())
            && !(init instanceof NullConstant)) {
            // 'String x = s; … x = "default";': an assignment gives the local an owned string, so it owns its first
            text = value.nullable ? text + ".map(|v| v.to_string())" : text + ".to_string()";
            held = Held.VALUE;
        } else if (reassigned && held == Held.SHARED && !(init instanceof NullConstant)
                   && RustTypes.isImmutableValue(lv.parameterizedType(), context.facts)) {
            // 'VarType t = md.getRet(); … t = VarType.VARTYPE_CHAR;': an immutable value (an Rc) the local is later
            // given owned owns its first one too, a clone of the handle
            text = text + (value.nullable ? ".cloned()" : ".clone()");
            held = Held.VALUE;
        } else if (held == Held.PLACE && (RustTypes.isString(lv.parameterizedType())
                                   || RustTypes.isImmutableValue(lv.parameterizedType(), context.facts))) {
            // an immutable value: the local is a copy, so that the field may be assigned while it lives
            text = text + ".clone()";
            held = Held.VALUE;
        } else if (held == Held.SHARED && RustTypes.isString(lv.parameterizedType()) && !(init instanceof NullConstant)
                   && !(CstQueries.unwrap(init) instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo)) {
            // a string borrowed out of a lookup ('(String) b.get("sku")'): a copy, which ends the borrow of what holds it
            text = text + (value.nullable ? ".cloned()" : ".clone()");
            held = Held.VALUE;
        } else if (mutated && (held == Held.PLACE || held == Held.SHARED)
                   && RustShared.current().isShared(lv.parameterizedType())) {
            // a shared object modified through the local ('ListStack s = data.getStack(); s.push(t)'): the local is a
            // handle of its own, as Java's reference is, so that it modifies without borrowing what holds it
            text = text + (held == Held.SHARED && value.nullable ? ".cloned()" : ".clone()");
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
        String boxed = held == Held.VALUE && !value.nullable ? asDyn(text, init, lv.parameterizedType()) : text;
        boolean dyn = !boxed.equals(text);
        text = boxed;
        if (nullable && !value.nullable && !(init instanceof NullConstant)) text = "Some(" + text + ")";
        locals.put(lv, held);
        // an Option is unwrapped with as_mut() to modify what it holds, which needs a mutable binding
        boolean mut = reassigned || mutated && (held == Held.VALUE || nullable);
        String type = init instanceof NullConstant
                ? ": " + RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), true)
                : (dyn || mentionsObject(lv.parameterizedType()) || emptyCollection(init)) && held == Held.VALUE
                ? ": " + RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), nullable) : "";
        return "let " + (mut ? "mut " : "") + name + type + " = " + text;
    }

    private String returnStatement(ReturnStatement rs) {
        Expression e = rs.expression();
        if (e == null || e instanceof EmptyExpression) return "return";
        MethodInfo method = context.method;
        Receiver receiver = context.facts.receiver(method);
        if (context.facts.fluent(method) && (receiver == Receiver.SHARED || receiver == Receiver.EXCLUSIVE)) {
            // 'return this': the borrow of the receiver the signature hands back
            return "return " + context.receiver;
        }
        return "return " + resultValue(e);
    }

    /*
     e as the method's result. A switch expression's or a conditional's arms are each converted, as each may be held
     otherwise: null, a field, a parameter ('case "a" -> null; case "b" -> list1; default -> list3').
     */
    private String resultValue(Expression e) {
        MethodInfo method = context.method;
        if (e instanceof SwitchExpression se && !RustTypes.isCopy(method.returnType())) {
            Function<Expression, String> outer = armResult;
            armResult = this::resultValue;
            try {
                return switchExpression(se).text;
            } finally {
                armResult = outer;
            }
        }
        if (e instanceof InlineConditional ic && !RustTypes.isCopy(method.returnType())) {
            return "if " + condition(ic.condition()) + " { " + resultValue(ic.ifTrue()) + " } else { "
                   + resultValue(ic.ifFalse()) + " }";
        }
        boolean nullable = RustFacts.nullable(context.facts.returnType(method));
        if (isClosure(e) && RustTypes.isFunctional(method.returnType())) {
            // the Box<dyn Fn…> the signature declares
            String boxed = boxed(closure(e, method.returnType()).text);
            return nullable ? "Some(" + boxed + ")" : boxed;
        }
        boolean borrows = context.facts.resultBorrowsReceiver(method) && context.facts.receiver(method) != Receiver.NONE
                          || context.facts.resultBorrows(method) != null;
        Printed value = widen(expression(e), e, method.returnType());
        // what is not translated diverges: '!' is any result type, a borrow too
        if (value.text.startsWith("todo!(") || value.text.startsWith("panic!(")) return value.text;
        String text;
        if (RustTypes.isCopy(method.returnType())) {
            text = value.text;
        } else if (borrows && nullable && value.nullable) {
            // Option<&T>: a borrow of the Option's content, not of the Option ('&self.t' is an &Option<T>)
            Printed inner = value.text.startsWith("if ") ? new Printed("(" + value.text + ")", value.held, true) : value;
            text = nullableBorrow(inner, e, RustTypes.isString(method.returnType()), method.returnType().arrays() > 0);
        } else if (borrows && !nullable && value.nullable && !(e instanceof NullConstant)) {
            // never null by the verdict, held in an Option: a borrow of its content ('&self.t.unwrap()' borrows a
            // temporary moved out of the field)
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, value.text);
            Printed inner = value.text.startsWith("if ") ? new Printed("(" + value.text + ")", value.held, true) : value;
            return nullableBorrow(inner, e, RustTypes.isString(method.returnType()),
                    method.returnType().arrays() > 0) + ".unwrap()";
        } else if (borrows) {
            text = adapt(value, RustTypes.isString(method.returnType()) ? Held.STR : Held.SHARED);
        } else if (!value.nullable && (value.held == Held.PLACE || value.held == Held.SHARED)
                   && RustFacts.ownedByFamily(method) && RustTypes.cloneable(method.returnType(), context.facts)
                   && !RustTypes.isImmutableValue(method.returnType(), context.facts)) {
            // the family's result is owned (another member makes it): this member's field or borrow is copied
            text = value.text + ".clone()";
        } else {
            text = value.nullable ? ownedNullable(value, e, method.returnType()) : asDyn(
                    asObject(owned(value, objectTarget(method.returnType()) ? e.parameterizedType() : method.returnType()),
                            e, method.returnType()), e, method.returnType());
        }
        if (RustTypes.streamItem(method.returnType(), context, method) != null && !text.startsWith("Box::new(")) {
            text = "Box::new(" + text + ")"; // the chain's own iterator type, behind the declared dyn Iterator
        }
        if (nullable && !value.nullable && !(e instanceof NullConstant)) text = "Some(" + text + ")";
        if (!nullable && value.nullable) {
            // the verdict says the result is never null: Java's value is used as it is, Rust's unwrapped
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, value.text);
            text = text + ".unwrap()";
        }
        return text;
    }

    private String ifElse(IfElseStatement ife, int depth) {
        String negated = negatedPatterns(ife, depth);
        return negated != null ? negated : ifElse(ife, depth, true);
    }

    private String ifElse(IfElseStatement ife, int depth, boolean statement) {
        String pad = INDENT.repeat(depth);
        String downcast = downcastPattern(ife.expression());
        StringBuilder sb = new StringBuilder("if " + (downcast != null ? downcast : condition(ife.expression())) + " {\n"
                                             + statements(ife.block(), depth + 1) + pad + "}");
        Block elseBlock = ife.elseBlock();
        if (elseBlock != null && !elseBlock.statements().isEmpty()) {
            if (elseBlock.statements().size() == 1 && elseBlock.statements().getFirst() instanceof IfElseStatement nested) {
                sb.append(" else ").append(ifElse(nested, depth, false));
            } else {
                sb.append(" else {\n").append(statements(elseBlock, depth + 1)).append(pad).append("}");
            }
        }
        return sb.toString();
    }

    /**
     * A pattern variable's binding: {@code pattern} binds it from the {@code Some} of {@code option}, the tested value
     * as the pattern's type or {@code None} ({@code &t} for a copy).
     */
    private record Binding(String pattern, String option) {
        String let() {
            return "let Some(" + pattern + ") = " + option;
        }
    }

    /**
     * {@code o instanceof T t} with a pattern variable: of an Object, T a class, {@code o.downcast_ref::<T>()}
     * ({@code downcast_mut} when {@code t} is modified); in a closed hierarchy ({@link #hierarchyBinding}) the object
     * itself when its kind is one of T's. {@code null} for anything else.
     */
    private Binding binding(InstanceOf io) {
        if (io.patternVariable() == null || io.patternVariable().localVariable() == null) return null;
        if (equalsParameter != null && isVariable(io.expression(), equalsParameter)
            && io.testType().typeInfo() == equalsParameter.methodInfo().typeInfo()) {
            // equals' 'return o instanceof Version v && …': PartialEq's other is already a &Self
            LocalVariable lv = io.patternVariable().localVariable();
            locals.put(lv, Held.SHARED);
            nullableLocals.put(lv, false);
            return new Binding(RustNames.snake(lv.simpleName()), "Some(other)");
        }
        Binding inHierarchy = hierarchyBinding(io);
        if (inHierarchy != null) return inHierarchy;
        if (interfaceDowncast(io.expression(), io.testType()) != null) {
            LocalVariable lv = io.patternVariable().localVariable();
            boolean mutated = mutated(lv) && !context.borrows.hasLifetime(io.testType().typeInfo());
            locals.put(lv, mutated ? Held.EXCLUSIVE : Held.SHARED);
            nullableLocals.put(lv, false);
            return new Binding(RustNames.snake(lv.simpleName()), interfaceOption(io.expression(), io.testType(), mutated));
        }
        LocalVariable lv = io.patternVariable().localVariable();
        if (!isObject(io.expression().parameterizedType())) return null;
        ParameterizedType type = lv.parameterizedType();
        if (!RustTypes.isDowncastTarget(type)) return null;
        context.message(RustPrintCode.DOWNCAST, io, type.fullyQualifiedName());
        boolean mutated = mutated(lv);
        boolean copy = RustTypes.isCopy(type);
        locals.put(lv, copy && !mutated ? Held.VALUE : mutated ? Held.EXCLUSIVE : Held.SHARED);
        String downcast = (mutated ? "downcast_mut" : "downcast_ref") + "::<" + RustTypes.owned(type, context, io) + ">()";
        Printed tested = expression(io.expression());
        String value = tested.nullable
                ? tested.text + (mutated ? ".as_mut()" : ".as_ref()") + ".and_then(|o| o." + downcast + ")"
                : tested.text + "." + downcast;
        return new Binding((copy && !mutated ? "&" : "") + RustNames.snake(lv.simpleName()), value);
    }

    private Binding binding(Expression e) {
        return CstQueries.unwrap(e) instanceof InstanceOf io ? binding(io) : null;
    }

    private static boolean bindsPattern(Expression e) {
        return CstQueries.unwrap(e) instanceof InstanceOf io && io.patternVariable() != null
               && io.patternVariable().localVariable() != null;
    }

    /**
     * The condition of an {@code if} that binds pattern variables: {@code let Some(t) = …}, and in a conjunction a let
     * chain (Rust 2024), each binding in scope for what follows it. {@code null} for a condition without one.
     */
    private String downcastPattern(Expression condition) {
        Expression unwrapped = CstQueries.unwrap(condition);
        if (unwrapped instanceof And and && and.expressions().stream().anyMatch(RustBodyPrinter::bindsPattern)) {
            List<String> parts = new ArrayList<>();
            for (Expression x : and.expressions()) {
                Binding b = binding(x);
                parts.add(b != null ? b.let() : operand(x));
            }
            return String.join(" && ", parts);
        }
        Binding b = binding(unwrapped);
        return b == null ? null : b.let();
    }

    /**
     * A conjunction with pattern variables as a value ({@code return e instanceof A a && a.isX();}): each binding's
     * {@code Option} tested with {@code is_some_and}, the closure's parameter the variable for the rest.
     */
    private Printed andWithBindings(And and) {
        return value(andWithBindings(and.expressions(), 0));
    }

    private String andWithBindings(List<Expression> operands, int from) {
        List<String> parts = new ArrayList<>();
        for (int i = from; i < operands.size(); i++) {
            Binding b = binding(operands.get(i));
            if (b != null) {
                String rest = i + 1 < operands.size() ? andWithBindings(operands, i + 1) : "true";
                parts.add(b.option() + ".is_some_and(|" + b.pattern() + "| " + rest + ")");
                break;
            }
            parts.add(operand(operands.get(i)));
        }
        return parts.size() == 1 ? parts.getFirst() : "(" + String.join(" && ", parts) + ")";
    }

    private static boolean isNot(Expression e) {
        return e instanceof UnaryOperator uo && uo.operator() != null && "!".equals(uo.operator().name());
    }

    /**
     * {@code if (!(e instanceof A a && …)) { return …; }}: Java's pattern variable is in scope after the statement,
     * as the block cannot complete normally. Each binding is a {@code let … else} with the block, each other operand
     * an {@code if !(…)} with it. {@code null} for another statement.
     */
    private String negatedPatterns(IfElseStatement ife, int depth) {
        if (!isNot(CstQueries.unwrap(ife.expression())) || ife.elseBlock() != null && !ife.elseBlock().isEmpty()
            || !exits(ife.block())) {
            return null;
        }
        Expression inner = CstQueries.unwrap(((UnaryOperator) CstQueries.unwrap(ife.expression())).expression());
        List<Expression> operands = inner instanceof And and ? and.expressions() : List.of(inner);
        if (operands.stream().noneMatch(RustBodyPrinter::bindsPattern)) return null;
        String pad = INDENT.repeat(depth);
        StringBuilder sb = new StringBuilder();
        for (Expression x : operands) {
            Binding b = binding(x);
            if (b == null && bindsPattern(x)) return null;
            String block = statements(ife.block(), depth + 1);
            if (sb.length() > 0) sb.append(pad);
            if (b != null) sb.append(b.let()).append(" else {\n").append(block).append(pad).append("};\n");
            else sb.append("if !").append(operand(x)).append(" {\n").append(block).append(pad).append("}\n");
        }
        return sb.substring(0, sb.length() - 1);
    }

    /** The block cannot complete normally: it ends in a return, a throw, a break or a continue. */
    private static boolean exits(Block block) {
        if (block == null || block.statements().isEmpty()) return false;
        Statement last = block.statements().getLast();
        return last instanceof ReturnStatement || last instanceof ThrowStatement || last instanceof BreakStatement
               || last instanceof ContinueStatement
               || last instanceof Block b && exits(b)
               || last instanceof IfElseStatement i && exits(i.block()) && i.elseBlock() != null && exits(i.elseBlock());
    }

    /**
     * {@code e instanceof A a} in a closed hierarchy (RustHierarchies): {@code a} is {@code e} itself, when its kind is
     * one of A's: {@code Some(e).filter(|o| matches!(o.kind, …))}, mutably when {@code a} is modified. {@code null}
     * for any other test.
     */
    private Binding hierarchyBinding(InstanceOf io) {
        RustHierarchies hs = RustHierarchies.current();
        TypeInfo tested = io.expression().parameterizedType().typeInfo();
        TypeInfo target = io.testType().typeInfo();
        if (!hs.isMember(target) || hs.root(tested) != hs.root(target) || io.testType().arrays() > 0) return null;
        LocalVariable lv = io.patternVariable().localVariable();
        boolean mutated = mutated(lv);
        Printed p = mutated ? mutable(io.expression()) : expression(io.expression());
        String option;
        if (p.nullable) option = p.text + (mutated ? ".as_mut()" : ".as_ref()");
        else if (p.held == Held.SHARED || p.held == Held.EXCLUSIVE && !mutated) option = "Some(&*" + p.text + ")";
        else if (p.held == Held.EXCLUSIVE) option = "Some(&mut *" + p.text + ")";
        else option = "Some(" + (mutated ? "&mut " : "&") + p.text + ")";
        locals.put(lv, mutated ? Held.EXCLUSIVE : Held.SHARED);
        nullableLocals.put(lv, false);
        useKind(target);
        return new Binding(RustNames.snake(lv.simpleName()), option + ".filter(|o| " + hs.test("o", target) + ")");
    }

    // the kind enum a test names, imported where the hierarchy is another module's
    private void useKind(TypeInfo member) {
        String module = RustNames.module(member.packageName());
        if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustHierarchies.current().kind(member));
    }

    /** {@code e instanceof A} in a closed hierarchy: a test of e's kind; false for a null {@code e}, as Java's. */
    private Printed hierarchyTest(InstanceOf io) {
        RustHierarchies hs = RustHierarchies.current();
        Printed p = expression(io.expression());
        TypeInfo target = io.testType().typeInfo();
        useKind(target);
        if (p.nullable) return value(p.text + ".as_ref().is_some_and(|o| " + hs.test("o", target) + ")");
        return value(hs.test(p.text, target));
    }

    /**
     * A value of a source interface tested or cast to a source class: the struct the trait converts to
     * ({@link RustHierarchies#downcasts}); else {@code null}.
     */
    private static TypeInfo interfaceDowncast(Expression tested, ParameterizedType to) {
        ParameterizedType from = tested.parameterizedType();
        if (from.arrays() > 0 || to.arrays() > 0) return null;
        return RustHierarchies.current().downcastStruct(from.typeInfo(), to.typeInfo());
    }

    /** {@code x.as_statement()}: the interface's value as the struct, or None; a member's kind tested too. */
    private String interfaceOption(Expression tested, ParameterizedType to, boolean exclusive) {
        RustHierarchies hs = RustHierarchies.current();
        TypeInfo struct = interfaceDowncast(tested, to);
        String conversion = RustHierarchies.downcastName(struct) + (exclusive ? "_mut" : "") + "()";
        Printed p = exclusive ? mutable(tested) : expression(tested);
        String option = p.nullable ? p.text + (exclusive ? ".as_mut()" : ".as_ref()") + ".and_then(|o| o." + conversion + ")"
                : p.text + "." + conversion;
        if (hs.isSubMember(to.typeInfo())) {
            useKind(to.typeInfo());
            option += ".filter(|o| " + hs.test("o", to.typeInfo()) + ")";
        }
        return option;
    }

    private static boolean isInterfaceTest(InstanceOf io) {
        return io.patternVariable() == null && interfaceDowncast(io.expression(), io.testType()) != null;
    }

    private static boolean isHierarchyTest(InstanceOf io) {
        RustHierarchies hs = RustHierarchies.current();
        TypeInfo target = io.testType().typeInfo();
        return io.patternVariable() == null && io.testType().arrays() == 0 && hs.isMember(target)
               && hs.root(io.expression().parameterizedType().typeInfo()) == hs.root(target);
    }

    /** {@code o instanceof T} of an Object, T a class: {@code o.is::<T>()}. */
    private Printed isInstance(InstanceOf io) {
        context.message(RustPrintCode.DOWNCAST, io, io.testType().fullyQualifiedName());
        String test = "is::<" + RustTypes.owned(io.testType(), context, io) + ">()";
        Printed tested = expression(io.expression());
        return value(tested.nullable ? tested.text + ".as_ref().is_some_and(|o| o." + test + ")"
                : tested.text + "." + test);
    }

    private String forEach(ForEachStatement fe, String label, int depth) {
        String pad = INDENT.repeat(depth);
        LocalVariable lv = fe.initializer().localVariable();
        String name = RustNames.snake(lv.simpleName());
        boolean mutated = mutated(lv);
        boolean handles = mutated && RustShared.current().isShared(lv.parameterizedType());
        Printed collection = mutated && !handles ? mutable(fe.expression()) : expression(fe.expression());
        String iterator;
        if (handles && !RustJdk.isIterator(collection.text)) {
            // shared objects modified through the loop variable ('for (Statement st : stat.getStats()) st.x()'): a
            // snapshot of the handles, each one Java's reference, so that neither the list nor its holder is borrowed
            // while the body runs
            String c = collection.nullable ? receiver(fe.expression(), false) : collection.text;
            locals.put(lv, Held.VALUE);
            return pad + label + "for mut " + name + " in " + c + ".iter().cloned().collect::<Vec<_>>() {\n"
                   + statements(fe.block(), depth + 1) + pad + "}\n";
        }
        if (RustJdk.isIterator(collection.text)) {
            iterator = collection.text;
        } else {
            // a nullable collection is unwrapped: Option's own iter() would go over the collection, once
            String c = collection.nullable ? receiver(fe.expression(), mutated) : collection.text;
            iterator = c + (mutated ? ".iter_mut()" : ".iter()");
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
        equalsParameter = o;
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
        StringBuilder sb = new StringBuilder("match " + selector(sw.expression(),
                sw.entries().stream().flatMap(e -> e.conditions().stream()).toList()) + " {\n");
        List<Expression> labels = new ArrayList<>();
        boolean hasDefault = false;
        for (SwitchEntry entry : sw.entries()) {
            if (entry.patternVariable() != null) return unsupported(entry, "pattern in a switch") + ";";
            boolean isDefault = isDefault(entry.conditions());
            hasDefault |= isDefault;
            entry.conditions().stream().filter(c -> !c.isEmpty()).forEach(labels::add);
            String head = isDefault ? "_" : caseLabels(entry.conditions(), sw.expression());
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
        StringBuilder sb = new StringBuilder("match " + selector(sw.expression(), groups.stream()
                .flatMap(g -> g.labels().stream()).map(SwitchStatementOldStyle.SwitchLabel::literal).toList()) + " {\n");
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
            String head = isDefault ? "_" : caseLabels(conditions, sw.expression());
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
        StringBuilder sb = new StringBuilder("match " + selector(se.selector(),
                se.entries().stream().flatMap(e -> e.conditions().stream()).toList()) + " {\n");
        List<Expression> labels = new ArrayList<>();
        boolean hasDefault = false;
        for (SwitchEntry entry : se.entries()) {
            if (entry.patternVariable() != null) return value(unsupported(entry, "pattern in a switch"));
            boolean isDefault = isDefault(entry.conditions());
            hasDefault |= isDefault;
            entry.conditions().stream().filter(c -> !c.isEmpty()).forEach(labels::add);
            String head = isDefault ? "_" : caseLabels(entry.conditions(), se.selector());
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
                // a yield before the last statement leaves the arm: a labeled block, broken out of with the value
                Function<Expression, String> result = armResult;
                Function<Expression, String> value = e -> result != null ? result.apply(e)
                        : armExpression(e, type);
                boolean early = yieldsEarly(body);
                String label = early ? "'y" + (yieldCount++) : null;
                StringBuilder sb = new StringBuilder(early ? label + ": {\n" : "{\n");
                armResult = null;
                if (early) yieldTargets.push(new YieldTarget(label, value));
                try {
                    for (int i = 0; i < body.size(); i++) {
                        Statement st = body.get(i);
                        if (i == body.size() - 1 && st instanceof YieldStatement ys) {
                            sb.append(pad).append(INDENT).append(value.apply(ys.expression())).append("\n");
                        } else {
                            sb.append(statement(st, depth + 1));
                        }
                    }
                } finally {
                    if (early) yieldTargets.pop();
                    armResult = result;
                }
                return sb.append(pad).append("}").toString();
            }
            default -> {
                return unsupported(s, s.getClass().getSimpleName());
            }
        }
    }

    /* a yield nested in the arm's statements, not its last one; not one of a switch expression inside it */
    private static boolean yieldsEarly(List<Statement> body) {
        boolean[] found = {false};
        for (int i = 0; i < body.size() - 1; i++) {
            body.get(i).visit(e -> {
                if (e instanceof YieldStatement) found[0] = true;
                return !found[0] && !(e instanceof SwitchExpression) && !(e instanceof Lambda);
            });
        }
        return found[0];
    }

    private String armExpression(Expression e, ParameterizedType type) {
        if (armResult != null) {
            Function<Expression, String> result = armResult;
            armResult = null;
            try {
                return result.apply(e);
            } finally {
                armResult = result;
            }
        }
        Printed p = widen(expression(e), e, type);
        return RustTypes.isCopy(type) ? p.text : owned(p, type);
    }

    /** The selector: a string is matched as a {@code &str}, a nullable value unwrapped as Java would throw. */
    private String selector(Expression selector, List<Expression> labels) {
        String text = operand(selector);
        if (RustTypes.isString(selector.parameterizedType())) return text + ".as_str()";
        // a byte or short matched against int constants ('switch (tag) { case CONSTANT_Utf8 -> ... }'): Java widens
        // the selector, a Rust pattern takes the constant's own type
        ParameterizedType type = selector.parameterizedType();
        if (type.arrays() == 0 && type.typeInfo() != null && (type.typeInfo().isByte() || type.typeInfo().isShort())
            && labels.stream().anyMatch(l -> l != null && CstQueries.unwrap(l) instanceof VariableExpression
                                             && l.parameterizedType().isInt())) {
            return text + " as i32";
        }
        return text;
    }

    private String caseLabels(List<Expression> conditions, Expression selector) {
        ParameterizedType type = selector.parameterizedType();
        // a char label of an int selector ('switch (data.readUnsignedByte()) { case 'e': … }'): Java widens it, a Rust
        // pattern cannot cast, so it is the char's code
        boolean integral = type.arrays() == 0 && type.typeInfo() != null && type.isPrimitiveExcludingVoid()
                           && !type.typeInfo().isChar() && !type.typeInfo().isBoolean();
        boolean character = type.arrays() == 0 && type.typeInfo() != null && type.typeInfo().isChar();
        return conditions.stream().filter(c -> c != null && !c.isEmpty())
                .map(c -> integral && c instanceof CharConstant cc ? Integer.toString(cc.constant())
                        // an int label of a char selector ('case 8:' for '\b'): the char of that code
                        : character && c instanceof IntConstant ic && ic.constant() >= 0 && ic.constant() < 0xD800
                        ? charLiteral((char) ic.constant().intValue())
                        : caseLabel(c))
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
            case MethodCall mc -> sharedCloneFirst(mc, methodCall(mc));
            case ConstructorCall cc -> constructorCall(cc);
            case BinaryOperator bo when bo.operator() != null -> binaryOperator(bo);
            case UnaryOperator uo when uo.operator() != null -> unaryOperator(uo);
            case Cast c -> cast(c);
            case ArrayLength al -> value(dereference(expression(al.scope())) + ".len() as i32");
            case Lambda lambda -> lambda(lambda);
            case SwitchExpression se -> switchExpression(se);
            case And and when and.expressions().stream().anyMatch(RustBodyPrinter::bindsPattern) -> andWithBindings(and);
            case And and -> value(and.expressions().stream().map(this::operand).collect(Collectors.joining(" && ")));
            case Or or -> value(or.expressions().stream().map(this::operand).collect(Collectors.joining(" || ")));
            case TypeExpression te -> value(RustTypes.owned(te.parameterizedType(), context, te));
            case MethodReference mr -> methodReference(mr);
            case ArrayInitializer ai -> arrayInitializer(ai.expressions(), ai.parameterizedType());
            case InstanceOf io when isHierarchyTest(io) -> hierarchyTest(io);
            case InstanceOf io when isInterfaceTest(io) -> value(interfaceOption(io.expression(), io.testType(), false)
                                                                 + ".is_some()");
            case InstanceOf io when io.patternVariable() == null && isObject(io.expression().parameterizedType())
                                    && RustTypes.isDowncastTarget(io.testType()) -> isInstance(io);
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
            case This t -> {
                Receiver receiver = context.method == null ? Receiver.EXCLUSIVE : context.facts.receiver(context.method);
                // 'super.add(e)' on a JDK collection base: the collection's own, not an override the class declares
                String text = t.writeSuper() && context.type != null
                              && RustTypePrinter.collectionBase(context.type, context) != null
                        ? context.receiver + ".base" : context.receiver;
                yield new Printed(text, context.receiver.equals("this") ? Held.VALUE
                        : receiver == Receiver.SHARED ? Held.SHARED : receiver == Receiver.OWNED ? Held.VALUE
                        : Held.EXCLUSIVE, false);
            }
            case ParameterInfo pi -> parameter(pi);
            case LocalVariable lv -> new Printed(RustNames.snake(lv.simpleName()), locals.getOrDefault(lv, Held.VALUE),
                    nullableLocals.getOrDefault(lv, false));
            case FieldReference fr -> field(fr);
            case DependentVariable dv -> {
                if (dv.arrayExpression() == null) yield value(unsupported(where, "array element"));
                // a nullable array (Option<Vec<…>>) is unwrapped, as Java's access would throw
                yield new Printed(dereference(expression(dv.arrayExpression())) + "[" + index(dv.indexExpression()) + "]",
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
        // an element is never an Option: the collections hold their values as they are
        if (closures.contains(pi.methodInfo())) {
            Held option = optionClosureParameters.get(pi);
            if (option != null) return new Printed(name, option, true);
            return new Printed(name, strClosureParameters.contains(pi) ? Held.STR : Held.PLACE, false);
        }
        if (movable(pi)) moved.add(name);
        Passing passing = context.passing(pi);
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
            if (RustTypePrinter.interfaceConstant(f)) {
                // a module-level const beside the trait
                String name = RustTypePrinter.staticName(f);
                String module = RustNames.module(owner.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + name);
                return new Printed(name, RustTypes.isString(f.type()) ? Held.STR : Held.VALUE, nullable);
            }
            if (RustTypePrinter.mutableStaticValue(f, context) != null) {
                // Java's static state: a static mut, touched inside the unsafe body of the method
                String name = RustTypePrinter.staticName(f);
                String module = RustNames.module(owner.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + name);
                // Rust 2024 refuses a reference to a static mut: a method call on one goes through a raw pointer
                return new Printed(RustTypes.isCopy(f.type()) ? name : "(*&raw mut " + name + ")", Held.PLACE, nullable);
            }
            if (!owner.compilationUnit().externalLibrary() && RustTypePrinter.lazyStatic(f, context)) {
                String name = RustTypePrinter.staticName(f);
                String module = RustNames.module(owner.packageName());
                if (!module.equals(context.module)) context.use("crate::" + module + "::" + name);
                if (RustTypePrinter.threadLocalStatic(f, context)) return new Printed(name + ".with(|v| v.clone())", Held.VALUE, nullable);
                // a LazyLock<i32> derefs to its value: an int where one is expected
                if (RustTypes.isCopy(f.type())) return new Printed("(*" + name + ")", Held.VALUE, nullable);
                return new Printed(name, Held.PLACE, nullable);
            }
            // a string constant is a &'static str
            return new Printed(prefix + "::" + RustTypePrinter.constantName(f),
                               RustTypes.isString(f.type()) && RustTypePrinter.constant(f) ? Held.STR : Held.VALUE, nullable);
        }
        boolean own = fr.scopeIsThis() || fr.isDefaultScope();
        String scope = own ? context.receiver : dereference(expression(fr.scope()));
        if (RustTypePrinter.constantField(f) != null) {
            // an enum's state set by its constants: a match over the variants (RustTypePrinter.enumType)
            return new Printed(scope + "." + RustTypePrinter.constantFieldMethod(f) + "()",
                               RustTypes.isString(f.type()) ? Held.STR : Held.VALUE, false);
        }
        TypeInfo scopeType = own ? context.type : fr.scope().parameterizedType().typeInfo();
        // an inherited field is in the base
        String path = scopeType == null ? "" : RustTypePrinter.basePath(scopeType, f.owner());
        String place = scope + path + "." + RustNames.snake(f.name());
        if (RustHierarchies.current().isSubMember(f.owner())) {
            // a field a class below a hierarchy's root adds: in its data, in the kind (RustHierarchies)
            place = scope + ".kind." + RustHierarchies.accessor(f.owner()) + (mutatingPlace ? "_mut()" : "()") + "."
                    + RustNames.snake(f.name());
        }
        if (own && context.cells.isCell(f)) {
            // a RefCell: directly where the object is held exclusively, else borrowed for the statement
            MethodInfo m = context.method;
            Receiver receiver = m == null || "this".equals(context.receiver) ? Receiver.EXCLUSIVE : context.facts.receiver(m);
            String access = receiver == Receiver.EXCLUSIVE ? ".get_mut()"
                    : context.facts.ignoreModifications(m) || receiver == Receiver.OWNED ? ".borrow_mut()" : ".borrow()";
            place = "(*" + place + access + ")";
        }
        if (context.boxes.isBoxed(f)) {
            // a Box (RustBoxes): its content is the place; a nullable one is read as an Option<&T>
            if (!nullable) return new Printed("(*" + place + ")", Held.PLACE, false);
            return new Printed(place + BOXED_OPTION, Held.SHARED, true);
        }
        return new Printed(place, Held.PLACE, nullable);
    }

    private static final String BOXED_OPTION = ".as_deref()";

    /** A nullable value dereferenced: Java would throw a NullPointerException, Rust panics in {@code unwrap()}. */
    private String dereference(Printed scope) {
        if (!scope.nullable) return scope.text;
        context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, null, scope.text);
        return scope.text + (mutatingPlace ? ".as_mut().unwrap()" : ".as_ref().unwrap()");
    }

    private String assignment(Assignment a, boolean statement) {
        Printed targetPrinted = mutable(a.target());
        String target = targetPrinted.text;
        boolean boxed = a.variableTarget() instanceof FieldReference fr && context.boxes.isBoxed(fr.fieldInfo());
        if (boxed && target.endsWith(BOXED_OPTION)) target = target.substring(0, target.length() - BOXED_OPTION.length());
        if (a.prefixPrimitiveOperator() != null || a.binaryOperator() != null && a.value() == null) {
            String op = a.assignmentOperatorIsPlus() ? " += 1" : " -= 1";
            if (statement) return target + op;
            // i++ as a value: a block that increments and gives the old value (++i: the new one)
            if (Boolean.TRUE.equals(a.prefixPrimitiveOperator())) return "{ " + target + op + "; " + target + " }";
            return "{ let v = " + target + "; " + target + op + "; v }";
        }
        String operator = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        Printed value = widen(expression(a.value()), a.value(), a.target().parameterizedType());
        if (!statement) {
            // '(idx = s.indexOf(x)) != -1': assigned, then read
            if (a.assignmentOperator() == null && RustTypes.isCopy(a.target().parameterizedType())
                && a.target() instanceof VariableExpression) {
                return "{ " + target + " = " + value.text + "; " + target + " }";
            }
            return unsupported(a, "assignment inside an expression");
        }
        ParameterizedType targetType = a.target().parameterizedType();
        if ("=".equals(operator) && RustShared.current().isShared(targetType)
            && (targetPrinted.held == Held.SHARED || targetPrinted.held == Held.EXCLUSIVE)
            && (a.variableTarget() instanceof ParameterInfo pi && context.passing(pi) != Passing.OWNED
                || a.variableTarget() instanceof LocalVariable lv && locals.get(lv) == Held.SHARED)) {
            // a borrowed handle walking a chain ('n = n.parent', 'head = head.getFirst()'): it borrows the next one,
            // it does not keep it; a nullable one (Option<&Shared<T>>) borrows the Option's content
            boolean exclusive = targetPrinted.held == Held.EXCLUSIVE;
            String borrowed;
            if (a.value() instanceof NullConstant) {
                borrowed = "None";
            } else if (targetPrinted.nullable) {
                borrowed = !value.nullable ? "Some(" + adapt(value, exclusive ? Held.EXCLUSIVE : Held.SHARED) + ")"
                        : value.held == Held.SHARED || value.held == Held.EXCLUSIVE ? value.text
                        : value.text + (exclusive ? ".as_mut()" : ".as_ref()");
            } else {
                borrowed = value.nullable ? value.text + (exclusive ? ".as_mut().unwrap()" : ".as_ref().unwrap()")
                        : adapt(value, exclusive ? Held.EXCLUSIVE : Held.SHARED);
            }
            return target + " = " + borrowed;
        }
        if ("+=".equals(operator) && RustTypes.isString(targetType)) {
            return target + ".push_str(" + adapt(value, Held.STR) + ")";
        }
        String text = RustTypes.isCopy(targetType) ? value.text
                : value.nullable ? ownedNullable(value, a.value(), targetType)
                : objectTarget(targetType) ? asObject(owned(value, a.value().parameterizedType()), a.value(), targetType)
                : asDyn(owned(value, targetType), a.value(), targetType);
        boolean nullableTarget = targetNullable(a.variableTarget());
        if (boxed && !target.startsWith("(*")) {
            text = RustBoxes.box(a.value() instanceof NullConstant ? "None" : text, nullableTarget, value.nullable);
        } else if (nullableTarget && !value.nullable && !(a.value() instanceof NullConstant)) {
            text = "Some(" + text + ")";
        } else if (!nullableTarget && value.nullable && !(a.value() instanceof NullConstant)
                   && !targetType.isPrimitiveExcludingVoid()) {
            // a nullable value where the verdicts hold the target non-null: Java's NPE at the next use, a panic here
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, a, text);
            text = text + ".unwrap()";
        }
        return target + " " + operator + " " + text;
    }

    // an object of a source class or record: Java compares it by identity; an enum constant, a string or a boxed
    // primitive is a value in Rust, compared by ==
    /** {@code p} as an {@code Option<&T>}. */
    private String optionalReference(Printed p) {
        if (!p.nullable) return "Some(" + adapt(p, Held.SHARED) + ")";
        return switch (p.held) {
            case SHARED -> p.text;
            case EXCLUSIVE -> p.text + ".as_deref()";
            default -> p.text + ".as_ref()";
        };
    }

    /**
     * Java's {@code equals} of objects of this type is identity: a source class that declares no {@code equals}
     * (nor does a superclass), or a member of a closed hierarchy (RustHierarchies, which prints no PartialEq).
     */
    static boolean equalsIsIdentity(ParameterizedType type) {
        if (!identity(type)) return false;
        if (RustHierarchies.current().isMember(type.typeInfo())) return true;
        for (TypeInfo t = type.typeInfo(); t != null && !t.isJavaLangObject();
             t = t.parentClass() == null ? null : t.parentClass().typeInfo()) {
            if (t.methods().stream().anyMatch(RustTypePrinter::isEquals)) return false;
        }
        return true;
    }

    private static boolean identity(ParameterizedType type) {
        TypeInfo ti = type.typeInfo();
        return ti != null && type.arrays() == 0 && !type.isTypeParameter() && !ti.compilationUnit().externalLibrary()
               && !ti.typeNature().isEnum() && !ti.isInterface();
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
        if (("==".equals(op) || "!=".equals(op)) && identity(bo.lhs().parameterizedType())
            && identity(bo.rhs().parameterizedType())) {
            // Java's == on objects is identity, which Rust's == (PartialEq) is not: the addresses compared
            Printed l = expression(bo.lhs());
            Printed r = expression(bo.rhs());
            boolean shared = RustShared.current().isShared(bo.lhs().parameterizedType())
                             || RustShared.current().isShared(bo.rhs().parameterizedType());
            if (shared && !l.nullable && !r.nullable && (isThis(bo.lhs()) || isThis(bo.rhs()))) {
                // 'this == other': self is the object, the other a handle to one
                Printed handle = isThis(bo.lhs()) ? r : l;
                String eqThis = "std::ptr::eq(" + context.receiver + ", &**" + adapt(handle, Held.SHARED) + ")";
                return value("==".equals(op) ? eqThis : "!" + eqThis);
            }
            if (shared) {
                // two handles: the same object behind them (Shared::is), not the handles' own addresses
                String eqShared = !l.nullable && !r.nullable
                        ? "crate::jrt::Shared::is(" + adapt(l, Held.SHARED) + ", " + adapt(r, Held.SHARED) + ")"
                        : "crate::jrt::same_shared(" + optionalReference(l) + ", " + optionalReference(r) + ")";
                return value("==".equals(op) ? eqShared : "!" + eqShared);
            }
            String eq = !l.nullable && !r.nullable
                    ? "std::ptr::eq(" + adapt(l, Held.SHARED) + ", " + adapt(r, Held.SHARED) + ")"
                    // either may be null: the same object, or both null
                    : "crate::jrt::same(" + optionalReference(l) + ", " + optionalReference(r) + ")";
            return value("==".equals(op) ? eq : "!" + eq);
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
    private static final Pattern AS_CAST = Pattern.compile(" as [\\w:<>]+$");

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
        if (p.nullable && isObject(type)) {
            return new String[]{"{}", p.text + ".as_ref().map(|v| crate::jrt::to_string(v)).unwrap_or_else(|| \"null\".to_string())"};
        }
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
            boolean debug = Set.of("Vec", "HashSet", "BTreeSet", "HashMap", "BTreeMap", "VecDeque", "Option",
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
            boolean exclusive = mutatingPlace && !RustTypes.isCopy(to);
            String text = inner.text + (inner.nullable ? (exclusive ? ".as_mut().unwrap()" : ".unwrap()") : "")
                          + (exclusive ? ".downcast_mut::<" : ".downcast_ref::<") + target + ">().unwrap()";
            return RustTypes.isCopy(to) ? value("*" + text)
                    : new Printed(text, exclusive ? Held.EXCLUSIVE : Held.SHARED, false);
        }
        TypeInfo struct = interfaceDowncast(c.expression(), to);
        if (struct != null) {
            // a value of an interface as a class: the trait's conversion, a panic where Java throws
            boolean exclusive = mutatingPlace && !context.borrows.hasLifetime(struct);
            String text = (inner.nullable ? inner.text + (exclusive ? ".as_mut().unwrap()" : ".as_ref().unwrap()")
                    : inner.text) + "."
                          + RustHierarchies.downcastName(struct) + (exclusive ? "_mut" : "") + "().unwrap()";
            return new Printed(text, exclusive ? Held.EXCLUSIVE : Held.SHARED, false);
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
        List<String> types = closureParameters();
        boolean optional = optionalResult;
        optionalResult = false;
        boolean copied = copiedResult;
        copiedResult = false;
        List<String> names = m.parameters().stream().map(p -> RustNames.snake(p.name())).toList();
        String params = typed(names, types);
        if (types != null && types.size() == names.size()) {
            for (int i = 0; i < names.size(); i++) {
                String t = types.get(i);
                if ("&str".equals(t)) strClosureParameters.add(m.parameters().get(i));
                // fernflower's iterateExprents(exprent -> …) of a nullable Function parameter: the body unwraps it
                if (t.startsWith("Option<")) {
                    optionClosureParameters.put(m.parameters().get(i), t.startsWith("Option<&str>") ? Held.STR
                            : t.startsWith("Option<&mut ") ? Held.EXCLUSIVE : t.startsWith("Option<&") ? Held.SHARED
                            : Held.VALUE);
                }
            }
        }
        Expression single = lambda.singleExpression();
        MethodInfo outer = context.method;
        closures.add(m);
        try {
            context.method = m;
            if (single != null) {
                // a Consumer's body is a statement: 'map.put(k, v)' returns a value Rust's 'for_each' does not take
                Expression outerStatement = statement;
                if (m.noReturnValue()) statement = single;
                Printed p;
                try {
                    p = expression(single);
                } finally {
                    statement = outerStatement;
                }
                String body = p.text;
                if (m.noReturnValue()) return value("|" + params + "| { " + body + "; }");
                if (p.nullable && optional) {
                    // Optional.map's function may return null: an empty Optional, Rust's and_then over an Option
                    String owned = p.held == Held.SHARED && (RustTypes.isString(single.parameterizedType())
                            || RustTypes.isImmutableValue(single.parameterizedType(), context.facts)) ? ".cloned()" : "";
                    return new Printed("|" + params + "| " + body + owned, Held.VALUE, true);
                }
                if (copied) {
                    if (p.nullable) context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, single, body);
                    return value("|" + params + "| " + copied(p, single));
                }
                if (p.nullable) {
                    // an element is never an Option: Java's null in the result, a panic here
                    context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, single, body);
                    body = owned(new Printed(body + ".unwrap()", p.held, false), single.parameterizedType());
                } else if (p.held == Held.STR && RustTypes.isString(single.parameterizedType())) {
                    // a closure's String result is owned: 'x -> "s"' in a stream mapped to a List<String>
                    body = owned(p, single.parameterizedType());
                }
                return value("|" + params + "| " + body);
            }
            return value("|" + params + "| {\n" + statements(m.methodBody(), 1) + "}");
        } finally {
            context.method = outer;
        }
    }

    /**
     * {@code fn} applied to the values named {@code args}, in parentheses: a lambda's single expression with its
     * parameters standing for them, a method reference's call; anything else is called. A closure literal called on
     * the spot ({@code (|x| x.f())(a)}) leaves rustc without the type of its parameter.
     */
    String applied(Expression fn, List<String> args) {
        Expression f = CstQueries.unwrap(fn);
        if (f instanceof Lambda l && l.singleExpression() != null && l.methodInfo().parameters().size() == args.size()) {
            List<ParameterInfo> ps = l.methodInfo().parameters();
            for (int i = 0; i < ps.size(); i++) aliases.put(ps.get(i), args.get(i));
            try {
                return "(" + expression(l.singleExpression()).text + ")";
            } finally {
                ps.forEach(aliases::remove);
            }
        }
        if (f instanceof MethodReference mr && mr.concreteParameterTypes().size() == args.size()) {
            String body = methodReferenceBody(mr, args);
            if (body != null) return "(" + body + ")";
        }
        return "(" + expression(fn).text + ")(" + String.join(", ", args) + ")";
    }

    /**
     * A method reference as a closure: {@code Order::customer} is {@code |x| x.customer()}, {@code Type::of} is
     * {@code |x| Type::of(x)}, {@code this::add} is {@code |x| self.add(x)}.
     */
    private Printed methodReference(MethodReference mr) {
        int n = mr.concreteParameterTypes().size();
        List<String> params = new ArrayList<>();
        for (int i = 0; i < n; i++) params.add(n == 1 ? "x" : "x" + (i + 1));
        List<String> types = closureParameters();
        String body = methodReferenceBody(mr, params);
        if (body == null) return value(unsupported(mr, "method reference " + mr.methodInfo().fullyQualifiedName()));
        return value("|" + typed(params, types) + "| " + body);
    }

    /** The parameter types of the closure {@link #closureTarget} asks for, consumed; {@code null} when there is none. */
    private List<String> closureParameters() {
        ParameterizedType target = closureTarget;
        closureTarget = null;
        if (target == null || target.typeInfo() == null) return null;
        if (target.typeInfo().compilationUnit().externalLibrary()) return RustTypes.closureParameters(target, context, null);
        return RustTypePrinter.closureParameters(target.typeInfo(), context);
    }

    private static String typed(List<String> names, List<String> types) {
        if (types == null || types.size() != names.size()) return String.join(", ", names);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) out.add(names.get(i) + ": " + types.get(i));
        return String.join(", ", out);
    }

    private static boolean isClosure(Expression e) {
        Expression c = CstQueries.unwrap(e);
        return c instanceof Lambda || c instanceof MethodReference;
    }

    /** Optional.map's function: {@code nullable} when its result may be null, and it is then {@code and_then}'s. */
    Printed optionalMapper(Expression e) {
        optionalResult = true;
        try {
            return expression(e);
        } finally {
            optionalResult = false;
        }
    }

    /** {@code e}, a lambda or method reference, as a closure implementing {@code target}. */
    private Printed closure(Expression e, ParameterizedType target) {
        closureTarget = target;
        try {
            return expression(e);
        } finally {
            closureTarget = null;
        }
    }

    /** The call a method reference makes, its parameters named {@code params}; {@code null} when not printable. */
    private String methodReferenceBody(MethodReference mr, List<String> params) {
        MethodInfo m = mr.methodInfo();
        int n = params.size();
        String closure = "";
        if (m.typeInfo().compilationUnit().externalLibrary()) {
            if (!m.isStatic() && !(mr.scope() instanceof TypeExpression)) {
                // bound: 'out::add' adds to out, the closure's parameter its argument
                ParameterizedType scope = mr.scope().parameterizedType();
                String kind = scope.typeInfo() == null ? null : RustTypes.mapped(scope.typeInfo());
                String body = RustJdk.boundReference(m, kind, expression(mr.scope()).text, params);
                // a Consumer drops add's boolean
                return body != null && mr.concreteReturnType().isVoid() ? "{ " + body + "; }" : body;
            }
            String jdk = RustJdk.reference(m, params);
            // an entry's key or value is a reference into the map: a copy, or a clone for a string
            if (jdk != null && (jdk.endsWith(".0") || jdk.endsWith(".1"))) {
                ParameterizedType result = mr.concreteReturnType();
                if (RustTypes.isCopy(result)) jdk = "*" + jdk;
                else if (RustTypes.isString(result) || RustTypes.isImmutableValue(result, context.facts)) jdk += ".clone()";
            }
            return jdk;
        }
        String owner = m.typeInfo() == context.type ? "Self" : RustNames.type(m.typeInfo());
        if (m.typeInfo() != context.type) {
            String module = RustNames.module(m.typeInfo().packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(m.typeInfo()));
        }
        String name = RustNames.method(m);
        if (m.isConstructor() || m.isStatic()) {
            return closure + owner + "::" + name + "(" + passed(params, m) + ")";
        }
        if (mr.scope() instanceof TypeExpression) {
            // unbound: the first parameter is the object
            if (params.isEmpty()) return null;
            return closure + params.getFirst() + "." + name + "(" + passed(params.subList(1, n), m) + ")";
        }
        return closure + expression(mr.scope()).text + "." + name + "(" + passed(params, m) + ")";
    }

    // ---- calls

    /**
     * {@code e.getDestination().preds.add(e)} of a shared {@code e} (RustShared): the argument's clone is taken first,
     * {@code { let __e = e.clone(); e.get_destination_mut().preds.push(__e) }}. Inline, it is evaluated while the
     * receiver chain holds {@code e} mutably, which two-phase borrows do not cover when the receiver is itself a
     * {@code &mut} call chain (E0502).
     */
    private static boolean isThis(Expression e) {
        return CstQueries.unwrap(e) instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    private Printed sharedCloneFirst(MethodCall mc, Printed printed) {
        if (mc.object() == null || mc.objectIsImplicit()) return printed;
        Expression root = CstQueries.unwrap(mc.object());
        while (true) {
            if (root instanceof MethodCall inner && inner.object() != null && !inner.objectIsImplicit()) {
                root = CstQueries.unwrap(inner.object());
            } else if (root instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                       && fr.scope() != null && !fr.scopeIsThis()) {
                root = CstQueries.unwrap(fr.scope());
            } else {
                break;
            }
        }
        if (!(root instanceof VariableExpression rve)
            || !(rve.variable() instanceof LocalVariable || rve.variable() instanceof ParameterInfo)
            || !RustShared.current().isShared(rve.parameterizedType())) {
            return printed;
        }
        boolean passed = mc.parameterExpressions().stream().anyMatch(a -> CstQueries.unwrap(a) instanceof VariableExpression ave
                                                                          && ave.variable().equals(rve.variable()));
        String name = expression(rve).text;
        String clone = name + ".clone()";
        int at = printed.text.indexOf(clone);
        if (!passed || !printed.text.startsWith(name + ".") || at <= 0) return printed;
        String temp = "__" + name.replace("r#", "");
        return new Printed("{ let " + temp + " = " + clone + "; " + printed.text.replace(clone, temp) + " }",
                printed.held, printed.nullable);
    }

    /* Enum.equals on an enum of the sources ('EdgeType.BREAK.equals(edge.getType())'): the variants' ==. */
    private static boolean sourceEnumEquals(MethodCall mc) {
        if (!RustTypePrinter.isEquals(mc.methodInfo()) || mc.object() == null || mc.objectIsImplicit()) return false;
        TypeInfo ti = mc.object().parameterizedType().typeInfo();
        return ti != null && ti.typeNature().isEnum() && !ti.compilationUnit().externalLibrary()
               && mc.object().parameterizedType().arrays() == 0;
    }

    private Printed methodCall(MethodCall mc) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary() && !sourceEnumEquals(mc)) {
            boolean outer = mutatingPlace;
            Printed mapped;
            try {
                mapped = RustJdk.call(mc, this);
            } finally {
                mutatingPlace = outer;
            }
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
            if (RustShared.current().isShared(mc.object().parameterizedType())) {
                // two handles' objects compared with the class's own equality, not the handles' identity
                return value("crate::jrt::Shared::equals(" + adapt(expression(mc.object()), Held.SHARED) + ", "
                             + adapt(expression(mc.parameterExpressions().getFirst()), Held.SHARED) + ")");
            }
            Printed left = expression(mc.object());
            Printed right = expression(mc.parameterExpressions().getFirst());
            if (right.nullable && !(mc.parameterExpressions().getFirst() instanceof NullConstant)) {
                // a.equals(b), b nullable: Java's equals(null) is false, which an Option's equality gives exactly,
                // both sides lifted to Option<&T>; a nullable a is unwrapped, Java's NullPointerException
                String l = optionalReference(left);
                if (left.nullable) {
                    context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, mc, left.text);
                    l = "Some(" + l + ".unwrap())";
                }
                return value(l + " == " + optionalReference(right));
            }
            return value(adapt(left, Held.SHARED) + " == " + adapt(right, Held.SHARED));
        }
        // a getter the caller modifies through: its exclusive twin (RustGetters)
        boolean mutableGetter = mutatingPlace && context.mutableGetters.contains(callee);
        String name = RustNames.method(callee) + (mutableGetter ? RustGetters.SUFFIX : "");
        if (mc.object() instanceof VariableExpression sve && sve.variable() instanceof This t && t.writeSuper()
            && RustHierarchies.current().isMember(callee.typeInfo())) {
            // super.m() in a closed hierarchy: the parent's own implementation, not the dispatching method
            name = RustHierarchies.current().implementationName(callee);
        }
        boolean exclusive = !callee.isStatic() && (mutableGetter || context.facts.receiver(callee) == Receiver.EXCLUSIVE);
        List<String> exclusiveArgs = exclusive ? withoutSelfBorrowsList(mc.parameterExpressions(), callee) : null;
        String args = exclusive ? String.join(", ", exclusiveArgs) : arguments(mc.parameterExpressions(), callee);
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
            String object;
            Printed borrowedHandle = own || !exclusive || !(CstQueries.unwrap(mc.object()) instanceof MethodCall)
                                     || !RustShared.current().isShared(mc.object().parameterizedType()) ? null
                    : expression(mc.object());
            if (own) {
                object = context.receiver;
            } else if (borrowedHandle != null && borrowedHandle.held == Held.SHARED) {
                // a shared object a call returns borrowed, modified ('stat.getFirst().removeSuccessor(e)'): a handle of
                // its own, as Java's reference is, rather than an exclusive borrow of what the getter reads
                if (borrowedHandle.nullable) context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, null, borrowedHandle.text);
                object = borrowedHandle.text + (borrowedHandle.nullable ? ".cloned().unwrap()" : ".clone()");
            } else if (exclusive) {
                boolean outer = mutatingPlace(true);
                try {
                    object = dereference(expression(mc.object()));
                } finally {
                    mutatingPlace = outer;
                }
            } else {
                object = dereference(expression(mc.object()));
            }
            TypeInfo objectType = own ? context.type : mc.object().parameterizedType().typeInfo();
            // an inherited method is the base's; super.m() too
            String path = objectType == null ? "" : RustTypePrinter.basePath(objectType, callee.typeInfo());
            // through a Shared handle's DerefMut only: a plain &mut receiver has Rust's two-phase borrow
            String hoisted = exclusive && !own && RustShared.current().isShared(mc.object().parameterizedType())
                    ? hoistReceiverReads(object, exclusiveArgs, callee) : null;
            if (hoisted != null) {
                int bar = hoisted.indexOf('\u0000');
                call = "{ " + hoisted.substring(0, bar) + object + path + "." + name + "(" + hoisted.substring(bar + 1) + ") }";
            } else {
                call = object + path + "." + name + "(" + args + ")";
            }
        }
        boolean nullable = !callee.isVoid() && RustFacts.nullable(context.facts.returnType(callee));
        if (callee.isVoid() || RustTypes.isCopy(callee.returnType())) return new Printed(call, Held.VALUE, nullable);
        boolean borrows = context.facts.resultBorrowsReceiver(callee) && context.facts.receiver(callee) != Receiver.NONE
                          || context.facts.resultBorrows(callee) != null
                          // a record's own accessor, which RustTypePrinter.accessor prints as a borrow
                          || callee.isSynthetic() && callee.typeInfo().typeNature().isRecord() && callee.parameters().isEmpty()
                             && !callee.isStatic();
        Held held = !borrows ? Held.VALUE : RustTypes.isString(callee.returnType()) ? Held.STR : Held.SHARED;
        if (mutableGetter) held = Held.EXCLUSIVE;
        return new Printed(call, held, nullable);
    }

    /** {@code {a, b}}, an array of {@code type}: a vec of its elements, owned as its component type. */
    private Printed arrayInitializer(List<Expression> elements, ParameterizedType type) {
        ParameterizedType component = type.arrays() > 0 ? type.componentType() : null;
        return value("vec![" + elements.stream().map(x -> ownedAs(x, component)).collect(Collectors.joining(", ")) + "]");
    }

    private Printed constructorCall(ConstructorCall cc) {
        if (cc.anonymousClass() != null) return value(unsupported(cc, "anonymous class"));
        ParameterizedType type = cc.parameterizedType();
        if (type.arrays() > 0) {
            if (cc.arrayInitializer() != null) return arrayInitializer(cc.arrayInitializer().expressions(), type);
            if (cc.parameterExpressions().size() == 1) {
                ParameterizedType component = type.componentType();
                String n = index(cc.parameterExpressions().getFirst());
                if (component.typeInfo() != null && component.typeInfo().isJavaLangObject() && component.arrays() == 0) {
                    // Java's nulls, until the array is filled
                    return value("(0.." + n + ").map(|_| crate::jrt::null()).collect::<Vec<Box<dyn std::any::Any>>>()");
                }
                String element = RustTypes.empty(component, false, context, cc);
                TypeInfo ct = component.typeInfo();
                if (component.arrays() == 0 && ct != null && !ct.compilationUnit().externalLibrary() && !ct.isInterface()
                    && !ct.typeNature().isEnum() && ct.typeParameters().isEmpty()
                    && (ct.constructors().isEmpty() || ct.constructors().stream().anyMatch(c -> c.parameters().isEmpty()))) {
                    // a class with a constructor without parameters: an object of it where Java has a null
                    String owned = RustTypes.owned(component, context, cc);
                    String plain = owned.startsWith("Rc<") ? owned.substring(3, owned.length() - 1) : owned;
                    element = owned.startsWith("Rc<") ? "Rc::new(" + plain + "::new())" : plain + "::new()";
                }
                if (RustTypes.isCopy(component) || RustTypes.isString(component) || component.arrays() > 0) {
                    return value("vec![" + element + "; " + n + "]");
                }
                // vec![x; n] clones x, which a class need not allow: each element built on its own
                return value("(0.." + n + ").map(|_| " + element + ").collect::<Vec<_>>()");
            }
            return value(unsupported(cc, "multi-dimensional array"));
        }
        MethodInfo constructor = cc.constructor();
        TypeInfo ti = type.typeInfo();
        if (ti.compilationUnit().externalLibrary()) {
            Printed mapped = RustJdk.construct(cc, this);
            if (mapped != null) return mapped;
            context.message(RustPrintCode.UNMAPPED_JDK_CALL, cc, ti.fullyQualifiedName());
            // the opaque stand-in's path, as its type is printed: a bare simple name is not in scope
            return value(RustTypes.owned(ti.asSimpleParameterizedType(), context, cc) + "::new("
                         + arguments(cc.parameterExpressions(), null) + ")");
        }
        // a generic struct, or one with a lifetime, is named: 'Self' would fix the impl's type arguments or lifetime
        String owner = ti == context.type && !context.borrows.hasLifetime(ti) && ti.typeParameters().isEmpty()
                ? "Self" : RustNames.type(ti);
        if (ti != context.type) {
            String module = RustNames.module(ti.packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.type(ti));
        }
        String name = constructor == null ? "new" : RustNames.method(constructor);
        String call = owner + "::" + name + "(" + arguments(cc.parameterExpressions(), constructor) + ")";
        // @Immutable(hc=true): the new object is held as an Rc, and shared by cloning that
        if (RustTypes.isShared(ti, context.facts)) {
            context.use("std::rc::Rc");
            return value("Rc::new(" + call + ")");
        }
        // Java's shared object: the new object in its handle (RustShared)
        if (RustShared.current().isShared(ti)) return value("crate::jrt::Shared::new(" + call + ")");
        return value(call);
    }

    /**
     * The arguments of a call that borrows {@code self} exclusively: a string read from {@code self} is passed as a
     * copy, as a borrow of it could not live beside the exclusive one (Java shares an immutable string freely).
     */
    private String withoutSelfBorrows(List<Expression> arguments, MethodInfo callee) {
        return String.join(", ", withoutSelfBorrowsList(arguments, callee));
    }

    private List<String> withoutSelfBorrowsList(List<Expression> arguments, MethodInfo callee) {
        boolean outer = mutatingPlace(false); // an argument is read, also of a call on a place written through
        try {
            return withoutSelfBorrows0(arguments, callee);
        } finally {
            mutatingPlace = outer;
        }
    }

    private List<String> withoutSelfBorrows0(List<Expression> arguments, MethodInfo callee) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Expression a = arguments.get(i);
            // as in arguments0: an argument the callee modifies is a place written through
            Printed p = modifiedArgument(arguments, i, callee) ? mutable(a) : expression(a);
            ParameterInfo pi = i < callee.parameters().size() ? callee.parameters().get(i) : null;
            boolean fromSelf = p.text.startsWith(context.receiver + ".") && RustTypes.isString(a.parameterizedType())
                               && !p.text.contains("(");
            // a string borrowed from a local or a lookup may be borrowed from self too ('let sku = self.m.get(k)'):
            // copied, as an immutable string can be
            boolean borrowed = p.held == Held.SHARED && RustTypes.isString(a.parameterizedType())
                               && !(a instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo);
            if (pi != null && (fromSelf || borrowed) && context.passing(pi) == Passing.SHARED) {
                String text = "&" + p.text + ".clone()";
                boolean nullable = RustFacts.nullable(context.facts.parameter(pi));
                if (p.nullable) text = p.text + (fromSelf ? ".clone().as_deref()" : ".cloned().as_deref()");
                out.add(nullable && !p.nullable ? "Some(" + text + ")" : text);
            } else {
                out.add(pi == null ? p.text : argument(p, pi, a));
            }
        }
        return out;
    }

    /*
     'stack.insertByOffset(d, stack.getByOffset(-1).copy())': the call borrows its object exclusively while an argument
     still reads it, which Rust's two-phase borrow does not allow through a Shared handle's DerefMut (E0502). An
     argument passed by value that reads the object is computed first, as Java evaluates it before the call anyway:
     '{ let __a1 = …; stack.insert_by_offset(d, __a1) }'. Answers the lets and, after a NUL, the arguments; null when
     no argument needs it, or the object is not a plain local or parameter.
     */
    private String hoistReceiverReads(String object, List<String> printed, MethodInfo callee) {
        if (!object.matches("(r#)?[a-z_][a-z0-9_]*") || "self".equals(object)) return null;
        java.util.regex.Pattern root = java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(object) + "\\b");
        StringBuilder lets = new StringBuilder();
        List<String> out = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < printed.size(); i++) {
            String text = printed.get(i);
            ParameterInfo pi = i < callee.parameters().size() ? callee.parameters().get(i) : null;
            boolean byValue = pi != null && (RustTypes.isCopy(pi.parameterizedType()) || context.passing(pi) == Passing.OWNED);
            if (byValue && root.matcher(text).find() && !text.startsWith("&")) {
                lets.append("let __a").append(i).append(" = ").append(text).append("; ");
                out.add("__a" + i);
                any = true;
            } else {
                out.add(text);
            }
        }
        return any ? lets + "\u0000" + String.join(", ", out) : null;
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
            String text = RustTypes.isCopy(type) ? p.text : switch (context.passing(pi)) {
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
        boolean outer = mutatingPlace(false);
        try {
            return arguments0(arguments, callee);
        } finally {
            mutatingPlace = outer;
        }
    }

    private String arguments0(List<Expression> arguments, MethodInfo callee) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Printed p = modifiedArgument(arguments, i, callee) ? mutable(arguments.get(i)) : expression(arguments.get(i));
            if (callee == null || i >= callee.parameters().size()) {
                out.add(p.text);
                continue;
            }
            ParameterInfo pi = callee.parameters().get(Math.min(i, callee.parameters().size() - 1));
            out.add(argument(p, pi, arguments.get(i)));
        }
        return String.join(", ", out);
    }

    // an argument the callee modifies is a place written through: a getter's exclusive twin
    private boolean modifiedArgument(List<Expression> arguments, int i, MethodInfo callee) {
        ParameterInfo modified = callee == null || callee.parameters().isEmpty() ? null
                : callee.parameters().get(Math.min(i, callee.parameters().size() - 1));
        return modified != null && !callee.typeInfo().compilationUnit().externalLibrary()
               && !RustTypes.isCopy(modified.parameterizedType()) && !isClosure(arguments.get(i))
               && context.passing(modified) == RustFacts.Passing.EXCLUSIVE;
    }

    private String argument(Printed p, ParameterInfo pi, Expression argument) {
        // a type parameter takes the argument's own type: Pair<String>'s T is a String, owned
        ParameterizedType type = pi.parameterizedType().isTypeParameter() && argument.parameterizedType().typeInfo() != null
                ? argument.parameterizedType() : pi.parameterizedType();
        if (isClosure(argument) && RustTypes.isFunctional(type)) {
            // 'Function<T, R> f' of a generic method: the closure's parameters are the call's types, not its T
            ParameterizedType concrete = argument.parameterizedType();
            Printed c = closure(argument, mentionsMethodTypeParameter(type) && concrete.typeInfo() != null
                                          && RustTypes.isFunctional(concrete) ? concrete : type);
            // a closure kept by the callee is a Box<dyn ...>; one it only calls is borrowed as a &dyn
            String text = switch (context.passing(pi)) {
                case OWNED -> boxed(c.text);
                case SHARED -> "&" + c.text;
                case EXCLUSIVE -> "&mut " + c.text;
            };
            return RustFacts.nullable(context.facts.parameter(pi)) ? "Some(" + text + ")" : text;
        }
        p = widen(p, argument, type);
        boolean nullable = RustFacts.nullable(context.facts.parameter(pi));
        String handle = sharedThis(p, type);
        if (handle != null && !RustTypes.isCopy(type)) {
            // 'this' passed where a Shared is expected, borrowed or kept: its own handle
            String text = switch (context.passing(pi)) {
                case OWNED -> handle;
                case SHARED -> "&" + handle;
                case EXCLUSIVE -> "&mut " + handle;
            };
            return nullable ? "Some(" + text + ")" : text;
        }
        String objectBorrow = objectBorrow(p, pi, argument, nullable);
        if (objectBorrow != null) return objectBorrow;
        // a type parameter is passed as the callee declares it, borrowed or owned, also for an int: VBStyleCollection's
        // 'removeWithKey(K key)' takes a &K, so its Integer key is a &i32
        boolean copy = RustTypes.isCopy(type) && !pi.parameterizedType().isTypeParameter();
        String text;
        if (copy) {
            text = p.text;
        } else {
            text = switch (context.passing(pi)) {
                case SHARED -> adapt(p, RustTypes.isString(type) ? Held.STR : Held.SHARED);
                // a borrowed handle where one to modify through is expected: a clone of it is the same object
                case EXCLUSIVE -> p.held == Held.SHARED && !p.nullable && RustShared.current().isShared(type)
                        ? "&mut " + p.text + ".clone()" : adapt(p, Held.EXCLUSIVE);
                case OWNED -> p.nullable ? ownedNullable(p, argument, type) : asDyn(
                        asObject(owned(p, objectTarget(type) ? argument.parameterizedType() : type), argument, type),
                        argument, type);
            };
        }
        if (nullable && !p.nullable && !(argument instanceof NullConstant)) return "Some(" + text + ")";
        if (!nullable && p.nullable) {
            // a borrow of the Option's content: '&operand.unwrap()' would move the Option out of a local or a field
            if (!copy && (p.held == Held.VALUE || p.held == Held.PLACE) && !"None".equals(p.text)) {
                switch (context.passing(pi)) {
                    case SHARED -> {
                        return p.text + (RustTypes.isString(type) ? ".as_deref()" : ".as_ref()") + ".unwrap()";
                    }
                    case EXCLUSIVE -> {
                        return p.text + ".as_mut().unwrap()";
                    }
                    default -> { }
                }
            }
            return text + ".unwrap()";
        }
        if (nullable && p.nullable && !copy && context.passing(pi) == Passing.SHARED) {
            return nullableBorrow(p, argument, RustTypes.isString(type), type.arrays() > 0);
        }
        if (nullable && p.nullable && !copy && context.passing(pi) == Passing.EXCLUSIVE
            && !RustTypes.isString(type) && type.arrays() == 0) {
            return nullableExclusive(p, argument, text, RustShared.current().isShared(type));
        }
        return text;
    }

    /**
     * A value of a known type where a borrowed {@code Object} is expected ({@code new ConstExprent(type, 1, ...)},
     * {@code &Box<dyn Any>}): a box of its own value, borrowed for the call. Java passes the reference; the callee only
     * reads it, so a copy says the same. {@code null} when this does not apply: an Object passed on, a value kept
     * (OWNED, {@link #asObject}), or a type with a lifetime, which {@code dyn Any} cannot hold.
     */
    private String objectBorrow(Printed p, ParameterInfo pi, Expression argument, boolean nullable) {
        if (!objectTarget(pi.parameterizedType()) || isObject(argument.parameterizedType())
            || argument instanceof NullConstant || "None".equals(p.text) || context.passing(pi) == Passing.OWNED
            || argument.parameterizedType().typeInfo() == null) {
            return null;
        }
        String own = RustTypes.owned(argument.parameterizedType(), context, argument);
        if (own.contains("'") || own.contains("dyn ")) return null;
        context.use("std::any::Any");
        boolean exclusive = context.passing(pi) == Passing.EXCLUSIVE;
        if (p.nullable) {
            String boxed = optionText(argument) + ".map(|v| Box::new(v) as Box<dyn Any>)";
            if (!nullable) return (exclusive ? "&mut " : "&") + boxed + ".unwrap()";
            return boxed + (exclusive ? ".as_mut()" : ".as_ref()");
        }
        String value = RustTypes.isCopy(argument.parameterizedType()) ? p.text : owned(p, argument.parameterizedType());
        String text = (exclusive ? "&mut " : "&") + "(Box::new(" + value + ") as Box<dyn Any>)";
        return nullable ? "Some(" + text + ")" : text;
    }

    /**
     * A nullable value where an {@code Option<&mut T>} is expected: {@code None} for null, an owned {@code Option<T>}
     * place (a field) as {@code .as_mut()} rather than {@code &mut} of the Option; one that is already an {@code
     * Option<&mut T>} (a parameter passed on) is reborrowed, {@code .as_deref_mut()}, so that it is not moved into the
     * first call (fernflower's {@code measureBytecode(values)} in a loop, then again).
     */
    private static String nullableExclusive(Printed p, Expression argument, String text, boolean shared) {
        if (argument instanceof NullConstant || "None".equals(p.text)) return "None";
        return switch (p.held) {
            case VALUE, PLACE -> p.text + ".as_mut()";
            case EXCLUSIVE -> p.text + ".as_deref_mut()";
            // an Option<&Shared<T>>: a clone of the handle, the same object, to modify through
            case SHARED -> shared ? p.text + ".cloned().as_mut()" : text;
            default -> text;
        };
    }

    /**
     * A nullable value where an {@code Option<&T>} ({@code Option<&str>}, {@code Option<&[T]>}) is expected:
     * {@code None} for null, a borrow of an owned {@code Option<T>}'s content, a borrowed string's {@code &String} as
     * {@code &str}, an array's {@code &Vec} as a slice: an Option does not coerce its content.
     */
    private static String nullableBorrow(Printed p, Expression argument, boolean string, boolean array) {
        if (argument instanceof NullConstant || "None".equals(p.text)) return "None";
        return switch (p.held) {
            case STR -> p.text;
            case VALUE, PLACE -> p.text + (string || array ? ".as_deref()" : ".as_ref()");
            // &s[..]: a &String's and a &str's str alike, a &Vec's and a slice's slice (as_str, as_slice are not
            // stable on str and slices)
            case SHARED, EXCLUSIVE -> string ? p.text + ".map(|s| &s[..])"
                    : array ? p.text + ".map(|v| &v[..])"
                    // an Option<&mut T> reborrowed as the Option<&T> expected, and not moved into the call
                    : p.held == Held.EXCLUSIVE ? p.text + ".as_deref()" : p.text;
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

    /**
     * A parameter moved in ({@code T}) and read once, outside a loop and a closure: that read takes it. Java's copy
     * of it ({@code List.copyOf(p)}, {@code new HashSet<>(p)}) or a string's clone is the value itself, as nothing reads
     * it afterwards; rust-printing.md's "no clone()".
     */
    boolean movable(ParameterInfo pi) {
        return movable.computeIfAbsent(pi, p -> {
            if (context.passing(p) != Passing.OWNED || RustTypes.isCopy(p.parameterizedType())) return false;
            Block body = p.methodInfo().methodBody();
            if (body == null) return false;
            int[] reads = {0};
            boolean[] repeated = {false};
            body.visit(e -> {
                if (e instanceof LoopStatement || e instanceof Lambda || e instanceof MethodReference) {
                    if (reads(e, p) > 0) repeated[0] = true;
                    return false;
                }
                if (e instanceof VariableExpression ve && p.equals(ve.variable())) reads[0]++;
                return true;
            });
            return !repeated[0] && reads[0] == 1;
        });
    }

    /** {@code e} is a parameter that {@link #movable} says its one read takes. */
    boolean movable(Expression e) {
        return CstQueries.unwrap(e) instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo pi
               && !closures.contains(pi.methodInfo()) && movable(pi);
    }

    private static int reads(Element element, ParameterInfo p) {
        int[] reads = {0};
        element.visit(e -> {
            if (e instanceof VariableExpression ve && p.equals(ve.variable())) reads[0]++;
            return true;
        });
        return reads[0];
    }

    /** {@code p} where an owned value is expected: moved, except that a borrowed string becomes a new one. */
    private String owned(Printed p, ParameterizedType type) {
        if ("None".equals(p.text)) return p.text;
        String handle = sharedThis(p, type);
        if (handle != null) return handle;
        // a Shared handle kept: the clone is the shared object, also of one held &mut
        if (RustShared.current().isShared(type) && p.held == Held.EXCLUSIVE) {
            return p.text + (p.nullable ? ".cloned()" : ".clone()");
        }
        if (p.held == Held.VALUE && moved.contains(p.text)) return p.text;
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
            // an Option<&T> copies its content with cloned(); an owned Option<T> (a local, a field) is cloned whole
            return p.text + (p.nullable && p.held == Held.SHARED ? ".cloned()" : ".clone()");
        }
        return p.text;
    }

    /**
     * {@code this} where a {@code Shared<T>} is expected (RustShared): the handle of the object {@code self} is,
     * {@code Shared::of(self)}; null otherwise. Inside its own constructor the object is not in a handle yet.
     */
    private String sharedThis(Printed p, ParameterizedType type) {
        if (!RustShared.current().isShared(type) || context.type == null
            || !p.text.equals(context.receiver) && !p.text.equals("*" + context.receiver)) {
            return null;
        }
        if (context.method != null && context.method.isConstructor() || "this".equals(context.receiver)) {
            context.message(RustPrintCode.SHARED_THIS_IN_CONSTRUCTOR, context.method, null);
            return "crate::jrt::Shared::of(&" + context.receiver + ")";
        }
        return "crate::jrt::Shared::of(" + context.receiver + ")";
    }

    /**
     * {@code Map<String, Object>}: Rust infers a collection's element type from its first insert ({@code Box<f64>}),
     * so the declaration states it ({@code Box<dyn Any>}).
     */
    private static boolean mentionsMethodTypeParameter(ParameterizedType type) {
        if (type.isTypeParameter()) return type.typeParameter().isMethodTypeParameter();
        return type.parameters().stream().anyMatch(RustBodyPrinter::mentionsMethodTypeParameter);
    }

    /* 'new ArrayList<>()': Rust infers the element type from a later insert, which a list only read or cleared lacks */
    private static boolean emptyCollection(Expression init) {
        return CstQueries.unwrap(init) instanceof ConstructorCall cc && cc.parameterExpressions().isEmpty()
               && cc.anonymousClass() == null && !cc.parameterizedType().parameters().isEmpty()
               && cc.constructor() != null && cc.constructor().typeInfo().compilationUnit().externalLibrary()
               && RustTypes.mapped(cc.constructor().typeInfo()) != null;
    }

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
    /** Whether the place being printed is written through; sets it to {@code value} and answers the old one. */
    boolean mutatingPlace(boolean value) {
        boolean old = mutatingPlace;
        mutatingPlace = value;
        return old;
    }

    // e printed as a place written through
    private Printed mutable(Expression e) {
        boolean outer = mutatingPlace(true);
        try {
            return expression(e);
        } finally {
            mutatingPlace = outer;
        }
    }

    String receiver(Expression e, boolean modifying) {
        if (modifying) {
            String inPlace = RustJdk.getOrDefaultMut(e, this);
            if (inPlace != null) return inPlace;
        }
        Printed p = modifying ? mutable(e) : expression(e);
        if (!p.nullable) return p.text;
        context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
        return p.text + (modifying ? ".as_mut().unwrap()" : ".as_ref().unwrap()");
    }

    /**
     * A closure kept as a {@code Box<dyn Fn…>}, which outlives the call: it takes what it captures ({@code move}), the
     * locals Java requires to be effectively final, and the {@code &self} it reads through.
     */
    private static String boxed(String closure) {
        return "Box::new(" + (closure.startsWith("|") ? "move " + closure : closure) + ")";
    }

    /** A two-parameter lambda over a map's entries: Rust's iterator gives one {@code (k, v)} tuple. */
    String tupleLambda(Expression e) {
        String text = expression(e).text;
        if (e instanceof Lambda l && l.methodInfo().parameters().size() == 2) {
            // a map's entries are references: a copied key or value is taken out by the pattern
            List<String> ps = new ArrayList<>();
            for (ParameterInfo p : l.methodInfo().parameters()) {
                ps.add((RustTypes.isCopy(p.parameterizedType()) ? "&" : "") + RustNames.snake(p.name()));
            }
            return text.replaceFirst("^\\|([^|]*)\\|", Matcher.quoteReplacement(
                    "|(" + String.join(", ", ps) + ")|"));
        }
        if (e instanceof MethodReference mr && mr.concreteParameterTypes().size() == 2) {
            List<String> ps = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                ps.add((RustTypes.isCopy(mr.concreteParameterTypes().get(i)) ? "&" : "") + "x" + (i + 1));
            }
            return text.replaceFirst("^\\|([^|]*)\\|", Matcher.quoteReplacement(
                    "|(" + String.join(", ", ps) + ")|"));
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
     * {@code e} as a value of its own: owned, and a field or a borrow cloned, where Java shares the object and Rust
     * cannot move it out ({@code opt.orElse(names)}).
     */
    String copiedText(Expression e) {
        return copied(expression(e), e);
    }

    /** {@code e} as an owned {@code Option}: a nullable value copied as it is, another in {@code Some}. */
    String optionText(Expression e) {
        Printed p = expression(e);
        if (!p.nullable) return "Some(" + owned(p, e.parameterizedType()) + ")";
        if (e instanceof NullConstant) return "None";
        return switch (p.held) {
            case PLACE -> p.text + ".clone()";
            case SHARED, EXCLUSIVE -> p.text + ".cloned()";
            default -> p.text;
        };
    }

    /** Optional.orElseGet's supplier, whose result is copied like {@link #copiedText}'s. */
    String copiedSupplier(Expression e) {
        copiedResult = true;
        try {
            return expression(e).text;
        } finally {
            copiedResult = false;
        }
    }

    /* p, printed from e, as a value of its own; a nullable one unwrapped: it stands where a value is expected */
    private String copied(Printed p, Expression e) {
        boolean copy = RustTypes.isCopy(e.parameterizedType()) || e instanceof NullConstant;
        boolean held = !copy && (p.held == Held.PLACE || p.held == Held.SHARED);
        if (p.nullable && !(e instanceof NullConstant)) {
            if (!held || copy) return p.text + ".unwrap()";
            return p.text + (p.held == Held.SHARED ? ".cloned()" : ".clone()") + ".unwrap()";
        }
        String text = owned(p, e.parameterizedType());
        return held && text.equals(p.text) ? text + ".clone()" : text;
    }

    /**
     * {@code e} stored where a {@code target} is held: owned, and boxed when the target is {@code Object}
     * ({@code Box<dyn Any>}) and the value is not.
     */
    String ownedAs(Expression e, ParameterizedType target) {
        if (target != null && isClosure(e) && RustTypes.isFunctional(target)) {
            return boxed(closure(e, target).text);
        }
        Printed p = expression(e);
        String text = owned(p, e.parameterizedType());
        if (p.nullable && !(e instanceof NullConstant)) {
            // a collection's values are not Options: Java's null in a map is a panic here
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
            text = text + ".unwrap()";
        }
        return asObject(text, e, target);
    }

    /*
     an owned value where an Object is held: boxed, as a Box<dyn Any>, unless it is one already; owned as its own type
     first ('"x".to_string()', a String in the box)
     */
    /*
     an owned object of a class where its interface is held: boxed, as the Box<dyn I> the declaration prints ('Selector
     s = new Any()'); not an Rc, which shares the object
     */
    private static String asDyn(String text, Expression e, ParameterizedType target) {
        if (!RustTypes.isDyn(target) || e instanceof NullConstant || isClosure(e) || text.startsWith("Rc::")) return text;
        TypeInfo from = e.parameterizedType().typeInfo();
        return from == null || from == target.typeInfo() || from.isInterface() || e.parameterizedType().arrays() > 0
                ? text : "Box::new(" + text + ")";
    }

    /*
     a nullable value where an Object is held: the Option's content boxed ('Option<String>' into an
     'Option<Box<dyn Any>>'), owned as its own type first
     */
    private String ownedNullable(Printed p, Expression e, ParameterizedType target) {
        if (!objectTarget(target) || e instanceof NullConstant || "None".equals(p.text)
            || e.parameterizedType().typeInfo() == null || e.parameterizedType().typeInfo().isJavaLangObject()) {
            return owned(p, target);
        }
        return owned(p, e.parameterizedType()) + ".map(|x| Box::new(x) as Box<dyn Any>)";
    }

    private static boolean objectTarget(ParameterizedType target) {
        return target != null && target.typeInfo() != null && target.typeInfo().isJavaLangObject() && target.arrays() == 0;
    }

    private static String asObject(String text, Expression e, ParameterizedType target) {
        boolean toObject = objectTarget(target);
        boolean fromObject = e.parameterizedType().typeInfo() != null && e.parameterizedType().typeInfo().isJavaLangObject();
        return toObject && !fromObject && !(e instanceof NullConstant) ? "Box::new(" + text + ")" : text;
    }

    /** What a field of {@code type} is initialized with: owned, and in {@code Some} when the field is nullable. */
    String fieldValue(Expression e, ParameterizedType type, boolean nullable) {
        if (isClosure(e) && RustTypes.isFunctional(type)) {
            String boxed = boxed(closure(e, type).text);
            return nullable ? "Some(" + boxed + ")" : boxed;
        }
        Printed p = expression(e);
        String text = RustTypes.isCopy(type) ? p.text : owned(p, type);
        if (!nullable && p.nullable && !(e instanceof NullConstant)) {
            // a nullable value where the verdicts hold the field non-null (checked before): Java's NPE, a panic here
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
            return text + ".unwrap()";
        }
        return nullable && !p.nullable && !(e instanceof NullConstant) ? "Some(" + text + ")" : text;
    }

    String index(Expression e) {
        boolean outer = mutatingPlace(false); // an index is read, also in a place written through
        try {
            return index0(e);
        } finally {
            mutatingPlace = outer;
        }
    }

    private String index0(Expression e) {
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
        return mutated(lv, context.method.methodBody());
    }

    /**
     * Whether the lambda changes what it is handed: one of its parameters is mutated in its body, by the same rules as
     * a local. A lambda's parameters carry no verdicts of their own in every run (RustJdk.changesElement).
     */
    boolean changesArgument(Lambda lambda) {
        Element body = lambda.methodBody();
        return body != null && lambda.methodInfo().parameters().stream()
                .anyMatch(p -> !RustTypes.isCopy(p.parameterizedType()) && mutated(p, body));
    }

    // the locals whose mutation is being decided: a local initialized from another that is initialized from it
    // (through a pattern variable, a reassigned local) would recurse without end
    private final Set<Variable> deciding = new HashSet<>();

    private boolean mutated(Variable lv, Element scope) {
        if (!deciding.add(lv)) return false;
        try {
            return mutatedOnce(lv, scope);
        } finally {
            deciding.remove(lv);
        }
    }

    private boolean mutatedOnce(Variable lv, Element scope) {
        boolean[] found = {false};
        scope.visit(e -> {
            if (found[0]) return false;
            if (e instanceof MethodCall mc) {
                if (rootedIn(mc.object(), lv) && modifies(mc)) found[0] = true;
                List<Expression> args = mc.parameterExpressions();
                MethodInfo callee = mc.methodInfo();
                for (int i = 0; i < args.size() && i < callee.parameters().size(); i++) {
                    if (isLocal(args.get(i), lv) && !callee.typeInfo().compilationUnit().externalLibrary()
                        && context.passing(callee.parameters().get(i)) == Passing.EXCLUSIVE) {
                        found[0] = true;
                    }
                }
                // Collections.sort(list), Arrays.fill(a, v): the JDK static modifies its first argument
                if (!args.isEmpty() && rootedIn(args.getFirst(), lv) && RustJdk.modifiesFirstArgument(mc)) found[0] = true;
            } else if (e instanceof MethodReference mr && !(mr.scope() instanceof TypeExpression)
                       && rootedIn(mr.scope(), lv) && modifies(mr.methodInfo())) {
                found[0] = true; // 'in.forEach(out::add)'
            } else if (e instanceof LocalVariableCreation lvc) {
                // 'Set<T> set2 = r.s; set2.add(t)': a &mut of r's field, which needs r mutable
                lvc.localVariableStream().filter(other -> other != lv && other.assignmentExpression() != null
                                                          && !isLocal(other.assignmentExpression(), lv)
                                                          && rootedIn(other.assignmentExpression(), lv)
                                                          && mutated(other, scope))
                        .findFirst().ifPresent(other -> found[0] = true);
            } else if (e instanceof ForEachStatement fe && fe.initializer().localVariable() != lv
                       && rootedIn(fe.expression(), lv) && fe.initializer().localVariable() != null
                       && mutated(fe.initializer().localVariable(), scope)) {
                found[0] = true; // 'for (Item i : l) i.touch()': the elements of l, modified through the loop
            } else if (e instanceof InstanceOf io && io.patternVariable() != null
                       && io.patternVariable().localVariable() != null && rootedIn(io.expression(), lv)
                       && mutated(io.patternVariable().localVariable(), scope)) {
                found[0] = true; // 'if (m instanceof If i) i.deepen()': the pattern variable is m's object
            } else if (e instanceof Assignment a && a.target().variable() instanceof FieldReference fr
                       && rootedIn(fr.scope(), lv)) {
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
        return modifies(mc.methodInfo());
    }

    private boolean modifies(MethodInfo callee) {
        if (callee.typeInfo().compilationUnit().externalLibrary()) return RustJdk.modifies(callee);
        return context.facts.receiver(callee) == Receiver.EXCLUSIVE;
    }

    private static boolean isLocal(Expression e, Variable lv) {
        return e instanceof VariableExpression ve && lv.equals(ve.variable());
    }

    /* the local, or a field of the object it holds ('a.list1.add(x)' modifies what 'a' owns) */
    private static boolean rootedIn(Expression e, Variable lv) {
        e = CstQueries.unwrap(e);
        if (isLocal(e, lv)) return true;
        // a cast of it, or what a JDK accessor hands out of it ('vlist.get(n - 1).put(..)', 'e.getValue().clear()'):
        // part of the object it holds
        if (e instanceof Cast c) return rootedIn(c.expression(), lv);
        // a getter handing out one of its object's fields ('h.getPaths().pop()'), whose exclusive twin the call uses
        if (e instanceof MethodCall g && g.object() != null && RustGetters.field(g.methodInfo()) != null) {
            return rootedIn(g.object(), lv);
        }
        if (e instanceof MethodCall mc && mc.object() != null && !mc.methodInfo().isStatic()
            && mc.methodInfo().typeInfo().compilationUnit().externalLibrary() && !RustJdk.modifies(mc)
            && !RustTypes.isCopy(mc.methodInfo().returnType()) && !RustTypes.isString(mc.methodInfo().returnType())) {
            return rootedIn(mc.object(), lv);
        }
        // or an element of the array it holds ('slots[0].v = 3')
        if (e instanceof VariableExpression ve && ve.variable() instanceof DependentVariable dv) {
            return rootedIn(dv.arrayExpression(), lv);
        }
        return e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr && fr.scope() != null
               && rootedIn(fr.scope(), lv);
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
