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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identifiers. A Java name that is a Kotlin <em>hard</em> keyword must be written between backticks: fernflower names a
 * variable {@code fun} and a parameter {@code in}, and the first of those made kotlinc 2.4 crash rather than report
 * an error. Soft and modifier keywords ({@code data}, {@code open}, {@code value}, …) are legal identifiers.
 */
public final class KotlinNames {
    private static final Set<String> HARD_KEYWORDS = Set.of("as", "break", "class", "continue", "do", "else",
            "false", "for", "fun", "if", "in", "interface", "is", "null", "object", "package", "return", "super",
            "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while");

    private KotlinNames() {
    }

    public static String name(String identifier) {
        return HARD_KEYWORDS.contains(identifier) ? "`" + identifier + "`" : identifier;
    }

    /** A dotted name (package, import), segment by segment. */
    public static String dotted(String dottedName) {
        return Arrays.stream(dottedName.split("\\.")).map(KotlinNames::name).collect(Collectors.joining("."));
    }
}
