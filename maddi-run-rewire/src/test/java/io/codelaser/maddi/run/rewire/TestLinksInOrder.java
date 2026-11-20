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

package io.codelaser.maddi.run.rewire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestLinksInOrder {

    @Test
    public void sameLinksInAnotherOrderCompareEqual() {
        String a = "{\"name\": \"Ftype(18)\", \"data\":{\"links\":[[\"F\",[\"T\",\"C<init>(1,int[])\"]],"
                   + "[\"P\",[\"T\",\"x]\"]]],\"unmodifiedField\":1}},";
        String b = "{\"name\": \"Ftype(18)\", \"data\":{\"links\":[[\"P\",[\"T\",\"x]\"]],"
                   + "[\"F\",[\"T\",\"C<init>(1,int[])\"]]],\"unmodifiedField\":1}},";
        assertEquals(RunRewireTests.linksInOrder(a), RunRewireTests.linksInOrder(b));
        assertEquals(a, RunRewireTests.linksInOrder(a), "already in order: unchanged");
    }

    @Test
    public void otherLinksStillDiffer() {
        String a = "{\"data\":{\"links\":[[\"F\",\"a\"],[\"F\",\"b\"]]}}";
        String b = "{\"data\":{\"links\":[[\"F\",\"a\"]]}}";
        assertEquals("{\"data\":{\"links\":[[\"F\",\"a\"]]}}", RunRewireTests.linksInOrder(b));
        assert !RunRewireTests.linksInOrder(a).equals(RunRewireTests.linksInOrder(b));
    }

    @Test
    public void lineWithoutLinksIsUntouched() {
        assertEquals("{\"data\":{\"x\":1}}", RunRewireTests.linksInOrder("{\"data\":{\"x\":1}}"));
        assertEquals("{\"data\":{\"links\":[]}}", RunRewireTests.linksInOrder("{\"data\":{\"links\":[]}}"));
    }
}
