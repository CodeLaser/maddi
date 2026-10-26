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
import java.time.*;
import java.time.chrono.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.Comparator;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaTimeChrono {
    public static final String PACKAGE_NAME = "java.time.chrono";
    //public interface ChronoLocalDate implements Temporal, TemporalAdjuster, Comparable<ChronoLocalDate>
    @ImmutableContainer
    class ChronoLocalDate$ {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoLocalDate> timeLineOrder() { return null; }
        static ChronoLocalDate from(@NotNull TemporalAccessor temporal) { return null; }
        Chronology getChronology() { return null; }
        Era getEra() { return null; }
        boolean isLeapYear() { return false; }
        int lengthOfMonth() { return 0; }
        int lengthOfYear() { return 0; }
        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate with(@NotNull TemporalField field, long newValue) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate plus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate minus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDate minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(@NotNull Temporal temporal, @NotNull TemporalUnit temporalUnit) { return 0L; }
        ChronoPeriod until(@NotNull ChronoLocalDate chronoLocalDate) { return null; }
        String format(@NotNull DateTimeFormatter formatter) { return null; }
        ChronoLocalDateTime<?> atTime(@NotNull LocalTime localTime) { return null; }
        long toEpochDay() { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ChronoLocalDate other) { return 0; }
        boolean isAfter(@NotNull ChronoLocalDate other) { return false; }
        boolean isBefore(@NotNull ChronoLocalDate other) { return false; }
        boolean isEqual(@NotNull ChronoLocalDate other) { return false; }
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
    @ImmutableContainer(hc = true)
    class ChronoLocalDateTime$<D extends ChronoLocalDate> {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoLocalDateTime<?>> timeLineOrder() { return null; }
        static ChronoLocalDateTime<?> from(@NotNull TemporalAccessor temporal) { return null; }
        Chronology getChronology() { return null; }
        D toLocalDate() { return null; }
        LocalTime toLocalTime() { return null; }
        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField temporalField) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> with(@NotNull TemporalField temporalField, long l) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> plus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> plus(long l, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> minus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoLocalDateTime<D> minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }
        String format(@NotNull DateTimeFormatter formatter) { return null; }
        //override has frequency 1
        ChronoZonedDateTime<D> atZone(@NotNull ZoneId zoneId) { return null; }
        Instant toInstant(@NotNull ZoneOffset offset) { return null; }
        long toEpochSecond(@NotNull ZoneOffset offset) { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ChronoLocalDateTime<?> other) { return 0; }
        boolean isAfter(@NotNull ChronoLocalDateTime<?> other) { return false; }
        boolean isBefore(@NotNull ChronoLocalDateTime<?> other) { return false; }
        boolean isEqual(@NotNull ChronoLocalDateTime<?> other) { return false; }
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
    @ImmutableContainer(hc = true)
    class ChronoZonedDateTime$<D extends ChronoLocalDate> {
        //@Immutable(hc=true)[T] @Independent[T]
        static Comparator<ChronoZonedDateTime<?>> timeLineOrder() { return null; }
        static ChronoZonedDateTime<?> from(@NotNull TemporalAccessor temporal) { return null; }
        //override from java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }

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
        ChronoZonedDateTime<D> withZoneSameLocal(@NotNull ZoneId zoneId) { return null; }
        //override has frequency 1
        ChronoZonedDateTime<D> withZoneSameInstant(@NotNull ZoneId zoneId) { return null; }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField temporalField) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> with(@NotNull TemporalField temporalField, long l) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> plus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> plus(long l, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> minus(@NotNull TemporalAmount amount) { return null; }

        //override from java.time.temporal.Temporal
        ChronoZonedDateTime<D> minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }

        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }
        String format(@NotNull DateTimeFormatter formatter) { return null; }
        Instant toInstant() { return null; }
        long toEpochSecond() { return 0L; }
        //override from java.lang.Comparable
        //frequency 6
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ChronoZonedDateTime<?> other) { return 0; }

        //frequency 6
        boolean isBefore(@NotNull ChronoZonedDateTime<?> other) { return false; }

        //frequency 4
        boolean isAfter(@NotNull ChronoZonedDateTime<?> other) { return false; }
        boolean isEqual(@NotNull ChronoZonedDateTime<?> other) { return false; }
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

    //public final class HijrahDate extends ChronoLocalDateImpl<HijrahDate> implements ChronoLocalDate, Serializable
    // value-based, like LocalDate; the hints of the ChronoLocalDate interface do not reach an implementation
    @ImmutableContainer
    class HijrahDate$ { }

    //public final class JapaneseDate extends ChronoLocalDateImpl<JapaneseDate> implements ChronoLocalDate, Serializable
    // value-based, like LocalDate; the hints of the ChronoLocalDate interface do not reach an implementation
    @ImmutableContainer
    class JapaneseDate$ { }

    //public final class MinguoDate extends ChronoLocalDateImpl<MinguoDate> implements ChronoLocalDate, Serializable
    // value-based, like LocalDate; the hints of the ChronoLocalDate interface do not reach an implementation
    @ImmutableContainer
    class MinguoDate$ { }

    //public final class ThaiBuddhistDate extends ChronoLocalDateImpl<ThaiBuddhistDate> implements ChronoLocalDate, Serializable
    // value-based, like LocalDate; the hints of the ChronoLocalDate interface do not reach an implementation
    @ImmutableContainer
    class ThaiBuddhistDate$ { }

    //public java.time.chrono.AbstractChronology (shadow created for its nullability, generated)
    class AbstractChronology$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int compareTo(@NotNull java.time.chrono.Chronology p0) { return 0; }
        @Nullable java.time.chrono.ChronoLocalDate resolveDate(@NotNull java.util.Map<java.time.temporal.TemporalField, java.lang.Long> p0, @NotNull java.time.format.ResolverStyle p1) { return null; }
    }

    //public java.time.chrono.ChronoPeriod (shadow created for its nullability, generated)
    class ChronoPeriod$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal addTo(@NotNull java.time.temporal.Temporal p0) { return null; }
        static java.time.chrono.ChronoPeriod between(@NotNull java.time.chrono.ChronoLocalDate p0, @NotNull java.time.chrono.ChronoLocalDate p1) { return null; }
        long get(@NotNull java.time.temporal.TemporalUnit p0) { return 0; }
        java.time.chrono.ChronoPeriod minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        java.time.chrono.ChronoPeriod plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        java.time.temporal.Temporal subtractFrom(@NotNull java.time.temporal.Temporal p0) { return null; }
    }

    //public java.time.chrono.Chronology (shadow created for its nullability, generated)
    class Chronology$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int compareTo(@NotNull java.time.chrono.Chronology p0) { return 0; }
        java.time.chrono.ChronoLocalDate date(@NotNull java.time.chrono.Era p0, int p1, int p2, int p3) { return null; }
        java.time.chrono.ChronoLocalDate date(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        java.time.chrono.ChronoLocalDate dateNow(@NotNull java.time.Clock p0) { return null; }
        java.time.chrono.ChronoLocalDate dateNow(@NotNull java.time.ZoneId p0) { return null; }
        java.time.chrono.ChronoLocalDate dateYearDay(@NotNull java.time.chrono.Era p0, int p1, int p2) { return null; }
        long epochSecond(@NotNull java.time.chrono.Era p0, int p1, int p2, int p3, int p4, int p5, int p6, @NotNull java.time.ZoneOffset p7) { return 0; }
        long epochSecond(int p0, int p1, int p2, int p3, int p4, int p5, @NotNull java.time.ZoneOffset p6) { return 0; }
        static java.time.chrono.Chronology from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        @Nullable java.lang.String getCalendarType() { return null; }
        java.lang.String getDisplayName(@NotNull java.time.format.TextStyle p0, @NotNull java.util.Locale p1) { return null; }
        java.time.chrono.ChronoLocalDateTime<? extends java.time.chrono.ChronoLocalDate> localDateTime(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        static java.time.chrono.Chronology of(@NotNull java.lang.String p0) { return null; }
        static java.time.chrono.Chronology ofLocale(@NotNull java.util.Locale p0) { return null; }
        int prolepticYear(@NotNull java.time.chrono.Era p0, int p1) { return 0; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.ChronoField p0) { return null; }
        @Nullable java.time.chrono.ChronoLocalDate resolveDate(@NotNull java.util.Map<java.time.temporal.TemporalField, java.lang.Long> p0, @NotNull java.time.format.ResolverStyle p1) { return null; }
        java.time.chrono.ChronoZonedDateTime<? extends java.time.chrono.ChronoLocalDate> zonedDateTime(@NotNull java.time.Instant p0, @NotNull java.time.ZoneId p1) { return null; }
        java.time.chrono.ChronoZonedDateTime<? extends java.time.chrono.ChronoLocalDate> zonedDateTime(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
    }
}
