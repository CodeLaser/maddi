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

package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;
import kotlin.text.MatchResult;

import java.util.List;

/**
 * The annotated API for {@code kotlin.text}. The type that matters here is {@link kotlin.text.Regex}, the exact
 * analogue of {@code java.util.regex.Pattern} in {@code jdk/JavaUtilRegex}: it wraps a compiled {@code Pattern}
 * and every method on it is read-only. A {@code private val} of type {@code Regex} is a common shape in Kotlin —
 * a file-level compiled pattern — and without this contract every type holding one is capped at
 * {@code @FinalFields}.
 * <p>
 * ⛔ NAME THE PART CLASS, NOT THE FACADE — see {@link KotlinCollections} for why. {@code kotlin.text.StringsKt}
 * is a multifile facade; {@code replace}/{@code matches} are declared in {@code StringsKt__StringsKt} and
 * {@code toRegex} in {@code StringsKt__RegexExtensionsKt}.
 */
public class KotlinText {
    public static final String PACKAGE_NAME = "kotlin.text";

    /*
    public actual class Regex actual constructor(private val nativePattern: Pattern) : Serializable

    A compiled regular expression is a final, thread-safe immutable value and all of its methods are read-only --
    the same contract JavaUtilRegex gives java.util.regex.Pattern, which is what this wraps. It is a CONTAINER
    rather than plainly immutable for the same reason Pattern is: nothing it is handed is modified either.
    */
    @ImmutableContainer
    class Regex$ {
        String getPattern() {
            return null;
        }

        boolean matches(@NotModified CharSequence input) {
            return false;
        }

        boolean containsMatchIn(@NotModified CharSequence input) {
            return false;
        }

        MatchResult matchEntire(@NotModified CharSequence input) {
            return null;
        }

        String replace(@NotModified CharSequence input, String replacement) {
            return null;
        }

        String replaceFirst(@NotModified CharSequence input, String replacement) {
            return null;
        }

        List<String> split(@NotModified CharSequence input, int limit) {
            return null;
        }

        //override from java.lang.Object
        public String toString() {
            return null;
        }
    }
}
