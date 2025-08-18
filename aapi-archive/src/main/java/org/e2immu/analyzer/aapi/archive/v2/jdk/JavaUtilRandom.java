package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.util.random.RandomGenerator;
import java.util.stream.*;
public class JavaUtilRandom {
    public static final String PACKAGE_NAME = "java.util.random";
    //public interface RandomGenerator
    class RandomGenerator$ {
        //public interface ArbitrarilyJumpableGenerator implements RandomGenerator.LeapableGenerator
        class ArbitrarilyJumpableGenerator {
            //override from java.util.random.RandomGenerator.JumpableGenerator, java.util.random.RandomGenerator.LeapableGenerator, java.util.random.RandomGenerator.StreamableGenerator
            static RandomGenerator.ArbitrarilyJumpableGenerator of(String name) { return null; }

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
            static RandomGenerator.JumpableGenerator of(String name) { return null; }
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
            static RandomGenerator.LeapableGenerator of(String name) { return null; }

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
            static RandomGenerator.SplittableGenerator of(String name) { return null; }
            RandomGenerator.SplittableGenerator split() { return null; }
            RandomGenerator.SplittableGenerator split(RandomGenerator.SplittableGenerator splittableGenerator) {
                return null;
            }
            Stream<RandomGenerator.SplittableGenerator> splits() { return null; }
            Stream<RandomGenerator.SplittableGenerator> splits(long l) { return null; }
            Stream<RandomGenerator.SplittableGenerator> splits(RandomGenerator.SplittableGenerator splittableGenerator) {
                return null;
            }

            Stream<RandomGenerator.SplittableGenerator> splits(
                long l,
                RandomGenerator.SplittableGenerator splittableGenerator) { return null; }

            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs() { return null; }

            //override from java.util.random.RandomGenerator.StreamableGenerator
            Stream<RandomGenerator> rngs(long streamSize) { return null; }
        }

        //public interface StreamableGenerator implements RandomGenerator
        class StreamableGenerator {
            static RandomGenerator.StreamableGenerator of(String name) { return null; }
            Stream<RandomGenerator> rngs() { return null; }
            Stream<RandomGenerator> rngs(long streamSize) { return null; }
        }
        static RandomGenerator of(String name) { return null; }
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
        boolean nextBoolean() { return false; }
        //override has frequency 1
        void nextBytes(byte [] bytes) { }
        float nextFloat() { return 0.0F; }
        float nextFloat(float bound) { return 0.0F; }
        float nextFloat(float origin, float bound) { return 0.0F; }
        //override has frequency 1
        double nextDouble() { return 0.0; }
        double nextDouble(double bound) { return 0.0; }
        double nextDouble(double origin, double d) { return 0.0; }
        int nextInt() { return 0; }
        //override has frequency 3
        int nextInt(int bound) { return 0; }
        int nextInt(int origin, int bound) { return 0; }
        long nextLong() { return 0L; }
        long nextLong(long bound) { return 0L; }
        long nextLong(long origin, long l) { return 0L; }
        double nextGaussian() { return 0.0; }
        double nextGaussian(double mean, double d) { return 0.0; }
        double nextExponential() { return 0.0; }
    }
}
