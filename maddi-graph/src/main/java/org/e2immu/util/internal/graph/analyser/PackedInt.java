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

package org.e2immu.util.internal.graph.analyser;

public enum PackedInt {

    // keep these in the order they appear! .ordinal() is being used.
    STATIC_METHOD_CALL_OR_ANNOTATION('A'),
    EXPRESSION('E'),
    STATIC_METHOD('S'),
    METHOD('M'),
    FIELD('F'),
    HIERARCHY('H');

    public final char code;

    PackedInt(char code) {
        this.code = code;
    }

    public int of(int i) {
        return Math.min(i, GROUP_MASK) << (BITS_PER_GROUP * ordinal());
    }

    public static final int GROUPS = PackedInt.values().length;
    public static final int BITS_PER_GROUP = 5; // 6 groups * 5 bits < 32!
    public static final int MAX_PER_GROUP = 1 << BITS_PER_GROUP;
    private static final int GROUP_MASK = MAX_PER_GROUP - 1;
    private static final int BITS = GROUPS * BITS_PER_GROUP;

    public static int sum(int i1, int i2) {
        int sum = 0;
        for (int shift = 0; shift < BITS; shift += BITS_PER_GROUP) {
            int v1 = (i1 >> shift) & GROUP_MASK;
            int v2 = (i2 >> shift) & GROUP_MASK;
            int s = Math.min(v1 + v2, GROUP_MASK);
            sum += (s << shift);
        }
        return sum;
    }

    public static long longSum(long i1, long i2) {
        long sum = 0;
        for (int shift = 0; shift < BITS; shift += BITS_PER_GROUP) {
            long v1 = (i1 >> shift) & GROUP_MASK;
            long v2 = (i2 >> shift) & GROUP_MASK;
            long s = Math.min(v1 + v2, GROUP_MASK);
            sum += (s << shift);
        }
        return sum;
    }

    public static String nice(int i) {
        if (i == 0) return "0";
        StringBuilder sb = new StringBuilder();
        boolean empty = true;
        for (int g = GROUPS - 1; g >= 0; g--) {
            int shift = BITS_PER_GROUP * g;
            int v = (i >> shift) & GROUP_MASK;
            if (v > 0) {
                if (empty) {
                    empty = false;
                } else {
                    sb.append(' ');
                }
                sb.append(values()[g].code);
                sb.append(":");
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
