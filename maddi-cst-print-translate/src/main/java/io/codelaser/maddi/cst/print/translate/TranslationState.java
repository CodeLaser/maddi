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
package io.codelaser.maddi.cst.print.translate;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a Java-to-X printer is, which the CST does not say: the printers are static and recursive, and created
 * through the cst-api factories, whose signatures carry no context. So each language keeps one of these per thread,
 * pushed and popped around the constructs that change the answer: the types, methods, statements and calls being
 * printed, innermost first; the types the text names by their simple name; and the {@link PrintMessage}s about the
 * file. What only one language needs (Kotlin's loop frames and pattern variables, C#'s scopes and usings) stays in
 * its own context.
 *
 * @param <C> the target language's message codes
 */
public final class TranslationState<C extends PrintMessage.Code> {

    private final Deque<TypeInfo> types = new ArrayDeque<>();
    private final Deque<MethodInfo> methods = new ArrayDeque<>();
    private final Deque<Statement> statements = new ArrayDeque<>();
    private final Deque<Expression> calls = new ArrayDeque<>();
    private Set<TypeInfo> referenced = new LinkedHashSet<>();
    // a set: the printer may print an element twice
    private final Set<PrintMessage<C>> messages = new LinkedHashSet<>();

    public void pushType(TypeInfo typeInfo) {
        types.push(typeInfo);
    }

    public void popType() {
        types.pop();
    }

    /** The innermost type being printed; null outside a type. */
    public TypeInfo currentType() {
        return types.peek();
    }

    /** The type, or one around it, is being printed: its static members can be written unqualified. */
    public boolean typeInScope(TypeInfo typeInfo) {
        return types.contains(typeInfo);
    }

    /** A type parameter of one of the types being printed. */
    public boolean typeParameterInScope(TypeParameter typeParameter) {
        return types.stream().anyMatch(t -> t.typeParameters().contains(typeParameter));
    }

    public void pushMethod(MethodInfo methodInfo) {
        methods.push(methodInfo);
    }

    public void popMethod() {
        methods.pop();
    }

    /** The innermost method whose body is being printed; null outside a body. */
    public MethodInfo currentMethod() {
        return methods.peek();
    }

    public void pushStatement(Statement statement) {
        statements.push(statement);
    }

    public void popStatement() {
        statements.pop();
    }

    /** The innermost statement being printed: where a use-site fact is asked for. Null outside a body. */
    public Statement currentStatement() {
        return statements.peek();
    }

    public void pushCall(Expression call) {
        calls.push(call);
    }

    public void popCall() {
        calls.pop();
    }

    /** The innermost call (or field access) being printed: where a per-expression fact is asked for. */
    public Expression currentCall() {
        return calls.peek();
    }

    /** The call around the innermost one: {@code Collections.sort(list, …)} while printing its comparingInt(…). */
    public Expression enclosingCall() {
        Iterator<Expression> it = calls.iterator();
        if (!it.hasNext()) return null;
        it.next();
        return it.hasNext() ? it.next() : null;
    }

    /** A type the printed text names by its simple name: the file must import it, or have it in scope. */
    public void referenced(TypeInfo typeInfo) {
        referenced.add(typeInfo);
    }

    public Set<TypeInfo> referenced() {
        return referenced;
    }

    /** The types referenced since the last call, which starts a new record. */
    public Set<TypeInfo> takeReferenced() {
        Set<TypeInfo> taken = referenced;
        referenced = new LinkedHashSet<>();
        return taken;
    }

    /** Records a message about {@code subject}, an element of the type being printed; without one, the type. */
    public void message(C code, Element subject, String detail) {
        TypeInfo type = currentType();
        Element located = subject != null ? subject : type;
        Source source = located == null ? null : located.source();
        boolean known = source != null && !source.isNoSource();
        messages.add(new PrintMessage<>(code, type == null ? "?" : type.fullyQualifiedName(),
                known ? source.beginLine() : -1, known ? source.beginPos() : -1, detail));
    }

    /** The messages recorded so far. */
    public List<PrintMessage<C>> messages() {
        return List.copyOf(messages);
    }

    /** The messages recorded since the last call, which starts a new record. */
    public List<PrintMessage<C>> takeMessages() {
        List<PrintMessage<C>> taken = List.copyOf(messages);
        messages.clear();
        return taken;
    }

    /** An element in a message: a member or type by its name, anything else by its Java text, on one line, shortened. */
    public static String describe(Element e) {
        if (e == null) return "";
        if (e instanceof MethodInfo m) return m.fullyQualifiedName();
        if (e instanceof FieldInfo f) return f.owner().simpleName() + "." + f.name();
        if (e instanceof TypeInfo t) return t.fullyQualifiedName();
        String text;
        try {
            text = e.toString().replaceAll("\\s+", " ").trim();
        } catch (RuntimeException re) {
            return e.getClass().getSimpleName();
        }
        return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }

    /**
     * A use-site fact: {@code variable} is known non-null where the current statement starts, or at the current call,
     * so that the target's flow analysis (Kotlin's smart cast, C#'s nullable analysis) knows it too and the use needs
     * no assertion.
     */
    public boolean knownNonNull(NullabilityFacts facts, Variable variable) {
        Statement statement = currentStatement();
        if (statement != null && facts.nonNullAt(statement, variable)) return true;
        Expression call = currentCall();
        return call != null && !dereferencedInArguments(call, variable) && facts.nonNullAt(call, variable);
    }

    /**
     * The fact at a call holds after its receiver and arguments are evaluated, left to right: a dereference of the
     * variable in an argument would make the fact claim what the receiver or an earlier argument does not have yet
     * ({@code stack.add(g.first)}: at the call g is non-null, at g it is not).
     */
    private static boolean dereferencedInArguments(Expression call, Variable variable) {
        List<Expression> arguments = switch (call) {
            case MethodCall mc -> mc.parameterExpressions();
            case ConstructorCall cc -> cc.parameterExpressions();
            default -> List.of();
        };
        boolean[] found = {false};
        for (Expression argument : arguments) {
            argument.visit((Element e) -> {
                Expression scope = switch (e) {
                    case MethodCall mc -> mc.object();
                    case VariableExpression ve when ve.variable() instanceof FieldReference fr -> fr.scope();
                    case VariableExpression ve when ve.variable() instanceof DependentVariable dv -> dv.arrayExpression();
                    default -> null;
                };
                if (scope != null && CstQueries.unwrap(scope) instanceof VariableExpression sv
                    && variable.equals(sv.variable())) {
                    found[0] = true;
                }
                return !found[0];
            });
        }
        return found[0];
    }
}
