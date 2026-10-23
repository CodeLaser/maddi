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
import java.util.List;
import java.util.Map;
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
            case ExpressionAsStatement es -> pad + expressionStatement(es.expression()) + ";\n";
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
            case SynchronizedStatement ss -> {
                context.message(RustPrintCode.SYNCHRONIZED, ss, null);
                yield pad + "{\n" + statements(ss.block(), depth + 1) + pad + "}\n";
            }
            default -> pad + unsupported(statement, statement.getClass().getSimpleName()) + ";\n";
        };
    }

    private String expressionStatement(Expression e) {
        if (e instanceof Assignment a) return assignment(a, true);
        return expression(e).text;
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
        if (held == Held.PLACE && !RustTypes.isCopy(lv.parameterizedType())) {
            // an element or a field: the local refers to it, as Java's reference does
            text = (mutated ? "&mut " : "&") + text;
            held = mutated ? Held.EXCLUSIVE : Held.SHARED;
        } else if (held == Held.PLACE) {
            held = Held.VALUE;
        }
        if (nullable && !value.nullable && !(init instanceof NullConstant)) text = "Some(" + text + ")";
        locals.put(lv, held);
        boolean mut = reassigned || mutated && held == Held.VALUE;
        String type = init instanceof NullConstant
                ? ": " + RustTypes.option(RustTypes.owned(lv.parameterizedType(), context, lvc), true) : "";
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
        if (collection.text.endsWith(".keys()") || collection.text.endsWith(".values()")) {
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
            default -> value(unsupported(e, e.getClass().getSimpleName()));
        };
    }

    private static Printed value(String text) {
        return new Printed(text, Held.VALUE, false);
    }

    private Printed variable(Variable variable, Element where) {
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
            return new Printed(prefix + "::" + RustNames.constant(f.name()), Held.VALUE, nullable);
        }
        String scope = fr.scopeIsThis() || fr.isDefaultScope() ? context.receiver : dereference(expression(fr.scope()));
        return new Printed(scope + "." + RustNames.snake(f.name()), Held.PLACE, nullable);
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
            if (!statement) return unsupported(a, "increment inside an expression");
            return target + op;
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
            return value(expression(other).text + ("==".equals(op) ? ".is_none()" : ".is_some()"));
        }
        if (">>>".equals(op)) {
            context.message(RustPrintCode.UNSIGNED_SHIFT, bo, null);
            String type = RustTypes.primitive(bo.parameterizedType().typeInfo());
            return value("(((" + operand(bo.lhs()) + ") as u" + type.substring(1) + ") >> " + operand(bo.rhs())
                         + ") as " + type);
        }
        ParameterizedType common = promoted(bo, op);
        return value(promotedOperand(bo.lhs(), common) + " " + op + " " + promotedOperand(bo.rhs(), common));
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
    private String operand(Expression e) {
        Printed p = expression(e);
        boolean binary = e instanceof BinaryOperator || e instanceof InlineConditional || e instanceof Assignment;
        String text = binary ? "(" + p.text + ")" : p.text;
        if (p.nullable) {
            context.message(RustPrintCode.UNWRAP_AT_DEREFERENCE, e, p.text);
            return text + ".unwrap()";
        }
        return text;
    }

    private Printed inlineConditional(InlineConditional ic) {
        boolean string = RustTypes.isString(ic.parameterizedType());
        String t = string ? owned(expression(ic.ifTrue()), ic.parameterizedType()) : expression(ic.ifTrue()).text;
        String f = string ? owned(expression(ic.ifFalse()), ic.parameterizedType()) : expression(ic.ifFalse()).text;
        return value("if " + condition(ic.condition()) + " { " + t + " } else { " + f + " }");
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
                format.append("{}");
                args.add(expression(part).text);
            }
        }
        return value("format!(" + stringLiteral(format.toString())
                     + (args.isEmpty() ? "" : ", " + String.join(", ", args)) + ")");
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
        try {
            context.method = m;
            if (single != null) return value("|" + params + "| " + expression(single).text);
            return value("|" + params + "| {\n" + statements(m.methodBody(), 1) + "}");
        } finally {
            context.method = outer;
        }
    }

    // ---- calls

    private Printed methodCall(MethodCall mc) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary()) {
            Printed mapped = RustJdk.call(mc, this);
            if (mapped != null) return mapped;
            context.message(RustPrintCode.UNMAPPED_JDK_CALL, mc, callee.fullyQualifiedName());
            String object = mc.object() == null || mc.objectIsImplicit() ? context.receiver : expression(mc.object()).text;
            return value(object + "." + RustNames.snake(callee.name()) + "(" + arguments(mc.parameterExpressions(), null)
                         + ")");
        }
        String name = RustNames.method(callee);
        String args = arguments(mc.parameterExpressions(), callee);
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
            String object = mc.object() == null || mc.objectIsImplicit() || mc.object() instanceof VariableExpression ve
                                                                          && ve.variable() instanceof This
                    ? context.receiver : dereference(expression(mc.object()));
            call = object + "." + name + "(" + args + ")";
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
        return text;
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
        if (RustTypes.isString(type) && (p.held == Held.STR || p.held == Held.SHARED)) return p.text + ".to_string()";
        // a string is immutable in Java: sharing it and copying it cannot be told apart, so a named one is cloned
        // rather than moved, and its later uses stay valid
        if (RustTypes.isString(type) && (p.held == Held.PLACE || p.held == Held.VALUE && isName(p.text))) {
            return p.text + ".clone()";
        }
        // an @Immutable record (no hidden content) derives Clone, and a copy cannot be told from the shared object:
        // a borrowed one is copied where an owned one is expected. Any other type stays a mismatch for rustc to report.
        if (RustTypes.isRecord(type) && context.facts.immutable(type.typeInfo())
            && (p.held == Held.SHARED || p.held == Held.PLACE)) {
            return p.text + (p.nullable ? ".cloned()" : ".clone()");
        }
        return p.text;
    }

    private static boolean isName(String text) {
        return text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '.');
    }

    // ---- what RustJdk needs

    String text(Expression e) {
        return expression(e).text;
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

    String index(Expression e) {
        if (e instanceof IntConstant ic) return Integer.toString(ic.constant());
        return operand(e) + " as usize";
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
