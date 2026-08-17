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

package org.e2immu.analyzer.modification.link.impl;

import java.util.Map;

/**
 * Cached environment gates. The process environment is immutable, but System.getenv(String) allocates on
 * every lookup (ProcessEnvironment$Variable) — the NOACM gate alone, evaluated per graph vertex, accounted
 * for 7% of a corpus run's allocations (async-profiler round 2). Copy the env once; gates are then
 * allocation-free map reads.
 */
public final class Gate {
    private Gate() {
    }

    private static final Map<String, String> ENV = Map.copyOf(System.getenv());

    public static boolean isSet(String name) {
        return ENV.get(name) != null;
    }

    public static String get(String name) {
        return ENV.get(name);
    }
}
