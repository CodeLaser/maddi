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
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;
import io.codelaser.maddi.cst.print.translate.Literals;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * Prints an {@link Expression} as C#.Every expression form is printed here, recursively, so that a Java-only form
 * nested anywhere is translated too; only a form this class does not know falls back to the Java
 * {@link Expression#print}, with a {@link CSharpPrintCode#JAVA_FALLBACK} message.
 *
 * C#'s operators and their precedence are Java's, so most expressions print as they do in Java. Where they differ:
 *
 * {@code x instanceof T} is {@code x is T}, and {@code !(x instanceof T)} is {@code x is not T};
 * {@code ==} between two Strings is Java's identity comparison; C#'s {@code ==} compares their contents, so it
 * is {@code object.ReferenceEquals(a, b)}, as is {@code ==} on a type parameter's value, which C# does not
 * allow;
 * a lambda is {@code (a, b) => \u2026}; a method reference is a method group ({@code Owner.Method},
 * {@code receiver.Method}) or, for an unbound receiver or a constructor, a lambda;
 * {@code X.class} is {@code typeof(X)}; {@code super.m()} is {@code base.M()}; an array's {@code length} is
 * {@code Length};
 * a diamond {@code new ArrayList<>()} has its type arguments written out.
 */
public class CSharpExpressionPrinter {
    static final Literals LITERALS = new Literals("double", "float", "NaN", "PositiveInfinity",
            "NegativeInfinity", Map.of('\f', "\\f", '\0', "\\0"));

    /** The JDK's sorted collections that C# creates with a comparer argument last: SortedDictionary, SortedSet. */
    private static final Set<String> SORTED_COLLECTIONS = Set.of("java.util.TreeMap", "java.util.TreeSet");
    private CSharpExpressionPrinter() { }
    /**
 * {@code (T) x}. C# casts to a type parameter, or a type of one, only from object, an interface or a type of type
 * parameters: {@code (T) this} in a self-typed builder is {@code (T) (object) this}.
 */
    static OutputBuilder cast(Cast cast, Qualification q) {
        // (int) 3.9e10, (long) Double.NaN: Java's saturated value, where C#'s constant conversion is unspecified
        String literal = CSharpConstants.floatingToIntegral(cast);
        if (literal != null) {
            return literal.startsWith("-") || literal.contains(" ") ? parenthesized(CSharpExpressionPrinter1.text(literal)) : CSharpExpressionPrinter1.text(literal);
        }
        // (byte) 200: Java narrows a constant, C# rejects it (CS0221)
        if (CSharpConstants.narrowingOverflow(cast)) return unchecked(CSharpExpressionPrinter1.plainCast(cast, q));
        return CSharpExpressionPrinter1.plainCast(cast, q);
    }

    /** The interface as the receiver's type implements it, with its type arguments. */
    static String interfaceType(TypeInfo owner, Expression object, Qualification q) {
        boolean self = object == null || object instanceof VariableExpression ve && ve.variable() instanceof This;
        if (!self && object.parameterizedType() != null && object.parameterizedType().arrays() == 0) {
            // a receiver with its own arguments, a captured AndThen<TS, TRC>: the interface as it has them
            String captured = capturedInterfaceType(owner, object, q);
            if (captured != null) return captured;
            ParameterizedType seen = CstSupertypes.asSeen(object.parameterizedType(), owner);
            if (seen != null && seen.parameters().stream().allMatch(a -> a.wildcard() == null)
                && writable(seen, List.of())) {
                return CSharpTypeName.of(seen, q);
            }
        }
        TypeInfo start = self ? CSharpContext.currentType() : object.parameterizedType().typeInfo();
        Deque<TypeInfo> todo = new ArrayDeque<>(List.of(start));
        Set<TypeInfo> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            TypeInfo t = todo.pop();
            if (!seen.add(t)) continue;
            for (ParameterizedType i : t.interfacesImplemented()) {
                if (owner.equals(i.typeInfo())) return CSharpTypeName.of(i, q);
                if (i.typeInfo() != null) todo.push(i.typeInfo());
            }
            if (t.parentClass() != null && t.parentClass().typeInfo() != null) todo.push(t.parentClass().typeInfo());
        }
        return CSharpTypeName.nameInExpression(owner, q);
    }

    /**
 * The interface {@code owner} as a captured parameter's type implements it, {@code IColl<TS, TRC>} for an
 * {@code AndThen<TS, TRC> a} captured from {@code AndThen<?, ?>}: the class's arguments to the interface are its
 * own type parameters, each the parameter's argument at that position, a captured wildcard by its name. Null
 * when the receiver is no captured parameter, or the arguments are not as plain as that.
 */
    private static String capturedInterfaceType(TypeInfo owner, Expression object, Qualification q) {
        MethodInfo method = CSharpContext.currentMethod();
        if (method == null || !(CSharpExpressionPrinter1.unwrap(object) instanceof VariableExpression ve) || !(ve.variable() instanceof ParameterInfo pi)
            || pi.methodInfo() != method || !CSharpWildcards.capturedParameter(method, pi.index())) {
            return null;
        }
        ParameterizedType pt = pi.parameterizedType();
        TypeInfo t = pt.typeInfo();
        List<String> names = CSharpWildcards.captured(method, q).names().get(pi.index());
        if (t == null || names == null || pt.parameters().size() != t.typeParameters().size()) return null;
        ParameterizedType asOwner = t.equals(owner) ? t.asParameterizedType() : CstSupertypes.asSeen(t, owner);
        if (asOwner == null) return null;
        List<String> printed = new ArrayList<>();
        for (ParameterizedType a : asOwner.parameters()) {
            if (!a.isTypeParameter() || a.arrays() != 0) return null;
            int i = t.typeParameters().indexOf(a.typeParameter());
            if (i < 0) return null;
            ParameterizedType argument = pt.parameters().get(i);
            if (argument.wildcard() != null) {
                int n = 0;
                for (int k = 0; k < i; k++) if (pt.parameters().get(k).wildcard() != null) n++;
                if (n >= names.size()) return null;
                printed.add(names.get(n));
            } else {
                printed.add(CSharpTypeName.argument(argument, q));
            }
        }
        return CSharpTypeName.name(owner, q) + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    /** {@code <Foo>} for a call passing type token {@code Foo.class}; null when the method takes no type token. */
    private static String typeTokenArgument(MethodCall mc, Qualification q) {
        int token = CSharpContext.program().typeToken(mc.methodInfo());
        if (token < 0) return null;
        ParameterizedType type = mc.parameterExpressions().get(token).parameterizedType();
        return "<" + CSharpTypeName.argument(type.parameters().getFirst(), q) + ">";
    }

    static OutputBuilder fieldReference(FieldReference fr, Qualification q) {
        FieldInfo field = fr.fieldInfo();
        if (!CSharpNames.translated(field.owner())) {
            LibraryMapping.CodeMapping mapped = CSharpContext.libraries().field(field);
            if (mapped != null) {
                if (mapped.namespace() != null) CSharpContext.using(mapped.namespace());
                return CSharpExpressionPrinter1.text(mapped.value());
            }
            CSharpContext.message(CSharpPrintCode.UNMAPPED_JDK, fr.scope() != null ? fr.scope() : null,
                    field.owner().fullyQualifiedName() + "." + field.name());
        }
        String name = CSharpNames.field(field);
        CSharpAnonymous.Hoisted withOuter = CSharpContext.hoistedWithOuter();
        if (withOuter != null && !field.isStatic() && CstQueries.onThis(fr)
            && CSharpNames.foreign(field.owner(), withOuter.type())
            && !(fr.scope() instanceof VariableExpression ve && ve.variable() instanceof This t
                 && t.explicitlyWriteType() != null)) {
            return CSharpExpressionPrinter1.text("outer." + name);
        }
        // a field of the enclosing instance, in an inner class
        if (!field.isStatic() && fr.isDefaultScope()) {
            String path = CSharpInner.pathTo(field.owner());
            if (path != null) return CSharpExpressionPrinter1.text(path + "." + name);
        }
        if (fr.isDefaultScope() || fr.scope() == null) {
            // a static import, or an interface's constant, which C# does not bring into the scope of the classes
            // implementing the interface
            if (field.isStatic() && !inScope(field.owner(), CSharpNames.inCompanion(field))) {
                return CSharpExpressionPrinter1.text(CSharpTypeName.nameInExpression(field.owner(), q) + "." + name);
            }
            return CSharpExpressionPrinter1.text(name);
        }
        if (fr.isStatic() && fr.scope() instanceof TypeExpression) {
            return CSharpExpressionPrinter1.text(CSharpTypeName.nameInExpression(field.owner(), q) + "." + name);
        }
        OutputBuilder scope = field.isStatic() ? receiver(fr.scope(), q) : CSharpNullability.dereferenced(fr.scope(), q);
        return new OutputBuilderImpl().add(scope).add(SymbolEnum.DOT).add(CSharpExpressionPrinter1.text(name));
    }

    /**
 * C# finds the static members of {@code owner} by their simple names: it is the type being printed, a type it is
 * nested in (in C#: not past a hoisted type), or a superclass of one of those.
 */
    private static boolean inScope(TypeInfo owner, boolean inCompanion) {
        TypeInfo companion = CSharpContext.companion();
        // a companion's members are in scope in the companion only; the companion is a sibling of its type
        if (inCompanion) return owner.equals(companion);
        TypeInfo start = CSharpContext.currentType();
        if (companion != null) start = CSharpNames.hoisted(start) ? null : CstQueries.enclosing(start);
        for (TypeInfo t = start; t != null; t = CSharpNames.hoisted(t) ? null : CstQueries.enclosing(t)) {
            if (CstQueries.classChain(t).contains(owner)) return true;
        }
        return false;
    }

    /**
 * What comes before the member a call names, with its dot: the enclosing instance ({@code outer.}), the receiver,
 * {@code base.}, the type of a static member out of scope; nothing for an implicit {@code this}.
 */
    static OutputBuilder qualifier(MethodCall mc, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        Expression object = mc.object();
        MethodInfo method = mc.methodInfo();
        CSharpAnonymous.Hoisted withOuter = CSharpContext.hoistedWithOuter();
        if (withOuter != null && !method.isStatic() && mc.objectIsImplicit()
            && CSharpNames.foreign(method.typeInfo(), withOuter.type())) {
            b.add(CSharpExpressionPrinter1.text("outer")).add(SymbolEnum.DOT);
        } else if (!method.isStatic() && mc.objectIsImplicit() && CSharpInner.pathTo(method.typeInfo()) != null) {
            // a method of the enclosing instance, in an inner class
            b.add(CSharpExpressionPrinter1.text(CSharpInner.pathTo(method.typeInfo()))).add(SymbolEnum.DOT);
        } else if (object instanceof VariableExpression ve && ve.variable() instanceof This t) {
            if (t.writeSuper() || !mc.objectIsImplicit()) b.add(CSharpExpressionPrinter1.text(CSharpExpressionPrinter2.thisOrBase(t))).add(SymbolEnum.DOT);
        } else if (object instanceof TypeExpression te) {
            if (!mc.objectIsImplicit() || method.isStatic() && !inScope(method.typeInfo(), CSharpNames.inCompanion(method))) {
                b.add(CSharpExpressionPrinter1.text(CSharpTypeName.nameInExpression(te.parameterizedType().typeInfo(), q))).add(SymbolEnum.DOT);
            }
        } else if (object != null && !mc.objectIsImplicit()) {
            b.add(method.isStatic() ? receiver(object, q) : CSharpNullability.dereferenced(object, q)).add(SymbolEnum.DOT);
        }
        return b;
    }

    /** The member a call names, after its {@link #qualifier}: a property, or the method with its arguments. */
    static OutputBuilder member(MethodCall mc, OutputBuilder b, Qualification q) {
        MethodInfo method = mc.methodInfo();
        String computed = CSharpContext.program().computedProperty(method);
        if (computed != null) return b.add(CSharpExpressionPrinter1.text(computed)); // x.type() is the property x.Type
        CSharpProperties.Property property = CSharpContext.program().accessorOf(method);
        if (property != null) {
            // x.getName() is the property x.Name, x.setName(v) an assignment to it
            b.add(CSharpExpressionPrinter1.text(property.name()));
            if (method == property.getter()) return b;
            Expression value = mc.parameterExpressions().getFirst();
            return b.add(SymbolEnum.assignment("=")).add(CSharpNullability.intoParameter(value, method, 0,
                    CSharpExpressionPrinter1.converted(value, method.parameters().getFirst().parameterizedType(), q)));
        }
        if (CstQueries.isRecordAccessor(method)) {
            // p.x() is the record's property p.X
            FieldInfo component = method.typeInfo().fields().stream()
                    .filter(f -> !f.isStatic() && f.name().equals(method.name())).findFirst().orElseThrow();
            return b.add(CSharpExpressionPrinter1.text(CSharpNames.field(component)));
        }
        unmapped(method, mc);
        b.add(CSharpExpressionPrinter1.text(CSharpNames.method(method) + typeArguments(mc, q)));
        return b.add(CSharpExpressionPrinter1.argumentsWithoutTypeToken(mc, q));
    }

    /** A call's type arguments: a dropped type token's, Java's explicit ones, or those C# cannot infer. */
    static String typeArguments(MethodCall mc, Qualification q) {
        String tokenArgument = typeTokenArgument(mc, q);
        return tokenArgument != null ? tokenArgument
                : CSharpContext.program().erasedGeneric(mc.methodInfo()) ? ""
                : mc.typeArguments().isEmpty() ? inferredTypeArguments(mc, q) : mc.typeArguments().stream()
                .map(t -> CSharpTypeName.argument(t, q)).collect(Collectors.joining(", ", "<", ">"));
    }

    /**
 * The type arguments Java infers and C# cannot: a method type parameter that no parameter's type mentions,
 * {@code <Value_> Value_ getStore(int index)}, which Java infers from the call's target and C# not at all (CS0411).
 * C# takes all type arguments or none, so all are printed, as Java inferred them from the call's result and its
 * arguments; none when one of them is not a type C# can write (a wildcard, a capture, the method's own parameter).
 */
    private static String inferredTypeArguments(MethodCall mc, Qualification q) {
        MethodInfo method = mc.methodInfo();
        List<TypeParameter> typeParameters = method.typeParameters();
        if (typeParameters.isEmpty() || !CSharpNames.translated(method.typeInfo())
            || !uninferable(method) && !onlyThroughLambdas(mc)) {
            return "";
        }
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        // a raw result goes into a target with arguments: Java's unchecked conversion, the target's arguments here
        ParameterizedType callTarget = CSharpContext.callTarget(mc);
        match(method.returnType(), callTarget != null ? callTarget : mc.concreteReturnType(), typeParameters, inferred);
        List<Expression> args = mc.parameterExpressions();
        for (int i = 0; i < method.parameters().size() && i < args.size(); i++) {
            ParameterInfo p = method.parameters().get(i);
            if (p.isVarArgs()) {
                varArgs(p.parameterizedType(), args.subList(i, args.size()), typeParameters, inferred);
                break;
            }
            match(p.parameterizedType(), args.get(i).parameterizedType(), typeParameters, inferred);
        }
        bounds(typeParameters, inferred);
        String printed = CSharpExpressionPrinter2.meetBounds(typeParameters, inferred, q) ? typeArgumentList(typeParameters, inferred, true, q) : "";
        if (printed.isEmpty() && CSharpExpressionPrinter2.onlyInClass(method)) {
            // <T> Class<T> resolveClass(String name): C#'s Type is not generic, any T will do: its bound
            printed = typeParameters.stream().map(tp -> CSharpTypeName.rawArgument(tp, q))
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        return printed;
    }

    /**
 * A variable arity parameter's type parameter: from the array passed as such, {@code buildMove(moves.toArray(…))},
 * else from the elements when they are of one type; elements of different types, {@code buildMove(unassign,
 * assign)}, leave it to its bound ({@link #bounds}), where Java's is their least upper bound.
 */
    private static void varArgs(ParameterizedType formal, List<Expression> args, List<TypeParameter> typeParameters,
                                Map<TypeParameter, ParameterizedType> inferred) {
        if (args.size() == 1 && args.getFirst().parameterizedType() != null
            && args.getFirst().parameterizedType().arrays() == formal.arrays()) {
            match(formal, args.getFirst().parameterizedType(), typeParameters, inferred);
            return;
        }
        Set<ParameterizedType> elements = new HashSet<>();
        for (Expression a : args) {
            if (!(CSharpExpressionPrinter1.unwrap(a) instanceof NullConstant) && a.parameterizedType() != null) elements.add(a.parameterizedType());
        }
        if (elements.size() == 1) match(formal.copyWithFewerArrays(1), elements.iterator().next(), typeParameters, inferred);
    }

    /**
 * A type parameter left without a type argument, of one bound in the others: that bound, {@code Move_ extends
 * Move<Solution_>} with {@code Solution_} inferred.
 */
    private static void bounds(List<TypeParameter> typeParameters, Map<TypeParameter, ParameterizedType> inferred) {
        for (TypeParameter tp : typeParameters) {
            if (inferred.containsKey(tp) || tp.typeBounds().size() != 1) continue;
            ParameterizedType bound = tp.typeBounds().getFirst();
            if (bound.isJavaLangObject() || CstQueries.mentionsAny(bound, List.of(tp))) continue;
            if (typeParameters.stream().anyMatch(o -> CstQueries.mentionsAny(bound, List.of(o)) && !inferred.containsKey(o))) {
                continue;
            }
            inferred.put(tp, CstSupertypes.substitute(bound, inferred));
        }
    }

    /** A type parameter of {@code method} that no parameter's type mentions as C# has it: C# cannot infer it. */
    static boolean uninferable(MethodInfo method) {
        return method.typeParameters().stream().anyMatch(tp -> method.parameters().stream()
                .noneMatch(p -> CSharpExpressionPrinter2.mentionsInCSharp(p.parameterizedType(), tp)));
    }

    /**
 * {@code <A, B>}: the type arguments Java inferred for {@code typeParameters}; empty when one is not a type C# can
 * write. {@code call}: a call's, where a wildcard {@code ? extends X} stands for {@code X}, {@code ?} for the type
 * parameter's bound, and {@code Object} for a bounded type parameter would trade CS0411 for CS0311.
 */
    private static String typeArgumentList(List<TypeParameter> typeParameters,
                                           Map<TypeParameter, ParameterizedType> inferred, boolean call, Qualification q) {
        List<String> printed = new ArrayList<>();
        for (TypeParameter tp : typeParameters) {
            ParameterizedType t = inferred.get(tp);
            if (call && t != null && t.wildcard() != null) {
                // T from a Class<? extends Acceptor>: the bound, as Java's result is; from a Class<?>: T's own bound
                if (t.wildcard().isExtendsNoIntersection() && t.typeInfo() != null) {
                    t = t.withWildcard(null);
                } else if (t.wildcard().isUnbound()) {
                    printed.add(CSharpTypeName.rawArgument(tp, q));
                    continue;
                }
            }
            if (t == null || !writableArgument(t, typeParameters)) return "";
            if (call && t.isJavaLangObject() && tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) return "";
            printed.add(CSharpTypeName.argument(t, q));
        }
        return "<" + String.join(", ", printed) + ">";
    }

    /**
 * A type parameter of the called method that only parameters given a lambda or a method reference mention:
 * {@code oneKeyGroupBy(key, Group1Mapping0CollectorUniNode::new)}'s {@code Tuple_}, of the builder's
 * {@code TupleLifecycle<Tuple_>} parameter. C# infers nothing from an implicitly typed lambda's parameters, and
 * a method reference is a lambda in C# (CS0411); Java infers it from the call's target. Nor from {@code null},
 * {@code ValueRangeItem.of(null, …)}'s {@code Entity_}.
 */
    static boolean onlyThroughLambdas(MethodCall mc) {
        MethodInfo method = mc.methodInfo();
        List<Expression> args = mc.parameterExpressions();
        if (method.parameters().stream().anyMatch(ParameterInfo::isVarArgs)) return false;
        return method.typeParameters().stream().anyMatch(tp -> {
            boolean mentioned = false;
            for (int i = 0; i < method.parameters().size() && i < args.size(); i++) {
                if (!CSharpExpressionPrinter2.mentionsInCSharp(method.parameters().get(i).parameterizedType(), tp)) continue;
                Expression arg = CSharpExpressionPrinter1.unwrap(args.get(i));
                if (!(arg instanceof Lambda) && !(arg instanceof MethodReference) && !(arg instanceof NullConstant)) {
                    return false;
                }
                mentioned = true;
            }
            return mentioned;
        });
    }

    /** Matches a formal type with Java's concrete one: {@code Value_} with {@code Bucket<…>}. */
    static void match(ParameterizedType formal, ParameterizedType concrete, List<TypeParameter> typeParameters,
                              Map<TypeParameter, ParameterizedType> inferred) {
        if (formal == null || concrete == null) return;
        if (formal.isTypeParameter() && typeParameters.contains(formal.typeParameter())) {
            if (concrete.arrays() >= formal.arrays()) {
                inferred.putIfAbsent(formal.typeParameter(), concrete.copyWithFewerArrays(formal.arrays()));
            }
            return;
        }
        if (formal.typeInfo() != null && formal.typeInfo().equals(concrete.typeInfo())
            && formal.parameters().size() == concrete.parameters().size()) {
            for (int i = 0; i < formal.parameters().size(); i++) {
                match(formal.parameters().get(i), concrete.parameters().get(i), typeParameters, inferred);
            }
        }
    }

    /**
 * A type C# can write as a type argument: no wildcard, none of the called method's own type parameters, and only
 * type parameters in scope where it is printed.
 */
/**
 * As {@link #writable}, but a wildcard use of a type with a wildcard interface inside counts, as C# writes it:
 * {@code BiFunction<A, Score<?>, ConstraintJustification>} is {@code Func<A, IScore, IConstraintJustification>}.
 */
    private static boolean writableArgument(ParameterizedType t, List<TypeParameter> typeParameters) {
        if (t.wildcard() != null || t.isTypeParameter() || t.typeInfo() == null) return writable(t, typeParameters);
        if (!t.parameters().isEmpty() && CSharpWildcardInterfaces.isWildcardUse(t)) {
            List<TypeParameter> removed = CSharpWildcardInterfaces.removed(t.typeInfo());
            for (int i = 0; i < t.parameters().size() && i < t.typeInfo().typeParameters().size(); i++) {
                if (!removed.contains(t.typeInfo().typeParameters().get(i))
                    && !writableArgument(t.parameters().get(i), typeParameters)) {
                    return false;
                }
            }
            return true;
        }
        return t.parameters().stream().allMatch(a -> writableArgument(a, typeParameters));
    }

    static boolean writable(ParameterizedType t, List<TypeParameter> typeParameters) {
        if (t.wildcard() != null) return false;
        // nor one C# cannot name here: a static member's in the companion class, an override's declaration's
        if (t.isTypeParameter()) return !typeParameters.contains(t.typeParameter()) && CSharpContext.inScope(t.typeParameter());
        if (t.typeInfo() == null) return false;
        return t.parameters().stream().allMatch(a -> writable(a, typeParameters));
    }

    static OutputBuilder constructorCall(ConstructorCall cc, Qualification q) {
        return constructorCall(cc, cc.parameterizedType(), q);
    }

    static OutputBuilder constructorCall(ConstructorCall cc, ParameterizedType type, Qualification q) {
        return CSharpExpressionPrinter1.inCall(cc, () -> constructorCallInCall(cc, type, q));
    }

    private static OutputBuilder constructorCallInCall(ConstructorCall cc, ParameterizedType type, Qualification q) {
        if (cc.anonymousClass() != null) return CSharpExpressionPrinter1.anonymousCreation(cc, type, q);
        if (type.arrays() > 0) return CSharpExpressionPrinter1.arrayCreation(cc, type, q);
        // new AndThen<>(delegate, f) of a Collector<A, ?, I>: delegate.Open_(new …Opener_<A, I, X>(f))
        CSharpOpeners.Site site = CSharpOpeners.site(cc);
        if (site != null) return CSharpOpeners.call(site, q, (i, a) -> CSharpExpressionPrinter1.argument(a, cc.constructor(), i, q));
        TypeInfo created = cc.constructor() == null ? null : cc.constructor().typeInfo();
        if (CSharpInner.withOuter(created)) {
            // an inner class's enclosing instance comes first: range.new Iterator() is new Iterator(range)
            String enclosing = cc.object() != null ? null : CSharpInner.enclosingInstance(created);
            if (cc.object() != null || enclosing != null) {
                OutputBuilder first = cc.object() != null ? receiver(cc.object(), q) : CSharpExpressionPrinter1.text(enclosing);
                return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE).add(CSharpExpressionPrinter1.text(CSharpTypeName.of(type, q)))
                        .add(CSharpExpressionPrinter1.argumentsAfter(first, cc.parameterExpressions(), cc.constructor(), q));
            }
            CSharpContext.message(CSharpPrintCode.OUTER_THIS, cc, CSharpContext.describe(cc));
        } else if (cc.object() != null) {
            CSharpContext.message(CSharpPrintCode.OUTER_THIS, cc, CSharpContext.describe(cc));
        }
        LibraryMapping.Rule rule = cc.constructor() == null || CSharpNames.translated(cc.constructor().typeInfo()) ? null
                : CSharpContext.libraries().constructor(cc.constructor());
        if (rule != null) {
            rule.namespaces().forEach(CSharpContext::using);
            List<Expression> parameterExpressions = cc.parameterExpressions();
            List<Supplier<OutputBuilder>> args = IntStream.range(0, parameterExpressions.size())
                    .mapToObj(i -> (Supplier<OutputBuilder>) () -> CSharpExpressionPrinter1.argument(parameterExpressions.get(i),
                            cc.constructor(), i, q)).toList();
            List<Supplier<OutputBuilder>> operands = parameterExpressions.stream()
                    .map(a -> (Supplier<OutputBuilder>) () -> receiver(a, q)).toList();
            return new CSharpTemplate(() -> CSharpExpressionPrinter1.text("this"), args, operands, null, type, null, q).render(rule.template(false));
        }
        ParameterizedType resolved = created(cc, type);
        String comparer = naturalComparer(cc, resolved, q);
        if (comparer != null) {
            // new TreeMap<String, V>(): ordered as Java's String.compareTo, not by the culture
            List<OutputBuilder> args = new ArrayList<>();
            for (int i = 0; i < cc.parameterExpressions().size(); i++) {
                args.add(CSharpExpressionPrinter1.argument(cc.parameterExpressions().get(i), cc.constructor(), i, q));
            }
            args.add(CSharpExpressionPrinter1.text(comparer));
            return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                    .add(CSharpExpressionPrinter1.text(CSharpTypeName.created(resolved, q)))
                    .add(args.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                            SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator())));
        }
        return new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(CSharpExpressionPrinter1.text(CSharpTypeName.created(resolved, q))).add(CSharpExpressionPrinter1.arguments(cc.parameterExpressions(), cc.constructor(), q));
    }

    /**
 * {@code JavaComparer<K>.Natural} for a TreeMap or TreeSet created without a comparator whose keys may be
 * Strings: Java orders them by {@code String.compareTo}, UTF-16 code units, where C#'s default comparer for
 * strings is the culture's ("a" before "B"). Not for keys of a number, an enum or a program type, whose default
 * comparer is Java's compareTo already; null then, and for a creation given a comparator or a sorted collection,
 * whose order it takes.
 */
    private static String naturalComparer(ConstructorCall cc, ParameterizedType created, Qualification q) {
        MethodInfo constructor = cc.constructor();
        TypeInfo t = created.typeInfo();
        if (constructor == null || t == null || created.arrays() != 0 || !SORTED_COLLECTIONS.contains(t.fullyQualifiedName())
            || created.parameters().isEmpty()) {
            return null;
        }
        for (ParameterInfo p : constructor.parameters()) {
            TypeInfo pt = p.parameterizedType().typeInfo();
            if (pt == null || !"java.util.Collection".equals(pt.fullyQualifiedName())
                              && !"java.util.Map".equals(pt.fullyQualifiedName())) {
                return null;
            }
        }
        ParameterizedType key = created.parameters().getFirst();
        boolean mayBeString = key.arrays() == 0 && key.wildcard() == null
                              && (key.isJavaLangString() || key.isJavaLangObject() || key.isTypeParameter()
                                  || key.typeInfo() != null && Set.of("java.lang.CharSequence", "java.lang.Comparable")
                                          .contains(key.typeInfo().fullyQualifiedName()));
        if (!mayBeString) return null;
        CSharpContext.using(CSharpCompat.NAMESPACE);
        return "JavaComparer<" + CSharpTypeName.argument(key, q) + ">.Natural";
    }

    /** The creation's type, its diamond resolved, remembered for its arguments' own diamonds. */
    private static ParameterizedType created(ConstructorCall cc, ParameterizedType type) {
        ParameterizedType resolved = diamond(cc, type);
        CSharpContext.created(cc, resolved);
        return resolved;
    }

    /**
 * {@code new UniMap<>(…)} whose type arguments the parser left as the class's own type parameters, which are not in
 * scope where it is created: inferred from the constructor's arguments, {@code mapping}'s
 * {@code UniNeighborhoodsMapper<Solution_, A, ResultA_>} for the parameter's {@code …Mapper<Solution_, A, NewA>};
 * else, as an argument, from the type Java inferred for its parameter: {@code share(new Bridge<>(parent),
 * group::setBridge)}'s {@code S} is {@code Bridge<NewA>}, which the method reference binds.
 */
    private static ParameterizedType diamond(ConstructorCall cc, ParameterizedType type) {
        TypeInfo created = type.typeInfo();
        if (created == null || type.arrays() != 0 || cc.constructor() == null || type.parameters().isEmpty()) return type;
        List<TypeParameter> own = created.typeParameters();
        if (type.parameters().stream().noneMatch(a -> a.isTypeParameter() && own.contains(a.typeParameter()))) {
            return CSharpExpressionPrinter1.fromCaptured(cc, type);
        }
        for (TypeInfo at = CSharpContext.currentType(); at != null; at = CstQueries.enclosing(at)) {
            if (at.equals(created)) return type; // its own type parameters are in scope
        }
        Map<TypeParameter, ParameterizedType> inferred = inferredFromArguments(cc, own);
        ParameterizedType target = argumentTarget(cc);
        // the parameter's type, or a supertype of the class, as it extends or implements it in its own type parameters:
        // HashMap<K, V> is a Map<K, V>, ListBasedScalingOrderedSet<E> a Collection<E> through Set<E>
        if (target != null && target.typeInfo() != null) {
            ParameterizedType asTarget = created.equals(target.typeInfo()) ? type
                    : CstSupertypes.asSeen(created, target.typeInfo());
            if (asTarget != null) match(asTarget, target, own, inferred);
        }
        List<ParameterizedType> arguments = new ArrayList<>();
        for (ParameterizedType a : type.parameters()) {
            if (a.isTypeParameter() && own.contains(a.typeParameter())) {
                ParameterizedType t = inferred.get(a.typeParameter());
                if (t == null || !writableNested(t, own)) return type;
                arguments.add(t);
            } else {
                arguments.add(a);
            }
        }
        return type.withParameters(arguments);
    }

    static Map<TypeParameter, ParameterizedType> inferredFromArguments(ConstructorCall cc,
                                                                            List<TypeParameter> own) {
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        List<ParameterInfo> parameters = cc.constructor().parameters();
        for (int i = 0; i < parameters.size() && i < cc.parameterExpressions().size(); i++) {
            if (parameters.get(i).isVarArgs()) break;
            Expression arg = CSharpExpressionPrinter1.unwrap(cc.parameterExpressions().get(i));
            ParameterizedType captured = CSharpContext.capturedType(arg);
            match(parameters.get(i).parameterizedType(), captured != null ? captured : arg.parameterizedType(), own,
                    inferred);
        }
        return inferred;
    }

    /**
 * {@link #writable}, but with wildcards below the top, which C# prints as it prints the argument's or the
 * parameter's type they come from: {@code Pair<Mapper<A>, Collector<A, ?, R>>} of a {@code collector} declared so.
 */
    static boolean writableNested(ParameterizedType t, List<TypeParameter> typeParameters) {
        if (t.wildcard() != null) return false;
        if (t.isTypeParameter() || t.typeInfo() == null) return writable(t, typeParameters);
        return t.parameters().stream().allMatch(a -> a.wildcard() == null ? writableNested(a, typeParameters)
                : a.wildcard().isUnbound() || writableNested(a.withWildcard(null), typeParameters));
    }

    /** The type Java inferred for the parameter that {@code cc} is the argument of; null when not a call's argument. */
    private static ParameterizedType argumentTarget(ConstructorCall cc) {
        Expression enclosing = CSharpContext.enclosingCall();
        MethodInfo method;
        List<Expression> args;
        Map<TypeParameter, ParameterizedType> bound = new HashMap<>();
        if (enclosing instanceof MethodCall mc && mc.methodInfo() != null) {
            method = mc.methodInfo();
            args = mc.parameterExpressions();
            match(method.returnType(), mc.concreteReturnType(), method.typeParameters(), bound);
        } else if (enclosing instanceof ConstructorCall outer && outer.constructor() != null
                   && CSharpContext.created(outer) instanceof ParameterizedType created && created.typeInfo() != null
                   && created.parameters().size() == created.typeInfo().typeParameters().size()) {
            // new State<>(true, new TreeMap<>(), …): the outer creation's type arguments, as printed
            method = outer.constructor();
            args = outer.parameterExpressions();
            for (int i = 0; i < created.parameters().size(); i++) {
                bound.put(created.typeInfo().typeParameters().get(i), created.parameters().get(i));
            }
        } else {
            return null;
        }
        int index = -1;
        for (int i = 0; i < args.size(); i++) {
            Expression arg = CSharpExpressionPrinter1.unwrap(args.get(i));
            // cond ? new A<>() : new B<>(): either branch is the argument
            if (arg == cc || arg instanceof InlineConditional ic && (CSharpExpressionPrinter1.unwrap(ic.ifTrue()) == cc || CSharpExpressionPrinter1.unwrap(ic.ifFalse()) == cc)) {
                index = i;
            }
        }
        if (index < 0 || index >= method.parameters().size() || method.parameters().get(index).isVarArgs()) return null;
        ParameterizedType formal = method.parameters().get(index).parameterizedType();
        if (formal.arrays() != 0) return null;
        if (formal.isTypeParameter()) return bound.get(formal.typeParameter());
        ParameterizedType target = CstSupertypes.substituteDroppingWildcards(formal, bound);
        return target.parameters().stream().allMatch(a -> a.wildcard() == null) ? target : null;
    }

    /**
 * An instance of a class of the program where a library's functional interface that C# has as a delegate is
 * expected ({@code Comparator<T>} is {@code Comparison<T>}): the class cannot implement the delegate, a sealed
 * type (CS0509), so its method is the method group, {@code new DepthFirst().Compare}.
 */
    static OutputBuilder implementationGroup(Expression value, Expression inner, ParameterizedType target,
                                                     Qualification q) {
        MethodInfo implementation = implementation(inner, target);
        if (implementation == null) return null;
        return new OutputBuilderImpl().add(receiver(value, q)).add(SymbolEnum.DOT)
                .add(CSharpExpressionPrinter1.text(CSharpNames.method(implementation)));
    }

    /** The class's method {@link #implementationGroup} takes the group of; null where it takes none. */
    static MethodInfo implementation(Expression inner, ParameterizedType target) {
        if (target.arrays() != 0 || target.typeInfo() == null) return null;
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || from.typeInfo() == null || from.typeInfo().isInterface()
            || !CSharpNames.translated(from.typeInfo()) || !CSharpTypeName.libraryDelegate(target.typeInfo())) {
            return null;
        }
        // Comparator declares equals(Object) abstract too
        MethodInfo sam = target.typeInfo().isFunctionalInterface() ? target.typeInfo().singleAbstractMethod() : null;
        if (sam == null) return null;
        return CstQueries.classChain(from.typeInfo()).stream().flatMap(t -> t.methods().stream())
                .filter(m -> !m.isStatic() && m.overrides().contains(sam)).findFirst().orElse(null);
    }

    /**
 * {@code Func<A, Property_>} where {@code Func<A, object>} is expected: Java's wildcard takes it, C#'s variance
 * only for reference types, which a type parameter is not known to be (CS1503). A variable is wrapped in a lambda,
 * {@code x0_ => leftMapping(x0_)}, which C# converts with the boxing; a parameter that differs is cast,
 * {@code x0_ => mapping((A) x0_)}. Not another expression, which the lambda would evaluate at every call, and only
 * where each type argument that differs is {@code object} in the target.
 */
    static OutputBuilder widenedDelegate(Expression inner, ParameterizedType target, Qualification q) {
        if (!(inner instanceof VariableExpression) || target.arrays() != 0) return null;
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || from.typeInfo() == null || !from.typeInfo().equals(target.typeInfo())) {
            return null;
        }
        // only Java's wildcard or raw use, object in C#: a position of the callee's own type parameter, Func<A, Result_>,
        // C# infers from the variable, which a lambda would hide (CS0411); a generic constructor's is object, C# has none
        if (!target.parameters().isEmpty() && target.parameters().size() != from.parameters().size()) return null;
        boolean differs = target.parameters().isEmpty() && !from.parameters().isEmpty();
        for (int i = 0; i < target.parameters().size(); i++) { // a raw target is object throughout
            ParameterizedType ti = target.parameters().get(i);
            String t = constructorTypeParameter(ti) ? "object" : CSharpTypeName.argument(ti, q);
            if (t.equals(CSharpTypeName.argument(from.parameters().get(i), q))) continue;
            if (!"object".equals(t)) return null;
            differs = true;
        }
        if (!differs) return null;
        LibraryMapping.TypeMapping mapping = CSharpContext.libraries().type(from.typeInfo());
        if (!CSharpContext.program().delegate(from.typeInfo()) && (mapping == null || !mapping.delegate())) return null;
        MethodInfo sam = CstSupertypes.singleAbstractMethod(from.typeInfo());
        if (sam == null) return null;
        Map<NamedType, ParameterizedType> fromMap = from.initialTypeParameterMap();
        Map<NamedType, ParameterizedType> toMap = target.initialTypeParameterMap();
        List<String> names = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        for (int i = 0; i < sam.parameters().size(); i++) {
            String name = "x" + i + "_";
            names.add(name);
            ParameterizedType formal = sam.parameters().get(i).parameterizedType();
            ParameterizedType fromP = formal.isTypeParameter() ? fromMap.get(formal.typeParameter()) : formal;
            ParameterizedType toP = formal.isTypeParameter() ? toMap.get(formal.typeParameter()) : formal;
            String fromS = fromP == null ? null : CSharpTypeName.of(fromP, q);
            boolean same = fromS == null || toP != null && fromS.equals(CSharpTypeName.of(toP, q));
            arguments.add(same ? name : "(" + fromS + ") " + name);
        }
        String parameters = names.size() == 1 ? names.getFirst() : "(" + String.join(", ", names) + ")";
        OutputBuilder b = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text(parameters + " =>")).add(SpaceEnum.ONE).add(CSharpExpressionPrinter1.print(inner, q));
        if (arguments.isEmpty()) b.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        else b.add(SymbolEnum.LEFT_PARENTHESIS).add(CSharpExpressionPrinter1.text(String.join(", ", arguments))).add(SymbolEnum.RIGHT_PARENTHESIS);
        // the result of a type parameter, which may be null, as the delegate's object, which the analysis has not
        // (CS8603): as a method reference's unboxing has it
        if (!sam.isVoid() && sam.returnType().isTypeParameter()
            && CSharpContext.program().facts().nullabilityAnalysed()) {
            b.add(CSharpNullability.NULL_FORGIVING);
        }
        return b;
    }

    /**
 * A value of a type's wildcard interface ({@link CSharpWildcardInterfaces}), raw or {@code ?} in Java, where the
 * type itself is expected with arguments C# can name here.
 */
    static boolean wildcardIntoGeneric(Expression inner, ParameterizedType target) {
        ParameterizedType from = inner.parameterizedType();
        if (from == null || from.arrays() != 0 || target.arrays() != 0 || from.typeInfo() == null
            || !from.typeInfo().equals(target.typeInfo()) || target.parameters().isEmpty()
            || !CSharpWildcardInterfaces.isWildcardUse(from) || CSharpWildcardInterfaces.isWildcardUse(target)
            || inner instanceof NullConstant || inner instanceof Lambda || inner instanceof MethodReference) {
            return false;
        }
        return target.parameters().stream().allMatch(a -> a.wildcard() == null && !constructorTypeParameter(a))
               && writable(target, List.of()) && CSharpExpressionPrinter1.ownTypeParameters(target);
    }

    /** A generic constructor's own type parameter, which C# erases to object: it has no generic constructors. */
    static boolean constructorTypeParameter(ParameterizedType t) {
        if (!t.isTypeParameter() || t.arrays() != 0) return false;
        var owner = t.typeParameter().getOwner();
        return owner != null && owner.isRight() && owner.getRight().isConstructor();
    }

    /**
 * The result type of a constructor reference passed where the called method infers a type parameter of its own from
 * the functional interface's result, when that is another type than the one created; null otherwise.
 */
    static ParameterizedType resultInferred(MethodReference mr, MethodInfo method, int i) {
        if (!mr.methodInfo().isConstructor() || method == null || method.typeParameters().isEmpty()
            || i >= method.parameters().size() || method.parameters().get(i).isVarArgs()) {
            return null;
        }
        ParameterizedType formal = method.parameters().get(i).parameterizedType();
        TypeInfo functional = formal.typeInfo();
        if (formal.arrays() != 0 || functional == null || formal.parameters().size() != functional.typeParameters().size()) {
            return null;
        }
        MethodInfo sam = CstSupertypes.singleAbstractMethod(functional);
        if (sam == null || sam.isVoid() || !sam.returnType().isTypeParameter() || sam.returnType().arrays() != 0) return null;
        int index = functional.typeParameters().indexOf(sam.returnType().typeParameter());
        if (index < 0) return null;
        ParameterizedType r = formal.parameters().get(index);
        if (!r.isTypeParameter() || !r.typeParameter().isMethodTypeParameter()
            || !method.typeParameters().contains(r.typeParameter())) {
            return null;
        }
        ParameterizedType result = mr.concreteReturnType();
        ParameterizedType created = mr.scope().parameterizedType();
        return result != null && result.arrays() == 0 && result.typeInfo() != null && created != null
               && created.arrays() == 0 && !result.typeInfo().equals(created.typeInfo())
               && result.typeInfo().isInterface() && writable(result, method.typeParameters()) ? result : null;
    }

    /**
 * An argument of a call printed by a template: as {@link #converted}, and a collection's {@code Object} parameter, of {@code get}, {@code contains},
 * {@code remove}, is its element or key: a type argument, the value type itself in C#.
 */
    static OutputBuilder templateArgument(MethodCall mc, int i, Expression value, Qualification q) {
        MethodInfo method = mc.methodInfo();
        ParameterizedType type = CSharpExpressionPrinter1.parameterType(method, i);
        Expression inner = CSharpExpressionPrinter1.unwrap(value);
        boolean elementOrKey = type != null && type.arrays() == 0
                               && (type.isJavaLangObject() || type.isTypeParameter() && !type.typeParameter().isMethodTypeParameter())
                               && "java.util".equals(method.typeInfo().packageName())
                               && !method.typeInfo().typeParameters().isEmpty();
        if (elementOrKey && type.isJavaLangObject() && CSharpExpressionPrinter1.unboxed(inner)) {
            return CSharpExpressionPrinter1.unboxCast(CSharpTypeName.argument(inner.parameterizedType(), q), value, q);
        }
        // a library's parameter of a struct, TimeSpan, is the value type
        if (type != null && !type.isTypeParameter() && CSharpTypeName.valueReference(type) && CSharpExpressionPrinter1.unboxedValue(value)) {
            return CSharpExpressionPrinter1.unboxCast(CSharpTypeName.argument(inner.parameterizedType(), q), value, q);
        }
        // the method's own type parameter is whatever the template makes of it: Objects.requireNonNullElse(i, 0) is
        // i ?? 0, of the int? itself
        if (type != null && type.isTypeParameter() && type.typeParameter().isMethodTypeParameter() && CSharpExpressionPrinter1.unboxed(inner)) {
            type = null;
        }
        // and a null literal stays null: Optional.ofNullable(null) is the template's $1, whose default would have no
        // type in (default) != null (CS0034)
        if (type != null && type.isTypeParameter() && type.typeParameter().isMethodTypeParameter()
            && inner instanceof NullConstant) {
            type = null;
        }
        // the library type's own T as the receiver has it: reduce(NONE, …) of a Stream<DefaultBiJoiner<A, B>> and a raw NONE
        ParameterizedType seen = asReceiverHasIt(mc, type);
        if (seen != null && (CSharpExpressionPrinter1.rawIntoGeneric(inner, seen) || wildcardIntoGeneric(inner, seen))) type = seen;
        return CSharpNullability.intoParameter(value, method, i, elementOrKey, CSharpExpressionPrinter1.converted(value, type, q));
    }

    /** A type parameter of the called method's type, as the receiver's type has it; null when there is none. */
    private static ParameterizedType asReceiverHasIt(MethodCall mc, ParameterizedType type) {
        MethodInfo method = mc.methodInfo();
        if (type == null || !type.isTypeParameter() || type.arrays() != 0 || type.typeParameter().isMethodTypeParameter()
            || method.isStatic() || mc.object() == null || mc.object().parameterizedType() == null
            || mc.object().parameterizedType().typeInfo() == null) {
            return null;
        }
        ParameterizedType seen = CstSupertypes.asSeen(mc.object().parameterizedType(), method.typeInfo());
        if (seen == null) return null;
        ParameterizedType substituted = CstSupertypes.substitute(type, CstSupertypes.typeArguments(seen));
        return substituted.isTypeParameter() || substituted.typeInfo() == null ? null : substituted;
    }

    /** A JDK method the mapping does not translate: counted, printed with its Java name. */
    private static void unmapped(MethodInfo method, Expression use) {
        if (!CSharpNames.translated(method.typeInfo()) && !"ToString".equals(CSharpNames.method(method))
            && !"Equals".equals(CSharpNames.method(method)) && !"GetHashCode".equals(CSharpNames.method(method))) {
            CSharpContext.message(CSharpPrintCode.UNMAPPED_JDK, use,
                    method.typeInfo().fullyQualifiedName() + "." + method.name() + "/" + method.parameters().size());
        }
    }

    /**
 * A method group where C# has one: {@code Owner.Method} for a static method, {@code receiver.Method} for a bound
 * receiver. A lambda otherwise: {@code (a, b) => a.Method(b)} for an unbound receiver, {@code (a) => new T(a)}
 * for a constructor.
 */
    static OutputBuilder methodReference(MethodReference mr, Qualification q) {
        return methodReference(mr, null, q);
    }

    /**
 * The type of a method reference's lambda parameter: the delegate's, {@code T?} where the delegate's parameter is
 * nullable ({@link CSharpTypePrinter#delegateParameter}), which a lambda's typed parameter must match (CS8622).
 */
    private static String typedParameter(MethodReference mr, int i, Qualification q) {
        ParameterizedType pt = mr.concreteParameterTypes().get(i);
        return asDelegateHasIt(mr.parameterizedType() == null ? null : mr.parameterizedType().typeInfo(), i, pt,
                CSharpTypeName.argument(pt, q));
    }

    /** {@code type}, a lambda's typed parameter {@code i}, {@code T?} where the delegate's parameter is nullable. */
    static String asDelegateHasIt(TypeInfo functional, int i, ParameterizedType pt, String type) {
        if (functional == null || !CSharpContext.program().delegate(functional)) return type;
        MethodInfo sam = CSharpProgram.invoked(functional);
        if (sam == null || i >= sam.parameters().size()) return type;
        NullableState state = CSharpTypePrinter.delegateParameter(functional, sam, sam.parameters().get(i));
        return CSharpTypeName.withNullable(pt, type, state);
    }

    /** {@code resultType}: a constructor reference's result as C# is to infer it, the created value cast to it. */
    static OutputBuilder methodReference(MethodReference mr, ParameterizedType resultType, Qualification q) {
        MethodInfo method = mr.methodInfo();
        Expression scope = mr.scope();
        int arity = mr.concreteParameterTypes().size();
        List<String> parameters = IntStream.range(0, arity).mapToObj(i -> "p" + i).toList();
        String parameterList = arity == 1 ? parameters.getFirst() : String.join(", ", parameters);
        if (arity != 1) parameterList = "(" + parameterList + ")";
        // String::length is (string p0) => p0.Length: typed, so that a generic method taking it infers its arguments;
        // as the delegate's type arguments, BigDecimal::add's (decimal p0, decimal p1), not decimal?
        boolean unboundReceiver = scope instanceof TypeExpression && !method.isStatic() && !method.isConstructor();
        if (CSharpContext.program().isInvoke(method)) {
            // f::apply of a delegate f: f.Invoke; ExprentIterator::processExprent: (ExprentIterator p0, …) => p0(…)
            if (!unboundReceiver) return new OutputBuilderImpl().add(receiver(scope, q)).add(SymbolEnum.DOT).add(CSharpExpressionPrinter1.text("Invoke"));
            String typedList = IntStream.range(0, arity).mapToObj(i -> CSharpTypeName.argument(mr.concreteParameterTypes().get(i), q)
                    + " " + parameters.get(i)).collect(Collectors.joining(", ", "(", ")"));
            return CSharpExpressionPrinter1.text(typedList + " => " + parameters.getFirst() + "("
                        + String.join(", ", parameters.subList(1, parameters.size())) + ")");
        }
        if (unboundReceiver && arity > 0) {
            parameterList = IntStream.range(0, arity).mapToObj(i -> typedParameter(mr, i, q) + " " + parameters.get(i))
                    .collect(Collectors.joining(", ", "(", ")"));
        }
        if (method.isConstructor()) {
            ParameterizedType type = scope.parameterizedType();
            // Triple::new is raw: the type Java inferred for the reference's result, new Triple<A, B, C>(…)
            // (LinkedHashSet::new as an IntFunction<Set<Value>>: LinkedHashSet<E> is a Set<E> through SequencedSet<E>,
            // new LinkedHashSet<Value>(…))
            ParameterizedType result = mr.concreteReturnType();
            boolean raw = type.arrays() == 0 && type.parameters().isEmpty() && type.typeInfo() != null
                          && !type.typeInfo().typeParameters().isEmpty();
            ParameterizedType inferred = raw && result != null && result.arrays() == 0
                                         && result.parameters().stream().allMatch(a -> a.wildcard() == null)
                    ? CSharpExpressionPrinter2.ownParametersAs(type.typeInfo(), result) : null;
            if (inferred != null) {
                type = inferred;
            } else if (raw) {
                // else from the delegate's parameters: Node::new as a Builder<String, List<String>>, whose
                // (K key, int index, Lifecycle<T> next) are (string, int, ILifecycle<List<string>>)
                type = fromParameters(type, method, mr.concreteParameterTypes());
            }
            String created = type.arrays() > 0
                    ? "new " + CSharpTypeName.element(type.copyWithoutArrays(), q) + "[" + parameters.getFirst() + "]"
                      + "[]".repeat(type.arrays() - 1)
                    : "new " + CSharpTypeName.of(type, q) + "(" + String.join(", ", CSharpExpressionPrinter2.constructorArguments(type, parameters))
                      + ")";
            if (resultType != null) created = "(" + CSharpTypeName.of(resultType, q) + ") " + created;
            return CSharpExpressionPrinter1.text(parameterList + " => " + created);
        }
        LibraryMapping.Rule rule = CSharpNames.translated(method.typeInfo()) ? null
                : CSharpContext.libraries().call(method, null);
        if (rule != null) {
            // a lambda around the member's translation: String::length is p0 => p0.Length
            rule.namespaces().forEach(CSharpContext::using);
            boolean unbound = scope instanceof TypeExpression && !method.isStatic();
            Supplier<OutputBuilder> receiver = unbound ? () -> CSharpExpressionPrinter1.text(parameters.getFirst())
                    : scope instanceof TypeExpression te ? () -> CSharpExpressionPrinter1.text(CSharpTypeName.nameInExpression(te.parameterizedType().typeInfo(), q))
                    : () -> receiver(scope, q);
            List<Supplier<OutputBuilder>> args = parameters.subList(unbound ? 1 : 0, parameters.size())
                    .stream().map(p -> (Supplier<OutputBuilder>) () -> CSharpExpressionPrinter1.text(p)).toList();
            OutputBuilder body = new CSharpTemplate(receiver, args, args, null, mr.concreteReturnType(),
                    scope.parameterizedType(), q).render(rule.template(false));
            if (rule.nullableResult() && CSharpContext.program().facts().nullabilityAnalysed()) {
                // a result C# declares nullable where Java's is not, Class::getName's p0.FullName, as a call has it
                body = (rule.template(false).contains(" ") ? parenthesized(body) : body).add(CSharpNullability.NULL_FORGIVING);
            }
            return new OutputBuilderImpl().add(CSharpExpressionPrinter1.text(parameterList)).add(SymbolEnum.binaryOperator("=>")).add(body);
        }
        unmapped(method, mr);
        String name = CSharpNames.method(method) + methodGroupTypeArguments(mr, q);
        // a result the analysis has nullable goes to a delegate whose result is not (CS8621): a method group takes no
        // !, a lambda does, where its receiver is evaluated once anyway, a type or this
        boolean forgiven = !method.isVoid() && CSharpContext.program().facts().nullabilityAnalysed()
                           && CSharpContext.program().facts().nullable(method) == NullableState.NULLABLE;
        // a Long result into a Function's R, C#'s long: a lambda that unboxes, (long) p0.Result()
        String cast = unboxing(mr, method, q);
        // with the analysis, ! as on any unboxing cast: C# thinks the long? may be null (CS8629)
        boolean asserted = forgiven || cast != null && CSharpContext.program().facts().nullabilityAnalysed();
        String arguments = "(" + String.join(", ", parameters) + ")" + (asserted ? "!" : "");
        String prefix = cast == null ? "" : cast;
        // a member C# has as a property, ConstraintRef::id: (ConstraintRef p0) => p0.Id, () => location.ClassName
        String property = method.isStatic() ? null : CSharpExpressionPrinter2.propertyOf(method);
        if (property != null && (unboundReceiver ? arity == 1 : arity == 0)) {
            OutputBuilder lambda = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text(parameterList + " =>")).add(SpaceEnum.ONE);
            if (cast != null) lambda.add(CSharpExpressionPrinter1.text(cast.strip())).add(SpaceEnum.ONE);
            lambda.add(unboundReceiver ? CSharpExpressionPrinter1.text(parameters.getFirst()) : receiver(scope, q));
            return lambda.add(SymbolEnum.DOT).add(CSharpExpressionPrinter1.text(property + (asserted ? "!" : "")));
        }
        if (scope instanceof TypeExpression te) {
            String owner = CSharpTypeName.nameInExpression(te.parameterizedType().typeInfo(), q);
            if (method.isStatic()) {
                return CSharpExpressionPrinter1.text(forgiven || cast != null ? parameterList + " => " + prefix + owner + "." + name + arguments
                        : owner + "." + name);
            }
            // String::length: the first parameter is the receiver
            return CSharpExpressionPrinter1.text(parameterList + " => " + prefix + parameters.getFirst() + "." + name + "("
                        + String.join(", ", parameters.subList(1, parameters.size())) + ")" + (asserted ? "!" : ""));
        }
        if ((forgiven || cast != null) && scope instanceof VariableExpression ve && ve.variable() instanceof This) {
            OutputBuilder lambda = new OutputBuilderImpl().add(CSharpExpressionPrinter1.text(parameterList + " =>")).add(SpaceEnum.ONE);
            if (cast != null) lambda.add(CSharpExpressionPrinter1.text(cast.strip())).add(SpaceEnum.ONE);
            return lambda.add(receiver(scope, q)).add(SymbolEnum.DOT).add(CSharpExpressionPrinter1.text(name + arguments));
        }
        return new OutputBuilderImpl().add(receiver(scope, q)).add(SymbolEnum.DOT).add(CSharpExpressionPrinter1.text(name));
    }

    /**
 * {@code (long) }, where a method reference's result is a boxed value, {@code long?}, and its functional
 * interface's result a type parameter, which C# has as the value type itself: {@code Func<State, long>}. Null
 * otherwise.
 */
    private static String unboxing(MethodReference mr, MethodInfo method, Qualification q) {
        if (method.isVoid() || method.isConstructor() || !CSharpTypeName.valueReference(method.returnType())) return null;
        ParameterizedType target = mr.parameterizedType();
        TypeInfo functional = target == null ? null : target.typeInfo();
        MethodInfo sam = functional == null ? null : CstSupertypes.singleAbstractMethod(functional);
        if (sam == null || !sam.returnType().isTypeParameter()) return null;
        return "(" + CSharpTypeName.argument(method.returnType(), q) + ") ";
    }

    /** A raw constructor reference's type, its type arguments from the delegate's parameter types; else as it is. */
    private static ParameterizedType fromParameters(ParameterizedType type, MethodInfo constructor,
                                                    List<ParameterizedType> concrete) {
        List<TypeParameter> own = type.typeInfo().typeParameters();
        if (concrete == null || concrete.size() != constructor.parameters().size()) return type;
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        for (int i = 0; i < concrete.size(); i++) {
            if (constructor.parameters().get(i).isVarArgs()) return type;
            match(constructor.parameters().get(i).parameterizedType(), concrete.get(i), own, inferred);
        }
        List<ParameterizedType> arguments = new ArrayList<>();
        for (TypeParameter tp : own) {
            ParameterizedType t = inferred.get(tp);
            if (t == null || !writable(t, List.of())) return type;
            arguments.add(t);
        }
        return type.withParameters(arguments);
    }

    /**
 * A method group's type arguments that C# does not infer from the delegate it converts to, a type parameter only the
 * result mentions: {@code AbstractMinMaxSlot::minState} as a {@code Supplier<State<R, R>>} is
 * {@code AbstractMinMaxSlot.MinState<R>} (CS0411). Empty when C# infers them, or one is not writable.
 */
    private static String methodGroupTypeArguments(MethodReference mr, Qualification q) {
        MethodInfo method = mr.methodInfo();
        List<TypeParameter> typeParameters = method.typeParameters();
        if (typeParameters.isEmpty() || !CSharpNames.translated(method.typeInfo())
            || CSharpContext.program().erasedGeneric(method) || !uninferable(method)) {
            return "";
        }
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        match(method.returnType(), mr.concreteReturnType(), typeParameters, inferred);
        List<ParameterizedType> concrete = mr.concreteParameterTypes();
        int offset = concrete.size() - method.parameters().size(); // an unbound receiver comes first
        for (int i = 0; i < method.parameters().size() && offset >= 0 && i + offset < concrete.size(); i++) {
            match(method.parameters().get(i).parameterizedType(), concrete.get(i + offset), typeParameters, inferred);
        }
        return typeArgumentList(typeParameters, inferred, false, q);
    }

    /** {@code unchecked(e)}: C#'s wrapping arithmetic, Java's, for a constant expression C# would reject. */
    static OutputBuilder unchecked(OutputBuilder e) {
        return new OutputBuilderImpl().add(CSharpExpressionPrinter1.text("unchecked")).add(SymbolEnum.LEFT_PARENTHESIS).add(e)
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /**
 * The tested type's type parameters as {@link #rawTestArguments} infers them, some of them maybe; null when the
 * test is not of a raw generic program type that the operand's type relates to.
 */
    static Map<TypeParameter, ParameterizedType> rawTestInferred(ParameterizedType tested, ParameterizedType operand) {
        TypeInfo target = tested == null ? null : tested.typeInfo();
        if (target == null || tested.arrays() != 0 || !tested.parameters().isEmpty()
            || target.typeParameters().isEmpty() || !CSharpNames.translated(target) || operand == null
            || operand.arrays() != 0 || operand.typeInfo() == null || operand.parameters().isEmpty()
            || target.equals(operand.typeInfo())) {
            return null;
        }
        ParameterizedType asOperand = CstSupertypes.asSeen(target, operand.typeInfo());
        Map<TypeParameter, ParameterizedType> inferred = new HashMap<>();
        if (asOperand != null) {
            match(asOperand, operand, target.typeParameters(), inferred);
        } else {
            // siblings, PhaseTermination<S> and SolverTermination<S> both a Termination<S>: Java lets no class have a
            // generic supertype twice, with two arguments, so the shared one determines them
            ParameterizedType shared = CstSupertypes.firstAsSeen(target, t -> !t.typeParameters().isEmpty()
                    && CstSupertypes.asSeen(operand, t) != null);
            if (shared == null) return null;
            match(shared, CstSupertypes.asSeen(operand, shared.typeInfo()), target.typeParameters(), inferred);
        }
        return inferred;
    }

    /**
 * A type test against a raw generic type, {@code termination instanceof PhaseTermination p} of a
 * {@code SolverTermination<Solution_>}: the arguments the operand's type determines, as the tested type extends
 * it, {@code is IPhaseTermination<Solution_> p}. Any object that passes Java's test has them; C#'s
 * {@code <object>} would match none of them. {@code tested} itself when it is not raw or the operand does not
 * determine every argument. A tested type that does not extend the operand's takes them through a generic supertype
 * the two share.
 */
    static ParameterizedType rawTestArguments(ParameterizedType tested, ParameterizedType operand) {
        Map<TypeParameter, ParameterizedType> inferred = rawTestInferred(tested, operand);
        if (inferred == null) return tested;
        TypeInfo target = tested.typeInfo();
        List<ParameterizedType> arguments = new ArrayList<>();
        for (TypeParameter tp : target.typeParameters()) {
            ParameterizedType t = inferred.get(tp);
            if (t == null || !writable(t, List.of())) return tested;
            arguments.add(t);
        }
        return tested.withParameters(arguments);
    }

    /** {@code @1} in a template: a receiver, or a literal ({@code (decimal) 3L}), which is an atom as an operand. */
    static OutputBuilder templateOperand(Expression e, Qualification q) {
        if (e instanceof ConstantExpression<?> c && !(e instanceof NullConstant) && !negative(c)) return CSharpExpressionPrinter1.print(e, q);
        // an operand is the value: BigDecimal.add's @0 + @1 of a decimal? is ((decimal) x)
        if (CSharpExpressionPrinter1.unboxedValue(e)) return parenthesized(CSharpExpressionPrinter1.unboxCast(CSharpTypeName.argument(e.parameterizedType(), q), e, q));
        return receiver(e, q);
    }

    static OutputBuilder parenthesized(OutputBuilder b) {
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(b).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /** A constant C# writes with a leading minus: a negative number, or a negative zero. */
    static boolean negative(ConstantExpression<?> c) {
        return switch (c.constant()) {
            case Double d -> Double.isFinite(d) && (d < 0 || d == 0 && 1 / d < 0);
            case Float f -> Float.isFinite(f) && (f < 0 || f == 0 && 1 / f < 0);
            case Number n -> n.longValue() < 0;
            default -> false;
        };
    }

    /** The receiver of {@code .member}: anything but an atom is enclosed. */
    static OutputBuilder receiver(Expression e, Qualification q) {
        OutputBuilder inner = CSharpExpressionPrinter1.print(e, q);
        return CSharpExpressionPrinter2.atomic(e) ? inner : new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(inner)
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    static OutputBuilder arguments(List<Expression> args, MethodInfo method, Qualification q) {
        return CSharpExpressionPrinter1.arguments(args, method, q);
    }

    static OutputBuilder arguments(List<Expression> args, Qualification q) {
        return CSharpExpressionPrinter1.arguments(args, q);
    }

    static OutputBuilder argumentsAfter(OutputBuilder first, List<Expression> args, MethodInfo method, Qualification q) {
        return CSharpExpressionPrinter1.argumentsAfter(first, args, method, q);
    }
    static boolean asLambda(ConstructorCall cc) { return CSharpExpressionPrinter2.asLambda(cc); }
    static String charLiteral(char c) { return CSharpExpressionPrinter1.charLiteral(c); }
    static OutputBuilder condition(Expression e, Qualification q) { return CSharpExpressionPrinter2.condition(e, q); }
    static OutputBuilder converted(Expression value, ParameterizedType target, Qualification q) {
        return CSharpExpressionPrinter1.converted(value, target, q);
    }

    static boolean converts(Expression value, ParameterizedType target) {
        return CSharpExpressionPrinter1.converts(value, target);
    }

    static OutputBuilder initializer(Expression value, ParameterizedType declared, Qualification q) {
        return CSharpExpressionPrinter1.initializer(value, declared, q);
    }
    static boolean javaStringConversion(Expression e) { return CSharpExpressionPrinter2.javaStringConversion(e); }
    static boolean javaToString(MethodCall mc) { return CSharpExpressionPrinter2.javaToString(mc); }
    static boolean nullableResult(MethodCall mc) { return CSharpExpressionPrinter2.nullableResult(mc); }
    static OutputBuilder operand(Precedence precedence, Expression e, Qualification q) {
        return CSharpExpressionPrinter1.operand(precedence, e, q);
    }
    static boolean ownTypeParameters(ParameterizedType t) { return CSharpExpressionPrinter1.ownTypeParameters(t); }
    static ParameterizedType parameterType(MethodInfo method, int i) {
        return CSharpExpressionPrinter1.parameterType(method, i);
    }
    public static OutputBuilder print(Expression e, Qualification q) { return CSharpExpressionPrinter1.print(e, q); }
    public static OutputBuilder printStatement(Expression e, Qualification q) {
        return CSharpExpressionPrinter1.printStatement(e, q);
    }

    static String recordPattern(RecordPattern p, ParameterizedType component, Qualification q) {
        return CSharpExpressionPrinter2.recordPattern(p, component, q);
    }

    static OutputBuilder returned(Expression value, Qualification q) {
        return CSharpExpressionPrinter1.returned(value, q);
    }
    static String stringLiteral(String s) { return CSharpExpressionPrinter1.stringLiteral(s); }
    static OutputBuilder stringOperand(Precedence precedence, Expression e, Qualification q) {
        return CSharpExpressionPrinter2.stringOperand(precedence, e, q);
    }

    static CSharpExpressionPrinter1.TypeLiteral typeLiteral(TypeInfo anonymous) {
        return CSharpExpressionPrinter2.typeLiteral(anonymous);
    }

    static OutputBuilder unboxCast(String primitive, Expression value, Qualification q) {
        return CSharpExpressionPrinter1.unboxCast(primitive, value, q);
    }
    static boolean unboxed(Expression e) { return CSharpExpressionPrinter1.unboxed(e); }
    static Expression unwrap(Expression e) { return CSharpExpressionPrinter1.unwrap(e); }
    static OutputBuilder valueOf(Expression e, Qualification q) { return CSharpExpressionPrinter2.valueOf(e, q); }
    static OutputBuilder variable(Variable v, Qualification q) { return CSharpExpressionPrinter2.variable(v, q); }
}
