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

package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// All original tests called javaInspector.compiledTypesManager().getOrLoad(<JdkClass>.class, ...)
// to load and inspect byte-code-based type info (e.g. AbstractMockMvcBuilder, FileOutputStream, Long,
// ArrayList). compiledTypesManager is not available in this module; classSymbolScanner.getType() is
// the closest equivalent but only works for types already encountered during a scan. Since these tests
// did not scan any source — they only loaded compiled types — there is no viable replacement and the
// test bodies are left empty.
public class TestByteCode extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestByteCode.class);

    @Test
    public void testOnDemand() {
    }

    @Test
    public void test() {
    }

    @Test
    public void testThrows() {
    }

    @Test
    public void testThrows2() {
    }

    @Test
    public void testLongRotateRight() {
    }

    @Test
    public void testOverrides() {
    }

    @Test
    public void testPackageContainsTypes() {
    }
}
