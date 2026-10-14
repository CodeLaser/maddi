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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.print.translate.TranslationState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * The state of one file's printing, shared by the printers: they are created through the cst-api factories, whose
 * signatures carry no context. One per thread; {@link CSharpCompilationUnitPrinter} resets it per file. What every
 * Java-to-X printer keeps (the types, methods, statements and calls being printed, the referenced types, the
 * messages) is a {@link TranslationState}.
 */
final class CSharpContext {

    private static final class State {
        final TranslationState<CSharpPrintCode> base = new TranslationState<>();
        TypeInfo companion;
        final Map<TypeInfo, Boolean> hasCompanion = new HashMap<>();
        final Set<String> usings = new TreeSet<>();
        Set<Object> privateReachedFromOutside = Set.of();
        CSharpProgram program = CSharpProgram.NONE;
        final Map<ConstructorCall, ParameterizedType>
                created = new IdentityHashMap<>();
        final Map<TypeInfo, CSharpAnonymous.Hoisted> hoisted = new HashMap<>();
        final Deque<Scope> scopes = new ArrayDeque<>();
    }

    /**
     * A block's locals: the Java name to the C# name, and the names it declares at its own level, later ones too;
     * the Java name to the local's nullability, as the analysis has it ({@link CSharpNullability}).
     */
    private record Scope(Map<String, String> names, Set<String> pending,
                         Map<String, NullableState> states) {
    }

    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private CSharpContext() {
    }

    /** A new file: no types, no messages, no usings. */
    static void reset() {
        STATE.set(new State());
    }

    static void hoisted(CSharpAnonymous.Hoisted hoisted) {
        STATE.get().hoisted.put(hoisted.type(), hoisted);
    }

    /** The hoisted form of an anonymous class; null when it is not hoisted (yet). */
    static CSharpAnonymous.Hoisted hoisted(TypeInfo anonymous) {
        return STATE.get().hoisted.get(anonymous);
    }

    /** The type being printed is a hoisted anonymous class that uses its enclosing instance: its {@code outer}. */
    static CSharpAnonymous.Hoisted hoistedWithOuter() {
        TypeInfo current = currentType();
        CSharpAnonymous.Hoisted h = current == null ? null : STATE.get().hoisted.get(current);
        return h != null && h.outer() ? h : null;
    }

    static void program(CSharpProgram program) {
        STATE.get().program = program;
    }

    /** The whole program's facts, {@link CSharpProgram#NONE} when the printer was given none. */
    static CSharpProgram program() {
        return STATE.get().program;
    }

    /** The library mappings of the translation: the program's. */
    static CSharpLibraries libraries() {
        return STATE.get().program.libraries();
    }

    static void pushType(TypeInfo typeInfo) {
        STATE.get().base.pushType(typeInfo);
    }

    static void popType() {
        STATE.get().base.popType();
    }

    /** The type whose companion class is being printed ({@link CSharpNames#hasCompanion}); null when none. */
    static TypeInfo companion() {
        return STATE.get().companion;
    }

    static boolean hasCompanion(TypeInfo typeInfo, Predicate<TypeInfo> compute) {
        Map<TypeInfo, Boolean> cache = STATE.get().hasCompanion;
        Boolean known = cache.get(typeInfo);
        if (known == null) {
            known = compute.test(typeInfo);
            cache.put(typeInfo, known);
        }
        return known;
    }

    static void companion(TypeInfo typeInfo) {
        STATE.get().companion = typeInfo;
    }

    static TypeInfo currentType() {
        return STATE.get().base.currentType();
    }

    static void pushMethod(MethodInfo methodInfo) {
        STATE.get().base.pushMethod(methodInfo);
    }

    static void popMethod() {
        STATE.get().base.popMethod();
    }

    static MethodInfo currentMethod() {
        return STATE.get().base.currentMethod();
    }

    /** The type a creation is printed with, its diamond resolved: what its arguments' diamonds are inferred from. */
    static void created(ConstructorCall cc,
                        ParameterizedType type) {
        STATE.get().created.put(cc, type);
    }

    static ParameterizedType created(ConstructorCall cc) {
        return STATE.get().created.get(cc);
    }

    /**
     * Whether C# can name {@code tp} where the printer is: a type parameter of a method being printed (not erased), or
     * of the current type or one it is nested in, or of one of their supertypes, short of a hoisted type and of a
     * companion class's static members.
     */
    static boolean inScope(TypeParameter tp) {
        State state = STATE.get();
        var owner = tp.getOwner();
        if (owner == null) return false;
        if (owner.isRight()) {
            MethodInfo m = owner.getRight();
            return state.base.methods().stream().anyMatch(m::equals) && !m.isConstructor() && !state.program.erasedGeneric(m);
        }
        TypeInfo t = owner.getLeft();
        for (TypeInfo at = currentType(); at != null; at = CSharpNames.hoisted(at) ? null : CSharpNames.enclosing(at)) {
            if (at.equals(state.companion)) return false; // the companion class is not generic
            // a supertype's, as an inherited field's type has it: the subclass passes its own, of the same name, on
            if (at.equals(t) || CSharpNames.inherits(at, t)) return true;
        }
        return false;
    }

    /**
     * The type parameters of the enclosing methods that C# prints erased ({@link CSharpProgram#erasedGeneric}), and
     * of a generic constructor: C# has none.
     */
    static List<TypeParameter> erasedTypeParameters() {
        State state = STATE.get();
        List<TypeParameter> erased = List.of();
        for (MethodInfo m : state.base.methods()) {
            if (!m.typeParameters().isEmpty() && (m.isConstructor() || state.program.erasedGeneric(m))) {
                if (erased.isEmpty()) erased = new ArrayList<>();
                erased.addAll(m.typeParameters());
            }
        }
        return erased;
    }

    /** A {@code using} directive the printed code needs: a BCL namespace a mapped type lives in. */
    static void using(String namespace) {
        STATE.get().usings.add(namespace);
    }

    static Set<String> usings() {
        return STATE.get().usings;
    }

    /** The private declarations of the file that code outside their owner reaches: see {@link CSharpAccess}. */
    static void privateReachedFromOutside(Set<Object> reached) {
        STATE.get().privateReachedFromOutside = reached;
    }

    static boolean reachedFromOutside(Object info) {
        return STATE.get().privateReachedFromOutside.contains(info);
    }

    /** A new scope, which declares {@code pending} at its own level (see {@link CSharpLocals}). */
    static void enterScope(Set<String> pending) {
        STATE.get().scopes.push(new Scope(new HashMap<>(), pending, new HashMap<>()));
    }

    static void exitScope() {
        STATE.get().scopes.pop();
    }

    /**
     * Declares a local, a parameter or a pattern variable in the current scope: its C# name, escaped. The Java name,
     * unless C# would see a clash: with a name of an enclosing scope (declared already, or later at that scope's own
     * level), or with a name declared already in this scope. Then {@code name2}, {@code name3}, ….
     */
    static String declare(String javaName) {
        Deque<Scope> scopes = STATE.get().scopes;
        if (scopes.isEmpty()) enterScope(Set.of());
        Scope current = scopes.peek();
        String candidate = javaName;
        for (int i = 2; taken(candidate, current, scopes); i++) candidate = javaName + i;
        current.names.put(javaName, candidate);
        return CSharpNames.name(candidate);
    }

    private static boolean taken(String candidate, Scope current, Deque<Scope> scopes) {
        for (Scope scope : scopes) {
            if (scope.names.containsValue(candidate)) return true;
            if (scope != current && scope.pending.contains(candidate)) return true;
        }
        return false;
    }

    /** The C# name of a use of the local {@code javaName}: what its innermost declaration was called. */
    static String local(String javaName) {
        for (Scope scope : STATE.get().scopes) {
            String name = scope.names.get(javaName);
            if (name != null) return CSharpNames.name(name);
        }
        return CSharpNames.name(javaName);
    }

    /** The nullability of the local {@code javaName} just declared, for its uses. */
    static void localState(String javaName, NullableState state) {
        Deque<Scope> scopes = STATE.get().scopes;
        if (scopes.isEmpty()) enterScope(Set.of());
        scopes.peek().states.put(javaName, state);
    }

    /** The nullability of the innermost declaration of the local {@code javaName}; UNSPECIFIED when not known. */
    static NullableState localState(String javaName) {
        for (Scope scope : STATE.get().scopes) {
            if (scope.names.containsKey(javaName)) {
                return scope.states.getOrDefault(javaName, NullableState.UNSPECIFIED);
            }
        }
        return NullableState.UNSPECIFIED;
    }

    static void pushStatement(Statement statement) {
        STATE.get().base.pushStatement(statement);
    }

    static void popStatement() {
        STATE.get().base.popStatement();
    }

    static void pushCall(Expression call) {
        STATE.get().base.pushCall(call);
    }

    static void popCall() {
        STATE.get().base.popCall();
    }

    /** The innermost call or field access being printed; null when none. */
    static Expression currentCall() {
        return STATE.get().base.currentCall();
    }

    /** The call around the innermost one: {@code share(…)} while printing its argument {@code new Bridge<>(…)}. */
    static Expression enclosingCall() {
        return STATE.get().base.enclosingCall();
    }

    /** A type the printed code names by its simple name: it must be in scope, through a {@code using}. */
    static void referenced(TypeInfo typeInfo) {
        STATE.get().base.referenced(typeInfo);
    }

    static Set<TypeInfo> referenced() {
        return STATE.get().base.referenced();
    }

    /** The shared part of the state: see {@link TranslationState}. */
    static TranslationState<CSharpPrintCode> state() {
        return STATE.get().base;
    }

    /** Records a message about {@code subject}, an element of the type being printed. */
    static void message(CSharpPrintCode code, Element subject, String detail) {
        STATE.get().base.message(code, subject, detail);
    }

    /** The messages recorded since the last reset. */
    static List<PrintMessage<CSharpPrintCode>> messages() {
        return STATE.get().base.messages();
    }

    /** An element in a message: its Java text, on one line, shortened. */
    static String describe(Element e) {
        return TranslationState.describe(e);
    }
}
