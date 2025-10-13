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

package org.e2immu.support;

public class EventuallyFinalOnDemand<T> {
    private T value;
    private boolean isFinal;
    private volatile Runnable onDemand;

    // not synchronized: we'll be calling get() from inside the runnable
    // it is important that only setFinal can clear onDemand, and set isFinal at the same time
    // no value should be returned as long as onDemand != null
    public T get() {
        Runnable runnable = onDemand;
        if (runnable != null) {
            runnable.run();
        }
        return value;
    }

    public void setFinal(T value) {
        if (this.isFinal) {
            throw new IllegalStateException("Trying to overwrite final value");
        }
        this.isFinal = true;
        this.value = value;
        this.onDemand = null;
    }

    public void setVariable(T value) {
        if (this.isFinal) throw new IllegalStateException("Value is already final");
        this.value = value;
    }

    public void setOnDemand(Runnable onDemand) {
        assert !isFinal && this.onDemand == null;
        this.onDemand = onDemand;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isVariable() {
        return !isFinal;
    }

    public boolean haveOnDemand() {
        return onDemand != null;
    }
}
