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
