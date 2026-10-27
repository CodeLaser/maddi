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

package io.codelaser.maddi.cst.impl.output;

import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.element.Space;
import io.codelaser.maddi.cst.api.output.element.Symbol;
import io.codelaser.maddi.util.StringUtil;

import static io.codelaser.maddi.cst.impl.output.SpaceEnum.*;

public record SymbolEnum(String symbol, Space left, Space right, String constant) implements Symbol {


    public SymbolEnum {
        assert symbol != null;
        assert left != null;
        assert right != null;
    }

    public static final Symbol INSTANCE_OF = new SymbolEnum("instanceof", ONE, ONE, "INSTANCE_OF");
    public static final Symbol UNARY_BOOLEAN_NOT = new SymbolEnum("!", RELAXED_NO_SPACE_SPLIT_ALLOWED, NONE, "UNARY_BOOLEAN_NOT");
    public static final Symbol UNARY_MINUS = new SymbolEnum("-", RELAXED_NO_SPACE_SPLIT_ALLOWED, NONE, "UNARY_MINUS");
    public static final Symbol AT = new SymbolEnum("@", ONE_IS_NICE_EASY_SPLIT, NONE, "AT");

    public static final Symbol PIPE = binaryOperator("|");

    public static final Symbol COMMA = new SymbolEnum(",", NONE, ONE_IS_NICE_EASY_SPLIT, "COMMA");
    public static final Symbol SEMICOLON = new SymbolEnum(";", NONE, ONE_IS_NICE_EASY_SPLIT, "SEMICOLON");

    // a ? b : c;
    public static final Symbol QUESTION_MARK = binaryOperator("?");
    public static final Symbol COLON = binaryOperator(":");
    public static final Symbol COLON_LABEL = new SymbolEnum(":", NONE, ONE_IS_NICE_EASY_SPLIT, "COLON_LABEL");
    public static final Symbol DOUBLE_COLON = new SymbolEnum("::", NONE, NONE, "DOUBLE_COLON");

    public static final Symbol DOT = new SymbolEnum(".", NO_SPACE_SPLIT_ALLOWED, NONE, "DOT");

    public static final Symbol LEFT_PARENTHESIS = new SymbolEnum("(", NONE, NO_SPACE_SPLIT_ALLOWED, "LEFT_PARENTHESIS");
    public static final Symbol RIGHT_PARENTHESIS = new SymbolEnum(")", NONE, RELAXED_NO_SPACE_SPLIT_ALLOWED, "RIGHT_PARENTHESIS");
    public static final Symbol OPEN_CLOSE_PARENTHESIS = new SymbolEnum("()", NONE, RELAXED_NO_SPACE_SPLIT_ALLOWED, "OPEN_CLOSE_PARENTHESIS");

    /**
     * The {@code (} of {@code if (}, {@code for (}, {@code while (}, {@code catch (}, {@code switch (},
     * {@code synchronized (} — every mainstream Java style (and checkstyle's {@code WhitespaceAround} on the
     * keyword tokens) writes a space between the keyword and the parenthesis, where a METHOD CALL writes
     * none. The shared {@link #LEFT_PARENTHESIS} cannot distinguish the two, which is how {@code if(}
     * shipped for years; the Kotlin printers already insert the space explicitly.
     * {@code ONE_IS_NICE_NEVER_SPLIT}: nice mode writes the space, COMPACT mode stays {@code if(} —
     * compact means smallest — and there is never a line break between a keyword and its parenthesis.
     */
    public static final Symbol LEFT_PARENTHESIS_AFTER_KEYWORD =
            new SymbolEnum("(", ONE_IS_NICE_NEVER_SPLIT, NO_SPACE_SPLIT_ALLOWED, "LEFT_PARENTHESIS_AFTER_KEYWORD");

    /**
     * The {@code )} closing a CAST: {@code (Type) x}, with the space checkstyle's {@code WhitespaceAfter
     * TYPECAST} demands and {@link #RIGHT_PARENTHESIS} (an expression's closing parenthesis, no space)
     * cannot express. Nice mode writes the space; compact mode stays {@code (Type)x}.
     */
    public static final Symbol RIGHT_PARENTHESIS_AFTER_CAST =
            new SymbolEnum(")", NONE, ONE_IS_NICE_EASY_SPLIT, "RIGHT_PARENTHESIS_AFTER_CAST");

    public static final Symbol LEFT_BRACE = new SymbolEnum("{", ONE_IS_NICE_EASY_SPLIT, ONE_IS_NICE_SPLIT_BEGIN_END, "LEFT_BRACE");
    public static final Symbol RIGHT_BRACE = new SymbolEnum("}", ONE_IS_NICE_SPLIT_BEGIN_END, ONE_IS_NICE_EASY_SPLIT, "RIGHT_BRACE");

    public static final Symbol LEFT_BRACKET = new SymbolEnum("[", NONE, NO_SPACE_SPLIT_ALLOWED, "LEFT_BRACKET");
    public static final Symbol RIGHT_BRACKET = new SymbolEnum("]", NONE, RELAXED_NO_SPACE_SPLIT_ALLOWED, "RIGHT_BRACKET");
    public static final Symbol OPEN_CLOSE_BRACKETS = new SymbolEnum("[]", NONE, RELAXED_NO_SPACE_SPLIT_ALLOWED, "OPEN_CLOSE_BRACKETS");

    public static final Symbol LEFT_ANGLE_BRACKET = new SymbolEnum("<", NONE, NONE, "LEFT_ANGLE_BRACKET");
    public static final Symbol RIGHT_ANGLE_BRACKET = new SymbolEnum(">", NONE, ONE_IS_NICE_EASY_SPLIT, "RIGHT_ANGLE_BRACKET");
    public static final Symbol AND_TYPES = binaryOperator("&");

    public static final Symbol LOGICAL_AND = binaryOperator("&&");
    public static final Symbol LOGICAL_OR = binaryOperator("||");
    public static final Symbol LAMBDA = binaryOperator("->");
    public static final Symbol EQUALS = binaryOperator("=");
    public static final Symbol NOT_EQUALS = binaryOperator("!=");

    public static final SymbolEnum LEFT_BLOCK_COMMENT = new SymbolEnum("/*", ONE_IS_NICE_EASY_SPLIT, NONE, "LEFT_BLOCK_COMMENT");
    public static final SymbolEnum RIGHT_BLOCK_COMMENT = new SymbolEnum("*/", NONE, ONE_IS_NICE_EASY_SPLIT, "RIGHT_BLOCK_COMMENT");

    public static final Symbol LEFT_BACKTICK = new SymbolEnum("`", ONE_IS_NICE_EASY_L, NONE, "LEFT_BACKTICK");
    public static final Symbol RIGHT_BACKTICK = new SymbolEnum("`", NONE, ONE_IS_NICE_EASY_R, "RIGHT_BACKTICK");

    public static final SymbolEnum SINGLE_LINE_COMMENT = new SymbolEnum("//", NONE, NONE, "SINGLE_LINE_COMMENT");

    public static final SymbolEnum DIAMOND = new SymbolEnum("<>", NONE, NONE, "DIAMOND");
    public static final Symbol UNDERSCORE = new SymbolEnum("_", ONE, ONE_IS_NICE_EASY_R, "UNDERSCORE");

    // #49: no space of its own on the left, as UNARY_MINUS and UNARY_BOOLEAN_NOT: a nice space there won over the
    // '(' in front of it, printing `if ( !x)`. The formatter keeps `a - -b` and `a + ++b` apart (ElementPrinter).
    public static Symbol plusPlusPrefix(String s) {
        return new SymbolEnum(s, RELAXED_NO_SPACE_SPLIT_ALLOWED, NONE, null);
    }

    public static Symbol plusPlusSuffix(String s) {
        return new SymbolEnum(s, NONE, ONE_IS_NICE_EASY_SPLIT, null);
    }

    public static Symbol assignment(String s) {
        return new SymbolEnum(s, ONE_IS_NICE_EASY_L, ONE_IS_NICE_EASY_R, null);
    }

    public static Symbol binaryOperator(String s) {
        return new SymbolEnum(s, ONE_IS_NICE_EASY_L, ONE_IS_NICE_EASY_R, null);
    }

    @Override
    public String minimal() {
        return left.minimal() + symbol + right.minimal();
    }

    @Override
    public int length(FormattingOptions options) {
        return left.length(options) + symbol.length() + right().length(options);
    }

    @Override
    public String write(FormattingOptions options) {
        return left.write(options) + symbol + right.write(options);
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(SymbolEnum" + (constant != null ? "." + constant : ".binaryOperator(" + StringUtil.quote(symbol) + ")") + ")";
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public boolean isLeftBlockComment() {
        return this == LEFT_BLOCK_COMMENT;
    }

    @Override
    public boolean isRightBlockComment() {
        return this == RIGHT_BLOCK_COMMENT;
    }

    @Override
    public boolean isSingleLineComment() {
        return this == SINGLE_LINE_COMMENT;
    }

    @Override
    public boolean isAt() {
        return this == AT;
    }

    @Override
    public boolean strongNoSpace() {
        return this == COMMA || this == SEMICOLON || this == RIGHT_ANGLE_BRACKET || this == RIGHT_PARENTHESIS
               || this == RIGHT_PARENTHESIS_AFTER_CAST; // #49: `(List<T>) o`, not `(List<T> ) o`
    }
}
