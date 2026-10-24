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

package io.codelaser.maddi.cst.impl.expression.eval;

import java.util.Set;

/**
 * The boolean evaluator's A/B gates ({@code NOSTRUCTNEG}, {@code NOSCOPEDNEGMEMO}, {@code NOSORTKEYS}). A gate is
 * off when its environment variable is set, or while a caller has switched it off ({@link #off(Set)}), so one JVM
 * can measure both sides. The same mechanism as the nullability pass's {@code Switches} in maddi-mod.
 */
public final class EvalSwitches {
    private static volatile Set<String> off = Set.of();

    private EvalSwitches() {
    }

    public static Gate gate(String name) {
        return new Gate(name, System.getenv(name) != null);
    }

    /** The gates switched off, besides the environment's, from now on. */
    public static void off(Set<String> names) {
        off = Set.copyOf(names);
    }

    public record Gate(String name, boolean inEnvironment) {
        /** Whether the gate is off: the behaviour it guards does not apply. */
        public boolean get() {
            return inEnvironment || off.contains(name);
        }
    }
}
