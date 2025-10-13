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

import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class JavaUtilRandom {
    public static final String PACKAGE_NAME = "java.util.random";

    @Independent
    interface RandomGenerator$ {

        void nextBytes(@Modified byte[] bytes);

        @Independent
        interface ArbitrarilyJumpableGenerator {
            @Independent
            ArbitrarilyJumpableGenerator of(String name);

            @Independent
            ArbitrarilyJumpableGenerator copy();
            @Independent
            void jump();

            @Independent
            void leap();
        }

        @Independent
        interface JumpableGenerator {
            @Independent
            ArbitrarilyJumpableGenerator of(String name);

            @Independent
            ArbitrarilyJumpableGenerator copy();

            @Independent
            void jump();

            @Independent
            Stream<RandomGenerator> rngs();
            @Independent
            Stream<RandomGenerator> rngs(long l);
        }

        @Independent
        interface LeapableGenerator {
            @Independent
            ArbitrarilyJumpableGenerator of(String name);

            @Independent
            ArbitrarilyJumpableGenerator copy();

            @Independent
            void leap();
        }

        @Independent
        interface StreamableGenerator {
            @Independent
            ArbitrarilyJumpableGenerator of(String name);

            @Independent
            Stream<RandomGenerator> rngs();
            @Independent
            Stream<RandomGenerator> rngs(long l);
        }

        @Independent
        interface SplittableGenerator {
            @Independent
            ArbitrarilyJumpableGenerator of(String name);

            @Independent
            Stream<RandomGenerator> rngs();
            @Independent
            Stream<RandomGenerator> rngs(long l);
        }
    }
}
