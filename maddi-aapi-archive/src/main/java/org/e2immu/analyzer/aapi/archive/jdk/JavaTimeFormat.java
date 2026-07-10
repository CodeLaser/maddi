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

package org.e2immu.analyzer.aapi.archive.jdk;
import java.text.Format;
import java.text.ParsePosition;
import java.time.Period;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.*;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotModified;
public class JavaTimeFormat {
    public static final String PACKAGE_NAME = "java.time.format";
    //public final class DateTimeFormatter
    @Immutable
    class DateTimeFormatter$ {
        static final DateTimeFormatter BASIC_ISO_DATE = null;
        static final DateTimeFormatter ISO_DATE = null;
        static final DateTimeFormatter ISO_DATE_TIME = null;
        static final DateTimeFormatter ISO_INSTANT = null;
        static final DateTimeFormatter ISO_LOCAL_DATE = null;
        static final DateTimeFormatter ISO_LOCAL_DATE_TIME = null;
        static final DateTimeFormatter ISO_LOCAL_TIME = null;
        static final DateTimeFormatter ISO_OFFSET_DATE = null;
        static final DateTimeFormatter ISO_OFFSET_DATE_TIME = null;
        static final DateTimeFormatter ISO_OFFSET_TIME = null;
        static final DateTimeFormatter ISO_ORDINAL_DATE = null;
        static final DateTimeFormatter ISO_TIME = null;
        static final DateTimeFormatter ISO_WEEK_DATE = null;
        static final DateTimeFormatter ISO_ZONED_DATE_TIME = null;
        static final DateTimeFormatter RFC_1123_DATE_TIME = null;
        String format(TemporalAccessor temporal) { return null; }
        void formatTo(TemporalAccessor temporal, Appendable appendable) { }
        Chronology getChronology() { return null; }
        DecimalStyle getDecimalStyle() { return null; }
        Locale getLocale() { return null; }
        Set<TemporalField> getResolverFields() { return null; }
        ResolverStyle getResolverStyle() { return null; }
        ZoneId getZone() { return null; }
        DateTimeFormatter localizedBy(Locale locale) { return null; }
        static DateTimeFormatter ofLocalizedDate(FormatStyle dateStyle) { return null; }
        static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateTimeStyle) { return null; }
        static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateStyle, FormatStyle timeStyle) { return null; }
        static DateTimeFormatter ofLocalizedPattern(String requestedTemplate) { return null; }
        static DateTimeFormatter ofLocalizedTime(FormatStyle timeStyle) { return null; }
        static DateTimeFormatter ofPattern(String pattern) { return null; }
        static DateTimeFormatter ofPattern(String pattern, Locale locale) { return null; }
        TemporalAccessor parse(CharSequence text) { return null; }
        TemporalAccessor parse(CharSequence text, ParsePosition position) { return null; }
        <T> T parse(CharSequence text, TemporalQuery<T> query) { return null; }
        TemporalAccessor parseBest(CharSequence text, TemporalQuery<?> ... queries) { return null; }
        TemporalAccessor parseUnresolved(CharSequence text, ParsePosition position) { return null; }
        static TemporalQuery<Period> parsedExcessDays() { return null; }
        static TemporalQuery<Boolean> parsedLeapSecond() { return null; }
        Format toFormat() { return null; }
        Format toFormat(TemporalQuery<?> parseQuery) { return null; }
        //override from java.lang.Object
        public String toString() { return null; }
        DateTimeFormatter withChronology(Chronology chrono) { return null; }
        DateTimeFormatter withDecimalStyle(DecimalStyle decimalStyle) { return null; }
        DateTimeFormatter withLocale(Locale locale) { return null; }
        DateTimeFormatter withResolverFields(TemporalField ... resolverFields) { return null; }
        DateTimeFormatter withResolverFields(Set<TemporalField> resolverFields) { return null; }
        DateTimeFormatter withResolverStyle(ResolverStyle resolverStyle) { return null; }
        DateTimeFormatter withZone(ZoneId zone) { return null; }
    }

    //public final class DateTimeFormatterBuilder
    @Container
    class DateTimeFormatterBuilder$ {
        DateTimeFormatterBuilder$() { }
        DateTimeFormatterBuilder append(DateTimeFormatter formatter) { return null; }
        DateTimeFormatterBuilder appendChronologyId() { return null; }
        DateTimeFormatterBuilder appendChronologyText(TextStyle textStyle) { return null; }
        DateTimeFormatterBuilder appendDayPeriodText(TextStyle style) { return null; }
        DateTimeFormatterBuilder appendFraction(TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {
            return null;
        }
        DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle) { return null; }
        DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle, Set<ZoneId> preferredZones) { return null; }
        DateTimeFormatterBuilder appendInstant() { return null; }
        DateTimeFormatterBuilder appendInstant(int fractionalDigits) { return null; }
        DateTimeFormatterBuilder appendLiteral(String literal) { return null; }
        DateTimeFormatterBuilder appendLiteral(char literal) { return null; }
        DateTimeFormatterBuilder appendLocalized(String requestedTemplate) { return null; }
        DateTimeFormatterBuilder appendLocalized(FormatStyle dateStyle, FormatStyle timeStyle) { return null; }
        DateTimeFormatterBuilder appendLocalizedOffset(TextStyle style) { return null; }
        DateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) { return null; }
        DateTimeFormatterBuilder appendOffsetId() { return null; }
        DateTimeFormatterBuilder appendOptional(DateTimeFormatter formatter) { return null; }
        DateTimeFormatterBuilder appendPattern(String pattern) { return null; }
        DateTimeFormatterBuilder appendText(TemporalField field) { return null; }
        DateTimeFormatterBuilder appendText(TemporalField field, TextStyle textStyle) { return null; }
        DateTimeFormatterBuilder appendText(TemporalField field, Map<Long, String> textLookup) { return null; }
        DateTimeFormatterBuilder appendValue(TemporalField field) { return null; }
        DateTimeFormatterBuilder appendValue(TemporalField field, int width) { return null; }
        DateTimeFormatterBuilder appendValue(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
            return null;
        }

        DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth, int baseValue) {
            return null;
        }

        DateTimeFormatterBuilder appendValueReduced(
            TemporalField field,
            int width,
            int maxWidth,
            ChronoLocalDate baseDate) { return null; }
        DateTimeFormatterBuilder appendZoneId() { return null; }
        DateTimeFormatterBuilder appendZoneOrOffsetId() { return null; }
        DateTimeFormatterBuilder appendZoneRegionId() { return null; }
        DateTimeFormatterBuilder appendZoneText(TextStyle textStyle) { return null; }
        DateTimeFormatterBuilder appendZoneText(TextStyle textStyle, Set<ZoneId> preferredZones) { return null; }
        static String getLocalizedDateTimePattern(String requestedTemplate, Chronology chrono, Locale locale) {
            return null;
        }

        static String getLocalizedDateTimePattern(
            FormatStyle dateStyle,
            FormatStyle timeStyle,
            Chronology chrono,
            Locale locale) { return null; }
        DateTimeFormatterBuilder optionalEnd() { return null; }
        DateTimeFormatterBuilder optionalStart() { return null; }
        DateTimeFormatterBuilder padNext(int padWidth) { return null; }
        DateTimeFormatterBuilder padNext(int padWidth, char padChar) { return null; }
        DateTimeFormatterBuilder parseCaseInsensitive() { return null; }
        DateTimeFormatterBuilder parseCaseSensitive() { return null; }
        DateTimeFormatterBuilder parseDefaulting(TemporalField field, long value) { return null; }
        DateTimeFormatterBuilder parseLenient() { return null; }
        DateTimeFormatterBuilder parseStrict() { return null; }
        @NotModified DateTimeFormatter toFormatter() { return null; }
        @NotModified DateTimeFormatter toFormatter(Locale locale) { return null; }
    }

    //public final class DecimalStyle
    @ImmutableContainer
    class DecimalStyle$ {
        static final DecimalStyle STANDARD = null;
        //override from java.lang.Object
        public boolean equals(Object obj) { return false; }
        static Set<Locale> getAvailableLocales() { return null; }
        char getDecimalSeparator() { return '\0'; }
        char getNegativeSign() { return '\0'; }
        char getPositiveSign() { return '\0'; }
        char getZeroDigit() { return '\0'; }
        //override from java.lang.Object
        public int hashCode() { return 0; }
        static DecimalStyle of(Locale locale) { return null; }
        static DecimalStyle ofDefaultLocale() { return null; }
        //override from java.lang.Object
        public String toString() { return null; }
        DecimalStyle withDecimalSeparator(char decimalSeparator) { return null; }
        DecimalStyle withNegativeSign(char negativeSign) { return null; }
        DecimalStyle withPositiveSign(char positiveSign) { return null; }
        DecimalStyle withZeroDigit(char zeroDigit) { return null; }
    }

    //public enum FormatStyle extends Enum<FormatStyle>
    @ImmutableContainer
    class FormatStyle$ {
        static final FormatStyle FULL = null;
        static final FormatStyle LONG = null;
        static final FormatStyle MEDIUM = null;
        static final FormatStyle SHORT = null;
        static FormatStyle valueOf(String name) { return null; }
        static FormatStyle [] values() { return null; }
    }

    //public enum ResolverStyle extends Enum<ResolverStyle>
    @ImmutableContainer
    class ResolverStyle$ {
        static final ResolverStyle LENIENT = null;
        static final ResolverStyle SMART = null;
        static final ResolverStyle STRICT = null;
        static ResolverStyle valueOf(String name) { return null; }
        static ResolverStyle [] values() { return null; }
    }

    //public enum SignStyle extends Enum<SignStyle>
    @ImmutableContainer
    class SignStyle$ {
        static final SignStyle ALWAYS = null;
        static final SignStyle EXCEEDS_PAD = null;
        static final SignStyle NEVER = null;
        static final SignStyle NORMAL = null;
        static final SignStyle NOT_NEGATIVE = null;
        static SignStyle valueOf(String name) { return null; }
        static SignStyle [] values() { return null; }
    }

    //public enum TextStyle extends Enum<TextStyle>
    @ImmutableContainer
    class TextStyle$ {
        static final TextStyle FULL = null;
        static final TextStyle FULL_STANDALONE = null;
        static final TextStyle NARROW = null;
        static final TextStyle NARROW_STANDALONE = null;
        static final TextStyle SHORT = null;
        static final TextStyle SHORT_STANDALONE = null;
        TextStyle asNormal() { return null; }
        TextStyle asStandalone() { return null; }
        boolean isStandalone() { return false; }
        static TextStyle valueOf(String name) { return null; }
        static TextStyle [] values() { return null; }
    }
}
