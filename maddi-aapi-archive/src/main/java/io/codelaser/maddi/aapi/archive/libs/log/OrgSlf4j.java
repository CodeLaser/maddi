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

package io.codelaser.maddi.aapi.archive.libs.log;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/*
 A Logger is a @Container: a log call reads its arguments. It is not immutable: a log call writes into the logging
 machinery's buffers, which is a modification, just not one we care about. So the log methods (trace ... error) carry
 a method-level @IgnoreModifications, as System.Logger.log does in JavaLang: the call is modifying, but neither the
 logger nor the field holding it counts as modified. No @StaticSideEffects: that is for static modifying methods on
 state the caller does not own (System.setOut), and none of slf4j's modifying methods are static.
 */
public class OrgSlf4j {
    public static final String PACKAGE_NAME = "org.slf4j";
    //public interface ILoggerFactory
    @Container @Independent
    class ILoggerFactory$ {Logger getLogger(String string) { return null; } }

    //public interface Logger
    @Container @Independent
    class Logger$ {
        static final String ROOT_LOGGER_NAME = null;
        @NotModified String getName() { return null; }
        @NotModified LoggingEventBuilder makeLoggingEventBuilder(Level level) { return null; }
        @NotModified LoggingEventBuilder atLevel(Level level) { return null; }
        boolean isEnabledForLevel(Level level) { return false; }
        @NotModified boolean isTraceEnabled() { return false; }
        @IgnoreModifications void trace(String string) { }
        @IgnoreModifications void trace(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void trace(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void trace(String string, Object ... object) { }
        @IgnoreModifications void trace(String string, Throwable throwable) { }
        @NotModified boolean isTraceEnabled(Marker marker) { return false; }
        @NotModified LoggingEventBuilder atTrace() { return null; }
        @IgnoreModifications void trace(Marker marker, String string) { }
        @IgnoreModifications void trace(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void trace(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void trace(Marker marker, String string, Object ... object) { }
        @IgnoreModifications void trace(Marker marker, String string, Throwable throwable) { }
        @NotModified boolean isDebugEnabled() { return false; }
        @IgnoreModifications void debug(String string) { }
        @IgnoreModifications void debug(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void debug(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void debug(String string, Object ... object) { }
        @IgnoreModifications void debug(String string, Throwable throwable) { }
        @NotModified boolean isDebugEnabled(Marker marker) { return false; }
        @IgnoreModifications void debug(Marker marker, String string) { }
        @IgnoreModifications void debug(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void debug(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void debug(Marker marker, String string, Object ... object) { }
        @IgnoreModifications void debug(Marker marker, String string, Throwable throwable) { }
        @NotModified LoggingEventBuilder atDebug() { return null; }
        @NotModified boolean isInfoEnabled() { return false; }
        @IgnoreModifications void info(String string) { }
        @IgnoreModifications void info(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }
        @IgnoreModifications void info(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void info(String string, Object ... object) { }
        @IgnoreModifications void info(String string, Throwable throwable) { }
        @NotModified boolean isInfoEnabled(Marker marker) { return false; }
        @IgnoreModifications void info(Marker marker, String string) { }
        @IgnoreModifications void info(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void info(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void info(Marker marker, String string, Object ... object) { }
        @IgnoreModifications void info(Marker marker, String string, Throwable throwable) { }
        @NotModified LoggingEventBuilder atInfo() { return null; }
        @NotModified boolean isWarnEnabled() { return false; }
        @IgnoreModifications void warn(String string) { }
        @IgnoreModifications void warn(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }
        @IgnoreModifications void warn(String string, Object ... object) { }
        @IgnoreModifications void warn(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void warn(String string, Throwable throwable) { }
        @NotModified boolean isWarnEnabled(Marker marker) { return false; }
        @IgnoreModifications void warn(Marker marker, String string) { }
        @IgnoreModifications void warn(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void warn(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void warn(Marker marker, String string, Object ... object) { }
        @IgnoreModifications void warn(Marker marker, String string, Throwable throwable) { }
        @NotModified LoggingEventBuilder atWarn() { return null; }
        @NotModified boolean isErrorEnabled() { return false; }
        @IgnoreModifications void error(String string) { }
        @IgnoreModifications void error(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void error(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void error(String string, Object ... object) { }
        @IgnoreModifications void error(String string, Throwable throwable) { }
        @NotModified boolean isErrorEnabled(Marker marker) { return false; }
        @IgnoreModifications void error(Marker marker, String string) { }
        @IgnoreModifications void error(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        @IgnoreModifications void error(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        @IgnoreModifications void error(Marker marker, String string, Object ... object) { }
        @IgnoreModifications void error(Marker marker, String string, Throwable throwable) { }
        @NotModified LoggingEventBuilder atError() { return null; }
    }

    //public final class LoggerFactory
    @Container @Independent
    class LoggerFactory$ {
        static final String PROVIDER_PROPERTY_KEY = null;
        //@Independent[T]
        static Logger getLogger(String name) { return null; }

        //@Independent[T]
        static Logger getLogger(Class<?> clazz) { return null; }

        //@Independent[T]
        static ILoggerFactory getILoggerFactory() { return null; }
    }
}
