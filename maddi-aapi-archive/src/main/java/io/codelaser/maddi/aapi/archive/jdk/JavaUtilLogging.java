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

import java.time.Instant;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

/**
 * java.util.logging, found by the library-call census (TestLibraryCallCensus, 2026-10-12): the module had no hints, so every
 * {@code Logger.log(level, msg, thrown)} read its {@code Throwable} and {@code Object...} parameters as modified
 * (1,177 calls in jenkins alone). As for slf4j ({@code libs/log}): logging reads its arguments, so the logger is a
 * {@code @Container}. A log call does modify (it writes into the handlers' buffers), but not in a way we care about:
 * the log methods carry a method-level {@code @IgnoreModifications}, as {@code System.Logger.log} does in JavaLang.
 * The configuration methods ({@code setLevel}, {@code addHandler}, {@code setParent}, ...) keep the modifying
 * default: what they change is read back ({@code getLevel}, {@code isLoggable}, {@code getHandlers}). None of them is
 * static, so none is a {@code @StaticSideEffects}.
 * {@code log(LogRecord)} stamps the record (logger name, resource bundle), so the record is {@code @Modified}.
 */
public class JavaUtilLogging {
    public static final String PACKAGE_NAME = "java.util.logging";

    //public class Level implements Serializable
    @ImmutableContainer
    class Level$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Level$(@NotNull String p0, int p1) { }
        Level$(@NotNull String p0, int p1, @Nullable String p2) { }
        @Nullable String getResourceBundleName() { return null; }
        static Level parse(@NotNull String p0) { return null; }
    }

    //public class Logger
    @Container @Independent
    class Logger$ {
        static Logger getGlobal() { return null; }
        static Logger getLogger(@NotNull String p0) { return null; }
        static Logger getLogger(@NotNull String p0, @Nullable String p1) { return null; }
        static Logger getAnonymousLogger() { return null; }
        static Logger getAnonymousLogger(@Nullable String p0) { return null; }
        @Independent(absent = true) @Nullable @NotModified ResourceBundle getResourceBundle() { return null; }
        @Nullable @NotModified String getResourceBundleName() { return null; }
        @Modified void setFilter(@Independent(absent = true) @Nullable Filter p0) { }
        @Independent(absent = true) @Nullable @NotModified Filter getFilter() { return null; }
        @IgnoreModifications void log(@NotNull @Modified LogRecord p0) { }
        @IgnoreModifications void log(@NotNull Level p0, @Nullable String p1) { }
        @IgnoreModifications void log(@NotNull Level p0, @NotNull(nullableArguments = 0) Supplier<String> p1) { }
        @IgnoreModifications void log(@NotNull Level p0, @Nullable String p1, @Nullable Object p2) { }
        @IgnoreModifications void log(@NotNull Level p0, @Nullable String p1, @Nullable(nullableArguments = 0) Object[] p2) { }
        @IgnoreModifications void log(@NotNull Level p0, @Nullable String p1, @Nullable Throwable p2) { }
        @IgnoreModifications void log(@NotNull Level p0, @Nullable Throwable p1, @NotNull(nullableArguments = 0) Supplier<String> p2) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @NotNull(nullableArguments = 0) Supplier<String> p3) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable Object p4) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable(nullableArguments = 0) Object[] p4) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable Throwable p4) { }
        @IgnoreModifications void logp(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable Throwable p3, @NotNull(nullableArguments = 0) Supplier<String> p4) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable String p4) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable String p4, @Nullable Object p5) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable String p4, @Nullable(nullableArguments = 0) Object[] p5) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable ResourceBundle p3, @Nullable String p4, @Nullable(nullableArguments = 0) Object... p5) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable ResourceBundle p1, @Nullable String p2, @Nullable(nullableArguments = 0) Object... p3) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable String p3, @Nullable String p4, @Nullable Throwable p5) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable String p1, @Nullable String p2, @Nullable ResourceBundle p3, @Nullable String p4, @Nullable Throwable p5) { }
        @IgnoreModifications void logrb(@NotNull Level p0, @Nullable ResourceBundle p1, @Nullable String p2, @Nullable Throwable p3) { }
        @IgnoreModifications void entering(@Nullable String p0, @Nullable String p1) { }
        @IgnoreModifications void entering(@Nullable String p0, @Nullable String p1, @Nullable Object p2) { }
        @IgnoreModifications void entering(@Nullable String p0, @Nullable String p1, @Nullable(nullableArguments = 0) Object[] p2) { }
        @IgnoreModifications void exiting(@Nullable String p0, @Nullable String p1) { }
        @IgnoreModifications void exiting(@Nullable String p0, @Nullable String p1, @Nullable Object p2) { }
        @IgnoreModifications void throwing(@Nullable String p0, @Nullable String p1, @Nullable Throwable p2) { }
        @IgnoreModifications void severe(@Nullable String p0) { }
        @IgnoreModifications void warning(@Nullable String p0) { }
        @IgnoreModifications void info(@Nullable String p0) { }
        @IgnoreModifications void config(@Nullable String p0) { }
        @IgnoreModifications void fine(@Nullable String p0) { }
        @IgnoreModifications void finer(@Nullable String p0) { }
        @IgnoreModifications void finest(@Nullable String p0) { }
        @IgnoreModifications void severe(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void warning(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void info(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void config(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void fine(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void finer(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @IgnoreModifications void finest(@NotNull(nullableArguments = 0) Supplier<String> p0) { }
        @Modified void setLevel(@Nullable Level p0) { }
        @Nullable @NotModified Level getLevel() { return null; }
        @NotModified boolean isLoggable(@NotNull Level p0) { return false; }
        @Nullable @NotModified String getName() { return null; }
        @Modified void addHandler(@Independent(absent = true) @NotNull Handler p0) { }
        @Modified void removeHandler(@Nullable Handler p0) { }
        @Independent(absent = true) @NotModified Handler[] getHandlers() { return null; }
        @Modified void setUseParentHandlers(boolean p0) { }
        @NotModified boolean getUseParentHandlers() { return false; }
        @Modified void setResourceBundle(@Independent(absent = true) @NotNull ResourceBundle p0) { }
        @Independent(absent = true) @Nullable @NotModified Logger getParent() { return null; }
        @Modified void setParent(@Independent(absent = true) @NotNull Logger p0) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Logger$(@Nullable String p0, @Nullable String p1) { }
    }

    //public java.util.logging.Handler (shadow created for its nullability, generated)
    class Handler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable String getEncoding() { return null; }
        @NotModified @Nullable Filter getFilter() { return null; }
        @NotModified @Nullable Formatter getFormatter() { return null; }
        @NotModified boolean isLoggable(@Nullable LogRecord p0) { return false; }
        void publish(@Nullable LogRecord p0) { }
        void reportError(@Nullable String p0, @Nullable Exception p1, int p2) { }
        @Modified void setEncoding(@Nullable String p0) { }
        @Modified void setErrorManager(@NotNull ErrorManager p0) { }
        @Modified void setFilter(@Nullable Filter p0) { }
        @Modified void setFormatter(@NotNull Formatter p0) { }
        @Modified void setLevel(@NotNull Level p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified void flush() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified ErrorManager getErrorManager() { return null; }
        @NotModified Level getLevel() { return null; }
    }

    //public java.util.logging.LogRecord (shadow created for its nullability, generated)
    class LogRecord$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LogRecord$(@NotNull Level p0, @Nullable String p1) { }
        @NotModified @Nullable String getLoggerName() { return null; }
        @NotModified @Nullable String getMessage() { return null; }
        @NotModified @Nullable(nullableArguments = 0) Object[] getParameters() { return null; }
        @NotModified @Nullable ResourceBundle getResourceBundle() { return null; }
        @NotModified @Nullable String getResourceBundleName() { return null; }
        @NotModified @Nullable String getSourceClassName() { return null; }
        @NotModified @Nullable String getSourceMethodName() { return null; }
        @NotModified @Nullable Throwable getThrown() { return null; }
        @Modified void setInstant(@NotNull Instant p0) { }
        @Modified void setLevel(@NotNull Level p0) { }
        @Modified void setLoggerName(@Nullable String p0) { }
        @Modified void setMessage(@Nullable String p0) { }
        @Modified void setParameters(@Nullable(nullableArguments = 0) Object[] p0) { }
        @Modified void setResourceBundle(@Nullable ResourceBundle p0) { }
        @Modified void setResourceBundleName(@Nullable String p0) { }
        @Modified void setSourceClassName(@Nullable String p0) { }
        @Modified void setSourceMethodName(@Nullable String p0) { }
        @Modified void setThrown(@Nullable Throwable p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified LogRecord setLongThreadID(long p0) { return null; }
        @Modified void setMillis(long p0) { }
        @Modified void setSequenceNumber(long p0) { }
        @Modified void setThreadID(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Instant getInstant() { return null; }
        @NotModified Level getLevel() { return null; }
        @NotModified long getLongThreadID() { return 0; }
        @NotModified long getMillis() { return 0; }
        @NotModified long getSequenceNumber() { return 0; }
        @NotModified int getThreadID() { return 0; }
    }
}
