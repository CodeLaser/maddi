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

package org.e2immu.util.internal.util;

public enum GradleConfiguration {
    COMPILE("implementation", "i", false, false),
    TEST("testImplementation", "ti", false, false),
    RUNTIME("runtimeOnly", "r", false, true),
    TEST_RUNTIME("testRuntimeOnly", "tr", false, true),
    COMPILE_TRANSITIVE("compileClasspath", "i-t", true, false),
    TEST_TRANSITIVE("testCompileClasspath", "ti-t", true, false),
    RUNTIME_TRANSITIVE("runtimeClasspath", "r-t", true, true),
    TEST_RUNTIME_TRANSITIVE("testRuntimeClasspath", "tr-t", true, true);

    public final String gradle;
    public final String abbrev;
    public final boolean transitive;
    public final boolean runtime;

    GradleConfiguration(String gradle, String abbrev, boolean transitive, boolean runtime) {
        this.gradle = gradle;
        this.abbrev = abbrev;
        this.transitive = transitive;
        this.runtime = runtime;
    }

    public GradleConfiguration nonTransitive() {
        return switch (this) {
            case COMPILE_TRANSITIVE -> COMPILE;
            case TEST_TRANSITIVE -> TEST;
            case RUNTIME_TRANSITIVE -> RUNTIME;
            case TEST_RUNTIME_TRANSITIVE -> TEST_RUNTIME;
            default -> throw new UnsupportedOperationException();
        };
    }
}
