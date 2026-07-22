/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.support;

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;

/**
 * As {@link EventuallyFinal}, but the <em>before</em> state may carry a loader that produces the final value on
 * first access. The state transition is the same one: {@code isFinal}.
 */
@ImmutableContainer(after = "isFinal", hc = true)
public class EventuallyFinalOnDemand<T> {
    private volatile T value;
    private volatile boolean isFinal;
    private volatile Runnable onDemand;

    // Concurrency: the on-demand loader (lazy bytecode inspection) can be first-touched by several analyzer
    // threads at once; unsynchronized, both would run it and the second setFinal throws. The monitor is
    // reentrant, so the loader calling get() from inside run() (same thread) still works as in the original
    // unsynchronized design. It is important that only setFinal can clear onDemand, and set isFinal at the
    // same time; no value should be returned as long as onDemand != null.
    @NotModified(after = "isFinal")
    public T get() {
        if (isFinal) return value; // fast path: a committed value never changes again
        Runnable runnable = onDemand;
        if (runnable != null) {
            synchronized (this) {
                Runnable again = onDemand; // may have been cleared while we waited for the monitor
                if (again != null) {
                    again.run();
                }
            }
        }
        return value;
    }

    @Mark("isFinal")
    public synchronized void setFinal(T value) {
        if (this.isFinal) {
            throw new IllegalStateException("Trying to overwrite final value");
        }
        this.value = value;
        this.isFinal = true;
        this.onDemand = null;
    }

    @Only(before = "isFinal")
    public synchronized void setVariable(T value) {
        if (this.isFinal) throw new IllegalStateException("Value is already final");
        this.value = value;
    }

    @Only(before = "isFinal")
    public synchronized void setOnDemand(Runnable onDemand) {
        assert !isFinal && this.onDemand == null;
        this.onDemand = onDemand;
    }

    @TestMark("isFinal")
    public boolean isFinal() {
        return isFinal;
    }

    @TestMark(value = "isFinal", before = true)
    public boolean isVariable() {
        return !isFinal;
    }

    public boolean haveOnDemand() {
        return onDemand != null;
    }
}
