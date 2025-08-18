package org.e2immu.analyzer.aapi.archive.v2.libs.test;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.e2immu.annotation.Immutable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class OrgJunitJupiterApi {
    public static final String PACKAGE_NAME = "org.junit.jupiter.api";
    //public class Assertions
    @Immutable
    class Assertions$ {
        //public interface TimeoutFailureFactory
        class TimeoutFailureFactory<T extends Throwable> {
            T createTimeoutFailure(
                /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration duration,
                /*@IgnoreModifications[T]*/ Supplier<String> supplier,
                Throwable throwable) { return null; }
        }

        //@Independent[T]
        static <V> V fail() { return null; }

        //@Independent[T]
        static <V> V fail(String message) { return null; }

        //@Independent[T]
        static <V> V fail(String message, Throwable cause) { return null; }

        //@Independent[T]
        static <V> V fail(Throwable cause) { return null; }

        //@Independent[T]
        static <V> V fail(/*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }
        static void assertTrue(boolean condition) { }
        static void assertTrue(boolean condition, /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertTrue(/*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier) { }
        static void assertTrue(/*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier, String message) { }
        static void assertTrue(boolean condition, String message) { }
        static void assertTrue(
            /*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertFalse(boolean condition) { }
        static void assertFalse(boolean condition, String message) { }
        static void assertFalse(boolean condition, /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertFalse(/*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier) { }
        static void assertFalse(/*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier, String message) { }
        static void assertFalse(
            /*@IgnoreModifications[T]*/ BooleanSupplier booleanSupplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNull(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }
        static void assertNull(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertNull(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotNull(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }

        static void assertNotNull(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertNotNull(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(short expected, short actual) { }
        static void assertEquals(short expected, Short actual) { }
        static void assertEquals(Short expected, short actual) { }
        static void assertEquals(Short expected, Short actual) { }
        static void assertEquals(short expected, short actual, String message) { }
        static void assertEquals(short expected, Short actual, String message) { }
        static void assertEquals(Short expected, short actual, String message) { }
        static void assertEquals(Short expected, Short actual, String message) { }
        static void assertEquals(
            short expected,
            short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            short expected,
            Short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Short expected,
            short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Short expected,
            Short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(byte expected, byte actual) { }
        static void assertEquals(byte expected, Byte actual) { }
        static void assertEquals(Byte expected, byte actual) { }
        static void assertEquals(Byte expected, Byte actual) { }
        static void assertEquals(byte expected, byte actual, String message) { }
        static void assertEquals(byte expected, Byte actual, String message) { }
        static void assertEquals(Byte expected, byte actual, String message) { }
        static void assertEquals(Byte expected, Byte actual, String message) { }
        static void assertEquals(
            byte expected,
            byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            byte expected,
            Byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Byte expected,
            byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Byte expected,
            Byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(int expected, int actual) { }
        static void assertEquals(int expected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual) { }
        static void assertEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected, int actual) { }
        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual) { }
        static void assertEquals(int expected, int actual, String message) { }
        static void assertEquals(
            int expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            String message) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected,
            int actual,
            String message) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            String message) { }

        static void assertEquals(int expected, int actual, /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            int expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected,
            int actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(long expected, long l) { }
        static void assertEquals(long expected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0) { }
        static void assertEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected, long actual) { }
        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual) { }
        static void assertEquals(long expected, long l, String actual) { }
        static void assertEquals(
            long expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0,
            String actual) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected,
            long actual,
            String string) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual,
            String message) { }
        static void assertEquals(long expected, long l, /*@IgnoreModifications[T]*/ Supplier<String> actual) { }
        static void assertEquals(
            long expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0,
            /*@IgnoreModifications[T]*/ Supplier<String> actual) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected,
            long actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(float expected, float actual) { }
        static void assertEquals(float expected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual) { }
        static void assertEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected, float actual) { }
        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual) { }
        static void assertEquals(float expected, float actual, String message) { }
        static void assertEquals(
            float expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            String message) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected,
            float actual,
            String message) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            String message) { }

        static void assertEquals(
            float expected,
            float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            float expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected,
            float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(float expected, float actual, float delta) { }
        static void assertEquals(float expected, float actual, float delta, String message) { }
        static void assertEquals(
            float expected,
            float actual,
            float delta,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(double expected, double d) { }
        static void assertEquals(double expected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0) { }
        static void assertEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected, double actual) { }
        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual) { }
        static void assertEquals(double expected, double d, String actual) { }
        static void assertEquals(
            double expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0,
            String actual) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected,
            double actual,
            String string) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual,
            String message) { }
        static void assertEquals(double expected, double d, /*@IgnoreModifications[T]*/ Supplier<String> actual) { }
        static void assertEquals(
            double expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0,
            /*@IgnoreModifications[T]*/ Supplier<String> actual) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected,
            double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }

        static void assertEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double expected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertEquals(double expected, double d, double actual) { }
        static void assertEquals(double expected, double d, double actual, String string) { }
        static void assertEquals(
            double expected,
            double d,
            double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }
        static void assertEquals(char expected, char actual) { }
        static void assertEquals(char expected, Character actual) { }
        static void assertEquals(Character expected, char actual) { }
        static void assertEquals(Character expected, Character actual) { }
        static void assertEquals(char expected, char actual, String message) { }
        static void assertEquals(char expected, Character actual, String message) { }
        static void assertEquals(Character expected, char actual, String message) { }
        static void assertEquals(Character expected, Character actual, String message) { }
        static void assertEquals(
            char expected,
            char actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            char expected,
            Character actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Character expected,
            char actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            Character expected,
            Character actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }

        static void assertEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(boolean [] expected, boolean [] actual) { }
        static void assertArrayEquals(boolean [] expected, boolean [] actual, String message) { }
        static void assertArrayEquals(
            boolean [] expected,
            boolean [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(char [] expected, char [] actual) { }
        static void assertArrayEquals(char [] expected, char [] actual, String message) { }
        static void assertArrayEquals(
            char [] expected,
            char [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(byte [] expected, byte [] actual) { }
        static void assertArrayEquals(byte [] expected, byte [] actual, String message) { }
        static void assertArrayEquals(
            byte [] expected,
            byte [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(short [] expected, short [] actual) { }
        static void assertArrayEquals(short [] expected, short [] actual, String message) { }
        static void assertArrayEquals(
            short [] expected,
            short [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(int [] expected, int [] actual) { }
        static void assertArrayEquals(int [] expected, int [] actual, String message) { }
        static void assertArrayEquals(
            int [] expected,
            int [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(long [] expected, long [] actual) { }
        static void assertArrayEquals(long [] expected, long [] actual, String message) { }
        static void assertArrayEquals(
            long [] expected,
            long [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(float [] expected, float [] actual) { }
        static void assertArrayEquals(float [] expected, float [] actual, String message) { }
        static void assertArrayEquals(
            float [] expected,
            float [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(float [] expected, float [] actual, float delta) { }
        static void assertArrayEquals(float [] expected, float [] actual, float delta, String message) { }
        static void assertArrayEquals(
            float [] expected,
            float [] actual,
            float delta,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(double [] expected, double [] actual) { }
        static void assertArrayEquals(double [] expected, double [] actual, String message) { }
        static void assertArrayEquals(
            double [] expected,
            double [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertArrayEquals(double [] expected, double [] actual, double delta) { }
        static void assertArrayEquals(double [] expected, double [] actual, double delta, String string) { }
        static void assertArrayEquals(
            double [] expected,
            double [] actual,
            double delta,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }
        static void assertArrayEquals(Object [] expected, Object [] actual) { }
        static void assertArrayEquals(Object [] expected, Object [] actual, String message) { }
        static void assertArrayEquals(
            Object [] expected,
            Object [] actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual) { }
        static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual, String message) { }
        static void assertIterableEquals(
            Iterable<?> expected,
            Iterable<?> actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertLinesMatch(List<String> expectedLines, List<String> actualLines) { }
        static void assertLinesMatch(List<String> expectedLines, List<String> actualLines, String message) { }
        static void assertLinesMatch(
            List<String> expectedLines,
            List<String> actualLines,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertLinesMatch(Stream<String> expectedLines, Stream<String> actualLines) { }
        static void assertLinesMatch(Stream<String> expectedLines, Stream<String> actualLines, String message) { }
        static void assertLinesMatch(
            Stream<String> expectedLines,
            Stream<String> actualLines,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(byte unexpected, byte actual) { }
        static void assertNotEquals(byte unexpected, Byte actual) { }
        static void assertNotEquals(Byte unexpected, byte actual) { }
        static void assertNotEquals(Byte unexpected, Byte actual) { }
        static void assertNotEquals(byte unexpected, byte actual, String message) { }
        static void assertNotEquals(byte unexpected, Byte actual, String message) { }
        static void assertNotEquals(Byte unexpected, byte actual, String message) { }
        static void assertNotEquals(Byte unexpected, Byte actual, String message) { }
        static void assertNotEquals(
            byte unexpected,
            byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            byte unexpected,
            Byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Byte unexpected,
            byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Byte unexpected,
            Byte actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(short unexpected, short actual) { }
        static void assertNotEquals(short unexpected, Short actual) { }
        static void assertNotEquals(Short unexpected, short actual) { }
        static void assertNotEquals(Short unexpected, Short actual) { }
        static void assertNotEquals(short unexpected, short actual, String message) { }
        static void assertNotEquals(short unexpected, Short actual, String message) { }
        static void assertNotEquals(Short unexpected, short actual, String message) { }
        static void assertNotEquals(Short unexpected, Short actual, String message) { }
        static void assertNotEquals(
            short unexpected,
            short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            short unexpected,
            Short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Short unexpected,
            short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Short unexpected,
            Short actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(int unexpected, int actual) { }
        static void assertNotEquals(int unexpected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual) { }

        static void assertNotEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected, int actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual) { }
        static void assertNotEquals(int unexpected, int actual, String message) { }
        static void assertNotEquals(
            int unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            String message) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected,
            int actual,
            String message) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            String message) { }

        static void assertNotEquals(
            int unexpected,
            int actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            int unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected,
            int actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Integer actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(long unexpected, long l) { }
        static void assertNotEquals(long unexpected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0) { }
        static void assertNotEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected, long actual) { }
        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual) { }
        static void assertNotEquals(long unexpected, long l, String actual) { }
        static void assertNotEquals(
            long unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0,
            String actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected,
            long actual,
            String string) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual,
            String message) { }
        static void assertNotEquals(long unexpected, long l, /*@IgnoreModifications[T]*/ Supplier<String> actual) { }
        static void assertNotEquals(
            long unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long long0,
            /*@IgnoreModifications[T]*/ Supplier<String> actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected,
            long actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Long actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(float unexpected, float actual) { }
        static void assertNotEquals(float unexpected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual) { }

        static void assertNotEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected, float actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual) { }
        static void assertNotEquals(float unexpected, float actual, String message) { }
        static void assertNotEquals(
            float unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            String message) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected,
            float actual,
            String message) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            String message) { }

        static void assertNotEquals(
            float unexpected,
            float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            float unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected,
            float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Float actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(float unexpected, float actual, float delta) { }
        static void assertNotEquals(float unexpected, float actual, float delta, String message) { }
        static void assertNotEquals(
            float unexpected,
            float actual,
            float delta,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(double unexpected, double d) { }
        static void assertNotEquals(double unexpected, /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0) { }

        static void assertNotEquals(/*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected, double actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual) { }
        static void assertNotEquals(double unexpected, double d, String actual) { }
        static void assertNotEquals(
            double unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0,
            String actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected,
            double actual,
            String string) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual,
            String message) { }

        static void assertNotEquals(double unexpected, double d, /*@IgnoreModifications[T]*/ Supplier<String> actual) { }

        static void assertNotEquals(
            double unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double double0,
            /*@IgnoreModifications[T]*/ Supplier<String> actual) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected,
            double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }

        static void assertNotEquals(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double unexpected,
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertNotEquals(double unexpected, double d, double actual) { }
        static void assertNotEquals(double unexpected, double d, double actual, String string) { }
        static void assertNotEquals(
            double unexpected,
            double d,
            double actual,
            /*@IgnoreModifications[T]*/ Supplier<String> supplier) { }
        static void assertNotEquals(char unexpected, char actual) { }
        static void assertNotEquals(char unexpected, Character actual) { }
        static void assertNotEquals(Character unexpected, char actual) { }
        static void assertNotEquals(Character unexpected, Character actual) { }
        static void assertNotEquals(char unexpected, char actual, String message) { }
        static void assertNotEquals(char unexpected, Character actual, String message) { }
        static void assertNotEquals(Character unexpected, char actual, String message) { }
        static void assertNotEquals(Character unexpected, Character actual, String message) { }
        static void assertNotEquals(
            char unexpected,
            char actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            char unexpected,
            Character actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Character unexpected,
            char actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            Character unexpected,
            Character actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }

        static void assertNotEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertNotEquals(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }

        static void assertSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object expected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        static void assertNotSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual) { }

        static void assertNotSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            String message) { }

        static void assertNotSame(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object unexpected,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actual,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }
        static void assertAll(Executable ... executables) { }
        static void assertAll(String heading, Executable ... executables) { }
        static void assertAll(Collection<Executable> executables) { }
        static void assertAll(String heading, Collection<Executable> executables) { }
        static void assertAll(Stream<Executable> executables) { }
        static void assertAll(String heading, Stream<Executable> executables) { }
        //@Independent[T]
        static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType, Executable executable) { return null; }

        //@Independent[T]
        static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType, Executable executable, String message) {
            return null;
        }

        //@Independent[T]
        static <T extends Throwable> T assertThrowsExactly(
            Class<T> expectedType,
            Executable executable,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }

        //@Independent[T]
        static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) { return null; }

        //@Independent[T]
        static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, String message) {
            return null;
        }

        //@Independent[T]
        static <T extends Throwable> T assertThrows(
            Class<T> expectedType,
            Executable executable,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }
        static void assertDoesNotThrow(Executable executable) { }
        static void assertDoesNotThrow(Executable executable, String message) { }
        static void assertDoesNotThrow(
            Executable executable,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        //@Independent[T]
        static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier) { return null; }

        //@Independent[T]
        static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier, String message) { return null; }

        //@Independent[T]
        static <T> T assertDoesNotThrow(
            ThrowingSupplier<T> supplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }

        static void assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable) { }

        static void assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable,
            String message) { }

        static void assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        //@Independent[T]
        static <T> T assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier) { return null; }

        //@Independent[T]
        static <T> T assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier,
            String message) { return null; }

        //@Independent[T]
        static <T> T assertTimeout(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }

        static void assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable) { }

        static void assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable,
            String message) { }

        static void assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            Executable executable,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { }

        //@Independent[T]
        static <T> T assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier) { return null; }

        //@Independent[T]
        static <T> T assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier,
            String message) { return null; }

        //@Independent[T]
        static <T> T assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }

        //@Independent[T]
        static <T, E extends Throwable> T assertTimeoutPreemptively(
            /*@Immutable[T] @Independent[T] @NotModified[T]*/ Duration timeout,
            ThrowingSupplier<T> supplier,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier,
            Assertions.TimeoutFailureFactory<E> failureFactory) { return null; }

        //@Independent[T]
        static <T> T assertInstanceOf(
            Class<T> expectedType,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actualValue) { return null; }

        //@Independent[T]
        static <T> T assertInstanceOf(
            Class<T> expectedType,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actualValue,
            String message) { return null; }

        //@Independent[T]
        static <T> T assertInstanceOf(
            Class<T> expectedType,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object actualValue,
            /*@IgnoreModifications[T]*/ Supplier<String> messageSupplier) { return null; }
    }
}
