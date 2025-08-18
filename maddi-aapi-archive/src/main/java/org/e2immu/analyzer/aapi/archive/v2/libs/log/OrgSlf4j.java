package org.e2immu.analyzer.aapi.archive.v2.libs.log;
import org.e2immu.annotation.Container;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

public class OrgSlf4j {
    public static final String PACKAGE_NAME = "org.slf4j";
    //public interface ILoggerFactory
    @Container @Independent
    class ILoggerFactory$ {Logger getLogger(String string) { return null; } }

    //public interface Logger
    @ImmutableContainer
    class Logger$ {
        static final String ROOT_LOGGER_NAME = null;
        String getName() { return null; }
        LoggingEventBuilder makeLoggingEventBuilder(Level level) { return null; }
        LoggingEventBuilder atLevel(Level level) { return null; }
        boolean isEnabledForLevel(Level level) { return false; }
        boolean isTraceEnabled() { return false; }
        void trace(String string) { }
        void trace(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void trace(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void trace(String string, Object ... object) { }
        void trace(String string, Throwable throwable) { }
        boolean isTraceEnabled(Marker marker) { return false; }
        LoggingEventBuilder atTrace() { return null; }
        void trace(Marker marker, String string) { }
        void trace(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void trace(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void trace(Marker marker, String string, Object ... object) { }
        void trace(Marker marker, String string, Throwable throwable) { }
        boolean isDebugEnabled() { return false; }
        void debug(String string) { }
        void debug(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void debug(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void debug(String string, Object ... object) { }
        void debug(String string, Throwable throwable) { }
        boolean isDebugEnabled(Marker marker) { return false; }
        void debug(Marker marker, String string) { }
        void debug(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void debug(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void debug(Marker marker, String string, Object ... object) { }
        void debug(Marker marker, String string, Throwable throwable) { }
        LoggingEventBuilder atDebug() { return null; }
        boolean isInfoEnabled() { return false; }
        void info(String string) { }
        void info(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }
        void info(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void info(String string, Object ... object) { }
        void info(String string, Throwable throwable) { }
        boolean isInfoEnabled(Marker marker) { return false; }
        void info(Marker marker, String string) { }
        void info(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void info(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void info(Marker marker, String string, Object ... object) { }
        void info(Marker marker, String string, Throwable throwable) { }
        LoggingEventBuilder atInfo() { return null; }
        boolean isWarnEnabled() { return false; }
        void warn(String string) { }
        void warn(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }
        void warn(String string, Object ... object) { }
        void warn(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void warn(String string, Throwable throwable) { }
        boolean isWarnEnabled(Marker marker) { return false; }
        void warn(Marker marker, String string) { }
        void warn(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void warn(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void warn(Marker marker, String string, Object ... object) { }
        void warn(Marker marker, String string, Throwable throwable) { }
        LoggingEventBuilder atWarn() { return null; }
        boolean isErrorEnabled() { return false; }
        void error(String string) { }
        void error(String string, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void error(
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void error(String string, Object ... object) { }
        void error(String string, Throwable throwable) { }
        boolean isErrorEnabled(Marker marker) { return false; }
        void error(Marker marker, String string) { }
        void error(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object) { }

        void error(
            Marker marker,
            String string,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object object1) { }
        void error(Marker marker, String string, Object ... object) { }
        void error(Marker marker, String string, Throwable throwable) { }
        LoggingEventBuilder atError() { return null; }
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
