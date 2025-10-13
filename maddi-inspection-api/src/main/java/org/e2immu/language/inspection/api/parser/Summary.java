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

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ModuleInfo;
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


        public ParseException(URI uri, Object where, String msg, Throwable throwable) {
            super(makeMessage(uri, where, msg, throwable), throwable);
            this.uri = uri;
            this.where = where;
            this.throwable = throwable;
        }

        private static String makeMessage(URI uri, Object where, String msg, Throwable throwable) {
            return (throwable == null ? "" : "Exception: " + throwable.getClass().getCanonicalName() + "\n")
                   + "In: " + uri + (uri == where || where == null ? "" : "\nIn: " + where) + "\nMessage: " + msg;
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
    }

    class FailFastException extends RuntimeException {
        public FailFastException(ParseException parseException) {
            super(parseException);
        }
    }

    void addType(TypeInfo typeInfo);

    void addParseException(ParseException parseException);

    List<ParseException> parseExceptions();

    Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap();

    void putSourceSetToModuleInfo(SourceSet sourceSet, ModuleInfo moduleInfo);

    Iterable<SourceSet> sourceSets();

    Set<TypeInfo> types();

    ParseResult parseResult();

}
