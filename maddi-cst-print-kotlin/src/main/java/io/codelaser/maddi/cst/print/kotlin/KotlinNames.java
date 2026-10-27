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
