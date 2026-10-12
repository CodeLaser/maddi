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

package io.codelaser.maddi.aapi.archive.jdk;

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;

/**
 * java.util.logging, found by the library-call census (TestLibraryCallCensus, 2026-10-12): the module had no hints, so every
 * {@code Logger.log(level, msg, thrown)} read its {@code Throwable} and {@code Object...} parameters as modified
 * (1,177 calls in jenkins alone). As for slf4j ({@code libs/log}): logging reads its arguments, so the logger is a
 * {@code @Container}, and a log call does not modify the logger (what it changes is the handlers' output). The
 * configuration methods ({@code setLevel}, {@code addHandler}, {@code setParent}, ...) keep the modifying default.
 * {@code log(LogRecord)} stamps the record (logger name, resource bundle), so the record is {@code @Modified}.
 */
public class JavaUtilLogging {
    public static final String PACKAGE_NAME = "java.util.logging";

    //public class Level implements Serializable
    @ImmutableContainer
    class Level$ { }

    //public class Logger
    @Container
    class Logger$ {
        static java.util.logging.Logger getGlobal() { return null; }
        static java.util.logging.Logger getLogger(java.lang.String p0) { return null; }
        static java.util.logging.Logger getLogger(java.lang.String p0, java.lang.String p1) { return null; }
        static java.util.logging.Logger getAnonymousLogger() { return null; }
        static java.util.logging.Logger getAnonymousLogger(java.lang.String p0) { return null; }
        @NotModified java.util.ResourceBundle getResourceBundle() { return null; }
        @NotModified java.lang.String getResourceBundleName() { return null; }
        void setFilter(java.util.logging.Filter p0) { }
        @NotModified java.util.logging.Filter getFilter() { return null; }
        @NotModified void log(@Modified java.util.logging.LogRecord p0) { }
        @NotModified void log(java.util.logging.Level p0, java.lang.String p1) { }
        @NotModified void log(java.util.logging.Level p0, java.util.function.Supplier<java.lang.String> p1) { }
        @NotModified void log(java.util.logging.Level p0, java.lang.String p1, java.lang.Object p2) { }
        @NotModified void log(java.util.logging.Level p0, java.lang.String p1, java.lang.Object[] p2) { }
        @NotModified void log(java.util.logging.Level p0, java.lang.String p1, java.lang.Throwable p2) { }
        @NotModified void log(java.util.logging.Level p0, java.lang.Throwable p1, java.util.function.Supplier<java.lang.String> p2) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.util.function.Supplier<java.lang.String> p3) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.Object p4) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.Object[] p4) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.Throwable p4) { }
        @NotModified void logp(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.Throwable p3, java.util.function.Supplier<java.lang.String> p4) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.String p4) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.String p4, java.lang.Object p5) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.String p4, java.lang.Object[] p5) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.util.ResourceBundle p3, java.lang.String p4, java.lang.Object... p5) { }
        @NotModified void logrb(java.util.logging.Level p0, java.util.ResourceBundle p1, java.lang.String p2, java.lang.Object... p3) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, java.lang.String p4, java.lang.Throwable p5) { }
        @NotModified void logrb(java.util.logging.Level p0, java.lang.String p1, java.lang.String p2, java.util.ResourceBundle p3, java.lang.String p4, java.lang.Throwable p5) { }
        @NotModified void logrb(java.util.logging.Level p0, java.util.ResourceBundle p1, java.lang.String p2, java.lang.Throwable p3) { }
        @NotModified void entering(java.lang.String p0, java.lang.String p1) { }
        @NotModified void entering(java.lang.String p0, java.lang.String p1, java.lang.Object p2) { }
        @NotModified void entering(java.lang.String p0, java.lang.String p1, java.lang.Object[] p2) { }
        @NotModified void exiting(java.lang.String p0, java.lang.String p1) { }
        @NotModified void exiting(java.lang.String p0, java.lang.String p1, java.lang.Object p2) { }
        @NotModified void throwing(java.lang.String p0, java.lang.String p1, java.lang.Throwable p2) { }
        @NotModified void severe(java.lang.String p0) { }
        @NotModified void warning(java.lang.String p0) { }
        @NotModified void info(java.lang.String p0) { }
        @NotModified void config(java.lang.String p0) { }
        @NotModified void fine(java.lang.String p0) { }
        @NotModified void finer(java.lang.String p0) { }
        @NotModified void finest(java.lang.String p0) { }
        @NotModified void severe(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void warning(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void info(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void config(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void fine(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void finer(java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void finest(java.util.function.Supplier<java.lang.String> p0) { }
        void setLevel(java.util.logging.Level p0) { }
        @NotModified java.util.logging.Level getLevel() { return null; }
        @NotModified boolean isLoggable(java.util.logging.Level p0) { return false; }
        @NotModified java.lang.String getName() { return null; }
        void addHandler(java.util.logging.Handler p0) { }
        void removeHandler(java.util.logging.Handler p0) { }
        @NotModified java.util.logging.Handler[] getHandlers() { return null; }
        void setUseParentHandlers(boolean p0) { }
        @NotModified boolean getUseParentHandlers() { return false; }
        void setResourceBundle(java.util.ResourceBundle p0) { }
        @NotModified java.util.logging.Logger getParent() { return null; }
        void setParent(java.util.logging.Logger p0) { }
    }
}
