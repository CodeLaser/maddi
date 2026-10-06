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
    }

    /** A method body starts: the pattern variables of the enclosing method are not in scope. Restore with the result. */
    static Map<String, Supplier<OutputBuilder>> enterMethod() {
        Map<String, Supplier<OutputBuilder>> saved = new HashMap<>(PATTERNS.get());
        PATTERNS.get().clear();
        return saved;
    }

    static void exitMethod(Map<String, Supplier<OutputBuilder>> saved) {
        PATTERNS.get().clear();
        PATTERNS.get().putAll(saved);
    }

    /** A label no enclosing construct uses, for a {@code run label@{ }} the translation introduces. */
    static String freshLabel(String base) {
        return base + (++COUNTER.get()[0]);
    }
}
