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

package io.codelaser.maddi.aapi.archive.jdk;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilRegex {
    public static final String PACKAGE_NAME = "java.util.regex";
    //public interface MatchResult
    // read-only view (start/end/group/...); no method modifies a parameter. Implemented by the
    // mutable Matcher, so @Container but not @Immutable.
    @Container
    class MatchResult$ {
        @NotModified int start() { return 0; }
        @NotModified int start(int i) { return 0; }
        @NotModified int start(@NotNull String name) { return 0; }
        @NotModified int end() { return 0; }
        @NotModified int end(int i) { return 0; }
        @NotModified int end(@NotNull String name) { return 0; }
        @NotModified String group() { return null; }
        @Nullable @NotModified String group(int i) { return null; }
        @Nullable @NotModified String group(@NotNull String name) { return null; }
        @NotModified int groupCount() { return 0; }
        @NotModified Map<String, Integer> namedGroups() { return null; }
        @NotModified boolean hasMatch() { return false; }
    }

    //public final class Matcher implements MatchResult
    class Matcher$ {
        @NotModified Pattern pattern() { return null; }
        @NotModified MatchResult toMatchResult() { return null; }
        Matcher usePattern(@NotNull Pattern newPattern) { return null; }
        @Modified Matcher reset() { return null; }
        @Modified Matcher reset(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input) {
            return null;
        }

        //override from java.util.regex.MatchResult
        @NotModified int start() { return 0; }

        //override from java.util.regex.MatchResult
        int start(int group) { return 0; }

        //override from java.util.regex.MatchResult
        @NotModified int start(@NotNull String name) { return 0; }

        //override from java.util.regex.MatchResult
        @NotModified int end() { return 0; }

        //override from java.util.regex.MatchResult
        int end(int group) { return 0; }

        //override from java.util.regex.MatchResult
        @NotModified int end(@NotNull String name) { return 0; }

        //override from java.util.regex.MatchResult
        @NotModified String group() { return null; }

        //override from java.util.regex.MatchResult
        @Nullable String group(int group) { return null; }

        //override from java.util.regex.MatchResult
        @Nullable @NotModified String group(@NotNull String name) { return null; }

        //override from java.util.regex.MatchResult
        @NotModified int groupCount() { return 0; }
        boolean matches() { return false; }
        boolean find() { return false; }
        boolean find(int start) { return false; }
        boolean lookingAt() { return false; }
        static String quoteReplacement(@NotNull String s) { return null; }
        @Modified Matcher appendReplacement(@NotNull StringBuffer sb, @NotNull String replacement) { return null; }
        @Modified Matcher appendReplacement(@NotNull StringBuilder sb, @NotNull String replacement) { return null; }
        @Modified StringBuffer appendTail(@NotNull StringBuffer sb) { return null; }
        @Modified StringBuilder appendTail(@NotNull StringBuilder sb) { return null; }
        //frequency 2
        @Modified String replaceAll(@NotNull String replacement) { return null; }
        @Modified String replaceAll(/*@IgnoreModifications[T]*/ @NotNull Function<MatchResult, String> replacer) { return null; }
        Stream<MatchResult> results() { return null; }
        @Modified String replaceFirst(@NotNull String replacement) { return null; }
        @Modified String replaceFirst(/*@IgnoreModifications[T]*/ @NotNull Function<MatchResult, String> replacer) { return null; }
        Matcher region(int start, int end) { return null; }
        @NotModified int regionStart() { return 0; }
        @NotModified int regionEnd() { return 0; }
        @NotModified boolean hasTransparentBounds() { return false; }
        Matcher useTransparentBounds(boolean b) { return null; }
        @NotModified boolean hasAnchoringBounds() { return false; }
        Matcher useAnchoringBounds(boolean b) { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified boolean hitEnd() { return false; }
        @NotModified boolean requireEnd() { return false; }
        //override from java.util.regex.MatchResult
        @NotModified Map<String, Integer> namedGroups() { return null; }

        //override from java.util.regex.MatchResult
        @NotModified boolean hasMatch() { return false; }
    }

    //public final class Pattern implements Serializable
    // a compiled pattern is a final, thread-safe immutable value; all methods are read-only
    @ImmutableContainer
    class Pattern$ {
        static final int UNIX_LINES = 0;
        static final int CASE_INSENSITIVE = 0;
        static final int COMMENTS = 0;
        static final int MULTILINE = 0;
        static final int LITERAL = 0;
        static final int DOTALL = 0;
        static final int UNICODE_CASE = 0;
        static final int CANON_EQ = 0;
        static final int UNICODE_CHARACTER_CLASS = 0;
        static Pattern compile(@NotNull String regex) { return null; }
        static Pattern compile(@NotNull String regex, int flags) { return null; }
        String pattern() { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //frequency 2
        Matcher matcher(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input) {
            return null;
        }
        int flags() { return 0; }
        static boolean matches(
            @NotNull String regex,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input) { return false; }

        String [] split(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input,
            int limit) { return null; }

        String [] splitWithDelimiters(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input,
            int limit) { return null; }

        String [] split(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input) {
            return null;
        }
        static String quote(@NotNull String s) { return null; }
        @NotModified Map<String, Integer> namedGroups() { return null; }
        Predicate<String> asPredicate() { return null; }
        Predicate<String> asMatchPredicate() { return null; }
        Stream<String> splitAsStream(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence input) { return null; }
    }

    //public java.util.regex.PatternSyntaxException (shadow created for its nullability, generated)
    class PatternSyntaxException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PatternSyntaxException$(@NotNull java.lang.String p0, @NotNull java.lang.String p1, int p2) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getDescription() { return null; }
        @NotModified int getIndex() { return 0; }
        @NotModified String getPattern() { return null; }
    }
}
