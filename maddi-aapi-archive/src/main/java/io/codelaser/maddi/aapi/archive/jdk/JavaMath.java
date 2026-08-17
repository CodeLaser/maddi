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
import java.math.*;
import java.util.Random;

import org.e2immu.annotation.ImmutableContainer;
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
        BigDecimal$(String val) { }
        BigDecimal$(String val, MathContext mc) { }
        BigDecimal$(char [] in) { }
        BigDecimal$(char [] in, int offset, int len) { }
        BigDecimal$(char [] in, int offset, int len, MathContext mc) { }
        BigDecimal$(char [] in, MathContext mc) { }
        BigDecimal$(double val) { }
        BigDecimal$(double val, MathContext mathContext) { }
        BigDecimal$(int val) { }
        BigDecimal$(int val, MathContext mc) { }
        BigDecimal$(BigInteger val) { }
        BigDecimal$(BigInteger unscaledVal, int scale) { }
        BigDecimal$(BigInteger unscaledVal, int scale, MathContext mc) { }
        BigDecimal$(BigInteger val, MathContext mc) { }
        BigDecimal$(long val) { }
        BigDecimal$(long val, MathContext mathContext) { }
        BigDecimal abs() { return null; }
        BigDecimal abs(MathContext mc) { return null; }
        BigDecimal add(BigDecimal augend) { return null; }
        BigDecimal add(BigDecimal augend, MathContext mc) { return null; }
        byte byteValueExact() { return 0; }
        //override from java.lang.Comparable
        int compareTo(BigDecimal val) { return 0; }
        BigDecimal divide(BigDecimal divisor) { return null; }
        BigDecimal divide(BigDecimal divisor, int roundingMode) { return null; }
        BigDecimal divide(BigDecimal divisor, int scale, int roundingMode) { return null; }
        BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode) { return null; }
        BigDecimal divide(BigDecimal divisor, MathContext mc) { return null; }
        BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode) { return null; }
        BigDecimal [] divideAndRemainder(BigDecimal divisor) { return null; }
        BigDecimal [] divideAndRemainder(BigDecimal divisor, MathContext mc) { return null; }
        BigDecimal divideToIntegralValue(BigDecimal divisor) { return null; }
        BigDecimal divideToIntegralValue(BigDecimal divisor, MathContext mc) { return null; }
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
        BigDecimal max(BigDecimal val) { return null; }
        BigDecimal min(BigDecimal val) { return null; }
        BigDecimal movePointLeft(int n) { return null; }
        BigDecimal movePointRight(int n) { return null; }
        BigDecimal multiply(BigDecimal multiplicand) { return null; }
        BigDecimal multiply(BigDecimal multiplicand, MathContext mc) { return null; }
        BigDecimal negate() { return null; }
        BigDecimal negate(MathContext mc) { return null; }
        BigDecimal plus() { return null; }
        BigDecimal plus(MathContext mc) { return null; }
        BigDecimal pow(int n) { return null; }
        BigDecimal pow(int n, MathContext mc) { return null; }
        int precision() { return 0; }
        BigDecimal remainder(BigDecimal divisor) { return null; }
        BigDecimal remainder(BigDecimal divisor, MathContext mc) { return null; }
        BigDecimal round(MathContext mc) { return null; }
        int scale() { return 0; }
        BigDecimal scaleByPowerOfTen(int n) { return null; }
        BigDecimal setScale(int newScale) { return null; }
        BigDecimal setScale(int newScale, int roundingMode) { return null; }
        BigDecimal setScale(int newScale, RoundingMode roundingMode) { return null; }
        short shortValueExact() { return 0; }
        int signum() { return 0; }
        BigDecimal sqrt(MathContext mc) { return null; }
        BigDecimal stripTrailingZeros() { return null; }
        BigDecimal subtract(BigDecimal subtrahend) { return null; }
        BigDecimal subtract(BigDecimal subtrahend, MathContext mc) { return null; }
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
    }

    //public class BigInteger extends Number implements Comparable<BigInteger>
    @ImmutableContainer
    class BigInteger$ {
        static final BigInteger ONE = null;
        static final BigInteger TEN = null;
        static final BigInteger TWO = null;
        static final BigInteger ZERO = null;
        BigInteger$(String val) { }
        BigInteger$(String val, int radix) { }
        BigInteger$(byte [] val) { }
        BigInteger$(byte [] val, int off, int len) { }
        BigInteger$(int signum, byte [] magnitude) { }
        BigInteger$(int signum, byte [] magnitude, int off, int len) { }
        BigInteger$(int bitLength, int certainty, Random rnd) { }
        BigInteger$(int numBits, Random rnd) { }
        BigInteger abs() { return null; }
        BigInteger add(BigInteger val) { return null; }
        BigInteger and(BigInteger val) { return null; }
        BigInteger andNot(BigInteger val) { return null; }
        int bitCount() { return 0; }
        int bitLength() { return 0; }
        byte byteValueExact() { return 0; }
        BigInteger clearBit(int n) { return null; }
        //override from java.lang.Comparable
        int compareTo(BigInteger val) { return 0; }
        BigInteger divide(BigInteger val) { return null; }
        BigInteger [] divideAndRemainder(BigInteger val) { return null; }
        //override from java.lang.Number
        double doubleValue() { return 0.0; }

        //override from java.lang.Object
        public boolean equals(Object x) { return false; }
        BigInteger flipBit(int n) { return null; }
        //override from java.lang.Number
        float floatValue() { return 0.0F; }
        BigInteger gcd(BigInteger val) { return null; }
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
        BigInteger max(BigInteger val) { return null; }
        BigInteger min(BigInteger val) { return null; }
        BigInteger mod(BigInteger m) { return null; }
        BigInteger modInverse(BigInteger m) { return null; }
        BigInteger modPow(BigInteger exponent, BigInteger m) { return null; }
        BigInteger multiply(BigInteger val) { return null; }
        BigInteger negate() { return null; }
        BigInteger nextProbablePrime() { return null; }
        BigInteger not() { return null; }
        BigInteger or(BigInteger val) { return null; }
        BigInteger parallelMultiply(BigInteger val) { return null; }
        BigInteger pow(int exponent) { return null; }
        static BigInteger probablePrime(int bitLength, Random rnd) { return null; }
        BigInteger remainder(BigInteger val) { return null; }
        BigInteger rootn(int n) { return null; }
        BigInteger [] rootnAndRemainder(int n) { return null; }
        BigInteger setBit(int n) { return null; }
        BigInteger shiftLeft(int n) { return null; }
        BigInteger shiftRight(int n) { return null; }
        short shortValueExact() { return 0; }
        int signum() { return 0; }
        BigInteger sqrt() { return null; }
        BigInteger [] sqrtAndRemainder() { return null; }
        BigInteger subtract(BigInteger val) { return null; }
        boolean testBit(int n) { return false; }
        byte [] toByteArray() { return null; }
        //override from java.lang.Object
        public String toString() { return null; }
        String toString(int radix) { return null; }
        static BigInteger valueOf(long val) { return null; }
        BigInteger xor(BigInteger val) { return null; }
    }

    //public final class MathContext implements Serializable
    @ImmutableContainer
    class MathContext$ {
        static final MathContext DECIMAL128 = null;
        static final MathContext DECIMAL32 = null;
        static final MathContext DECIMAL64 = null;
        static final MathContext UNLIMITED = null;
        MathContext$(String val) { }
        MathContext$(int setPrecision) { }
        MathContext$(int setPrecision, RoundingMode setRoundingMode) { }
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
