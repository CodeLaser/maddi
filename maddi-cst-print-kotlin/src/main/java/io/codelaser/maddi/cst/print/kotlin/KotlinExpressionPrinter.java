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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Prints an {@link Expression} as Kotlin. Every expression form is printed here, recursively, so that a Java-only
 * form nested anywhere (a {@code new} inside an argument, a cast inside a field initializer) is translated too;
 * only a form this class does not know falls back to the Java {@link Expression#print}.
 * <p>
 * Two contexts. {@link #print} is a VALUE: Kotlin has no assignment expressions, so an assignment used as a value
 * prints as {@code value.also { target = it }}, which evaluates once and yields the assigned value, as Java's does.
 * {@link #printStatement} is a STATEMENT, where an assignment prints as itself.
 * <p>
 * Where Kotlin's operators differ from Java's:
 * <ul>
 *   <li>bitwise and shift operators are infix functions ({@code and}, {@code or}, {@code xor}, {@code shl},
 *   {@code shr}, {@code ushr}, {@code inv()}), which bind tighter than comparison, so their non-trivial operands
 *   are parenthesised;</li>
 *   <li>{@code ==} on two references is Java's identity comparison, Kotlin's {@code ===};</li>
 *   <li>a cast between primitive types is a conversion ({@code .toInt()}), never {@code as}, which would throw
 *   (#105).</li>
 * </ul>
 */
public class KotlinExpressionPrinter {

    private static final Map<String, String> INFIX = Map.of("&", "and", "|", "or", "^", "xor",
            "<<", "shl", ">>", "shr", ">>>", "ushr");

    /** An expression whose value is used. */
    public static OutputBuilder print(Expression e, Qualification q) {
        return switch (e) {
            case ConstructorCall cc -> constructorCall(cc, q);
            case Cast cast -> cast(cast, q);
            case InstanceOf io -> new OutputBuilderImpl().add(operand(io.precedence(), io.expression(), q))
                    .add(KotlinSymbols.binary("is"))
                    .add(new TextImpl(KotlinTypeName.of(io.testType(), q)));
            case InlineConditional ic when isElvis(ic) ->
                // desugared elvis `a ?: b` = InlineConditional(a==null, ifTrue=b, ifFalse=a); recover the `?:`
                    new OutputBuilderImpl().add(operand(ic.precedence(), ic.ifFalse(), q))
                            .add(SymbolEnum.binaryOperator("?:")).add(operand(ic.precedence(), ic.ifTrue(), q));
            case InlineConditional ic -> new OutputBuilderImpl()
                    .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ic.condition(), q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                    .add(print(ic.ifTrue(), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.ELSE).add(SpaceEnum.ONE).add(print(ic.ifFalse(), q));
            case MethodCall mc -> methodCall(mc, q);
            case MethodReference mr -> methodReference(mr, q);
            case SwitchExpression se -> KotlinStatementPrinter.whenExpression(se.selector(), se.entries(), q);
            case Lambda lambda -> lambda(lambda, q);
            case Assignment a -> assignmentAsValue(a, q);
            case BitwiseNegation bn -> new OutputBuilderImpl().add(receiver(bn.expression(), q)).add(SymbolEnum.DOT)
                    .add(new TextImpl("inv")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
            case Negation neg -> negation(neg, q);
            case StringConcat sc -> stringConcat(sc, q);
            case BinaryOperator bo when bo.operator() != null -> binaryOperator(bo, q);
            case And and -> and.expressions().stream().map(x -> operand(and.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND));
            case Or or -> or.expressions().stream().map(x -> operand(or.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_OR));
            case UnaryOperator uo -> unaryOperator(uo, q);
            case EnclosedExpression ee -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ee.inner(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            case VariableExpression ve -> variable(ve.variable(), q);
            case ArrayLength al -> new OutputBuilderImpl().add(receiver(al.scope(), q)).add(SymbolEnum.DOT)
                    .add(new TextImpl("size"));
            case ArrayInitializer ai -> arrayInitializer(ai, ai.parameterizedType(), q);
            case ClassExpression ce -> new OutputBuilderImpl().add(new TextImpl(classLiteral(ce.type(), q)));
            case TypeExpression te -> new OutputBuilderImpl()
                    .add(new TextImpl(KotlinTypeName.name(te.parameterizedType().typeInfo(), q)));
            case StringConstant sc -> new OutputBuilderImpl().add(new TextImpl(stringLiteral(sc.constant())));
            case CharConstant cc -> new OutputBuilderImpl().add(new TextImpl(charLiteral(cc.constant())));
            case IntConstant ic when ic.constant() == Integer.MIN_VALUE -> text("Int.MIN_VALUE");
            case LongConstant lc when lc.constant() == Long.MIN_VALUE -> text("Long.MIN_VALUE");
            case DoubleConstant dc -> text(doubleLiteral(dc.constant()));
            case FloatConstant fc -> text(floatLiteral(fc.constant()));
            case ByteConstant bc -> text(Byte.toString(bc.constant()));
            case ShortConstant sc -> text(Short.toString(sc.constant()));
            case CommaExpression ce -> ce.expressions().stream().map(x -> printStatement(x, q))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
            default -> e.print(q); // constants (int, long, boolean, null) and anything unknown: as in Java
        };
    }

    /** An expression whose value is discarded: an expression statement, a for-loop update, a lambda body. */
    public static OutputBuilder printStatement(Expression e, Qualification q) {
        if (e instanceof Assignment a) return assignmentAsStatement(a, q);
        if (e instanceof EnclosedExpression ee) return printStatement(ee.inner(), q);
        return print(e, q);
    }

    private static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }

    // ---------------------------------------------------------------- variables

    static OutputBuilder variable(Variable v, Qualification q) {
        return switch (v) {
            case This t -> new OutputBuilderImpl().add(new TextImpl(thisOrSuper(t)));
            case FieldReference fr -> fieldReference(fr, q);
            case DependentVariable dv -> new OutputBuilderImpl().add(receiver(dv.arrayExpression(), q))
                    .add(SymbolEnum.LEFT_BRACKET).add(print(dv.indexExpression(), q)).add(SymbolEnum.RIGHT_BRACKET);
            default -> new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(v.simpleName())));
        };
    }

    /** {@code this}, {@code this@Outer}, {@code super}, {@code super<Iface>}. */
    private static String thisOrSuper(This t) {
        TypeInfo explicit = t.explicitlyWriteType();
        if (t.writeSuper()) return explicit == null ? "super" : "super<" + explicit.simpleName() + ">";
        return explicit == null ? "this" : "this@" + explicit.simpleName();
    }

    private static OutputBuilder fieldReference(FieldReference fr, Qualification q) {
        String name = KotlinNames.name(fr.fieldInfo().name());
        Expression scope = fr.scope();
        if (fr.isStatic()) {
            if (fr.isDefaultScope()) return text(name);
            TypeInfo owner = fr.fieldInfo().owner();
            return text(KotlinTypeName.name(owner, q) + "." + name);
        }
        if (fr.isDefaultScope() || scope == null) return text(name);
        return new OutputBuilderImpl().add(receiver(scope, q)).add(SymbolEnum.DOT).add(new TextImpl(name));
    }

    // ---------------------------------------------------------------- calls

    /**
     * ⛔ #103: a call on {@code super} keeps its {@code super.}. The object is the same {@code This} pseudo-variable
     * as an implicit {@code this}, distinguished only by {@link This#writeSuper()}; dropping it like an implicit
     * {@code this} turned {@code return super.addAll(c)} in an override into {@code return addAll(c)}: a call to
     * itself.
     */
    private static OutputBuilder methodCall(MethodCall mc, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        Expression object = mc.object();
        if (object instanceof VariableExpression ve && ve.variable() instanceof This t) {
            if (t.writeSuper() || (t.explicitlyWriteType() != null && !mc.objectIsImplicit())) {
                b.add(new TextImpl(thisOrSuper(t))).add(SymbolEnum.DOT);
            }
        } else if (object instanceof TypeExpression te) {
            if (!mc.objectIsImplicit()) {
                b.add(new TextImpl(KotlinTypeName.name(te.parameterizedType().typeInfo(), q))).add(SymbolEnum.DOT);
            }
        } else if (object != null && !mc.objectIsImplicit()) {
            b.add(receiver(object, q)).add(SymbolEnum.DOT);
        }
        b.add(new TextImpl(KotlinNames.name(mc.methodInfo().name())));
        if (!mc.typeArguments().isEmpty()) {
            b.add(new TextImpl(mc.typeArguments().stream().map(t -> KotlinTypeName.of(t, q))
                    .collect(java.util.stream.Collectors.joining(", ", "<", ">"))));
        }
        return b.add(arguments(mc.parameterExpressions(), q));
    }

    private static OutputBuilder arguments(List<Expression> args, Qualification q) {
        if (args.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        return args.stream().map(a -> print(a, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }

    private static OutputBuilder methodReference(MethodReference mr, Qualification q) {
        Expression scope = mr.scope();
        if (mr.methodInfo().isConstructor()) {
            ParameterizedType type = scope.parameterizedType();
            String name = type.arrays() > 0 ? KotlinTypeName.of(type, q) : KotlinTypeName.name(type.typeInfo(), q);
            return text("::" + name);
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (scope instanceof TypeExpression te) {
            b.add(new TextImpl(KotlinTypeName.name(te.parameterizedType().typeInfo(), q)));
        } else {
            b.add(receiver(scope, q));
        }
        return b.add(new TextImpl("::" + KotlinNames.name(mr.methodInfo().name())));
    }

    /**
     * {@code Foo(a)} for {@code new Foo(a)}; {@code outer.Inner()} for {@code outer.new Inner()}; an anonymous class
     * becomes an object expression; arrays become their Kotlin factory calls.
     */
    private static OutputBuilder constructorCall(ConstructorCall cc, Qualification q) {
        ParameterizedType type = cc.parameterizedType();
        if (cc.anonymousClass() != null) return anonymousClass(cc, q);
        if (type.arrays() > 0) {
            if (cc.arrayInitializer() != null) return arrayInitializer(cc.arrayInitializer(), type, q);
            List<Expression> dimensions = cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).toList();
            return arrayCreation(type, dimensions, 0, q);
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (cc.object() != null) b.add(receiver(cc.object(), q)).add(SymbolEnum.DOT);
        String name = KotlinTypeName.of(type, q);
        if (name.endsWith("?")) name = name.substring(0, name.length() - 1); // a constructor call is never null
        return b.add(new TextImpl(name))
                .add(arguments(cc.parameterExpressions(), q));
    }

    private static OutputBuilder anonymousClass(ConstructorCall cc, Qualification q) {
        TypeInfo anonymous = cc.anonymousClass();
        OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.OBJECT).add(SpaceEnum.ONE)
                .add(SymbolEnum.COLON).add(SpaceEnum.ONE);
        ParameterizedType parent = anonymous.parentClass();
        if (parent != null && !parent.isJavaLangObject()) {
            b.add(new TextImpl(KotlinTypeName.of(parent, q))).add(arguments(cc.parameterExpressions(), q));
        } else if (!anonymous.interfacesImplemented().isEmpty()) {
            b.add(new TextImpl(KotlinTypeName.of(anonymous.interfacesImplemented().getFirst(), q)));
        } else {
            b.add(new TextImpl("Any")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        CompilationUnitPrinterImpl.ImportDataImpl importData = new CompilationUnitPrinterImpl.ImportDataImpl(List.of(), q, q);
        OutputBuilder body = new KotlinTypePrinter(anonymous, true).print(importData, false);
        if (body.isEmpty()) return b.add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        return b.add(body);
    }

    /**
     * {@code IntArray(n)}, {@code arrayOfNulls<String>(n)}, {@code Array(n) { IntArray(m) }} -- one level per
     * dimension given, the rest left null as in Java.
     */
    private static OutputBuilder arrayCreation(ParameterizedType type, List<Expression> dimensions, int level,
                                               Qualification q) {
        ParameterizedType element = type.copyWithArrays(type.arrays() - 1);
        OutputBuilder size = print(dimensions.get(level), q);
        String primitive = KotlinTypeName.primitiveArray(element);
        if (primitive != null) {
            return new OutputBuilderImpl().add(new TextImpl(primitive)).add(SymbolEnum.LEFT_PARENTHESIS).add(size)
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (element.arrays() > 0 && dimensions.size() > level + 1) {
            return new OutputBuilderImpl().add(new TextImpl("Array")).add(SymbolEnum.LEFT_PARENTHESIS).add(size)
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE)
                    .add(SpaceEnum.ONE).add(arrayCreation(element, dimensions, level + 1, q))
                    .add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
        }
        return new OutputBuilderImpl().add(new TextImpl("arrayOfNulls<" + KotlinTypeName.of(element, q) + ">"))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(size).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /** {@code intArrayOf(1, 2)}, {@code arrayOf<String>("a")}; nested initializers recurse with the element type. */
    private static OutputBuilder arrayInitializer(ArrayInitializer ai, ParameterizedType type, Qualification q) {
        ParameterizedType element = type != null && type.arrays() > 0 ? type.copyWithArrays(type.arrays() - 1) : null;
        String primitive = element == null ? null : KotlinTypeName.primitiveArray(element);
        String factory;
        if (primitive != null) {
            factory = Character.toLowerCase(primitive.charAt(0)) + primitive.substring(1, primitive.length() - 5)
                      + "ArrayOf";
        } else {
            factory = element == null ? "arrayOf" : "arrayOf<" + KotlinTypeName.of(element, q) + ">";
        }
        OutputBuilder args = ai.expressions().isEmpty()
                ? new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                : ai.expressions().stream()
                .map(x -> x instanceof ArrayInitializer nested ? arrayInitializer(nested, element, q) : print(x, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
        return new OutputBuilderImpl().add(new TextImpl(factory)).add(args);
    }

    private static String classLiteral(ParameterizedType type, Qualification q) {
        if (type.arrays() == 0 && type.typeInfo() != null && type.isPrimitiveExcludingVoid()) {
            return KotlinTypeName.of(type, q) + "::class.javaPrimitiveType!!";
        }
        if (type.isVoid()) return "Void.TYPE";
        if (type.arrays() > 0) return KotlinTypeName.of(type, q) + "::class.java";
        if (type.typeInfo() == null) return "Any::class.java";
        return KotlinTypeName.name(type.typeInfo(), q) + "::class.java";
    }

    // ---------------------------------------------------------------- lambdas

    /**
     * {@code { a, b -> body }}. A block body ending in {@code return x} ends in {@code x}; any other {@code return}
     * inside it would return from the enclosing FUNCTION in Kotlin, so the lambda gets the label {@code lambda@}
     * and those returns print as {@code return@lambda} (KotlinStatementPrinter, through {@link KotlinContext}).
     */
    private static OutputBuilder lambda(Lambda lambda, Qualification q) {
        List<ParameterInfo> params = lambda.parameters();
        Block body = lambda.methodBody();
        List<Statement> statements = body.statements().stream().filter(s -> !s.isSynthetic()).toList();

        OutputBuilder inner;
        boolean labelled = false;
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.LAMBDA, null, KotlinContext.LAMBDA_LABEL));
        try {
            if (statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs && !rs.hasNoValue()) {
                inner = printStatement(rs.expression(), q);
            } else {
                labelled = hasInnerReturn(statements);
                inner = KotlinStatementPrinter.lambdaBody(statements, q);
            }
        } finally {
            KotlinContext.pop();
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (labelled) b.add(new TextImpl(KotlinContext.LAMBDA_LABEL + "@"));
        b.add(SymbolEnum.LEFT_BRACE);
        if (!params.isEmpty()) {
            b.add(SpaceEnum.ONE).add(params.stream()
                    .map(p -> new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(p.name()))))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA))).add(SpaceEnum.ONE).add(SymbolEnum.LAMBDA);
        }
        return b.add(SpaceEnum.ONE).add(inner).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    /** A {@code return} other than a final one, outside nested lambdas: it needs {@code return@lambda}. */
    private static boolean hasInnerReturn(List<Statement> statements) {
        boolean[] found = {false};
        for (int i = 0; i < statements.size(); i++) {
            Statement s = statements.get(i);
            if (i == statements.size() - 1 && s instanceof ReturnStatement) continue;
            s.visit((Element e) -> {
                if (e instanceof Lambda) return false;
                if (e instanceof ReturnStatement) found[0] = true;
                return !found[0];
            });
        }
        return found[0];
    }

    // ---------------------------------------------------------------- assignments

    private static OutputBuilder assignmentAsStatement(Assignment a, Qualification q) {
        OutputBuilder target = variable(a.variableTarget(), q);
        if (a.prefixPrimitiveOperator() != null) {
            String op = a.assignmentOperatorIsPlus() ? "++" : "--";
            return a.prefixPrimitiveOperator()
                    ? new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(op)).add(target)
                    : new OutputBuilderImpl().add(target).add(SymbolEnum.plusPlusSuffix(op));
        }
        String op = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        String binary = op.length() > 1 ? op.substring(0, op.length() - 1) : null;
        if (binary != null && INFIX.containsKey(binary)) {
            // x &= y -> x = x and (y): Kotlin has no compound form of its infix operators
            return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment("="))
                    .add(infix(variableAsExpression(a), INFIX.get(binary), a.value(), q));
        }
        return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment(op))
                .add(print(a.value(), q));
    }

    /** {@code value.also { target = it }}: the assignment happens, once, and the expression is its value. */
    private static OutputBuilder assignmentAsValue(Assignment a, Qualification q) {
        if (a.prefixPrimitiveOperator() != null) return assignmentAsStatement(a, q); // ++i and i++ are expressions
        OutputBuilder value;
        String op = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        if ("=".equals(op)) {
            value = receiver(a.value(), q);
        } else {
            String binary = op.substring(0, op.length() - 1);
            OutputBuilder computed = INFIX.containsKey(binary)
                    ? infix(variableAsExpression(a), INFIX.get(binary), a.value(), q)
                    : new OutputBuilderImpl().add(variable(a.variableTarget(), q))
                    .add(KotlinSymbols.binary(binary)).add(operand(PrecedenceEnum.MULTIPLICATIVE, a.value(), q));
            value = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(computed)
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return value.add(SymbolEnum.DOT).add(new TextImpl("also")).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE)
                .add(SpaceEnum.ONE).add(variable(a.variableTarget(), q)).add(KotlinSymbols.assignment("="))
                .add(new TextImpl("it")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    private static Expression variableAsExpression(Assignment a) {
        return a.target();
    }

    // ---------------------------------------------------------------- operators

    private static OutputBuilder binaryOperator(BinaryOperator bo, Qualification q) {
        String op = bo.operator().name();
        String infix = INFIX.get(op);
        if (infix != null) return infix(bo.lhs(), infix, bo.rhs(), q);
        if (("==".equals(op) || "!=".equals(op)) && isReferenceComparison(bo.lhs(), bo.rhs())) {
            op = op + "="; // Java's identity comparison of references is Kotlin's === (Kotlin's == calls equals)
        }
        return new OutputBuilderImpl()
                .add(operand(bo.precedence(), bo.lhs(), q))
                .add(KotlinSymbols.binary(op))
                .add(operand(bo.precedence(), bo.rhs(), q));
    }

    /** Both sides references (not primitives), and neither the null literal: identity, not equality. */
    private static boolean isReferenceComparison(Expression lhs, Expression rhs) {
        if (lhs instanceof NullConstant || rhs instanceof NullConstant) return false;
        ParameterizedType l = lhs.parameterizedType();
        ParameterizedType r = rhs.parameterizedType();
        if (l == null || r == null) return false;
        boolean lRef = l.arrays() > 0 || !l.isPrimitiveExcludingVoid();
        boolean rRef = r.arrays() > 0 || !r.isPrimitiveExcludingVoid();
        return lRef && rRef;
    }

    /** {@code a and b}: Kotlin's infix functions bind tighter than comparisons, so anything compound is enclosed. */
    private static OutputBuilder infix(Expression lhs, String function, Expression rhs, Qualification q) {
        return new OutputBuilderImpl().add(atomic(lhs, q)).add(KotlinSymbols.binary(function))
               .add(atomic(rhs, q));
    }

    /** Java's {@code 1 + "a"}: Kotlin's {@code +} takes its type from the left, so a non-String left side converts. */
    private static OutputBuilder stringConcat(StringConcat sc, Qualification q) {
        ParameterizedType lhsType = sc.lhs().parameterizedType();
        OutputBuilder lhs = lhsType != null && !lhsType.isJavaLangString()
                ? new OutputBuilderImpl().add(receiver(sc.lhs(), q)).add(SymbolEnum.DOT).add(new TextImpl("toString"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                : operand(sc.precedence(), sc.lhs(), q);
        return new OutputBuilderImpl().add(lhs).add(KotlinSymbols.binary("+"))
                .add(operand(sc.precedence(), sc.rhs(), q));
    }

    private static OutputBuilder negation(Negation neg, Qualification q) {
        Expression inner = unwrap(neg.expression());
        if (inner instanceof Equals equals) { // !(a == b) -> a != b
            String op = isReferenceComparison(equals.lhs(), equals.rhs()) ? "!==" : "!=";
            return new OutputBuilderImpl().add(operand(equals.precedence(), equals.lhs(), q))
                    .add(KotlinSymbols.binary(op)).add(operand(equals.precedence(), equals.rhs(), q));
        }
        if (inner instanceof InstanceOf io) return notInstanceOf(io, q); // !(x is T) -> x !is T
        return new OutputBuilderImpl()
                .add(neg.expression().isNumeric() ? SymbolEnum.UNARY_MINUS : SymbolEnum.UNARY_BOOLEAN_NOT)
                .add(operand(neg.precedence(), neg.expression(), q));
    }

    private static OutputBuilder unaryOperator(UnaryOperator uo, Qualification q) {
        Expression inner = unwrap(uo.expression());
        if (inner instanceof InstanceOf io) return notInstanceOf(io, q); // a unary op wrapping `is` is `!` -> `!is`
        if ("~".equals(uo.operator().name())) {
            return receiver(uo.expression(), q).add(SymbolEnum.DOT).add(new TextImpl("inv()"));
        }
        return new OutputBuilderImpl()
                .add(SymbolEnum.plusPlusPrefix(uo.operator().name())).add(operand(uo.precedence(), uo.expression(), q));
    }

    private static OutputBuilder notInstanceOf(InstanceOf io, Qualification q) {
        return new OutputBuilderImpl().add(operand(io.precedence(), io.expression(), q))
                .add(KotlinSymbols.binary("!is")).add(new TextImpl(KotlinTypeName.of(io.testType(), q)));
    }

    // ---------------------------------------------------------------- casts (#105)

    private enum Primitive {BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE}

    private static Primitive primitive(ParameterizedType pt) {
        if (pt == null || pt.arrays() > 0 || pt.typeInfo() == null) return null;
        return switch (pt.typeInfo().fullyQualifiedName()) {
            case "boolean", "java.lang.Boolean" -> Primitive.BOOLEAN;
            case "char", "java.lang.Character" -> Primitive.CHAR;
            case "byte", "java.lang.Byte" -> Primitive.BYTE;
            case "short", "java.lang.Short" -> Primitive.SHORT;
            case "int", "java.lang.Integer" -> Primitive.INT;
            case "long", "java.lang.Long" -> Primitive.LONG;
            case "float", "java.lang.Float" -> Primitive.FLOAT;
            case "double", "java.lang.Double" -> Primitive.DOUBLE;
            default -> null;
        };
    }

    /**
     * ⛔ #105: a cast between primitive types converts, so it prints as a conversion function. {@code x as Int} on a
     * {@code Long} throws ClassCastException where Java's {@code (int) x} truncates. Narrowing to byte, short or
     * char from a floating type goes through {@code toInt()}, as Java's does (and Kotlin deprecates the direct
     * functions); from char, through {@code code}.
     */
    private static OutputBuilder cast(Cast cast, Qualification q) {
        Primitive to = primitive(cast.parameterizedType());
        Primitive from = primitive(cast.expression().parameterizedType());
        if (to != null && from != null && cast.parameterizedType().isPrimitiveExcludingVoid()) {
            String conversion = conversion(from, to);
            OutputBuilder receiver = receiver(cast.expression(), q);
            for (String call : conversion.split("\\.")) {
                if (!call.isEmpty()) receiver.add(SymbolEnum.DOT).add(new TextImpl(call));
            }
            return receiver;
        }
        return new OutputBuilderImpl().add(operand(cast.precedence(), cast.expression(), q)).add(SpaceEnum.ONE)
                .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinTypeName.of(cast.parameterizedType(), q)));
    }

    private static String conversion(Primitive from, Primitive to) {
        if (from == to || to == Primitive.BOOLEAN || from == Primitive.BOOLEAN) return "";
        if (from == Primitive.CHAR) {
            return to == Primitive.INT ? ".code" : ".code" + toFunction(to);
        }
        boolean floating = from == Primitive.FLOAT || from == Primitive.DOUBLE;
        if (to == Primitive.CHAR) {
            return from == Primitive.INT ? ".toChar()" : ".toInt().toChar()";
        }
        if (floating && (to == Primitive.BYTE || to == Primitive.SHORT)) return ".toInt()" + toFunction(to);
        return toFunction(to);
    }

    private static String toFunction(Primitive to) {
        return switch (to) {
            case BYTE -> ".toByte()";
            case SHORT -> ".toShort()";
            case INT -> ".toInt()";
            case LONG -> ".toLong()";
            case FLOAT -> ".toFloat()";
            case DOUBLE -> ".toDouble()";
            case CHAR -> ".toChar()";
            case BOOLEAN -> "";
        };
    }

    // ---------------------------------------------------------------- literals

    static String stringLiteral(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) sb.append(escape(c, '"'));
        return sb.append('"').toString();
    }

    private static String charLiteral(char c) {
        return "'" + escape(c, '\'') + "'";
    }

    /** Kotlin's escapes; {@code $} too, which would start a string template. Other control characters as \\uXXXX. */
    private static String escape(char c, char quote) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '$' -> "\\$";
            default -> {
                if (c == quote) yield "\\" + c;
                if (c < 0x20 || (c >= 0x7f && c < 0xa0) || Character.isSurrogate(c)) {
                    yield String.format("\\u%04x", (int) c);
                }
                yield String.valueOf(c);
            }
        };
    }

    private static String doubleLiteral(double d) {
        if (Double.isNaN(d)) return "Double.NaN";
        if (d == Double.POSITIVE_INFINITY) return "Double.POSITIVE_INFINITY";
        if (d == Double.NEGATIVE_INFINITY) return "Double.NEGATIVE_INFINITY";
        return Double.toString(d);
    }

    private static String floatLiteral(float f) {
        if (Float.isNaN(f)) return "Float.NaN";
        if (f == Float.POSITIVE_INFINITY) return "Float.POSITIVE_INFINITY";
        if (f == Float.NEGATIVE_INFINITY) return "Float.NEGATIVE_INFINITY";
        return Float.toString(f) + "f";
    }

    // ---------------------------------------------------------------- precedence

    /** True when this desugared inline conditional was a Kotlin elvis, marked in {@link DetailedSources}. */
    private static boolean isElvis(InlineConditional ic) {
        Source source = ic.source();
        return source != null && source.detailedSources() != null
               && source.detailedSources().detail(DetailedSources.NULL_COALESCING) != null;
    }

    private static Expression unwrap(Expression e) {
        while (e instanceof EnclosedExpression ee) e = ee.inner();
        return e;
    }

    /** Recurse into an operand, parenthesising when the enclosing operator binds tighter (as the Java printer does). */
    static OutputBuilder operand(io.codelaser.maddi.cst.api.expression.Precedence precedence, Expression e,
                                 Qualification q) {
        OutputBuilder inner = print(e, q);
        if (precedence.greaterThan(e.precedence()) || needsParenthesesInKotlin(e)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return inner;
    }

    /** A form whose Kotlin precedence is lower than its Java one: an infix function, an assignment's {@code also}. */
    private static boolean needsParenthesesInKotlin(Expression e) {
        return e instanceof BinaryOperator bo && bo.operator() != null && INFIX.containsKey(bo.operator().name())
               || e instanceof Lambda || e instanceof InlineConditional;
    }

    /** The receiver of {@code .member}: anything but an atom is enclosed. */
    static OutputBuilder receiver(Expression e, Qualification q) {
        return atomic(e, q);
    }

    private static OutputBuilder atomic(Expression e, Qualification q) {
        OutputBuilder inner = print(e, q);
        if (isAtom(e)) return inner;
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    private static boolean isAtom(Expression e) {
        return switch (e) {
            case VariableExpression ve -> true;
            case MethodCall mc -> true;
            case EnclosedExpression ee -> true;
            case ArrayLength al -> true;
            case ClassExpression ce -> true;
            case TypeExpression te -> true;
            case StringConstant sc -> true;
            case CharConstant cc -> true;
            case BooleanConstant bc -> true;
            case NullConstant nc -> true;
            case IntConstant ic -> ic.constant() >= 0;
            case LongConstant lc -> lc.constant() >= 0;
            case ConstructorCall cc -> cc.anonymousClass() == null;
            default -> e.precedence() == PrecedenceEnum.TOP || e.precedence() == PrecedenceEnum.ACCESS;
        };
    }
}
