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

package io.codelaser.maddi.aapi.archive.jdk;
import java.util.random.RandomGenerator;
import java.util.stream.*;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotNull;
public class JavaUtilRandom {
    public static final String PACKAGE_NAME = "java.util.random";
    //public interface RandomGenerator
    class RandomGenerator$ {
        //public interface ArbitrarilyJumpableGenerator implements RandomGenerator.LeapableGenerator
        class ArbitrarilyJumpableGenerator {
            //override from java.util.random.RandomGenerator.JumpableGenerator, java.util.random.RandomGenerator.LeapableGenerator, java.util.random.RandomGenerator.StreamableGenerator
            static RandomGenerator.ArbitrarilyJumpableGenerator of(@NotNull String name) { return null; }

            //override from java.util.random.RandomGenerator.JumpableGenerator, java.util.random.RandomGenerator.LeapableGenerator
            RandomGenerator.ArbitrarilyJumpableGenerator copy() { return null; }
            void jumpPowerOfTwo(int i) { }
            void jump(double d) { }
            //override from java.util.random.RandomGenerator.JumpableGenerator
            void jump() { }
            Stream<RandomGenerator.ArbitrarilyJumpableGenerator> jumps(double distance) { return null; }
            Stream<RandomGenerator.ArbitrarilyJumpableGenerator> jumps(long streamSize, double d) { return null; }
            //override from java.util.random.RandomGenerator.LeapableGenerator
            void leap() { }
            RandomGenerator.ArbitrarilyJumpableGenerator copyAndJump(double distance) { return null; }
        }

        //public interface JumpableGenerator implements RandomGenerator.StreamableGenerator
        class JumpableGenerator {
            //override from java.util.random.RandomGenerator.StreamableGenerator
            static RandomGenerator.JumpableGenerator of(@NotNull String name) { return null; }
            RandomGenerator.JumpableGenerator copy() { return null; }
            void jump() { }
            double jumpDistance() { return 0.0; }
            Stream<RandomGenerator> jumps() { return null; }
            Stream<RandomGenerator> jumps(long streamSize) { return null; }
            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs() { return null; }

            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs(long streamSize) { return null; }
            RandomGenerator copyAndJump() { return null; }
        }

        //public interface LeapableGenerator implements RandomGenerator.JumpableGenerator
        class LeapableGenerator {
            //override from java.util.random.RandomGenerator.JumpableGenerator, java.util.random.RandomGenerator.StreamableGenerator
            static RandomGenerator.LeapableGenerator of(@NotNull String name) { return null; }

            //override from java.util.random.RandomGenerator.JumpableGenerator
            RandomGenerator.LeapableGenerator copy() { return null; }
            void leap() { }
            double leapDistance() { return 0.0; }
            Stream<RandomGenerator.JumpableGenerator> leaps() { return null; }
            Stream<RandomGenerator.JumpableGenerator> leaps(long streamSize) { return null; }
            RandomGenerator.JumpableGenerator copyAndLeap() { return null; }
        }

        //public interface SplittableGenerator implements RandomGenerator.StreamableGenerator
        class SplittableGenerator {
            //override from java.util.random.RandomGenerator.StreamableGenerator
            static RandomGenerator.SplittableGenerator of(@NotNull String name) { return null; }
            RandomGenerator.SplittableGenerator split() { return null; }
            RandomGenerator.SplittableGenerator split(@NotNull RandomGenerator.SplittableGenerator splittableGenerator) {
                return null;
            }
            Stream<RandomGenerator.SplittableGenerator> splits() { return null; }
            Stream<RandomGenerator.SplittableGenerator> splits(long l) { return null; }
            Stream<RandomGenerator.SplittableGenerator> splits(@NotNull RandomGenerator.SplittableGenerator splittableGenerator) {
                return null;
            }

            Stream<RandomGenerator.SplittableGenerator> splits(
                long l,
                @NotNull RandomGenerator.SplittableGenerator splittableGenerator) { return null; }

            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs() { return null; }

            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs(long streamSize) { return null; }
        }

        //public interface StreamableGenerator implements RandomGenerator
        class StreamableGenerator {
            static RandomGenerator.StreamableGenerator of(@NotNull String name) { return null; }
            Stream<RandomGenerator> rngs() { return null; }
            Stream<RandomGenerator> rngs(long streamSize) { return null; }
        }
        static RandomGenerator of(@NotNull String name) { return null; }
        static RandomGenerator getDefault() { return null; }
        boolean isDeprecated() { return false; }
        DoubleStream doubles() { return null; }
        DoubleStream doubles(double randomNumberOrigin, double d) { return null; }
        DoubleStream doubles(long streamSize) { return null; }
        DoubleStream doubles(long streamSize, double d, double randomNumberOrigin) { return null; }
        DoubleStream equiDoubles(double left, double d, boolean right, boolean b) { return null; }
        IntStream ints() { return null; }
        IntStream ints(int randomNumberOrigin, int randomNumberBound) { return null; }
        IntStream ints(long streamSize) { return null; }
        IntStream ints(long streamSize, int i, int randomNumberOrigin) { return null; }
        LongStream longs() { return null; }
        LongStream longs(long randomNumberOrigin, long l) { return null; }
        LongStream longs(long streamSize) { return null; }
        LongStream longs(long streamSize, long l, long randomNumberOrigin) { return null; }
        @Modified boolean nextBoolean() { return false; }
        //override has frequency 1
        @Modified void nextBytes(@NotNull byte [] bytes) { }
        @Modified float nextFloat() { return 0.0F; }
        @Modified float nextFloat(float bound) { return 0.0F; }
        @Modified float nextFloat(float origin, float bound) { return 0.0F; }
        //override has frequency 1
        @Modified double nextDouble() { return 0.0; }
        @Modified double nextDouble(double bound) { return 0.0; }
        @Modified double nextDouble(double origin, double d) { return 0.0; }
        @Modified int nextInt() { return 0; }
        //override has frequency 3
        @Modified int nextInt(int bound) { return 0; }
        @Modified int nextInt(int origin, int bound) { return 0; }
        @Modified long nextLong() { return 0L; }
        @Modified long nextLong(long bound) { return 0L; }
        @Modified long nextLong(long origin, long l) { return 0L; }
        @Modified double nextGaussian() { return 0.0; }
        @Modified double nextGaussian(double mean, double d) { return 0.0; }
        @Modified double nextExponential() { return 0.0; }
    }

    //public java.util.random.RandomGeneratorFactory (shadow created for its nullability, generated)
    class RandomGeneratorFactory$<T extends java.util.random.RandomGenerator> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        T create(@NotNull byte[] p0) { return null; }
        static <T extends java.util.random.RandomGenerator> java.util.random.RandomGeneratorFactory<T> of(@NotNull java.lang.String p0) { return null; }
    }
}
