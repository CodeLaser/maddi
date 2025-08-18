package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.util.function.*;
import org.e2immu.annotation.Independent;

public class JavaUtilConcurrentAtomic {
    public static final String PACKAGE_NAME = "java.util.concurrent.atomic";
    //public class AtomicBoolean implements Serializable
    @Independent
    class AtomicBoolean$ {
        //frequency 2
        AtomicBoolean$(boolean initialValue) { }
        AtomicBoolean$() { }
        //frequency 1
        boolean get() { return false; }
        boolean compareAndSet(boolean expectedValue, boolean newValue) { return false; }
        boolean weakCompareAndSet(boolean expectedValue, boolean newValue) { return false; }
        boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue) { return false; }
        //frequency 2
        void set(boolean newValue) { }
        void lazySet(boolean newValue) { }
        boolean getAndSet(boolean newValue) { return false; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        boolean getPlain() { return false; }
        void setPlain(boolean newValue) { }
        boolean getOpaque() { return false; }
        void setOpaque(boolean newValue) { }
        boolean getAcquire() { return false; }
        void setRelease(boolean newValue) { }
        boolean compareAndExchange(boolean expectedValue, boolean newValue) { return false; }
        boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) { return false; }
        boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) { return false; }
        boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) { return false; }
        boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue) { return false; }
        boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue) { return false; }
    }

    //public class AtomicInteger extends Number implements Serializable
    class AtomicInteger$ {
        AtomicInteger$(int initialValue) { }
        //frequency 2
        AtomicInteger$() { }

        //frequency 4
        int get() { return 0; }
        void set(int newValue) { }
        void lazySet(int newValue) { }
        int getAndSet(int newValue) { return 0; }
        boolean compareAndSet(int expectedValue, int newValue) { return false; }
        boolean weakCompareAndSet(int expectedValue, int newValue) { return false; }
        boolean weakCompareAndSetPlain(int expectedValue, int newValue) { return false; }
        int getAndIncrement() { return 0; }
        int getAndDecrement() { return 0; }
        int getAndAdd(int delta) { return 0; }
        //frequency 1
        int incrementAndGet() { return 0; }
        int decrementAndGet() { return 0; }
        //frequency 1
        int addAndGet(int delta) { return 0; }
        int getAndUpdate(/*@IgnoreModifications[T]*/ IntUnaryOperator updateFunction) { return 0; }
        int updateAndGet(/*@IgnoreModifications[T]*/ IntUnaryOperator updateFunction) { return 0; }
        int getAndAccumulate(int x, /*@IgnoreModifications[T]*/ IntBinaryOperator accumulatorFunction) { return 0; }
        int accumulateAndGet(int x, /*@IgnoreModifications[T]*/ IntBinaryOperator accumulatorFunction) { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Number
        //@NotModified[H]
        int intValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[H]
        long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[H]
        float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[H]
        double doubleValue() { return 0.0; }
        int getPlain() { return 0; }
        void setPlain(int newValue) { }
        int getOpaque() { return 0; }
        void setOpaque(int newValue) { }
        int getAcquire() { return 0; }
        void setRelease(int newValue) { }
        int compareAndExchange(int expectedValue, int newValue) { return 0; }
        int compareAndExchangeAcquire(int expectedValue, int newValue) { return 0; }
        int compareAndExchangeRelease(int expectedValue, int newValue) { return 0; }
        boolean weakCompareAndSetVolatile(int expectedValue, int newValue) { return false; }
        boolean weakCompareAndSetAcquire(int expectedValue, int newValue) { return false; }
        boolean weakCompareAndSetRelease(int expectedValue, int newValue) { return false; }
    }

    //public class AtomicReference implements Serializable
    class AtomicReference$<V> {
        //frequency 1
        AtomicReference$(/*@Independent(hc=true)[T] @NotModified[T]*/ V initialValue) { }
        AtomicReference$() { }
        //frequency 2
        //@Independent(hc=true)[T]
        V get() { return null; }
        void set(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        void lazySet(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        boolean compareAndSet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        boolean weakCompareAndSet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        boolean weakCompareAndSetPlain(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        //@Independent(hc=true)[T]
        V getAndSet(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        V getAndUpdate(/*@IgnoreModifications[T]*/ UnaryOperator<V> updateFunction) { return null; }

        //@Independent(hc=true)[T]
        V updateAndGet(/*@IgnoreModifications[T]*/ UnaryOperator<V> updateFunction) { return null; }

        //@Independent(hc=true)[T]
        V getAndAccumulate(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V x,
            /*@IgnoreModifications[T]*/ BinaryOperator<V> accumulatorFunction) { return null; }

        //@Independent(hc=true)[T]
        V accumulateAndGet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V x,
            /*@IgnoreModifications[T]*/ BinaryOperator<V> accumulatorFunction) { return null; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //@Independent(hc=true)[T]
        V getPlain() { return null; }
        void setPlain(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        V getOpaque() { return null; }
        void setOpaque(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        V getAcquire() { return null; }
        void setRelease(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        V compareAndExchange(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        V compareAndExchangeAcquire(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        V compareAndExchangeRelease(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        boolean weakCompareAndSetVolatile(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        boolean weakCompareAndSetAcquire(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        boolean weakCompareAndSetRelease(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }
    }
}
