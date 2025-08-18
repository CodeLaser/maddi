package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.time.*;
import java.time.chrono.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.Comparator;

public class JavaTimeChrono {
    public static final String PACKAGE_NAME = "java.time.chrono";
    //public interface ChronoLocalDate implements Temporal, TemporalAdjuster, Comparable<ChronoLocalDate>
    class ChronoLocalDate$ {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoLocalDate> timeLineOrder() { return null; }
        static ChronoLocalDate from(TemporalAccessor temporal) { return null; }
        Chronology getChronology() { return null; }
        Era getEra() { return null; }
        boolean isLeapYear() { return false; }
        int lengthOfMonth() { return 0; }
        int lengthOfYear() { return 0; }
        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate with(TemporalField field, long newValue) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate plus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate minus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(Temporal temporal, TemporalUnit temporalUnit) { return 0L; }
        ChronoPeriod until(ChronoLocalDate chronoLocalDate) { return null; }
        String format(DateTimeFormatter formatter) { return null; }
        ChronoLocalDateTime<?> atTime(LocalTime localTime) { return null; }
        long toEpochDay() { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ChronoLocalDate other) { return 0; }
        boolean isAfter(ChronoLocalDate other) { return false; }
        boolean isBefore(ChronoLocalDate other) { return false; }
        boolean isEqual(ChronoLocalDate other) { return false; }
        //override from java.lang.Object
        //override has frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public interface ChronoLocalDateTime implements Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime<?>>
    class ChronoLocalDateTime$<D extends ChronoLocalDate> {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoLocalDateTime<?>> timeLineOrder() { return null; }
        static ChronoLocalDateTime<?> from(TemporalAccessor temporal) { return null; }
        Chronology getChronology() { return null; }
        D toLocalDate() { return null; }
        LocalTime toLocalTime() { return null; }
        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField temporalField) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> with(TemporalField temporalField, long l) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> plus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> plus(long l, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> minus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }
        String format(DateTimeFormatter formatter) { return null; }
        //override has frequency 1
        ChronoZonedDateTime<D> atZone(ZoneId zoneId) { return null; }
        Instant toInstant(ZoneOffset offset) { return null; }
        long toEpochSecond(ZoneOffset offset) { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ChronoLocalDateTime<?> other) { return 0; }
        boolean isAfter(ChronoLocalDateTime<?> other) { return false; }
        boolean isBefore(ChronoLocalDateTime<?> other) { return false; }
        boolean isEqual(ChronoLocalDateTime<?> other) { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public interface ChronoZonedDateTime implements Temporal, Comparable<ChronoZonedDateTime<?>>
    class ChronoZonedDateTime$<D extends ChronoLocalDate> {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoZonedDateTime<?>> timeLineOrder() { return null; }
        static ChronoZonedDateTime<?> from(TemporalAccessor temporal) { return null; }
        //override from java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }

        //override has frequency 6
        D toLocalDate() { return null; }

        //override has frequency 1
        LocalTime toLocalTime() { return null; }
        ChronoLocalDateTime<D> toLocalDateTime() { return null; }
        Chronology getChronology() { return null; }
        ZoneOffset getOffset() { return null; }
        ZoneId getZone() { return null; }
        ChronoZonedDateTime<D> withEarlierOffsetAtOverlap() { return null; }
        ChronoZonedDateTime<D> withLaterOffsetAtOverlap() { return null; }
        ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zoneId) { return null; }
        //override has frequency 1
        ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zoneId) { return null; }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField temporalField) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> with(TemporalField temporalField, long l) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> plus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> plus(long l, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> minus(TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }
        String format(DateTimeFormatter formatter) { return null; }
        Instant toInstant() { return null; }
        long toEpochSecond() { return 0L; }
        //override from java.lang.Comparable
        //frequency 6
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ChronoZonedDateTime<?> other) { return 0; }

        //frequency 6
        boolean isBefore(ChronoZonedDateTime<?> other) { return false; }

        //frequency 4
        boolean isAfter(ChronoZonedDateTime<?> other) { return false; }
        boolean isEqual(ChronoZonedDateTime<?> other) { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //override has frequency 6
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }
}
