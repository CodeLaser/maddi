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
import java.time.Duration;
import java.time.temporal.*;
import java.util.List;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaTimeTemporal {
    public static final String PACKAGE_NAME = "java.time.temporal";
    //public enum ChronoUnit extends Enum<ChronoUnit> implements TemporalUnit
    @ImmutableContainer
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
        boolean isSupportedBy(@NotNull Temporal temporal) { return false; }

        //override from java.time.temporal.TemporalUnit
        <R extends Temporal> R addTo(R temporal, long amount) { return null; }

        //override from java.time.temporal.TemporalUnit
        //frequency 1
        long between(@NotNull Temporal temporal1Inclusive, @NotNull Temporal temporal2Exclusive) { return 0L; }

        //override from java.lang.Enum, java.lang.Object, java.time.temporal.TemporalUnit
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public interface Temporal implements TemporalAccessor
    @ImmutableContainer
    class Temporal$ {
        boolean isSupported(@NotNull TemporalUnit temporalUnit) { return false; }
        Temporal with(@NotNull TemporalAdjuster adjuster) { return null; }
        Temporal with(@NotNull TemporalField temporalField, long l) { return null; }
        Temporal plus(@NotNull TemporalAmount amount) { return null; }
        //override has frequency 1
        Temporal plus(long l, @NotNull TemporalUnit temporalUnit) { return null; }
        Temporal minus(@NotNull TemporalAmount amount) { return null; }
        //override has frequency 6
        Temporal minus(long amountToSubtract, @NotNull TemporalUnit temporalUnit) { return null; }
        long until(@NotNull Temporal temporal, @NotNull TemporalUnit temporalUnit) { return 0L; }
    }

    //public interface TemporalAccessor
    @ImmutableContainer
    class TemporalAccessor$ {
        boolean isSupported(@NotNull TemporalField temporalField) { return false; }
        ValueRange range(@NotNull TemporalField field) { return null; }
        int get(@NotNull TemporalField field) { return 0; }
        long getLong(@NotNull TemporalField temporalField) { return 0L; }
        //@Independent(hc=true)[T]
        <R> R query(@NotNull TemporalQuery<R> query) { return null; }
    }

    //public interface TemporalAdjuster
    @ImmutableContainer(hc = true)
    class TemporalAdjuster$ {Temporal adjustInto(@NotNull Temporal temporal) { return null; } }

    //public interface TemporalAmount
    @ImmutableContainer
    class TemporalAmount$ {
        long get(@NotNull TemporalUnit temporalUnit) { return 0L; }
        List<TemporalUnit> getUnits() { return null; }
        Temporal addTo(@NotNull Temporal temporal) { return null; }
        Temporal subtractFrom(@NotNull Temporal temporal) { return null; }
    }

    //public interface TemporalUnit
    @ImmutableContainer
    class TemporalUnit$ {
        Duration getDuration() { return null; }
        boolean isDurationEstimated() { return false; }
        boolean isDateBased() { return false; }
        boolean isTimeBased() { return false; }
        boolean isSupportedBy(@NotNull Temporal temporal) { return false; }
        <R extends Temporal> R addTo(R r, long l) { return null; }
        //override has frequency 1
        long between(@NotNull Temporal temporal, @NotNull Temporal temporal1) { return 0L; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public java.time.temporal.ChronoField (shadow created for its nullability, generated)
    class ChronoField$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.lang.String getDisplayName(@NotNull java.util.Locale p0) { return null; }
        long getFrom(@NotNull java.time.temporal.TemporalAccessor p0) { return 0; }
        boolean isSupportedBy(@NotNull java.time.temporal.TemporalAccessor p0) { return false; }
        java.time.temporal.ValueRange rangeRefinedBy(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
    }

    //public java.time.temporal.TemporalField (shadow created for its nullability, generated)
    class TemporalField$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.lang.String getDisplayName(@NotNull java.util.Locale p0) { return null; }
        long getFrom(@NotNull java.time.temporal.TemporalAccessor p0) { return 0; }
        boolean isSupportedBy(@NotNull java.time.temporal.TemporalAccessor p0) { return false; }
        java.time.temporal.ValueRange rangeRefinedBy(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
        @Nullable java.time.temporal.TemporalAccessor resolve(@NotNull java.util.Map<java.time.temporal.TemporalField, java.lang.Long> p0, @NotNull java.time.temporal.TemporalAccessor p1, @NotNull java.time.format.ResolverStyle p2) { return null; }
    }

    //public java.time.temporal.TemporalQuery (shadow created for its nullability, generated)
    class TemporalQuery$<R> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        R queryFrom(@NotNull java.time.temporal.TemporalAccessor p0) { return null; }
    }

    //public java.time.temporal.UnsupportedTemporalTypeException (shadow created for its nullability, generated)
    class UnsupportedTemporalTypeException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsupportedTemporalTypeException$(@Nullable java.lang.String p0) { }
        UnsupportedTemporalTypeException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
    }

    //public java.time.temporal.WeekFields (shadow created for its nullability, generated)
    class WeekFields$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.time.temporal.WeekFields of(@NotNull java.time.DayOfWeek p0, int p1) { return null; }
        static java.time.temporal.WeekFields of(@NotNull java.util.Locale p0) { return null; }
    }
}
