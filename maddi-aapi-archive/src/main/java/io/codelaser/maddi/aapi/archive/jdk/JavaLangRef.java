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
