package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class JavaUtilRegex {
    public static final String PACKAGE_NAME = "java.util.regex";
    //public interface MatchResult
    class MatchResult$ {
        int start() { return 0; }
        int start(int i) { return 0; }
        int start(String name) { return 0; }
        int end() { return 0; }
        int end(int i) { return 0; }
        int end(String name) { return 0; }
        String group() { return null; }
        String group(int i) { return null; }
        String group(String name) { return null; }
        int groupCount() { return 0; }
        Map<String, Integer> namedGroups() { return null; }
        boolean hasMatch() { return false; }
    }

    //public final class Matcher implements MatchResult
    class Matcher$ {
        Pattern pattern() { return null; }
        MatchResult toMatchResult() { return null; }
        Matcher usePattern(Pattern newPattern) { return null; }
        Matcher reset() { return null; }
        Matcher reset(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input) {
            return null;
        }

        //override from java.util.regex.MatchResult
        int start() { return 0; }

        //override from java.util.regex.MatchResult
        int start(int group) { return 0; }

        //override from java.util.regex.MatchResult
        int start(String name) { return 0; }

        //override from java.util.regex.MatchResult
        int end() { return 0; }

        //override from java.util.regex.MatchResult
        int end(int group) { return 0; }

        //override from java.util.regex.MatchResult
        int end(String name) { return 0; }

        //override from java.util.regex.MatchResult
        String group() { return null; }

        //override from java.util.regex.MatchResult
        String group(int group) { return null; }

        //override from java.util.regex.MatchResult
        String group(String name) { return null; }

        //override from java.util.regex.MatchResult
        int groupCount() { return 0; }
        boolean matches() { return false; }
        boolean find() { return false; }
        boolean find(int start) { return false; }
        boolean lookingAt() { return false; }
        static String quoteReplacement(String s) { return null; }
        Matcher appendReplacement(StringBuffer sb, String replacement) { return null; }
        Matcher appendReplacement(StringBuilder sb, String replacement) { return null; }
        StringBuffer appendTail(StringBuffer sb) { return null; }
        StringBuilder appendTail(StringBuilder sb) { return null; }
        //frequency 2
        String replaceAll(String replacement) { return null; }
        String replaceAll(/*@IgnoreModifications[T]*/ Function<MatchResult, String> replacer) { return null; }
        Stream<MatchResult> results() { return null; }
        String replaceFirst(String replacement) { return null; }
        String replaceFirst(/*@IgnoreModifications[T]*/ Function<MatchResult, String> replacer) { return null; }
        Matcher region(int start, int end) { return null; }
        int regionStart() { return 0; }
        int regionEnd() { return 0; }
        boolean hasTransparentBounds() { return false; }
        Matcher useTransparentBounds(boolean b) { return null; }
        boolean hasAnchoringBounds() { return false; }
        Matcher useAnchoringBounds(boolean b) { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        boolean hitEnd() { return false; }
        boolean requireEnd() { return false; }
        //override from java.util.regex.MatchResult
        Map<String, Integer> namedGroups() { return null; }

        //override from java.util.regex.MatchResult
        boolean hasMatch() { return false; }
    }

    //public final class Pattern implements Serializable
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
        static Pattern compile(String regex) { return null; }
        static Pattern compile(String regex, int flags) { return null; }
        String pattern() { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //frequency 2
        Matcher matcher(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input) {
            return null;
        }
        int flags() { return 0; }
        static boolean matches(
            String regex,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input) { return false; }

        String [] split(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input,
            int limit) { return null; }

        String [] splitWithDelimiters(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input,
            int limit) { return null; }

        String [] split(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input) {
            return null;
        }
        static String quote(String s) { return null; }
        Map<String, Integer> namedGroups() { return null; }
        Predicate<String> asPredicate() { return null; }
        Predicate<String> asMatchPredicate() { return null; }
        Stream<String> splitAsStream(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence input) { return null; }
    }
}
