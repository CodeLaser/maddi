package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.time.Duration;
import java.time.temporal.*;
import java.util.List;

public class JavaTimeTemporal {
    public static final String PACKAGE_NAME = "java.time.temporal";
    //public enum ChronoUnit extends Enum<ChronoUnit> implements TemporalUnit
    class ChronoUnit$ {
        //@NotNull[O]
        static final ChronoUnit NANOS = null;

        //@NotNull[O]
        static final ChronoUnit MICROS = null;

        //@NotNull[O]
        static final ChronoUnit MILLIS = null;

        //@NotNull[O]
        static final ChronoUnit SECONDS = null;

        //@NotNull[O]
        static final ChronoUnit MINUTES = null;

        //@NotNull[O]
        static final ChronoUnit HOURS = null;

        //@NotNull[O]
        static final ChronoUnit HALF_DAYS = null;

        //@NotNull[O]
        static final ChronoUnit DAYS = null;

        //@NotNull[O]
        static final ChronoUnit WEEKS = null;

        //@NotNull[O]
        static final ChronoUnit MONTHS = null;

        //@NotNull[O]
        static final ChronoUnit YEARS = null;

        //@NotNull[O]
        static final ChronoUnit DECADES = null;

        //@NotNull[O]
        static final ChronoUnit CENTURIES = null;

        //@NotNull[O]
        static final ChronoUnit MILLENNIA = null;

        //@NotNull[O]
        static final ChronoUnit ERAS = null;

        //@NotNull[O]
        static final ChronoUnit FOREVER = null;
        static ChronoUnit [] values() { return null; }
        static ChronoUnit valueOf(String name) { return null; }
        //override from java.time.temporal.TemporalUnit
        Duration getDuration() { return null; }

        //override from java.time.temporal.TemporalUnit
        boolean isDurationEstimated() { return false; }

        //override from java.time.temporal.TemporalUnit
        boolean isDateBased() { return false; }

        //override from java.time.temporal.TemporalUnit
        boolean isTimeBased() { return false; }

        //override from java.time.temporal.TemporalUnit
        boolean isSupportedBy(Temporal temporal) { return false; }

        //override from java.time.temporal.TemporalUnit
        <R extends Temporal> R addTo(R temporal, long amount) { return null; }

        //override from java.time.temporal.TemporalUnit
        //frequency 1
        long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) { return 0L; }

        //override from java.lang.Enum, java.lang.Object, java.time.temporal.TemporalUnit
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public interface Temporal implements TemporalAccessor
    class Temporal$ {
        boolean isSupported(TemporalUnit temporalUnit) { return false; }
        Temporal with(TemporalAdjuster adjuster) { return null; }
        Temporal with(TemporalField temporalField, long l) { return null; }
        Temporal plus(TemporalAmount amount) { return null; }
        //override has frequency 1
        Temporal plus(long l, TemporalUnit temporalUnit) { return null; }
        Temporal minus(TemporalAmount amount) { return null; }
        //override has frequency 6
        Temporal minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }
        long until(Temporal temporal, TemporalUnit temporalUnit) { return 0L; }
    }

    //public interface TemporalAccessor
    class TemporalAccessor$ {
        boolean isSupported(TemporalField temporalField) { return false; }
        ValueRange range(TemporalField field) { return null; }
        int get(TemporalField field) { return 0; }
        long getLong(TemporalField temporalField) { return 0L; }
        //@Independent(hc=true)[T]
        <R> R query(TemporalQuery<R> query) { return null; }
    }

    //public interface TemporalAdjuster
    class TemporalAdjuster$ {Temporal adjustInto(Temporal temporal) { return null; } }

    //public interface TemporalAmount
    class TemporalAmount$ {
        long get(TemporalUnit temporalUnit) { return 0L; }
        List<TemporalUnit> getUnits() { return null; }
        Temporal addTo(Temporal temporal) { return null; }
        Temporal subtractFrom(Temporal temporal) { return null; }
    }

    //public interface TemporalUnit
    class TemporalUnit$ {
        Duration getDuration() { return null; }
        boolean isDurationEstimated() { return false; }
        boolean isDateBased() { return false; }
        boolean isTimeBased() { return false; }
        boolean isSupportedBy(Temporal temporal) { return false; }
        <R extends Temporal> R addTo(R r, long l) { return null; }
        //override has frequency 1
        long between(Temporal temporal, Temporal temporal1) { return 0L; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }
}
