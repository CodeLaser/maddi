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

package io.codelaser.maddi.cst.print.csharp;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.And;
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.ArrayLength;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.BitwiseNegation;
import io.codelaser.maddi.cst.api.expression.BooleanConstant;
import io.codelaser.maddi.cst.api.expression.ByteConstant;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.CharConstant;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.CommaExpression;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.DoubleConstant;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.Equals;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.FloatConstant;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.LongConstant;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.Or;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.ShortConstant;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.UnaryOperator;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
class CSharpExpressionPrinter1 {
    private static final Set<String> VALUE_TYPE_NAMES = Set.of("int", "long", "short", "sbyte", "byte", "char", "bool",
            "double", "float", "decimal");

    record TypeLiteral(ParameterizedType raw, List<ParameterizedType> arguments) {
    }

    /**
 * An expression whose value is not used: an expression statement, a for-loop update. A call of a JDK method may
 * then have its idiomatic form ({@code map[k] = v} for {@code map.put(k, v)}, see {@link CSharpBcl}).
 */
    static OutputBuilder printStatement(Expression e, Qualification q) {
        if (e instanceof MethodCall mc) {
            OutputBuilder call = methodCall(mc, q, true);
            // s.length(); is s.Length, which is no statement in C# (CS0201): a discard keeps the evaluation, and its
            // NullPointerException
            return mc.methodInfo().isVoid() || validStatement(call.toString()) ? call
                    : new OutputBuilderImpl().add(text("_")).add(SymbolEnum.assignment("=")).add(call);
        }
        return print(e, q);
    }

    /** A call, an assignment, an increment: what C# takes as an expression statement. */
    private static boolean validStatement(String printed) {
        String t = printed.strip();
        if (t.endsWith(")")) {
            if (!t.startsWith("(")) return true;
            // (a ? b : c).Add(x) is a call; (int) x.Y() a cast, (x) a parenthesized value, neither
            int close = CSharpExpressionPrinter2.matching(t);
            if (close > 0 && close + 1 < t.length() && ".[(?".indexOf(t.charAt(close + 1)) >= 0) return true;
        }
        int depth = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (depth == 0 && c == '=' && i > 0 && i + 1 < t.length() && t.charAt(i + 1) != '='
                     && "=!<>".indexOf(t.charAt(i - 1)) < 0) {
                return true;
            }
        }
        return t.endsWith("++") || t.endsWith("--") || t.startsWith("++") || t.startsWith("--")
               || t.startsWith("new ") || t.startsWith("await ");
    }

    static OutputBuilder print(Expression e, Qualification q) {
        return switch (e) {
            case ConstructorCall cc -> CSharpExpressionPrinter.constructorCall(cc, q);
            case Cast cast -> CSharpExpressionPrinter.cast(cast, q);
            case InstanceOf io -> instanceOf(io, false, q);
            case InlineConditional ic -> new OutputBuilderImpl().add(booleanOperand(ic.precedence(), ic.condition(), q))
                    .add(SymbolEnum.QUESTION_MARK).add(CSharpExpressionPrinter2.conditionalBranch(ic, ic.ifTrue(), q))
                    .add(SymbolEnum.COLON).add(CSharpExpressionPrinter2.conditionalBranch(ic, ic.ifFalse(), q));
            case MethodCall mc -> methodCall(mc, q, false);
            case MethodReference mr -> CSharpExpressionPrinter2.adapted(mr.parameterizedType(), CSharpExpressionPrinter.methodReference(mr, q), q);
            case SwitchExpression se -> CSharpStatementPrinter.switchExpression(se, q);
            case Lambda lambda -> CSharpExpressionPrinter2.adapted(lambda.concreteFunctionalType(), CSharpExpressionPrinter2.lambda(lambda, q), q);
            case Assignment a -> CSharpExpressionPrinter2.assignment(a, q);
            case Negation neg -> negation(neg, q);
            case BitwiseNegation bn -> new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix("~"))
                    .add(operand(bn.precedence(), bn.expression(), q));
            case BinaryOperator bo when bo.operator() != null -> CSharpExpressionPrinter2.binaryOperator(bo, q);
            case And and -> and.expressions().stream().map(x -> booleanOperand(and.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND));
            case Or or -> or.expressions().stream().map(x -> booleanOperand(or.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_OR));
            // !(x instanceof T) is x is not T
            case UnaryOperator uo when "!".equals(uo.operator().name())
                                       && unwrap(uo.expression()) instanceof InstanceOf io -> instanceOf(io, true, q);
            case UnaryOperator uo when "!".equals(uo.operator().name()) -> new OutputBuilderImpl()
                    .add(SymbolEnum.UNARY_BOOLEAN_NOT).add(booleanOperand(uo.precedence(), uo.expression(), q));
            case UnaryOperator uo -> new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(uo.operator().name()))
                    .add(operand(uo.precedence(), uo.expression(), q));
            case EnclosedExpression ee -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ee.inner(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            // a field access is where the facts about its scope are asked for, as at a call
            case VariableExpression ve when ve.variable() instanceof FieldReference fr && fr.scope() != null ->
                    inCall(ve, () -> CSharpExpressionPrinter2.variable(ve.variable(), q));
            case VariableExpression ve -> CSharpExpressionPrinter2.variable(ve.variable(), q);
            case ArrayLength al -> new OutputBuilderImpl().add(CSharpNullability.dereferenced(al.scope(), q)).add(SymbolEnum.DOT)
                    .add(text("Length"));
            case ArrayInitializer ai -> arrayInitializer(ai, ai.parameterizedType(), q);
            case ClassExpression ce -> text("typeof(" + CSharpExpressionPrinter2.typeOfArgument(ce.type(), q) + ")");
            case TypeExpression te -> text(CSharpTypeName.inExpression(te.parameterizedType(), q));
            case StringConstant sc -> text(stringLiteral(sc.constant()));
            case CharConstant cc -> text(charLiteral(cc.constant()));
            case BooleanConstant bc -> text(Boolean.toString(bc.constant()));
            case NullConstant nc -> text("null");
            case IntConstant ic -> text(Integer.toString(ic.constant()));
            case LongConstant lc -> text(lc.constant() + "L");
            case ShortConstant sc -> text(Short.toString(sc.constant()));
            case ByteConstant bc -> text(Byte.toString(bc.constant()));
            case DoubleConstant dc -> text(CSharpExpressionPrinter2.doubleLiteral(dc.constant()));
            case FloatConstant fc -> text(CSharpExpressionPrinter2.floatLiteral(fc.constant()));
            case CommaExpression ce -> ce.expressions().stream().map(x -> print(x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
            default -> {
                CSharpContext.message(CSharpPrintCode.JAVA_FALLBACK, e, e.getClass().getSimpleName());
                yield e.print(q);
            }
        };
    }

    static OutputBuilder plainCast(Cast cast, Qualification q) {
        // (Function<A, Object>) mapping of a Function<Object, Object>: no cast C# has, the value wrapped
        OutputBuilder delegateCast = delegateCast(cast.expression(), unwrap(cast.expression()), cast.parameterizedType(), q);
        if (delegateCast != null) return delegateCast;
        // a value that may be null keeps its null through a reference cast, in C# as in Java: (T?) x (CS8600)
        // (AiMessage) null, which picks an overload, is (AiMessage?) null
        boolean nullConstant = unwrap(cast.expression()) instanceof NullConstant
                               && CSharpContext.program().facts().nullabilityAnalysed();
        boolean nullable = nullConstant || CSharpNullability.mayBeNull(cast.expression());
        String own = CSharpWildcardInterfaces.ownUse(cast.parameterizedType(), q);
        ParameterizedType castType = Objects.requireNonNullElse(wildcardCastArguments(cast), cast.parameterizedType());
        String type = own != null ? (nullable ? own + "?" : own)
                : nullable ? CSharpTypeName.declared(castType, false, NullableState.NULLABLE, q)
                : CSharpTypeName.of(castType, q);
        OutputBuilder b = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(text(type)).add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
        ParameterizedType from = cast.expression().parameterizedType();
        ParameterizedType to = cast.parameterizedType();
        if (from != null && (to.isTypeParameter() && to.arrays() == 0 && !from.isTypeParameter()
                             || CstQueries.mentionsTypeParameter(to) && !CstQueries.mentionsTypeParameter(from))
            && !from.isJavaLangObject() && !(from.typeInfo() != null && from.typeInfo().isInterface()
                                             // a functional interface C# has as a delegate casts as a class does
                                             && !CSharpContext.program().delegate(from.typeInfo())
                                             && !CSharpTypeName.libraryDelegate(from.typeInfo()))) {
            // object? where the value may be null, as the outer cast's type is (CS8600)
            boolean nullableObject = nullable && CSharpContext.program().facts().nullabilityAnalysed();
            b.add(SymbolEnum.LEFT_PARENTHESIS).add(text(nullableObject ? "object?" : "object"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
        }
        OutputBuilder operand = operand(cast.precedence(), cast.expression(), q);
        // a method's own type parameter has no T?: the null goes through (T) x! as it does through Java's cast
        boolean asserted = nullable && CSharpNullability.toMethodTypeParameter(cast);
        return b.add(asserted ? CSharpNullability.asserted(cast.expression(), operand) : operand);
    }

    /**
 * A cast to a wildcard type of an operand that has the arguments, {@code (ListVariableDescriptor<?>) descriptor}
 * of a {@code GenuineVariableDescriptor<Solution_>}: the wildcards take the operand's arguments as the cast's
 * type extends the operand's, {@code (ListVariableDescriptor<Solution_>)}, where C#'s {@code <object>} converts
 * to nothing. Null when the cast has no unbound wildcard, the type has a wildcard interface for it, or the
 * operand does not determine the arguments.
 */
    private static ParameterizedType wildcardCastArguments(Cast cast) {
        ParameterizedType to = cast.parameterizedType();
        ParameterizedType from = cast.expression().parameterizedType();
        TypeInfo target = to.typeInfo();
        if (target == null || to.arrays() != 0 || from == null || from.arrays() != 0 || from.typeInfo() == null
            || from.parameters().isEmpty() || !CSharpNames.translated(target)
            || to.parameters().size() != target.typeParameters().size()
            || to.parameters().stream().noneMatch(a -> a.wildcard() != null && a.wildcard().isUnbound())
            || CSharpWildcardInterfaces.isWildcardUse(to)) {
            return null;
        }
        ParameterizedType asOperand = target.equals(from.typeInfo()) ? target.asParameterizedType()
                : CstSupertypes.asSeen(target, from.typeInfo());
        if (asOperand == null) return null;
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        CSharpExpressionPrinter.match(asOperand, from, target.typeParameters(), inferred);
        List<ParameterizedType> arguments = new ArrayList<>();
        for (int i = 0; i < to.parameters().size(); i++) {
            ParameterizedType a = to.parameters().get(i);
            if (a.wildcard() == null || !a.wildcard().isUnbound()) {
                arguments.add(a);
                continue;
            }
            ParameterizedType t = inferred.get(target.typeParameters().get(i));
            if (t == null || t.wildcard() != null || !CSharpExpressionPrinter.writable(t, List.of())) return null;
            arguments.add(t);
        }
        return to.withParameters(arguments);
    }

    /** A default method of a translated interface, called on a class (this, or a class-typed receiver). */
    private static boolean defaultThroughClass(MethodInfo method, Expression object, boolean implicit) {
        if (method.isStatic() || !method.isDefault() || !method.typeInfo().isInterface()
            || !CSharpNames.translated(method.typeInfo()) || CSharpContext.program().delegate(method.typeInfo())) {
            return false;
        }
        TypeInfo receiverType;
        if (object == null || implicit || object instanceof VariableExpression ve && ve.variable() instanceof This) {
            receiverType = CSharpContext.currentType();
        } else {
            ParameterizedType t = object.parameterizedType();
            // a wildcard use is its wildcard interface, which declares the method itself
            if (t != null && t.arrays() == 0 && CSharpWildcardInterfaces.isWildcardUse(t)) return false;
            receiverType = t == null ? null : t.typeInfo();
        }
        return receiverType != null && !receiverType.isInterface() && !receiverType.equals(method.typeInfo());
    }

    /**
 * {@code target}, where {@code e} is a generic call whose result Java infers raw, {@code newInstance(Class<?
 * extends SelectionFilter>)} converted unchecked into a {@code SelectionFilter<Solution_, Move<Solution_>>}: C#
 * prints the call's type arguments ({@link #inferredTypeArguments}) from the target instead of
 * {@code <ISelectionFilter<object, object>>}, which converts to nothing. Null otherwise.
 */
    private static ParameterizedType rawResultTarget(Expression e, ParameterizedType target) {
        if (!(e instanceof MethodCall mc) || target == null || target.arrays() != 0 || target.typeInfo() == null
            || target.parameters().isEmpty() || !CSharpExpressionPrinter.writable(target, List.of()) || !mc.typeArguments().isEmpty()) {
            return null;
        }
        MethodInfo method = mc.methodInfo();
        if (method.typeParameters().isEmpty() || !CSharpNames.translated(method.typeInfo())
            || !CSharpExpressionPrinter.uninferable(method) && !CSharpExpressionPrinter.onlyThroughLambdas(mc)) {
            return null;
        }
        ParameterizedType result = mc.concreteReturnType();
        if (result == null || result.arrays() != 0 || !target.typeInfo().equals(result.typeInfo())
            || !CstQueries.mentionsAny(method.returnType(), method.typeParameters())) {
            return null;
        }
        boolean rawResult = raw(result) || result.parameters().stream().allMatch(ParameterizedType::isJavaLangObject);
        return rawResult ? target : null;
    }

    /** The arguments of a call, without its type token. */
    private static List<Expression> withoutTypeToken(MethodCall mc) {
        int token = CSharpContext.program().typeToken(mc.methodInfo());
        if (token < 0) return mc.parameterExpressions();
        List<Expression> args = new ArrayList<>(mc.parameterExpressions());
        args.remove(token);
        return args;
    }

    /** A call or field access is printed: the per-expression nullability facts are asked for with it. */
    static OutputBuilder inCall(Expression call, Supplier<OutputBuilder> printer) {
        CSharpContext.pushCall(call);
        try {
            return printer.get();
        } finally {
            CSharpContext.popCall();
        }
    }

    private static OutputBuilder methodCall(MethodCall mc, Qualification q, boolean statement) {
        // isTerminated(b, score) of a Score<?>: score.Open_(new IsTerminatedOpener_(this, b))
        CSharpCallOpeners.Site opener = CSharpCallOpeners.site(mc);
        if (opener != null) return CSharpCallOpeners.call(opener, q);
        // list.toString(), b.toString() of a Boolean: Java's conversion, where C#'s ToString differs
        if (!statement && CSharpExpressionPrinter2.javaToString(mc)) return CSharpExpressionPrinter2.valueOf(mc.object(), q);
        OutputBuilder call = inCall(mc, () -> methodCallInCall(mc, q, statement));
        String cast = statement ? null : CSharpExpressionPrinter2.erasedResultCast(mc, q);
        if (cast == null) return call;
        // a result that may be null stays nullable through the casts (CS8600)
        boolean nullable = CSharpContext.program().facts().nullabilityAnalysed() && CSharpNullability.mayBeNull(mc)
                           && !cast.endsWith("?");
        OutputBuilder b = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(nullable ? cast + "?" : cast))
                .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
        // InnerScore<IScore> is no InnerScore<SimpleScore> to C#'s compiler: through object
        if (!mc.methodInfo().returnType().isTypeParameter()) {
            b.add(SymbolEnum.LEFT_PARENTHESIS).add(text(nullable ? "object?" : "object"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
        }
        return b.add(call);
    }

    private static OutputBuilder methodCallInCall(MethodCall mc, Qualification q, boolean statement) {
        OutputBuilder own = CSharpExpressionPrinter2.librarySingleAbstractMethodCall(mc, q);
        if (own != null) return own;
        LibraryMapping.Rule rule = CSharpExpressionPrinter2.rule(mc);
        if (rule != null) return mappedCall(mc, rule, q, statement);
        OutputBuilder special = specialCall(mc, q);
        if (special != null) return special;
        return CSharpExpressionPrinter.member(mc, CSharpExpressionPrinter.qualifier(mc, q), q);
    }

    /** A call of a library's method, as its rule's template has it. */
    private static OutputBuilder mappedCall(MethodCall mc, LibraryMapping.Rule rule, Qualification q, boolean statement) {
        rule.namespaces().forEach(CSharpContext::using);
        Expression object = mc.object();
        Supplier<OutputBuilder> receiver = () -> object == null ? text("this")
                : object instanceof VariableExpression ve && ve.variable() instanceof This t ? text(CSharpExpressionPrinter2.thisOrBase(t))
                : object instanceof TypeExpression te ? text(CSharpTypeName.nameInExpression(te.parameterizedType().typeInfo(), q))
                : mc.methodInfo().isStatic() ? CSharpExpressionPrinter.receiver(object, q) : CSharpNullability.dereferenced(object, q);
        List<Expression> parameterExpressions = mc.parameterExpressions();
        List<Supplier<OutputBuilder>> args = IntStream.range(0, parameterExpressions.size())
                .mapToObj(i -> (Supplier<OutputBuilder>) () -> CSharpExpressionPrinter.templateArgument(mc, i,
                        parameterExpressions.get(i), q)).toList();
        List<Supplier<OutputBuilder>> operands = mc.parameterExpressions().stream()
                .map(a -> (Supplier<OutputBuilder>) () -> CSharpExpressionPrinter.templateOperand(a, q)).toList();
        return new CSharpTemplate(receiver, args, operands, mc, mc.concreteReturnType(),
                object == null ? null : object.parameterizedType(), q).render(rule.template(statement));
    }

    /**
 * A call C# writes in a form of its own: an attribute's element, a delegate's invocation, an interface's default
 * method through the interface. Null for any other.
 */
    private static OutputBuilder specialCall(MethodCall mc, Qualification q) {
        Expression object = mc.object();
        MethodInfo method = mc.methodInfo();
        if (CSharpAttributes.isElement(method) && object != null) {
            // tool.name() reads the attribute's property
            return new OutputBuilderImpl().add(CSharpNullability.dereferenced(object, q)).add(SymbolEnum.DOT)
                    .add(text(CSharpAttributes.property(method)));
        }
        if (CSharpContext.program().isInvoke(method) && object != null && !mc.objectIsImplicit()) {
            // a call of a delegate is an invocation: f(x)
            return new OutputBuilderImpl().add(CSharpNullability.dereferenced(object, q))
                    .add(arguments(mc.parameterExpressions(), method, q));
        }
        if (defaultThroughClass(method, object, mc.objectIsImplicit())) {
            // a C# class does not inherit its interfaces' default methods: ((IResult) this).IsSuccess()
            OutputBuilder self = object == null || mc.objectIsImplicit() ? text("this")
                    : CSharpNullability.dereferenced(object, q);
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(text(CSharpExpressionPrinter.interfaceType(method.typeInfo(), object, q))).add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST)
                    .add(self).add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.DOT)
                    .add(text(CSharpNames.method(method) + CSharpExpressionPrinter.typeArguments(mc, q)))
                    .add(argumentsWithoutTypeToken(mc, q));
        }
        return null;
    }

    /** The arguments of a call; a type token's index is shifted out, and the others keep their parameter's type. */
    static OutputBuilder argumentsWithoutTypeToken(MethodCall mc, Qualification q) {
        int token = CSharpContext.program().typeToken(mc.methodInfo());
        if (token < 0) return arguments(mc.parameterExpressions(), mc.methodInfo(), q);
        List<Expression> args = withoutTypeToken(mc);
        if (args.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        return IntStream.range(0, args.size())
                .mapToObj(i -> argument(args.get(i), mc.methodInfo(), i < token ? i : i + 1, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
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
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter.receiver(cause, q))
                    .add(CSharpTemplate.NULL_CONDITIONAL).add(text("ToString")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                    .add(SymbolEnum.COMMA).add(print(cause, q)).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return IntStream.range(0, args.size()).mapToObj(i -> argument(args.get(i), method, i, q)).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA,
                SymbolEnum.LEFT_PARENTHESIS, SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }

    /** {@code (first, args…)}: an inner class's enclosing instance, then the arguments. */
    static OutputBuilder argumentsAfter(OutputBuilder first, List<Expression> args, MethodInfo method, Qualification q) {
        return Stream.concat(Stream.of(first), IntStream.range(0, args.size()).mapToObj(i -> argument(args.get(i), method, i, q)))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }

    /**
 * An anonymous class's creation: a type literal, a lambda, the hoisted class's constructor with the superclass's
 * arguments, the enclosing instance and the captures; the anonymous class's base otherwise, reported.
 */
    static OutputBuilder anonymousCreation(ConstructorCall cc, ParameterizedType type, Qualification q) {
        CSharpExpressionPrinter1.TypeLiteral literal = CSharpExpressionPrinter2.typeLiteral(cc.anonymousClass());
        if (literal != null) {
            return text("typeof(" + CSharpTypeName.name(literal.raw().typeInfo(), q) + "<" + literal.arguments()
                    .stream().map(a -> CSharpTypeName.argument(a, q)).collect(Collectors.joining(", ")) + ">)");
        }
        OutputBuilder adapted = anonymousAsLambda(cc, q);
        if (adapted != null) return adapted;
        CSharpAnonymous.Hoisted hoisted = CSharpContext.hoisted(cc.anonymousClass());
        if (hoisted != null) {
            // new GraphImpl(superArguments…, this, captures…)
            OutputBuilder arguments = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS);
            List<OutputBuilder> all = new ArrayList<>();
            // converted to the superclass constructor's parameters, which the hoisted constructor's are
            List<Expression> superArguments = hoisted.superArguments().isEmpty() ? List.of() : cc.parameterExpressions();
            MethodInfo superConstructor = CSharpAnonymous.superConstructor(cc);
            for (int i = 0; i < superArguments.size(); i++) {
                all.add(superConstructor == null ? print(superArguments.get(i), q)
                        : argument(superArguments.get(i), superConstructor, i, q));
            }
            if (hoisted.outer()) all.add(text(CSharpContext.hoistedWithOuter() != null ? "outer" : "this"));
            hoisted.captures().forEach(c -> all.add(text(CSharpContext.local(c.name()))));
            for (int i = 0; i < all.size(); i++) {
                if (i > 0) arguments.add(SymbolEnum.COMMA);
                arguments.add(all.get(i));
            }
            arguments.add(SymbolEnum.RIGHT_PARENTHESIS);
            String typeArguments = CSharpNames.typeParameters(hoisted.typeParameters());
            return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(text(hoisted.name() + typeArguments)).add(arguments);
        }
        CSharpContext.message(CSharpPrintCode.ANONYMOUS_CLASS, cc, CSharpContext.describe(cc));
        ParameterizedType parent = cc.anonymousClass().parentClass();
        ParameterizedType named = parent != null && !parent.isJavaLangObject() ? parent
                : cc.anonymousClass().interfacesImplemented().stream().findFirst().orElse(type);
        return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(text(CSharpTypeName.of(named, q))).add(arguments(cc.parameterExpressions(), q));
    }

    /** {@code new int[n][]}, C#'s jagged array as Java's, or an array with its initializer. */
    static OutputBuilder arrayCreation(ConstructorCall cc, ParameterizedType type, Qualification q) {
        if (cc.arrayInitializer() != null) {
            return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(text(CSharpTypeName.of(type, q))).add(SpaceEnum.ONE).add(arrayInitializer(cc.arrayInitializer(), type, q));
        }
        List<Expression> dimensions = cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).toList();
        if (dimensions.size() > 1 && dimensions.size() <= 4) {
            // new int[2][3]: Java creates the rows, C#'s new int[2][3] is no expression (CS0178), and new int[2][]
            // leaves them null: the compatibility library creates them, JavaArrays.Jagged<int>(2, 3)
            CSharpContext.using(CSharpCompat.NAMESPACE);
            ParameterizedType element = type.copyWithArrays(type.arrays() - dimensions.size());
            return new OutputBuilderImpl().add(text("JavaArrays.Jagged<" + CSharpTypeName.argument(element, q) + ">"))
                    .add(arguments(dimensions, q));
        }
        if (dimensions.size() > 1) {
            CSharpContext.message(CSharpPrintCode.MULTI_DIMENSIONAL_ARRAY, cc, CSharpContext.describe(cc));
        }
        // new int[n][] is C#'s jagged array creation, as Java's
        OutputBuilder b = new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(text(CSharpTypeName.element(type.copyWithoutArrays(), q)));
        for (int i = 0; i < type.arrays(); i++) {
            if (i < dimensions.size()) {
                b.add(SymbolEnum.LEFT_BRACKET).add(print(dimensions.get(i), q)).add(SymbolEnum.RIGHT_BRACKET);
            } else {
                b.add(SymbolEnum.OPEN_CLOSE_BRACKETS);
            }
        }
        return b;
    }

    /**
 * A diamond whose arguments Java inferred from a captured wildcard parameter, {@code new Equal<>(u, s)} of an
 * {@code Unpacker<?> u} the method captures as {@code TK} ({@link CSharpWildcards#captured}): the capture where
 * the parser has a wildcard, {@code Equal<T, TK>}; a type parameter no argument determines, as its bound has it
 * ({@code C extends Collection<K>}: {@code ICollection<TK>}). {@code type} itself otherwise.
 */
    static ParameterizedType fromCaptured(ConstructorCall cc, ParameterizedType type) {
        TypeInfo created = type.typeInfo();
        List<TypeParameter> own = created.typeParameters();
        if (type.parameters().size() != own.size() || type.parameters().stream().noneMatch(CSharpExpressionPrinter2::hasWildcard)
            || cc.parameterExpressions().stream().noneMatch(a -> CSharpContext.capturedType(unwrap(a)) != null)) {
            return type;
        }
        Map<TypeParameter, ParameterizedType> inferred = CSharpExpressionPrinter.inferredFromArguments(cc, own);
        List<ParameterizedType> arguments = new ArrayList<>();
        for (int i = 0; i < own.size(); i++) {
            ParameterizedType a = type.parameters().get(i);
            if (!CSharpExpressionPrinter2.hasWildcard(a)) {
                arguments.add(a);
                continue;
            }
            ParameterizedType t = inferred.get(own.get(i));
            if (t == null && own.get(i).typeBounds().size() == 1) {
                ParameterizedType bound = CstSupertypes.substitute(own.get(i).typeBounds().getFirst(), inferred);
                if (!CstQueries.mentionsAny(bound, own)) t = bound;
            }
            if (t == null || !CSharpExpressionPrinter.writable(t, List.of())) return type;
            arguments.add(t);
        }
        return type.withParameters(arguments);
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
        return converted(value, target, q, true);
    }

    /** As above; {@code erase} false where the target's wildcard is a type parameter C# captures it into. */
    private static OutputBuilder converted(Expression value, ParameterizedType target, Qualification q, boolean erase) {
        if (target == null) return print(value, q);
        Expression inner = unwrap(value);
        if (inner instanceof NullConstant && target.isTypeParameter() && target.arrays() == 0) return text("default");
        if (inner instanceof InlineConditional ic && (converts(ic.ifTrue(), target) || converts(ic.ifFalse(), target))) {
            // the branches: C#'s conditional takes its type from them
            return new OutputBuilderImpl().add(booleanOperand(ic.precedence(), ic.condition(), q))
                    .add(SymbolEnum.QUESTION_MARK).add(converted(ic.ifTrue(), target, q, erase))
                    .add(SymbolEnum.COLON).add(converted(ic.ifFalse(), target, q, erase));
        }
        if (target.isPrimitiveExcludingVoid() && unboxed(inner)) return unboxCast(CSharpTypeName.of(target, q), value, q);
        // Java creates an array of a generic type raw, new Set[n]; C# creates it with the declared type's arguments
        if (CSharpExpressionPrinter2.rawArray(inner, target)) return CSharpExpressionPrinter.constructorCall((ConstructorCall) inner, target, q);
        ParameterizedType fromTarget = erasedDiamond(inner, target);
        if (fromTarget != null) return CSharpExpressionPrinter.constructorCall((ConstructorCall) inner, fromTarget, q);
        ParameterizedType resultTarget = rawResultTarget(inner, target);
        if (resultTarget != null) {
            CSharpContext.callTarget((MethodCall) inner, resultTarget);
            return print(value, q);
        }
        // a raw constant lambda returned as the generic type, TRI_PICK_FIRST of a TriFunction<A, B, C, A>: the lambda
        // itself, which C# caches per type arguments as Java's one instance is cached
        Expression constant = constantLambda(inner, target);
        if (constant instanceof Lambda lambda) return CSharpExpressionPrinter2.adapted(target, CSharpExpressionPrinter2.lambda(lambda, q), q);
        if (constant != null) return print(constant, q);
        // a type argument is the value type itself, List<int>: a K of a Dictionary<int, V> is an int
        if (target.isTypeParameter() && target.arrays() == 0 && unboxed(inner)) {
            return unboxCast(CSharpTypeName.argument(inner.parameterizedType(), q), value, q);
        }
        // a class's wildcard interface where its superclass is expected: IAbstractConcatNode is no AbstractNode to C#
        if (CSharpExpressionPrinter2.wildcardToSuperclass(inner, target)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(CSharpTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter.receiver(value, q));
        }
        OutputBuilder group = CSharpExpressionPrinter.implementationGroup(value, inner, target, q);
        if (group != null) return group;
        OutputBuilder widened = CSharpExpressionPrinter.widenedDelegate(inner, target, q);
        if (widened != null) return widened;
        OutputBuilder delegateCast = delegateCast(value, inner, target, q);
        if (delegateCast != null) return delegateCast;
        // a raw or wildcard use where the type is expected with arguments, ScoreDefinition getScoreDefinition() into a
        // ScoreDefinition<Score_>: Java's unchecked conversion, C#'s cast of the wildcard interface, which the value is
        if (CSharpExpressionPrinter.wildcardIntoGeneric(inner, target)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(CSharpTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter.receiver(value, q));
        }
        // a raw value of a type printed with object arguments, a raw sentinel EMPTY_TUPLE into a UniTuple<B>: Java's
        // unchecked conversion, C#'s cast; a class's constructed types cast through object only, (X<B>) (object) v
        if (rawIntoGeneric(inner, target)) {
            OutputBuilder cast = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(CSharpTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
            if (!target.typeInfo().isInterface()) {
                cast.add(SymbolEnum.LEFT_PARENTHESIS).add(text("object")).add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST);
            }
            return cast.add(CSharpExpressionPrinter.receiver(value, q));
        }
        // Map.keySet where a Set is expected: .Keys is an ICollection, the compatibility library's view a set
        if (CSharpExpressionPrinter2.keySetAsSet(inner, target)) {
            CSharpContext.using(CSharpCompat.NAMESPACE);
            return new OutputBuilderImpl().add(CSharpExpressionPrinter.receiver(((MethodCall) inner).object(), q)).add(SymbolEnum.DOT)
                    .add(text("KeySet()"));
        }
        // Java's raw cast into a parameterized target, (Map) weights for a Map<Constraint, SimpleScore>: unchecked in
        // Java, C#'s cast to the target, (IDictionary<IConstraint, SimpleScore>) weights, not IDictionary<object, object>
        if (CSharpExpressionPrinter2.rawCastInto(inner, target)) {
            Expression operand = ((Cast) inner).expression();
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(CSharpTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter.receiver(operand, q));
        }
        // Java's array covariance of a type parameter, Node_[] as an Object[]: C#'s holds for a reference type only,
        // which it does not know the type parameter to be; at run time it is one, (object[]) (object) values
        if (covariantArray(inner, target)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(CSharpTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(SymbolEnum.LEFT_PARENTHESIS).add(text("object"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter.receiver(value, q));
        }
        // a concrete collector where Java's wildcard of it is expected, IUniConstraintCollector<A, object, double>
        ParameterizedType adapted = erase ? CSharpErasures.adapted(inner, target) : null;
        if (adapted != null) return CSharpErasures.adapter(adapted, print(value, q), q);
        return print(value, q);
    }

    /**
 * A {@code Func} or {@code Action} where the same functional interface is expected with other type arguments, which
 * C#'s variance does not convert: a type parameter, a value type, or {@code object} narrowed, at a position that
 * differs. {@code JavaDelegates.Cast<object, object, A, object>(castJoiner.GetLeftMapping(i))}, which casts the
 * arguments and the result at each call as Java's unchecked conversion lets them through. Not a lambda or a method
 * reference, which take the target's type themselves.
 */
    private static OutputBuilder delegateCast(Expression value, Expression inner, ParameterizedType target,
                                              Qualification q) {
        if (target == null || inner instanceof Lambda || inner instanceof MethodReference || inner instanceof NullConstant) {
            return null;
        }
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || target.arrays() != 0 || from.typeInfo() == null
            || !from.typeInfo().equals(target.typeInfo()) || !CSharpTypeName.libraryDelegate(from.typeInfo())
            // not a generic callee's parameter, Func<T, R> of Max<T, R>, whose type parameters C# infers, nor a super
            // constructor's, Func<A, Input_>, of the superclass's
            || !CSharpExpressionPrinter.writableNested(target, List.of()) || !ownTypeParameters(target)) {
            return null;
        }
        List<String> fromArgs = delegateArguments(CSharpTypeName.of(from, q));
        List<String> toArgs = delegateArguments(CSharpTypeName.of(target, q));
        if (fromArgs == null || toArgs == null || fromArgs.size() != toArgs.size() || fromArgs.equals(toArgs)) return null;
        List<String> typeParameters = new ArrayList<>();
        for (TypeInfo t = CSharpContext.currentType(); t != null; t = CstQueries.enclosing(t)) {
            t.typeParameters().forEach(tp -> typeParameters.add(CSharpNames.typeParameter(tp)));
        }
        MethodInfo method = CSharpContext.currentMethod();
        if (method != null) method.typeParameters().forEach(tp -> typeParameters.add(CSharpNames.typeParameter(tp)));
        boolean needed = false;
        for (int i = 0; i < fromArgs.size(); i++) {
            String f = fromArgs.get(i);
            String t = toArgs.get(i);
            if (f.equals(t)) continue;
            needed |= typeParameters.contains(f) || typeParameters.contains(t) || "object".equals(f)
                      || VALUE_TYPE_NAMES.contains(f) || VALUE_TYPE_NAMES.contains(t);
        }
        if (!needed) return null;
        CSharpContext.using(CSharpCompat.NAMESPACE);
        List<String> arguments = new ArrayList<>(fromArgs);
        arguments.addAll(toArgs);
        return new OutputBuilderImpl().add(text("JavaDelegates.Cast<" + String.join(", ", arguments) + ">"))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(print(value, q)).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /**
 * The type arguments of a printed {@code Func<…>} or {@code Action<…>}, at its top level; null for another type, or
 * one {@code JavaDelegates} has no overload of.
 */
    private static List<String> delegateArguments(String printed) {
        String inside;
        if (printed.startsWith("Func<") && printed.endsWith(">")) inside = printed.substring(5, printed.length() - 1);
        else if (printed.startsWith("Action<") && printed.endsWith(">")) inside = printed.substring(7, printed.length() - 1);
        else return null;
        List<String> arguments = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                arguments.add(inside.substring(start, i).strip());
                start = i + 1;
            }
        }
        arguments.add(inside.substring(start).strip());
        // JavaDelegates' overloads: a Func of up to three parameters, an Action of up to two
        return arguments.size() <= (printed.startsWith("Func<") ? 4 : 2) ? arguments : null;
    }

    /**
 * A value of a raw type that C# prints with {@code object} arguments (no wildcard interface), a variable or a
 * call's result, where the type itself is expected with arguments C# can name here: Java's unchecked
 * conversion, which C#'s reified generics check at run time, as Java's would have at the first use.
 */
    static boolean rawIntoGeneric(Expression inner, ParameterizedType target) {
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || target.arrays() != 0 || from.typeInfo() == null
            || !from.typeInfo().equals(target.typeInfo()) || target.parameters().isEmpty() || !raw(from)
            || !CSharpNames.translated(from.typeInfo()) || CSharpWildcardInterfaces.isWildcardUse(from)
            || CSharpWildcardInterfaces.isWildcardUse(target)) {
            return false;
        }
        // a variable declared raw, or a call of a method declared to return the raw type; not a generic method's
        // result Java inferred raw from a raw argument, which C# infers from the argument as it prints it
        boolean declaredRaw = inner instanceof VariableExpression
                              || inner instanceof MethodCall mc && raw(mc.methodInfo().returnType());
        return declaredRaw && target.parameters().stream().allMatch(a -> a.wildcard() == null && !CSharpExpressionPrinter.constructorTypeParameter(a))
               && CSharpExpressionPrinter.writable(target, List.of()) && ownTypeParameters(target);
    }

    /**
 * Whether each type parameter {@code t} mentions is the current method's or the current type's (or an enclosing
 * type's): not a superclass's, of a super constructor's parameter, which the subclass may name otherwise.
 */
    static boolean ownTypeParameters(ParameterizedType t) {
        if (t.isTypeParameter()) {
            var owner = t.typeParameter().getOwner();
            if (owner == null) return false;
            if (owner.isRight()) return owner.getRight().equals(CSharpContext.currentMethod());
            for (TypeInfo at = CSharpContext.currentType(); at != null; at = CstQueries.enclosing(at)) {
                if (at.equals(owner.getLeft())) return true;
            }
            return false;
        }
        return t.parameters().stream().allMatch(CSharpExpressionPrinter1::ownTypeParameters);
    }

    /**
 * An array of a type parameter where an array of Object is expected, as many dimensions: C# converts it only for
 * a type parameter it knows to be a reference type, of a class bound.
 */
    private static boolean covariantArray(Expression inner, ParameterizedType target) {
        ParameterizedType from = inner.parameterizedType();
        if (from == null || target.arrays() == 0 || from.arrays() != target.arrays() || !from.isTypeParameter()
            || target.typeInfo() == null || !target.isJavaLangObject() && !"java.lang.Object".equals(target.typeInfo()
                .fullyQualifiedName())) {
            return false;
        }
        return from.typeParameter().typeBounds().stream().allMatch(b -> b.isJavaLangObject()
                                                                         || b.typeInfo() != null && b.typeInfo().isInterface());
    }

    /** {@code (int) x}; Java's cast to a boxed type, {@code (Integer) o}, becomes the cast to the primitive. */
    static OutputBuilder unboxCast(String primitive, Expression value, Qualification q) {
        Expression inner = unwrap(value);
        Expression operand = inner instanceof Cast cast && CSharpTypeName.valueReference(cast.parameterizedType())
                ? cast.expression() : value;
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(text(primitive))
                .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST)
                .add(CSharpNullability.unboxed(operand, CSharpExpressionPrinter.receiver(operand, q)));
    }

    /** A JDK exception's constructor of its cause. */
    private static boolean causeOnly(MethodInfo method) {
        return method != null && method.isConstructor() && !CSharpNames.translated(method.typeInfo())
               && method.parameters().size() == 1
               && method.parameters().getFirst().parameterizedType().typeInfo() != null
               && "java.lang.Throwable".equals(method.parameters().getFirst().parameterizedType().typeInfo()
                .fullyQualifiedName());
    }

    /** The {@code i}-th argument of a call of {@code method}: converted to its parameter's type, asserted when null may not go there. */
    static OutputBuilder argument(Expression value, MethodInfo method, int i, Qualification q) {
        // Marshal(TimeSpan v) of an adapter's XmlAdapter<string, TimeSpan>: the value type
        if (method != null && unboxedValue(value) && CSharpMethodPrinter.plainParameter(method, i)) {
            return unboxCast(CSharpTypeName.argument(value.parameterizedType(), q), value, q);
        }
        ParameterizedType target = argumentTarget(value, method, i);
        // a constructor's T created as a struct, new Range<DateOnly>(from): the value type, as a type argument is
        ParameterizedType declared = parameterType(method, i);
        if (declared != null && declared.isTypeParameter() && declared.arrays() == 0 && target != null
            && !target.isTypeParameter() && CSharpTypeName.valueReference(target) && unboxed(unwrap(value))) {
            return CSharpNullability.intoParameter(value, method, i,
                    unboxCast(CSharpTypeName.argument(unwrap(value).parameterizedType(), q), value, q));
        }
        // HashMap::new as a generic method's Supplier<Result_>: Java infers Result_ from where the reference goes, a
        // Map<Key, Value>, C# from the lambda's result, which is cast to it: () => (IDictionary<Key, Value>) new Dictionary…
        ParameterizedType inferred = unwrap(value) instanceof MethodReference mr ? CSharpExpressionPrinter.resultInferred(mr, method, i) : null;
        if (inferred != null) {
            MethodReference mr = (MethodReference) unwrap(value);
            return CSharpNullability.intoParameter(value, method, i,
                    CSharpExpressionPrinter2.adapted(mr.parameterizedType(), CSharpExpressionPrinter.methodReference(mr, inferred, q), q));
        }
        boolean captured = method != null && i < method.parameters().size() && CSharpWildcards.capturedParameter(method, i);
        return CSharpNullability.intoParameter(value, method, i, converted(value, target, q, !captured));
    }

    /**
 * The type an argument converts to: its parameter's; for a variable arity parameter, the array type when the
 * argument is the array itself, a raw {@code new BiJoiner[] { joiner }} that becomes {@code new IBiJoiner<A, B>[]}.
 */
    private static ParameterizedType argumentTarget(Expression value, MethodInfo method, int i) {
        ParameterizedType target = parameterType(method, i);
        if (target != null || method == null || i != method.parameters().size() - 1) return createdTypeArguments(target, method);
        ParameterInfo p = method.parameters().get(i);
        ParameterizedType type = value.parameterizedType();
        return p.isVarArgs() && type != null && type.arrays() == p.parameterizedType().arrays()
                ? createdTypeArguments(p.parameterizedType(), method) : null;
    }

    /**
 * A constructor's parameter type with the class's type parameters the type arguments it is created with:
 * {@code new DefaultBiJoiner(new Function[0])}, raw in a static member, is
 * {@code new DefaultBiJoiner<object, object>(new Func<object, object>[0])}, not the class's {@code A}, out of scope.
 */
    private static ParameterizedType createdTypeArguments(ParameterizedType target, MethodInfo method) {
        if (target == null || method == null || !method.isConstructor()) return target;
        if (!(CSharpContext.currentCall() instanceof ConstructorCall cc) || cc.constructor() != method) return target;
        ParameterizedType created = CSharpContext.created(cc);
        // created raw, new DefaultBiJoiner(new Function[0], …) in a static member: C#'s of object, whose parameter
        // is no conversion's target; the raw array is its own Func<object, object>[]
        if (created != null && created.typeInfo() != null && created.parameters().isEmpty()
            && !created.typeInfo().typeParameters().isEmpty() && CstQueries.mentionsAny(target,
                created.typeInfo().typeParameters())) {
            return null;
        }
        if (created == null || created.typeInfo() == null
            || created.parameters().size() != created.typeInfo().typeParameters().size()) {
            return target;
        }
        return CstSupertypes.substituteDroppingWildcards(target, CstSupertypes.typeArguments(created));
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
        return target.isTypeParameter() && target.arrays() == 0 && (inner instanceof NullConstant || unboxed(inner))
               || target.isPrimitiveExcludingVoid() && unboxed(inner) || CSharpExpressionPrinter2.rawArray(inner, target)
               || erasedDiamond(inner, target) != null || rawResultTarget(inner, target) != null
               || CSharpErasures.adapted(inner, target) != null
               || constantLambda(inner, target) != null || CSharpExpressionPrinter.implementation(inner, target) != null
               || covariantArray(inner, target) || CSharpExpressionPrinter2.rawCastInto(inner, target) || CSharpExpressionPrinter2.keySetAsSet(inner, target)
               || CSharpExpressionPrinter.wildcardIntoGeneric(inner, target) || rawIntoGeneric(inner, target)
               || inner instanceof InlineConditional ic
                  && (converts(ic.ifTrue(), target) || converts(ic.ifFalse(), target));
    }

    /**
 * A diamond of raw arguments, {@code new DefaultBiJoiner<>(newLeftMappings, …)} of a {@code new Function[n]}: Java
 * infers the raw type, which converts to the {@code DefaultBiJoiner<A, B>} returned unchecked; C# creates it with
 * the target's type arguments, or those of the class that make it the target, a supertype's. Null
 * when the creation is not such a diamond, or the target does not determine them.
 */
    private static ParameterizedType erasedDiamond(Expression e, ParameterizedType target) {
        if (!(e instanceof ConstructorCall cc) || cc.anonymousClass() != null || cc.diamond() == null
            || !cc.diamond().isYes() || target.arrays() != 0 || target.typeInfo() == null) {
            return null;
        }
        ParameterizedType type = cc.parameterizedType();
        TypeInfo created = type.typeInfo();
        if (created == null || type.arrays() != 0 || created.typeParameters().isEmpty()
            || !type.parameters().stream().allMatch(ParameterizedType::isJavaLangObject)) {
            return null;
        }
        List<TypeParameter> own = created.typeParameters();
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        ParameterizedType asTarget = created.equals(target.typeInfo()) ? created.asParameterizedType()
                : CstSupertypes.asSeen(created, target.typeInfo());
        if (asTarget != null) CSharpExpressionPrinter.match(asTarget, target, own, inferred);
        List<ParameterizedType> arguments = new ArrayList<>();
        for (TypeParameter tp : own) {
            ParameterizedType t = inferred.get(tp);
            if (t == null || !CSharpExpressionPrinter.writable(t, List.of())) return null;
            arguments.add(t);
        }
        if (arguments.stream().allMatch(ParameterizedType::isJavaLangObject)) return null;
        // a raw argument is C#'s of object, Func<object, object>[]: only where its parameter becomes one of object too,
        // the Comparator<T> of a raw Comparator with T object, not the Function<A, ?>[] of a raw Function[]
        Map<TypeParameter, ParameterizedType> bound = new HashMap<>();
        for (int i = 0; i < own.size(); i++) bound.put(own.get(i), arguments.get(i));
        List<ParameterInfo> parameters = cc.constructor() == null ? List.of() : cc.constructor().parameters();
        for (int i = 0; i < parameters.size() && i < cc.parameterExpressions().size(); i++) {
            ParameterizedType argument = CSharpExpressionPrinter2.printedType(cc.parameterExpressions().get(i));
            if (!raw(argument)) continue;
            ParameterizedType formal = CstSupertypes.substituteDroppingWildcards(parameters.get(i).parameterizedType(), bound);
            if (!formal.parameters().stream().allMatch(p -> p.isJavaLangObject() || p.wildcard() != null
                                                            && p.wildcard().isUnbound())
                && !CSharpExpressionPrinter2.storedAs(cc.parameterExpressions().get(i), formal) && !CSharpExpressionPrinter2.variantRaw(argument, formal)) {
                return null;
            }
        }
        return type.withParameters(arguments);
    }

    /**
 * The initializer of a static final field of a raw functional type, a lambda, or {@code Function.identity()},
 * where its value goes as the type with arguments: {@code private static final TriFunction TRI_PICK_FIRST =
 * (a, b, c) -> a} returned as a {@code TriFunction<A, B, C, A>}. C#'s field is of object, which no
 * {@code ITriFunction<A, B, C, A>} is; a lambda that captures nothing C# creates once per type arguments. Null
 * otherwise.
 */
    private static Expression constantLambda(Expression inner, ParameterizedType target) {
        if (!(inner instanceof VariableExpression ve) || !(ve.variable() instanceof FieldReference fr)) return null;
        FieldInfo field = fr.fieldInfo();
        ParameterizedType type = field.type();
        if (!field.isStatic() || !field.isFinal() || type == null || !raw(type) || type.arrays() != 0
            || !type.typeInfo().equals(target.typeInfo()) || target.arrays() != 0 || target.parameters().isEmpty()
            || target.parameters().stream().allMatch(ParameterizedType::isJavaLangObject)
            || !CSharpExpressionPrinter.writable(target, List.of())) {
            return null;
        }
        Expression init = field.initializer() == null ? null : unwrap(field.initializer());
        // a -> null: null is no value of the target's type parameter B in C#
        if (init instanceof Lambda lambda) {
            Expression returned = CstQueries.returnedValue(CstQueries.own(lambda.methodInfo().methodBody().statements()));
            return returned != null && unwrap(returned) instanceof NullConstant ? null : init;
        }
        if (init instanceof MethodCall mc && mc.methodInfo().isStatic() && mc.parameterExpressions().isEmpty()
            && "identity".equals(mc.methodInfo().name())
            && mc.methodInfo().typeInfo().fullyQualifiedName().startsWith("java.util.function.")) {
            return init;
        }
        return null;
    }

    /** A generic type without its type arguments, {@code Function[]}. */
    private static boolean raw(ParameterizedType t) {
        return t != null && t.typeInfo() != null && !t.typeInfo().typeParameters().isEmpty() && t.parameters().isEmpty();
    }

    /** The value of a {@code return} in the current method or lambda. */
    static OutputBuilder returned(Expression value, Qualification q) {
        MethodInfo method = CSharpContext.currentMethod();
        // a lambda's result is its delegate's type argument, an override of T Get()'s the class's: the value type.
        // An override's boxed value too, int GetKey() => (int) key! of an Integer key (CS0266 without the cast)
        if (method != null && (unboxedValue(value) && method.typeInfo().isAnonymous()
                               || CSharpMethodPrinter.plainResult(method) && unboxed(unwrap(value)))) {
            return unboxCast(CSharpTypeName.argument(value.parameterizedType(), q), value, q);
        }
        return CSharpNullability.intoReturn(value, converted(value, method == null ? null : method.returnType(), q));
    }

    /**
 * A value of a reference type that is C#'s nullable value type, an {@code Integer}'s {@code int?}
 * ({@link CSharpTypeName#valueReference}): a call or a variable, not a constant or an operation.
 */
    static boolean unboxed(Expression e) {
        ParameterizedType type = e.parameterizedType();
        return CSharpTypeName.valueReference(type) && !CSharpExpressionPrinter2.plainValue(e)
               && (e instanceof MethodCall || e instanceof VariableExpression && !CSharpExpressionPrinter2.lambdaParameter(e) || e instanceof Cast
                   || e instanceof Assignment);
    }

    /** An operand of an arithmetic operator: a boxed one is cast to its value type. */
    static OutputBuilder unboxedOperand(Precedence precedence, Expression e, Qualification q) {
        Expression inner = unwrap(e);
        return unboxed(inner) && !(inner.parameterizedType().isJavaLangString())
                ? unboxCast(CSharpTypeName.argument(inner.parameterizedType(), q), e, q) : operand(precedence, e, q);
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
                : unboxedValue(x) ? unboxCast(CSharpTypeName.argument(x.parameterizedType(), q), x, q)
                // an element converted as a value into the element type: a wildcard interface's value cast into the
                // generic type, new ITriFunction<A, B, C, object>[] { (ITriFunction<A, B, C, object>) leftMapping }
                : CSharpNullability.intoElement(x, element, converted(x, element, q))).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA,
                SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.defaultGuideGenerator()));
    }

    /**
 * An anonymous class of a translated functional interface that only implements its method, without fields and
 * without using itself: a lambda in the interface's adapter class, {@code new IVisitor.Lambda((string node) => {
 * … })}. Null for any other.
 */
    private static OutputBuilder anonymousAsLambda(ConstructorCall cc, Qualification q) {
        TypeInfo anonymous = cc.anonymousClass();
        if (!CSharpExpressionPrinter2.asLambda(cc)) return null;
        ParameterizedType functionalType = anonymous.interfacesImplemented().getFirst();
        MethodInfo method = anonymous.methods().stream().filter(m -> !m.isSynthetic()).findFirst().orElseThrow();
        OutputBuilder lambda = CSharpExpressionPrinter2.lambda(method, () -> method.parameters().stream()
                .map(p -> CSharpExpressionPrinter.asDelegateHasIt(functionalType.typeInfo(), p.index(), p.parameterizedType(),
                        CSharpTypeName.argument(p.parameterizedType(), q)) + " " + CSharpContext.declare(p.name()))
                .collect(Collectors.joining(", ", "(", ")")), false, q);
        return CSharpExpressionPrinter2.adapted(functionalType, lambda, q);
    }

    /**
 * {@code c += 2} of a char, a byte or a short: Java's compound assignment narrows its result implicitly, C#'s does
 * not convert an int to a char (CS0266), nor a non-constant int to a byte or a short. Of a plain variable, which
 * may be read twice: {@code c = (char) (c + 2)}. Null otherwise.
 */
    static OutputBuilder narrowingCompound(Assignment a, String operator, Qualification q) {
        if (a.assignmentOperator() == null || !operator.endsWith("=") || operator.length() < 2) return null;
        char target = CSharpConstants.kind(a.variableTarget().parameterizedType());
        char value = CSharpConstants.kind(a.value().parameterizedType());
        if (target != 'C' && target != 'B' && target != 'S') return null;
        boolean fits = target != 'C' && value != 'J' && value != 'F' && value != 'D'
                       && CSharpConstants.value(a.value()) instanceof Integer i
                       && (target == 'B' ? i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE : i >= Short.MIN_VALUE && i <= Short.MAX_VALUE);
        if (fits || !(a.variableTarget() instanceof LocalVariable || a.variableTarget() instanceof ParameterInfo
                      || a.variableTarget() instanceof FieldReference fr && (fr.fieldInfo().isStatic() || CstQueries.onThis(fr)))) {
            return null;
        }
        String type = target == 'C' ? "char" : target == 'B' ? "sbyte" : "short";
        String op = operator.substring(0, operator.length() - 1);
        return new OutputBuilderImpl().add(CSharpExpressionPrinter2.variable(a.variableTarget(), q)).add(SymbolEnum.assignment("="))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(text(type)).add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST)
                .add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter2.variable(a.variableTarget(), q))
                .add(SymbolEnum.binaryOperator(op)).add(CSharpExpressionPrinter2.atomic(unwrap(a.value())) || unwrap(a.value()) instanceof ConstantExpression<?> c && !CSharpExpressionPrinter.negative(c)
                     ? print(a.value(), q) : CSharpExpressionPrinter.parenthesized(print(a.value(), q)))
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    private static OutputBuilder negation(Negation neg, Qualification q) {
        Expression inner = unwrap(neg.expression());
        if (inner instanceof InstanceOf io) return instanceOf(io, true, q); // !(x instanceof T) -> x is not T
        // !(a == b) is a != b, unless == is not C#'s == (binaryOperator): a reference identity, or a value against an
        // object, which the default below negates
        if (inner instanceof Equals equals && !CSharpExpressionPrinter2.referenceIdentity(equals.lhs(), equals.rhs())
            && !CSharpExpressionPrinter2.valueAndObject(equals.lhs(), equals.rhs()) && !CSharpExpressionPrinter2.valueAndObject(equals.rhs(), equals.lhs())) {
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
        // a type test a library maps to a pattern: a java.lang.reflect.Type that is a Class is a System.Type that is
        // neither a type parameter nor constructed
        LibraryMapping.CodeMapping mapped = CSharpContext.libraries().pattern(io.testType(),
                io.expression().parameterizedType());
        if (mapped != null && mapped.namespace() != null) CSharpContext.using(mapped.namespace());
        String classPattern = mapped == null ? null : mapped.value();
        if (pattern != null && pattern.localVariable() != null) {
            ParameterizedType declared = CSharpExpressionPrinter.rawTestArguments(pattern.localVariable().parameterizedType(),
                    io.expression().parameterizedType());
            return b.add(text(classPattern != null ? classPattern : CSharpTypeName.pattern(declared, q)))
                    .add(SpaceEnum.ONE).add(text(CSharpContext.declare(pattern.localVariable().simpleName())));
        }
        String deconstructed = pattern == null ? null : CSharpExpressionPrinter2.recordPattern(pattern, null, q);
        if (deconstructed != null) return b.add(text(deconstructed));
        if (pattern != null) {
            CSharpContext.message(CSharpPrintCode.SWITCH_FORM, io, CSharpContext.describe(io));
        }
        return b.add(text(classPattern != null ? classPattern
                : CSharpTypeName.pattern(CSharpExpressionPrinter.rawTestArguments(io.testType(), io.expression().parameterizedType()), q)));
    }

    static String stringLiteral(String s) {
        return CSharpExpressionPrinter.LITERALS.stringLiteral(s);
    }

    static String charLiteral(char c) {
        return CSharpExpressionPrinter.LITERALS.charLiteral(c);
    }

    static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }

    static Expression unwrap(Expression e) {
        return CstQueries.unwrap(e);
    }

    /** An operand, in parentheses when its precedence is lower than its operator's. */
    static OutputBuilder operand(Precedence precedence, Expression e, Qualification q) {
        OutputBuilder inner = print(e, q);
        // a method reference may print as a lambda, p0 => new HashSet<object>(p0)
        if (precedence.greaterThan(e.precedence()) || e instanceof Lambda || e instanceof MethodReference
            || CSharpExpressionPrinter2.nonAtomicCall(e)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return inner;
    }

    /** An enum's or a struct's {@code X?} ({@link #unboxed}), not an {@code Integer}'s {@code int?}. */
    static boolean unboxedValue(Expression e) {
        Expression inner = unwrap(e);
        return unboxed(inner) && !inner.parameterizedType().isBoxedExcludingVoid();
    }
}
