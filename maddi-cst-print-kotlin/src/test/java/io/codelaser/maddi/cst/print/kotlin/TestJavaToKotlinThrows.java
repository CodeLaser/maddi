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

package io.codelaser.maddi.cst.print.kotlin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * CodeLaser/maddi#223: a Java {@code throws} clause becomes {@code @Throws(X::class, …)}, so that Java code overriding
 * the translated method with {@code throws X}, or catching X around a call, still compiles.
 */
public class TestJavaToKotlinThrows extends CommonJavaToKotlin {

    @Test
    public void throwsClauses() {
        String kotlin = kotlin("""
                package a;
                import java.io.IOException;
                public class Provider {
                    public interface Source { byte[] bytes(String path) throws IOException; }
                    public Provider(String root) throws IOException { if (root == null) throw new IOException(); }
                    public byte[] read(Source s, String p) throws IOException, InterruptedException {
                        return s.bytes(p);
                    }
                    public <E extends Exception> void rethrow(E e) throws E { throw e; }
                    public int plain() { return 1; }
                }
                """);
        // on a function, the annotation has its own line: the declaration wraps as it would without it
        String oneLine = kotlin.replaceAll("\\s+", " ");
        assertTrue(kotlin.contains("@Throws(IOException::class)\n"), kotlin);
        assertTrue(oneLine.contains("@Throws(IOException::class) fun bytes("), kotlin);
        assertTrue(kotlin.contains("class Provider @Throws(IOException::class) constructor("), kotlin);
        assertTrue(oneLine.contains("@Throws(IOException::class, InterruptedException::class) open fun read("), kotlin);
        assertFalse(kotlin.contains("E::class"), kotlin);
        assertFalse(kotlin.contains("@Throws(Exception::class) fun plain"), kotlin);
        assertTrue(kotlin.lines().filter(l -> l.contains("@Throws")).count() == 3, kotlin);
        compiles(kotlin);
    }
}
