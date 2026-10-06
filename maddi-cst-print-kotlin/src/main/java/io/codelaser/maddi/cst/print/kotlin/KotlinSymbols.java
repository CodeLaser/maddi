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

import io.codelaser.maddi.cst.api.output.element.Symbol;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

/**
 * Operators that break the line AFTER themselves. Kotlin ends a statement at a newline in front of {@code +},
 * {@code or}, {@code ==} or {@code =}: {@code a\n + b} is the statement {@code a} followed by {@code +b}. Java's
 * operator symbols let the formatter break before them, and so does the {@code )} of a left operand; these withdraw
 * that split point. ({@code .}, {@code ?:}, {@code &&} and {@code ||} may start a line in Kotlin, and are left alone.)
 */
final class KotlinSymbols {

    private KotlinSymbols() {
    }

    /** {@code +}, {@code ==}, {@code ===}, and the infix functions {@code and}, {@code or}, {@code shl}, … */
    static Symbol binary(String operator) {
        return new SymbolEnum(operator, SpaceEnum.ONE_NO_SPLIT_BEFORE, SpaceEnum.ONE_REQUIRED_EASY_SPLIT, null);
    }

    /** {@code =}, {@code +=}, … */
    static Symbol assignment(String operator) {
        return binary(operator);
    }
}
