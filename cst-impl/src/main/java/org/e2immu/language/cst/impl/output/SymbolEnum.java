/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.element.Space;
import org.e2immu.language.cst.api.output.element.Symbol;
import org.e2immu.util.internal.util.StringUtil;

import static org.e2immu.language.cst.impl.output.SpaceEnum.*;

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

    public static Symbol plusPlusPrefix(String s) {
        return new SymbolEnum(s, ONE_IS_NICE_EASY_SPLIT, NONE, null);
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
        return this == COMMA || this == SEMICOLON || this == RIGHT_ANGLE_BRACKET || this == RIGHT_PARENTHESIS;
    }
}
