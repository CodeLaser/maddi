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

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.output.element.Symbol;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Renders a {@link CSharpBcl} template. The pieces are printed when the template names them, and only then: printing
 * declares pattern variables, so an argument a template drops must not be printed.
 *
 * @param receiver   {@code $0}, as an atom
 * @param arguments  {@code $1}, {@code $2}, …
 * @param operands   {@code @1}, {@code @2}, …: the arguments as operands
 * @param call       the call, for {@code $1.2} (an argument of an argument); null for a method reference
 * @param returnType {@code {R}}, {@code {R0}}, … and {@code {R0.1}}, a type argument of a type argument
 * @param receiverType {@code {T0}}, …: the type arguments of the receiver's type
 */
record CSharpTemplate(Supplier<OutputBuilder> receiver, List<Supplier<OutputBuilder>> arguments,
                      List<Supplier<OutputBuilder>> operands, MethodCall call, ParameterizedType returnType,
                      ParameterizedType receiverType, Qualification q) {

    OutputBuilder render(String template) {
        OutputBuilder b = new OutputBuilderImpl();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if ((c == '$' || c == '@') && i + 1 < template.length()) {
                char d = template.charAt(i + 1);
                if (c == '$' && (d == '*' || d == '+')) {
                    flush(b, literal);
                    b.add(join(d == '*' ? 0 : 1));
                    i += 2;
                    continue;
                }
                if (Character.isDigit(d)) {
                    int j = i + 1;
                    while (j < template.length() && Character.isDigit(template.charAt(j))) j++;
                    int index = Integer.parseInt(template.substring(i + 1, j));
                    flush(b, literal);
                    if (c == '$' && j + 1 < template.length() && template.charAt(j) == '.'
                        && Character.isDigit(template.charAt(j + 1))) {
                        int k = j + 1;
                        while (k < template.length() && Character.isDigit(template.charAt(k))) k++;
                        b.add(inner(index, Integer.parseInt(template.substring(j + 1, k))));
                        i = k;
                        continue;
                    }
                    b.add(index == 0 ? receiver.get() : (c == '@' ? operands : arguments).get(index - 1).get());
                    i = j;
                    continue;
                }
            }
            if (c == '{' && i + 2 < template.length() && (template.charAt(i + 1) == 'R' || template.charAt(i + 1) == 'T')) {
                int close = template.indexOf('}', i);
                String token = template.substring(i + 1, close);
                if (token.matches("[RT](\\d+(\\.\\d+)*)?")) {
                    literal.append(type(token));
                    i = close + 1;
                    continue;
                }
            }
            literal.append(c);
            i++;
        }
        flush(b, literal);
        return b;
    }

    /** All arguments, from {@code from} on, separated by commas. */
    private OutputBuilder join(int from) {
        OutputBuilder b = new OutputBuilderImpl();
        for (int i = from; i < arguments.size(); i++) {
            if (i > from) b.add(SymbolEnum.COMMA);
            b.add(arguments.get(i).get());
        }
        return b;
    }

    /** {@code $1.2}: argument 2 of argument 1, a call. */
    private OutputBuilder inner(int argument, int index) {
        Expression arg = CSharpExpressionPrinter.unwrap(call.parameterExpressions().get(argument - 1));
        MethodCall inner = (MethodCall) arg;
        return CSharpExpressionPrinter.print(inner.parameterExpressions().get(index - 1), q);
    }

    private String type(String token) {
        ParameterizedType t = token.charAt(0) == 'R' ? returnType : receiverType;
        if (t == null) return "object";
        if (token.length() == 1) return CSharpTypeName.of(t, q);
        // {R0.1}: the second type argument of the first type argument
        for (String index : token.substring(1).split("\\.")) {
            int i = Integer.parseInt(index);
            if (i >= t.parameters().size()) return "object";
            t = t.parameters().get(i);
        }
        return CSharpTypeName.argument(t, q);
    }

    static final Symbol NULL_CONDITIONAL = new SymbolEnum("?.", SpaceEnum.NONE, SpaceEnum.NONE, null);
    private static final Symbol RANGE = new SymbolEnum("..", SpaceEnum.NONE, SpaceEnum.NONE, null);
    private static final Set<String> OPERATORS = Set.of("=>", "==", "!=", "??", "+", "-", "*", "/", "<", ">", "<=",
            ">=", "||", "&&", "=", "or", "is");

    /**
     * The template's literal text, as the formatter's own elements: symbols for punctuation and operators, which carry
     * their spacing, words as text. Two text elements in a row get a space between them from the formatter; a
     * symbol decides for itself.
     */
    private static void flush(OutputBuilder b, StringBuilder literal) {
        String text = literal.toString();
        literal.setLength(0);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ' ') {
                i++;
                continue;
            }
            if (c == '"') {
                int end = i + 1;
                while (end < text.length() && text.charAt(end) != '"') end += text.charAt(end) == '\\' ? 2 : 1;
                b.add(new TextImpl(text.substring(i, end + 1)));
                i = end + 1;
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '_' || c == '^') {
                int end = i;
                while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_'
                                               || text.charAt(end) == '^')) {
                    end++;
                }
                if (end < text.length() && text.charAt(end) == '<' && Character.isLetter(c)) {
                    // a generic type: List<int>, up to the matching >
                    int depth = 0;
                    do {
                        if (text.charAt(end) == '<') depth++;
                        else if (text.charAt(end) == '>') depth--;
                        end++;
                    } while (end < text.length() && depth > 0);
                }
                String word = text.substring(i, end);
                b.add(OPERATORS.contains(word) ? SymbolEnum.binaryOperator(word) : new TextImpl(word));
                i = end;
                continue;
            }
            String two = i + 1 < text.length() ? text.substring(i, i + 2) : "";
            if ("?.".equals(two)) {
                b.add(NULL_CONDITIONAL);
                i += 2;
            } else if ("..".equals(two)) {
                b.add(RANGE);
                i += 2;
            } else if (OPERATORS.contains(two)) {
                b.add(SymbolEnum.binaryOperator(two));
                i += 2;
            } else if (c == '.') {
                b.add(SymbolEnum.DOT);
                i++;
            } else if (c == '(') {
                b.add(SymbolEnum.LEFT_PARENTHESIS);
                i++;
            } else if (c == ')') {
                // (int) x: a cast's parenthesis keeps its space
                boolean cast = i + 1 < text.length() && text.charAt(i + 1) == ' ';
                b.add(cast ? SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST : SymbolEnum.RIGHT_PARENTHESIS);
                i++;
            } else if (c == '[') {
                b.add(SymbolEnum.LEFT_BRACKET);
                i++;
            } else if (c == ']') {
                b.add(SymbolEnum.RIGHT_BRACKET);
                i++;
            } else if (c == ',') {
                b.add(SymbolEnum.COMMA);
                i++;
            } else if (c == '!') {
                b.add(SymbolEnum.UNARY_BOOLEAN_NOT);
                i++;
            } else if (OPERATORS.contains(String.valueOf(c))) {
                b.add(SymbolEnum.binaryOperator(String.valueOf(c)));
                i++;
            } else {
                b.add(new TextImpl(String.valueOf(c))); // { } ;
                i++;
            }
        }
    }

    /** A template whose result is not an atom: an operator outside brackets, a cast, a prefix. */
    static boolean atomic(String template) {
        if (template.startsWith("!") || template.startsWith("(") && !template.startsWith("($")) return false;
        int depth = 0;
        for (char c : template.toCharArray()) {
            if (c == '(' || c == '[' || c == '{' || c == '<') depth++;
            else if (c == ')' || c == ']' || c == '}' || c == '>') depth--;
            else if (c == ' ' && depth == 0) return false;
        }
        return true;
    }
}
