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

package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
When parsing is successful, switch to ParseResult.

 */
public interface Summary {

    void ensureSourceSet(SourceSet sourceSet);

    boolean haveErrors();

    class ParseException extends RuntimeException {
        private final URI uri;
        private final Throwable throwable;
        private final Object where;
        private final Source source;       // line/col, derived from 'where' when it is a located Element
        private final Message.Level level; // WARN (tolerable) or ERROR

        public ParseException(URI uri, Object where, String msg, Throwable throwable) {
            this(uri, where, msg, throwable, Message.Severity.ERROR);
        }

        public ParseException(URI uri, Object where, String msg, Throwable throwable, Message.Level level) {
            super(makeMessage(uri, deriveSource(where), where, msg, throwable), throwable);
            this.uri = uri;
            this.where = where;
            this.source = deriveSource(where);
            this.level = level;
            this.throwable = throwable;
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

        /** Location (line/col) of this error, or {@code null} if unknown. */
        public Source source() {
            return source;
        }

        /** Severity: {@link Message.Severity#WARN} or {@link Message.Severity#ERROR}. */
        public Message.Level level() {
            return level;
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

}
