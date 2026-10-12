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
