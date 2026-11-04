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

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;
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
        Level$(@NotNull java.lang.String p0, int p1) { }
        Level$(@NotNull java.lang.String p0, int p1, @Nullable java.lang.String p2) { }
        @Nullable java.lang.String getResourceBundleName() { return null; }
        static java.util.logging.Level parse(@NotNull java.lang.String p0) { return null; }
    }

    //public class Logger
    @Container @Independent
    class Logger$ {
        static java.util.logging.Logger getGlobal() { return null; }
        static java.util.logging.Logger getLogger(@NotNull java.lang.String p0) { return null; }
        static java.util.logging.Logger getLogger(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { return null; }
        static java.util.logging.Logger getAnonymousLogger() { return null; }
        static java.util.logging.Logger getAnonymousLogger(@Nullable java.lang.String p0) { return null; }
        @Independent(absent = true) @Nullable @NotModified java.util.ResourceBundle getResourceBundle() { return null; }
        @Nullable @NotModified java.lang.String getResourceBundleName() { return null; }
        @Modified void setFilter(@Independent(absent = true) @Nullable java.util.logging.Filter p0) { }
        @Independent(absent = true) @Nullable @NotModified java.util.logging.Filter getFilter() { return null; }
        @IgnoreModifications void log(@NotNull @Modified java.util.logging.LogRecord p0) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p1) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable(nullableArguments = 0) java.lang.Object[] p2) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.Throwable p2) { }
        @IgnoreModifications void log(@NotNull java.util.logging.Level p0, @Nullable java.lang.Throwable p1, @NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p2) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p3) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.Object p4) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable(nullableArguments = 0) java.lang.Object[] p4) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.Throwable p4) { }
        @IgnoreModifications void logp(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.Throwable p3, @NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p4) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.Object p5) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable(nullableArguments = 0) java.lang.Object[] p5) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.util.ResourceBundle p3, @Nullable java.lang.String p4, @Nullable(nullableArguments = 0) java.lang.Object... p5) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.util.ResourceBundle p1, @Nullable java.lang.String p2, @Nullable(nullableArguments = 0) java.lang.Object... p3) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.lang.String p3, @Nullable java.lang.String p4, @Nullable java.lang.Throwable p5) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2, @Nullable java.util.ResourceBundle p3, @Nullable java.lang.String p4, @Nullable java.lang.Throwable p5) { }
        @IgnoreModifications void logrb(@NotNull java.util.logging.Level p0, @Nullable java.util.ResourceBundle p1, @Nullable java.lang.String p2, @Nullable java.lang.Throwable p3) { }
        @IgnoreModifications void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
        @IgnoreModifications void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @IgnoreModifications void entering(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable(nullableArguments = 0) java.lang.Object[] p2) { }
        @IgnoreModifications void exiting(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
        @IgnoreModifications void exiting(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Object p2) { }
        @IgnoreModifications void throwing(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.Throwable p2) { }
        @IgnoreModifications void severe(@Nullable java.lang.String p0) { }
        @IgnoreModifications void warning(@Nullable java.lang.String p0) { }
        @IgnoreModifications void info(@Nullable java.lang.String p0) { }
        @IgnoreModifications void config(@Nullable java.lang.String p0) { }
        @IgnoreModifications void fine(@Nullable java.lang.String p0) { }
        @IgnoreModifications void finer(@Nullable java.lang.String p0) { }
        @IgnoreModifications void finest(@Nullable java.lang.String p0) { }
        @IgnoreModifications void severe(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void warning(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void info(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void config(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void fine(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void finer(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @IgnoreModifications void finest(@NotNull(nullableArguments = 0) java.util.function.Supplier<java.lang.String> p0) { }
        @Modified void setLevel(@Nullable java.util.logging.Level p0) { }
        @Nullable @NotModified java.util.logging.Level getLevel() { return null; }
        @NotModified boolean isLoggable(@NotNull java.util.logging.Level p0) { return false; }
        @Nullable @NotModified java.lang.String getName() { return null; }
        @Modified void addHandler(@Independent(absent = true) @NotNull java.util.logging.Handler p0) { }
        @Modified void removeHandler(@Nullable java.util.logging.Handler p0) { }
        @Independent(absent = true) @NotModified java.util.logging.Handler[] getHandlers() { return null; }
        @Modified void setUseParentHandlers(boolean p0) { }
        @NotModified boolean getUseParentHandlers() { return false; }
        @Modified void setResourceBundle(@Independent(absent = true) @NotNull java.util.ResourceBundle p0) { }
        @Independent(absent = true) @Nullable @NotModified java.util.logging.Logger getParent() { return null; }
        @Modified void setParent(@Independent(absent = true) @NotNull java.util.logging.Logger p0) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Logger$(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { }
    }

    //public java.util.logging.Handler (shadow created for its nullability, generated)
    class Handler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable java.lang.String getEncoding() { return null; }
        @NotModified @Nullable java.util.logging.Filter getFilter() { return null; }
        @NotModified @Nullable java.util.logging.Formatter getFormatter() { return null; }
        @NotModified boolean isLoggable(@Nullable java.util.logging.LogRecord p0) { return false; }
        void publish(@Nullable java.util.logging.LogRecord p0) { }
        void reportError(@Nullable java.lang.String p0, @Nullable java.lang.Exception p1, int p2) { }
        @Modified void setEncoding(@Nullable java.lang.String p0) { }
        @Modified void setErrorManager(@NotNull java.util.logging.ErrorManager p0) { }
        @Modified void setFilter(@Nullable java.util.logging.Filter p0) { }
        @Modified void setFormatter(@NotNull java.util.logging.Formatter p0) { }
        @Modified void setLevel(@NotNull java.util.logging.Level p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified void flush() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified java.util.logging.ErrorManager getErrorManager() { return null; }
        @NotModified java.util.logging.Level getLevel() { return null; }
    }

    //public java.util.logging.LogRecord (shadow created for its nullability, generated)
    class LogRecord$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LogRecord$(@NotNull java.util.logging.Level p0, @Nullable java.lang.String p1) { }
        @NotModified @Nullable java.lang.String getLoggerName() { return null; }
        @NotModified @Nullable java.lang.String getMessage() { return null; }
        @NotModified @Nullable(nullableArguments = 0) java.lang.Object[] getParameters() { return null; }
        @NotModified @Nullable java.util.ResourceBundle getResourceBundle() { return null; }
        @NotModified @Nullable java.lang.String getResourceBundleName() { return null; }
        @NotModified @Nullable java.lang.String getSourceClassName() { return null; }
        @NotModified @Nullable java.lang.String getSourceMethodName() { return null; }
        @NotModified @Nullable java.lang.Throwable getThrown() { return null; }
        @Modified void setInstant(@NotNull java.time.Instant p0) { }
        @Modified void setLevel(@NotNull java.util.logging.Level p0) { }
        @Modified void setLoggerName(@Nullable java.lang.String p0) { }
        @Modified void setMessage(@Nullable java.lang.String p0) { }
        @Modified void setParameters(@Nullable(nullableArguments = 0) java.lang.Object[] p0) { }
        @Modified void setResourceBundle(@Nullable java.util.ResourceBundle p0) { }
        @Modified void setResourceBundleName(@Nullable java.lang.String p0) { }
        @Modified void setSourceClassName(@Nullable java.lang.String p0) { }
        @Modified void setSourceMethodName(@Nullable java.lang.String p0) { }
        @Modified void setThrown(@Nullable java.lang.Throwable p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified java.util.logging.LogRecord setLongThreadID(long p0) { return null; }
        @Modified void setMillis(long p0) { }
        @Modified void setSequenceNumber(long p0) { }
        @Modified void setThreadID(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified java.time.Instant getInstant() { return null; }
        @NotModified java.util.logging.Level getLevel() { return null; }
        @NotModified long getLongThreadID() { return 0; }
        @NotModified long getMillis() { return 0; }
        @NotModified long getSequenceNumber() { return 0; }
        @NotModified int getThreadID() { return 0; }
    }
}
