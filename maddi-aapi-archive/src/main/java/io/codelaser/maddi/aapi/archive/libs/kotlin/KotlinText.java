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
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import kotlin.sequences.Sequence;
import kotlin.ranges.IntRange;
import kotlin.text.MatchGroup;
import kotlin.text.MatchGroupCollection;
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

    /*
    public fun CharSequence.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String>

    Only the ARRAY parameters need a contract. A String or CharSequence argument is already unmodified by default
    (CharSequence is @Immutable(hc=true) in jdk/JavaLang), which is why most StringsKt calls the library-call census
    lists as uncontracted are harmless. A vararg is an array, and an array argument the defaults call MODIFIED.
    */
    class StringsKt__StringsKt$ {
        @Independent(hc = true)
        @NotNull
        static List<String> split(@NotModified CharSequence receiver, @NotModified String[] delimiters,
                                  boolean ignoreCase, int limit) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static List<String> split(@NotModified CharSequence receiver, @NotModified char[] delimiters,
                                  boolean ignoreCase, int limit) {
            return null;
        }

        /* a lazy view over the receiver: dependent, so no @Independent */
        @NotNull
        static Sequence<String> splitToSequence(@NotModified CharSequence receiver, @NotModified String[] delimiters,
                                                boolean ignoreCase, int limit) {
            return null;
        }

        @NotNull
        static String trim(String receiver, @NotModified char... chars) {
            return null;
        }

        @NotNull
        static String trimStart(String receiver, @NotModified char... chars) {
            return null;
        }

        @NotNull
        static String trimEnd(String receiver, @NotModified char... chars) {
            return null;
        }
    }

    /* public fun ByteArray.decodeToString(): String -- reads the array */
    class StringsKt__StringsJVMKt$ {
        @NotNull
        static String decodeToString(@NotModified byte[] receiver) {
            return null;
        }
    }

    /* a regex match is a finished value: every accessor only reads it (next() finds the NEXT match, a new object) */
    interface MatchResult$ {
        @NotModified
        IntRange getRange();

        @NotModified
        String getValue();

        @NotModified
        MatchGroupCollection getGroups();

        @NotModified
        List<String> getGroupValues();
    }

    interface MatchGroupCollection$ {
        @NotModified
        MatchGroup get(int index);
    }
}
