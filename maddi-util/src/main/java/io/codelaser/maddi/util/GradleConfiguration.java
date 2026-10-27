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

package io.codelaser.maddi.util;

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
