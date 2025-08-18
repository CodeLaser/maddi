package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;

import java.time.*;
import java.time.chrono.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.*;
import java.time.zone.ZoneRules;
import java.util.*;
import java.util.stream.Stream;

public class JavaTime {
    public static final String PACKAGE_NAME = "java.time";
    //public enum DayOfWeek extends Enum<DayOfWeek> implements TemporalAccessor, TemporalAdjuster
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
        static DayOfWeek from(TemporalAccessor temporal) { return null; }
        //frequency 2
        int getValue() { return 0; }

        //frequency 2
        String getDisplayName(TextStyle style, Locale locale) { return null; }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }
        DayOfWeek plus(long days) { return null; }
        DayOfWeek minus(long days) { return null; }
        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }
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
        static Duration of(long amount, TemporalUnit temporalUnit) { return null; }
        static Duration from(TemporalAmount amount) { return null; }
        static Duration parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text) {
            return null;
        }

        //frequency 5
        @Independent
        static Duration between(Temporal startInclusive, Temporal endExclusive) { return null; }

        //override from java.time.temporal.TemporalAmount
        long get(TemporalUnit unit) { return 0L; }

        //override from java.time.temporal.TemporalAmount
        List<TemporalUnit> getUnits() { return null; }
        boolean isPositive() { return false; }
        boolean isZero() { return false; }
        boolean isNegative() { return false; }
        long getSeconds() { return 0L; }
        int getNano() { return 0; }
        Duration withSeconds(long seconds) { return null; }
        Duration withNanos(int nanoOfSecond) { return null; }
        Duration plus(Duration duration) { return null; }
        Duration plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }
        Duration plusDays(long daysToAdd) { return null; }
        Duration plusHours(long hoursToAdd) { return null; }
        Duration plusMinutes(long minutesToAdd) { return null; }
        Duration plusSeconds(long secondsToAdd) { return null; }
        Duration plusMillis(long millisToAdd) { return null; }
        Duration plusNanos(long nanosToAdd) { return null; }
        Duration minus(Duration duration) { return null; }
        Duration minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }
        Duration minusDays(long daysToSubtract) { return null; }
        Duration minusHours(long hoursToSubtract) { return null; }
        Duration minusMinutes(long minutesToSubtract) { return null; }
        Duration minusSeconds(long secondsToSubtract) { return null; }
        Duration minusMillis(long millisToSubtract) { return null; }
        Duration minusNanos(long nanosToSubtract) { return null; }
        Duration multipliedBy(long multiplicand) { return null; }
        Duration dividedBy(long divisor) { return null; }
        long dividedBy(Duration divisor) { return 0L; }
        Duration negated() { return null; }
        Duration abs() { return null; }
        //override from java.time.temporal.TemporalAmount
        Temporal addTo(Temporal temporal) { return null; }

        //override from java.time.temporal.TemporalAmount
        Temporal subtractFrom(Temporal temporal) { return null; }
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
        Duration truncatedTo(TemporalUnit unit) { return null; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ Duration otherDuration) { return 0; }

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
    class Instant$ {
        static final Instant EPOCH = null;
        static final Instant MIN = null;
        static final Instant MAX = null;
        //frequency 11
        static Instant now() { return null; }
        static Instant now(Clock clock) { return null; }
        static Instant ofEpochSecond(long epochSecond) { return null; }
        static Instant ofEpochSecond(long epochSecond, long l) { return null; }
        static Instant ofEpochMilli(long epochMilli) { return null; }
        static Instant from(TemporalAccessor temporal) { return null; }
        static Instant parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text) {
            return null;
        }

        //override from java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }
        long getEpochSecond() { return 0L; }
        int getNano() { return 0; }
        //override from java.time.temporal.Temporal
        Instant with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.temporal.Temporal
        Instant with(TemporalField field, long newValue) { return null; }
        Instant truncatedTo(TemporalUnit unit) { return null; }
        //override from java.time.temporal.Temporal
        Instant plus(TemporalAmount amountToAdd) { return null; }

        //override from java.time.temporal.Temporal
        //frequency 1
        Instant plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }
        Instant plusSeconds(long secondsToAdd) { return null; }
        Instant plusMillis(long millisToAdd) { return null; }
        Instant plusNanos(long nanosToAdd) { return null; }
        //override from java.time.temporal.Temporal
        Instant minus(TemporalAmount amountToSubtract) { return null; }

        //override from java.time.temporal.Temporal
        //frequency 6
        Instant minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }

        //frequency 1
        Instant minusSeconds(long secondsToSubtract) { return null; }
        Instant minusMillis(long millisToSubtract) { return null; }
        Instant minusNanos(long nanosToSubtract) { return null; }
        //override from java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(Temporal endExclusive, TemporalUnit unit) { return 0L; }
        Duration until(Instant endExclusive) { return null; }
        OffsetDateTime atOffset(ZoneOffset offset) { return null; }
        ZonedDateTime atZone(ZoneId zone) { return null; }
        long toEpochMilli() { return 0L; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ Instant otherInstant) { return 0; }

        //frequency 1
        boolean isAfter(Instant otherInstant) { return false; }
        boolean isBefore(Instant otherInstant) { return false; }
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

    //public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable
    class LocalDate$ {
        static final LocalDate MIN = null;
        static final LocalDate MAX = null;
        static final LocalDate EPOCH = null;
        static LocalDate now() { return null; }
        static LocalDate now(ZoneId zone) { return null; }
        static LocalDate now(Clock clock) { return null; }
        static LocalDate of(int year, Month month, int dayOfMonth) { return null; }
        static LocalDate of(int year, int month, int dayOfMonth) { return null; }
        static LocalDate ofYearDay(int year, int dayOfYear) { return null; }
        static LocalDate ofInstant(Instant instant, ZoneId zone) { return null; }
        static LocalDate ofEpochDay(long epochDay) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        static LocalDate from(TemporalAccessor temporal) { return null; }

        static LocalDate parse(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text) {
            return null;
        }

        static LocalDate parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text,
            DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }

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
        LocalDate with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate with(TemporalField field, long newValue) { return null; }
        LocalDate withYear(int year) { return null; }
        LocalDate withMonth(int month) { return null; }
        LocalDate withDayOfMonth(int dayOfMonth) { return null; }
        LocalDate withDayOfYear(int dayOfYear) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate plus(TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }
        LocalDate plusYears(long yearsToAdd) { return null; }
        LocalDate plusMonths(long monthsToAdd) { return null; }
        LocalDate plusWeeks(long weeksToAdd) { return null; }
        LocalDate plusDays(long daysToAdd) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate minus(TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        LocalDate minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }
        LocalDate minusYears(long yearsToSubtract) { return null; }
        LocalDate minusMonths(long monthsToSubtract) { return null; }
        LocalDate minusWeeks(long weeksToSubtract) { return null; }
        LocalDate minusDays(long daysToSubtract) { return null; }
        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAccessor
        //@Independent(hc=true)[H]
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }

        //override from java.time.chrono.ChronoLocalDate, java.time.temporal.Temporal
        long until(Temporal endExclusive, TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoLocalDate
        Period until(ChronoLocalDate endDateExclusive) { return null; }
        Stream<LocalDate> datesUntil(LocalDate endExclusive) { return null; }
        Stream<LocalDate> datesUntil(LocalDate endExclusive, Period step) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        String format(DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDate
        LocalDateTime atTime(LocalTime time) { return null; }
        LocalDateTime atTime(int hour, int minute) { return null; }
        LocalDateTime atTime(int hour, int minute, int second) { return null; }
        LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) { return null; }
        OffsetDateTime atTime(OffsetTime time) { return null; }
        LocalDateTime atStartOfDay() { return null; }
        ZonedDateTime atStartOfDay(ZoneId zone) { return null; }
        //override from java.time.chrono.ChronoLocalDate
        long toEpochDay() { return 0L; }
        long toEpochSecond(LocalTime time, ZoneOffset offset) { return 0L; }
        //override from java.lang.Comparable, java.time.chrono.ChronoLocalDate
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ChronoLocalDate other) { return 0; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isAfter(ChronoLocalDate other) { return false; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isBefore(ChronoLocalDate other) { return false; }

        //override from java.time.chrono.ChronoLocalDate
        boolean isEqual(ChronoLocalDate other) { return false; }

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
    class LocalDateTime$ {
        static final LocalDateTime MIN = null;
        static final LocalDateTime MAX = null;
        //frequency 1
        static LocalDateTime now() { return null; }
        static LocalDateTime now(ZoneId zone) { return null; }
        static LocalDateTime now(Clock clock) { return null; }
        static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute) { return null; }
        static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute, int second) { return null; }

        static LocalDateTime of(
            int year,
            Month month,
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
        static LocalDateTime of(LocalDate date, LocalTime time) { return null; }
        static LocalDateTime ofInstant(Instant instant, ZoneId zone) { return null; }
        static LocalDateTime ofEpochSecond(long epochSecond, int i, ZoneOffset nanoOfSecond) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime
        static LocalDateTime from(TemporalAccessor temporal) { return null; }

        static LocalDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text) { return null; }

        static LocalDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text,
            DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }

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
        LocalDateTime with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime with(TemporalField field, long newValue) { return null; }
        LocalDateTime withYear(int year) { return null; }
        LocalDateTime withMonth(int month) { return null; }
        LocalDateTime withDayOfMonth(int dayOfMonth) { return null; }
        LocalDateTime withDayOfYear(int dayOfYear) { return null; }
        LocalDateTime withHour(int hour) { return null; }
        LocalDateTime withMinute(int minute) { return null; }
        LocalDateTime withSecond(int second) { return null; }
        LocalDateTime withNano(int nanoOfSecond) { return null; }
        LocalDateTime truncatedTo(TemporalUnit unit) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime plus(TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }
        LocalDateTime plusYears(long years) { return null; }
        LocalDateTime plusMonths(long months) { return null; }
        LocalDateTime plusWeeks(long weeks) { return null; }
        LocalDateTime plusDays(long days) { return null; }
        LocalDateTime plusHours(long hours) { return null; }
        LocalDateTime plusMinutes(long minutes) { return null; }
        LocalDateTime plusSeconds(long seconds) { return null; }
        LocalDateTime plusNanos(long nanos) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime minus(TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.Temporal
        LocalDateTime minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }
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
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.chrono.ChronoLocalDateTime, java.time.temporal.TemporalAdjuster
        Temporal adjustInto(Temporal temporal) { return null; }

        //override from java.time.temporal.Temporal
        long until(Temporal endExclusive, TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoLocalDateTime
        String format(DateTimeFormatter formatter) { return null; }
        OffsetDateTime atOffset(ZoneOffset offset) { return null; }
        //override from java.time.chrono.ChronoLocalDateTime
        //frequency 1
        ZonedDateTime atZone(ZoneId zone) { return null; }

        //override from java.lang.Comparable, java.time.chrono.ChronoLocalDateTime
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ ChronoLocalDateTime<?> other) { return 0; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isAfter(ChronoLocalDateTime<?> other) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isBefore(ChronoLocalDateTime<?> other) { return false; }

        //override from java.time.chrono.ChronoLocalDateTime
        boolean isEqual(ChronoLocalDateTime<?> other) { return false; }

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
    class ZoneId$ {
        static final Map<String, String> SHORT_IDS = null;
        //frequency 2
        static ZoneId systemDefault() { return null; }

        //@Independent[T]
        static Set<String> getAvailableZoneIds() { return null; }
        static ZoneId of(String zoneId, Map<String, String> aliasMap) { return null; }
        //frequency 2
        static ZoneId of(String zoneId) { return null; }
        static ZoneId ofOffset(String prefix, ZoneOffset offset) { return null; }
        static ZoneId from(TemporalAccessor temporal) { return null; }
        String getId() { return null; }
        String getDisplayName(TextStyle style, Locale locale) { return null; }
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
    class ZonedDateTime$ {
        //frequency 112
        static ZonedDateTime now() { return null; }

        //frequency 1
        static ZonedDateTime now(ZoneId zone) { return null; }
        static ZonedDateTime now(Clock clock) { return null; }
        static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) { return null; }
        static ZonedDateTime of(LocalDateTime localDateTime, ZoneId zone) { return null; }
        //frequency 3
        static ZonedDateTime of(
            int year,
            int month,
            int dayOfMonth,
            int hour,
            int minute,
            int second,
            int nanoOfSecond,
            ZoneId zone) { return null; }

        static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone, ZoneOffset preferredOffset) { return null; }
        static ZonedDateTime ofInstant(Instant instant, ZoneId zone) { return null; }
        static ZonedDateTime ofInstant(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) { return null; }
        static ZonedDateTime ofStrict(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime
        static ZonedDateTime from(TemporalAccessor temporal) { return null; }

        //frequency 4
        static ZonedDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text) { return null; }

        static ZonedDateTime parse(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence text,
            DateTimeFormatter formatter) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        boolean isSupported(TemporalField field) { return false; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        boolean isSupported(TemporalUnit unit) { return false; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        ValueRange range(TemporalField field) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        int get(TemporalField field) { return 0; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.TemporalAccessor
        long getLong(TemporalField field) { return 0L; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZoneOffset getOffset() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withEarlierOffsetAtOverlap() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withLaterOffsetAtOverlap() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZoneId getZone() { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        ZonedDateTime withZoneSameLocal(ZoneId zone) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime
        //frequency 1
        ZonedDateTime withZoneSameInstant(ZoneId zone) { return null; }
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
        ZonedDateTime with(TemporalAdjuster adjuster) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime with(TemporalField field, long newValue) { return null; }
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
        ZonedDateTime truncatedTo(TemporalUnit unit) { return null; }
        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime plus(TemporalAmount amountToAdd) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime plus(long amountToAdd, TemporalUnit temporalUnit) { return null; }
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
        ZonedDateTime minus(TemporalAmount amountToSubtract) { return null; }

        //override from java.time.chrono.ChronoZonedDateTime, java.time.temporal.Temporal
        ZonedDateTime minus(long amountToSubtract, TemporalUnit temporalUnit) { return null; }
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
        <R> R query(TemporalQuery<R> query) { return null; }

        //override from java.time.temporal.Temporal
        long until(Temporal endExclusive, TemporalUnit unit) { return 0L; }

        //override from java.time.chrono.ChronoZonedDateTime
        String format(DateTimeFormatter formatter) { return null; }
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
}
