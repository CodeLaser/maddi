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

package io.codelaser.maddi.aapi.archive.libs.junitparams;

import io.codelaser.maddi.annotation.NotModified;

/**
 * JUnit Jupiter's parameterized-test arguments, found by the library-call census (TestLibraryCallCensus,
 * 2026-10-12): {@code Arguments.of(Object...)} and {@code arguments(Object...)} read their values as modified, 305
 * calls in four corpora. They only hold them. Names types in full and imports none, like libs/assertj.
 */
public class OrgJunitJupiterParamsProvider {
    public static final String PACKAGE_NAME = "org.junit.jupiter.params.provider";

    //public class org.junit.jupiter.params.provider.Arguments
    class Arguments$ {
        static org.junit.jupiter.params.provider.Arguments of(@NotModified java.lang.Object... arg0) { return null; }
        static org.junit.jupiter.params.provider.Arguments arguments(@NotModified java.lang.Object... arg0) { return null; }
        static org.junit.jupiter.params.provider.Arguments.ArgumentSet argumentSet(java.lang.String arg0, @NotModified java.lang.Object... arg1) { return null; }
    }
}
