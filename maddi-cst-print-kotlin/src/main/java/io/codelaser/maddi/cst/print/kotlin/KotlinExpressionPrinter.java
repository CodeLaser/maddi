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

import io.codelaser.maddi.cst.print.translate.Emission;
import io.codelaser.maddi.cst.print.translate.Literals;
import io.codelaser.maddi.cst.print.translate.CstQueries;
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

    private static final Literals LITERALS = new Literals("Double", "Float", "NaN", "POSITIVE_INFINITY",
            "NEGATIVE_INFINITY", Map.of('$', "\\$"));

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
            case InlineConditional ic -> inlineConditional(ic, q);
            // a getter's call whose value a local val holds (KotlinValCopies)
            case MethodCall mc when KotlinSmartCasts.copy(mc) != null ->
                    new OutputBuilderImpl().add(new TextImpl(KotlinNames.name(KotlinSmartCasts.copy(mc).name())));
            case MethodCall mc when equality(mc) != null -> equality(equality(mc), "==", q);
            case MethodCall mc when KotlinCollectionFactories.applies(mc) -> KotlinCollectionFactories.print(mc, q);
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
            case And and -> junction(and.expressions(), and.precedence(), true, q);
            case Or or -> junction(or.expressions(), or.precedence(), false, q);
            case UnaryOperator uo -> unaryOperator(uo, q);
            // ((T) x).m() where x is a T: the cast and its parentheses go
            case EnclosedExpression ee when unwrap(ee.inner()) instanceof Cast c && smartCast(c) -> print(c.expression(), q);
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
            // from the value, never the Java spelling (#49): Kotlin has no octal '017' and no lowercase 'l' suffix
            case IntConstant ic -> text(Integer.toString(ic.constant()));
            case LongConstant lc -> text(lc.constant() + "L");
            case DoubleConstant dc -> text(doubleLiteral(dc.constant()));
            case FloatConstant fc -> text(floatLiteral(fc.constant()));
            case ByteConstant bc -> text(Byte.toString(bc.constant()));
            case ShortConstant sc -> text(Short.toString(sc.constant()));
            case CommaExpression ce -> ce.expressions().stream().map(x -> printStatement(x, q))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, GuideImpl.generatorForBlock()));
            default -> {
                // constants (int, long, boolean, null) and anything unknown: as in Java
                if (!(e instanceof ConstantExpression<?>)) {
                    KotlinContext.message(KotlinPrintCode.JAVA_FALLBACK, e, e.getClass().getSimpleName());
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
                    .add(SymbolEnum.LEFT_BRACKET).add(arrayIndex(dv.indexExpression(), q)).add(SymbolEnum.RIGHT_BRACKET);
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
        String name = KotlinFieldPrinter.name(fr.fieldInfo());
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
        if (object != null && !mc.objectIsImplicit() && expectedElement(mc) == null && (toArrayOf(mc.methodInfo())
                && mc.parameterExpressions().getFirst().parameterizedType() != null
                && mc.parameterExpressions().getFirst().parameterizedType().arrays() == 1
                && mc.parameterExpressions().getFirst().parameterizedType().componentType().typeParameter() != null
            || toArray(mc.methodInfo()) && typeVariableElement(object, mc.methodInfo().typeInfo()))) {
            // delegate().toArray(array) with a T[]: toTypedArray<T>() wants a reified T, and Java fills and returns the
            // array it is handed (guava-testlib checks it). delegate().toArray() on a Collection<E>: no Collection<Any>
            // where E may be nullable. Java's own toArray, through java.util.Collection, whose platform array fits
            return b.add(SymbolEnum.LEFT_PARENTHESIS).add(print(object, q)).add(SpaceEnum.ONE).add(KotlinKeyword.AS)
                    .add(SpaceEnum.ONE).add(new TextImpl("java.util.Collection<*>")).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SymbolEnum.DOT).add(new TextImpl("toArray"))
                    .add(arguments(mc.parameterExpressions(), mc.methodInfo(), q));
        }
        if (KotlinMappedMembers.isJavaStringOnly(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            // s.stripTrailing() -> (s as java.lang.String).stripTrailing()
            return b.add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(KotlinNullability.asserted(object, KotlinPrintCode.ASSERT_AT_DEREFERENCE, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl("java.lang.String"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.DOT).add(new TextImpl(mc.methodInfo().name()))
                    .add(arguments(mc.parameterExpressions(), mc.methodInfo(), q));
        }
        if (KotlinMappedMembers.isUnboxing(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            return KotlinNullability.asserted(object, KotlinPrintCode.ASSERT_AT_UNBOXING, q);
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
            b.add(new TextImpl(KotlinNames.name(mc.methodInfo().name()))); // a data class property
            ParameterizedType result = mc.methodInfo().returnType();
            if (KotlinTypePrinter.isAnnotationElement(mc.methodInfo()) && result.typeInfo() != null
                && "java.lang.Class".equals(result.typeInfo().fullyQualifiedName())) {
                // @JsonSubTypes.Type's Class<?> value() is a KClass<*> property in Kotlin: .java is Java's Class
                if (result.arrays() == 0) b.add(SymbolEnum.DOT).add(new TextImpl("java"));
                else if (result.arrays() == 1) b.add(new TextImpl(".map { it.java }.toTypedArray()"));
            }
            return b;
        }
        if (KotlinMappedMembers.isRemoveAt(mc.methodInfo())) {
            return b.add(new TextImpl("removeAt")).add(arguments(mc.parameterExpressions(), q));
        }
        if (toArrayOf(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            // c.toArray(new X[0]): Kotlin's collections have no toArray(T[]); toTypedArray<X>() types the array as
            // Java's argument does (Kotlin's arrays are invariant, its Collection<out E> is not), with the element
            // state of what it initializes
            ParameterizedType array = mc.parameterExpressions().getFirst().parameterizedType();
            ParameterizedType expected = expectedElement(mc);
            String element = expected != null ? KotlinTypeName.of(expected, q)
                    : array == null || array.arrays() != 1 ? null : KotlinTypeName.of(array.componentType()
                    .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q);
            return b.add(new TextImpl(element == null ? "toTypedArray()" : "toTypedArray<" + element + ">()"));
        }
        if (toArray(mc.methodInfo()) && object != null && !mc.objectIsImplicit()) {
            // c.toArray(): Kotlin's collections have no toArray() either. toTypedArray<Any?>() or <Any>(), as Java's
            // Object[] prints here (a Collection<out E> is a Collection of either): typed by the collection, a local
            // would be an Array<String> where Java declares an Object[]
            ParameterizedType result = KotlinNullability.kotlinType(mc);
            ParameterizedType expected = expectedElement(mc);
            ParameterizedType component = result == null || result.arrays() != 1 ? null : result.componentType();
            if (expected == null && component != null && !KotlinNullability.isNullable(component)
                && nullableElements(object)) {
                // collection.toArray() of a Collection<out Number?> (guava's Ints.toArray): Kotlin's toTypedArray<T>
                // wants a T the elements fit, Java's Object[] holds the nulls too
                component = component.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE);
            }
            String element = expected != null ? KotlinTypeName.of(expected, q)
                    : component == null ? null : KotlinTypeName.of(component, q);
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
        // Iterators.<@Nullable Entry<K, V>>getNext(it, null): the type-use annotation's state, as in a class header
        String typeArguments = !mc.typeArguments().isEmpty() ? mc.typeArguments().stream()
                .map(t -> KotlinTypeName.of(KotlinTypePrinter.declared(t), q)).collect(java.util.stream.Collectors.joining(", ", "<", ">"))
                : comparingKeyCall(mc) ? comparedType(mc, q) : resultOnlyOrLambdaOnly(mc, q);
        b.add(new TextImpl(KotlinNames.name(mc.methodInfo().name()) + (typeArguments == null ? "" : typeArguments)));
        return b.add(arguments(mc.parameterExpressions(), mc.methodInfo(), mc, q));
    }

    /** The method returns a type whose type arguments Kotlin types nullable: {@code Ordering<@Nullable S>}. */
    private static boolean nullableArgument(io.codelaser.maddi.cst.api.info.MethodInfo methodInfo) {
        ParameterizedType verdict = KotlinNullability.returnType(methodInfo);
        ParameterizedType declared = KotlinTypePrinter.declaredArguments(methodInfo.returnType());
        return java.util.stream.Stream.of(verdict, declared).anyMatch(t -> t != null
                && t.parameters().stream().anyMatch(KotlinNullability::isNullable));
    }

    private static String resultOnlyOrLambdaOnly(MethodCall mc, Qualification q) {
        String resultOnly = resultOnlyTypeArguments(mc, q);
        return resultOnly != null ? resultOnly : lambdaOnlyTypeArguments(mc, q);
    }

    /**
     * Type arguments Kotlin cannot infer from a lambda: each type parameter only mentioned by the parameters taking a
     * lambda or method reference, and there as the lambda's input ({@code <F> Ordering<F> onResultOf(Function<F, ? extends T>)} called with
     * {@code Entry::getValue}). Java infers F from the reference's receiver; Kotlin's {@code { it.value }} has no type
     * for {@code it} unless an expected type fixes F, and inside another generic call (guava's
     * {@code Arrays.sort(entries, 0, size, ordering.onResultOf(…))}) none does. Java's inferred arguments, read off the
     * call's concrete result; null when they are not all there.
     */
    private static String lambdaOnlyTypeArguments(MethodCall mc, Qualification q) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mc.methodInfo();
        List<io.codelaser.maddi.cst.api.info.TypeParameter> typeParameters = m.typeParameters();
        if (typeParameters.isEmpty() || !KotlinContext.translatingJava() || m.returnType() == null
            || mc.concreteReturnType() == null) {
            return null;
        }
        List<Expression> args = mc.parameterExpressions();
        // every type parameter so: one a lambda's result fixes, or a plain argument, Kotlin infers better than Java's
        // inferred argument pinned without its states (Collectors.toMap's U into a Map<…, V?>, langchain4j)
        boolean lambdaOnly = typeParameters.stream().allMatch(tp -> {
            boolean input = false;
            for (int i = 0; i < m.parameters().size() && i < args.size(); i++) {
                ParameterizedType p = m.parameters().get(i).parameterizedType();
                if (!CstQueries.mentions(p, tp)) continue;
                Expression arg = unwrap(args.get(i));
                if (!(arg instanceof Lambda) && !(arg instanceof MethodReference)) return false;
                if (lambdaInput(p, tp)) input = true;
            }
            return input;
        });
        if (!lambdaOnly) return null;
        Map<io.codelaser.maddi.cst.api.type.NamedType, ParameterizedType> concrete =
                m.returnType().formalToConcrete(mc.concreteReturnType());
        List<String> printed = new ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.TypeParameter tp : typeParameters) {
            ParameterizedType c = concrete.get(tp);
            if (c == null || c.wildcard() != null || c.typeParameter() == tp) return null;
            printed.add(KotlinTypeName.of(c, q));
        }
        return printed.stream().collect(java.util.stream.Collectors.joining(", ", "<", ">"));
    }

    /**
     * {@code tp} is in a type argument of the functional type {@code p} that its single abstract method takes: the
     * type of the lambda's parameter ({@code F} in {@code Function<F, ? extends T>}), not of its result, which Kotlin
     * infers from the body.
     */
    private static boolean lambdaInput(ParameterizedType p, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        TypeInfo functional = p.typeInfo();
        if (functional == null || functional.singleAbstractMethod() == null
            || functional.typeParameters().size() != p.parameters().size()) {
            return false;
        }
        io.codelaser.maddi.cst.api.info.MethodInfo sam = functional.singleAbstractMethod();
        for (int j = 0; j < p.parameters().size(); j++) {
            io.codelaser.maddi.cst.api.info.TypeParameter formal = functional.typeParameters().get(j);
            if (CstQueries.mentions(p.parameters().get(j), tp)
                && sam.parameters().stream().anyMatch(sp -> CstQueries.mentions(sp.parameterizedType(), formal))) {
                return true;
            }
        }
        return false;
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
                .filter(tp -> m.parameters().stream().noneMatch(p -> CstQueries.mentions(p.parameterizedType(), tp))).toList();
        if (resultOnly.isEmpty()) return null;
        boolean bounded = resultOnly.stream().anyMatch(tp -> bound(tp) != null);
        boolean noExpectedType = noExpectedType(mc);
        if (!bounded && !noExpectedType) return null;
        Map<io.codelaser.maddi.cst.api.type.NamedType, ParameterizedType> concrete = mc.concreteReturnType() == null
                ? Map.of() : m.returnType().formalToConcrete(mc.concreteReturnType());
        List<String> printed = new ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.TypeParameter tp : typeParameters) {
            ParameterizedType t = concrete.get(tp);
            if (t == null || t.wildcard() != null || CstQueries.mentionsAny(t, typeParameters)) {
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


    private static OutputBuilder arguments(List<Expression> args, Qualification q) {
        return arguments(args, null, q);
    }

    static OutputBuilder arguments(List<Expression> args, io.codelaser.maddi.cst.api.info.MethodInfo method,
                                   Qualification q) {
        return arguments(args, method, null, q);
    }

    /**
     * The {@code i}-th parameter of {@code method}, a translated method, is nullable where a sibling overload of the
     * same arity takes the same type non-null, and the argument is not nullable: with Java's choice more specific in
     * another parameter ({@code Class<T>} over {@code Type}), Kotlin sees neither candidate as the more specific one.
     */
    private static boolean nullabilityTiesOverload(io.codelaser.maddi.cst.api.info.MethodInfo method, int i,
                                                   Expression argument) {
        if (!KotlinContext.translatingJava() || !KotlinNullability.translated(method.typeInfo())) return false;
        ParameterizedType mine = KotlinNullability.parameterType(method.parameters().get(i));
        ParameterizedType declared = method.parameters().get(i).parameterizedType();
        if (!KotlinNullability.isNullable(mine) || declared.isPrimitiveExcludingVoid()
            || KotlinNullability.nullableInKotlin(argument)) {
            return false;
        }
        List<io.codelaser.maddi.cst.api.info.MethodInfo> siblings = method.isConstructor()
                ? method.typeInfo().constructors()
                : method.typeInfo().methods().stream().filter(m -> m.name().equals(method.name())).toList();
        return siblings.stream().anyMatch(m -> m != method && !m.isSynthetic()
                && m.parameters().size() == method.parameters().size() && !m.parameters().get(i).isVarArgs()
                && m.parameters().get(i).parameterizedType().typeInfo() == declared.typeInfo()
                && m.parameters().get(i).parameterizedType().arrays() == declared.arrays()
                && !KotlinNullability.isNullable(KotlinNullability.parameterType(m.parameters().get(i))));
    }

    /** The result of a call to a member Kotlin sees as Java's: its type arguments are inferred, flexible. */
    private static boolean libraryResult(Expression receiver) {
        return receiver != null && unwrap(receiver) instanceof MethodCall mc && mc.methodInfo() != null
               && !KotlinNullability.translated(mc.methodInfo().typeInfo())
               && !KotlinTypeName.isMapped(mc.methodInfo().typeInfo().fullyQualifiedName());
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
        // A static (java.lang.Integer.toHexString) is called as Java's, with Java's parameter types; List.of still
        // takes no null.
        boolean mappedType = method != null && KotlinTypeName.isMapped(method.typeInfo().fullyQualifiedName());
        boolean mapped = mappedType && !method.isStatic();
        boolean translated = method != null && (mappedType ? !"equals".equals(method.name())
                : KotlinNullability.translated(method.typeInfo()));
        List<OutputBuilder> printed = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            if (call != null && unwrap(args.get(i)) instanceof Lambda lambda) {
                KotlinNullability.inferLambdaParameters(lambda, method, i, call);
            }
            if (spread(args, method, i)) {
                printed.add(new OutputBuilderImpl().add(SPREAD)
                        .add(KotlinNullability.asserted(args.get(i), KotlinPrintCode.ASSERT_INTO_NON_NULL, q)));
                continue;
            }
            boolean hasParameter = method != null && i < method.parameters().size()
                                   && !method.parameters().get(i).isVarArgs();
            ParameterizedType target = !hasParameter || mapped ? null : method.parameters().get(i).parameterizedType();
            String forced = hasParameter && !mapped ? forcedReadOnlyElement(args.get(i), q) : null;
            if (forced != null && !KotlinMethodPrinter.acceptsReadOnly(method, i)) {
                // removeAll(c: Collection<E>) handing c to standardRemoveAll(MutableCollection<*>): Kotlin's override
                // signature makes c read-only, its uses were not checked; the same java.util.Collection at runtime
                printed.add(new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(print(args.get(i), q))
                        .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                        .add(new TextImpl("MutableCollection<" + forced + ">")).add(SymbolEnum.RIGHT_PARENTHESIS));
                continue;
            }
            // an argument converts to the parameter's interface by itself; no SAM constructor needed
            if (call != null && unwrap(args.get(i)) instanceof MethodReference mr && nullableStreamElements(method, call)) {
                OutputBuilder asserting = assertingReference(mr, q);
                if (asserting != null) {
                    printed.add(asserting);
                    continue;
                }
            }
            if (hasParameter && unwrap(args.get(i)) instanceof MethodReference mr && genericReferenceTyping(method, i, mr)) {
                // Collector.of(LinkedHashMultiset::create, …): Java infers create's E, and so Collector.of's A, from
                // the target; Kotlin infers neither from a bare reference. A SAM constructor of the reference's
                // functional type gives both
                printed.add(new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(mr.parameterizedType()
                                .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q)))
                        .add(SymbolEnum.LEFT_PARENTHESIS).add(print(args.get(i), q)).add(SymbolEnum.RIGHT_PARENTHESIS));
                continue;
            }
            if (hasParameter && unwrap(args.get(i)) instanceof MethodReference mr && besideTypeParameter(method, i)) {
                // Function<Range<K>, Cut<K>>({ it.lowerBound() }): the reference's own type arguments, or Kotlin infers
                // none for a SAM constructor around a lambda (guava's ImmutableRangeMap)
                ParameterizedType functional = mr.parameterizedType();
                TypeInfo samType = method.parameters().get(i).parameterizedType().typeInfo();
                boolean typed = functional != null && functional.typeInfo() == samType && functional.arrays() == 0
                                && !functional.parameters().isEmpty()
                                && functional.parameters().stream().allMatch(p -> p.wildcard() == null);
                printed.add(new OutputBuilderImpl().add(new TextImpl(typed
                                ? KotlinTypeName.of(functional.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q)
                                : KotlinTypeName.name(samType, q)))
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
                if (call != null && hasParameter && wildcardReceiver(call)) {
                    // metadata.forEach { key, value -> } on a Map<String, *>: Kotlin's value is an Any?, where Java's
                    // capture of ? is an Object
                    List<ParameterizedType> fromStar = new ArrayList<>(types.size() == params.size() ? types
                            : java.util.Collections.nCopies(params.size(), (ParameterizedType) null));
                    for (int j = 0; j < params.size(); j++) {
                        ParameterizedType java = params.get(j).parameterizedType();
                        if ((fromStar.get(j) == null || fromStar.get(j).isTypeParameter()) && java.arrays() == 0
                            && (java.typeInfo() == null ? !java.isTypeParameter() || java.isUnboundWildcard()
                                : java.typeInfo().isJavaLangObject())) {
                            fromStar.set(j, java.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE));
                        }
                    }
                    types = fromStar;
                }
                boolean typed = types.size() == params.size();
                for (int j = 0; typed && j < params.size(); j++) {
                    ParameterizedType t = types.get(j);
                    if (t != null && KotlinNullability.isNullable(t)) KotlinContext.lambdaParameterType(params.get(j), t);
                }
                try {
                    printed.add(lambda(l, hasParameter && besideOtherInterface(method, i), q));
                } finally {
                    if (typed) params.forEach(p -> KotlinContext.lambdaParameterType(p, null));
                }
            } else {
                ParameterizedType declared = !hasParameter ? mapped ? null : varargsElement(method, i)
                        : KotlinNullability.parameterType(method.parameters().get(i));
                boolean argumentTranslated = translated;
                if (call != null) {
                    ParameterizedType seen = KotlinNullability.throughReceiver(declared, call, true);
                    // the receiver's type argument is a declaration of ours, even on a library member (queue.add);
                    // not a library call's result (Optional.ofNullable(d).orElse(e)), whose type argument Kotlin
                    // infers, and whose member takes the flexible T!: null goes in as in Java, where `!!` throws
                    if (seen != declared && !libraryResult(call.object())) argumentTranslated = true;
                    declared = seen;
                }
                // set.contains(x), map.get(k) with a nullable x or k: Java's take an Object, and null finds nothing;
                // Kotlin's stdlib extensions take any supertype of the element or key, so no `!!` (which throws)
                ParameterizedType lookedUp = hasParameter && call != null ? nullableLookup(method, call, args.get(i), i) : null;
                // get(key: K) overriding Map.get: its key is a K already in Kotlin, whatever Java's Object says
                if (lookedUp != null && unwrap(args.get(i)) instanceof VariableExpression mappedVe
                    && mappedVe.variable() instanceof ParameterInfo mappedPi
                    && KotlinMappedMembers.overriddenParameterType(mappedPi.methodInfo(), mappedPi.index()) != null) {
                    lookedUp = null;
                }
                if (lookedUp != null) {
                    if (lookedUp.typeParameter() != null
                        || unwrap(args.get(i)).parameterizedType().typeInfo() != lookedUp.typeInfo()
                           && KotlinNullability.nullableInKotlin(args.get(i))
                        // map.entries.contains(immutableEntry(key, value)) into a Set<Entry<K, V>>: the extension's
                        // T is an Entry<Any?, Any?> or an Entry<K, V>, never both; the cast checks only the class
                        || !lookedUp.parameters().isEmpty()
                           && !java.util.Objects.equals(unwrap(args.get(i)).parameterizedType(), lookedUp)) {
                        // lst.remove(varassign) with a VarExprent? from a List<Exprent>: T is inferred from the
                        // inputs only, so the argument is cast up to the element type. Into a translated override
                        // whose Kotlin parameter is non-null (super.get(key) of guava's SynchronizedAsMap), to that
                        boolean nonNullOverride = !method.typeInfo().fullyQualifiedName().startsWith("java.util.")
                                && KotlinNullability.translated(method.typeInfo())
                                && !KotlinNullability.isNullable(KotlinNullability.parameterType(method.parameters().get(i)));
                        printed.add(new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                                .add(operand(PrecedenceEnum.CAST, args.get(i), q))
                                .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                                .add(new TextImpl(KotlinTypeName.of(lookedUp.withNullable(nonNullOverride
                                        ? io.codelaser.maddi.cst.api.type.NullableState.NONNULL
                                        : io.codelaser.maddi.cst.api.type.NullableState.NULLABLE), q)))
                                .add(SymbolEnum.RIGHT_PARENTHESIS));
                        continue;
                    }
                    argumentTranslated = false;
                }
                if (hasParameter && nullableBesideWiderOverload(method, i, args.get(i))) {
                    // guava's Ascii.toUpperCase(chars) with a String, to toUpperCase(string: String?) beside
                    // toUpperCase(chars: CharSequence): a String fits both, neither more specific in Kotlin
                    printed.add(new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                            .add(operand(PrecedenceEnum.CAST, args.get(i), q)).add(SpaceEnum.ONE).add(KotlinKeyword.AS)
                            .add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(declared, q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS));
                    continue;
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
                // a constructor call takes the target's states (Kotlin's generics are invariant); so does (AiMessage)
                // null, which picks an overload: `null as AiMessage?` into a nullable parameter (`as AiMessage` throws)
                ParameterizedType widenTo = (unwrap(args.get(i)) instanceof ConstructorCall
                                             || unwrap(args.get(i)) instanceof Cast c && unwrap(c.expression()) instanceof NullConstant)
                                            && declared != null ? declared : target;
                OutputBuilder argument = KotlinNullability.toTarget(args.get(i), declared, argumentTranslated,
                        widened(args.get(i), widenTo, q), q);
                if (hasParameter && covariantArrayArgument(method, i, args.get(i))) {
                    KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, args.get(i), KotlinContext.describe(args.get(i)));
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            // a nullable parameter's `as Array<Any?>?` lets a null through, as Java does
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && varargsIntoArray(method, i, args.get(i))) {
                    // ensureNotEmpty(values, name) with a vararg values: Kotlin's is an Array<out T?>, and the
                    // parameter's Array<T?> is invariant; erased, the cast checks nothing, as Java's arrays are covariant
                    ParameterInfo vararg = (ParameterInfo) ((VariableExpression) unwrap(args.get(i))).variable();
                    KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, args.get(i), KotlinContext.describe(args.get(i)));
                    ParameterizedType castType = KotlinNullability.parameterType(vararg)
                            .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL);
                    // ImmutableList.copyOf(futures) into copyOf(elements: Array<E?>): the elements' state is the
                    // callee's, as Kotlin's Array is invariant (the cast checks no element)
                    ParameterizedType into = KotlinNullability.parameterType(method.parameters().get(i));
                    if (into.arrays() == 1 && castType.arrays() == 1
                        && KotlinNullability.isNullable(into.componentType())
                        && !KotlinNullability.isNullable(castType.componentType())) {
                        castType = castType.withComponentType(castType.componentType()
                                .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE));
                    }
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(castType, q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && castable(method, i) && nullabilityTiesOverload(method, i, args.get(i))) {
                    // Json.fromJson(String?, Class<T>) beside fromJson(String, Type), with a non-null String: each is
                    // more specific in one parameter, and Kotlin finds the call ambiguous; Java's choice, by its type
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && castable(method, i) && boxedIntoReference(method, i, args.get(i))) {
                    // new PrimitiveConstant(tag, Integer.valueOf(v)): Java's Integer takes (int, Object), Kotlin's
                    // Int the (int, int) overload; the cast keeps Java's choice
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && castable(method, i) && collectionBesideObject(method, i)) {
                    // template.apply(Collections.singletonMap("name", "Klaus")) beside apply(Object): Java takes the
                    // Map overload; Kotlin's MutableMap<String, Any?> is invariant and takes no Map<String, String>,
                    // so Kotlin took apply(value: Any). The cast keeps Java's choice, unchecked, as Java's generics are
                    argument = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(argument).add(SpaceEnum.ONE)
                            .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                            .add(new TextImpl(KotlinTypeName.of(KotlinNullability.parameterType(method.parameters().get(i)), q)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                } else if (hasParameter && castable(method, i) && nullableBesideVarargs(method, i)) {
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
            List<Statement> statements = CstQueries.own(l.methodBody().statements());
            Expression value = CstQueries.returnedValue(statements);
            return value != null && unwrap(value) instanceof BinaryOperator bo && bo.operator() != null
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
        KotlinContext.message(KotlinPrintCode.ASSERT_AT_DEREFERENCE, mr, KotlinContext.describe(mr));
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

    /**
     * A method's own varargs parameter (Kotlin's {@code Array<out T>}) passed to a translated method's reference array
     * parameter, which Kotlin declares invariant.
     */
    private static boolean varargsIntoArray(io.codelaser.maddi.cst.api.info.MethodInfo method, int i, Expression arg) {
        if (!KotlinContext.translatingJava() || !KotlinNullability.translated(method.typeInfo())) return false;
        if (!(unwrap(arg) instanceof VariableExpression ve) || !(ve.variable() instanceof ParameterInfo pi)
            || !pi.isVarArgs()) {
            return false;
        }
        ParameterizedType target = method.parameters().get(i).parameterizedType();
        return target.arrays() == 1 && !target.componentType().isPrimitiveExcludingVoid()
               && !pi.parameterizedType().componentType().isPrimitiveExcludingVoid();
    }

    /** The receiver's Java type has a {@code ?} argument: {@code Map<String, ?>}, Kotlin's {@code Map<String, *>}. */
    private static boolean wildcardReceiver(MethodCall call) {
        if (call.object() == null || call.objectIsImplicit()) return false;
        ParameterizedType java = call.object().parameterizedType();
        return java != null && java.parameters().stream().anyMatch(p -> p != null && p.isUnboundWildcard());
    }

    /**
     * An argument passed as one of the varargs: {@code runtime(format, key, value)} into {@code vararg args: Any}, the
     * element's type with its verdict. Null when {@code i} is no varargs position.
     */
    private static ParameterizedType varargsElement(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (method == null || method.parameters().isEmpty()) return null;
        ParameterInfo last = method.parameters().getLast();
        if (!last.isVarArgs() || i < method.parameters().size() - 1) return null;
        ParameterizedType array = KotlinNullability.parameterType(last);
        return array.arrays() == 1 ? array.componentType() : null;
    }

    /**
     * {@code withRetry(() -> …, n)} beside an overload {@code withRetry(Runnable, int)}: Kotlin may take the lambda as
     * the other interface (a Runnable, whose result is Unit), unless a SAM constructor names Java's.
     */
    private static boolean besideOtherInterface(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        if (!KotlinContext.translatingJava()) return false;
        TypeInfo functional = method.parameters().get(i).parameterizedType().typeInfo();
        if (functional == null || !functional.isInterface()) return false;
        return method.typeInfo().methods().stream().anyMatch(other -> other != method
                && other.name().equals(method.name()) && other.parameters().size() == method.parameters().size()
                && other.parameters().get(i).parameterizedType().arrays() == 0
                && other.parameters().get(i).parameterizedType().typeInfo() instanceof TypeInfo t
                && t != functional && t.isFunctionalInterface());
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
            KotlinContext.message(KotlinPrintCode.ASSERT_INTO_NON_NULL, mr, KotlinContext.describe(mr));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACE).add(SpaceEnum.ONE).add(receiver(mr.scope(), q))
                    .add(SymbolEnum.DOT).add(new TextImpl(name + "(it!!)")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
        }
        // a library's parameter is a platform type, which takes the null: Objects::isNull stays a reference
        if (m.isStatic() && m.parameters().size() == 1 && KotlinNullability.translated(m.typeInfo())
            && !KotlinNullability.isNullable(KotlinNullability.parameterType(m.parameters().getFirst()))) {
            KotlinContext.message(KotlinPrintCode.ASSERT_INTO_NON_NULL, mr, KotlinContext.describe(mr));
            return text("{ " + KotlinTypeName.staticOwner(te.parameterizedType().typeInfo(), q) + "." + name + "(it!!) }");
        }
        if (!m.isStatic() && m.parameters().isEmpty()) {
            KotlinContext.message(KotlinPrintCode.ASSERT_AT_DEREFERENCE, mr, KotlinContext.describe(mr));
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
        if (x instanceof InlineConditional ic && KotlinNullCheckShapes.elvis(ic) != null) {
            return mayBeNull(KotlinNullCheckShapes.elvis(ic).otherwise());
        }
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
        if (!LOOKUPS.contains(method.name()) || call.object() == null || call.objectIsImplicit()) return null;
        // this@LocalCache.get(key): the receiver's own override of a java.util lookup, which Kotlin declares
        // get(key: K) as Map's; and Map's remove(key, value), whose value is a V
        io.codelaser.maddi.cst.api.info.MethodInfo lookup = method.typeInfo().fullyQualifiedName().startsWith("java.util.")
                ? method : method.overrides().stream()
                .filter(o -> o.typeInfo().fullyQualifiedName().startsWith("java.util.")).findFirst().orElse(null);
        if (lookup == null) return null;
        boolean removeEntry = "remove".equals(method.name()) && method.parameters().size() == 2
                              && lookup.typeInfo().fullyQualifiedName().endsWith("Map");
        if ("remove".equals(method.name()) && method.parameters().size() != 1 && !removeEntry) return null;
        ParameterizedType p = lookup.parameters().get(i).parameterizedType();
        if (!p.isJavaLangObject() || p.arrays() != 0 || i != 0 && !removeEntry) return null;
        ParameterizedType element = lookedUp(call.object().parameterizedType(), lookup.typeInfo(),
                "containsValue".equals(method.name()) || removeEntry && i == 1 ? 1 : 0);
        if (element != null && element.wildcard() != null && element.wildcard().isSuper()) {
            // lst.remove(post) on a MutableList<in Statement?>: the member takes the bound, when that is nullable
            ParameterizedType kotlin = lookedUp(KotlinNullability.kotlinType(call.object()), method.typeInfo(),
                    "containsValue".equals(method.name()) ? 1 : 0);
            element = kotlin != null && kotlin.wildcard() != null && kotlin.wildcard().isSuper()
                      && KotlinNullability.isNullable(kotlin) ? kotlin.withWildcard(null) : null;
        }
        ParameterizedType argumentType = argument.parameterizedType();
        if (typeVariableLookup(element, argumentType)) return element;
        boolean found = element != null && argumentType != null && element.arrays() == argumentType.arrays()
                        && element.wildcard() == null && element.typeInfo() != null
                        && (element.typeInfo() == argumentType.typeInfo()
                            || element.arrays() == 0
                               && isSubtype(argumentType.typeInfo(), element.typeInfo(), new java.util.HashSet<>()));
        return found ? element : null;
    }

    /**
     * guava's {@code map.remove(key)} with an {@code Object} key into a {@code Map<K, V>}: the element is a type
     * variable the argument is not. The cast to {@code K?} is unchecked, as Java's lookup is; casting to a class would
     * throw where Java finds nothing.
     */
    private static boolean typeVariableLookup(ParameterizedType element, ParameterizedType argumentType) {
        return element != null && argumentType != null && element.typeParameter() != null && element.arrays() == 0
               && element.wildcard() == null && argumentType.arrays() == 0
               && argumentType.typeParameter() != element.typeParameter();
    }

    /**
     * The parameter's type can be written at the call: it names none of the called method's own type parameters
     * ({@code Maps.transformValues}'s {@code Function<? super V1, V2>}), which only the callee knows.
     */
    /**
     * A reference to a generic constructor or static method, whose type arguments only the target gives, passed where
     * the callee's own type parameters are inferred: {@code Collector.of(LinkedHashMultiset::create, …)}. Only a
     * supplier, with the reference's functional type fully known.
     */
    private static boolean genericReferenceTyping(io.codelaser.maddi.cst.api.info.MethodInfo method, int i,
                                                  MethodReference mr) {
        if (!KotlinContext.translatingJava() || castable(method, i) || mr.methodInfo() == null) return false;
        io.codelaser.maddi.cst.api.info.MethodInfo referenced = mr.methodInfo();
        boolean generic = referenced.isConstructor() ? !referenced.typeInfo().typeParameters().isEmpty()
                : referenced.isStatic() && !referenced.typeParameters().isEmpty();
        ParameterizedType functional = mr.parameterizedType();
        TypeInfo samType = method.parameters().get(i).parameterizedType().typeInfo();
        // a supplier only: a reference that takes values (Optional.map(List::of)) gets them typed by the receiver
        boolean supplier = samType != null && samType.singleAbstractMethod() != null
                           && samType.singleAbstractMethod().parameters().isEmpty();
        return generic && supplier && functional != null && functional.typeInfo() == samType
               && functional.arrays() == 0 && !functional.parameters().isEmpty()
               && functional.parameters().stream().allMatch(p -> p.wildcard() == null && !CstQueries.mentionsAny(p,
                        method.typeParameters()));
    }

    private static boolean castable(io.codelaser.maddi.cst.api.info.MethodInfo method, int i) {
        ParameterizedType p = method.parameters().get(i).parameterizedType();
        return method.typeParameters().stream().noneMatch(tp -> CstQueries.mentions(p, tp));
    }

    /**
     * The callee's parameter is nullable in Kotlin, an overload of the same arity takes a non-null supertype there,
     * and the argument is non-null: Kotlin finds neither more specific, Java takes the narrower one.
     */
    private static boolean nullableBesideWiderOverload(io.codelaser.maddi.cst.api.info.MethodInfo method, int i,
                                                       Expression argument) {
        if (!KotlinContext.translatingJava() || !KotlinNullability.translated(method.typeInfo())
            || i >= method.parameters().size() || method.parameters().get(i).isVarArgs()) return false;
        ParameterInfo p = method.parameters().get(i);
        ParameterizedType type = p.parameterizedType();
        if (type.arrays() != 0 || type.typeInfo() == null || type.typeParameter() != null
            || !KotlinNullability.isNullable(KotlinNullability.parameterType(p))
            || KotlinNullability.nullableInKotlin(argument)) return false;
        return method.typeInfo().methods().stream().anyMatch(o -> o != method && o.name().equals(method.name())
                && o.parameters().size() == method.parameters().size() && !o.parameters().get(i).isVarArgs()
                && o.parameters().get(i).parameterizedType().arrays() == 0
                && o.parameters().get(i).parameterizedType().typeInfo() != null
                && o.parameters().get(i).parameterizedType().typeInfo() != type.typeInfo()
                && isSubtype(type.typeInfo(), o.parameters().get(i).parameterizedType().typeInfo(), new java.util.HashSet<>())
                && !KotlinNullability.isNullable(KotlinNullability.parameterType(o.parameters().get(i))));
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
     * As {@link #widened}, for a value that is returned or initializes a variable (not an argument, whose own paths
     * convert it): a reference array Java's covariance accepts gets the unchecked cast Kotlin needs.
     * {@code TypeVariable<?>[] params = cls.getTypeParameters()}, {@code return array} of a vararg: Kotlin's are an
     * {@code Array<out TypeVariable<Class<T>>>}, an {@code Array<out Any?>}. Erased, the cast checks only that it is
     * an array.
     */
    static OutputBuilder assigned(Expression e, ParameterizedType target, Qualification q) {
        if (KotlinContext.translatingJava() && unwrap(e) instanceof MethodCall mc && target != null
            && !target.parameters().isEmpty() && mc.typeArguments().stream().anyMatch(KotlinTypeName::definitelyNonNull)
            && sameUpToStates(mc.parameterizedType(), target) && nullableArgument(mc.methodInfo())) {
            // guava's NullsFirstOrdering.reverse(): ….<@NonNull S>nullsLast(), an Ordering<@Nullable S>, returned as an
            // Ordering<S> whose S may be null itself: Java's nullness is unchecked, Kotlin's invariant generics cast
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(print(e, q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (!KotlinContext.translatingJava() || !covariantArray(e, target)) return widened(e, target, q);
        KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, e, KotlinContext.describe(e));
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(print(e, q)).add(SpaceEnum.ONE)
                .add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(target, q)))
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /**
     * {@code e}, converted to {@code target} when Java widens it implicitly and Kotlin does not: {@code s.toInt()}
     * for a short where an int is expected, {@code i.toLong()}, {@code c.code}. A Kotlin integer literal adapts to an
     * integral type by itself.
     */
    static OutputBuilder widened(Expression e, ParameterizedType target, Qualification q) {
        if (unwrap(e) instanceof Lambda l && target != null && target.arrays() == 0 && !target.parameters().isEmpty()
            && samConstructor(l, q) != null && sameUpToStates(l.concreteFunctionalType(), target)) {
            // DEFAULT_MAX_RESULTS: Function<Query, Int?> = Function<Query, Int?> { 3 }: Kotlin's generics are
            // invariant, so the SAM constructor takes the type arguments of what it initializes, states included
            return lambda(l, KotlinTypeName.of(target.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q), q);
        }
        if (unwrap(e) instanceof Cast c && KotlinNullability.isNullable(target) && KotlinNullability.nullableCast(c)) {
            // String factory = (String) properties.get(key): Java's cast lets the null through
            return cast(c, true, q);
        }
        if (unwrap(e) instanceof Cast c && target != null && !target.parameters().isEmpty()
            && c.parameterizedType().typeInfo() != null && c.parameterizedType().typeInfo() == target.typeInfo()
            && c.parameterizedType().arrays() == target.arrays()
            && c.parameterizedType().parameters().size() == target.parameters().size()
            // states only: guardrailClass(Class<C>) takes (Class<G>) x, whose G is no C
            && sameUpToStates(c.parameterizedType(), target)) {
            // (Map<String, Object>) Json.fromJson(…) into a Map<String, Object?>: Kotlin's generics are invariant, and
            // the cast checks no type argument; it takes those of what it initializes
            ParameterizedType castType = target.withNullable(c.parameterizedType().nullable());
            return cast(c, castType, KotlinNullability.isNullable(target) && KotlinNullability.nullableCast(c), q);
        }
        if (unwrap(e) instanceof Cast c && target != null && rawCastTo(c.parameterizedType(), target)) {
            // return (Collector) TO_IMMUTABLE_LIST into a Collector<E, ?, ImmutableList<E>>: Java's raw type converts
            // unchecked, Kotlin's Collector<*, *, *> does not; the cast takes the type arguments of what it initializes
            ParameterizedType castType = target.withNullable(c.parameterizedType().nullable());
            return cast(c, castType, KotlinNullability.isNullable(target) && KotlinNullability.nullableCast(c), q);
        }
        if (target != null && unwrap(e) instanceof MethodCall mc && optionalFactory(mc) && target.typeInfo() != null
            && "java.util.Optional".equals(target.typeInfo().fullyQualifiedName()) && target.arrays() == 0
            && target.parameters().size() == 1 && target.parameters().getFirst().typeParameter() != null
            && target.parameters().getFirst().wildcard() == null) {
            // Optional.ofNullable(element) into an Optional<T>: Kotlin's Optional bounds its T non-null, so the call
            // is an Optional<T & Any>, which the invariant Optional<T> is not. Erased, the cast checks nothing
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(print(e, q)).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (target != null && target.arrays() == 1 && unwrap(e) instanceof MethodCall mc && mc.methodInfo() != null
            && (toArray(mc.methodInfo()) || toArrayOf(mc.methodInfo())) && KotlinNullability.isNullable(target.componentType())) {
            // return c.toArray() into an Object[] whose elements are nullable: toTypedArray<Any?>(), as Kotlin's
            // arrays are invariant
            java.util.Map.Entry<MethodCall, ParameterizedType> previous = TO_ARRAY_TARGET.get();
            TO_ARRAY_TARGET.set(java.util.Map.entry(mc, target.componentType()));
            try {
                return print(e, q);
            } finally {
                TO_ARRAY_TARGET.set(previous);
            }
        }
        if (unwrap(e) instanceof Cast c && target != null && target.arrays() > 0
            && c.parameterizedType().arrays() == target.arrays() && sameUpToStates(c.parameterizedType(), target)
            && KotlinTypeName.primitiveArray(c.parameterizedType().componentType()) == null) {
            // (E[]) new Object[n] into an Array<E?>: Kotlin's arrays are invariant, and the cast checks no element;
            // it takes the elements' states of what it initializes
            ParameterizedType castType = target.withNullable(c.parameterizedType().nullable());
            return cast(c, castType, KotlinNullability.isNullable(target) && KotlinNullability.nullableCast(c), q);
        }
        if (e instanceof ArrayInitializer ai && target != null && target.arrays() > 0) {
            // {…} takes its type from what it initializes: its elements may all be null, or arrays themselves
            return arrayInitializer(ai, target, q);
        }
        if (e instanceof InlineConditional ic && target != null && KotlinNullCheckShapes.elvis(ic) != null) {
            KotlinNullCheckShapes.Elvis elvis = KotlinNullCheckShapes.elvis(ic);
            return elvis(elvis, widened(elvis.otherwise(), target, q), q);
        }
        if (e instanceof InlineConditional ic && target != null && KotlinNullCheckShapes.safeCall(ic) != null) {
            return print(ic, q);
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
        if (unwrap(e) instanceof ConstructorCall cc && target != null && typeToken(cc, target)) {
            // MAP_TYPE: TypeReference<MutableMap<String, Any?>> = object : TypeReference<MutableMap<String, Any?>>() { }:
            // 'new TypeReference<>() {}' takes the type arguments of what it initializes, states included, as a
            // constructor call does (Kotlin's generics are invariant; langchain4j DefaultStructuredPromptFactory,
            // maddi-mod#38). Only without members, which would override with the supertype's arguments
            return anonymousClass(cc, target.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q);
        }
        if (unwrap(e) instanceof ConstructorCall cc && cc.arrayInitializer() == null && cc.anonymousClass() == null
            && target != null && target.arrays() > 0 && cc.parameterizedType().arrays() == target.arrays()
            && cc.parameterExpressions().stream().filter(x -> !x.isEmpty()).count() == 1 && nonNullElements(target)) {
            // Offsets[] offsets = new Offsets[n], filled before it is read: arrayOfNulls is an Array<Offsets?>, and the
            // declaration's elements are non-null. Erased, the cast checks nothing: a slot read before it is filled
            // fails later, as Java's null does
            ParameterizedType created = rawOf(cc.parameterizedType(), target) ? target : cc.parameterizedType();
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, cc, KotlinContext.describe(cc));
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
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(operand(PrecedenceEnum.CAST, e, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target, q))).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (target != null && !(unwrap(e) instanceof ConstructorCall) && !(unwrap(e) instanceof NullConstant)
            && rawOf(rawValueType(e), target)) {
            // FastSparseSet<Integer>[] empty = FastSparseSet.EMPTY_ARRAY: Java's unchecked conversion is Kotlin's
            // unchecked cast; an Array<FastSparseSet<*>?> is no Array<FastSparseSet<Int>?>
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, e, KotlinContext.describe(e));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(operand(PrecedenceEnum.CAST, e, q))
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target, q))).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        Primitive to = target == null ? null : primitive(target);
        Primitive from = primitive(e.parameterizedType());
        if (narrowedConstant(from, to)) {
            // char MIN = 0, byte MAX_POWER_OF_TWO = 1 << 6: Java narrows a constant implicitly. A Kotlin integer
            // literal adapts to Byte and Short by itself, not to Char
            if (unwrap(e) instanceof IntConstant ic && to == Primitive.CHAR && ic.constant() >= 0 && ic.constant() <= 0xFFFF) {
                return text(charLiteral((char) ic.constant().intValue()));
            }
            if (unwrap(e) instanceof IntConstant) return print(e, q);
            OutputBuilder narrowed = valueOperand(e, q);
            for (String call : conversion(from, to).split("\\.")) {
                if (!call.isEmpty()) narrowed.add(SymbolEnum.DOT).add(new TextImpl(call));
            }
            return narrowed;
        }
        if (to == null || from == null || from == to || from == Primitive.BOOLEAN || to == Primitive.BOOLEAN
            || to == Primitive.CHAR || rank(to) < rank(from)) {
            return print(e, q);
        }
        if (unwrap(e) instanceof IntConstant && to != Primitive.FLOAT && to != Primitive.DOUBLE) return print(e, q);
        // limit(maxResults) with an Integer field: Java unboxes, and throws on null; Kotlin's Int? has no toLong()
        OutputBuilder receiver = valueOperand(e, q);
        for (String call : conversion(from, to).split("\\.")) {
            if (!call.isEmpty()) receiver.add(SymbolEnum.DOT).add(new TextImpl(call));
        }
        return receiver;
    }

    /** Java widens {@code e}'s primitive into {@code target}'s, and Kotlin wants it explicit. */
    private static boolean widens(Expression e, ParameterizedType target) {
        Primitive to = target == null ? null : primitive(target);
        Primitive from = primitive(e.parameterizedType());
        return to != null && from != null && from != to && from != Primitive.BOOLEAN && to != Primitive.BOOLEAN
               && to != Primitive.CHAR && rank(to) >= rank(from) && !(unwrap(e) instanceof IntConstant);
    }

    /** An int (or char) constant Java narrows into a char, byte or short without a cast. */
    private static boolean narrowedConstant(Primitive from, Primitive to) {
        return to != null && from != null && from != to
               && (to == Primitive.CHAR || to == Primitive.BYTE || to == Primitive.SHORT)
               && (from == Primitive.INT || from == Primitive.CHAR || from == Primitive.SHORT);
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
            || constructed.parameters().size() != target.parameters().size()
            || !passesParametersOn(constructed.typeInfo(), target.typeInfo())) {
            return null;
        }
        boolean changed = false;
        List<ParameterizedType> arguments = new ArrayList<>();
        for (int i = 0; i < constructed.parameters().size(); i++) {
            ParameterizedType mine = constructed.parameters().get(i);
            ParameterizedType theirs = target.parameters().get(i);
            // Metadata(new HashMap<>()) into a MutableMap<String?, *>: a star takes any argument, the constructed one
            if (theirs != null && theirs.isUnboundWildcard()) {
                arguments.add(mine);
                continue;
            }
            if (!sameUpToStates(mine, theirs)) return null;
            changed |= !mine.equals(theirs);
            arguments.add(theirs);
        }
        return changed ? constructed.withParameters(arguments) : null;
    }

    /**
     * {@code sub}'s type parameters are {@code sup}'s arguments as they are, in order: ArrayList<E> a List<E>. Not
     * guava's NullsFirstOrdering<T>, an Ordering<@Nullable T>: the target's Ordering<S?> is its S, not its S?.
     */
    private static boolean passesParametersOn(TypeInfo sub, TypeInfo sup) {
        if (sub == null || sup == null) return false;
        if (sub == sup) return true;
        ParameterizedType formal = sub.asParameterizedType();
        ParameterizedType seen;
        try {
            seen = formal.concreteSuperType(sup.asParameterizedType());
        } catch (RuntimeException | AssertionError e) {
            return false;
        }
        if (seen == null || seen.parameters().size() != formal.parameters().size()) return false;
        for (int i = 0; i < formal.parameters().size(); i++) {
            if (seen.parameters().get(i).typeParameter() != formal.parameters().get(i).typeParameter()) return false;
        }
        // a direct supertype that annotates an argument @Nullable passes it on as T?
        java.util.stream.Stream<ParameterizedType> direct = java.util.stream.Stream.concat(
                sub.parentClass() == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(sub.parentClass()),
                sub.interfacesImplemented().stream());
        return direct.filter(d -> d.typeInfo() == sup).noneMatch(d -> KotlinTypePrinter.declaredArguments(d).parameters()
                .stream().anyMatch(KotlinNullability::isNullable));
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

    /**
     * A reference array value Kotlin types otherwise than the array it lands in: a vararg parameter (or its clone),
     * Kotlin's {@code Array<out T>}; or a value whose Java element type is another than the target's, which only
     * Java's covariance accepts. Not where Java casts, creates or converts the array itself.
     */
    private static boolean covariantArray(Expression e, ParameterizedType target) {
        if (target == null || target.arrays() == 0 || KotlinTypeName.primitiveArray(target.componentType()) != null
            || target.componentType().isPrimitiveExcludingVoid()
            // a raw array target (Box[][] g = grid) is typed from its value
            || target.typeInfo() != null && !target.typeInfo().typeParameters().isEmpty() && target.parameters().isEmpty()) {
            return false;
        }
        Expression u = unwrap(e);
        if (u instanceof MethodCall mc && "clone".equals(mc.methodInfo() == null ? null : mc.methodInfo().name())
            && mc.object() != null && vararg(mc.object())) {
            return true;
        }
        if (vararg(u)) return true;
        if (!(u instanceof MethodCall mc && !toArray(mc.methodInfo()) && !toArrayOf(mc.methodInfo())
              || u instanceof VariableExpression)) {
            return false;
        }
        ParameterizedType value = u.parameterizedType();
        if (value == null || value.arrays() != target.arrays() || target.typeInfo() == null
            || target.typeParameter() != null || value.isPrimitiveExcludingVoid()) {
            return false;
        }
        // @Nullable Object[] unsoundlyCovariantArray = a, of a T[] (guava's toArray(T[] a) implementations)
        if (value.typeParameter() != null) return target.typeInfo().isJavaLangObject();
        return value.typeInfo() != null && !sameUpToStates(value, target)
               && !(value.typeInfo() == target.typeInfo() && value.parameters().isEmpty());
    }

    /**
     * A reference array handed to a translated method's array parameter that is invariant ({@code Array<Any?>}, not
     * printed {@code Array<out …>}) where Java's covariance accepts it: {@code copyIntoArray(other, 0)} of a T[].
     */
    private static boolean covariantArrayArgument(io.codelaser.maddi.cst.api.info.MethodInfo method, int i,
                                                  Expression arg) {
        if (!KotlinContext.translatingJava() || method == null || i >= method.parameters().size()
            || !KotlinNullability.translated(method.typeInfo()) || !castable(method, i)) {
            return false;
        }
        ParameterInfo p = method.parameters().get(i);
        ParameterizedType type = KotlinNullability.parameterType(p);
        return !p.isVarArgs() && type.arrays() == 1 && !KotlinMethodPrinter.covariantArray(p, type)
               && covariantArray(arg, p.parameterizedType());
    }

    private static boolean vararg(Expression e) {
        return unwrap(e) instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo pi && pi.isVarArgs();
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
        if (kotlin != null && kotlin.isTypeParameter() && kotlin.arrays() == 0 && unwrap(e) instanceof MethodCall mc
            && mc.methodInfo().typeParameters().contains(kotlin.typeParameter())) {
            // CODEC.fromJson(json, Map.class): the callee's own T is what Java infers, a raw Map here
            return e.parameterizedType();
        }
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

    /** A cast to a raw type, into a parameterization of it: unlike a creation, a cast may write a wildcard. */
    private static boolean rawCastTo(ParameterizedType raw, ParameterizedType typed) {
        return raw.typeInfo() != null && raw.typeInfo() == typed.typeInfo() && raw.arrays() == typed.arrays()
               && !raw.typeInfo().typeParameters().isEmpty() && raw.parameters().isEmpty()
               && typed.parameters().size() == raw.typeInfo().typeParameters().size()
               && typed.parameters().stream().noneMatch(p -> p.isTypeParameter() && p.typeParameter() == null);
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
            String name = mr.methodInfo().isStatic() ? KotlinTypeName.staticOwner(owner, q) : KotlinTypeName.name(owner, q);
            if (KotlinContext.translatingJava() && !owner.typeParameters().isEmpty() && KotlinNullability.translated(owner)) {
                // Acc::create, Acc::add on a generic class of ours: Kotlin wants the class's type arguments, and finds
                // a static in the companion
                ParameterizedType receiver = m.isStatic() ? null : referenceReceiver(mr, owner);
                if (m.isStatic()) name += ".Companion";
                else if (receiver != null) name = KotlinTypeName.of(receiver
                        .withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q);
            }
            b.add(new TextImpl(name));
        } else {
            b.add(receiver(scope, q));
        }
        return b.add(new TextImpl("::" + KotlinNames.name(mr.methodInfo().name())));
    }

    /**
     * The receiver of an unbound reference {@code Acc::add}: the first parameter of the functional type's single
     * abstract method ({@code BiConsumer<Acc<Object>, Object>} takes an {@code Acc<Object>}), when it is the owner
     * with its type arguments. Where the single abstract method is not known (a library interface not inspected),
     * or one inherited and not seen through ({@code BinaryOperator}'s {@code apply} is {@code BiFunction}'s), the
     * functional type's one distinct type argument of the owner. Null otherwise.
     */
    private static ParameterizedType referenceReceiver(MethodReference mr, TypeInfo owner) {
        ParameterizedType functional = mr.parameterizedType();
        if (functional == null) return null;
        List<ParameterizedType> types = KotlinNullability.samParameterTypes(functional);
        if (!types.isEmpty() && typedOwner(types.getFirst(), owner)) return types.getFirst();
        List<ParameterizedType> candidates = functional.parameters().stream()
                .filter(p -> typedOwner(p, owner)).distinct().toList();
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private static boolean typedOwner(ParameterizedType type, TypeInfo owner) {
        return type != null && type.typeInfo() == owner && type.arrays() == 0 && type.wildcard() == null
               && type.parameters().size() == owner.typeParameters().size()
               && type.parameters().stream().allMatch(p -> p.wildcard() == null);
    }

    private static boolean optionalFactory(MethodCall mc) {
        io.codelaser.maddi.cst.api.info.MethodInfo m = mc.methodInfo();
        return m != null && m.isStatic() && "java.util.Optional".equals(m.typeInfo().fullyQualifiedName())
               && java.util.Set.of("of", "ofNullable", "empty").contains(m.name());
    }

    /** The receiver's element type (E of its Collection<E>) is a type variable. */
    private static boolean typeVariableElement(Expression object, TypeInfo collection) {
        ParameterizedType element = lookedUp(object.parameterizedType(), collection, 0);
        return element != null && element.typeParameter() != null && element.arrays() == 0;
    }

    /** The toArray call {@link #widened} prints into an array whose elements are nullable, with that element type. */
    private static final ThreadLocal<java.util.Map.Entry<MethodCall, ParameterizedType>> TO_ARRAY_TARGET = new ThreadLocal<>();

    private static ParameterizedType expectedElement(MethodCall mc) {
        java.util.Map.Entry<MethodCall, ParameterizedType> t = TO_ARRAY_TARGET.get();
        return t != null && t.getKey() == mc ? t.getValue() : null;
    }

    private static boolean toArray(io.codelaser.maddi.cst.api.info.MethodInfo m) {
        // not a stream's: IntStream.toArray() is an IntArray already, Stream.toArray() an Array<Any?>
        return m != null && "toArray".equals(m.name()) && m.parameters().isEmpty()
               && m.typeInfo().fullyQualifiedName().startsWith("java.util.")
               && !m.typeInfo().fullyQualifiedName().startsWith("java.util.stream.");
    }

    /** The receiver's Kotlin element type admits null: a {@code Collection<out Number?>}, a {@code List<E?>}. */
    private static boolean nullableElements(Expression receiver) {
        ParameterizedType type = KotlinNullability.kotlinType(receiver);
        if (type == null || type.parameters().size() != 1) return false;
        ParameterizedType element = type.parameters().getFirst();
        return KotlinNullability.isNullable(element);
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
        String constructed = KotlinTypeName.constructed(type, q);
        if (cc.object() == null && type.typeInfo() != null && type.typeInfo().isInnerClass()) {
            constructed = implicitOuter(constructed, type.typeInfo());
        }
        return b.add(new TextImpl(constructed)).add(arguments(cc.parameterExpressions(), cc.constructor(), q));
    }

    /**
     * {@code new Values()} of an inner class, the outer instance implicit (this, or an enclosing this): by its simple
     * name, which Kotlin resolves with that receiver, in the outer class, its subclasses and their inner classes.
     * {@code AbstractMultimap.Values()} is the static-style access an inner class does not allow (guava's multimaps).
     */
    private static String implicitOuter(String constructed, TypeInfo inner) {
        String simple = KotlinNames.name(inner.simpleName());
        int lt = constructed.indexOf('<');
        String head = lt < 0 ? constructed : constructed.substring(0, lt);
        return head.endsWith("." + simple) ? simple + (lt < 0 ? "" : constructed.substring(lt)) : constructed;
    }

    /** {@code new TypeReference<>() {}} into a {@code TypeReference<…>}: a diamond, no members, the target's class. */
    private static boolean typeToken(ConstructorCall cc, ParameterizedType target) {
        TypeInfo anonymous = cc.anonymousClass();
        if (anonymous == null || cc.diamond() == null || !cc.diamond().isYes() || target.arrays() > 0
            || target.parameters().isEmpty() || !anonymous.fields().isEmpty()
            || anonymous.methods().stream().anyMatch(m -> !m.isSynthetic())) {
            return false;
        }
        ParameterizedType supertype = supertypeOf(anonymous);
        return supertype != null && supertype.typeInfo() == target.typeInfo() && sameUpToStates(supertype, target);
    }

    private static ParameterizedType supertypeOf(TypeInfo anonymous) {
        ParameterizedType parent = anonymous.parentClass();
        if (parent != null && !parent.isJavaLangObject()) return parent;
        return anonymous.interfacesImplemented().isEmpty() ? null : anonymous.interfacesImplemented().getFirst();
    }

    private static OutputBuilder anonymousClass(ConstructorCall cc, Qualification q) {
        return anonymousClass(cc, null, q);
    }

    /** {@code supertype}: the class or interface to write, null for the anonymous class's own. */
    private static OutputBuilder anonymousClass(ConstructorCall cc, ParameterizedType supertype, Qualification q) {
        TypeInfo anonymous = cc.anonymousClass();
        OutputBuilder b = new OutputBuilderImpl().add(KotlinKeyword.OBJECT).add(SpaceEnum.ONE)
                .add(SymbolEnum.COLON).add(SpaceEnum.ONE);
        ParameterizedType parent = anonymous.parentClass();
        // new ListMultimapBuilder<K0, @Nullable Object>() {…}: the type arguments' type-use annotations, as a named
        // class's header writes them (maddi#144 kept them on the anonymous class's supertype)
        if (parent != null && !parent.isJavaLangObject()) {
            b.add(new TextImpl(KotlinTypeName.of(supertype != null ? supertype
                            : KotlinTypePrinter.declaredArguments(parent), q)))
                    .add(arguments(cc.parameterExpressions(), q));
        } else if (!anonymous.interfacesImplemented().isEmpty()) {
            b.add(new TextImpl(KotlinTypeName.of(supertype != null ? supertype
                    : KotlinTypePrinter.declaredArguments(anonymous.interfacesImplemented().getFirst()), q)));
        } else {
            b.add(new TextImpl("Any")).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        CompilationUnitPrinterImpl.ImportDataImpl importData = new CompilationUnitPrinterImpl.ImportDataImpl(List.of(), q, q);
        // its methods' returns are their own, also inside a lambda: no return@lambda (guava's Optional, Splitter)
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.METHOD, null));
        OutputBuilder body;
        try {
            body = new KotlinTypePrinter(anonymous, true).print(importData, false);
        } finally {
            KotlinContext.pop();
        }
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
                        // an element into a non-null element type is asserted, as a write a[i] = x is; and widened
                        // to it, as Java does implicitly: double[] d = {1, 2} is doubleArrayOf(1.0, 2.0)
                        : KotlinNullability.toTarget(x, element, true,
                                element == null ? print(x, q) : widened(x, element, q), q))
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
        // Integer.class: Kotlin's Int::class.java is int.class, the primitive's
        if (type.typeInfo().isBoxedExcludingVoid()) return KotlinTypeName.name(type.typeInfo(), q) + "::class.javaObjectType";
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

    private static final ThreadLocal<java.util.Set<Lambda>> ITERABLE_LAMBDAS =
            ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private static OutputBuilder lambda(Lambda lambda, boolean samConstructor, Qualification q) {
        return lambda(lambda, samConstructor ? samConstructor(lambda, q) : null, q);
    }

    /** As {@link #lambda(Lambda, boolean, Qualification)}, with {@code sam} as its SAM constructor, or none. */
    private static OutputBuilder lambda(Lambda lambda, String sam, Qualification q) {
        ParameterizedType functional = lambda.concreteFunctionalType();
        if (KotlinContext.translatingJava() && functional != null && functional.typeInfo() != null
            && "java.lang.Iterable".equals(functional.typeInfo().fullyQualifiedName())
            && lambda.methodInfo().returnType() != null && ITERABLE_LAMBDAS.get().add(lambda)) {
            // return () -> new Iterator<B>() {…} into an Iterable<B> (guava's Converter.convertAll,
            // Traverser.breadthFirst): Kotlin's Iterable is no fun interface, it takes no lambda
            try {
                ParameterizedType iterable = functional.parameters().stream().anyMatch(a -> a.wildcard() != null)
                        ? null : functional;
                if (iterable != null) {
                    return new OutputBuilderImpl().add(KotlinKeyword.OBJECT).add(SpaceEnum.ONE).add(SymbolEnum.COLON)
                            .add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(iterable, q) + " { override fun iterator(): "
                                    + KotlinTypeName.of(lambda.methodInfo().returnType(), q) + " = run "))
                            .add(lambda(lambda, (String) null, q)).add(new TextImpl(" }"));
                }
            } finally {
                ITERABLE_LAMBDAS.get().remove(lambda);
            }
        }
        List<ParameterInfo> params = lambda.parameters();
        params.forEach(p -> KotlinContext.declared(p.name()));
        Block body = lambda.methodBody();
        List<Statement> statements = CstQueries.own(body.statements());

        OutputBuilder inner;
        boolean labelled = false;
        KotlinContext.push(new KotlinContext.Frame(KotlinContext.Kind.LAMBDA, KotlinContext.LAMBDA_LABEL));
        KotlinContext.pushScope(body);
        try {
            // a parameter the body assigns: `var p = p` first, as in a method (a Kotlin lambda parameter is a val)
            List<OutputBuilder> reassigned = KotlinStatementPrinter.reassignedParameters(params, body, q);
            Expression value = reassigned.isEmpty() ? CstQueries.returnedValue(statements) : null;
            if (value != null) {
                ParameterizedType returnType = lambda.methodInfo().returnType();
                if (unwrap(value) instanceof ConstructorCall cc && inferred(cc)) {
                    inner = inferredConstructorCall(cc, q);
                } else if (primitive(returnType) != null && !(unwrap(value) instanceof Assignment)) {
                    // thenComparingLong { o -> if (c) o.intValue() else Long.MIN_VALUE }: Java widens to the
                    // functional interface's long, branch by branch; Kotlin's branches stay Int and Long
                    inner = widened(value, returnType, q);
                } else {
                    inner = printStatement(value, q);
                }
                // as a method's expression body: into the result the pass decides non-null, a value Kotlin types
                // nullable is asserted. '{ query -> minScore!! }': a public signature's boxed Double is Double? (the
                // Java ABI, KotlinNullability.parameterType), the Function<Query, Double> it goes into is not
                // (langchain4j EmbeddingStoreContentRetriever, maddi-mod#38). Not without a verdict: no decision
                ParameterizedType decided = KotlinNullability.verdicts().returnType(lambda.methodInfo());
                if (decided != null && !KotlinNullability.isNullable(decided)
                    && !(unwrap(value) instanceof ConstructorCall cc && inferred(cc))) {
                    inner = KotlinNullability.toTarget(value, decided, true, inner, q);
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
        OutputBuilder increment = Emission.increment(a, target);
        if (increment != null) return increment;
        String op = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        String binary = op.length() > 1 ? op.substring(0, op.length() - 1) : null;
        Primitive narrow = primitive(a.variableTarget().parameterizedType());
        if (binary != null && (narrow == Primitive.BYTE || narrow == Primitive.SHORT)
            && (INFIX.containsKey(binary) || ARITHMETIC.contains(binary))) {
            // array[i] ^= Byte.MAX_VALUE, resultBytes[i] += nextBytes[i]: Java computes in int and narrows back;
            // Kotlin's Byte has no xor, its Byte + Byte is an Int
            String function = INFIX.containsKey(binary) ? INFIX.get(binary) : null;
            OutputBuilder computed = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(intOperand(variableAsExpression(a), q))
                    .add(KotlinSymbols.binary(function != null ? function : binary)).add(intOperand(a.value(), q))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.DOT)
                    .add(new TextImpl(narrow == Primitive.BYTE ? "toByte()" : "toShort()"));
            return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment("=")).add(computed);
        }
        if (binary != null && INFIX.containsKey(binary)) {
            // x &= y -> x = x and (y): Kotlin has no compound form of its infix operators; h ^= length with an int
            // length into a long h widens it, as Java's does
            return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment("="))
                    .add(compoundInfix(a, INFIX.get(binary), q));
        }
        if (!"=".equals(op)) return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment(op)).add(print(a.value(), q));
        ParameterizedType targetType = declaredType(a.variableTarget());
        boolean translated = !(a.variableTarget() instanceof FieldReference fr) || KotlinNullability.translated(fr.fieldInfo().owner());
        return new OutputBuilderImpl().add(target).add(KotlinSymbols.assignment(op))
                .add(KotlinNullability.toTarget(a.value(), targetType, translated,
                        assigned(a.value(), targetType != null ? targetType : a.variableTarget().parameterizedType(), q), q));
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
            value = assigned(a.value(), declaredType(a.variableTarget()), q);
        } else if ("=".equals(op) && widens(a.value(), a.variableTarget().parameterizedType())) {
            // (read = in.read(buf)) != -1 with a long read: in.read(buf).toLong().also { read = it }
            value = assigned(a.value(), a.variableTarget().parameterizedType(), q);
        } else if ("=".equals(op)) {
            value = receiver(a.value(), q);
        } else {
            String binary = op.substring(0, op.length() - 1);
            OutputBuilder computed = INFIX.containsKey(binary)
                    ? compoundInfix(a, INFIX.get(binary), q)
                    : new OutputBuilderImpl().add(variable(a.variableTarget(), q))
                    .add(KotlinSymbols.binary(binary)).add(operand(PrecedenceEnum.MULTIPLICATIVE, a.value(), q));
            value = new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(computed)
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return value.add(SymbolEnum.DOT).add(new TextImpl("also")).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE)
                .add(SpaceEnum.ONE).add(variable(a.variableTarget(), q)).add(KotlinSymbols.assignment("="))
                .add(new TextImpl("it")).add(SpaceEnum.ONE).add(SymbolEnum.RIGHT_BRACE);
    }

    private static OutputBuilder compoundInfix(Assignment a, String function, Qualification q) {
        boolean shift = function.startsWith("sh") || "ushr".equals(function);
        Primitive to = primitive(a.variableTarget().parameterizedType());
        return new OutputBuilderImpl().add(valueOperand(variableAsExpression(a), q)).add(KotlinSymbols.binary(function))
                .add(shift ? valueOperand(a.value(), q) : bitOperand(a.value(), to, q));
    }

    private static Expression variableAsExpression(Assignment a) {
        return a.target();
    }

    // ---------------------------------------------------------------- operators

    private static OutputBuilder binaryOperator(BinaryOperator bo, Qualification q) {
        String op = bo.operator().name();
        if ("&&".equals(op) || "||".equals(op)) {
            OutputBuilder idiom = nullOrEmpty(bo.lhs(), bo.rhs(), "||".equals(op), q);
            if (idiom != null) return idiom;
            // x is T && x.m(): the right-hand side runs where the left one is true (false for ||), and knows its tests
            OutputBuilder left = operand(bo.precedence(), bo.lhs(), q);
            OutputBuilder right = KotlinSmartCasts.withTypes("&&".equals(op) ? KotlinSmartCasts.whenTrue(bo.lhs())
                    : KotlinSmartCasts.whenFalse(bo.lhs()), () -> operand(bo.precedence(), bo.rhs(), q));
            return new OutputBuilderImpl().add(left).add(KotlinSymbols.binary(op)).add(right);
        }
        String infix = INFIX.get(op);
        if (infix != null) return infix(bo, infix, q);
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
        if (COMPARISON.contains(op) && (primitive(bo.lhs().parameterizedType()) == Primitive.CHAR)
                                       != (primitive(bo.rhs().parameterizedType()) == Primitive.CHAR)) {
            // ch < 128: Java compares the char's code; Kotlin compares a Char with a Char only
            return new OutputBuilderImpl().add(promoted(bo.lhs(), bo.precedence(), q)).add(KotlinSymbols.binary(op))
                    .add(promoted(bo.rhs(), bo.precedence(), q));
        }
        if (RELATIONAL.contains(op) && !(bo.parameterizedType() != null && bo.parameterizedType().isJavaLangString())
            && (primitive(bo.lhs().parameterizedType()) != null
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

    private static final java.util.Set<String> COMPARISON = java.util.Set.of("<", ">", "<=", ">=");

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
            return KotlinNullability.asserted(e, KotlinPrintCode.ASSERT_AT_UNBOXING, q);
        }
        return operand(precedence, e, q);
    }

    private static OutputBuilder valueOperand(Expression e, Qualification q) {
        if (!(unwrap(e) instanceof NullConstant) && KotlinNullability.nullableInKotlin(e)) {
            return KotlinNullability.asserted(e, KotlinPrintCode.ASSERT_AT_UNBOXING, q);
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

    /**
     * {@code a and b}: Kotlin's infix functions bind tighter than comparisons, so anything compound is enclosed. Java
     * promotes a byte, short or char operand to int (an int one to long beside a long); Kotlin has no {@code Byte and
     * Int}: {@code b.toInt() and 255}. A shift's distance is an Int in Kotlin.
     */
    private static OutputBuilder infix(BinaryOperator bo, String function, Qualification q) {
        Primitive result = primitive(bo.parameterizedType());
        boolean shift = function.startsWith("sh") || "ushr".equals(function);
        return new OutputBuilderImpl().add(bitOperand(bo.lhs(), result, q)).add(KotlinSymbols.binary(function))
               .add(bitOperand(bo.rhs(), shift ? Primitive.INT : result, q));
    }

    private static OutputBuilder infix(Expression lhs, String function, Expression rhs, Qualification q) {
        return new OutputBuilderImpl().add(valueOperand(lhs, q)).add(KotlinSymbols.binary(function))
               .add(valueOperand(rhs, q));
    }

    /** {@code e} as Java's int promotion has it: a byte or short {@code .toInt()}, a char {@code .code}. */
    private static OutputBuilder intOperand(Expression e, Qualification q) {
        Primitive p = primitive(e.parameterizedType());
        if (unwrap(e) instanceof IntConstant) return valueOperand(e, q);
        if (p == Primitive.BYTE || p == Primitive.SHORT) return receiver(e, q).add(SymbolEnum.DOT).add(new TextImpl("toInt()"));
        if (p == Primitive.CHAR) return receiver(e, q).add(SymbolEnum.DOT).add(new TextImpl("code"));
        return valueOperand(e, q);
    }

    private static OutputBuilder bitOperand(Expression e, Primitive to, Qualification q) {
        Primitive from = primitive(e.parameterizedType());
        OutputBuilder operand = valueOperand(e, q);
        // an integer literal takes the type the function expects
        if (to == null || from == null || from == to || to == Primitive.BOOLEAN || from == Primitive.BOOLEAN
            || unwrap(e) instanceof IntConstant) {
            return operand;
        }
        for (String call : conversion(from, to).split("\\.")) {
            if (!call.isEmpty()) operand.add(SymbolEnum.DOT).add(new TextImpl(call));
        }
        return operand;
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
        if (inner instanceof MethodCall mc && equality(mc) != null) return equality(equality(mc), "!=", q);
        return new OutputBuilderImpl()
                .add(neg.expression().isNumeric() ? SymbolEnum.UNARY_MINUS : SymbolEnum.UNARY_BOOLEAN_NOT)
                .add(operand(neg.precedence(), neg.expression(), q));
    }

    /**
     * The operands of a {@code &&} ({@code and}) or {@code ||}, in the order printed, which Kotlin evaluates: each knows
     * the {@code is} tests of those before it.
     */
    private static OutputBuilder junction(List<Expression> operands, io.codelaser.maddi.cst.api.expression.Precedence precedence,
                                          boolean and, Qualification q) {
        if (operands.size() == 2) {
            OutputBuilder idiom = nullOrEmpty(operands.get(0), operands.get(1), !and, q);
            if (idiom != null) return idiom;
        }
        List<OutputBuilder> printed = new ArrayList<>();
        KotlinSmartCasts.Facts known = new KotlinSmartCasts.Facts();
        for (Expression operand : operands) {
            KotlinSmartCasts.Facts before = new KotlinSmartCasts.Facts(new java.util.HashMap<>(known.types()),
                    new java.util.HashSet<>(known.nonNull()));
            printed.add(KotlinSmartCasts.withTypes(before, () -> condition(precedence, operand, q)));
            KotlinSmartCasts.Facts more = and ? KotlinSmartCasts.whenTrue(operand) : KotlinSmartCasts.whenFalse(operand);
            known.types().putAll(more.types());
            known.nonNull().addAll(more.nonNull());
        }
        return printed.stream().collect(OutputBuilderImpl.joining(and ? SymbolEnum.LOGICAL_AND : SymbolEnum.LOGICAL_OR));
    }

    /**
     * {@code x == null || x.isEmpty()} as {@code x.isNullOrEmpty()}, and {@code x != null && !x.isEmpty()} as
     * {@code !x.isNullOrEmpty()}, for a variable x of a String, CharSequence, collection or map type; null otherwise.
     * Kotlin's extension takes a nullable receiver, so neither x's verdict nor a {@code !!} matters.
     */
    private static OutputBuilder nullOrEmpty(Expression first, Expression second, boolean or, Qualification q) {
        Expression tested = nullTest(first, or);
        if (tested == null || !(unwrap(tested) instanceof VariableExpression ve)) return null;
        Expression emptyTest = or ? second : negated(second);
        if (!(emptyTest != null && unwrap(emptyTest) instanceof MethodCall mc) || !"isEmpty".equals(mc.methodInfo().name())
            || !mc.parameterExpressions().isEmpty() || mc.object() == null
            || !(unwrap(mc.object()) instanceof VariableExpression receiver) || !ve.variable().equals(receiver.variable())) {
            return null;
        }
        ParameterizedType type = ve.variable().parameterizedType();
        if (type == null || type.arrays() > 0 || type.typeInfo() == null
            || !(type.isJavaLangString() || "java.lang.CharSequence".equals(type.typeInfo().fullyQualifiedName())
                 || KotlinReadOnly.isCollection(type))) {
            return null;
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (!or) b.add(SymbolEnum.UNARY_BOOLEAN_NOT);
        return b.add(print(ve, q)).add(SymbolEnum.DOT).add(new TextImpl("isNullOrEmpty"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
    }

    /** {@code x == null} ({@code isNull}) or {@code x != null}: x; null for anything else. */
    private static Expression nullTest(Expression e, boolean isNull) {
        Expression u = unwrap(e);
        Expression inner = negated(u);
        if (inner != null) return nullTest(inner, !isNull);
        if (u instanceof BinaryOperator bo && bo.operator() != null
            && (isNull ? "==" : "!=").equals(bo.operator().name())) {
            if (unwrap(bo.rhs()) instanceof NullConstant) return bo.lhs();
            if (unwrap(bo.lhs()) instanceof NullConstant) return bo.rhs();
        }
        return null;
    }

    /** The operand of a boolean {@code !}; null when {@code e} is no negation. */
    private static Expression negated(Expression e) {
        Expression u = unwrap(e);
        if (u instanceof Negation neg && !neg.expression().isNumeric()) return neg.expression();
        if (u instanceof UnaryOperator uo && uo.operator() != null && "!".equals(uo.operator().name())) return uo.expression();
        return null;
    }

    /** An operand of && or ||: Java unboxes a Boolean there ({@code last && map.get(k)}), Kotlin needs the {@code !!}. */
    private static OutputBuilder condition(io.codelaser.maddi.cst.api.expression.Precedence precedence, Expression e, Qualification q) {
        if (!(unwrap(e) instanceof NullConstant) && KotlinNullability.nullableInKotlin(e)) {
            return KotlinNullability.asserted(e, KotlinPrintCode.ASSERT_AT_UNBOXING, q);
        }
        return operand(precedence, e, q);
    }

    private static OutputBuilder unaryOperator(UnaryOperator uo, Qualification q) {
        Expression inner = unwrap(uo.expression());
        if (inner instanceof InstanceOf io) return notInstanceOf(io, q); // a unary op wrapping `is` is `!` -> `!is`
        if ("!".equals(uo.operator().name()) && inner instanceof MethodCall mc && equality(mc) != null) {
            return equality(equality(mc), "!=", q); // !a.equals(b) -> a != b
        }
        Primitive operand = primitive(uo.expression().parameterizedType());
        boolean promoted = operand == Primitive.BYTE || operand == Primitive.SHORT || operand == Primitive.CHAR;
        if ("~".equals(uo.operator().name())) {
            // ~readShort(): Java promotes to int, Kotlin's Short and Byte have no inv()
            return (promoted ? intOperand(uo.expression(), q) : receiver(uo.expression(), q))
                    .add(SymbolEnum.DOT).add(new TextImpl("inv()"));
        }
        if ("-".equals(uo.operator().name()) && operand == Primitive.CHAR) {
            // -c1: Java's int, Kotlin's Char has no unaryMinus
            return new OutputBuilderImpl().add(new TextImpl("-")).add(intOperand(uo.expression(), q));
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
        return cast(cast, cast.parameterizedType(), nullable, q);
    }

    /** As Java's cast, to {@code castType}: Java's type with the type argument states of what it initializes. */
    private static OutputBuilder cast(Cast cast, ParameterizedType castType, boolean nullable, Qualification q) {
        if (smartCast(cast)) return print(cast.expression(), q);
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
        if (KotlinContext.translatingJava() && (unwrap(cast.expression()) instanceof MethodReference
                                                 || unwrap(cast.expression()) instanceof Lambda)
            && castType.typeInfo() != null && castType.typeInfo().isFunctionalInterface() && castType.arrays() == 0
            && !castType.parameters().isEmpty() && castType.parameters().stream().allMatch(p -> p.wildcard() == null)) {
            // (Supplier<ImmutableTable.Builder<R, C, V>>) ImmutableTable.Builder::new: the cast is what types the
            // reference for Java; Kotlin's `{ ImmutableTable.Builder() } as Supplier<…>` infers nothing from it, a SAM
            // constructor does
            return new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(castType.withNullable(
                            io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q)))
                    .add(SymbolEnum.LEFT_PARENTHESIS).add(print(cast.expression(), q)).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        // Java's (String) map.get(k) lets null through, Kotlin's `as String` throws: as String?
        ParameterizedType type = nullable
                ? castType.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NULLABLE)
                : castType;
        String typeName = rawComparable(cast.parameterizedType())
                // ((Comparable) actual).compareTo(expected): Comparable<*> takes nothing, Comparable<Any?> anything,
                // unchecked, as Java's raw type does
                ? "Comparable<Any?>" + (nullable ? "?" : "") : KotlinTypeName.of(type, q);
        return new OutputBuilderImpl().add(operand(cast.precedence(), cast.expression(), q)).add(SpaceEnum.ONE)
                .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                .add(new TextImpl(typeName));
    }

    /**
     * {@code (T) x} where an {@code x instanceof T} that holds here was tested: Kotlin has smart-cast x to T already
     * (kotlinc: "no cast needed"). Only to a type without type arguments or array dimensions, the test's own.
     */
    private static boolean smartCast(Cast cast) {
        if (!KotlinContext.translatingJava() || !(unwrap(cast.expression()) instanceof VariableExpression ve)) return false;
        ParameterizedType known = KotlinSmartCasts.knownType(ve.variable());
        ParameterizedType type = cast.parameterizedType();
        return known != null && type != null && type.typeInfo() != null && type.arrays() == 0
               && type.parameters().isEmpty() && known.arrays() == 0 && known.typeInfo() == type.typeInfo()
               && known.parameters().isEmpty();
    }

    /**
     * The element type, printed, of a parameter of the current method that Kotlin's override signature makes a
     * read-only {@code Collection<E>} ({@link KotlinMappedMembers#overriddenParameterType}: removeAll, retainAll,
     * addAll, containsAll), when {@code e} reads it; null otherwise.
     */
    private static String forcedReadOnlyElement(Expression e, Qualification q) {
        if (!(unwrap(e) instanceof VariableExpression ve) || !(ve.variable() instanceof ParameterInfo pi)) return null;
        io.codelaser.maddi.cst.api.info.MethodInfo current = KotlinContext.currentMethod();
        if (current == null || pi.methodInfo() != current) return null;
        ParameterizedType t = KotlinMappedMembers.overriddenParameterType(current, pi.index());
        if (t == null || t.typeInfo() == null || !"java.util.Collection".equals(t.typeInfo().fullyQualifiedName())
            || t.parameters().isEmpty()) {
            return null;
        }
        return KotlinTypeName.of(t.parameters().getFirst(), q);
    }

    /**
     * An array index, an Int: Java widens a char, byte or short index ({@code replacements[c]} in guava's escapers),
     * Kotlin does not ({@code replacements[c.code]}).
     */
    private static OutputBuilder arrayIndex(Expression index, Qualification q) {
        Primitive from = primitive(index.parameterizedType());
        if (from != Primitive.CHAR && from != Primitive.BYTE && from != Primitive.SHORT) return print(index, q);
        OutputBuilder receiver = valueOperand(index, q);
        for (String call : conversion(from, Primitive.INT).split("\\.")) {
            if (!call.isEmpty()) receiver.add(SymbolEnum.DOT).add(new TextImpl(call));
        }
        return receiver;
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
        return LITERALS.stringLiteral(s);
    }

    static String charLiteral(char c) {
        return LITERALS.charLiteral(c);
    }

    private static String doubleLiteral(double d) {
        return LITERALS.doubleLiteral(d);
    }

    private static String floatLiteral(float f) {
        return LITERALS.floatLiteral(f);
    }

    // ---------------------------------------------------------------- precedence

    private static final java.util.Set<String> BOXED_OR_STRING = java.util.Set.of("java.lang.Integer",
            "java.lang.Long", "java.lang.Short", "java.lang.Byte", "java.lang.Character", "java.lang.Boolean",
            "java.lang.String");
    private static final java.util.Map<String, String> BOX = java.util.Map.of("int", "java.lang.Integer",
            "long", "java.lang.Long", "short", "java.lang.Short", "byte", "java.lang.Byte", "char",
            "java.lang.Character", "boolean", "java.lang.Boolean", "double", "java.lang.Double",
            "float", "java.lang.Float");
    // what a boxed type or String may be compared with in Kotlin, besides itself
    private static final java.util.Set<String> SUPERTYPES_OF_BOXED = java.util.Set.of("java.lang.Object",
            "java.lang.Number", "java.lang.Comparable", "java.io.Serializable", "java.lang.CharSequence");

    /**
     * {@code Objects.equals(a, b)} and {@code a.equals(b)} as Kotlin's {@code a == b}, which is
     * {@code a?.equals(b) ?: (b === null)} (CodeLaser/maddi#140, item 1). The two operands, or null where that does
     * not hold or does not compile:
     * <ul>
     *     <li>{@code a.equals(b)} on an {@code a} Kotlin types nullable throws in Java and not in Kotlin: kept;</li>
     *     <li>{@code super.equals(o)} calls another method than {@code ==}: kept;</li>
     *     <li>Double and Float: on non-null floating-point types {@code ==} compares as IEEE does ({@code NaN != NaN},
     *     {@code 0.0 == -0.0}), equals does not: kept;</li>
     *     <li>{@code ==} does not compile between incompatible types ({@code Int == Long}), nor is it wanted on
     *     arrays: kept.</li>
     * </ul>
     */
    private static Expression[] equality(MethodCall mc) {
        if (!KotlinContext.translatingJava() || !"equals".equals(mc.methodInfo().name())) return null;
        List<Expression> args = mc.parameterExpressions();
        Expression a, b;
        if (mc.methodInfo().isStatic() && args.size() == 2
            && "java.util.Objects".equals(mc.methodInfo().typeInfo().fullyQualifiedName())) {
            a = args.get(0);
            b = args.get(1);
        } else if (!mc.methodInfo().isStatic() && args.size() == 1 && mc.object() != null && !mc.objectIsImplicit()
                   && mc.methodInfo().parameters().getFirst().parameterizedType().isJavaLangObject()
                   && !(unwrap(mc.object()) instanceof VariableExpression ve && ve.variable() instanceof This t
                        && t.writeSuper())
                   && !(unwrap(mc.object()) instanceof TypeExpression)
                   && !KotlinNullability.nullableInKotlin(mc.object())) {
            a = mc.object();
            b = args.getFirst();
        } else {
            return null;
        }
        return comparableInKotlin(a.parameterizedType(), b.parameterizedType()) ? new Expression[]{a, b} : null;
    }

    private static boolean comparableInKotlin(ParameterizedType a, ParameterizedType b) {
        if (a.arrays() > 0 || b.arrays() > 0) return false;
        String na = boxedName(a), nb = boxedName(b);
        if ("java.lang.Double".equals(na) || "java.lang.Float".equals(na) || "java.lang.Double".equals(nb)
            || "java.lang.Float".equals(nb)) {
            return false;
        }
        if (na == null || nb == null || na.equals(nb)) return true;
        if (BOXED_OR_STRING.contains(na)) return SUPERTYPES_OF_BOXED.contains(nb);
        if (BOXED_OR_STRING.contains(nb)) return SUPERTYPES_OF_BOXED.contains(na);
        TypeInfo ta = a.typeInfo(), tb = b.typeInfo();
        return ta == null || tb == null || ta.isInterface() || tb.isInterface()
               || "java.lang.Object".equals(na) || "java.lang.Object".equals(nb)
               || ta.recursiveSuperTypeStream().anyMatch(tb::equals) || tb.recursiveSuperTypeStream().anyMatch(ta::equals);
    }

    // the fully qualified name, boxed; null for a type parameter
    private static String boxedName(ParameterizedType type) {
        if (type.typeInfo() == null) return null;
        String fqn = type.typeInfo().fullyQualifiedName();
        return BOX.getOrDefault(fqn, fqn);
    }

    private static OutputBuilder equality(Expression[] operands, String operator, Qualification q) {
        return new OutputBuilderImpl().add(operand(PrecedenceEnum.EQUALITY, operands[0], q))
                .add(KotlinSymbols.binary(operator)).add(operand(PrecedenceEnum.EQUALITY, operands[1], q));
    }

    // x != null ? x : d is x ?: d, x != null ? x.m() : null is x?.m() (KotlinNullCheckShapes); otherwise an if
    private static OutputBuilder inlineConditional(InlineConditional ic, Qualification q) {
        KotlinNullCheckShapes.Elvis elvis = KotlinNullCheckShapes.elvis(ic);
        if (elvis != null) return elvis(elvis, print(elvis.otherwise(), q), q);
        KotlinNullCheckShapes.SafeCall safeCall = KotlinNullCheckShapes.safeCall(ic);
        if (safeCall != null) {
            OutputBuilder printed = KotlinNullability.withSafeReceiver(safeCall.receiver(),
                    () -> print(safeCall.call(), q));
            if (printed != null) return printed;
        }
        return new OutputBuilderImpl()
                .add(KotlinKeyword.IF).add(SpaceEnum.ONE).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(print(ic.condition(), q)).add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE)
                .add(KotlinSmartCasts.withTypes(KotlinSmartCasts.whenTrue(ic.condition()),
                        () -> branch(ic.ifTrue(), () -> print(ic.ifTrue(), q), q))).add(SpaceEnum.ONE)
                .add(KotlinKeyword.ELSE).add(SpaceEnum.ONE)
                .add(KotlinSmartCasts.withTypes(KotlinSmartCasts.whenFalse(ic.condition()),
                        () -> branch(ic.ifFalse(), () -> print(ic.ifFalse(), q), q)));
    }

    // the right operand in parentheses when it binds looser than '+': '?:' binds tighter than comparison, equality,
    // && and ||, and looser than arithmetic, ranges, infix calls and 'as'
    private static OutputBuilder elvis(KotlinNullCheckShapes.Elvis elvis, OutputBuilder otherwise, Qualification q) {
        Expression right = unwrap(elvis.otherwise());
        boolean bare = !PrecedenceEnum.ADDITIVE.greaterThan(right.precedence())
                       && !(right instanceof MethodCall mc && equality(mc) != null);
        OutputBuilder rightPrinted = bare ? otherwise : new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(otherwise).add(SymbolEnum.RIGHT_PARENTHESIS);
        return new OutputBuilderImpl().add(print(elvis.value(), q)).add(SymbolEnum.binaryOperator("?:"))
                .add(rightPrinted);
    }

    /** True when this desugared inline conditional was a Kotlin elvis, marked in {@link DetailedSources}. */
    private static boolean isElvis(InlineConditional ic) {
        Source source = ic.source();
        return source != null && source.detailedSources() != null
               && source.detailedSources().detail(DetailedSources.NULL_COALESCING) != null;
    }

    static Expression unwrap(Expression e) {
        return io.codelaser.maddi.cst.print.translate.CstQueries.unwrap(e);
    }

    /** Recurse into an operand, parenthesising when the enclosing operator binds tighter (as the Java printer does). */
    static OutputBuilder operand(io.codelaser.maddi.cst.api.expression.Precedence precedence, Expression e,
                                 Qualification q) {
        OutputBuilder inner = print(e, q);
        // a call printed as a == b binds as == does
        io.codelaser.maddi.cst.api.expression.Precedence own = unwrap(e) instanceof MethodCall mc
                                                               && equality(mc) != null ? PrecedenceEnum.EQUALITY
                : e.precedence();
        if (precedence.greaterThan(own) || needsParenthesesInKotlin(e)) {
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
