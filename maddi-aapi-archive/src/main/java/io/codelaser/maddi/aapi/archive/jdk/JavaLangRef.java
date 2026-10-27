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

import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaLangRef {
    public static final String PACKAGE_NAME = "java.lang.ref";

    class Reference$<T> {
        @Nullable T get() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static void reachabilityFence(@Nullable java.lang.Object p0) { }
        boolean refersTo(@Nullable T p0) { return false; }
    }

    //public java.lang.ref.Cleaner (shadow created for its nullability, generated)
    class Cleaner$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.lang.ref.Cleaner create(@NotNull java.util.concurrent.ThreadFactory p0) { return null; }
        java.lang.ref.Cleaner.Cleanable register(@NotNull java.lang.Object p0, @NotNull java.lang.Runnable p1) { return null; }
    }

    //public java.lang.ref.PhantomReference (shadow created for its nullability, generated)
    class PhantomReference$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PhantomReference$(@Nullable T p0, @Nullable java.lang.ref.ReferenceQueue<? super T> p1) { }
        @Nullable T get() { return null; }
    }

    //public java.lang.ref.ReferenceQueue (shadow created for its nullability, generated)
    class ReferenceQueue$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.ref.Reference<? extends T> poll() { return null; }
        @Nullable java.lang.ref.Reference<? extends T> remove(long p0) { return null; }
    }

    //public java.lang.ref.SoftReference (shadow created for its nullability, generated)
    class SoftReference$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SoftReference$(@Nullable T p0) { }
        SoftReference$(@Nullable T p0, @Nullable java.lang.ref.ReferenceQueue<? super T> p1) { }
        @Nullable T get() { return null; }
    }

    //public java.lang.ref.WeakReference (shadow created for its nullability, generated)
    class WeakReference$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        WeakReference$(@Nullable T p0) { }
        WeakReference$(@Nullable T p0, @Nullable java.lang.ref.ReferenceQueue<? super T> p1) { }
    }
}
