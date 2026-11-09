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
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.ArrayLength;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;
import io.codelaser.maddi.cst.print.translate.Emission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
class CSharpExpressionPrinter2 {
    /** Java members whose C# template's result is nullable, though the template is shared ({@code $0[$1]}). */
    private static final Set<String> NULLABLE_MEMBERS = Set.of(
            "com.fasterxml.jackson.databind.JsonNode.get", "java.lang.Object.toString");

    /** The JDK's interfaces that values of every kind implement: a Boolean, a Double, a list may be one. */
    private static final Set<String> ANY_VALUE = Set.of("java.io.Serializable", "java.lang.Comparable",
            "java.lang.Cloneable", "java.lang.constant.Constable", "java.lang.constant.ConstantDesc");

    /**
 * The library types whose values C# converts to a string otherwise than Java: the collections, maps and entries
 * (their .NET type's name), and Number, which a Double or a Float is.
 */
    private static final Set<String> CONVERTED_DIFFERENTLY = Set.of("java.lang.Iterable", "java.util.Collection",
            "java.util.Map", "java.util.Map$Entry", "java.util.Map.Entry", "java.lang.Number");

    /** The index of the parenthesis that closes {@code t}'s first; -1 when none. */
    static int matching(String t) {
        int depth = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return i;
        }
        return -1;
    }

    static OutputBuilder variable(Variable v, Qualification q) {
        return switch (v) {
            case This t -> CSharpExpressionPrinter1.text(thisOrBase(t));
            case FieldReference fr -> CSharpExpressionPrinter.fieldReference(fr, q);
            case DependentVariable dv -> new OutputBuilderImpl().add(CSharpNullability.dereferenced(dv.arrayExpression(), q))
                    .add(SymbolEnum.LEFT_BRACKET).add(CSharpExpressionPrinter1.print(dv.indexExpression(), q)).add(SymbolEnum.RIGHT_BRACKET);
            case ParameterInfo pi when CSharpContext.program().typeToken(pi.methodInfo()) == pi.index() ->
                    CSharpExpressionPrinter1.text("typeof(" + CSharpNames.typeParameter(pi.methodInfo().typeParameters().getFirst()) + ")");
            default -> CSharpExpressionPrinter1.text(CSharpContext.local(v.simpleName()));
        };
    }

    static String thisOrBase(This t) {
        if (t.writeSuper()) return "base";
        // Outer.this in a hoisted anonymous class
        if (t.explicitlyWriteType() != null && t.explicitlyWriteType() != CSharpContext.currentType()
            && CSharpContext.hoistedWithOuter() != null) {
            return "outer";
        }
        // Outer.this in an inner class
        if (t.explicitlyWriteType() != null && t.explicitlyWriteType() != CSharpContext.currentType()) {
            String path = CSharpInner.pathTo(t.explicitlyWriteType());
            if (path != null) return path;
        }
        if (t.explicitlyWriteType() != null && t.explicitlyWriteType() != CSharpContext.currentType()) {
            CSharpContext.message(CSharpPrintCode.OUTER_THIS, null, t.explicitlyWriteType().simpleName() + ".this");
        }
        return "this";
    }

    /** The JDK → BCL rule of a call; null for a call of a translated method, or a JDK method without one. */
    static LibraryMapping.Rule rule(MethodCall mc) {
        MethodInfo method = mc.methodInfo();
        TypeInfo owner = method.typeInfo();
        if (CSharpNames.translated(owner)) {
            // the implicit values() and valueOf(String) of an enum C# declares as an enum
            if (owner.typeNature().isEnum() && method.isStatic() && CSharpNames.simpleEnum(owner)) {
                String self = CSharpTypeName.name(owner, null);
                if ("values".equals(method.name()) && method.parameters().isEmpty()) {
                    return new LibraryMapping.Rule("Enum.GetValues<" + self + ">()", null, List.of("System"));
                }
                if ("valueOf".equals(method.name()) && method.parameters().size() == 1) {
                    return new LibraryMapping.Rule("Enum.Parse<" + self + ">($1)", null, List.of("System"));
                }
            }
            return null;
        }
        return CSharpContext.rule(mc);
    }

    /**
 * Whether C# declares the result of the call {@code mc} nullable, a library's member that the analysis's facts
 * do not know: {@code object.ToString()}'s {@code string?}, {@code node["k"]}'s {@code JsonNode?}, a rule's
 * ({@link LibraryMapping.Rule#nullableResult}).
 */
    static boolean nullableResult(MethodCall mc) {
        MethodInfo method = mc.methodInfo();
        if (method.isVoid() || method.returnType().isPrimitiveExcludingVoid()) return false;
        if (NULLABLE_MEMBERS.contains(method.typeInfo().fullyQualifiedName() + "." + method.name())) return true;
        LibraryMapping.Rule rule = rule(mc);
        return rule != null && rule.value() != null && rule.nullableResult();
    }

    /** A call whose C# form is not an atom ({@code list.Count == 0}): in parentheses as an operand or a receiver. */
    static boolean nonAtomicCall(Expression e) {
        if (!(e instanceof MethodCall mc)) return false;
        if (rule(mc) instanceof LibraryMapping.Rule r) {
            // Arrays.stream(a) is a itself: as atomic as a, not Objects.requireNonNull(a)'s a ?? throw …
            Expression passed = passedThrough(mc, r);
            if (passed != null) return !atomic(passed);
            if (!r.atomic()) return true;
        }
        return erasedResultCast(mc);
    }

    /** The argument or receiver a rule's template is, {@code $1}; null for any other template. */
    private static Expression passedThrough(MethodCall mc, LibraryMapping.Rule r) {
        String value = r.value();
        if (value == null || !value.matches("\\$\\d")) return null;
        int n = value.charAt(1) - '0';
        if (n == 0) return mc.object();
        return n <= mc.parameterExpressions().size() ? mc.parameterExpressions().get(n - 1) : null;
    }

    /** An expression {@link #receiver} prints without parentheses. */
    static boolean atomic(Expression e) {
        return switch (e) {
            case VariableExpression ve -> true;
            case MethodCall mc -> !nonAtomicCall(mc);
            case EnclosedExpression ee -> true;
            case ArrayLength al -> true;
            case ClassExpression ce -> true;
            case TypeExpression te -> true;
            case StringConstant sc -> true;
            case ConstructorCall cc -> cc.parameterizedType().arrays() == 0 && cc.anonymousClass() == null;
            case null, default -> false;
        };
    }

    /** The type Java inferred for a call of a method whose type parameters C# erased; null when none is needed. */
    static String erasedResultCast(MethodCall mc, Qualification q) {
        return erasedResultCast(mc) ? CSharpTypeName.of(mc.concreteReturnType(), q) : null;
    }

    private static boolean erasedResultCast(MethodCall mc) {
        if (!CSharpContext.program().erasedGeneric(mc.methodInfo())) return false;
        // a result the erasure leaves as it is, buildTermination's Termination<Solution_>: nothing to cast
        if (!CstQueries.mentionsAny(mc.methodInfo().returnType(), mc.methodInfo().typeParameters())) return false;
        ParameterizedType concrete = mc.concreteReturnType();
        // a type that mentions an enclosing erased method's type parameter is printed erased already
        List<TypeParameter> enclosing = CSharpContext.erasedTypeParameters();
        return concrete != null && !concrete.isJavaLangObject() && CSharpExpressionPrinter.writable(concrete, mc.methodInfo().typeParameters())
               && enclosing.stream().noneMatch(tp -> CstQueries.mentions(concrete, tp));
    }

    /**
 * MovableFilter's test, BiPredicate's, through the program's interface: the interface's own Test
 * ({@link CSharpNames#librarySingleAbstractMethod}), not the delegate's invocation that the library's rule has.
 */
    static OutputBuilder librarySingleAbstractMethodCall(MethodCall mc, Qualification q) {
        MethodInfo method = mc.methodInfo();
        Expression object = mc.object();
        if (method.isStatic() || CSharpNames.translated(method.typeInfo())) return null;
        TypeInfo receiverType = object == null || mc.objectIsImplicit() ? CSharpContext.currentType()
                : object.parameterizedType().typeInfo();
        MethodInfo sam = CSharpNames.librarySingleAbstractMethod(receiverType);
        if (sam == null || !sam.name().equals(method.name()) || !sam.typeInfo().equals(method.typeInfo())
            || sam.parameters().size() != method.parameters().size()) {
            return null;
        }
        OutputBuilder b = new OutputBuilderImpl();
        if (object != null && !mc.objectIsImplicit()) b.add(CSharpNullability.dereferenced(object, q)).add(SymbolEnum.DOT);
        return b.add(CSharpExpressionPrinter1.text(CSharpNames.pascal(method.name()))).add(CSharpExpressionPrinter1.arguments(mc.parameterExpressions(), method, q));
    }

    /** Each of the method's type parameters is unbounded and mentioned only as a {@code Class<T>}, C#'s {@code Type}. */
    static boolean onlyInClass(MethodInfo method) {
        List<ParameterizedType> signature = new ArrayList<>();
        signature.add(method.returnType());
        method.parameters().forEach(p -> signature.add(p.parameterizedType()));
        return method.typeParameters().stream().allMatch(tp -> tp.typeBounds().stream().allMatch(ParameterizedType::isJavaLangObject)
                && signature.stream().allMatch(t -> onlyInClass(t, tp)));
    }

    private static boolean onlyInClass(ParameterizedType t, TypeParameter tp) {
        if (t.isTypeParameter()) return !t.typeParameter().equals(tp);
        if (t.typeInfo() != null && "java.lang.Class".equals(t.typeInfo().fullyQualifiedName())) return true;
        return t.parameters().stream().allMatch(a -> onlyInClass(a, tp));
    }

    /**
 * Each type argument as its generic bound has it, the others substituted, as C# prints them: a captured
 * {@code VariableListener<Solution_, ?>}, C#'s {@code IVariableListener<object, IChangeEvent>}, is no
 * {@code Listener_ extends VariableListener<Solution_, ChangeEvent_>} (CS0311), where C#'s CS0411 is as good.
 */
    static boolean meetBounds(List<TypeParameter> typeParameters, Map<TypeParameter, ParameterizedType> inferred,
                                      Qualification q) {
        for (TypeParameter tp : typeParameters) {
            ParameterizedType t = inferred.get(tp);
            if (t == null || t.wildcard() != null || t.typeInfo() == null) continue;
            for (ParameterizedType bound : tp.typeBounds()) {
                if (bound.typeInfo() == null || bound.parameters().isEmpty()) continue;
                ParameterizedType expected = CstSupertypes.substitute(bound, inferred);
                ParameterizedType seen = t.typeInfo().equals(bound.typeInfo()) ? t : CstSupertypes.asSeen(t, bound.typeInfo());
                if (seen == null || CstQueries.mentionsAny(expected, typeParameters)) continue;
                if (!CSharpTypeName.argument(seen, q).equals(CSharpTypeName.argument(expected, q))) return false;
            }
        }
        return true;
    }

    /** As {@link CstQueries#mentions}, but not through a {@code Class<T>}, which is C#'s {@code Type}, without arguments. */
    static boolean mentionsInCSharp(ParameterizedType t, TypeParameter tp) {
        if (t == null) return false;
        if (t.isTypeParameter()) return tp.equals(t.typeParameter());
        if (t.typeInfo() != null && "java.lang.Class".equals(t.typeInfo().fullyQualifiedName())) return false;
        return t.parameters().stream().anyMatch(a -> mentionsInCSharp(a, tp));
    }

    static boolean hasWildcard(ParameterizedType t) {
        return t.wildcard() != null || t.parameters().stream().anyMatch(CSharpExpressionPrinter2::hasWildcard);
    }

    /**
 * A wildcard of a class printed as its wildcard interface ({@link CSharpWildcardInterfaces}), {@code
 * AbstractConcatNode<?, ?, ?>}, where a superclass of it is expected: Java's is one, C#'s interface cannot extend
 * the class, a cast is.
 */
    static boolean wildcardToSuperclass(Expression inner, ParameterizedType target) {
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || target.arrays() != 0 || from.typeInfo() == null
            || target.typeInfo() == null || from.typeInfo().isInterface() || target.typeInfo().isInterface()
            || target.isJavaLangObject() || from.typeInfo().equals(target.typeInfo())
            || !CSharpWildcardInterfaces.isWildcardUse(from) || CSharpWildcardInterfaces.isWildcardUse(target)) {
            return false;
        }
        return from.typeInfo().recursiveSuperTypeStream().anyMatch(target.typeInfo()::equals);
    }

    /** {@code m.keySet()} where a Set, C#'s ISet, is expected; a map whose C# type has its own Keys, not a read-only one. */
    static boolean keySetAsSet(Expression inner, ParameterizedType target) {
        if (!(inner instanceof MethodCall mc) || mc.object() == null || !"keySet".equals(mc.methodInfo().name())
            || !mc.methodInfo().parameters().isEmpty() || target.arrays() != 0 || target.typeInfo() == null) {
            return false;
        }
        TypeInfo declaring = mc.methodInfo().typeInfo();
        boolean mapKeySet = "java.util.Map".equals(declaring.fullyQualifiedName())
                            || mc.methodInfo().overrides().stream().anyMatch(o -> "java.util.Map".equals(o.typeInfo()
                .fullyQualifiedName()));
        if (!mapKeySet || CSharpNames.translated(declaring)) return false;
        if (CSharpContext.program().readOnly(mc.object())) return false;
        String fqn = target.typeInfo().fullyQualifiedName();
        return "java.util.Set".equals(fqn) || "java.util.SequencedSet".equals(fqn);
    }

    /**
 * A cast to a raw generic type, {@code (Map) m}, whose value goes where the type is expected with type arguments,
 * none a wildcard or a type parameter C# would not have in scope: the cast is to the target.
 */
    static boolean rawCastInto(Expression inner, ParameterizedType target) {
        if (!(inner instanceof Cast cast) || target.arrays() != 0 || target.typeInfo() == null
            // a method group or a lambda takes its delegate's type exactly: no cast of it is the target's
            || cast.expression() instanceof MethodReference || cast.expression() instanceof Lambda) {
            return false;
        }
        ParameterizedType to = cast.parameterizedType();
        if (to == null || to.arrays() != 0 || to.typeInfo() == null || !to.typeInfo().equals(target.typeInfo())
            || !to.parameters().isEmpty() || to.typeInfo().typeParameters().isEmpty()
            || target.parameters().size() != to.typeInfo().typeParameters().size()) {
            return false;
        }
        return target.parameters().stream().allMatch(a -> a.wildcard() == null && !CSharpExpressionPrinter.constructorTypeParameter(a))
               && CSharpExpressionPrinter.writable(target, List.of()) && CSharpExpressionPrinter1.ownTypeParameters(target);
    }

    /**
 * A raw array local whose stored elements give it the formal's type, {@code var copy = new Function[n]} of
 * {@code Function<A, Object>} elements for a {@code Function<A, ?>[]} ({@link CSharpStatementPrinter#storedArrayType}).
 */
    static boolean storedAs(Expression argument, ParameterizedType formal) {
        if (!(CSharpExpressionPrinter1.unwrap(argument) instanceof VariableExpression ve) || !(ve.variable() instanceof LocalVariable lv)) {
            return false;
        }
        ParameterizedType stored = CSharpStatementPrinter.storedArrayType(lv);
        return stored != null && sameErased(stored, formal);
    }

    /**
 * A raw library delegate, {@code Comparator} as C#'s {@code Comparison<object>}, converts to the formal's by the
 * delegate's variance: a {@code Comparison<IMove<Solution_>>}, whose {@code in} argument is a reference type
 * (not a type parameter or a value type, which C#'s variance does not convert); an {@code out} argument of
 * {@code object} only.
 */
    static boolean variantRaw(ParameterizedType argument, ParameterizedType formal) {
        TypeInfo t = argument.typeInfo();
        if (t == null || formal.typeInfo() == null || !t.equals(formal.typeInfo()) || formal.arrays() != 0
            || !CSharpTypeName.libraryDelegate(t)) {
            return false;
        }
        String variance = CSharpVariance.library(t.fullyQualifiedName());
        if (variance == null || variance.length() != formal.parameters().size()) return false;
        for (int i = 0; i < formal.parameters().size(); i++) {
            ParameterizedType a = formal.parameters().get(i);
            boolean reference = a.wildcard() == null && a.arrays() == 0 && !a.isTypeParameter() && a.typeInfo() != null
                                && !a.isPrimitiveExcludingVoid() && !CSharpTypeName.valueReference(a);
            boolean ok = switch (variance.charAt(i)) {
                case '-' -> reference;
                case '+' -> a.isJavaLangObject();
                default -> a.isJavaLangObject();
            };
            if (!ok) return false;
        }
        return true;
    }

    /** The same type, a wildcard, a constructor's type parameter or Object alike: C#'s object each. */
    private static boolean sameErased(ParameterizedType a, ParameterizedType b) {
        boolean objectA = a.isJavaLangObject() || a.wildcard() != null && a.wildcard().isUnbound()
                          || CSharpExpressionPrinter.constructorTypeParameter(a);
        boolean objectB = b.isJavaLangObject() || b.wildcard() != null && b.wildcard().isUnbound()
                          || CSharpExpressionPrinter.constructorTypeParameter(b);
        if (objectA || objectB) return objectA && objectB && a.arrays() == b.arrays();
        if (a.arrays() != b.arrays()) return false;
        if (a.isTypeParameter() || b.isTypeParameter()) {
            return a.isTypeParameter() && b.isTypeParameter() && a.typeParameter().equals(b.typeParameter());
        }
        if (a.typeInfo() == null || !a.typeInfo().equals(b.typeInfo()) || a.parameters().size() != b.parameters().size()) {
            return false;
        }
        for (int i = 0; i < a.parameters().size(); i++) {
            if (!sameErased(a.parameters().get(i), b.parameters().get(i))) return false;
        }
        return true;
    }

    /** An expression's type as C# prints it: a raw local's is its initializer's ({@link CSharpStatementPrinter#rawFromInitializer}). */
    static ParameterizedType printedType(Expression e) {
        if (CSharpExpressionPrinter1.unwrap(e) instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
            ParameterizedType fromInitializer = CSharpStatementPrinter.rawFromInitializer(lv);
            if (fromInitializer != null) return fromInitializer;
        }
        return e.parameterizedType();
    }

    static boolean rawArray(Expression e, ParameterizedType target) {
        if (!(e instanceof ConstructorCall cc) || cc.anonymousClass() != null) return false;
        ParameterizedType type = cc.parameterizedType();
        return type.arrays() > 0 && type.arrays() == target.arrays() && type.typeInfo() != null
               && type.typeInfo().equals(target.typeInfo()) && type.parameters().isEmpty()
               && !target.parameters().isEmpty();
    }

    /**
 * Not an {@code X?} in C#: an enum's constant, and a library's field or method of an enum or a struct, which C#
 * declares with the value type ({@code TimeSpan.FromMilliseconds}); a library's {@code Integer} is C#'s
 * {@code int?} still, and a type parameter's result its argument.
 */
    static boolean plainValue(Expression e) {
        if (e.parameterizedType().isBoxedExcludingVoid()) return false;
        return switch (e) {
            case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                    CSharpNames.isEnumConstant(fr.fieldInfo()) || !CSharpNames.translated(fr.fieldInfo().owner())
                                                                  && !fr.fieldInfo().type().isTypeParameter();
            case MethodCall mc -> !CSharpNames.translated(mc.methodInfo().typeInfo())
                                  && !mc.methodInfo().returnType().isTypeParameter();
            default -> false;
        };
    }

    /** A lambda's parameter has the delegate's type argument as its type, the value type itself: Func<int, int>. */
    static boolean lambdaParameter(Expression e) {
        return e instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo pi
               && pi.methodInfo() != null && pi.methodInfo().typeInfo().isAnonymous();
    }

    /** A condition: a {@code Boolean} is unboxed. */
    static OutputBuilder condition(Expression e, Qualification q) {
        Expression inner = CSharpExpressionPrinter1.unwrap(e);
        return CSharpExpressionPrinter1.unboxed(inner) && inner.parameterizedType().isBooleanOrBoxedBoolean() ? CSharpExpressionPrinter1.unboxCast("bool", e, q)
                : CSharpExpressionPrinter1.print(e, q);
    }

    /** {@code typeof(List<>)} for Java's {@code List.class}: the unbound generic type. */
    static String typeOfArgument(ParameterizedType type, Qualification q) {
        LibraryMapping.TypeMapping bcl = type.typeInfo() == null ? null : CSharpContext.libraries().type(type.typeInfo());
        if (bcl != null && (bcl.template().contains("{") || bcl.dropArguments() || bcl.template().contains("?"))) {
            // Optional.class: Optional<T> is T itself in C#, which has no type of its own
            CSharpContext.message(CSharpPrintCode.UNMAPPED_JDK, null, type.typeInfo().fullyQualifiedName() + ".class");
            return "object";
        }
        if (type.arrays() == 0 && type.typeInfo() != null && !type.typeInfo().typeParameters().isEmpty()) {
            return CSharpTypeName.name(type.typeInfo(), q) + "<" + ",".repeat(type.typeInfo().typeParameters().size() - 1) + ">";
        }
        return CSharpTypeName.of(type, q);
    }

    /** A lambda of a translated functional interface, which C# has as an interface: in its adapter class. */
    static OutputBuilder adapted(ParameterizedType functionalType, OutputBuilder lambda, Qualification q) {
        if (functionalType == null || functionalType.typeInfo() == null
            || !CSharpNames.lambdaAdapter(functionalType.typeInfo())) {
            return lambda;
        }
        return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(CSharpExpressionPrinter1.text(CSharpNames.lambdaAdapterReference(functionalType.typeInfo(), CSharpTypeName.of(functionalType, q))))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(lambda).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /**
 * An anonymous java.lang.reflect.ParameterizedType whose methods return constants, Java's way of writing the
 * type {@code Map<String, Object>} as a value: .NET's reflection has it, {@code typeof(IDictionary<string, object>)}.
 * Null for any other anonymous class.
 */
    static CSharpExpressionPrinter1.TypeLiteral typeLiteral(TypeInfo anonymous) {
        if (anonymous.interfacesImplemented().size() != 1 || anonymous.parentClass() != null
            && anonymous.parentClass().typeInfo() != null && !anonymous.parentClass().typeInfo().isJavaLangObject()) {
            return null;
        }
        TypeInfo implemented = anonymous.interfacesImplemented().getFirst().typeInfo();
        if (implemented == null || !"java.lang.reflect.ParameterizedType".equals(implemented.fullyQualifiedName())) {
            return null;
        }
        ParameterizedType raw = null;
        List<ParameterizedType> arguments = null;
        for (MethodInfo m : anonymous.methods()) {
            if (m.isSynthetic()) continue;
            List<Statement> statements = m.methodBody() == null ? List.of()
                    : CstQueries.own(m.methodBody().statements());
            if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs) || rs.hasNoValue()) {
                return null;
            }
            Expression returned = CSharpExpressionPrinter1.unwrap(rs.expression());
            switch (m.name()) {
                case "getRawType" -> {
                    if (!(returned instanceof ClassExpression ce) || ce.type().typeInfo() == null) return null;
                    raw = ce.type();
                }
                case "getActualTypeArguments" -> {
                    ArrayInitializer ai = returned instanceof ArrayInitializer a ? a
                            : returned instanceof ConstructorCall c ? c.arrayInitializer() : null;
                    if (ai == null || !ai.expressions().stream().allMatch(x -> CSharpExpressionPrinter1.unwrap(x) instanceof ClassExpression)) {
                        return null;
                    }
                    arguments = ai.expressions().stream().map(x -> ((ClassExpression) CSharpExpressionPrinter1.unwrap(x)).type()).toList();
                }
                case "getOwnerType" -> {
                    if (!(returned instanceof NullConstant)) return null;
                }
                default -> {
                    return null;
                }
            }
        }
        return raw != null && arguments != null && !arguments.isEmpty()
               && arguments.size() == raw.typeInfo().typeParameters().size() ? new CSharpExpressionPrinter1.TypeLiteral(raw, arguments) : null;
    }

    /** The anonymous class of {@code cc} is printed as a lambda, see {@link #anonymousAsLambda}. */
    static boolean asLambda(ConstructorCall cc) {
        TypeInfo anonymous = cc.anonymousClass();
        if (!CSharpProgram.lambdaLike(anonymous)) return false;
        ParameterizedType functionalType = anonymous.interfacesImplemented().getFirst();
        // a library's that C# has as a delegate too: new Comparator<String>() { compare … } is a Comparison<string>
        return functionalType.typeInfo() != null && (CSharpNames.lambdaAdapter(functionalType.typeInfo())
                                                     || CSharpContext.program().delegate(functionalType.typeInfo())
                                                     || CSharpTypeName.libraryDelegate(functionalType.typeInfo()));
    }

    static OutputBuilder lambda(Lambda lambda, Qualification q) {
        List<ParameterInfo> params = lambda.parameters();
        // typed as Java wrote it: (int a, int b) -> …; a C# lambda types all its parameters or none
        boolean typed = !lambda.outputVariants().isEmpty()
                        && lambda.outputVariants().stream().allMatch(Lambda.OutputVariant::isTyped);
        TypeInfo functional = lambda.abstractFunctionalTypeInfo();
        return lambda(lambda.methodInfo(), () -> params.size() == 1 && !typed
                ? CSharpContext.declare(params.getFirst().name())
                : params.stream().map(p -> (typed ? CSharpExpressionPrinter.asDelegateHasIt(functional, p.index(), p.parameterizedType(),
                                                    CSharpTypeName.of(p.parameterizedType(), q)) + " " : "")
                                           + CSharpContext.declare(p.name()))
                        .collect(Collectors.joining(", ", "(", ")")), true, q);
    }

    /**
 * {@code parameters => body} of {@code method}, a lambda's or an anonymous class's, in a scope of its own, where
 * {@code parameters} declares them: a body that returns a value is that value; with {@code expressionStatement},
 * a body of one expression statement is its expression; any other is a block.
 */
    static OutputBuilder lambda(MethodInfo method, Supplier<String> parameters, boolean expressionStatement,
                                        Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        CSharpContext.pushMethod(method);
        CSharpContext.enterScope(Set.of()); // the parameters'
        try {
            b.add(CSharpExpressionPrinter1.text(parameters.get())).add(SymbolEnum.binaryOperator("=>"));
            List<Statement> statements = CstQueries.own(method.methodBody().statements());
            Expression returnedValue = CstQueries.returnedValue(statements);
            if (returnedValue != null) return b.add(CSharpExpressionPrinter1.returned(returnedValue, q));
            if (expressionStatement && statements.size() == 1 && statements.getFirst() instanceof ExpressionAsStatement eas) {
                // a lambda of a void method: its expression's value is not used
                return b.add(method.isVoid() ? CSharpExpressionPrinter1.printStatement(eas.expression(), q) : CSharpExpressionPrinter1.print(eas.expression(), q));
            }
            return b.add(CSharpStatementPrinter.block(method.methodBody(), q));
        } finally {
            CSharpContext.exitScope();
            CSharpContext.popMethod();
        }
    }

    /** The property C# has for a method, as {@link #member} prints a call of it: a record's component, a getter's. */
    static String propertyOf(MethodInfo method) {
        String computed = CSharpContext.program().computedProperty(method);
        if (computed != null) return computed;
        CSharpProperties.Property property = CSharpContext.program().accessorOf(method);
        if (property != null) return method == property.getter() ? property.name() : null;
        if (CstQueries.isRecordAccessor(method)) {
            return method.typeInfo().fields().stream().filter(f -> !f.isStatic() && f.name().equals(method.name()))
                    .findFirst().map(CSharpNames::field).orElse(null);
        }
        return null;
    }

    /**
 * The created class with the type arguments that make it {@code declared}, where it passes its own type parameters
 * on to that supertype, directly or through others: {@code LinkedHashSet<Value>} of a {@code Set<Value>}.
 */
    static ParameterizedType ownParametersAs(TypeInfo created, ParameterizedType declared) {
        List<TypeParameter> own = created.typeParameters();
        if (own.isEmpty()) return null;
        ParameterizedType seen = created.equals(declared.typeInfo()) ? created.asParameterizedType()
                : CstSupertypes.asSeen(created, declared.typeInfo());
        if (seen == null || seen.parameters().size() != own.size()) return null;
        List<ParameterizedType> arguments = new ArrayList<>();
        for (int i = 0; i < own.size(); i++) {
            ParameterizedType p = seen.parameters().get(i);
            if (!p.isTypeParameter() || !own.get(i).equals(p.typeParameter())) return null;
            arguments.add(declared.parameters().get(i));
        }
        return created.asParameterizedType().withParameters(arguments);
    }

    /** Slot::new of an inner class: its enclosing instance first, as {@code new Slot(…)} has it (CS7036). */
    static List<String> constructorArguments(ParameterizedType type, List<String> parameters) {
        TypeInfo created = type.typeInfo();
        if (type.arrays() > 0 || !CSharpInner.withOuter(created)) return parameters;
        String enclosing = CSharpInner.enclosingInstance(created);
        if (enclosing == null) return parameters;
        List<String> arguments = new ArrayList<>(parameters);
        arguments.addFirst(enclosing);
        return arguments;
    }

    static OutputBuilder assignment(Assignment a, Qualification q) {
        OutputBuilder target = variable(a.variableTarget(), q);
        OutputBuilder increment = Emission.increment(a, target);
        if (increment != null) return increment;
        String operator = a.assignmentOperator() == null ? "=" : a.assignmentOperator().name();
        OutputBuilder narrowing = CSharpExpressionPrinter1.narrowingCompound(a, operator, q);
        if (narrowing != null) return narrowing;
        if (a.assignmentOperator() == null && a.variableTarget() instanceof DependentVariable && CSharpExpressionPrinter1.unboxedValue(a.value())) {
            // an array's element is the enum's or the struct's value type, decimal[]: the X? as its value
            OutputBuilder value = CSharpExpressionPrinter1.unboxCast(CSharpTypeName.argument(a.value().parameterizedType(), q), a.value(), q);
            return new OutputBuilderImpl().add(target).add(SymbolEnum.assignment(operator)).add(value);
        }
        ParameterizedType targetType = a.variableTarget().parameterizedType();
        OutputBuilder value = "+=".equals(operator) && targetType != null && targetType.isJavaLangString()
                              && targetType.arrays() == 0 ? stringOperand(a.precedence(), a.value(), q)
                : CSharpExpressionPrinter1.converts(a.value(), targetType) ? CSharpExpressionPrinter1.converted(a.value(), targetType, q) : CSharpExpressionPrinter1.operand(a.precedence(), a.value(), q);
        if (a.assignmentOperator() == null) value = CSharpNullability.intoVariable(a.value(), a.variableTarget(), value);
        return new OutputBuilderImpl().add(target).add(SymbolEnum.assignment(operator)).add(value);
    }

    /** {@code value} is a C# value type (a primitive, an enum C# declares as an enum), {@code other} an Object. */
    static boolean valueAndObject(Expression value, Expression other) {
        ParameterizedType v = value.parameterizedType();
        ParameterizedType o = other.parameterizedType();
        if (v == null || o == null || v.arrays() > 0 || !o.isJavaLangObject() || o.arrays() > 0) return false;
        return v.isPrimitiveExcludingVoid() || v.typeInfo() != null && v.typeInfo().typeNature().isEnum()
                                               && CSharpNames.translated(v.typeInfo()) && CSharpNames.simpleEnum(v.typeInfo());
    }

    static OutputBuilder binaryOperator(BinaryOperator bo, Qualification q) {
        // Integer.MAX_VALUE + 1: Java wraps a constant's overflow, C# rejects it (CS0220)
        if (CSharpConstants.overflows(bo)) return CSharpExpressionPrinter.unchecked(plainBinaryOperator(bo, q));
        return plainBinaryOperator(bo, q);
    }

    private static OutputBuilder plainBinaryOperator(BinaryOperator bo, Qualification q) {
        String op = bo.operator().name();
        if (("==".equals(op) || "!=".equals(op)) && (valueAndObject(bo.lhs(), bo.rhs()) || valueAndObject(bo.rhs(), bo.lhs()))) {
            // Java compares a boxed enum or number with an Object by reference; C# cannot compare a value type with
            // an object: by value
            OutputBuilder call = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text("object.Equals"))
                    .add(CSharpExpressionPrinter1.arguments(List.of(bo.lhs(), bo.rhs()), q));
            return "==".equals(op) ? call : new OutputBuilderImpl().add(SymbolEnum.UNARY_BOOLEAN_NOT).add(call);
        }
        if (("==".equals(op) || "!=".equals(op)) && referenceIdentity(bo.lhs(), bo.rhs())) {
            // Java's == on two Strings compares references, C#'s compares contents; C# has no == on a type parameter
            OutputBuilder call = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text("object.ReferenceEquals"))
                    .add(CSharpExpressionPrinter1.arguments(List.of(bo.lhs(), bo.rhs()), q));
            return "==".equals(op) ? call
                    : new OutputBuilderImpl().add(SymbolEnum.UNARY_BOOLEAN_NOT).add(call);
        }
        if ("+".equals(op) && bo.parameterizedType() != null && bo.parameterizedType().isJavaLangString()
            && bo.parameterizedType().arrays() == 0) {
            // Java's string conversion of each operand, String.valueOf, where C#'s ToString differs
            return new OutputBuilderImpl().add(stringOperand(bo.precedence(), bo.lhs(), q))
                    .add(SymbolEnum.binaryOperator(op)).add(stringOperand(bo.precedence(), bo.rhs(), q));
        }
        if (!"==".equals(op) && !"!=".equals(op) && bo.parameterizedType() != null
            && bo.parameterizedType().isPrimitiveExcludingVoid()) {
            // Java unboxes an Integer operand; C# lifts the operation to int?
            return new OutputBuilderImpl().add(CSharpExpressionPrinter1.unboxedOperand(bo.precedence(), bo.lhs(), q))
                    .add(SymbolEnum.binaryOperator(op)).add(CSharpExpressionPrinter1.unboxedOperand(bo.precedence(), bo.rhs(), q));
        }
        return new OutputBuilderImpl().add(CSharpExpressionPrinter1.operand(bo.precedence(), bo.lhs(), q))
                .add(SymbolEnum.binaryOperator(op))
                .add(CSharpExpressionPrinter1.operand(bo.precedence(), bo.rhs(), q));
    }

    /**
 * An operand of a string concatenation, converted as Java's {@code String.valueOf} converts it where C#'s
 * conversion differs ({@link #javaStringConversion}): {@code JavaString.ValueOf(b)} for a boolean ({@code true},
 * not {@code True}), a double ({@code 1.0}, not {@code 1}), a collection ({@code [1, 2]}, not its type's name), a
 * value that may be null ({@code null}, not the empty string); a String that may be null is {@code (s ?? "null")}.
 */
    static OutputBuilder stringOperand(Precedence precedence, Expression e, Qualification q) {
        Expression x = CSharpExpressionPrinter1.unwrap(e);
        ParameterizedType t = x.parameterizedType();
        if (x instanceof NullConstant) return CSharpExpressionPrinter1.text("\"null\"");
        if (t != null && t.isJavaLangString() && t.arrays() == 0) {
            if (!CSharpNullability.mayBeNull(x)) return CSharpExpressionPrinter1.operand(precedence, e, q);
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter1.operand(precedence, e, q))
                    .add(CSharpExpressionPrinter1.text("?? \"null\"")).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (!javaStringConversion(x)) return CSharpExpressionPrinter1.operand(precedence, e, q);
        return valueOf(e, q);
    }

    /** {@code x.toString()} of a value whose C# {@code ToString} is not Java's ({@link #javaStringConversion}). */
    static boolean javaToString(MethodCall mc) {
        MethodInfo m = mc.methodInfo();
        Expression object = mc.object();
        return "toString".equals(m.name()) && m.parameters().isEmpty() && !m.isStatic() && object != null
               && !mc.objectIsImplicit() && !(CSharpExpressionPrinter1.unwrap(object) instanceof VariableExpression ve && ve.variable() instanceof This)
               && javaStringConversion(CSharpExpressionPrinter1.unwrap(object));
    }

    /** {@code JavaString.ValueOf(e)}: Java's string conversion. */
    static OutputBuilder valueOf(Expression e, Qualification q) {
        CSharpContext.using(CSharpCompat.NAMESPACE);
        OutputBuilder b = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text("JavaString.ValueOf")).add(SymbolEnum.LEFT_PARENTHESIS);
        Expression x = CSharpExpressionPrinter1.unwrap(e);
        ParameterizedType t = x.parameterizedType();
        // Double.valueOf(2) prints as 2, an int to C#: the double's overload, 2.0
        if (t != null && floatingPoint(t) && (x instanceof ConstantExpression<?>
                                              || x instanceof MethodCall mc && !CSharpNames.translated(mc.methodInfo().typeInfo()))) {
            b.add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter1.text(t.typeInfo().isFloat()
                    || "java.lang.Float".equals(t.typeInfo().fullyQualifiedName()) ? "float" : "double"))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter1.operand(e.precedence(), e, q));
            return b.add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return b.add(CSharpExpressionPrinter1.print(e, q)).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /**
 * C# converts {@code e} to a string otherwise than Java: a boolean, a float or a double (boxed too), an Object, a
 * type parameter, a library type (a collection, a map), a value that may be null. Not an integer or a char, an
 * enum, an array, nor a program type's value known not to be null, whose {@code ToString} is its
 * {@code toString}.
 */
    static boolean javaStringConversion(Expression e) {
        ParameterizedType t = e.parameterizedType();
        if (t == null || t.arrays() > 0) return false;
        if (t.isBooleanOrBoxedBoolean() || floatingPoint(t)) return true;
        if (t.isPrimitiveExcludingVoid()) return false;
        if (CSharpNullability.mayBeNull(e)) return true;
        // an Integer is C#'s int?, whose null concatenates as the empty string
        if (t.isBoxedExcludingVoid()) return true;
        if (t.isTypeParameter() || t.isJavaLangObject()) return true;
        // the common type of a conditional's String and Class<?>: an intersection, or one of its interfaces, whichever
        // the running JDK's String and Class have in common; any value may be one
        if (t.isIntersectionType()) return true;
        TypeInfo ti = t.typeInfo();
        if (ti == null || ti.typeNature().isEnum() || CSharpNames.translated(ti)) return false;
        if (ANY_VALUE.contains(ti.fullyQualifiedName())) return true;
        // a java.util type that is one; not java.nio.file.Path, an Iterable with its own toString
        return CONVERTED_DIFFERENTLY.contains(ti.fullyQualifiedName())
               || ti.fullyQualifiedName().startsWith("java.util.")
                  && ti.recursiveSuperTypeStream().anyMatch(s -> CONVERTED_DIFFERENTLY.contains(s.fullyQualifiedName()));
    }

    /**
 * A branch of a conditional. Java types {@code b ? c : 0}, a char and an int constant that fits, as a char; C#
 * as an int (97 where Java prints a): the constant is cast to the other branch's char, byte or short.
 */
    static OutputBuilder conditionalBranch(InlineConditional ic, Expression branch, Qualification q) {
        // JLS 15.25: the other operand's char, byte or short, where this one is an int constant that fits it
        Expression other = branch == ic.ifTrue() ? ic.ifFalse() : ic.ifTrue();
        ParameterizedType type = other.parameterizedType();
        ParameterizedType branchType = branch.parameterizedType();
        TypeInfo ti = type == null || type.arrays() != 0 ? null : type.typeInfo();
        if (ti != null && (ti.isChar() || ti.isByte() || ti.isShort()) && branchType != null && branchType.arrays() == 0
            && branchType.typeInfo() != null && branchType.typeInfo().isInt()
            && CSharpExpressionPrinter1.unwrap(branch) instanceof ConstantExpression<?> c && c.constant() instanceof Integer i
            && (ti.isChar() ? i >= 0 && i <= Character.MAX_VALUE : ti.isByte() ? i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE
                : i >= Short.MIN_VALUE && i <= Short.MAX_VALUE)) {
            String to = ti.isChar() ? "char" : ti.isByte() ? "sbyte" : "short";
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter1.text(to))
                    .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST).add(CSharpExpressionPrinter1.operand(ic.precedence(), branch, q));
        }
        return CSharpExpressionPrinter1.operand(ic.precedence(), branch, q);
    }

    /** A float or a double, boxed too. */
    private static boolean floatingPoint(ParameterizedType t) {
        TypeInfo ti = t.typeInfo();
        return t.arrays() == 0 && ti != null && (ti.isFloat() || ti.isDouble()
                                                 || "java.lang.Float".equals(ti.fullyQualifiedName())
                                                 || "java.lang.Double".equals(ti.fullyQualifiedName()));
    }

    /**
 * Java's {@code ==} compares references where C#'s does not: two Strings, which C# compares by contents, and a
 * type parameter's value, which C# cannot compare with {@code ==} at all. Against {@code null}, C#'s is fine.
 */
    static boolean referenceIdentity(Expression lhs, Expression rhs) {
        if (CSharpExpressionPrinter1.unwrap(lhs) instanceof NullConstant || CSharpExpressionPrinter1.unwrap(rhs) instanceof NullConstant) return false;
        ParameterizedType l = lhs.parameterizedType();
        ParameterizedType r = rhs.parameterizedType();
        if (l == null || r == null || l.arrays() > 0 || r.arrays() > 0) return false;
        return l.isJavaLangString() && r.isJavaLangString() || l.isTypeParameter() || r.isTypeParameter()
               || translatedRecord(l) || translatedRecord(r);
    }

    /** A record of the program: C#'s {@code ==} on it compares the components, Java's the references. */
    private static boolean translatedRecord(ParameterizedType t) {
        TypeInfo ti = t.typeInfo();
        return ti != null && ti.typeNature().isRecord() && CSharpNames.translated(ti);
    }

    /**
 * A record pattern, Java's {@code R(long x, P(var a, _) p)}, as C#'s property pattern
 * {@code R { X: var x, P: P { A: var a } }}: a record of the program is a C# record whose components are its
 * properties. A variable of the component's own type is {@code var}, of a narrower one a type test, as in Java.
 * Null for a library's record, which has no such properties. {@code component} is null at the top.
 */
    static String recordPattern(RecordPattern p, ParameterizedType component, Qualification q) {
        if (p.unnamedPattern()) return "_";
        LocalVariable lv = p.localVariable();
        if (lv != null) {
            String name = CSharpContext.declare(lv.simpleName());
            boolean same = component != null && lv.parameterizedType().equals(component);
            return (same ? "var" : CSharpTypeName.pattern(lv.parameterizedType(), q)) + " " + name;
        }
        ParameterizedType type = p.recordType();
        TypeInfo record = type == null ? null : type.typeInfo();
        if (record == null || !record.typeNature().isRecord() || !CSharpNames.translated(record)) return null;
        List<FieldInfo> components = record.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList();
        if (components.size() != p.patterns().size()) return null;
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            String sub = recordPattern(p.patterns().get(i), components.get(i).type(), q);
            if (sub == null) return null;
            if (!"_".equals(sub)) parts.add(CSharpNames.field(components.get(i)) + ": " + sub);
        }
        return CSharpTypeName.pattern(type, q) + " { " + String.join(", ", parts) + (parts.isEmpty() ? "}" : " }");
    }

    static String doubleLiteral(double d) {
        return CSharpExpressionPrinter.LITERALS.doubleLiteral(d);
    }

    static String floatLiteral(float f) {
        return CSharpExpressionPrinter.LITERALS.floatLiteral(f);
    }
}
