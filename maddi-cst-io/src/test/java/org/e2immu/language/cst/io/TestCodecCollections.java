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

package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Robustness tests for the structural layer of {@link CodecImpl}: list / set / map / mapAsList encoding and
 * decoding, including empty collections, nesting, key sorting and the kv-pair layout. Round-trips go
 * encode -> toString -> JSON parse ({@code makeD}) -> decode, decoding the leaf values back to ints/strings.
 */
public class TestCodecCollections extends CommonTest {

    private Codec.EncodedValue i(int v) {
        return codec.encodeInt(context, v);
    }

    private Codec.EncodedValue s(String v) {
        return codec.encodeString(context, v);
    }

    private List<Integer> decodeIntList(Codec.EncodedValue d) {
        return codec.decodeList(context, d).stream().map(ev -> codec.decodeInt(context, ev)).toList();
    }

    // ---- list ---------------------------------------------------------------

    @DisplayName("list: empty, single, multiple round-trip")
    @Test
    public void testList() {
        assertEquals("[]", codec.encodeList(context, List.of()).toString());
        assertEquals(List.of(), decodeIntList(makeD("[]")));

        assertEquals("[7]", codec.encodeList(context, List.of(i(7))).toString());
        assertEquals(List.of(7), decodeIntList(makeD("[7]")));

        Codec.EncodedValue ev = codec.encodeList(context, List.of(i(1), i(2), i(3)));
        assertEquals("[1,2,3]", ev.toString());
        assertEquals(List.of(1, 2, 3), decodeIntList(makeD(ev.toString())));
    }

    @DisplayName("list preserves order (no sorting), unlike set")
    @Test
    public void testListOrder() {
        Codec.EncodedValue ev = codec.encodeList(context, List.of(i(3), i(1), i(2)));
        assertEquals("[3,1,2]", ev.toString());
        assertEquals(List.of(3, 1, 2), decodeIntList(makeD(ev.toString())));
    }

    @DisplayName("nested list round-trip")
    @Test
    public void testNestedList() {
        Codec.EncodedValue inner = codec.encodeList(context, List.of(i(1), i(2)));
        Codec.EncodedValue outer = codec.encodeList(context, List.of(inner, i(3)));
        assertEquals("[[1,2],3]", outer.toString());

        List<Codec.EncodedValue> decoded = codec.decodeList(context, makeD(outer.toString()));
        assertEquals(2, decoded.size());
        assertTrue(codec.isList(decoded.get(0)));
        assertEquals(List.of(1, 2), decodeIntList(decoded.get(0)));
        assertEquals(3, codec.decodeInt(context, decoded.get(1)));
    }

    // ---- set ----------------------------------------------------------------

    @DisplayName("set is sorted on encoding and round-trips")
    @Test
    public void testSet() {
        Set<Codec.EncodedValue> in = new LinkedHashSet<>(List.of(s("banana"), s("apple"), s("cherry")));
        Codec.EncodedValue ev = codec.encodeSet(context, in);
        assertEquals("[\"apple\",\"banana\",\"cherry\"]", ev.toString());

        Set<Codec.EncodedValue> decoded = codec.decodeSet(context, makeD(ev.toString()));
        Set<String> asStrings = new TreeSet<>();
        decoded.forEach(d -> asStrings.add(codec.decodeString(context, d)));
        assertEquals(Set.of("apple", "banana", "cherry"), asStrings);
    }

    @DisplayName("empty set")
    @Test
    public void testEmptySet() {
        assertEquals("[]", codec.encodeSet(context, Set.of()).toString());
        assertTrue(codec.decodeSet(context, makeD("[]")).isEmpty());
    }

    // ---- map ----------------------------------------------------------------

    private Map<String, Integer> decodeStringIntMap(Codec.EncodedValue d) {
        Map<String, Integer> result = new TreeMap<>();
        codec.decodeMap(context, d).forEach((k, v) ->
                result.put(codec.decodeString(context, k), codec.decodeInt(context, v)));
        return result;
    }

    @DisplayName("map is key-sorted on encoding and round-trips")
    @Test
    public void testMap() {
        Map<Codec.EncodedValue, Codec.EncodedValue> in = new LinkedHashMap<>();
        in.put(s("k2"), i(2));
        in.put(s("k1"), i(1));
        in.put(s("k3"), i(3));
        Codec.EncodedValue ev = codec.encodeMap(context, in);
        assertEquals("{\"k1\":1,\"k2\":2,\"k3\":3}", ev.toString());

        assertEquals(Map.of("k1", 1, "k2", 2, "k3", 3), decodeStringIntMap(makeD(ev.toString())));
    }

    @DisplayName("empty map")
    @Test
    public void testEmptyMap() {
        assertEquals("{}", codec.encodeMap(context, Map.of()).toString());
        assertTrue(codec.decodeMap(context, makeD("{}")).isEmpty());
    }

    @DisplayName("map with numeric-looking keys: keys are quoted so the JSON stays valid")
    @Test
    public void testMapNumericKeys() {
        Map<Codec.EncodedValue, Codec.EncodedValue> in = new LinkedHashMap<>();
        in.put(s("10"), i(100));
        in.put(s("2"), i(200));
        Codec.EncodedValue ev = codec.encodeMap(context, in);
        // numeric keys are wrapped in quotes (quoteNumber); sorting is lexicographic on the encoded key
        assertEquals("{\"10\":100,\"2\":200}", ev.toString());
        assertEquals(Map.of("10", 100, "2", 200), decodeStringIntMap(makeD(ev.toString())));
    }

    // ---- mapAsList ----------------------------------------------------------

    @DisplayName("mapAsList encodes as a flat, key-sorted list and round-trips")
    @Test
    public void testMapAsList() {
        Map<Codec.EncodedValue, Codec.EncodedValue> in = new LinkedHashMap<>();
        in.put(s("b"), i(2));
        in.put(s("a"), i(1));
        Codec.EncodedValue ev = codec.encodeMapAsList(context, in);
        assertEquals("[\"a\",1,\"b\",2]", ev.toString());

        Map<String, Integer> out = new TreeMap<>();
        codec.decodeMapAsList(context, makeD(ev.toString())).forEach((k, v) ->
                out.put(codec.decodeString(context, k), codec.decodeInt(context, v)));
        assertEquals(Map.of("a", 1, "b", 2), out);
    }
}
