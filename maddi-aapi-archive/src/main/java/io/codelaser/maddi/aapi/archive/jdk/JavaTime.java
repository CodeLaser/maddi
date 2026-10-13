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
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;

import java.time.*;
import java.time.chrono.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.*;
import java.time.zone.ZoneRules;
import java.util.*;
import java.util.stream.Stream;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaTime {
    public static final String PACKAGE_NAME = "java.time";
    //public enum DayOfWeek extends Enum<DayOfWeek> implements TemporalAccessor, TemporalAdjuster
    @ImmutableContainer
    class DayOfWeek$ {
        //@NotNull[O]
        static final DayOfWeek MONDAY = null;

        //@NotNull[O]
        static final DayOfWeek TUESDAY = null;

        //@NotNull[O]
        static final DayOfWeek WEDNESDAY = null;

        //@NotNull[O]
        static final DayOfWeek THURSDAY = null;

        //@NotNull[O]
        static final DayOfWeek FRIDAY = null;

        //@NotNull[O]
        static final DayOfWeek SATURDAY = null;

        //@NotNull[O]
        static final DayOfWeek SUNDAY = null;
        static DayOfWeek [] values() { return null; }
        static DayOfWeek valueOf(String name) { return null; }
        static DayOfWeek of(int dayOfWeek) { return null; }
        static DayOfWeek from(@NotNull TemporalAccessor temporal) { return null; }
        //frequency 2
        int getValue() { return 0; }

        //frequency 2
        String getDisplayName(@NotNull TextStyle style, @NotNull Locale locale) { return null; }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }
        DayOfWeek plus(long days) { return null; }
        DayOfWeek minus(long days) { return null; }
        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }
    }

    //public final class Duration implements TemporalAmount, Comparable<Duration>, Serializable
    @ImmutableContainer
    class Duration$ {
        static final Duration ZERO = null;
        static Duration ofDays(long days) { return null; }
        //frequency 1
        static Duration ofHours(long hours) { return null; }

        //frequency 1
        static Duration ofMinutes(long minutes) { return null; }
        static Duration ofSeconds(long seconds) { return null; }
        static Duration ofSeconds(long seconds, long l) { return null; }
        @Independent
        static Duration ofMillis(long millis) { return null; }
        static Duration ofNanos(long nanos) { return null; }
        //frequency 1
        @Independent
        static Duration of(long amount, @NotNull TemporalUnit temporalUnit) { return null; }
        static Duration from(@NotNull TemporalAmount amount) { return null; }
        static Duration parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text) {
            return null;
        }

        //frequency 5
        @Independent
        static Duration between(@NotNull Temporal startInclusive, @NotNull Temporal endExclusive) { return null; }

        //override from java.time.temporal.TemporalAmount
        long get(@NotNull TemporalUnit unit) { return 0L; }

        //override from java.time.temporal.TemporalAmount
        List<TemporalUnit> getUnits() { return null; }
        boolean isPositive() { return false; }
        boolean isZero() { return false; }
        boolean isNegative() { return false; }
        long getSeconds() { return 0L; }
        int getNano() { return 0; }
        Duration withSeconds(long seconds) { return null; }
        Duration withNanos(int nanoOfSecond) { return null; }
        Duration plus(@NotNull Duration duration) { return null; }
        Duration plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }
        Duration plusDays(long daysToAdd) { return null; }
        Duration plusHours(long hoursToAdd) { return null; }
        Duration plusMinutes(long minutesToAdd) { return null; }
        Duration plusSeconds(long secondsToAdd) { return null; }
        Duration plusMillis(long millisToAdd) { return null; }
        Duration plusNanos(long nanosToAdd) { return null; }
        Duration minus(@NotNull Duration duration) { return null; }
        Duration minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }
        Duration minusDays(long daysToSubtract) { return null; }
        Duration minusHours(long hoursToSubtract) { return null; }
        Duration minusMinutes(long minutesToSubtract) { return null; }
        Duration minusSeconds(long secondsToSubtract) { return null; }
        Duration minusMillis(long millisToSubtract) { return null; }
        Duration minusNanos(long nanosToSubtract) { return null; }
        Duration multipliedBy(long multiplicand) { return null; }
        Duration dividedBy(long divisor) { return null; }
        long dividedBy(@NotNull Duration divisor) { return 0L; }
        Duration negated() { return null; }
        Duration abs() { return null; }
        //override from java.time.temporal.TemporalAmount
        Temporal addTo(@NotNull Temporal temporal) { return null; }

        //override from java.time.temporal.TemporalAmount
        Temporal subtractFrom(@NotNull Temporal temporal) { return null; }
        long toDays() { return 0L; }
        long toHours() { return 0L; }
        //frequency 4
        long toMinutes() { return 0L; }
        long toSeconds() { return 0L; }
        long toMillis() { return 0L; }
        long toNanos() { return 0L; }
        long toDaysPart() { return 0L; }
        int toHoursPart() { return 0; }
        //frequency 1
        int toMinutesPart() { return 0; }
        int toSecondsPart() { return 0; }
        int toMillisPart() { return 0; }
        int toNanosPart() { return 0; }
        Duration truncatedTo(@NotNull TemporalUnit unit) { return null; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Duration otherDuration) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object other) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public final class Instant implements Temporal, TemporalAdjuster, Comparable<Instant>, Serializable
    @ImmutableContainer
    class Instant$ {
        static final Instant EPOCH = null;
        static final Instant MIN = null;
        static final Instant MAX = null;
        //frequency 11
        static Instant now() { return null; }
        static Instant now(@NotNull Clock clock) { return null; }
        static Instant ofEpochSecond(long epochSecond) { return null; }
        static Instant ofEpochSecond(long epochSecond, long l) { return null; }
        static Instant ofEpochMilli(long epochMilli) { return null; }
        static Instant from(@NotNull TemporalAccessor temporal) { return null; }
        static Instant parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text) {
            return null;
        }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }
        long getEpochSecond() { return 0L; }
        int getNano() { return 0; }
        //override from java.time.temporal.Temporal
        Instant with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        Instant with(@NotNull TemporalField field, long newValue) { return null; }
        Instant truncatedTo(@NotNull TemporalUnit unit) { return null; }
        //override from java.time.temporal.Temporal
        Instant plus(@NotNull TemporalAmount amountToAdd) { return null; }

        //override from java.time.temporal.Temporal
        //frequency 1
        Instant plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }
        Instant plusSeconds(long secondsToAdd) { return null; }
        Instant plusMillis(long millisToAdd) { return null; }
        Instant plusNanos(long nanosToAdd) { return null; }
        //override from java.time.temporal.Temporal
        Instant minus(@NotNull TemporalAmount amountToSubtract) { return null; }

        //override from java.time.temporal.Temporal
        //frequency 6
        Instant minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }

        //frequency 1
        Instant minusSeconds(long secondsToSubtract) { return null; }
        Instant minusMillis(long millisToSubtract) { return null; }
        Instant minusNanos(long nanosToSubtract) { return null; }
        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(@NotNull Temporal endExclusive, @NotNull TemporalUnit unit) { return 0L; }
        Duration until(@NotNull Instant endExclusive) { return null; }
        OffsetDateTime atOffset(@NotNull ZoneOffset offset) { return null; }
        ZonedDateTime atZone(@NotNull ZoneId zone) { return null; }
        long toEpochMilli() { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Instant otherInstant) { return 0; }

        //frequency 1
        boolean isAfter(@NotNull Instant otherInstant) { return false; }
        boolean isBefore(@NotNull Instant otherInstant) { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object other) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.Instant plusSaturating(@NotNull java.time.Duration p0) { return null; }
    }

    //public final class LocalTime implements Temporal, TemporalAdjuster, Comparable<LocalTime>, Serializable
    // Not hinted before 2026-10-10: an unannotated library parameter reads as modified, so 'LocalTime.now(clock)'
    // modified its Clock (langchain4j PromptTemplate.injectDateTimeVariables, CodeLaser/maddi-mod#24 R5).
    @ImmutableContainer
    class LocalTime$ {
        static final LocalTime MIN = null;
        static final LocalTime MAX = null;
        static final LocalTime MIDNIGHT = null;
        static final LocalTime NOON = null;
        static LocalTime now() { return null; }
        static LocalTime now(@NotNull ZoneId zone) { return null; }
        static LocalTime now(@NotNull Clock clock) { return null; }
        static LocalTime of(int hour, int minute) { return null; }
        static LocalTime of(int hour, int minute, int second) { return null; }
        static LocalTime of(int hour, int minute, int second, int nanoOfSecond) { return null; }
        static LocalTime ofSecondOfDay(long secondOfDay) { return null; }
        static LocalTime ofNanoOfDay(long nanoOfDay) { return null; }
        static LocalTime parse(@NotNull CharSequence text) { return null; }
        int getHour() { return 0; }
        int getMinute() { return 0; }
        int getSecond() { return 0; }
        int getNano() { return 0; }
        int toSecondOfDay() { return 0; }
        long toNanoOfDay() { return 0L; }
        boolean isAfter(@NotNull LocalTime other) { return false; }
        boolean isBefore(@NotNull LocalTime other) { return false; }
        public String toString() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        java.time.LocalDateTime atDate(@NotNull java.time.LocalDate p0) { return null; }
        java.time.OffsetTime atOffset(@NotNull java.time.ZoneOffset p0) { return null; }
        int compareTo(@NotNull java.time.LocalTime p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.LocalTime from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalUnit p0) { return false; }
        java.time.LocalTime minus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.LocalTime minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.LocalTime ofInstant(@NotNull java.time.Instant p0, @NotNull java.time.ZoneId p1) { return null; }
        static java.time.LocalTime parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        java.time.LocalTime plus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.LocalTime plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        long toEpochSecond(@NotNull java.time.LocalDate p0, @NotNull java.time.ZoneOffset p1) { return 0; }
        java.time.LocalTime truncatedTo(@NotNull java.time.temporal.TemporalUnit p0) { return null; }
        long until(@NotNull java.time.temporal.Temporal p0, @NotNull java.time.temporal.TemporalUnit p1) { return 0; }
        java.time.LocalTime with(@NotNull java.time.temporal.TemporalAdjuster p0) { return null; }
        java.time.LocalTime with(@NotNull java.time.temporal.TemporalField p0, long p1) { return null; }
    }

    //public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable
    @ImmutableContainer
    class LocalDate$ {
        static final LocalDate MIN = null;
        static final LocalDate MAX = null;
        static final LocalDate EPOCH = null;
        static LocalDate now() { return null; }
        static LocalDate now(@NotNull ZoneId zone) { return null; }
        static LocalDate now(@NotNull Clock clock) { return null; }
        static LocalDate of(int year, @NotNull Month month, int dayOfMonth) { return null; }
        static LocalDate of(int year, int month, int dayOfMonth) { return null; }
        static LocalDate ofYearDay(int year, int dayOfYear) { return null; }
        static LocalDate ofInstant(@NotNull Instant instant, @NotNull ZoneId zone) { return null; }
        static LocalDate ofEpochDay(long epochDay) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        static LocalDate from(@NotNull TemporalAccessor temporal) { return null; }

        static LocalDate parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text) {
            return null;
        }

        static LocalDate parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text,
            @NotNull DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }

        //override from java.time.chrono.ChronoLocalDate
        IsoChronology getChronology() { return null; }

        //override from java.time.chrono.ChronoLocalDate
        IsoEra getEra() { return null; }
        int getYear() { return 0; }
        int getMonthValue() { return 0; }
        Month getMonth() { return null; }
        int getDayOfMonth() { return 0; }
        int getDayOfYear() { return 0; }
        DayOfWeek getDayOfWeek() { return null; }
        //override from java.time.chrono.ChronoLocalDate
        boolean isLeapYear() { return false; }

        //override from java.time.chrono.ChronoLocalDate
        int lengthOfMonth() { return 0; }

        //override from java.time.chrono.ChronoLocalDate
        int lengthOfYear() { return 0; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate with(@NotNull TemporalField field, long newValue) { return null; }
        LocalDate withYear(int year) { return null; }
        LocalDate withMonth(int month) { return null; }
        LocalDate withDayOfMonth(int dayOfMonth) { return null; }
        LocalDate withDayOfYear(int dayOfYear) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate plus(@NotNull TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }
        LocalDate plusYears(long yearsToAdd) { return null; }
        LocalDate plusMonths(long monthsToAdd) { return null; }
        LocalDate plusWeeks(long weeksToAdd) { return null; }
        LocalDate plusDays(long daysToAdd) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate minus(@NotNull TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }
        LocalDate minusYears(long yearsToSubtract) { return null; }
        LocalDate minusMonths(long monthsToSubtract) { return null; }
        LocalDate minusWeeks(long weeksToSubtract) { return null; }
        LocalDate minusDays(long daysToSubtract) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        long until(@NotNull Temporal endExclusive, @NotNull TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoLocalDate
        Period until(@NotNull ChronoLocalDate endDateExclusive) { return null; }
        Stream<LocalDate> datesUntil(@NotNull LocalDate endExclusive) { return null; }
        Stream<LocalDate> datesUntil(@NotNull LocalDate endExclusive, @NotNull Period step) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        String format(@NotNull DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDate
        LocalDateTime atTime(@NotNull LocalTime time) { return null; }
        LocalDateTime atTime(int hour, int minute) { return null; }
        LocalDateTime atTime(int hour, int minute, int second) { return null; }
        LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) { return null; }
        OffsetDateTime atTime(@NotNull OffsetTime time) { return null; }
        LocalDateTime atStartOfDay() { return null; }
        ZonedDateTime atStartOfDay(@NotNull ZoneId zone) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        long toEpochDay() { return 0L; }
        long toEpochSecond(@NotNull LocalTime time, @NotNull ZoneOffset offset) { return 0L; }
        //override from java.lang.Comparable, java.time.chrono.ChronoLocalDate
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ChronoLocalDate other) { return 0; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isAfter(@NotNull ChronoLocalDate other) { return false; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isBefore(@NotNull ChronoLocalDate other) { return false; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isEqual(@NotNull ChronoLocalDate other) { return false; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDate
        //frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDate
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDate
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public final class LocalDateTime implements Temporal, TemporalAdjuster, ChronoLocalDateTime<LocalDate>, Serializable
    @ImmutableContainer
    class LocalDateTime$ {
        static final LocalDateTime MIN = null;
        static final LocalDateTime MAX = null;
        //frequency 1
        static LocalDateTime now() { return null; }
        static LocalDateTime now(@NotNull ZoneId zone) { return null; }
        static LocalDateTime now(@NotNull Clock clock) { return null; }
        static LocalDateTime of(int year, @NotNull Month month, int dayOfMonth, int hour, int minute) { return null; }
        static LocalDateTime of(int year, @NotNull Month month, int dayOfMonth, int hour, int minute, int second) { return null; }

        static LocalDateTime of(
            int year,
            @NotNull Month month,
            int dayOfMonth,
            int hour,
            int minute,
            int second,
            int nanoOfSecond) { return null; }
        static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) { return null; }
        static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second) { return null; }
        static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) {
            return null;
        }
        static LocalDateTime of(@NotNull LocalDate date, @NotNull LocalTime time) { return null; }
        static LocalDateTime ofInstant(@NotNull Instant instant, @NotNull ZoneId zone) { return null; }
        static LocalDateTime ofEpochSecond(long epochSecond, int i, @NotNull ZoneOffset nanoOfSecond) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime
        static LocalDateTime from(@NotNull TemporalAccessor temporal) { return null; }

        static LocalDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text) { return null; }

        static LocalDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text,
            @NotNull DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }

        //override from java.time.chrono.ChronoLocalDateTime
        LocalDate toLocalDate() { return null; }
        int getYear() { return 0; }
        int getMonthValue() { return 0; }
        Month getMonth() { return null; }
        int getDayOfMonth() { return 0; }
        int getDayOfYear() { return 0; }
        DayOfWeek getDayOfWeek() { return null; }
        //override from java.time.chrono.ChronoLocalDateTime
        LocalTime toLocalTime() { return null; }
        int getHour() { return 0; }
        int getMinute() { return 0; }
        int getSecond() { return 0; }
        int getNano() { return 0; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime with(@NotNull TemporalField field, long newValue) { return null; }
        LocalDateTime withYear(int year) { return null; }
        LocalDateTime withMonth(int month) { return null; }
        LocalDateTime withDayOfMonth(int dayOfMonth) { return null; }
        LocalDateTime withDayOfYear(int dayOfYear) { return null; }
        LocalDateTime withHour(int hour) { return null; }
        LocalDateTime withMinute(int minute) { return null; }
        LocalDateTime withSecond(int second) { return null; }
        LocalDateTime withNano(int nanoOfSecond) { return null; }
        LocalDateTime truncatedTo(@NotNull TemporalUnit unit) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime plus(@NotNull TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }
        LocalDateTime plusYears(long years) { return null; }
        LocalDateTime plusMonths(long months) { return null; }
        LocalDateTime plusWeeks(long weeks) { return null; }
        LocalDateTime plusDays(long days) { return null; }
        LocalDateTime plusHours(long hours) { return null; }
        LocalDateTime plusMinutes(long minutes) { return null; }
        LocalDateTime plusSeconds(long seconds) { return null; }
        LocalDateTime plusNanos(long nanos) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime minus(@NotNull TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }
        LocalDateTime minusYears(long years) { return null; }
        LocalDateTime minusMonths(long months) { return null; }
        LocalDateTime minusWeeks(long weeks) { return null; }
        LocalDateTime minusDays(long days) { return null; }
        LocalDateTime minusHours(long hours) { return null; }
        LocalDateTime minusMinutes(long minutes) { return null; }
        LocalDateTime minusSeconds(long seconds) { return null; }
        LocalDateTime minusNanos(long nanos) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.TemporalAdjuster
        Temporal adjustInto(@NotNull Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(@NotNull Temporal endExclusive, @NotNull TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoLocalDateTime
        String format(@NotNull DateTimeFormatter formatter) { return null; }
        OffsetDateTime atOffset(@NotNull ZoneOffset offset) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime
        //frequency 1
        ZonedDateTime atZone(@NotNull ZoneId zone) { return null; }

        //override from java.lang.Comparable, java.time.chrono.ChronoLocalDateTime
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull ChronoLocalDateTime<?> other) { return 0; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isAfter(@NotNull ChronoLocalDateTime<?> other) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isBefore(@NotNull ChronoLocalDateTime<?> other) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isEqual(@NotNull ChronoLocalDateTime<?> other) { return false; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDateTime
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDateTime
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object, java.time.chrono.ChronoLocalDateTime
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public abstract class ZoneId implements Serializable
    // abstract -> extensible, so this lands as @Immutable(hc=true)
    @ImmutableContainer
    class ZoneId$ {
        static final Map<String, String> SHORT_IDS = null;
        //frequency 2
        static ZoneId systemDefault() { return null; }

        //@Independent[T]
        static Set<String> getAvailableZoneIds() { return null; }
        static ZoneId of(@NotNull String zoneId, @NotNull Map<String, String> aliasMap) { return null; }
        //frequency 2
        static ZoneId of(@NotNull String zoneId) { return null; }
        static ZoneId ofOffset(@NotNull String prefix, @NotNull ZoneOffset offset) { return null; }
        static ZoneId from(@NotNull TemporalAccessor temporal) { return null; }
        String getId() { return null; }
        String getDisplayName(@NotNull TextStyle style, @NotNull Locale locale) { return null; }
        ZoneRules getRules() { return null; }
        ZoneId normalized() { return null; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public final class ZonedDateTime implements Temporal, ChronoZonedDateTime<LocalDate>, Serializable
    @ImmutableContainer
    class ZonedDateTime$ {
        //frequency 112
        static ZonedDateTime now() { return null; }

        //frequency 1
        static ZonedDateTime now(@NotNull ZoneId zone) { return null; }
        static ZonedDateTime now(@NotNull Clock clock) { return null; }
        static ZonedDateTime of(@NotNull LocalDate date, @NotNull LocalTime time, @NotNull ZoneId zone) { return null; }
        static ZonedDateTime of(@NotNull LocalDateTime localDateTime, @NotNull ZoneId zone) { return null; }
        //frequency 3
        static ZonedDateTime of(
            int year,
            int month,
            int dayOfMonth,
            int hour,
            int minute,
            int second,
            int nanoOfSecond,
            @NotNull ZoneId zone) { return null; }

        static ZonedDateTime ofLocal(@NotNull LocalDateTime localDateTime, @NotNull ZoneId zone, @Nullable ZoneOffset preferredOffset) { return null; }
        static ZonedDateTime ofInstant(@NotNull Instant instant, @NotNull ZoneId zone) { return null; }
        static ZonedDateTime ofInstant(@NotNull LocalDateTime localDateTime, @NotNull ZoneOffset offset, @NotNull ZoneId zone) { return null; }
        static ZonedDateTime ofStrict(@NotNull LocalDateTime localDateTime, @NotNull ZoneOffset offset, @NotNull ZoneId zone) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime
        static ZonedDateTime from(@NotNull TemporalAccessor temporal) { return null; }

        //frequency 4
        static ZonedDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text) { return null; }

        static ZonedDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence text,
            @NotNull DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        boolean isSupported(@NotNull TemporalField field) { return false; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        boolean isSupported(@NotNull TemporalUnit unit) { return false; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        ValueRange range(@NotNull TemporalField field) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        int get(@NotNull TemporalField field) { return 0; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        long getLong(@NotNull TemporalField field) { return 0L; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZoneOffset getOffset() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withEarlierOffsetAtOverlap() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withLaterOffsetAtOverlap() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZoneId getZone() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withZoneSameLocal(@NotNull ZoneId zone) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        //frequency 1
        ZonedDateTime withZoneSameInstant(@NotNull ZoneId zone) { return null; }
        ZonedDateTime withFixedOffsetZone() { return null; }
        //override from java.time.chrono.ChronoZonedDateTime
        LocalDateTime toLocalDateTime() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        //frequency 6
        LocalDate toLocalDate() { return null; }
        int getYear() { return 0; }
        int getMonthValue() { return 0; }
        Month getMonth() { return null; }
        int getDayOfMonth() { return 0; }
        int getDayOfYear() { return 0; }
        //frequency 5
        DayOfWeek getDayOfWeek() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        //frequency 1
        LocalTime toLocalTime() { return null; }
        int getHour() { return 0; }
        int getMinute() { return 0; }
        int getSecond() { return 0; }
        int getNano() { return 0; }
        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime with(@NotNull TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime with(@NotNull TemporalField field, long newValue) { return null; }
        ZonedDateTime withYear(int year) { return null; }
        ZonedDateTime withMonth(int month) { return null; }
        ZonedDateTime withDayOfMonth(int dayOfMonth) { return null; }
        ZonedDateTime withDayOfYear(int dayOfYear) { return null; }
        //frequency 6
        ZonedDateTime withHour(int hour) { return null; }

        //frequency 5
        ZonedDateTime withMinute(int minute) { return null; }

        //frequency 2
        ZonedDateTime withSecond(int second) { return null; }
        ZonedDateTime withNano(int nanoOfSecond) { return null; }
        ZonedDateTime truncatedTo(@NotNull TemporalUnit unit) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime plus(@NotNull TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime plus(long amountToAdd, @NotNull TemporalUnit temporalUnit) { return null; }
        ZonedDateTime plusYears(long years) { return null; }
        ZonedDateTime plusMonths(long months) { return null; }
        ZonedDateTime plusWeeks(long weeks) { return null; }
        //frequency 16
        ZonedDateTime plusDays(long days) { return null; }

        //frequency 1
        ZonedDateTime plusHours(long hours) { return null; }
        ZonedDateTime plusMinutes(long minutes) { return null; }
        ZonedDateTime plusSeconds(long seconds) { return null; }
        ZonedDateTime plusNanos(long nanos) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime minus(@NotNull TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }
        ZonedDateTime minusYears(long years) { return null; }
        ZonedDateTime minusMonths(long months) { return null; }
        ZonedDateTime minusWeeks(long weeks) { return null; }
        //frequency 2
        ZonedDateTime minusDays(long days) { return null; }
        ZonedDateTime minusHours(long hours) { return null; }
        ZonedDateTime minusMinutes(long minutes) { return null; }
        ZonedDateTime minusSeconds(long seconds) { return null; }
        ZonedDateTime minusNanos(long nanos) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.Temporal
        long until(@NotNull Temporal endExclusive, @NotNull TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoZonedDateTime
        String format(@NotNull DateTimeFormatter formatter) { return null; }
        OffsetDateTime toOffsetDateTime() { return null; }
        //override from java.lang.Object, java.time.chrono.ChronoZonedDateTime
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object, java.time.chrono.ChronoZonedDateTime
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object, java.time.chrono.ChronoZonedDateTime
        //frequency 6
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public java.time.Clock (shadow created for its nullability, generated)
    class Clock$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.time.Clock fixed(@NotNull java.time.Instant p0, @NotNull java.time.ZoneId p1) { return null; }
        static java.time.Clock offset(@NotNull java.time.Clock p0, @NotNull java.time.Duration p1) { return null; }
        static java.time.Clock system(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.Clock tick(@NotNull java.time.Clock p0, @NotNull java.time.Duration p1) { return null; }
        static java.time.Clock tickMillis(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.Clock tickMinutes(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.Clock tickSeconds(@NotNull java.time.ZoneId p0) { return null; }
        java.time.Clock withZone(@NotNull java.time.ZoneId p0) { return null; }
    }

    //public java.time.DateTimeException (shadow created for its nullability, generated)
    class DateTimeException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DateTimeException$(@Nullable java.lang.String p0) { }
        DateTimeException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
    }

    //public java.time.InstantSource (shadow created for its nullability, generated)
    class InstantSource$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.time.InstantSource fixed(@NotNull java.time.Instant p0) { return null; }
        static java.time.InstantSource offset(@NotNull java.time.InstantSource p0, @NotNull java.time.Duration p1) { return null; }
        static java.time.InstantSource tick(@NotNull java.time.InstantSource p0, @NotNull java.time.Duration p1) { return null; }
        java.time.Clock withZone(@NotNull java.time.ZoneId p0) { return null; }
    }

    //public java.time.Month (shadow created for its nullability, generated)
    class Month$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        static java.time.Month from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        java.lang.String getDisplayName(@NotNull java.time.format.TextStyle p0, @NotNull java.util.Locale p1) { return null; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
    }

    //public java.time.MonthDay (shadow created for its nullability, generated)
    class MonthDay$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        int compareTo(@NotNull java.time.MonthDay p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.MonthDay from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isAfter(@NotNull java.time.MonthDay p0) { return false; }
        boolean isBefore(@NotNull java.time.MonthDay p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        static java.time.MonthDay now(@NotNull java.time.Clock p0) { return null; }
        static java.time.MonthDay now(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.MonthDay of(@NotNull java.time.Month p0, int p1) { return null; }
        static java.time.MonthDay parse(@NotNull java.lang.CharSequence p0) { return null; }
        static java.time.MonthDay parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        java.time.MonthDay with(@NotNull java.time.Month p0) { return null; }
    }

    //public java.time.OffsetDateTime (shadow created for its nullability, generated)
    class OffsetDateTime$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        java.time.ZonedDateTime atZoneSameInstant(@NotNull java.time.ZoneId p0) { return null; }
        java.time.ZonedDateTime atZoneSimilarLocal(@NotNull java.time.ZoneId p0) { return null; }
        int compareTo(@NotNull java.time.OffsetDateTime p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.OffsetDateTime from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isAfter(@NotNull java.time.OffsetDateTime p0) { return false; }
        boolean isBefore(@NotNull java.time.OffsetDateTime p0) { return false; }
        boolean isEqual(@NotNull java.time.OffsetDateTime p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalUnit p0) { return false; }
        java.time.OffsetDateTime minus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.OffsetDateTime minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.OffsetDateTime now(@NotNull java.time.Clock p0) { return null; }
        static java.time.OffsetDateTime now(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.OffsetDateTime of(int p0, int p1, int p2, int p3, int p4, int p5, int p6, @NotNull java.time.ZoneOffset p7) { return null; }
        static java.time.OffsetDateTime of(@NotNull java.time.LocalDate p0, @NotNull java.time.LocalTime p1, @NotNull java.time.ZoneOffset p2) { return null; }
        static java.time.OffsetDateTime of(@NotNull java.time.LocalDateTime p0, @NotNull java.time.ZoneOffset p1) { return null; }
        static java.time.OffsetDateTime ofInstant(@NotNull java.time.Instant p0, @NotNull java.time.ZoneId p1) { return null; }
        static java.time.OffsetDateTime parse(@NotNull java.lang.CharSequence p0) { return null; }
        static java.time.OffsetDateTime parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        java.time.OffsetDateTime plus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.OffsetDateTime plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        java.time.OffsetDateTime truncatedTo(@NotNull java.time.temporal.TemporalUnit p0) { return null; }
        long until(@NotNull java.time.temporal.Temporal p0, @NotNull java.time.temporal.TemporalUnit p1) { return 0; }
        java.time.OffsetDateTime with(@NotNull java.time.temporal.TemporalAdjuster p0) { return null; }
        java.time.OffsetDateTime with(@NotNull java.time.temporal.TemporalField p0, long p1) { return null; }
        java.time.OffsetDateTime withOffsetSameInstant(@NotNull java.time.ZoneOffset p0) { return null; }
        java.time.OffsetDateTime withOffsetSameLocal(@NotNull java.time.ZoneOffset p0) { return null; }
    }

    //public java.time.OffsetTime (shadow created for its nullability, generated)
    class OffsetTime$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        java.time.OffsetDateTime atDate(@NotNull java.time.LocalDate p0) { return null; }
        int compareTo(@NotNull java.time.OffsetTime p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.OffsetTime from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isAfter(@NotNull java.time.OffsetTime p0) { return false; }
        boolean isBefore(@NotNull java.time.OffsetTime p0) { return false; }
        boolean isEqual(@NotNull java.time.OffsetTime p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalUnit p0) { return false; }
        java.time.OffsetTime minus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.OffsetTime minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.OffsetTime now(@NotNull java.time.Clock p0) { return null; }
        static java.time.OffsetTime now(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.OffsetTime of(int p0, int p1, int p2, int p3, @NotNull java.time.ZoneOffset p4) { return null; }
        static java.time.OffsetTime of(@NotNull java.time.LocalTime p0, @NotNull java.time.ZoneOffset p1) { return null; }
        static java.time.OffsetTime ofInstant(@NotNull java.time.Instant p0, @NotNull java.time.ZoneId p1) { return null; }
        static java.time.OffsetTime parse(@NotNull java.lang.CharSequence p0) { return null; }
        static java.time.OffsetTime parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        java.time.OffsetTime plus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.OffsetTime plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        long toEpochSecond(@NotNull java.time.LocalDate p0) { return 0; }
        java.time.OffsetTime truncatedTo(@NotNull java.time.temporal.TemporalUnit p0) { return null; }
        long until(@NotNull java.time.temporal.Temporal p0, @NotNull java.time.temporal.TemporalUnit p1) { return 0; }
        java.time.OffsetTime with(@NotNull java.time.temporal.TemporalAdjuster p0) { return null; }
        java.time.OffsetTime with(@NotNull java.time.temporal.TemporalField p0, long p1) { return null; }
        java.time.OffsetTime withOffsetSameInstant(@NotNull java.time.ZoneOffset p0) { return null; }
        java.time.OffsetTime withOffsetSameLocal(@NotNull java.time.ZoneOffset p0) { return null; }
    }

    //public java.time.Period (shadow created for its nullability, generated)
    class Period$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal addTo(@NotNull java.time.temporal.Temporal p0) { return null; }
        static java.time.Period between(@NotNull java.time.LocalDate p0, @NotNull java.time.LocalDate p1) { return null; }
        static java.time.Period from(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        long get(@NotNull java.time.temporal.TemporalUnit p0) { return 0; }
        java.time.Period minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.Period parse(@NotNull java.lang.CharSequence p0) { return null; }
        java.time.Period plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        java.time.temporal.Temporal subtractFrom(@NotNull java.time.temporal.Temporal p0) { return null; }
    }

    //public java.time.Year (shadow created for its nullability, generated)
    class Year$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        java.time.YearMonth atMonth(@NotNull java.time.Month p0) { return null; }
        java.time.LocalDate atMonthDay(@NotNull java.time.MonthDay p0) { return null; }
        int compareTo(@NotNull java.time.Year p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.Year from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isAfter(@NotNull java.time.Year p0) { return false; }
        boolean isBefore(@NotNull java.time.Year p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalUnit p0) { return false; }
        boolean isValidMonthDay(@NotNull java.time.MonthDay p0) { return false; }
        java.time.Year minus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.Year minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.Year now(@NotNull java.time.Clock p0) { return null; }
        static java.time.Year now(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.Year parse(@NotNull java.lang.CharSequence p0) { return null; }
        static java.time.Year parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        java.time.Year plus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.Year plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        long until(@NotNull java.time.temporal.Temporal p0, @NotNull java.time.temporal.TemporalUnit p1) { return 0; }
        java.time.Year with(@NotNull java.time.temporal.TemporalAdjuster p0) { return null; }
        java.time.Year with(@NotNull java.time.temporal.TemporalField p0, long p1) { return null; }
    }

    //public java.time.YearMonth (shadow created for its nullability, generated)
    class YearMonth$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        int compareTo(@NotNull java.time.YearMonth p0) { return 0; }
        java.lang.String format(@NotNull java.time.format.DateTimeFormatter p0) { return null; }
        static java.time.YearMonth from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isAfter(@NotNull java.time.YearMonth p0) { return false; }
        boolean isBefore(@NotNull java.time.YearMonth p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        boolean isSupported(@NotNull java.time.temporal.TemporalUnit p0) { return false; }
        java.time.YearMonth minus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.YearMonth minus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        static java.time.YearMonth now(@NotNull java.time.Clock p0) { return null; }
        static java.time.YearMonth now(@NotNull java.time.ZoneId p0) { return null; }
        static java.time.YearMonth of(int p0, @NotNull java.time.Month p1) { return null; }
        static java.time.YearMonth parse(@NotNull java.lang.CharSequence p0) { return null; }
        static java.time.YearMonth parse(@NotNull java.lang.CharSequence p0, @NotNull java.time.format.DateTimeFormatter p1) { return null; }
        java.time.YearMonth plus(long p0, @NotNull java.time.temporal.TemporalUnit p1) { return null; }
        java.time.YearMonth plus(@NotNull java.time.temporal.TemporalAmount p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
        long until(@NotNull java.time.temporal.Temporal p0, @NotNull java.time.temporal.TemporalUnit p1) { return 0; }
        java.time.YearMonth with(@NotNull java.time.temporal.TemporalAdjuster p0) { return null; }
        java.time.YearMonth with(@NotNull java.time.temporal.TemporalField p0, long p1) { return null; }
    }

    //public java.time.ZoneOffset (shadow created for its nullability, generated)
    class ZoneOffset$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.time.temporal.Temporal adjustInto(@NotNull java.time.temporal.Temporal p0) { return null; }
        int compareTo(@NotNull java.time.ZoneOffset p0) { return 0; }
        static java.time.ZoneOffset from(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        int get(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        long getLong(@NotNull java.time.temporal.TemporalField p0) { return 0; }
        boolean isSupported(@NotNull java.time.temporal.TemporalField p0) { return false; }
        static java.time.ZoneOffset of(@NotNull java.lang.String p0) { return null; }
        <R> R query(@NotNull java.time.temporal.TemporalQuery<R> p0) { return null; }
        java.time.temporal.ValueRange range(@NotNull java.time.temporal.TemporalField p0) { return null; }
    }
}
