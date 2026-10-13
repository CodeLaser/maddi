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
import java.math.*;
import java.util.Random;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotNull;
public class JavaMath {
    public static final String PACKAGE_NAME = "java.math";
    //public class BigDecimal extends Number implements Comparable<BigDecimal>
    @ImmutableContainer
    class BigDecimal$ {
        static final BigDecimal ONE = null;
        static final int ROUND_CEILING = 0;
        static final int ROUND_DOWN = 0;
        static final int ROUND_FLOOR = 0;
        static final int ROUND_HALF_DOWN = 0;
        static final int ROUND_HALF_EVEN = 0;
        static final int ROUND_HALF_UP = 0;
        static final int ROUND_UNNECESSARY = 0;
        static final int ROUND_UP = 0;
        static final BigDecimal TEN = null;
        static final BigDecimal TWO = null;
        static final BigDecimal ZERO = null;
        BigDecimal$(@NotNull String val) { }
        BigDecimal$(@NotNull String val, @NotNull MathContext mc) { }
        BigDecimal$(@NotNull char [] in) { }
        BigDecimal$(@NotNull char [] in, int offset, int len) { }
        BigDecimal$(@NotNull char [] in, int offset, int len, @NotNull MathContext mc) { }
        BigDecimal$(@NotNull char [] in, @NotNull MathContext mc) { }
        BigDecimal$(double val) { }
        BigDecimal$(double val, @NotNull MathContext mathContext) { }
        BigDecimal$(int val) { }
        BigDecimal$(int val, @NotNull MathContext mc) { }
        BigDecimal$(@NotNull BigInteger val) { }
        BigDecimal$(@NotNull BigInteger unscaledVal, int scale) { }
        BigDecimal$(@NotNull BigInteger unscaledVal, int scale, @NotNull MathContext mc) { }
        BigDecimal$(@NotNull BigInteger val, @NotNull MathContext mc) { }
        BigDecimal$(long val) { }
        BigDecimal$(long val, @NotNull MathContext mathContext) { }
        BigDecimal abs() { return null; }
        BigDecimal abs(@NotNull MathContext mc) { return null; }
        BigDecimal add(@NotNull BigDecimal augend) { return null; }
        BigDecimal add(@NotNull BigDecimal augend, @NotNull MathContext mc) { return null; }
        byte byteValueExact() { return 0; }
        //override from java.lang.Comparable
        int compareTo(@NotNull BigDecimal val) { return 0; }
        BigDecimal divide(@NotNull BigDecimal divisor) { return null; }
        BigDecimal divide(@NotNull BigDecimal divisor, int roundingMode) { return null; }
        BigDecimal divide(@NotNull BigDecimal divisor, int scale, int roundingMode) { return null; }
        BigDecimal divide(@NotNull BigDecimal divisor, int scale, @NotNull RoundingMode roundingMode) { return null; }
        BigDecimal divide(@NotNull BigDecimal divisor, @NotNull MathContext mc) { return null; }
        BigDecimal divide(@NotNull BigDecimal divisor, @NotNull RoundingMode roundingMode) { return null; }
        BigDecimal [] divideAndRemainder(@NotNull BigDecimal divisor) { return null; }
        BigDecimal [] divideAndRemainder(@NotNull BigDecimal divisor, @NotNull MathContext mc) { return null; }
        BigDecimal divideToIntegralValue(@NotNull BigDecimal divisor) { return null; }
        BigDecimal divideToIntegralValue(@NotNull BigDecimal divisor, @NotNull MathContext mc) { return null; }
        //override from java.lang.Number
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        public boolean equals(Object x) { return false; }

        //override from java.lang.Number
        float floatValue() { return 0.0F; }

        //override from java.lang.Object
        public int hashCode() { return 0; }

        //override from java.lang.Number
        int intValue() { return 0; }
        int intValueExact() { return 0; }
        //override from java.lang.Number
        long longValue() { return 0L; }
        long longValueExact() { return 0L; }
        BigDecimal max(@NotNull BigDecimal val) { return null; }
        BigDecimal min(@NotNull BigDecimal val) { return null; }
        BigDecimal movePointLeft(int n) { return null; }
        BigDecimal movePointRight(int n) { return null; }
        BigDecimal multiply(@NotNull BigDecimal multiplicand) { return null; }
        BigDecimal multiply(@NotNull BigDecimal multiplicand, @NotNull MathContext mc) { return null; }
        BigDecimal negate() { return null; }
        BigDecimal negate(@NotNull MathContext mc) { return null; }
        BigDecimal plus() { return null; }
        BigDecimal plus(@NotNull MathContext mc) { return null; }
        BigDecimal pow(int n) { return null; }
        BigDecimal pow(int n, @NotNull MathContext mc) { return null; }
        int precision() { return 0; }
        BigDecimal remainder(@NotNull BigDecimal divisor) { return null; }
        BigDecimal remainder(@NotNull BigDecimal divisor, @NotNull MathContext mc) { return null; }
        BigDecimal round(@NotNull MathContext mc) { return null; }
        int scale() { return 0; }
        BigDecimal scaleByPowerOfTen(int n) { return null; }
        BigDecimal setScale(int newScale) { return null; }
        BigDecimal setScale(int newScale, int roundingMode) { return null; }
        BigDecimal setScale(int newScale, @NotNull RoundingMode roundingMode) { return null; }
        short shortValueExact() { return 0; }
        int signum() { return 0; }
        BigDecimal sqrt(@NotNull MathContext mc) { return null; }
        BigDecimal stripTrailingZeros() { return null; }
        BigDecimal subtract(@NotNull BigDecimal subtrahend) { return null; }
        BigDecimal subtract(@NotNull BigDecimal subtrahend, @NotNull MathContext mc) { return null; }
        BigInteger toBigInteger() { return null; }
        BigInteger toBigIntegerExact() { return null; }
        String toEngineeringString() { return null; }
        String toPlainString() { return null; }
        //override from java.lang.Object
        public String toString() { return null; }
        BigDecimal ulp() { return null; }
        BigInteger unscaledValue() { return null; }
        static BigDecimal valueOf(double val) { return null; }
        static BigDecimal valueOf(long val) { return null; }
        static BigDecimal valueOf(long unscaledVal, int i) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.math.BigDecimal rootn(int p0, @NotNull java.math.MathContext p1) { return null; }
    }

    //public class BigInteger extends Number implements Comparable<BigInteger>
    @ImmutableContainer
    class BigInteger$ {
        static final BigInteger ONE = null;
        static final BigInteger TEN = null;
        static final BigInteger TWO = null;
        static final BigInteger ZERO = null;
        BigInteger$(@NotNull String val) { }
        BigInteger$(@NotNull String val, int radix) { }
        BigInteger$(@NotNull byte [] val) { }
        BigInteger$(@NotNull byte [] val, int off, int len) { }
        BigInteger$(int signum, @NotNull byte [] magnitude) { }
        BigInteger$(int signum, @NotNull byte [] magnitude, int off, int len) { }
        BigInteger$(int bitLength, int certainty, @NotNull Random rnd) { }
        BigInteger$(int numBits, @NotNull Random rnd) { }
        BigInteger abs() { return null; }
        BigInteger add(@NotNull BigInteger val) { return null; }
        BigInteger and(@NotNull BigInteger val) { return null; }
        BigInteger andNot(@NotNull BigInteger val) { return null; }
        int bitCount() { return 0; }
        int bitLength() { return 0; }
        byte byteValueExact() { return 0; }
        BigInteger clearBit(int n) { return null; }
        //override from java.lang.Comparable
        int compareTo(@NotNull BigInteger val) { return 0; }
        BigInteger divide(@NotNull BigInteger val) { return null; }
        BigInteger [] divideAndRemainder(@NotNull BigInteger val) { return null; }
        //override from java.lang.Number
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        public boolean equals(Object x) { return false; }
        BigInteger flipBit(int n) { return null; }
        //override from java.lang.Number
        float floatValue() { return 0.0F; }
        BigInteger gcd(@NotNull BigInteger val) { return null; }
        int getLowestSetBit() { return 0; }
        //override from java.lang.Object
        public int hashCode() { return 0; }

        //override from java.lang.Number
        int intValue() { return 0; }
        int intValueExact() { return 0; }
        boolean isProbablePrime(int certainty) { return false; }
        //override from java.lang.Number
        long longValue() { return 0L; }
        long longValueExact() { return 0L; }
        BigInteger max(@NotNull BigInteger val) { return null; }
        BigInteger min(@NotNull BigInteger val) { return null; }
        BigInteger mod(@NotNull BigInteger m) { return null; }
        BigInteger modInverse(@NotNull BigInteger m) { return null; }
        BigInteger modPow(@NotNull BigInteger exponent, @NotNull BigInteger m) { return null; }
        BigInteger multiply(@NotNull BigInteger val) { return null; }
        BigInteger negate() { return null; }
        BigInteger nextProbablePrime() { return null; }
        BigInteger not() { return null; }
        BigInteger or(@NotNull BigInteger val) { return null; }
        BigInteger parallelMultiply(@NotNull BigInteger val) { return null; }
        BigInteger pow(int exponent) { return null; }
        static BigInteger probablePrime(int bitLength, @NotNull Random rnd) { return null; }
        BigInteger remainder(@NotNull BigInteger val) { return null; }
        BigInteger rootn(int n) { return null; }
        BigInteger [] rootnAndRemainder(int n) { return null; }
        BigInteger setBit(int n) { return null; }
        BigInteger shiftLeft(int n) { return null; }
        BigInteger shiftRight(int n) { return null; }
        short shortValueExact() { return 0; }
        int signum() { return 0; }
        BigInteger sqrt() { return null; }
        BigInteger [] sqrtAndRemainder() { return null; }
        BigInteger subtract(@NotNull BigInteger val) { return null; }
        boolean testBit(int n) { return false; }
        byte [] toByteArray() { return null; }
        //override from java.lang.Object
        public String toString() { return null; }
        String toString(int radix) { return null; }
        static BigInteger valueOf(long val) { return null; }
        BigInteger xor(@NotNull BigInteger val) { return null; }
    }

    //public final class MathContext implements Serializable
    @ImmutableContainer
    class MathContext$ {
        static final MathContext DECIMAL128 = null;
        static final MathContext DECIMAL32 = null;
        static final MathContext DECIMAL64 = null;
        static final MathContext UNLIMITED = null;
        MathContext$(@NotNull String val) { }
        MathContext$(int setPrecision) { }
        MathContext$(int setPrecision, @NotNull RoundingMode setRoundingMode) { }
        //override from java.lang.Object
        public boolean equals(Object x) { return false; }
        int getPrecision() { return 0; }
        RoundingMode getRoundingMode() { return null; }
        //override from java.lang.Object
        public int hashCode() { return 0; }

        //override from java.lang.Object
        public String toString() { return null; }
    }

    //public enum RoundingMode extends Enum<RoundingMode>
    @ImmutableContainer
    class RoundingMode$ {
        static final RoundingMode CEILING = null;
        static final RoundingMode DOWN = null;
        static final RoundingMode FLOOR = null;
        static final RoundingMode HALF_DOWN = null;
        static final RoundingMode HALF_EVEN = null;
        static final RoundingMode HALF_UP = null;
        static final RoundingMode UNNECESSARY = null;
        static final RoundingMode UP = null;
        static RoundingMode valueOf(String name) { return null; }
        static RoundingMode valueOf(int rm) { return null; }
        static RoundingMode [] values() { return null; }
    }
}
