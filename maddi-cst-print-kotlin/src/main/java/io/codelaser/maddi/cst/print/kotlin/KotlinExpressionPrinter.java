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
import io.codelaser.maddi.cst.api.output.element.Symbol;
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

    private static final Symbol SPREAD = new SymbolEnum("*", SpaceEnum.NONE, SpaceEnum.NONE, null);

    private static final Map<String, String> INFIX = Map.of("&", "and", "|", "or", "^", "xor",
            "<<", "shl", ">>", "shr", ">>>", "ushr");

    /** An expression whose value is used. */
    public static OutputBuilder print(Expression e, Qualification q) {
        return switch (e) {
            case ConstructorCall cc -> inCall(cc, () -> constructorCall(cc, cc.parameterizedType(), q));
            case Cast cast -> cast(cast, false, q);
            case InstanceOf io -> instanceOf(io, q);
            case InlineConditional ic when isElvis(ic) ->
                // desugared elvis `a ?: b` = InlineConditional(a==null, ifTrue=b, ifFalse=a); recover the `?:`
                    new OutputBuilderImpl().add(operand(ic.precedence(), ic.ifFalse(), q))
                            .add(SymbolEnum.binaryOperator("?:")).add(operand(ic.precedence(), ic.ifTrue(), q));
            case InlineConditional ic -> new OutputBuilderImpl()
                    .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ic.condition(), q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                    .add(branch(ic.ifTrue(), () -> print(ic.ifTrue(), q), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.ELSE).add(SpaceEnum.ONE).add(branch(ic.ifFalse(), () -> print(ic.ifFalse(), q), q));
            case MethodCall mc -> inCall(mc, () -> nonNullFilter(mc, methodCall(mc, q)));
            case MethodReference mr -> methodReference(mr, q);
            case SwitchExpression se -> KotlinStatementPrinter.whenExpression(se.selector(), se.entries(), false, q);
            case Lambda lambda -> lambda(lambda, q);
            case Assignment a when HOISTED.get().contains(a) -> variable(a.variableTarget(), q);
            case Assignment a -> assignmentAsValue(a, q);
            case BitwiseNegation bn -> new OutputBuilderImpl().add(receiver(bn.expression(), q)).add(SymbolEnum.DOT)
                    .add(new TextImpl("inv")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
            case Negation neg -> negation(neg, q);
            case StringConcat sc -> stringConcat(sc, q);
            case BinaryOperator bo when bo.operator() != null -> binaryOperator(bo, q);
            case And and -> and.expressions().stream().map(x -> condition(and.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND));
            case Or or -> or.expressions().stream().map(x -> condition(or.precedence(), x, q))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_OR));
            case UnaryOperator uo -> unaryOperator(uo, q);
            case EnclosedExpression ee -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ee.inner(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
            // a field or array access is where its own per-expression fact is asked: before the dereference
            case VariableExpression ve when ve.variable() instanceof DependentVariable
                                            || ve.variable() instanceof FieldReference fr && !fr.isStatic() ->
                    inCall(ve, () -> variable(ve.variable(), q));
            case VariableExpression ve -> variable(ve.variable(), q);
            case ArrayLength al -> new OutputBuilderImpl().add(KotlinNullability.receiverWithDot(al.scope(), q))
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
            default -> {
                // constants (int, long, boolean, null) and anything unknown: as in Java
                if (!(e instanceof ConstantExpression<?>)) {
                    KotlinContext.message(KotlinPrintMessage.Code.JAVA_FALLBACK, e, e.getClass().getSimpleName());
                }
                yield e.print(q);
            }
        };
    }

    /** An expression whose value is discarded: an expression statement, a for-loop update, a lambda body. */
    public static OutputBuilder printStatement(Expression e, Qualification q) {
        if (e instanceof Assignment a) return assignmentAsStatement(a, q);
        if (e instanceof EnclosedExpression ee) return printStatement(ee.inner(), q);
        if (e instanceof MethodCall mc) {
            List<Assignment> hoisted = hoistable(mc);
            if (!hoisted.isEmpty()) return hoisted(mc, hoisted, q);
        }
        return print(e, q);
    }

    // assignments printed before the statement they are an argument of; in it, they are their target
    private static final ThreadLocal<java.util.Set<Expression>> HOISTED =
            ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /**
     * {@code map.put(k, lst = new ArrayList<>())} as a statement: {@code lst = ArrayList()} first, then
     * {@code map.put(k, lst)}. As a value, the assignment is {@code ArrayList().also { lst = it }}, and a local a lambda
     * assigns gets no smart casts anywhere ({@code lst.add(id)} on the next line). Only where Java evaluates nothing
     * with an effect before the assignment: a receiver and earlier arguments that are variables or constants, none of
     * them the assigned local.
     */
    private static List<Assignment> hoistable(MethodCall mc) {
        List<Assignment> hoisted = new ArrayList<>();
        java.util.Set<io.codelaser.maddi.cst.api.variable.Variable> assigned = new java.util.HashSet<>();
        if (!mc.objectIsImplicit() && mc.object() != null && !pure(mc.object(), assigned)) return hoisted;
        for (Expression argument : mc.parameterExpressions()) {
            if (unwrap(argument) instanceof Assignment a && a.prefixPrimitiveOperator() == null
                && (a.assignmentOperator() == null || "=".equals(a.assignmentOperator().name()))
                && a.variableTarget() instanceof io.codelaser.maddi.cst.api.variable.LocalVariable
                && !(unwrap(a.value()) instanceof Assignment)) {
                hoisted.add(a);
                assigned.add(a.variableTarget());
            } else if (!pure(argument, assigned)) {
                break;
            }
        }
        // the receiver is evaluated before any argument: it must not read what an argument assigns
        if (!mc.objectIsImplicit() && mc.object() != null && !pure(mc.object(), assigned)) return List.of();
        return hoisted;
    }

    /** A constant, or a variable read (through fields of variables) that is none of {@code assigned}. */
    private static boolean pure(Expression e, java.util.Set<io.codelaser.maddi.cst.api.variable.Variable> assigned) {
        Expression x = unwrap(e);
        if (x instanceof ConstantExpression<?>) return true;
        if (!(x instanceof VariableExpression ve) || assigned.contains(ve.variable())) return false;
        return switch (ve.variable()) {
            case FieldReference fr -> fr.isStatic() || fr.scope() == null || pure(fr.scope(), assigned);
            case io.codelaser.maddi.cst.api.variable.DependentVariable dv -> false;
            default -> true;
        };
    }

    private static OutputBuilder hoisted(MethodCall mc, List<Assignment> hoisted, Qualification q) {
        List<OutputBuilder> lines = new ArrayList<>();
        hoisted.forEach(a -> lines.add(assignmentAsStatement(a, q)));
        HOISTED.get().addAll(hoisted);
        try {
            lines.add(print(mc, q));
        } finally {
            hoisted.forEach(HOISTED.get()::remove);
        }
        return lines.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
    }

    private static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }

    // ---------------------------------------------------------------- variables

    static OutputBuilder variable(Variable v, Qualification q) {
        return switch (v) {
            case This t -> new OutputBuilderImpl().add(new TextImpl(thisOrSuper(t)));
            case FieldReference fr -> fieldReference(fr, q);
            case DependentVariable dv -> new OutputBuilderImpl().add(KotlinNullability.asserted(dv.arrayExpression(), q))
                    .add(SymbolEnum.LEFT_BRACKET).add(print(dv.indexExpression(), q)).add(SymbolEnum.RIGHT_BRACKET);
            default -> {
                java.util.function.Supplier<OutputBuilder> pattern = KotlinContext.patternVariable(v);
                yield pattern != null ? pattern.get() : new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(v.simpleName())));
            }
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
            TypeInfo owner = fr.fieldInfo().owner();
            // Kotlin inherits no statics: unqualified only when the owner's companion is in scope
            if (fr.isDefaultScope() && (KotlinContext.typeInScope(owner) || KotlinTypePrinter.fromKotlinSource(owner))) {
                return text(name);
            }
            return text(KotlinTypeName.staticOwner(owner, q) + "." + name);
        }
        if (fr.isDefaultScope() || scope == null) {
            return text(KotlinContext.shadowedByParameter(fr.fieldInfo().name()) ? "this." + name : name);
        }
        return new OutputBuilderImpl().add(KotlinNullability.receiverWithDot(scope, q)).add(new TextImpl(name));
    }

    // ---------------------------------------------------------------- calls

    /** A call is printed: the per-expression nullability facts are asked for with it (its receiver, its arguments). */
    private static OutputBuilder inCall(Expression call, java.util.function.Supplier<OutputBuilder> printer) {
        KotlinContext.pushCall(call);
        try {
            return printer.get();
        } finally {
            KotlinContext.popCall();
        }
    }

    /**
     * ⛔ #103: a call on {@code super} keeps its {@code super.}. The object is the same {@code This} pseudo-variable
     * as an implicit {@code this}, distinguished only by {@link This#writeSuper()}; dropping it like an implicit
     * {@code this} turned {@code return super.addAll(c)} in an override into {@code return addAll(c)}: a call to
     * itself.
     */
    private static OutputBuilder methodCall(MethodCall mc, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        Expression object = mc.object();
        if (KotlinMappedMembers.isMonitorMethod(mc.methodInfo())) {
            // lock.notifyAll() -> (lock as java.lang.Object).notifyAll()
            OutputBuilder receiver = object == null || mc.objectIsImplicit() ? text("this") : print(object, q);
            return b.add(SymbolEnum.LEFT_PARENTHESIS).add(receiver).add(SpaceEnum.ONE).add(KotlinKeyword.AS)
                    .add(SpaceEnum.ONE).add(new TextImpl("java.lang.Object")).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SymbolEnum.DOT).add(new TextImpl(mc.methodInfo().name()))
                    .add(arguments(mc.parameterExpressions(), mc.methodInfo(), q));
        }
        if (KotlinMappedMembers.isJavaStringOnly(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            // s.stripTrailing() -> (s as java.lang.String).stripTrailing()
            return b.add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinNullability.asserted(object, KotlinPrintMessage.Code.ASSERT_AT_DEREFERENCE, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl("java.lang.String"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.DOT).add(new TextImpl(mc.methodInfo().name()))
                    .add(arguments(mc.parameterExpressions(), mc.methodInfo(), q));
        }
        if (KotlinMappedMembers.isUnboxing(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            return KotlinNullability.asserted(object, KotlinPrintMessage.Code.ASSERT_AT_UNBOXING, q);
        }
        if (KotlinMappedMembers.isRequireNonNull(mc.methodInfo())) {
            return KotlinNullability.asserted(mc.parameterExpressions().getFirst(), q);
        }
        if (object instanceof VariableExpression ve && ve.variable() instanceof This t) {
            if (t.writeSuper() || (t.explicitlyWriteType() != null && !mc.objectIsImplicit())) {
                b.add(new TextImpl(thisOrSuper(t))).add(SymbolEnum.DOT);
            }
        } else if (object instanceof TypeExpression te) {
            TypeInfo owner = mc.methodInfo().typeInfo();
            if (!mc.objectIsImplicit() || !KotlinContext.typeInScope(owner) && !KotlinTypePrinter.fromKotlinSource(owner)) {
                // SingleClassesTest.collectClasses(f), declared in SingleClassesTestBase: a companion's members are
                // not inherited, so the declaring type names them
                TypeInfo named = mc.methodInfo().isStatic() && KotlinNullability.translated(owner)
                                 && te.parameterizedType().typeInfo() != owner ? owner : te.parameterizedType().typeInfo();
                b.add(new TextImpl(KotlinTypeName.staticOwner(named, q))).add(SymbolEnum.DOT);
            }
        } else if (object != null && !mc.objectIsImplicit()) {
            b.add(KotlinNullability.receiverWithDot(object, q));
        }
        if (KotlinTypePrinter.isRecordAccessor(mc.methodInfo()) || KotlinTypePrinter.isAnnotationElement(mc.methodInfo())) {
            // a data class's property; an annotation's element, Java's too, which Kotlin reads as a property
            return b.add(new TextImpl(KotlinNames.name(mc.methodInfo().name()))); // a data class property
        }
        if (KotlinMappedMembers.isRemoveAt(mc.methodInfo())) {
            return b.add(new TextImpl("removeAt")).add(arguments(mc.parameterExpressions(), q));
        }
        if (toArrayOf(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            // c.toArray(new X[0]): Kotlin's collections have no toArray(T[]); toTypedArray<X>() types the array as
            // Java's argument does (Kotlin's arrays are invariant, its Collection<out E> is not)
            ParameterizedType array = mc.parameterExpressions().getFirst().parameterizedType();
            String element = array == null || array.arrays() != 1 ? null : KotlinTypeName.of(array.componentType()
                    .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q);
            return b.add(new TextImpl(element == null ? "toTypedArray()" : "toTypedArray<" + element + ">()"));
        }
        String mapped = KotlinMappedMembers.property(mc.methodInfo());
        if (mapped != null) return b.add(new TextImpl(mapped));
        if (KotlinMappedMembers.isIndexGet(mc.methodInfo())) {
            // s.charAt(i) -> s[i]; the receiver was written above, with a dot that must go
            OutputBuilder indexed = new OutputBuilderImpl();
            if (object != null && !mc.objectIsImplicit()) indexed.add(KotlinNullability.asserted(object, q));
            else indexed.add(text("this"));
            return indexed.add(SymbolEnum.LEFT_BRACKET).add(print(mc.parameterExpressions().getFirst(), q))
                    .add(SymbolEnum.RIGHT_BRACKET);
        }
        String renamed = KotlinMappedMembers.function(mc.methodInfo());
        if (renamed != null) {
            return b.add(new TextImpl(renamed)).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        String sameArguments = KotlinMappedMembers.renamed(mc.methodInfo());
        if (sameArguments != null) {
            return b.add(new TextImpl(sameArguments)).add(arguments(mc.parameterExpressions(), mc.methodInfo(), q));
        }
        if (KotlinMappedMembers.isSplit(mc.methodInfo())) {
            // Java's split drops trailing empty strings, and returns an array
            return b.add(new TextImpl("split")).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(receiver(mc.parameterExpressions().getFirst(), q)).add(SymbolEnum.DOT)
                    .add(new TextImpl("toRegex")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SymbolEnum.DOT).add(new TextImpl("dropLastWhile { it.isEmpty() }"))
                    .add(SymbolEnum.DOT).add(new TextImpl("toTypedArray")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        KotlinMappedMembers.Regex regex = KotlinMappedMembers.regex(mc.methodInfo());
        if (regex != null) {
            // s.replaceAll(r, x) -> s.replace(r.toRegex(), x): Kotlin's replace(String, String) is literal
            List<Expression> args = mc.parameterExpressions();
            OutputBuilder call = b.add(new TextImpl(regex.kotlinName())).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(receiver(args.getFirst(), q)).add(SymbolEnum.DOT).add(new TextImpl("toRegex"))
                    .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
            for (Expression a : args.subList(1, args.size())) call.add(SymbolEnum.COMMA).add(print(a, q));
            return call.add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (KotlinMappedMembers.isEqualsIgnoreCase(mc.methodInfo())) {
            return b.add(new TextImpl("equals")).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(mc.parameterExpressions().getFirst(), q)).add(SymbolEnum.COMMA)
                    .add(new TextImpl("ignoreCase = true")).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        String typeArguments = !mc.typeArguments().isEmpty() ? mc.typeArguments().stream()
                .map(t -> KotlinTypeName.of(t, q)).collect(java.util.stream.Collectors.joining(", ", "<", ">"))
                : comparingKeyCall(mc) ? comparedType(mc, q) : resultOnlyTypeArguments(mc, q);
        b.add(new TextImpl(KotlinNames.name(mc.methodInfo().name()) + (typeArguments == null ? "" : typeArguments)));
        return b.add(arguments(mc.parameterExpressions(), mc.methodInfo(), mc, q));
    }

    /**
     * Type arguments Kotlin cannot infer: of type parameters that no parameter mentions, so that only an expected
     * type could fix them. Java takes the bound, {@code <F extends Failure> List<F> failures()} gives a
     * {@code List<Failure>} for {@code failures().stream()}, which Kotlin writes {@code failures<Failure>()}. Where
     * the call has no expected type (a receiver; an argument into an Object or a class's type parameter, as
     * {@code map.put("items", Collections.emptyMap())}), the unbounded ones take Java's inferred type, or Object.
     * Null when Kotlin infers them by itself.
     */
    private static String resultOnlyTypeArguments(MethodCall mc, Qualification q) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mc.methodInfo();
        List<io.codelaser.maddi.cst.api.info.TypeParameter> typeParameters = m.typeParameters();
        if (typeParameters.isEmpty() || !KotlinContext.translatingJava() || m.returnType() == null) return null;
        List<io.codelaser.maddi.cst.api.info.TypeParameter> resultOnly = typeParameters.stream()
                .filter(tp -> m.parameters().stream().noneMatch(p -> mentions(p.parameterizedType(), tp))).toList();
        if (resultOnly.isEmpty()) return null;
        boolean bounded = resultOnly.stream().anyMatch(tp -> bound(tp) != null);
        boolean noExpectedType = noExpectedType(mc);
        if (!bounded && !noExpectedType) return null;
        Map<io.codelaser.maddi.cst.api.type.NamedType, ParameterizedType> concrete = mc.concreteReturnType() == null
                ? Map.of() : m.returnType().formalToConcrete(mc.concreteReturnType());
        List<String> printed = new ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.TypeParameter tp : typeParameters) {
            ParameterizedType t = concrete.get(tp);
            if (t == null || t.wildcard() != null || mentionsAny(t, typeParameters)) {
                if (!resultOnly.contains(tp)) return null;
                t = bound(tp);
                if (t == null) {
                    if (!noExpectedType) return null;
                    printed.add("Any");
                    continue;
                }
            }
            if (t.typeParameter() != null && !KotlinContext.typeParameterInScope(t.typeParameter())) return null;
            printed.add(KotlinTypeName.of(t, q));
        }
        return printed.stream().collect(java.util.stream.Collectors.joining(", ", "<", ">"));
    }

    /** The single bound of a type parameter, when it is not Object and does not mention a type parameter. */
    private static ParameterizedType bound(io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        if (tp.typeBounds().size() != 1) return null;
        ParameterizedType bound = tp.typeBounds().getFirst();
        return bound.isJavaLangObject() || bound.typeParameter() != null || bound.hasTypeParameters() ? null : bound;
    }

    /**
     * The call is a receiver, or an argument into an Object or into a type parameter of the callee's type
     * ({@code map.put(k, v)}): Kotlin has nothing to infer its type arguments from.
     */
    private static boolean noExpectedType(MethodCall mc) {
        if (!(KotlinContext.enclosingCall() instanceof MethodCall outer)) return false;
        if (outer.object() != null && unwrap(outer.object()) == mc) return true;
        List<Expression> args = outer.parameterExpressions();
        List<ParameterInfo> params = outer.methodInfo().parameters();
        for (int i = 0; i < args.size() && i < params.size(); i++) {
            if (unwrap(args.get(i)) != mc || params.get(i).isVarArgs()) continue;
            ParameterizedType target = params.get(i).parameterizedType();
            return target.isJavaLangObject() || target.typeParameter() != null && !target.typeParameter().isMethodTypeParameter();
        }
        return false;
    }

    private static boolean mentions(ParameterizedType type, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        if (tp.equals(type.typeParameter())) return true;
        return type.parameters().stream().anyMatch(p -> mentions(p, tp));
    }

    private static boolean mentionsAny(ParameterizedType type, List<io.codelaser.maddi.cst.api.info.TypeParameter> tps) {
        return tps.stream().anyMatch(tp -> mentions(type, tp));
    }

    private static OutputBuilder arguments(List<Expression> args, Qualification q) {
        return arguments(args, null, q);
    }

    static OutputBuilder arguments(List<Expression> args, io.codelaser.maddi.cst.api.info.MethodInfo method,
                                   Qualification q) {
        return arguments(args, method, null, q);
    }

    /**
     * The arguments, each widened to its parameter's primitive type as Java does implicitly (not a varargs one). A
     * parameter typed by the receiver's type parameter takes the receiver's type argument as its target
     * ({@code list.add(x)} on a {@code List<String>}: x is asserted non-null when Kotlin types it nullable).
     */
    static OutputBuilder arguments(List<Expression> args, io.codelaser.maddi.cst.api.info.MethodInfo method,
                                   MethodCall call, Qualification q) {
        if (args.isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        // A member of a type Kotlin maps to its own has Kotlin's parameter types: String.indexOf takes a Char there,
        // so no widening; and they are non-null (MutableList<Statement>.add takes a Statement), so a nullable
        // argument is asserted like one for translated code. equals takes Any? everywhere.
        boolean mapped = method != null && KotlinTypeName.isMapped(method.typeInfo().fullyQualifiedName());
        boolean translated = method != null && (mapped ? !"equals".equals(method.name())
                : KotlinNullability.translated(method.typeInfo()));
        List<OutputBuilder> printed = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            if (spread(args, method, i)) {
                printed.add(new OutputBuilderImpl().add(SPREAD)
                        .add(KotlinNullability.asserted(args.get(i), KotlinPrintMessage.Code.ASSERT_INTO_NON_NULL, q)));
                continue;
            }
            boolean hasParameter = method != null && i < method.parameters().size()
                                   && !method.parameters().get(i).isVarArgs();
            ParameterizedType target = !hasParameter || mapped ? null : method.parameters().get(i).parameterizedType();
            // an argument converts to the parameter's interface by itself; no SAM constructor needed
            if (call != null && unwrap(args.get(i)) instanceof MethodReference mr && nullableStreamElements(method, call)) {
                OutputBuilder asserting = assertingReference(mr, q);
                if (asserting != null) {
                    printed.add(asserting);
                    continue;
                }
            }
            if (hasParameter && unwrap(args.get(i)) instanceof MethodReference && besideTypeParameter(method, i)) {
                printed.add(new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.name(
                                method.parameters().get(i).parameterizedType().typeInfo(), q)))
                        .add(SymbolEnum.LEFT_PARENTHESIS).add(print(args.get(i), q)).add(SymbolEnum.RIGHT_PARENTHESIS));
                continue;
            }
            if (hasParameter && comparingKey(method, args.get(i))) {
                printed.add(comparingKeyLambda((MethodReference) unwrap(args.get(i)), q));
                continue;
            }
            if (unwrap(args.get(i)) instanceof Lambda l) {
                // Kotlin types the parameters from the call: a nullable element makes them nullable
                List<ParameterizedType> types = call == null || !hasParameter ? List.of()
                        : KotlinNullability.lambdaParameterTypes(call, method.parameters().get(i).parameterizedType());
                List<ParameterInfo> params = l.parameters();
                if (call != null && (sortsNullable(method) || optionalOfNullableValue(method, call))) {
                    // Kotlin infers the parameters nullable where Java hands over a value, or throws: `o!!.id`
                    types = params.stream().map(p -> p.parameterizedType().withNullable(
                            io.codelaser.maddi.cst.api.type.NullableState.NULLABLE)).toList();
                }
                boolean typed = types.size() == params.size();
                for (int j = 0; typed && j < params.size(); j++) {
                    ParameterizedType t = types.get(j);
                    if (t != null && KotlinNullability.isNullable(t)) KotlinContext.lambdaParameterType(params.get(j), t);
                }
                try {
                    printed.add(lambda(l, false, q));
                } finally {
                    if (typed) params.forEach(p -> KotlinContext.lambdaParameterType(p, null));
                }
            } else {
                ParameterizedType declared = !hasParameter ? null : KotlinNullability.parameterType(method.parameters().get(i));
                boolean argumentTranslated = translated;
                if (call != null) {
                    ParameterizedType seen = KotlinNullability.throughReceiver(declared, call, true);
                    // the receiver's type argument is a declaration of ours, even on a library member (queue.add)
                    if (seen != declared) argumentTranslated = true;
                    declared = seen;
                }
                // set.contains(x), map.get(k) with a nullable x or k: Java's take an Object, and null finds nothing;
                // Kotlin's stdlib extensions take any supertype of the element or key, so no `!!` (which throws)
                ParameterizedType lookedUp = hasParameter && call != null ? nullableLookup(method, call, args.get(i), i) : null;
                if (lookedUp != null) {
                    if (unwrap(args.get(i)).parameterizedType().typeInfo() != lookedUp.typeInfo()
                        && KotlinNullability.nullableInKotlin(args.get(i))) {
                        // lst.remove(varassign) with a VarExprent? from a List<Exprent>: T is inferred from the
                        // inputs only, so the argument is cast up to the element type
                        printed.add(new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                                .add(operand(PrecedenceEnum.CAST, args.get(i), q))
                                .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                                .add(new TextImpl(KotlinTypeName.of(lookedUp.withNullable(
                                        io.codelaser.maddi.cst.api.type.NullableState.NULLABLE), q)))
                                .add(SymbolEnum.RIGHT_PARENTHESIS));
                        continue;
                    }
                    argumentTranslated = false;
                }
                if (hasParameter && unwrap(args.get(i)) instanceof ConstructorCall cc && cc.arrayInitializer() != null
                    && cc.parameterizedType().arrays() == 1 && !cc.parameterizedType().componentType().isPrimitiveExcludingVoid()
                    && method.parameters().get(i).parameterizedType().typeParameter() != null
                    && method.parameters().get(i).parameterizedType().typeParameter().isMethodTypeParameter()) {
                    // Map.of(k1, new Integer[]{1, 2}, k2, new Integer[]{null, 3}): V is one type for all of them,
                    // Array<Int?> as the target says; Kotlin infers it when no arrayOf<Int> fixes one array's
                    printed.add(arrayInitializer(cc.arrayInitializer(), null, q));
                    continue;
                }
                // a constructor call takes the target's states (Kotlin's generics are invariant)
                ParameterizedType widenTo = unwrap(args.get(i)) instanceof ConstructorCall && declared != null
                        ? declared : target;
                OutputBuilder argument = KotlinNullability.toTarget(args.get(i), declared, argumentTranslated,
                        widened(args.get(i), widenTo, q), q);
                if (hasParameter && boxedIntoReference(method, i, args.get(i))) {
                    // new PrimitiveConstant(tag, Integer.valueOf(v)): Java's Integer takes (int, Object), Kotlin's
                    // Int the (int, int) overload; the cast keeps Java's choice
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && collectionBesideObject(method, i)) {
                    // template.apply(Collections.singletonMap("name", "Klaus")) beside apply(Object): Java takes the
                    // Map overload; Kotlin's MutableMap<String, Any?> is invariant and takes no Map<String, String>,
                    // so Kotlin took apply(value: Any). The cast keeps Java's choice, unchecked, as Java's generics are
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && nullableBesideVarargs(method, i)) {
                    // UserMessage(text: String?) beside UserMessage(name: String, vararg contents: Content): Java
                    // takes the first for UserMessage("hi"), Kotlin the more specific second with no contents
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                }
                if (KotlinContext.translatingJava() && mockitoMatcher(args.get(i))) {
                    // verify(listener).onRequest(any()): the matcher returns null, and Kotlin checks a platform value
                    // passed as a non-null parameter -- "any(...) must not be null". The helper's unchecked cast does not
                    argument = new OutputBuilderImpl().add(new TextImpl(MOCKITO_MATCHED)).add(SymbolEnum.LEFT_PARENTHESIS)
                            .add(argument).add(SymbolEnum.RIGHT_PARENTHESIS);
                    KotlinContext.fileHelper(MOCKITO_MATCHED_DECLARATION);
                }
                printed.add(argument);
            }
        }
        return printed.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator()));
    }

    /**
     * {@code stream.filter(Objects::nonNull)} followed by {@code .map { it!! }}: Java's filter leaves no null, Kotlin's
     * keeps the element type nullable ({@code map(classes::get)} makes it a {@code StructClass?}), and every lambda
     * further down the chain would need a {@code !!} it cannot be given. The {@code it!!} never throws.
     */
    private static OutputBuilder nonNullFilter(MethodCall mc, OutputBuilder printed) {
        if (!"filter".equals(mc.methodInfo().name()) || mc.parameterExpressions().size() != 1
            || !"java.util.stream.Stream".equals(mc.methodInfo().typeInfo().fullyQualifiedName())
            || !nonNullPredicate(unwrap(mc.parameterExpressions().getFirst()))) {
            return printed;
        }
        return printed.add(SymbolEnum.DOT).add(new TextImpl("map")).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE)
                .add(SpaceEnum.ONE).add(new TextImpl("it!!")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    /** {@code Objects::nonNull}, or {@code x -> x != null}. */
    private static boolean nonNullPredicate(Expression e) {
        if (e instanceof MethodReference mr) {
            return "nonNull".equals(mr.methodInfo().name())
                   && "java.util.Objects".equals(mr.methodInfo().typeInfo().fullyQualifiedName());
        }
        if (e instanceof Lambda l && l.parameters().size() == 1) {
            List<Statement> statements = l.methodBody().statements().stream().filter(st -> !st.isSynthetic()).toList();
            return statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs
                   && unwrap(rs.expression()) instanceof BinaryOperator bo && bo.operator() != null
                   && "!=".equals(bo.operator().name())
                   && (unwrap(bo.rhs()) instanceof NullConstant && isParameter(bo.lhs(), l.parameters().getFirst())
                       || unwrap(bo.lhs()) instanceof NullConstant && isParameter(bo.rhs(), l.parameters().getFirst()));
        }
        return false;
    }

    private static boolean isParameter(Expression e, ParameterInfo p) {
        return unwrap(e) instanceof VariableExpression ve && p.equals(ve.variable());
    }

    /**
     * {@code Comparator.comparing(order::get)}: a key extractor whose result may be null. Kotlin wants a Comparable
     * key, and an {@code Int?} is none; Java compares the null, and throws there. As
     * {@code comparing({ order.get(it)!! })}, which throws at the same point (and takes an {@code Int?} element too).
     */
    private static final java.util.Set<String> COMPARING_KEY = java.util.Set.of("comparing", "thenComparing",
            "comparingInt", "comparingLong", "comparingDouble", "thenComparingInt", "thenComparingLong",
            "thenComparingDouble");

    private static boolean comparingKey(io.codelaser.maddi.cst.api.info.MethodInfo method, Expression argument) {
        if (!"java.util.Comparator".equals(method.typeInfo().fullyQualifiedName())
            || !COMPARING_KEY.contains(method.name())
            || !(unwrap(argument) instanceof MethodReference mr) || mr.methodInfo().isConstructor()
            || mr.methodInfo().isStatic() || mr.methodInfo().parameters().size() != 1
            || mr.scope() == null || mr.scope() instanceof TypeExpression) {
            return false;
        }
        io.codelaser.maddi.cst.api.info.MethodInfo m = mr.methodInfo();
        return KotlinNullability.nullableJdkResult(m) || KotlinNullability.isNullable(KotlinNullability.returnType(m));
    }

    /**
     * {@code Comparator.comparingDouble(scores::get).reversed()}: the key extractor became a lambda, whose parameter
     * Kotlin cannot type with nothing expected of the comparator. Not {@code comparing}, whose key type is a second
     * type parameter.
     */
    private static boolean comparingKeyCall(MethodCall mc) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mc.methodInfo();
        return m.isStatic() && m.typeParameters().size() == 1 && mc.parameterExpressions().size() == 1
               && m.name().startsWith("comparing") && comparingKey(m, mc.parameterExpressions().getFirst())
               && noExpectedType(mc);
    }

    /** The compared type as Java infers it, {@code <Any>} for Object. */
    private static String comparedType(MethodCall mc, Qualification q) {
        ParameterizedType comparator = mc.concreteReturnType();
        ParameterizedType t = comparator == null || comparator.parameters().size() != 1 ? null
                : comparator.parameters().getFirst();
        if (t == null || t.wildcard() != null || t.typeParameter() != null && !KotlinContext.typeParameterInScope(t.typeParameter())) {
            return "<Any>";
        }
        return "<" + KotlinTypeName.of(t, q) + ">";
    }

    private static OutputBuilder comparingKeyLambda(MethodReference mr, Qualification q) {
        KotlinContext.message(KotlinPrintMessage.Code.ASSERT_AT_DEREFERENCE, mr, KotlinContext.describe(mr));
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SpaceEnum.ONE).add(receiver(mr.scope(), q))
                .add(SymbolEnum.DOT).add(new TextImpl(KotlinNames.name(mr.methodInfo().name())))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(new TextImpl("it")).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(new TextImpl("!!")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    /**
     * {@code getOrDefault(x, Foo::new)} beside an overload {@code getOrDefault(T, T)}: Kotlin takes the reference as
     * a value of T (Any) there, unless a SAM constructor makes it the Supplier.
     */
    private static boolean besideTypeParameter(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (!KotlinContext.translatingJava()) return false;
        ParameterizedType functional = method.parameters().get(i).parameterizedType();
        if (functional.typeInfo() == null || !functional.typeInfo().isInterface()) return false;
        return method.typeInfo().methods().stream().anyMatch(other -> other != method
                && other.name().equals(method.name()) && other.parameters().size() == method.parameters().size()
                && other.parameters().get(i).parameterizedType().typeParameter() != null
                && other.parameters().get(i).parameterizedType().arrays() == 0);
    }

    /** A call on a {@code Stream<X?>}: Kotlin hands its functions an X?. */
    private static boolean nullableStreamElements(io.codelaser.maddi.cst.api.info.MethodInfo method, MethodCall call) {
        if (method == null || !"java.util.stream.Stream".equals(method.typeInfo().fullyQualifiedName())
            || call.object() == null || call.objectIsImplicit()) {
            return false;
        }
        ParameterizedType stream = KotlinNullability.kotlinType(call.object());
        if (stream != null && stream.parameters().size() == 1 && KotlinNullability.isNullable(stream.parameters().getFirst())) {
            return true;
        }
        // a Collection<?>'s stream() is a Stream<out Any?> in Kotlin
        ParameterizedType java = call.object().parameterizedType();
        return java != null && java.parameters().size() == 1 && java.parameters().getFirst().isUnboundWildcard();
    }

    /**
     * A method reference applied to the nullable elements of a stream, which Java passes on as they are:
     * {@code mapToInt(Helper::size)} as {@code { Helper.size(it!!) }} where size takes a non-null Statement, and
     * {@code map(Statement::getId)} as {@code { it!!.getId() }}, {@code map(this::transform)} as
     * {@code { this.transform(it!!) }}. Null when the reference takes the element as it is.
     */
    private static OutputBuilder assertingReference(MethodReference mr, Qualification q) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mr.methodInfo();
        if (m.isConstructor() || mappedMember(mr) != null) return null;
        String name = KotlinNames.name(m.name());
        if (!(mr.scope() instanceof TypeExpression te)) {
            if (m.isStatic() || m.parameters().size() != 1 || !KotlinNullability.translated(m.typeInfo())
                || KotlinNullability.isNullable(KotlinNullability.parameterType(m.parameters().getFirst()))) {
                return null;
            }
            KotlinContext.message(KotlinPrintMessage.Code.ASSERT_INTO_NON_NULL, mr, KotlinContext.describe(mr));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SpaceEnum.ONE).add(receiver(mr.scope(), q))
                    .add(SymbolEnum.DOT).add(new TextImpl(name + "(it!!)")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
        }
        // a library's parameter is a platform type, which takes the null: Objects::isNull stays a reference
        if (m.isStatic() && m.parameters().size() == 1 && KotlinNullability.translated(m.typeInfo())
            && !KotlinNullability.isNullable(KotlinNullability.parameterType(m.parameters().getFirst()))) {
            KotlinContext.message(KotlinPrintMessage.Code.ASSERT_INTO_NON_NULL, mr, KotlinContext.describe(mr));
            return text("{ " + KotlinTypeName.staticOwner(te.parameterizedType().typeInfo(), q) + "." + name + "(it!!) }");
        }
        if (!m.isStatic() && m.parameters().isEmpty()) {
            KotlinContext.message(KotlinPrintMessage.Code.ASSERT_AT_DEREFERENCE, mr, KotlinContext.describe(mr));
            return text("{ it!!." + name + "() }");
        }
        return null;
    }

    /**
     * {@code Optional<Statement> o = stats.stream().filter(…).findAny()}: Java's Optional never holds a null, but
     * Kotlin types the result after the stream's {@code Statement?} elements, or after a {@code map} lambda that
     * returns null (Java's empty), and an {@code Optional<Statement?>} is no {@code Optional<Statement>}.
     */
    private static boolean optionalOfNullable(Expression e, ParameterizedType target) {
        if (target == null || target.typeInfo() == null || !"java.util.Optional".equals(target.typeInfo().fullyQualifiedName())
            || target.parameters().size() != 1 || KotlinNullability.isNullable(target.parameters().getFirst())
            || !(unwrap(e) instanceof MethodCall mc)) {
            return false;
        }
        String declaring = mc.methodInfo().typeInfo().fullyQualifiedName();
        ParameterizedType result = mc.methodInfo().returnType();
        return result != null && result.typeInfo() != null && "java.util.Optional".equals(result.typeInfo().fullyQualifiedName())
               && ("java.util.Optional".equals(declaring) || "java.util.stream.Stream".equals(declaring))
               && nullableInChain(mc);
    }

    /**
     * Somewhere in this Optional or Stream chain Kotlin's element type becomes nullable: a stream of X? elements, or
     * a {@code map} lambda that returns null.
     */
    private static boolean nullableInChain(MethodCall mc) {
        for (Expression x = mc; unwrap(x) instanceof MethodCall c; x = c.object()) {
            String declaring = c.methodInfo().typeInfo().fullyQualifiedName();
            if (!"java.util.Optional".equals(declaring) && !"java.util.stream.Stream".equals(declaring)) {
                ParameterizedType t = KotlinNullability.kotlinType(c);
                return t != null && t.parameters().size() == 1 && KotlinNullability.isNullable(t.parameters().getFirst());
            }
            if ("map".equals(c.methodInfo().name()) && c.parameterExpressions().size() == 1
                && unwrap(c.parameterExpressions().getFirst()) instanceof Lambda l && returnsNull(l)) {
                return true;
            }
            if (c.object() == null || c.objectIsImplicit()) return false;
        }
        return false;
    }

    /** {@code null}, {@code c ? x : null}, or a value Kotlin types nullable ({@code stats.get(0).getExprents()}). */
    private static boolean mayBeNull(Expression e) {
        Expression x = unwrap(e);
        if (x instanceof NullConstant) return true;
        if (x instanceof InlineConditional ic) return mayBeNull(ic.ifTrue()) || mayBeNull(ic.ifFalse());
        return KotlinNullability.nullableInKotlin(x);
    }

    private static boolean returnsNull(Lambda l) {
        boolean[] found = {false};
        l.methodBody().visit((io.codelaser.maddi.cst.api.element.Element e) -> {
            if (e instanceof ReturnStatement rs && !rs.hasNoValue() && mayBeNull(rs.expression())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    /**
     * A key extractor of {@code Comparator.comparingInt(o -> o.id)} that sorts nullable elements: Kotlin infers its
     * parameter from the list, {@code Collections.sort(sorted, …)} with a {@code MutableList<Statement?>}, and Java
     * throws where it dereferences the null.
     */
    private static boolean sortsNullable(io.codelaser.maddi.cst.api.info.MethodInfo method) {
        if (method == null || !"java.util.Comparator".equals(method.typeInfo().fullyQualifiedName())
            || !method.name().startsWith("comparing") && !method.name().startsWith("thenComparing")) {
            return false;
        }
        if (!(KotlinContext.enclosingCall() instanceof MethodCall outer)) return false;
        String owner = outer.methodInfo().typeInfo().fullyQualifiedName();
        Expression sorted = switch (outer.methodInfo().name()) {
            case "sort" -> "java.util.Collections".equals(owner) && !outer.parameterExpressions().isEmpty()
                    ? outer.parameterExpressions().getFirst() : outer.objectIsImplicit() ? null : outer.object();
            case "sorted", "min", "max" -> outer.objectIsImplicit() ? null : outer.object();
            default -> null;
        };
        ParameterizedType t = sorted == null ? null : KotlinNullability.kotlinType(sorted);
        return t != null && t.parameters().size() == 1 && KotlinNullability.isNullable(t.parameters().getFirst());
    }

    /**
     * A lambda of {@code Optional.map}, {@code filter}, {@code flatMap}, {@code ifPresent} on an Optional whose
     * Kotlin type argument is nullable (a map lambda before it returned null, Java's empty): Java never calls it with
     * null, Kotlin types its parameter nullable. The {@code !!} on its dereferences cannot throw.
     */
    private static boolean optionalOfNullableValue(io.codelaser.maddi.cst.api.info.MethodInfo method, MethodCall call) {
        return method != null && "java.util.Optional".equals(method.typeInfo().fullyQualifiedName())
               && java.util.Set.of("map", "flatMap", "filter", "ifPresent", "ifPresentOrElse").contains(method.name())
               && !call.objectIsImplicit() && unwrap(call.object()) instanceof MethodCall receiver
               && nullableInChain(receiver);
    }

    private static final java.util.Set<String> LOOKUPS = java.util.Set.of("get", "getOrDefault", "containsKey",
            "containsValue", "contains", "indexOf", "lastIndexOf", "remove");

    /**
     * The {@code Object} parameter of a lookup on a Java collection or map that Kotlin maps to its own: Kotlin's member
     * takes the element or key type, its stdlib extension of the same name ({@code Map<out K, V>.get(key: K)},
     * {@code Iterable<T>.contains(element: T)}, …) a nullable one too, and returns what Java's returns for a null.
     * <p>
     * The extension infers its T from its inputs alone: {@code set.contains(exit)} with a {@code BasicBlockStatement?}
     * into a {@code Set<Statement>} has none to infer from, and the argument is cast to {@code Statement?}.
     *
     * @return the element (or key) type; null when this is no such lookup, or the argument is not of a subtype
     */
    private static ParameterizedType nullableLookup(io.codelaser.maddi.cst.api.info.MethodInfo method, MethodCall call,
                                          Expression argument, int i) {
        if (!LOOKUPS.contains(method.name()) || !method.typeInfo().fullyQualifiedName().startsWith("java.util.")
            || "remove".equals(method.name()) && method.parameters().size() != 1
            || call.object() == null || call.objectIsImplicit()) {
            return null;
        }
        ParameterizedType p = method.parameters().get(i).parameterizedType();
        if (!p.isJavaLangObject() || p.arrays() != 0 || i != 0) return null;
        ParameterizedType element = lookedUp(call.object().parameterizedType(), method.typeInfo(),
                "containsValue".equals(method.name()) ? 1 : 0);
        if (element != null && element.wildcard() != null && element.wildcard().isSuper()) {
            // lst.remove(post) on a MutableList<in Statement?>: the member takes the bound, when that is nullable
            ParameterizedType kotlin = lookedUp(KotlinNullability.kotlinType(call.object()), method.typeInfo(),
                    "containsValue".equals(method.name()) ? 1 : 0);
            element = kotlin != null && kotlin.wildcard() != null && kotlin.wildcard().isSuper()
                      && KotlinNullability.isNullable(kotlin) ? kotlin.withWildcard(null) : null;
        }
        ParameterizedType argumentType = argument.parameterizedType();
        boolean found = element != null && argumentType != null && element.arrays() == argumentType.arrays()
                        && element.wildcard() == null && element.typeInfo() != null
                        && (element.typeInfo() == argumentType.typeInfo()
                            || element.arrays() == 0 && element.parameters().isEmpty()
                               && isSubtype(argumentType.typeInfo(), element.typeInfo(), new java.util.HashSet<>()));
        return found ? element : null;
    }

    private static boolean isSubtype(TypeInfo sub, TypeInfo sup, java.util.Set<TypeInfo> visited) {
        if (sub == null || !visited.add(sub)) return false;
        if (sub == sup) return true;
        if (sub.parentClass() != null && isSubtype(sub.parentClass().typeInfo(), sup, visited)) return true;
        return sub.interfacesImplemented().stream().anyMatch(i -> isSubtype(i.typeInfo(), sup, visited));
    }

    /**
     * A collection parameter of a translated method where an overload of the same arity takes {@code Object}: the
     * argument may not be exactly Kotlin's invariant {@code MutableMap<K, V>}, and Kotlin then picks the other.
     */
    private static boolean collectionBesideObject(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (method == null || i >= method.parameters().size() || !KotlinNullability.translated(method.typeInfo())) {
            return false;
        }
        ParameterizedType p = method.parameters().get(i).parameterizedType();
        if (p.arrays() > 0 || p.typeParameter() != null || method.parameters().get(i).isVarArgs()
            || KotlinTypeName.readOnly(p, null).equals(KotlinTypeName.of(p, null))) {
            return false;
        }
        List<io.codelaser.maddi.cst.api.info.MethodInfo> candidates = method.isConstructor()
                ? method.typeInfo().constructors() : method.typeInfo().methods();
        return candidates.stream().anyMatch(m -> m != method && m.name().equals(method.name())
                && m.parameters().size() == method.parameters().size()
                && m.parameters().get(i).parameterizedType().isJavaLangObject());
    }

    private static final String MOCKITO_MATCHED = "mockitoMatched";
    private static final String MOCKITO_MATCHED_DECLARATION = """
            @Suppress("UNCHECKED_CAST")
            private fun <T> mockitoMatched(value: T?): T = value as T""";
    private static final java.util.Set<String> MOCKITO_MATCHER_TYPES = java.util.Set.of("org.mockito.ArgumentMatchers",
            "org.mockito.AdditionalMatchers", "org.mockito.hamcrest.MockitoHamcrest");

    /**
     * A Mockito matcher as an argument, {@code any()}, {@code eq(x)}, {@code argThat(…)}, {@code captor.capture()}:
     * it registers the matcher and returns null (or 0, for a primitive), a value Kotlin would check against a
     * non-null parameter.
     */
    private static boolean mockitoMatcher(Expression arg) {
        if (!(unwrap(arg) instanceof MethodCall mc) || mc.methodInfo() == null) return false;
        io.codelaser.maddi.cst.api.info.MethodInfo m = mc.methodInfo();
        if (m.returnType() == null || m.returnType().isPrimitiveExcludingVoid()) return false;
        String owner = m.typeInfo().fullyQualifiedName();
        return MOCKITO_MATCHER_TYPES.contains(owner)
               || "org.mockito.ArgumentCaptor".equals(owner) && "capture".equals(m.name());
    }

    /**
     * A boxed argument ({@code Integer}) into a reference parameter ({@code Object}) where an overload takes the
     * primitive at that position: Kotlin's type for both is {@code Int}, and it picks the primitive overload.
     */
    private static boolean boxedIntoReference(io.codelaser.maddi.cst.api.info.MethodInfo method, int i, Expression arg) {
        if (method == null || i >= method.parameters().size()) return false;
        ParameterizedType p = method.parameters().get(i).parameterizedType();
        ParameterizedType a = arg.parameterizedType();
        if (p == null || a == null || p.isPrimitiveExcludingVoid() || p.arrays() > 0 || method.parameters().get(i).isVarArgs()
            || a.isPrimitiveExcludingVoid() || primitive(a) == null || primitive(p) != null) {
            return false;
        }
        Primitive boxed = primitive(a);
        List<io.codelaser.maddi.cst.api.info.MethodInfo> candidates = method.isConstructor()
                ? method.typeInfo().constructors() : method.typeInfo().methods();
        return candidates.stream().anyMatch(m -> m != method && m.name().equals(method.name())
                && m.parameters().size() == method.parameters().size()
                && m.parameters().get(i).parameterizedType().isPrimitiveExcludingVoid()
                && primitive(m.parameters().get(i).parameterizedType()) == boxed);
    }

    /**
     * A nullable parameter of a method without varargs, where an overload starts with the same types, non-null, and
     * ends in varargs: Kotlin finds that one more specific for a non-null argument, and calls it without varargs.
     */
    private static boolean nullableBesideVarargs(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (method == null || i >= method.parameters().size() || method.parameters().getLast().isVarArgs()) return false;
        ParameterizedType declared = KotlinNullability.parameterType(method.parameters().get(i));
        if (declared == null || declared.isPrimitiveExcludingVoid() || !KotlinNullability.isNullable(declared)) return false;
        int n = method.parameters().size();
        List<io.codelaser.maddi.cst.api.info.MethodInfo> candidates = method.isConstructor()
                ? method.typeInfo().constructors() : method.typeInfo().methods();
        return candidates.stream().anyMatch(m -> m != method && m.name().equals(method.name())
                && m.parameters().size() == n + 1 && m.parameters().getLast().isVarArgs()
                && java.util.stream.IntStream.range(0, n).allMatch(j -> {
                    ParameterizedType mine = method.parameters().get(j).parameterizedType();
                    ParameterizedType theirs = m.parameters().get(j).parameterizedType();
                    return mine.typeInfo() != null && mine.typeInfo() == theirs.typeInfo() && mine.arrays() == theirs.arrays();
                }));
    }

    /** The receiver's type argument {@code index} as the declaring type sees it: E of a Set<E>, K or V of a Map. */
    private static ParameterizedType lookedUp(ParameterizedType receiver, TypeInfo declaring, int index) {
        if (receiver == null || receiver.typeInfo() == null) return null;
        try {
            ParameterizedType seen = receiver.typeInfo() == declaring ? receiver
                    : receiver.concreteSuperType(declaring.asParameterizedType());
            return seen == null || index >= seen.parameters().size() ? null : seen.parameters().get(index);
        } catch (RuntimeException | AssertionError e) {
            return null;
        }
    }

    /**
     * The array that Java passes as the varargs themselves: Kotlin takes it spread, {@code *names}. That is the last
     * argument in the varargs position, with as many array dimensions as the parameter; not {@code null}.
     */
    private static boolean spread(List<Expression> args, io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (method == null || method.parameters().isEmpty() || i != args.size() - 1
            || i != method.parameters().size() - 1) {
            return false;
        }
        ParameterInfo varargs = method.parameters().getLast();
        ParameterizedType type = args.get(i).parameterizedType();
        return varargs.isVarArgs() && type != null && !(unwrap(args.get(i)) instanceof NullConstant)
               && type.arrays() == varargs.parameterizedType().arrays();
    }

    /**
     * {@code e}, converted to {@code target} when Java widens it implicitly and Kotlin does not: {@code s.toInt()}
     * for a short where an int is expected, {@code i.toLong()}, {@code c.code}. A Kotlin integer literal adapts to an
     * integral type by itself.
     */
    static OutputBuilder widened(Expression e, ParameterizedType target, Qualification q) {
        if (unwrap(e) instanceof Cast c && KotlinNullability.isNullable(target) && KotlinNullability.nullableCast(c)) {
            // String factory = (String) properties.get(key): Java's cast lets the null through
            return cast(c, true, q);
        }
        if (e instanceof ArrayInitializer ai && target != null && target.arrays() > 0) {
            // {…} takes its type from what it initializes: its elements may all be null, or arrays themselves
            return arrayInitializer(ai, target, q);
        }
        if (e instanceof InlineConditional ic && target != null && !isElvis(ic)) {
            // each branch converted to what the whole initializes: if (m != null) ArrayList<Pair?>(m) else null
            return new OutputBuilderImpl().add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(print(ic.condition(), q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                    .add(branch(ic.ifTrue(), () -> widened(ic.ifTrue(), target, q), q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.ELSE).add(SpaceEnum.ONE)
                    .add(branch(ic.ifFalse(), () -> widened(ic.ifFalse(), target, q), q));
        }
        if (unwrap(e) instanceof ConstructorCall cc && target != null && cc.anonymousClass() == null) {
            ParameterizedType withStates = withTargetStates(cc.parameterizedType(), target);
            if (withStates != null) return inCall(cc, () -> constructorCall(cc, withStates, q));
        }
        if (unwrap(e) instanceof ConstructorCall cc && cc.arrayInitializer() == null && cc.anonymousClass() == null
            && target != null && target.arrays() > 0 && cc.parameterizedType().arrays() == target.arrays()
            && cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).count() == 1 && nonNullElements(target)) {
            // Offsets[] offsets = new Offsets[n], filled before it is read: arrayOfNulls is an Array<Offsets?>, and the
            // declaration's elements are non-null. Erased, the cast checks nothing: a slot read before it is filled
            // fails later, as Java's null does
            ParameterizedType created = rawOf(cc.parameterizedType(), target) ? target : cc.parameterizedType();
            KotlinContext.message(KotlinPrintMessage.Code.UNCHECKED_CAST, cc, KotlinContext.describe(cc));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(inCall(cc, () -> constructorCall(cc, created, q))).add(SpaceEnum.ONE).add(KotlinKeyword.AS)
                    .add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(target, q))).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (unwrap(e) instanceof ConstructorCall cc && cc.arrayInitializer() == null && cc.anonymousClass() == null
            && target != null && cc.parameterizedType().arrays() > 0 && rawOf(cc.parameterizedType(), target)) {
            // new FastSparseSet[n][]: Kotlin's arrays are invariant, so the elements are the declaration's, <Int> not <*>
            return inCall(cc, () -> constructorCall(cc, target, q));
        }
        if (unwrap(e) instanceof ConstructorCall cc && cc.arrayInitializer() != null && target != null
            && target.arrays() == cc.parameterizedType().arrays()) {
            // new Object[]{…}: as {…}, with the element state of what it initializes (Array<Any?>)
            return arrayInitializer(cc.arrayInitializer(), target, q);
        }
        if (optionalOfNullable(e, target)) {
            KotlinContext.message(KotlinPrintMessage.Code.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(operand(PrecedenceEnum.CAST, e, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target, q))).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (target != null && !(unwrap(e) instanceof ConstructorCall) && !(unwrap(e) instanceof NullConstant)
            && rawOf(rawValueType(e), target)) {
            // FastSparseSet<Integer>[] empty = FastSparseSet.EMPTY_ARRAY: Java's unchecked conversion is Kotlin's
            // unchecked cast; an Array<FastSparseSet<*>?> is no Array<FastSparseSet<Int>?>
            KotlinContext.message(KotlinPrintMessage.Code.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(operand(PrecedenceEnum.CAST, e, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target, q))).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        Primitive to = target == null ? null : primitive(target);
        Primitive from = primitive(e.parameterizedType());
        if (to == null || from == null || from == to || from == Primitive.BOOLEAN || to == Primitive.BOOLEAN
            || to == Primitive.CHAR || rank(to) < rank(from)) {
            return print(e, q);
        }
        if (unwrap(e) instanceof IntConstant && to != Primitive.FLOAT && to != Primitive.DOUBLE) return print(e, q);
        OutputBuilder receiver = receiver(e, q);
        for (String call : conversion(from, to).split("\\.")) {
            if (!call.isEmpty()) receiver.add(SymbolEnum.DOT).add(new TextImpl(call));
        }
        return receiver;
    }

    /**
     * {@code ArrayList<BasicBlock?>()} where a {@code MutableList<BasicBlock?>} is initialized: the target's arguments,
     * when they are the constructed type's arguments up to their states at any depth (same number, same types:
     * {@code ArrayList<E>} for {@code List<E>}, {@code HashMap<K, V>} for {@code Map<K, V>}). Kotlin's generics are
     * invariant, so {@code ArrayList<BasicBlock>} is no {@code MutableList<BasicBlock?>}, and neither is
     * {@code ArrayList<MutableList<Exprent>>} a {@code MutableList<MutableList<Exprent?>>} (fernflower: 25 initializers).
     * Null when there is nothing to change.
     */
    private static ParameterizedType withTargetStates(ParameterizedType constructed, ParameterizedType target) {
        if (constructed.arrays() > 0 || target.arrays() > 0 || constructed.parameters().isEmpty()
            || constructed.parameters().size() != target.parameters().size()) {
            return null;
        }
        boolean changed = false;
        for (int i = 0; i < constructed.parameters().size(); i++) {
            ParameterizedType mine = constructed.parameters().get(i);
            ParameterizedType theirs = target.parameters().get(i);
            if (!sameUpToStates(mine, theirs)) return null;
            changed |= !mine.equals(theirs);
        }
        return changed ? constructed.withParameters(target.parameters()) : null;
    }

    /** The same type, ignoring the nullable states of it and its arguments; a wildcard is never the same. */
    private static boolean sameUpToStates(ParameterizedType a, ParameterizedType b) {
        if (a == null || b == null || a.wildcard() != null || b.wildcard() != null
            || !java.util.Objects.equals(a.typeInfo(), b.typeInfo()) || a.arrays() != b.arrays()
            || !java.util.Objects.equals(a.typeParameter(), b.typeParameter())
            || a.parameters().size() != b.parameters().size()) {
            return false;
        }
        for (int i = 0; i < a.parameters().size(); i++) {
            if (!sameUpToStates(a.parameters().get(i), b.parameters().get(i))) return false;
        }
        return true;
    }

    /** An array of references whose elements the declaration says are non-null: arrayOfNulls would make them X?. */
    private static boolean nonNullElements(ParameterizedType array) {
        ParameterizedType component = array.componentType();
        return component != null && KotlinTypeName.primitiveArray(component) == null
               && !KotlinNullability.isNullable(component) && component.wildcard() == null && !component.isTypeParameter()
               && (component.typeInfo() != null || component.arrays() > 0);
    }

    /** The type Kotlin sees for {@code e}, where the printer knows it (a local typed from its initializer is not raw). */
    private static ParameterizedType rawValueType(Expression e) {
        ParameterizedType kotlin = KotlinNullability.kotlinType(e);
        return kotlin != null ? kotlin : e.parameterizedType();
    }

    /** {@code raw} is {@code typed} without its type arguments: the same type, the same arrays, no arguments written. */
    static boolean rawOf(ParameterizedType raw, ParameterizedType typed) {
        if (raw == null || typed == null || raw.arrays() != typed.arrays() || raw.typeInfo() == null
            || raw.typeInfo() != typed.typeInfo() || raw.typeInfo().typeParameters().isEmpty()) {
            return false;
        }
        // Class<?> is not raw, and Class.forName's Class<*> is nothing to adopt
        return raw.parameters().isEmpty() && !typed.parameters().isEmpty()
               && typed.parameters().stream().noneMatch(p -> p.wildcard() != null || p.isTypeParameter() && p.typeParameter() == null);
    }

    private static int rank(Primitive p) {
        return switch (p) {
            case BYTE -> 1;
            case SHORT, CHAR -> 2;
            case INT -> 3;
            case LONG -> 4;
            case FLOAT -> 5;
            case DOUBLE -> 6;
            case BOOLEAN -> 0;
        };
    }

    private static OutputBuilder methodReference(MethodReference mr, Qualification q) {
        Expression scope = mr.scope();
        if (mr.methodInfo().isConstructor()) {
            ParameterizedType type = scope.parameterizedType();
            String name = type.arrays() > 0 ? KotlinTypeName.of(type, q) : KotlinTypeName.name(type.typeInfo(), q);
            if (type.arrays() == 0 && name.contains(".") && mr.methodInfo().parameters().size() <= 2) {
                // ::java.util.ArrayList is no reference Kotlin can parse
                return text(switch (mr.methodInfo().parameters().size()) {
                    case 0 -> "{ " + name + "() }";
                    case 1 -> "{ " + name + "(it) }";
                    default -> "{ a, b -> " + name + "(a, b) }";
                });
            }
            return text("::" + name);
        }
        String member = mappedMember(mr);
        if (member != null) return text("{ it" + member + " }");
        io.codelaser.maddi.cst.api.info.MethodInfo m = mr.methodInfo();
        if (scope instanceof TypeExpression te && m.isStatic() && (!m.isVarargs() || m.parameters().size() == 1)
            && KotlinTypeName.isMapped(te.parameterizedType().typeInfo().fullyQualifiedName()) && m.parameters().size() <= 2) {
            // java.util.List::of: Kotlin wants the mapped type's arguments (List<E>) and has no static to refer to;
            // List.of(E...) takes the array it is handed
            String call = KotlinTypeName.staticOwner(te.parameterizedType().typeInfo(), q) + "." + KotlinNames.name(m.name());
            return text(switch (m.parameters().size()) {
                case 0 -> "{ " + call + "() }";
                case 1 -> "{ " + call + (m.isVarargs() ? "(*it) }" : "(it) }");
                default -> "{ a, b -> " + call + "(a, b) }";
            });
        }
        if (scope instanceof TypeExpression && !m.isStatic() && m.parameters().isEmpty()
            && m.typeInfo().fields().stream().anyMatch(f -> f.name().equals(m.name()))) {
            // Failure::retry where Failure has a field retry too: Kotlin finds the property and the function
            return text("{ it." + KotlinNames.name(m.name()) + "() }");
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (scope instanceof TypeExpression te) {
            TypeInfo owner = te.parameterizedType().typeInfo();
            b.add(new TextImpl(mr.methodInfo().isStatic() ? KotlinTypeName.staticOwner(owner, q) : KotlinTypeName.name(owner, q)));
        } else {
            b.add(receiver(scope, q));
        }
        return b.add(new TextImpl("::" + KotlinNames.name(mr.methodInfo().name())));
    }

    private static boolean toArrayOf(io.codelaser.maddi.cst.api.info.MethodInfo m) {
        return m != null && "toArray".equals(m.name()) && m.parameters().size() == 1
               && m.parameters().getFirst().parameterizedType().arrays() == 1
               && m.typeInfo().fullyQualifiedName().startsWith("java.util.");
    }

    private static boolean rawComparable(ParameterizedType type) {
        return type.typeInfo() != null && "java.lang.Comparable".equals(type.typeInfo().fullyQualifiedName())
               && type.parameters().isEmpty() && type.arrays() == 0;
    }

    /**
     * {@code Map.Entry::getValue}: Kotlin has a property {@code value} and no function to refer to, so the reference
     * becomes the lambda {@code { it.value }}. So does a reference through a raw type, {@code Collection::stream},
     * where {@code MutableCollection<*>::stream} would lose the element type. The member's text after {@code it}, or
     * null when the reference stays one.
     */
    private static String mappedMember(MethodReference mr) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mr.methodInfo();
        if (!(mr.scope() instanceof TypeExpression te) || m.isStatic() || !m.parameters().isEmpty()) return null;
        if (KotlinMappedMembers.isUnboxing(m)) return "";
        String property = KotlinMappedMembers.property(m);
        if (property != null) return "." + property;
        String function = KotlinMappedMembers.function(m);
        if (function == null) function = KotlinMappedMembers.renamed(m);
        if (function != null) return "." + function + "()";
        ParameterizedType type = te.parameterizedType();
        boolean raw = type.typeInfo() != null && type.parameters().isEmpty() && !type.typeInfo().typeParameters().isEmpty();
        return raw && KotlinContext.translatingJava() ? "." + KotlinNames.name(m.name()) + "()" : null;
    }

    /**
     * {@code Foo(a)} for {@code new Foo(a)}; {@code outer.Inner()} for {@code outer.new Inner()}; an anonymous class
     * becomes an object expression; arrays become their Kotlin factory calls.
     */
    private static OutputBuilder constructorCall(ConstructorCall cc, ParameterizedType type, Qualification q) {
        if (cc.anonymousClass() != null) return anonymousClass(cc, q);
        if (type.arrays() > 0) {
            if (cc.arrayInitializer() != null) return arrayInitializer(cc.arrayInitializer(), type, q);
            List<Expression> dimensions = cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).toList();
            return arrayCreation(type, dimensions, 0, q);
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (cc.object() != null) b.add(receiver(cc.object(), q)).add(SymbolEnum.DOT);
        return b.add(new TextImpl(KotlinTypeName.constructed(type, q)))
                .add(arguments(cc.parameterExpressions(), cc.constructor(), q));
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
        ParameterizedType element = type.componentType();
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
        // the element type with its own nullability: arrayOf<IntArray?>(null, intArrayOf(1))
        ParameterizedType element = type != null && type.arrays() > 0 ? type.componentType() : null;
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
                .map(x -> x instanceof ArrayInitializer nested ? arrayInitializer(nested, element, q)
                        // an element into a non-null element type is asserted, as a write a[i] = x is
                        : KotlinNullability.toTarget(x, element, true, print(x, q), q))
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
    /**
     * The interface a Java lambda implements, as a SAM constructor in front of it: {@code Runnable { … }}, where the
     * lambda is not an argument. Kotlin converts a lambda argument to the parameter's Java interface or
     * {@code fun interface} by itself; anywhere else ({@code val r: Runnable = { … }}) it needs the constructor,
     * which also gives the lambda's parameters their types. Not with type
     * projections, which a constructor call cannot take, and not for a lambda parsed from Kotlin.
     */
    private static String samConstructor(Lambda lambda, Qualification q) {
        if (!KotlinContext.translatingJava()) return null;
        if (lambda.methodInfo().typeInfo().interfacesImplemented().isEmpty()) return null;
        ParameterizedType type = lambda.concreteFunctionalType();
        if (type == null || type.typeInfo() == null || !type.typeInfo().isInterface()
            || type.parameters().stream().anyMatch(p -> p.wildcard() != null)) {
            return null;
        }
        return KotlinTypeName.of(type, q);
    }

    private static OutputBuilder lambda(Lambda lambda, Qualification q) {
        return lambda(lambda, true, q);
    }

    private static OutputBuilder lambda(Lambda lambda, boolean samConstructor, Qualification q) {
        List<ParameterInfo> params = lambda.parameters();
        params.forEach(p -> KotlinContext.declared(p.name()));
        Block body = lambda.methodBody();
        List<Statement> statements = body.statements().stream().filter(s -> !s.isSynthetic()).toList();

        OutputBuilder inner;
        boolean labelled = false;
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.LAMBDA, null, KotlinContext.LAMBDA_LABEL));
        KotlinContext.pushScope(body);
        try {
            // a parameter the body assigns: `var p = p` first, as in a method (a Kotlin lambda parameter is a val)
            List<OutputBuilder> reassigned = KotlinStatementPrinter.reassignedParameters(params, body, q);
            if (reassigned.isEmpty() && statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs
                && !rs.hasNoValue()) {
                ParameterizedType returnType = lambda.methodInfo().returnType();
                if (unwrap(rs.expression()) instanceof ConstructorCall cc && inferred(cc)) {
                    inner = inferredConstructorCall(cc, q);
                } else if (primitive(returnType) != null && !(unwrap(rs.expression()) instanceof Assignment)) {
                    // thenComparingLong { o -> if (c) o.intValue() else Long.MIN_VALUE }: Java widens to the
                    // functional interface's long, branch by branch; Kotlin's branches stay Int and Long
                    inner = widened(rs.expression(), returnType, q);
                } else {
                    inner = printStatement(rs.expression(), q);
                }
            } else {
                labelled = hasInnerReturn(statements);
                OutputBuilder lines = KotlinStatementPrinter.lambdaBody(statements, q);
                inner = reassigned.isEmpty() ? lines : java.util.stream.Stream.concat(reassigned.stream(),
                        java.util.stream.Stream.of(lines)).filter(o -> !o.isEmpty())
                        .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
            }
        } finally {
            KotlinContext.popScope();
            KotlinContext.pop();
        }
        OutputBuilder b = new OutputBuilderImpl();
        String sam = samConstructor ? samConstructor(lambda, q) : null;
        if (sam != null) b.add(new TextImpl(sam)).add(SpaceEnum.ONE);
        if (labelled) b.add(new TextImpl(KotlinContext.LAMBDA_LABEL + "@"));
        b.add(SymbolEnum.LEFT_BRACE);
        if (!params.isEmpty()) {
            b.add(SpaceEnum.ONE).add(params.stream()
                    .map(p -> new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(p.name()))))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA))).add(SpaceEnum.ONE).add(SymbolEnum.LAMBDA);
        }
        return b.add(SpaceEnum.ONE).add(inner).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    /**
     * A diamond {@code new HashMap<>()} as a lambda's result: Kotlin infers the arguments from what the lambda must
     * return, the receiver's states included ({@code computeIfAbsent(k) { HashMap() }}); written out, they lose them.
     */
    private static boolean inferred(ConstructorCall cc) {
        return cc.diamond() != null && cc.diamond().isYes() && cc.anonymousClass() == null
               && cc.parameterizedType().arrays() == 0 && cc.parameterizedType().typeInfo() != null;
    }

    private static OutputBuilder inferredConstructorCall(ConstructorCall cc, Qualification q) {
        return inCall(cc, () -> {
            OutputBuilder b = new OutputBuilderImpl();
            if (cc.object() != null) b.add(receiver(cc.object(), q)).add(SymbolEnum.DOT);
            return b.add(new TextImpl(KotlinTypeName.name(cc.parameterizedType().typeInfo(), q)))
                    .add(arguments(cc.parameterExpressions(), cc.constructor(), q));
        });
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
        if (!"=".equals(op)) return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment(op)).add(print(a.value(), q));
        ParameterizedType targetType = declaredType(a.variableTarget());
        boolean translated = !(a.variableTarget() instanceof FieldReference fr) || KotlinNullability.translated(fr.fieldInfo().owner());
        return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment(op))
                .add(KotlinNullability.toTarget(a.value(), targetType, translated,
                        widened(a.value(), targetType != null ? targetType : a.variableTarget().parameterizedType(), q), q));
    }

    /** The type a variable was declared with in Kotlin: the nullability verdict's, where there is one. */
    private static ParameterizedType declaredType(Variable v) {
        return switch (v) {
            // an element written: the array's element type, with its state (slots[0] = map.get(k)!!)
            case DependentVariable dv -> {
                ParameterizedType array = KotlinNullability.kotlinType(dv.arrayExpression());
                yield array != null && array.arrays() > 0 ? array.componentType() : v.parameterizedType();
            }
            case FieldReference fr -> KotlinNullability.fieldType(fr.fieldInfo());
            case ParameterInfo pi -> KotlinNullability.parameterType(pi);
            case io.codelaser.maddi.cst.api.variable.LocalVariable lv -> {
                ParameterizedType t = KotlinContext.localType(lv.simpleName());
                yield t != null ? t : lv.parameterizedType();
            }
            default -> v.parameterizedType();
        };
    }

    /**
     * A branch of {@code if … else}: an assignment to a local as {@code { counter = 0; counter }}, a block's value, not
     * {@code 0.also { counter = it }}. A local that a lambda assigns gets no smart casts anywhere, also not in the
     * other branch's {@code ++counter} (fernflower's {@code counter == null ? counter = 0 : ++counter}).
     */
    private static OutputBuilder branch(Expression e, java.util.function.Supplier<OutputBuilder> otherwise, Qualification q) {
        if (unwrap(e) instanceof Assignment a && a.prefixPrimitiveOperator() == null
            && (a.assignmentOperator() == null || "=".equals(a.assignmentOperator().name()))
            && a.variableTarget() instanceof io.codelaser.maddi.cst.api.variable.LocalVariable) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SpaceEnum.ONE).add(assignmentAsStatement(a, q))
                    .add(SymbolEnum.SEMICOLON).add(SpaceEnum.ONE).add(variable(a.variableTarget(), q)).add(SpaceEnum.ONE)
                    .add(SymbolEnum.RIGHT_BRACE);
        }
        return otherwise.get();
    }

    /** {@code value.also { target = it }}: the assignment happens, once, and the expression is its value. */
    private static OutputBuilder assignmentAsValue(Assignment a, Qualification q) {
        if (a.prefixPrimitiveOperator() != null) return assignmentAsStatement(a, q); // ++i and i++ are expressions
        OutputBuilder value;
        String op = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        if ("=".equals(op) && unwrap(a.value()) instanceof ConstructorCall cc && cc.anonymousClass() == null
            && cc.parameterizedType().arrays() == 0) {
            // ArrayList<Int?>().also { lst = it }: the constructed type takes the target's states, as an assignment's
            value = widened(a.value(), declaredType(a.variableTarget()), q);
        } else if ("=".equals(op)) {
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
        if ("==".equals(op) || "!=".equals(op)) {
            // Kotlin compares numbers of one type only: the narrower side widens, as Java's does implicitly
            Primitive l = primitive(bo.lhs().parameterizedType());
            Primitive r = primitive(bo.rhs().parameterizedType());
            if (l != null && r != null && l != r && l != Primitive.BOOLEAN && r != Primitive.BOOLEAN) {
                boolean widenLeft = rank(l) < rank(r) || l == Primitive.CHAR;
                OutputBuilder left = widenLeft ? compared(bo.lhs(), bo.rhs().parameterizedType(), q) : operand(bo.precedence(), bo.lhs(), q);
                OutputBuilder right = widenLeft ? operand(bo.precedence(), bo.rhs(), q) : compared(bo.rhs(), bo.lhs().parameterizedType(), q);
                return new OutputBuilderImpl().add(left).add(KotlinSymbols.binary(op)).add(right);
            }
        }
        ParameterizedType lhsType = bo.lhs().parameterizedType();
        if ("+".equals(op) && bo.parameterizedType() != null && bo.parameterizedType().isJavaLangString()
            && lhsType != null && !lhsType.isJavaLangString()) {
            // 1 + "a": Kotlin's + takes its meaning from the left operand, so a non-String left side converts
            return new OutputBuilderImpl().add(receiver(bo.lhs(), q)).add(SymbolEnum.DOT).add(new TextImpl("toString"))
                    .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS).add(KotlinSymbols.binary(op))
                    .add(operand(bo.precedence(), bo.rhs(), q));
        }
        if (ARITHMETIC.contains(op) && primitive(bo.parameterizedType()) != null
            && primitive(bo.parameterizedType()) != Primitive.CHAR) {
            // Java promotes a char operand to int; Kotlin's Char + Int is a Char, and Int + Char does not exist
            return new OutputBuilderImpl().add(promoted(bo.lhs(), bo.precedence(), q)).add(KotlinSymbols.binary(op))
                    .add(promoted(bo.rhs(), bo.precedence(), q));
        }
        if (RELATIONAL.contains(op) && (primitive(bo.lhs().parameterizedType()) != null
                                        || primitive(bo.rhs().parameterizedType()) != null)) {
            // numbers only: Kotlin's String? + String is fine, and prints "null" as Java does
            return new OutputBuilderImpl().add(numericOperand(bo.precedence(), bo.lhs(), q))
                    .add(KotlinSymbols.binary(op)).add(numericOperand(bo.precedence(), bo.rhs(), q));
        }
        return new OutputBuilderImpl()
                .add(operand(bo.precedence(), bo.lhs(), q))
                .add(KotlinSymbols.binary(op))
                .add(operand(bo.precedence(), bo.rhs(), q));
    }

    private static final java.util.Set<String> RELATIONAL = java.util.Set.of("<", ">", "<=", ">=", "+", "-", "*", "/", "%");

    /**
     * The narrower side of {@code ==}, widened. An int literal does not adapt to a Long there, as it does in an
     * assignment: {@code x == -1L}.
     */
    private static OutputBuilder compared(Expression e, ParameterizedType target, Qualification q) {
        Expression inner = unwrap(e);
        if (primitive(target) == Primitive.LONG) {
            if (inner instanceof IntConstant ic) return text(ic.constant() + "L");
            if (inner instanceof Negation n && unwrap(n.expression()) instanceof IntConstant ic) {
                return text("-" + ic.constant() + "L");
            }
        }
        return widened(e, target, q);
    }

    private static final java.util.Set<String> ARITHMETIC = java.util.Set.of("+", "-", "*", "/", "%");

    /** An operand of an int (or wider) operation: a char becomes its code. */
    private static OutputBuilder promoted(Expression e, Precedence precedence, Qualification q) {
        if (primitive(e.parameterizedType()) == Primitive.CHAR) {
            return valueOperand(e, q).add(SymbolEnum.DOT).add(new TextImpl("code"));
        }
        return numericOperand(precedence, e, q);
    }

    /**
     * An operand of an arithmetic, comparison or bit operator: Java unboxes it, and throws there when it is null;
     * Kotlin has no operator on a nullable type, so a value Kotlin types nullable is asserted
     * ({@code collinstr.getKey(p)!! + offset}).
     */
    private static OutputBuilder numericOperand(Precedence precedence, Expression e, Qualification q) {
        if (!(unwrap(e) instanceof NullConstant) && KotlinNullability.nullableInKotlin(e)) {
            return KotlinNullability.asserted(e, KotlinPrintMessage.Code.ASSERT_AT_UNBOXING, q);
        }
        return operand(precedence, e, q);
    }

    private static OutputBuilder valueOperand(Expression e, Qualification q) {
        if (!(unwrap(e) instanceof NullConstant) && KotlinNullability.nullableInKotlin(e)) {
            return KotlinNullability.asserted(e, KotlinPrintMessage.Code.ASSERT_AT_UNBOXING, q);
        }
        return receiver(e, q);
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
        return new OutputBuilderImpl().add(valueOperand(lhs, q)).add(KotlinSymbols.binary(function))
               .add(valueOperand(rhs, q));
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

    /** An operand of && or ||: Java unboxes a Boolean there ({@code last && map.get(k)}), Kotlin needs the {@code !!}. */
    private static OutputBuilder condition(io.codelaser.maddi.cst.api.expression.Precedence precedence, Expression e, Qualification q) {
        if (!(unwrap(e) instanceof NullConstant) && KotlinNullability.nullableInKotlin(e)) {
            return KotlinNullability.asserted(e, KotlinPrintMessage.Code.ASSERT_AT_UNBOXING, q);
        }
        return operand(precedence, e, q);
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

    private static OutputBuilder instanceOf(InstanceOf io, Qualification q) {
        registerPattern(io, q);
        return new OutputBuilderImpl().add(operand(io.precedence(), io.expression(), q))
                .add(KotlinSymbols.binary("is")).add(new TextImpl(KotlinTypeName.of(io.testType(), q)));
    }

    /**
     * {@code x instanceof T t}: Kotlin's {@code is} declares nothing, so {@code t} prints as {@code x}, which Kotlin
     * smart-casts to {@code T} where the test holds, when {@code x} is a local variable or a parameter; otherwise as
     * {@code (x as T)}. The cast is safe, but re-evaluates {@code x}: fine for the field reads and getters it meets.
     */
    private static void registerPattern(InstanceOf io, Qualification q) {
        if (io.patternVariable() == null || io.patternVariable().unnamedPattern()
            || io.patternVariable().localVariable() == null) return;
        Expression tested = unwrap(io.expression());
        boolean smartCast = tested instanceof VariableExpression ve
                            && (ve.variable() instanceof io.codelaser.maddi.cst.api.variable.LocalVariable
                                || ve.variable() instanceof ParameterInfo);
        String type = KotlinTypeName.of(io.testType(), q);
        KotlinContext.patternVariable(io.patternVariable().localVariable(), smartCast
                ? () -> print(tested, q)
                : () -> new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(operand(PrecedenceEnum.CAST, tested, q))
                .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl(type))
                .add(SymbolEnum.RIGHT_PARENTHESIS));
    }

    private static OutputBuilder notInstanceOf(InstanceOf io, Qualification q) {
        registerPattern(io, q);
        return new OutputBuilderImpl().add(operand(io.precedence(), io.expression(), q))
                .add(KotlinSymbols.binary("!is")).add(new TextImpl(KotlinTypeName.of(io.testType(), q)));
    }

    // ---------------------------------------------------------------- casts (#105)

    enum Primitive {BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE}

    static Primitive primitive(ParameterizedType pt) {
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
     * functions); from char, through {@code code}. A reference cast of a nullable value into a nullable target is to the
     * nullable type.
     */
    private static OutputBuilder cast(Cast cast, boolean nullable, Qualification q) {
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
        // Java's (String) map.get(k) lets null through, Kotlin's `as String` throws: as String?; and (AiMessage) null,
        // which picks an overload, is always null
        if (unwrap(cast.expression()) instanceof NullConstant) nullable = true;
        ParameterizedType type = nullable
                ? cast.parameterizedType().withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE)
                : cast.parameterizedType();
        String typeName = rawComparable(cast.parameterizedType())
                // ((Comparable) actual).compareTo(expected): Comparable<*> takes nothing, Comparable<Any?> anything,
                // unchecked, as Java's raw type does
                ? "Comparable<Any?>" + (nullable ? "?" : "") : KotlinTypeName.of(type, q);
        return new OutputBuilderImpl().add(operand(cast.precedence(), cast.expression(), q)).add(SpaceEnum.ONE)
                .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                .add(new TextImpl(typeName));
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

    static Expression unwrap(Expression e) {
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
