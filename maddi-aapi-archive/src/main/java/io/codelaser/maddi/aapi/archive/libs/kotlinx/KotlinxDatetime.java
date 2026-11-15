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
package io.codelaser.maddi.aapi.archive.libs.kotlinx;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlinx.datetime} (0.7.1, the {@code 0.6.x-compat} build Exposed uses). The value
 * types wrap their {@code java.time} counterparts, which {@code jdk/JavaTime} already makes {@code @ImmutableContainer};
 * the type-level contract is the one that matters, since the compiler then writes non-modifying defaults for every
 * member it does not see contracted here. Companions and {@code Formats} objects hold constants only.
 * <p>
 * Names kotlinx types in full and imports none, as the {@code libs/kotlin} units do: the unit survives on the shared
 * hints factory, which carries no kotlinx jar (see {@code TestParseAnalyzeWrite}).
 * <p>
 * Not here: {@code DateTimeFormatBuilder} and its {@code WithDate}/{@code WithTime} views, which really are builders
 * ({@code hour()}, {@code char(':')} append to the format being built).
 */
public class KotlinxDatetime {
    public static final String PACKAGE_NAME = "kotlinx.datetime";

    // open class TimeZone: its two implementations (the region zone and FixedOffsetTimeZone) wrap a ZoneId
    @ImmutableContainer
    class TimeZone$ {
        @NotModified
        kotlinx.datetime.LocalDateTime toLocalDateTime(@NotModified kotlin.time.Instant instant) { return null; }

        @ImmutableContainer
        class Companion {
            @NotModified
            kotlinx.datetime.TimeZone currentSystemDefault() { return null; }

            @NotModified
            kotlinx.datetime.TimeZone of(String zoneId) { return null; }

            @NotModified
            kotlinx.datetime.FixedOffsetTimeZone getUTC() { return null; }
        }
    }

    @ImmutableContainer
    class FixedOffsetTimeZone$ {
    }

    @ImmutableContainer
    class UtcOffset$ {
    }

    // the deprecated kotlinx.datetime.Instant of the compat build, a wrapper of java.time.Instant
    @ImmutableContainer
    class Instant$ {
    }

    @ImmutableContainer
    class LocalDate$ {
        @NotModified
        int getYear() { return 0; }

        @ImmutableContainer
        class Companion {
            @NotModified
            kotlinx.datetime.LocalDate parse(@NotModified CharSequence input,
                                             @NotModified kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalDate> format) {
                return null;
            }
        }

        @ImmutableContainer
        class Formats {
            @NotModified
            kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalDate> getISO() { return null; }
        }
    }

    @ImmutableContainer
    class LocalDateTime$ {
        LocalDateTime$(@NotModified kotlinx.datetime.LocalDate date, @NotModified kotlinx.datetime.LocalTime time) {
        }

        @NotModified
        kotlinx.datetime.LocalDate getDate() { return null; }

        @NotModified
        kotlinx.datetime.LocalTime getTime() { return null; }

        @ImmutableContainer
        class Companion {
            @NotModified
            kotlinx.datetime.LocalDateTime parse(@NotModified CharSequence input,
                                                 @NotModified kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalDateTime> format) {
                return null;
            }

            /* runs the builder block on a fresh builder of its own; the block is the argument, not data */
            @NotModified
            kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalDateTime> Format(
                    kotlin.jvm.functions.Function1<? super kotlinx.datetime.format.DateTimeFormatBuilder.WithDateTime,
                            kotlin.Unit> builder) {
                return null;
            }
        }
    }

    @ImmutableContainer
    class LocalTime$ {
        @NotModified
        int getHour() { return 0; }

        @NotModified
        int getMinute() { return 0; }

        @NotModified
        int getSecond() { return 0; }

        @NotModified
        int getNanosecond() { return 0; }

        @ImmutableContainer
        class Companion {
            @NotModified
            kotlinx.datetime.LocalTime parse(@NotModified CharSequence input,
                                             @NotModified kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalTime> format) {
                return null;
            }
        }

        @ImmutableContainer
        class Formats {
            @NotModified
            kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalTime> getISO() { return null; }
        }
    }

    @ImmutableContainer
    class DatePeriod$ {
    }

    @ImmutableContainer
    class DateTimePeriod$ {
    }

    // the extension functions: conversions between immutable values
    class TimeZoneKt__TimeZoneJvmKt$ {
        @NotModified
        static kotlinx.datetime.LocalDateTime toLocalDateTime(@NotModified kotlin.time.Instant instant,
                                                              @NotModified kotlinx.datetime.TimeZone timeZone) {
            return null;
        }

        @NotModified
        static kotlin.time.Instant toInstant(@NotModified kotlinx.datetime.LocalDateTime dateTime,
                                             @NotModified kotlinx.datetime.TimeZone timeZone,
                                             @NotModified kotlinx.datetime.OverloadMarker marker) {
            return null;
        }

        @NotModified
        static kotlin.time.Instant atStartOfDayIn(@NotModified kotlinx.datetime.LocalDate date,
                                                  @NotModified kotlinx.datetime.TimeZone timeZone,
                                                  @NotModified kotlinx.datetime.OverloadMarker marker) {
            return null;
        }
    }

    class ConvertersKt$ {
        @NotModified
        static java.time.LocalDateTime toJavaLocalDateTime(@NotModified kotlinx.datetime.LocalDateTime dateTime) {
            return null;
        }

        @NotModified
        static java.time.LocalTime toJavaLocalTime(@NotModified kotlinx.datetime.LocalTime time) { return null; }

        @NotModified
        static java.time.LocalDate toJavaLocalDate(@NotModified kotlinx.datetime.LocalDate date) { return null; }

        @NotModified
        static kotlinx.datetime.LocalDateTime toKotlinLocalDateTime(@NotModified java.time.LocalDateTime dateTime) {
            return null;
        }

        @NotModified
        static kotlinx.datetime.LocalDate toKotlinLocalDate(@NotModified java.time.LocalDate date) { return null; }

        @NotModified
        static kotlinx.datetime.LocalTime toKotlinLocalTime(@NotModified java.time.LocalTime time) { return null; }
    }

    class LocalDateKt$ {
        @NotModified
        static kotlinx.datetime.LocalDateTime atTime(@NotModified kotlinx.datetime.LocalDate date,
                                                     @NotModified kotlinx.datetime.LocalTime time) {
            return null;
        }

        @NotModified
        static String format(@NotModified kotlinx.datetime.LocalDate date,
                             @NotModified kotlinx.datetime.format.DateTimeFormat<kotlinx.datetime.LocalDate> format) {
            return null;
        }
    }

    class MonthKt__MonthKt$ {
        @NotModified
        static int getNumber(@NotModified kotlinx.datetime.Month month) { return 0; }
    }
}
