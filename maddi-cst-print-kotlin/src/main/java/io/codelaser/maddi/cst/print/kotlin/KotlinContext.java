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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What a statement needs to know about where it is printed, which the CST does not say: the printers are static
 * and recursive, so this is a per-thread stack, pushed and popped around the constructs that change the answer.
 * <ul>
 *   <li><b>break targets.</b> An unlabelled Java {@code break} leaves the innermost loop OR switch. A Kotlin
 *   {@code break} inside a {@code when} leaves the enclosing LOOP, so a Java switch-exit must not print as
 *   {@code break}: the final one of a case is dropped, any other becomes {@code return@label} out of a
 *   {@code run label@{ }} around the {@code when}.</li>
 *   <li><b>lambdas.</b> A Kotlin {@code return} inside a lambda returns from the enclosing FUNCTION, so a Java
 *   {@code return} inside a lambda body prints as {@code return@lambda}.</li>
 *   <li><b>the types being printed</b>, innermost first. Their companion objects are in scope, so a static member
 *   of one of them is written unqualified; any other static member, an inherited one included, gets its owner.</li>
 *   <li><b>pattern variables.</b> Kotlin has no {@code x instanceof T t}; {@code t} prints as what the
 *   {@code instanceof} tested, smart-cast or cast.</li>
 * </ul>
 */
final class KotlinContext {

    /** A loop, a switch, or a lambda body, innermost first. */
    static final class Frame {
        private final Kind kind;
        private final Statement statement;
        private final String label;
        private boolean leftEarly;

        Frame(Kind kind, Statement statement, String label) {
            this.kind = kind;
            this.statement = statement;
            this.label = label;
        }

        Kind kind() {
            return kind;
        }

        Statement statement() {
            return statement;
        }

        String label() {
            return label;
        }

        /** A jump out of this switch or labelled block other than a case's final break: it needs its run label@{ }. */
        void markLeftEarly() {
            leftEarly = true;
        }

        boolean leftEarly() {
            return leftEarly;
        }
    }

    enum Kind {LOOP, SWITCH, LAMBDA}

    static final String LAMBDA_LABEL = "lambda";

    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<int[]> COUNTER = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<Deque<TypeInfo>> TYPES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<KotlinPrintOptions> OPTIONS = ThreadLocal.withInitial(() -> KotlinPrintOptions.DEFAULT);
    private static final ThreadLocal<Deque<io.codelaser.maddi.cst.api.info.MethodInfo>> METHODS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Map<String, io.codelaser.maddi.cst.api.type.ParameterizedType>> LOCAL_TYPES =
            ThreadLocal.withInitial(HashMap::new);
    // the body a local variable is declared in: the method's, or a lambda's
    private static final ThreadLocal<Deque<java.util.Optional<io.codelaser.maddi.cst.api.element.Element>>> SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<java.util.Set<String>> SHADOWING = ThreadLocal.withInitial(java.util.Set::of);
    // by name: a local variable's equality is its name, and so is its scope as far as Kotlin is concerned
    private static final ThreadLocal<Map<String, Supplier<OutputBuilder>>> PATTERNS =
            ThreadLocal.withInitial(HashMap::new);

    private KotlinContext() {
    }

    static void push(Frame frame) {
        FRAMES.get().push(frame);
    }

    static void pop() {
        FRAMES.get().pop();
    }

    /** The innermost loop or switch, which an unlabelled {@code break} leaves; null outside both. */
    static Frame breakTarget() {
        for (Frame f : FRAMES.get()) {
            return f.kind == Kind.LAMBDA ? null : f;
        }
        return null;
    }

    /** The enclosing loop, switch or block with this label, not looking past a lambda; null when there is none. */
    static Frame frameLabelled(String label) {
        for (Frame f : FRAMES.get()) {
            if (f.kind == Kind.LAMBDA) return null;
            if (label.equals(f.label)) return f;
        }
        return null;
    }

    /** True inside a lambda body, before any enclosing loop/switch of the lambda's own body ends that question. */
    static boolean inLambda() {
        for (Frame f : FRAMES.get()) {
            if (f.kind == Kind.LAMBDA) return true;
        }
        return false;
    }

    /** Numbers the labels from 1 again: once per file, so that printing the same file twice gives the same text. */
    static KotlinPrintOptions options() {
        return OPTIONS.get();
    }

    /** Set for a file; returns the previous options, to restore. */
    static KotlinPrintOptions options(KotlinPrintOptions options) {
        KotlinPrintOptions previous = OPTIONS.get();
        OPTIONS.set(options);
        return previous;
    }

    private static final ThreadLocal<Deque<Statement>> STATEMENTS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<io.codelaser.maddi.cst.api.expression.Expression>> CALLS =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** The innermost method or constructor call being printed: where a per-expression fact is asked for. */
    static io.codelaser.maddi.cst.api.expression.Expression currentCall() {
        return CALLS.get().peek();
    }

    static void pushCall(io.codelaser.maddi.cst.api.expression.Expression call) {
        CALLS.get().push(call);
    }

    static void popCall() {
        CALLS.get().pop();
    }

    /** The innermost statement being printed: where a use-site fact is asked for. */
    static Statement currentStatement() {
        return STATEMENTS.get().peek();
    }

    static void pushStatement(Statement statement) {
        STATEMENTS.get().push(statement);
    }

    static void popStatement() {
        STATEMENTS.get().pop();
    }

    static io.codelaser.maddi.cst.api.info.MethodInfo currentMethod() {
        return METHODS.get().peek();
    }

    /** The type a local variable was printed with, by name (the innermost declaration of that name wins). */
    static void localType(String name, io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        LOCAL_TYPES.get().put(name, type);
    }

    static io.codelaser.maddi.cst.api.type.ParameterizedType localType(String name) {
        return LOCAL_TYPES.get().get(name);
    }

    static void resetLabels() {
        COUNTER.get()[0] = 0;
        PATTERNS.get().clear();
    }

    /**
     * The primary constructor's parameter names, while printing property initializers and init blocks: there a field
     * of the same name must be written {@code this.x}, or it reads the parameter.
     */
    static void shadowingParameters(java.util.Set<String> names) {
        SHADOWING.set(names);
    }

    static java.util.Set<String> shadowingParameters() {
        return SHADOWING.get();
    }

    static boolean shadowedByParameter(String name) {
        return SHADOWING.get().contains(name);
    }

    static void pushType(TypeInfo typeInfo) {
        TYPES.get().push(typeInfo);
    }

    static void popType() {
        TYPES.get().pop();
    }

    /** The innermost type being printed comes from Java: its {@code java.util.List} is a {@code MutableList}. */
    static boolean translatingJava() {
        TypeInfo top = TYPES.get().peek();
        return top != null && !KotlinTypePrinter.fromKotlinSource(top);
    }

    /** A static member of this type can be written unqualified: the type, or one around it, is being printed. */
    static boolean typeInScope(TypeInfo typeInfo) {
        return TYPES.get().contains(typeInfo);
    }

    /** A type parameter of one of the types being printed. */
    static boolean typeParameterInScope(io.codelaser.maddi.cst.api.info.TypeParameter typeParameter) {
        return TYPES.get().stream().anyMatch(t -> t.typeParameters().contains(typeParameter));
    }

    /** {@code x instanceof T t}: from here on, {@code t} prints as {@code replacement}. */
    static void patternVariable(Variable variable, Supplier<OutputBuilder> replacement) {
        PATTERNS.get().put(variable.simpleName(), replacement);
    }

    static Supplier<OutputBuilder> patternVariable(Variable variable) {
        return PATTERNS.get().get(variable.simpleName());
    }

    /** A local variable, parameter or catch variable of this name is declared: it is not the pattern's any more. */
    static void declared(String name) {
        PATTERNS.get().remove(name);
        LOCAL_TYPES.get().remove(name);
    }

    /** A method body starts: the pattern variables of the enclosing method are not in scope. Restore with the result. */
    record MethodScope(Map<String, Supplier<OutputBuilder>> patterns,
                       Map<String, io.codelaser.maddi.cst.api.type.ParameterizedType> localTypes) {
    }

    static MethodScope enterMethod(io.codelaser.maddi.cst.api.info.MethodInfo methodInfo) {
        MethodScope saved = new MethodScope(new HashMap<>(PATTERNS.get()), new HashMap<>(LOCAL_TYPES.get()));
        PATTERNS.get().clear();
        LOCAL_TYPES.get().clear();
        METHODS.get().push(methodInfo);
        pushScope(methodInfo.methodBody());
        return saved;
    }

    static void exitMethod(MethodScope saved) {
        METHODS.get().pop();
        popScope();
        PATTERNS.get().clear();
        PATTERNS.get().putAll(saved.patterns());
        LOCAL_TYPES.get().clear();
        LOCAL_TYPES.get().putAll(saved.localTypes());
    }

    /** A method or lambda body starts: the scope of the local variables declared in it. */
    static void pushScope(io.codelaser.maddi.cst.api.element.Element body) {
        SCOPES.get().push(java.util.Optional.ofNullable(body));
    }

    static void popScope() {
        SCOPES.get().pop();
    }

    /** A local variable of the current body is assigned after its declaration; true when there is no body to scan. */
    static boolean reassigned(Variable local) {
        var scope = SCOPES.get().peek();
        return scope == null || scope.isEmpty() || KotlinAssignments.assignedIn(scope.get(), local);
    }

    /** A label no enclosing construct uses, for a {@code run label@{ }} the translation introduces. */
    static String freshLabel(String base) {
        return base + (++COUNTER.get()[0]);
    }
}
