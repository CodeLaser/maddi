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

import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prints an {@link Expression} as C#. Every expression form is printed here, recursively, so that a Java-only form
 * nested anywhere is translated too; only a form this class does not know falls back to the Java
 * {@link Expression#print}, with a {@link CSharpPrintMessage.Code#JAVA_FALLBACK} message.
 * <p>
 * C#'s operators and their precedence are Java's, so most expressions print as they do in Java. Where they differ:
 * <ul>
 *   <li>{@code x instanceof T} is {@code x is T}, and {@code !(x instanceof T)} is {@code x is not T};</li>
 *   <li>{@code ==} between two Strings is Java's identity comparison; C#'s {@code ==} compares their contents, so it
 *   is {@code object.ReferenceEquals(a, b)};</li>
 *   <li>a lambda is {@code (a, b) => …}; a method reference is a method group ({@code Owner.Method},
 *   {@code receiver.Method}) or, for an unbound receiver or a constructor, a lambda;</li>
 *   <li>{@code X.class} is {@code typeof(X)}; {@code super.m()} is {@code base.M()}; an array's {@code length} is
 *   {@code Length};</li>
 *   <li>a diamond {@code new ArrayList<>()} has its type arguments written out.</li>
 * </ul>
 */
public final class CSharpExpressionPrinter {

    private CSharpExpressionPrinter() {
    }

    /**
     * An expression whose value is not used: an expression statement, a for-loop update. A call of a JDK method may
     * then have its idiomatic form ({@code map[k] = v} for {@code map.put(k, v)}, see {@link CSharpBcl}).
     */
    public static OutputBuilder printStatement(Expression e, Qualification q) {
        if (e instanceof MethodCall mc) return methodCall(mc, q, true);
        return print(e, q);
    }

    public static OutputBuilder print(Expression e, Qualification q) {
        return switch (e) {
            case ConstructorCall cc -> constructorCall(cc, q);
            case Cast cast -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(text(CSharpTypeName.of(cast.parameterizedType(), q))).add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST)
                    .add(operand(cast.precedence(), cast.expression(), q));
            case InstanceOf io -> instanceOf(io, false, q);
            case InlineConditional ic -> new OutputBuilderImpl().add(booleanOperand(ic.precedence(), ic.condition(), q))
                    .add(SymbolEnum.QUESTION_MARK).add(operand(ic.precedence(), ic.ifTrue(), q))
                    .add(SymbolEnum.COLON).add(operand(ic.precedence(), ic.ifFalse(), q));
            case MethodCall mc -> methodCall(mc, q, false);
            case MethodReference mr -> methodReference(mr, q);
            case SwitchExpression se -> CSharpStatementPrinter.switchExpression(se, q);
            case Lambda lambda -> lambda(lambda, q);
            case Assignment a -> assignment(a, q);
            case Negation neg -> negation(neg, q);
            case BitwiseNegation bn -> new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix("~"))
                    .add(operand(bn.precedence(), bn.expression(), q));
            case BinaryOperator bo when bo.operator() != null -> binaryOperator(bo, q);
            case And and -> and.expressions().stream().map(x -> booleanOperand(and.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND));
            case Or or -> or.expressions().stream().map(x -> booleanOperand(or.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_OR));
            // !(x instanceof T) is x is not T
            case UnaryOperator uo when "!".equals(uo.operator().name())
                                       && unwrap(uo.expression()) instanceof InstanceOf io -> instanceOf(io, true, q);
            case UnaryOperator uo -> new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(uo.operator().name()))
                    .add(operand(uo.precedence(), uo.expression(), q));
            case EnclosedExpression ee -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ee.inner(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            case VariableExpression ve -> variable(ve.variable(), q);
            case ArrayLength al -> new OutputBuilderImpl().add(receiver(al.scope(), q)).add(SymbolEnum.DOT)
                    .add(text("Length"));
            case ArrayInitializer ai -> arrayInitializer(ai, ai.parameterizedType(), q);
            case ClassExpression ce -> text("typeof(" + typeOfArgument(ce.type(), q) + ")");
            case TypeExpression te -> text(CSharpTypeName.of(te.parameterizedType(), q));
            case StringConstant sc -> text(stringLiteral(sc.constant()));
            case CharConstant cc -> text(charLiteral(cc.constant()));
            case BooleanConstant bc -> text(Boolean.toString(bc.constant()));
            case NullConstant nc -> text("null");
            case IntConstant ic -> text(Integer.toString(ic.constant()));
            case LongConstant lc -> text(lc.constant() + "L");
            case ShortConstant sc -> text(Short.toString(sc.constant()));
            case ByteConstant bc -> text(Byte.toString(bc.constant()));
            case DoubleConstant dc -> text(doubleLiteral(dc.constant()));
            case FloatConstant fc -> text(floatLiteral(fc.constant()));
            case CommaExpression ce -> ce.expressions().stream().map(x -> print(x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
            default -> {
                CSharpContext.message(CSharpPrintMessage.Code.JAVA_FALLBACK, e, e.getClass().getSimpleName());
                yield e.print(q);
            }
        };
    }

    // ---------------------------------------------------------------- variables

    static OutputBuilder variable(Variable v, Qualification q) {
        return switch (v) {
            case This t -> text(thisOrBase(t));
            case FieldReference fr -> fieldReference(fr, q);
            case DependentVariable dv -> new OutputBuilderImpl().add(receiver(dv.arrayExpression(), q))
                    .add(SymbolEnum.LEFT_BRACKET).add(print(dv.indexExpression(), q)).add(SymbolEnum.RIGHT_BRACKET);
            default -> text(CSharpContext.local(v.simpleName()));
        };
    }

    private static String thisOrBase(This t) {
        if (t.writeSuper()) return "base";
        if (t.explicitlyWriteType() != null && t.explicitlyWriteType() != CSharpContext.currentType()) {
            CSharpContext.message(CSharpPrintMessage.Code.OUTER_THIS, null, t.explicitlyWriteType().simpleName() + ".this");
        }
        return "this";
    }

    private static OutputBuilder fieldReference(FieldReference fr, Qualification q) {
        FieldInfo field = fr.fieldInfo();
        if (!CSharpNames.translated(field.owner())) {
            String[] mapped = CSharpBcl.field(field);
            if (mapped != null) {
                if (mapped[1] != null) CSharpContext.using(mapped[1]);
                return text(mapped[0]);
            }
            CSharpContext.message(CSharpPrintMessage.Code.UNMAPPED_JDK, fr.scope() != null ? fr.scope() : null,
                    field.owner().fullyQualifiedName() + "." + field.name());
        }
        String name = CSharpNames.field(field);
        if (fr.isDefaultScope() || fr.scope() == null) {
            // C# does not bring an interface's constants into the scope of the classes implementing it
            if (field.isStatic() && field.owner().isInterface() && !insideOf(field.owner())) {
                return text(CSharpTypeName.name(field.owner(), q) + "." + name);
            }
            return text(name);
        }
        if (fr.isStatic() && fr.scope() instanceof TypeExpression) {
            return text(CSharpTypeName.name(field.owner(), q) + "." + name);
        }
        return new OutputBuilderImpl().add(receiver(fr.scope(), q)).add(SymbolEnum.DOT).add(text(name));
    }

    /** The type being printed is {@code type} or one of its nested types. */
    private static boolean insideOf(TypeInfo type) {
        for (TypeInfo t = CSharpContext.currentType(); t != null; ) {
            if (t.equals(type)) return true;
            var cuOrEnclosing = t.compilationUnitOrEnclosingType();
            t = cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
        }
        return false;
    }

    // ---------------------------------------------------------------- calls

    /** The JDK → BCL rule of a call; null for a call of a translated method, or a JDK method without one. */
    private static CSharpBcl.Rule rule(MethodCall mc) {
        MethodInfo method = mc.methodInfo();
        TypeInfo owner = method.typeInfo();
        if (CSharpNames.translated(owner)) {
            // the implicit values() and valueOf(String) of an enum C# declares as an enum
            if (owner.typeNature().isEnum() && method.isStatic() && CSharpTypePrinter.simpleEnum(owner)) {
                String self = CSharpTypeName.name(owner, null);
                if ("values".equals(method.name()) && method.parameters().isEmpty()) {
                    return new CSharpBcl.Rule("Enum.GetValues<" + self + ">()", null, List.of("System"));
                }
                if ("valueOf".equals(method.name()) && method.parameters().size() == 1) {
                    return new CSharpBcl.Rule("Enum.Parse<" + self + ">($1)", null, List.of("System"));
                }
            }
            return null;
        }
        return CSharpBcl.call(method, mc);
    }

    /** A call whose C# form is not an atom ({@code list.Count == 0}): in parentheses as an operand or a receiver. */
    private static boolean nonAtomicCall(Expression e) {
        return e instanceof MethodCall mc && rule(mc) instanceof CSharpBcl.Rule r && !CSharpTemplate.atomic(r.template(false));
    }

    private static OutputBuilder methodCall(MethodCall mc, Qualification q, boolean statement) {
        CSharpBcl.Rule rule = rule(mc);
        if (rule != null) {
            rule.namespaces().forEach(CSharpContext::using);
            Expression object = mc.object();
            java.util.function.Supplier<OutputBuilder> receiver = () -> object == null ? text("this")
                    : object instanceof VariableExpression ve && ve.variable() instanceof This t ? text(thisOrBase(t))
                    : object instanceof TypeExpression te ? text(CSharpTypeName.name(te.parameterizedType().typeInfo(), q))
                    : receiver(object, q);
            List<Expression> parameterExpressions = mc.parameterExpressions();
            List<java.util.function.Supplier<OutputBuilder>> args = IntStream.range(0, parameterExpressions.size())
                    .mapToObj(i -> (java.util.function.Supplier<OutputBuilder>) () -> converted(parameterExpressions.get(i),
                            parameterType(mc.methodInfo(), i), q)).toList();
            List<java.util.function.Supplier<OutputBuilder>> operands = mc.parameterExpressions().stream()
                    .map(a -> (java.util.function.Supplier<OutputBuilder>) () -> receiver(a, q)).toList();
            return new CSharpTemplate(receiver, args, operands, mc, mc.concreteReturnType(),
                    object == null ? null : object.parameterizedType(), q).render(rule.template(statement));
        }
        OutputBuilder b = new OutputBuilderImpl();
        Expression object = mc.object();
        MethodInfo method = mc.methodInfo();
        if (object instanceof VariableExpression ve && ve.variable() instanceof This t) {
            if (t.writeSuper() || !mc.objectIsImplicit()) b.add(text(thisOrBase(t))).add(SymbolEnum.DOT);
        } else if (object instanceof TypeExpression te) {
            if (!mc.objectIsImplicit()) {
                b.add(text(CSharpTypeName.name(te.parameterizedType().typeInfo(), q))).add(SymbolEnum.DOT);
            }
        } else if (object != null && !mc.objectIsImplicit()) {
            b.add(receiver(object, q)).add(SymbolEnum.DOT);
        }
        if (CSharpTypePrinter.isRecordAccessor(method)) {
            // p.x() is the record's property p.X
            FieldInfo component = method.typeInfo().fields().stream()
                    .filter(f -> !f.isStatic() && f.name().equals(method.name())).findFirst().orElseThrow();
            return b.add(text(CSharpNames.field(component)));
        }
        String typeArguments = mc.typeArguments().isEmpty() ? "" : mc.typeArguments().stream()
                .map(t -> CSharpTypeName.argument(t, q)).collect(Collectors.joining(", ", "<", ">"));
        unmapped(method, mc);
        b.add(text(CSharpNames.method(method) + typeArguments));
        return b.add(arguments(mc.parameterExpressions(), method, q));
    }

    static OutputBuilder arguments(List<Expression> args, Qualification q) {
        return arguments(args, null, q);
    }

    /** The arguments of a call of {@code method}, converted to its parameters' types. */
    static OutputBuilder arguments(List<Expression> args, MethodInfo method, Qualification q) {
        if (args.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        if (causeOnly(method)) {
            // Java's new RuntimeException(cause) takes the cause's description as its message; C#'s exceptions
            // have no constructor of the inner exception alone
            Expression cause = args.getFirst();
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(receiver(cause, q))
                    .add(CSharpTemplate.NULL_CONDITIONAL).add(text("ToString")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                    .add(SymbolEnum.COMMA).add(print(cause, q)).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return IntStream.range(0, args.size()).mapToObj(i -> converted(args.get(i), parameterType(method, i), q)).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA,
                SymbolEnum.LEFT_PARENTHESIS, SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }

    private static OutputBuilder constructorCall(ConstructorCall cc, Qualification q) {
        ParameterizedType type = cc.parameterizedType();
        if (cc.anonymousClass() != null) {
            CSharpContext.message(CSharpPrintMessage.Code.ANONYMOUS_CLASS, cc, CSharpContext.describe(cc));
            ParameterizedType parent = cc.anonymousClass().parentClass();
            ParameterizedType named = parent != null && !parent.isJavaLangObject() ? parent
                    : cc.anonymousClass().interfacesImplemented().stream().findFirst().orElse(type);
            return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(text(CSharpTypeName.of(named, q))).add(arguments(cc.parameterExpressions(), q));
        }
        if (type.arrays() > 0) {
            if (cc.arrayInitializer() != null) {
                return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                        .add(text(CSharpTypeName.of(type, q))).add(SpaceEnum.ONE).add(arrayInitializer(cc.arrayInitializer(), type, q));
            }
            List<Expression> dimensions = cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).toList();
            if (dimensions.size() > 1) {
                CSharpContext.message(CSharpPrintMessage.Code.MULTI_DIMENSIONAL_ARRAY, cc, CSharpContext.describe(cc));
            }
            // new int[n][] is C#'s jagged array creation, as Java's
            OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(text(CSharpTypeName.of(type.copyWithoutArrays(), q)));
            for (int i = 0; i < type.arrays(); i++) {
                if (i < dimensions.size()) {
                    b.add(SymbolEnum.LEFT_BRACKET).add(print(dimensions.get(i), q)).add(SymbolEnum.RIGHT_BRACKET);
                } else {
                    b.add(SymbolEnum.OPEN_CLOSE_BRACKETS);
                }
            }
            return b;
        }
        if (cc.object() != null) {
            CSharpContext.message(CSharpPrintMessage.Code.OUTER_THIS, cc, CSharpContext.describe(cc));
        }
        return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE).add(text(CSharpTypeName.of(type, q)))
                .add(arguments(cc.parameterExpressions(), cc.constructor(), q));
    }

    /**
     * The value of a declaration: an array initializer gets the declared type, which its nested initializers need
     * (see {@link #arrayInitializer}).
     */
    static OutputBuilder initializer(Expression value, ParameterizedType declared, Qualification q) {
        if (value instanceof ArrayInitializer ai && declared != null && declared.arrays() > 0) {
            return arrayInitializer(ai, declared, q);
        }
        return converted(value, declared, q);
    }

    /**
     * A value where a {@code target} is expected, with the conversions C# does not make implicitly: Java's
     * {@code null} of a type parameter is C#'s {@code default}, and Java unboxes an {@code Integer} where an
     * {@code int} is expected, where C# needs a cast of its {@code int?}.
     */
    static OutputBuilder converted(Expression value, ParameterizedType target, Qualification q) {
        if (target == null) return print(value, q);
        Expression inner = unwrap(value);
        if (inner instanceof NullConstant && target.isTypeParameter() && target.arrays() == 0) return text("default");
        if (target.isPrimitiveExcludingVoid() && unboxed(inner)) return unboxCast(CSharpTypeName.of(target, q), value, q);
        return print(value, q);
    }

    /** {@code (int) x}; Java's cast to a boxed type, {@code (Integer) o}, becomes the cast to the primitive. */
    private static OutputBuilder unboxCast(String primitive, Expression value, Qualification q) {
        Expression inner = unwrap(value);
        Expression operand = inner instanceof Cast cast && cast.parameterizedType().isBoxedExcludingVoid()
                ? cast.expression() : value;
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(primitive))
                .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(receiver(operand, q));
    }

    /** A JDK exception's constructor of its cause. */
    private static boolean causeOnly(MethodInfo method) {
        return method != null && method.isConstructor() && !CSharpNames.translated(method.typeInfo())
               && method.parameters().size() == 1
               && method.parameters().getFirst().parameterizedType().typeInfo() != null
               && "java.lang.Throwable".equals(method.parameters().getFirst().parameterizedType().typeInfo()
                .fullyQualifiedName());
    }

    /** The type of the {@code i}-th parameter of {@code method}; null for a variable arity one. */
    static ParameterizedType parameterType(MethodInfo method, int i) {
        if (method == null || i >= method.parameters().size()) return null;
        ParameterInfo p = method.parameters().get(i);
        return p.isVarArgs() ? null : p.parameterizedType();
    }

    /** Whether {@link #converted} changes the value. */
    static boolean converts(Expression value, ParameterizedType target) {
        if (target == null) return false;
        Expression inner = unwrap(value);
        return inner instanceof NullConstant && target.isTypeParameter() && target.arrays() == 0
               || target.isPrimitiveExcludingVoid() && unboxed(inner);
    }

    /** The value of a {@code return} in the current method or lambda. */
    static OutputBuilder returned(Expression value, Qualification q) {
        MethodInfo method = CSharpContext.currentMethod();
        return converted(value, method == null ? null : method.returnType(), q);
    }

    /** A value of a boxed type, C#'s nullable value type: a call or a variable, not a constant or an operation. */
    private static boolean unboxed(Expression e) {
        ParameterizedType type = e.parameterizedType();
        return type != null && type.isBoxedExcludingVoid() && type.arrays() == 0
               && (e instanceof MethodCall || e instanceof VariableExpression || e instanceof Cast);
    }

    /** A condition: a {@code Boolean} is unboxed. */
    static OutputBuilder condition(Expression e, Qualification q) {
        Expression inner = unwrap(e);
        return unboxed(inner) && inner.parameterizedType().isBooleanOrBoxedBoolean() ? unboxCast("bool", e, q)
                : print(e, q);
    }

    /** An operand of a boolean operator, {@code !}, {@code &&} or {@code ||}. */
    private static OutputBuilder booleanOperand(Precedence precedence, Expression e, Qualification q) {
        Expression inner = unwrap(e);
        return unboxed(inner) && inner.parameterizedType().isBooleanOrBoxedBoolean() ? unboxCast("bool", e, q)
                : operand(precedence, e, q);
    }

    /**
     * {@code { a, b }}. C# allows nested braces only in a rectangular array's initializer: an element of a jagged
     * array that is itself an initializer is an array creation, {@code { new int[] { 1 }, null }}.
     */
    private static OutputBuilder arrayInitializer(ArrayInitializer ai, ParameterizedType arrayType, Qualification q) {
        if (ai.expressions().isEmpty()) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        }
        ParameterizedType element = arrayType.arrays() > 0 ? arrayType.copyWithOneFewerArrays() : arrayType;
        return ai.expressions().stream().map(x -> x instanceof ArrayInitializer nested
                ? new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE).add(text(CSharpTypeName.of(element, q)))
                        .add(SpaceEnum.ONE).add(arrayInitializer(nested, element, q))
                : print(x, q)).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA,
                SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.defaultGuideGenerator()));
    }

    /** {@code typeof(List<>)} for Java's {@code List.class}: the unbound generic type. */
    private static String typeOfArgument(ParameterizedType type, Qualification q) {
        if (type.arrays() == 0 && type.typeInfo() != null && !type.typeInfo().typeParameters().isEmpty()) {
            return CSharpTypeName.name(type.typeInfo(), q) + "<" + ",".repeat(type.typeInfo().typeParameters().size() - 1) + ">";
        }
        return CSharpTypeName.of(type, q);
    }

    /** A JDK method the mapping does not translate: counted, printed with its Java name. */
    private static void unmapped(MethodInfo method, Expression use) {
        if (!CSharpNames.translated(method.typeInfo()) && !"ToString".equals(CSharpNames.method(method))
            && !"Equals".equals(CSharpNames.method(method)) && !"GetHashCode".equals(CSharpNames.method(method))) {
            CSharpContext.message(CSharpPrintMessage.Code.UNMAPPED_JDK, use,
                    method.typeInfo().fullyQualifiedName() + "." + method.name() + "/" + method.parameters().size());
        }
    }

    // ---------------------------------------------------------------- lambdas and method references

    private static OutputBuilder lambda(Lambda lambda, Qualification q) {
        List<ParameterInfo> params = lambda.parameters();
        // typed as Java wrote it: (int a, int b) -> …; a C# lambda types all its parameters or none
        boolean typed = !lambda.outputVariants().isEmpty()
                        && lambda.outputVariants().stream().allMatch(Lambda.OutputVariant::isTyped);
        OutputBuilder b = new OutputBuilderImpl();
        CSharpContext.pushMethod(lambda.methodInfo());
        CSharpContext.enterScope(java.util.Set.of()); // the parameters'
        try {
            if (params.size() == 1 && !typed) {
                b.add(text(CSharpContext.declare(params.getFirst().name())));
            } else {
                b.add(text(params.stream()
                        .map(p -> (typed ? CSharpTypeName.of(p.parameterizedType(), q) + " " : "")
                                  + CSharpContext.declare(p.name()))
                        .collect(Collectors.joining(", ", "(", ")"))));
            }
            b.add(SymbolEnum.binaryOperator("=>"));
            List<Statement> statements = lambda.methodBody().statements().stream().filter(s -> !s.isSynthetic()).toList();
            if (statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs && !rs.hasNoValue()) {
                return b.add(returned(rs.expression(), q));
            }
            if (statements.size() == 1 && statements.getFirst() instanceof ExpressionAsStatement eas) {
                // a lambda of a void method: its expression's value is not used
                return b.add(lambda.methodInfo().isVoid() ? printStatement(eas.expression(), q) : print(eas.expression(), q));
            }
            return b.add(CSharpStatementPrinter.block(lambda.methodBody(), q));
        } finally {
            CSharpContext.exitScope();
            CSharpContext.popMethod();
        }
    }

    /**
     * A method group where C# has one: {@code Owner.Method} for a static method, {@code receiver.Method} for a bound
     * receiver. A lambda otherwise: {@code (a, b) => a.Method(b)} for an unbound receiver, {@code (a) => new T(a)}
     * for a constructor.
     */
    private static OutputBuilder methodReference(MethodReference mr, Qualification q) {
        MethodInfo method = mr.methodInfo();
        Expression scope = mr.scope();
        int arity = mr.concreteParameterTypes().size();
        List<String> parameters = IntStream.range(0, arity).mapToObj(i -> "p" + i).toList();
        String parameterList = arity == 1 ? parameters.getFirst() : String.join(", ", parameters);
        if (arity != 1) parameterList = "(" + parameterList + ")";
        if (method.isConstructor()) {
            ParameterizedType type = scope.parameterizedType();
            String created = type.arrays() > 0
                    ? "new " + CSharpTypeName.of(type.copyWithoutArrays(), q) + "[" + parameters.getFirst() + "]"
                      + "[]".repeat(type.arrays() - 1)
                    : "new " + CSharpTypeName.of(type, q) + "(" + String.join(", ", parameters) + ")";
            return text(parameterList + " => " + created);
        }
        CSharpBcl.Rule rule = CSharpNames.translated(method.typeInfo()) ? null : CSharpBcl.call(method, null);
        if (rule != null) {
            // a lambda around the member's translation: String::length is p0 => p0.Length
            rule.namespaces().forEach(CSharpContext::using);
            boolean unbound = scope instanceof TypeExpression && !method.isStatic();
            java.util.function.Supplier<OutputBuilder> receiver = unbound ? () -> text(parameters.getFirst())
                    : scope instanceof TypeExpression te ? () -> text(CSharpTypeName.name(te.parameterizedType().typeInfo(), q))
                    : () -> receiver(scope, q);
            List<java.util.function.Supplier<OutputBuilder>> args = parameters.subList(unbound ? 1 : 0, parameters.size())
                    .stream().map(p -> (java.util.function.Supplier<OutputBuilder>) () -> text(p)).toList();
            return new OutputBuilderImpl().add(text(parameterList)).add(SymbolEnum.binaryOperator("=>"))
                    .add(new CSharpTemplate(receiver, args, args, null, mr.concreteReturnType(),
                            scope.parameterizedType(), q).render(rule.template(false)));
        }
        unmapped(method, mr);
        String name = CSharpNames.method(method);
        if (scope instanceof TypeExpression te) {
            String owner = CSharpTypeName.name(te.parameterizedType().typeInfo(), q);
            if (method.isStatic()) return text(owner + "." + name);
            // String::length: the first parameter is the receiver
            return text(parameterList + " => " + parameters.getFirst() + "." + name + "("
                        + String.join(", ", parameters.subList(1, parameters.size())) + ")");
        }
        return new OutputBuilderImpl().add(receiver(scope, q)).add(SymbolEnum.DOT).add(text(name));
    }

    // ---------------------------------------------------------------- operators

    private static OutputBuilder assignment(Assignment a, Qualification q) {
        OutputBuilder target = variable(a.variableTarget(), q);
        if (a.prefixPrimitiveOperator() != null) {
            String operator = a.assignmentOperatorIsPlus() ? "++" : "--";
            return a.prefixPrimitiveOperator()
                    ? new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(operator)).add(target)
                    : target.add(SymbolEnum.plusPlusSuffix(operator));
        }
        String operator = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        OutputBuilder value = "=".equals(operator)
                && converts(a.value(), a.variableTarget().parameterizedType())
                ? converted(a.value(), a.variableTarget().parameterizedType(), q) : operand(a.precedence(), a.value(), q);
        return new OutputBuilderImpl().add(target).add(SymbolEnum.assignment(operator)).add(value);
    }

    private static OutputBuilder binaryOperator(BinaryOperator bo, Qualification q) {
        String op = bo.operator().name();
        if (("==".equals(op) || "!=".equals(op)) && stringIdentity(bo.lhs(), bo.rhs())) {
            // Java's == on two Strings compares references; C#'s compares contents
            OutputBuilder call = new OutputBuilderImpl().add(text("object.ReferenceEquals"))
                    .add(arguments(List.of(bo.lhs(), bo.rhs()), q));
            return "==".equals(op) ? call
                    : new OutputBuilderImpl().add(SymbolEnum.UNARY_BOOLEAN_NOT).add(call);
        }
        return new OutputBuilderImpl().add(operand(bo.precedence(), bo.lhs(), q))
                .add(SymbolEnum.binaryOperator(op))
                .add(operand(bo.precedence(), bo.rhs(), q));
    }

    private static boolean stringIdentity(Expression lhs, Expression rhs) {
        if (unwrap(lhs) instanceof NullConstant || unwrap(rhs) instanceof NullConstant) return false;
        ParameterizedType l = lhs.parameterizedType();
        ParameterizedType r = rhs.parameterizedType();
        return l != null && r != null && l.arrays() == 0 && r.arrays() == 0 && l.isJavaLangString() && r.isJavaLangString();
    }

    private static OutputBuilder negation(Negation neg, Qualification q) {
        Expression inner = unwrap(neg.expression());
        if (inner instanceof InstanceOf io) return instanceOf(io, true, q); // !(x instanceof T) -> x is not T
        if (inner instanceof Equals equals && !stringIdentity(equals.lhs(), equals.rhs())) {
            return new OutputBuilderImpl().add(operand(equals.precedence(), equals.lhs(), q))
                    .add(SymbolEnum.NOT_EQUALS).add(operand(equals.precedence(), equals.rhs(), q));
        }
        if (neg.expression().isNumeric()) {
            return new OutputBuilderImpl().add(SymbolEnum.UNARY_MINUS).add(operand(neg.precedence(), neg.expression(), q));
        }
        return new OutputBuilderImpl().add(SymbolEnum.UNARY_BOOLEAN_NOT)
                .add(booleanOperand(neg.precedence(), neg.expression(), q));
    }

    private static OutputBuilder instanceOf(InstanceOf io, boolean not, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl().add(operand(io.precedence(), io.expression(), q))
                .add(SpaceEnum.ONE).add(CSharpKeyword.IS).add(SpaceEnum.ONE);
        if (not) b.add(CSharpKeyword.NOT).add(SpaceEnum.ONE);
        RecordPattern pattern = io.patternVariable();
        if (pattern != null && pattern.localVariable() != null) {
            return b.add(text(CSharpTypeName.pattern(pattern.localVariable().parameterizedType(), q)))
                    .add(SpaceEnum.ONE).add(text(CSharpContext.declare(pattern.localVariable().simpleName())));
        }
        if (pattern != null) {
            CSharpContext.message(CSharpPrintMessage.Code.SWITCH_FORM, io, CSharpContext.describe(io));
        }
        return b.add(text(CSharpTypeName.pattern(io.testType(), q)));
    }

    // ---------------------------------------------------------------- literals

    static String stringLiteral(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) sb.append(escape(c, '"'));
        return sb.append('"').toString();
    }

    static String charLiteral(char c) {
        return "'" + escape(c, '\'') + "'";
    }

    private static String escape(char c, char quote) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            case '\0' -> "\\0";
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
        if (Double.isNaN(d)) return "double.NaN";
        if (Double.isInfinite(d)) return d > 0 ? "double.PositiveInfinity" : "double.NegativeInfinity";
        String s = Double.toString(d);
        return s.contains(".") || s.contains("E") ? s : s + ".0";
    }

    private static String floatLiteral(float f) {
        if (Float.isNaN(f)) return "float.NaN";
        if (Float.isInfinite(f)) return f > 0 ? "float.PositiveInfinity" : "float.NegativeInfinity";
        return Float.toString(f) + "f";
    }

    // ---------------------------------------------------------------- helpers

    private static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }

    static Expression unwrap(Expression e) {
        Expression x = e;
        while (x instanceof EnclosedExpression ee) x = ee.inner();
        return x;
    }

    /** An operand, in parentheses when its precedence is lower than its operator's. */
    static OutputBuilder operand(Precedence precedence, Expression e, Qualification q) {
        OutputBuilder inner = print(e, q);
        if (precedence.greaterThan(e.precedence()) || e instanceof Lambda || nonAtomicCall(e)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return inner;
    }

    /** The receiver of {@code .member}: anything but an atom is enclosed. */
    static OutputBuilder receiver(Expression e, Qualification q) {
        OutputBuilder inner = print(e, q);
        return switch (e) {
            case VariableExpression ve -> inner;
            case MethodCall mc when !nonAtomicCall(mc) -> inner;
            case EnclosedExpression ee -> inner;
            case ArrayLength al -> inner;
            case ClassExpression ce -> inner;
            case TypeExpression te -> inner;
            case StringConstant sc -> inner;
            case ConstructorCall cc when cc.parameterizedType().arrays() == 0 && cc.anonymousClass() == null -> inner;
            default -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner)
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        };
    }
}
