package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.*;

/**
 * CodeLaser/maddi#49: a literal keeps how it was written ({@code 0xFFFF}, {@code 50_000L}, {@code 0b1010}, a char
 * written as a unicode escape, {@code 1e3}), so that code the printer re-renders does not restyle it. The spelling
 * is kept only when it differs from how the value prints by default, and only when it reads back as the same value:
 * a position that does not cover the literal exactly (javac's {@code -2147483648} is one literal, its sign
 * included) then falls back to the value.
 */
final class LiteralSpelling {

    private LiteralSpelling() {
    }

    /**
     * @return {@code written} when the constant should print that way, null when it prints as its value
     */
    static String spelling(ConstantExpression<?> constant, String written) {
        if (written == null || written.isEmpty() || written.equals(constant.print(null).toString())) return null;
        try {
            boolean same = switch (constant) {
                case IntConstant ic -> ic.constant() == (int) integral(written, false);
                case LongConstant lc -> lc.constant() == integral(written, true);
                case DoubleConstant dc -> Double.compare(dc.constant(), Double.parseDouble(digits(written))) == 0;
                case FloatConstant fc -> Float.compare(fc.constant(), Float.parseFloat(digits(written))) == 0;
                case CharConstant _ -> written.length() >= 3 && written.startsWith("'") && written.endsWith("'");
                default -> false;
            };
            return same ? written : null;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    private static String digits(String written) {
        return written.replace("_", "");
    }

    /*
    Java's integer literal: decimal, 0x hex, 0b binary, 0 octal; hex, binary and octal may set the sign bit
    (0xFFFFFFFF is -1), a decimal int may not exceed Integer.MAX_VALUE except after a minus.
     */
    private static long integral(String written, boolean isLong) {
        String s = digits(written);
        if (isLong) {
            if (!s.endsWith("L") && !s.endsWith("l")) throw new NumberFormatException(written);
            s = s.substring(0, s.length() - 1);
        }
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);
        int radix = 10;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            radix = 16;
            s = s.substring(2);
        } else if (s.startsWith("0b") || s.startsWith("0B")) {
            radix = 2;
            s = s.substring(2);
        } else if (s.length() > 1 && s.startsWith("0")) {
            radix = 8;
            s = s.substring(1);
        }
        long value;
        if (radix == 10) {
            value = Long.parseLong((negative ? "-" : "") + s);
            if (!isLong) Math.toIntExact(value);
            return value;
        }
        value = isLong ? Long.parseUnsignedLong(s, radix) : Integer.toUnsignedLong(Integer.parseUnsignedInt(s, radix));
        if (!isLong) value = (int) value;
        return negative ? -value : value;
    }
}
