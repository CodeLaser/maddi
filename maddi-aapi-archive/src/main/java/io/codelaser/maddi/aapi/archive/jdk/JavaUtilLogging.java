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
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

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
    class Level$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Level$(@NotNull java.lang.String p0, int p1) { }
        Level$(@NotNull java.lang.String p0, int p1, @Nullable java.lang.String p2) { }
        @Nullable java.lang.String getResourceBundleName() { return null; }
        static java.util.logging.Level parse(@NotNull java.lang.String p0) { return null; }
    }

    //public class Logger
    @Container
    class Logger$ {
        static java.util.logging.Logger getGlobal() { return null; }
        static java.util.logging.Logger getLogger(@NotNull java.lang.String p0) { return null; }
        static java.util.logging.Logger getLogger(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { return null; }
        static java.util.logging.Logger getAnonymousLogger() { return null; }
        static java.util.logging.Logger getAnonymousLogger(@Nullable java.lang.String p0) { return null; }
        @Nullable @NotModified java.util.ResourceBundle getResourceBundle() { return null; }
        @Nullable @NotModified java.lang.String getResourceBundleName() { return null; }
        void setFilter(@Nullable java.util.logging.Filter p0) { }
        @Nullable @NotModified java.util.logging.Filter getFilter() { return null; }
        @NotModified void log(@NotNull @Modified java.util.logging.LogRecord p0) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @NotNull java.util.function.Supplier<java.lang.String> p1) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.Object[] p2) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.Throwable p2) { }
        @NotModified void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.Throwable p1, @NotNull java.util.function.Supplier<java.lang.String> p2) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @NotNull java.util.function.Supplier<java.lang.String> p3) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.Object p4) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.Object[] p4) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.Throwable p4) { }
        @NotModified void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.Throwable p3, @NotNull java.util.function.Supplier<java.lang.String> p4) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.Object p5) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.Object[] p5) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.util.ResourceBundle p3, @Nullable java.lang.String p4, @Nullable java.lang.Object... p5) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.util.ResourceBundle p1, @Nullable java.lang.String p2, @Nullable java.lang.Object... p3) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.Throwable p5) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.util.ResourceBundle p3, @Nullable java.lang.String p4, @Nullable java.lang.Throwable p5) { }
        @NotModified void logrb(@NotNull java.util.logging.Level p0, @Nullable java.util.ResourceBundle p1, @Nullable java.lang.String p2, @Nullable java.lang.Throwable p3) { }
        @NotModified void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
        @NotModified void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @NotModified void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Object[] p2) { }
        @NotModified void exiting(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
        @NotModified void exiting(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @NotModified void throwing(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Throwable p2) { }
        @NotModified void severe(@Nullable java.lang.String p0) { }
        @NotModified void warning(@Nullable java.lang.String p0) { }
        @NotModified void info(@Nullable java.lang.String p0) { }
        @NotModified void config(@Nullable java.lang.String p0) { }
        @NotModified void fine(@Nullable java.lang.String p0) { }
        @NotModified void finer(@Nullable java.lang.String p0) { }
        @NotModified void finest(@Nullable java.lang.String p0) { }
        @NotModified void severe(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void warning(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void info(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void config(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void fine(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void finer(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        @NotModified void finest(@NotNull java.util.function.Supplier<java.lang.String> p0) { }
        void setLevel(@Nullable java.util.logging.Level p0) { }
        @Nullable @NotModified java.util.logging.Level getLevel() { return null; }
        @NotModified boolean isLoggable(@NotNull java.util.logging.Level p0) { return false; }
        @Nullable @NotModified java.lang.String getName() { return null; }
        void addHandler(@NotNull java.util.logging.Handler p0) { }
        void removeHandler(@Nullable java.util.logging.Handler p0) { }
        @NotModified java.util.logging.Handler[] getHandlers() { return null; }
        void setUseParentHandlers(boolean p0) { }
        @NotModified boolean getUseParentHandlers() { return false; }
        void setResourceBundle(@NotNull java.util.ResourceBundle p0) { }
        @Nullable @NotModified java.util.logging.Logger getParent() { return null; }
        void setParent(@NotNull java.util.logging.Logger p0) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Logger$(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
    }

    //public java.util.logging.Handler (shadow created for its nullability, generated)
    class Handler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.String getEncoding() { return null; }
        @Nullable java.util.logging.Filter getFilter() { return null; }
        @Nullable java.util.logging.Formatter getFormatter() { return null; }
        boolean isLoggable(@Nullable java.util.logging.LogRecord p0) { return false; }
        void publish(@Nullable java.util.logging.LogRecord p0) { }
        void reportError(@Nullable java.lang.String p0, @Nullable java.lang.Exception p1, int p2) { }
        void setEncoding(@Nullable java.lang.String p0) { }
        void setErrorManager(@NotNull java.util.logging.ErrorManager p0) { }
        void setFilter(@Nullable java.util.logging.Filter p0) { }
        void setFormatter(@NotNull java.util.logging.Formatter p0) { }
        void setLevel(@NotNull java.util.logging.Level p0) { }
    }

    //public java.util.logging.LogRecord (shadow created for its nullability, generated)
    class LogRecord$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LogRecord$(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1) { }
        @Nullable java.lang.String getLoggerName() { return null; }
        @Nullable java.lang.String getMessage() { return null; }
        @Nullable java.lang.Object[] getParameters() { return null; }
        @Nullable java.util.ResourceBundle getResourceBundle() { return null; }
        @Nullable java.lang.String getResourceBundleName() { return null; }
        @Nullable java.lang.String getSourceClassName() { return null; }
        @Nullable java.lang.String getSourceMethodName() { return null; }
        @Nullable java.lang.Throwable getThrown() { return null; }
        void setInstant(@NotNull java.time.Instant p0) { }
        void setLevel(@NotNull java.util.logging.Level p0) { }
        void setLoggerName(@Nullable java.lang.String p0) { }
        void setMessage(@Nullable java.lang.String p0) { }
        void setParameters(@Nullable java.lang.Object[] p0) { }
        void setResourceBundle(@Nullable java.util.ResourceBundle p0) { }
        void setResourceBundleName(@Nullable java.lang.String p0) { }
        void setSourceClassName(@Nullable java.lang.String p0) { }
        void setSourceMethodName(@Nullable java.lang.String p0) { }
        void setThrown(@Nullable java.lang.Throwable p0) { }
    }
}
