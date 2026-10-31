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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
When parsing is successful, switch to ParseResult.

 */
public interface Summary {

    void ensureSourceSet(SourceSet sourceSet);

    boolean haveErrors();

    /**
     * What javac will resolve a source set's references into is not what was parsed: the class output of one of its
     * dependencies is unusable, absent, or lacks or predates class files for types parsed from that dependency's
     * sources. References into those types do not resolve, and the compilation units holding them are dropped.
     * <p>
     * The structured form of the class-output WARN in {@link #parseWarnings()}: the same {@link ParseException}
     * carries it ({@link ParseException#classOutputProblem()}), and {@link #classOutputProblems()} collects them, so
     * a consumer never has to recognise the message text. One per (source set, dependency) pair with a problem; a
     * dependency whose class output is fresh produces none.
     * <p>
     * <b>Complete per summary.</b> Every parse checks every source set it holds, and so does a re-parse after a
     * write (kept and rewired sets as well as re-scanned ones): a summary's list replaces the previous one, it does
     * not add to it.
     * <p>
     * <b>What is checked:</b> for each source set, each of its dependencies that is itself a parsed source set read
     * from files on disk, and within it each parsed primary type: is there a class file (or, javac's class path
     * doubling as its source path, a source file) at its package path in the dependency's class output, and is that
     * class file no older than the type's source file?
     * <p>
     * <b>What is not:</b>
     * <ul>
     *     <li>a class directory that is only an external class-path part, not a parsed source set: there are no
     *     parsed sources to compare it with;</li>
     *     <li>an in-memory source set (no files, no build output to be wrong about);</li>
     *     <li>a class file whose type was deleted from the sources: it is not a parsed type, so it is never looked
     *     up, and a lingering class file is not flagged;</li>
     *     <li>package-info: never the target of a reference, and javac often writes no class file for it;</li>
     *     <li>nested types: only the primary type's class file is compared;</li>
     *     <li>a class file less than a second (file-system timestamp granularity) older than its source.</li>
     * </ul>
     * When maddi generates the class files itself ({@code JavaInspector.setGeneratedClassesDirectory}), the
     * generated directory is what is checked, and it is current by construction.
     *
     * @param sourceSet         the source set being compiled
     * @param dependency        the source set it resolves into through class files
     * @param kind              what is wrong
     * @param classDirectory    the class output checked; for {@link Kind#UNUSABLE_URI}, the dependency's URI as given
     * @param parsedTypes       how many types were parsed from the dependency's sources (package-infos included)
     * @param missing           fully qualified names of the primary types with neither a class file nor a source file
     *                          in the class output; for {@link Kind#UNUSABLE_URI} and {@link Kind#NO_CLASS_OUTPUT},
     *                          every primary type parsed from the dependency. Sorted. Package-infos never count.
     * @param stale             fully qualified names of the primary types whose class file is older than their source
     *                          file (by more than a second, file-system timestamp granularity). Sorted.
     * @param newestClassFile   the most recent modification time among the class files found for the parsed types,
     *                          {@code null} when none was found
     * @param oldestStaleSource the earliest modification time among the sources of the {@code stale} types,
     *                          {@code null} when nothing is stale
     */
    record ClassOutputProblem(String sourceSet,
                              String dependency,
                              Kind kind,
                              URI classDirectory,
                              int parsedTypes,
                              List<String> missing,
                              List<String> stale,
                              Instant newestClassFile,
                              Instant oldestStaleSource) {
        public enum Kind {
            /** the dependency's URI is not a usable class-path entry (relative, opaque, not a file URI) */
            UNUSABLE_URI,
            /** the class output directory does not exist */
            NO_CLASS_OUTPUT,
            /** the directory exists, but class files are missing or older than their sources */
            MISSING_OR_STALE
        }

        public ClassOutputProblem {
            Objects.requireNonNull(sourceSet);
            Objects.requireNonNull(dependency);
            Objects.requireNonNull(kind);
            missing = List.copyOf(missing);
            stale = List.copyOf(stale);
        }
    }

    /** The class-output problems among {@link #parseWarnings()}, in the order they were reported. */
    default List<ClassOutputProblem> classOutputProblems() {
        return parseWarnings().stream().map(ParseException::classOutputProblem).filter(Objects::nonNull).toList();
    }

    class ParseException extends RuntimeException {
        private final URI uri;
        private final Throwable throwable;
        private final Object where;
        private final String msg;
        private final Source source;       // line/col, derived from 'where' when it is a located Element
        private final Message.Level level; // WARN (tolerable) or ERROR
        private final ClassOutputProblem classOutputProblem; // null unless this reports one

        public ParseException(URI uri, Object where, String msg, Throwable throwable) {
            this(uri, where, msg, throwable, Message.Severity.ERROR);
        }

        public ParseException(URI uri, Object where, String msg, Throwable throwable, Message.Level level) {
            this(uri, where, msg, throwable, level, null);
        }

        /**
         * A WARN carrying the structured form of a class-output problem; {@code msg} stays the human-readable one.
         * A factory rather than a constructor: a constructor would make every existing {@code (uri, where, msg, null)}
         * call ambiguous.
         */
        public static ParseException classOutput(URI uri, Object where, String msg,
                                                 ClassOutputProblem classOutputProblem) {
            return new ParseException(uri, where, msg, null, Message.Severity.WARN,
                    Objects.requireNonNull(classOutputProblem));
        }

        private ParseException(URI uri, Object where, String msg, Throwable throwable, Message.Level level,
                               ClassOutputProblem classOutputProblem) {
            super(makeMessage(uri, deriveSource(where), where, msg, throwable), throwable);
            this.uri = uri;
            this.where = where;
            this.msg = msg;
            this.source = deriveSource(where);
            this.level = level;
            this.throwable = throwable;
            this.classOutputProblem = classOutputProblem;
        }

        // the CST carries a Source (line/col) on every element; recover it from 'where' so errors are locatable
        private static Source deriveSource(Object where) {
            if (where instanceof Source s) return s.isNoSource() ? null : s;
            if (where instanceof Element e) {
                Source s = e.source();
                return s == null || s.isNoSource() ? null : s;
            }
            return null;
        }

        private static String makeMessage(URI uri, Source source, Object where, String msg, Throwable throwable) {
            String at = uri == null ? "?" : uri.toString();
            if (source != null) at += ":" + source.compact();
            return (throwable == null ? "" : "Exception: " + throwable.getClass().getCanonicalName() + "\n")
                   + "In: " + at + (uri == where || where == null ? "" : "\nIn: " + where)
                   + "\nMessage: " + msg;
        }

        public ParseException(Context context, Object where, String msg, Throwable throwable) {
            this(context.enclosingType() == null ? null : context.enclosingType().compilationUnit().uri(), where, msg,
                    throwable);
        }

        public ParseException(Context context, Object where, String msg, Throwable throwable, Message.Level level) {
            this(context.enclosingType() == null ? null : context.enclosingType().compilationUnit().uri(), where, msg,
                    throwable, level);
        }

        public ParseException(Context context, String msg) {
            this(context.enclosingType() == null ? null : context.enclosingType().compilationUnit().uri(),
                    context.info(), msg, null);
        }

        public ParseException(CompilationUnit compilationUnit, Object where, String msg, Throwable throwable) {
            this(compilationUnit.uri(), where, msg, throwable);
        }

        public URI uri() {
            return uri;
        }

        public Throwable throwable() {
            return throwable;
        }

        public Object where() {
            return where;
        }

        /** The message alone, without the location and exception {@link #getMessage()} puts around it. */
        public String msg() {
            return msg;
        }

        /** Location (line/col) of this error, or {@code null} if unknown. */
        public Source source() {
            return source;
        }

        /** Severity: {@link Message.Severity#WARN} or {@link Message.Severity#ERROR}. */
        public Message.Level level() {
            return level;
        }

        /** The structured class-output problem this warning reports, {@code null} for any other parse message. */
        public ClassOutputProblem classOutputProblem() {
            return classOutputProblem;
        }
    }

    class FailFastException extends RuntimeException {
        public FailFastException(ParseException parseException) {
            super(parseException);
        }
    }

    void addType(TypeInfo typeInfo);

    void addParseException(ParseException parseException);

    List<ParseException> parseExceptions();

    // Non-fatal diagnostics (e.g. javac errors on maddi's deliberately partial classpath: unresolved references
    // that are expected, not failures). Warnings never trigger fail-fast and never make haveErrors() true.
    void addParseWarning(ParseException parseWarning);

    List<ParseException> parseWarnings();

    Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap();

    void putSourceSetToModuleInfo(SourceSet sourceSet, ModuleInfo moduleInfo);

    Iterable<SourceSet> sourceSets();

    Set<TypeInfo> types();

    ParseResult parseResult();

    /**
     * The parse result even when {@link #haveErrors()} — the types that DID parse, with the compilation units
     * that failed simply absent.
     * <p>
     * {@link #parseResult()} refuses on errors, and for a batch run that is right: a configuration that does not
     * fully resolve produces verdicts that are quietly weaker than the code allows, and silence is safer than a
     * plausible wrong answer. An IDE is the case where it is not right — a tree is routinely mid-edit, and one
     * unresolvable module must not cost the user every hint in every other module.
     * <p>
     * ⚠ The caller owns the consequences: properties of the surviving types may be weaker than they would be
     * with the missing ones present, because a reference that did not resolve contributes nothing. Report the
     * result as partial (a non-zero parse-error count) rather than as an analysis.
     */
    ParseResult parseResultIgnoringErrors();

}
