package org.e2immu.analyzer.aapi.archive.jdk;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.method.GetSet;

import java.math.RoundingMode;
import java.text.*;
import java.util.Currency;
import java.util.Locale;
public class JavaText {
    public static final String PACKAGE_NAME = "java.text";

    class NumberFormat$ {
        static final int INTEGER_FIELD = 0;
        static final int FRACTION_FIELD = 0;

        @Immutable
        class Field {
            static final NumberFormat.Field INTEGER = null;
            static final NumberFormat.Field FRACTION = null;
            static final NumberFormat.Field EXPONENT = null;
            static final NumberFormat.Field DECIMAL_SEPARATOR = null;
            static final NumberFormat.Field SIGN = null;
            static final NumberFormat.Field GROUPING_SEPARATOR = null;
            static final NumberFormat.Field EXPONENT_SYMBOL = null;
            static final NumberFormat.Field PERCENT = null;
            static final NumberFormat.Field PERMILLE = null;
            static final NumberFormat.Field CURRENCY = null;
            static final NumberFormat.Field EXPONENT_SIGN = null;
            static final NumberFormat.Field PREFIX = null;
            static final NumberFormat.Field SUFFIX = null;
        }

        @Immutable
        class Style {
            static final NumberFormat.Style SHORT = null;
            static final NumberFormat.Style LONG = null;
            static NumberFormat.Style[] values() { return null; }
            static NumberFormat.Style valueOf(String name) { return null; }
        }

        @NotModified
        StringBuffer format(@NotModified Object number, @Modified StringBuffer toAppendTo, FieldPosition pos) { return null; }
        Object parseObject(String source, ParsePosition pos) { return null; }
        @NotModified
        String format(double number) { return null; }
        @NotModified
        String format(long number) { return null; }
        @NotModified
        StringBuffer format(double d, @Modified StringBuffer stringBuffer, FieldPosition fieldPosition) { return null; }
        @NotModified
        StringBuffer format(long l, @Modified StringBuffer stringBuffer, FieldPosition fieldPosition) { return null; }
        @NotModified
        Number parse(String string, ParsePosition parsePosition) { return null; }
        @NotModified
        Number parse(String source) { return null; }
        boolean isParseIntegerOnly() { return false; }
        void setParseIntegerOnly(boolean value) { }
        static NumberFormat getInstance() { return null; }
        static NumberFormat getInstance(@NotModified Locale inLocale) { return null; }
        static NumberFormat getNumberInstance() { return null; }
        static NumberFormat getNumberInstance(@NotModified Locale inLocale) { return null; }
        static NumberFormat getIntegerInstance() { return null; }
        static NumberFormat getIntegerInstance(@NotModified Locale inLocale) { return null; }
        static NumberFormat getCurrencyInstance() { return null; }
        static NumberFormat getCurrencyInstance(@NotModified Locale inLocale) { return null; }
        static NumberFormat getPercentInstance() { return null; }
        static NumberFormat getPercentInstance(@NotModified Locale inLocale) { return null; }
        static NumberFormat getCompactNumberInstance() { return null; }
        static NumberFormat getCompactNumberInstance(@NotModified Locale locale, NumberFormat.Style formatStyle) { return null; }
        static Locale[] getAvailableLocales() { return null; }

        @GetSet
        boolean isGroupingUsed() { return false; }
        @GetSet
        void setGroupingUsed(boolean newValue) { }
        @GetSet
        int getMaximumIntegerDigits() { return 0; }
        @GetSet
        void setMaximumIntegerDigits(int newValue) { }
        @GetSet
        int getMinimumIntegerDigits() { return 0; }
        @GetSet
        void setMinimumIntegerDigits(int newValue) { }
        @GetSet
        int getMaximumFractionDigits() { return 0; }
        @GetSet
        void setMaximumFractionDigits(int newValue) { }
        @GetSet
        int getMinimumFractionDigits() { return 0; }
        @GetSet
        void setMinimumFractionDigits(int newValue) { }
        @GetSet
        Currency getCurrency() { return null; }
        @GetSet
        void setCurrency(Currency currency) { }
        @GetSet
        RoundingMode getRoundingMode() { return null; }
        @GetSet
        void setRoundingMode(RoundingMode roundingMode) { }
    }
}
