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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.info.MethodInfo;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Members of the JDK types that Kotlin maps to its own (String, the collections, Number, Throwable, Enum, Object):
 * their Java methods are not visible from Kotlin, the Kotlin member is. {@code s.length()} is {@code s.length},
 * {@code m.entrySet()} is {@code m.entries}, {@code n.intValue()} is {@code n.toInt()}, {@code s.charAt(i)} is
 * {@code s[i]}. A call matches through the method it overrides too: {@code size()} on an {@code ArrayList} subclass.
 */
final class KotlinMappedMembers {

    private record Key(String owner, String name, int parameters) {
    }

    private static final Map<Key, String> PROPERTY = Map.ofEntries(
            Map.entry(new Key("java.lang.CharSequence", "length", 0), "length"),
            Map.entry(new Key("java.lang.String", "length", 0), "length"),
            Map.entry(new Key("java.util.Collection", "size", 0), "size"),
            Map.entry(new Key("java.util.List", "size", 0), "size"),
            Map.entry(new Key("java.util.Set", "size", 0), "size"),
            Map.entry(new Key("java.util.Map", "size", 0), "size"),
            Map.entry(new Key("java.util.Map", "keySet", 0), "keys"),
            Map.entry(new Key("java.util.Map", "values", 0), "values"),
            Map.entry(new Key("java.util.Map", "entrySet", 0), "entries"),
            Map.entry(new Key("java.util.Map.Entry", "getKey", 0), "key"),
            Map.entry(new Key("java.util.Map.Entry", "getValue", 0), "value"),
            Map.entry(new Key("java.lang.Throwable", "getMessage", 0), "message"),
            Map.entry(new Key("java.lang.Throwable", "getCause", 0), "cause"),
            Map.entry(new Key("java.lang.Enum", "name", 0), "name"),
            Map.entry(new Key("java.lang.Enum", "ordinal", 0), "ordinal"),
            Map.entry(new Key("java.lang.Object", "getClass", 0), "javaClass"));

    private static final Map<Key, String> FUNCTION = Map.ofEntries(
            Map.entry(new Key("java.lang.Number", "intValue", 0), "toInt"),
            Map.entry(new Key("java.lang.Number", "longValue", 0), "toLong"),
            Map.entry(new Key("java.lang.Number", "doubleValue", 0), "toDouble"),
            Map.entry(new Key("java.lang.Number", "floatValue", 0), "toFloat"),
            Map.entry(new Key("java.lang.Number", "shortValue", 0), "toShort"),
            Map.entry(new Key("java.lang.Number", "byteValue", 0), "toByte"));

    /** Same arguments, Kotlin's name. */
    private static final Map<Key, String> RENAMED = Map.ofEntries(
            Map.entry(new Key("java.util.SequencedCollection", "getFirst", 0), "first"),
            Map.entry(new Key("java.util.SequencedCollection", "getLast", 0), "last"),
            Map.entry(new Key("java.util.List", "getFirst", 0), "first"),
            Map.entry(new Key("java.util.List", "getLast", 0), "last"),
            Map.entry(new Key("java.lang.String", "toLowerCase", 0), "lowercase"),
            Map.entry(new Key("java.lang.String", "toUpperCase", 0), "uppercase"),
            Map.entry(new Key("java.lang.String", "toLowerCase", 1), "lowercase"),
            Map.entry(new Key("java.lang.String", "toUpperCase", 1), "uppercase"),
            Map.entry(new Key("java.lang.String", "getBytes", 0), "toByteArray"),
            Map.entry(new Key("java.lang.String", "getBytes", 1), "toByteArray"));

    /** A regular expression as its first argument: Kotlin takes a {@code Regex} there. */
    record Regex(String kotlinName) {
    }

    private static final Map<Key, Regex> REGEX = Map.of(
            new Key("java.lang.String", "replaceAll", 2), new Regex("replace"),
            new Key("java.lang.String", "replaceFirst", 2), new Regex("replaceFirst"),
            new Key("java.lang.String", "matches", 1), new Regex("matches"));

    private static final Map<Key, Boolean> EQUALS_IGNORE_CASE = Map.of(
            new Key("java.lang.String", "equalsIgnoreCase", 1), true);

    private static final Map<Key, Boolean> INDEX_GET = Map.of(
            new Key("java.lang.CharSequence", "charAt", 1), true,
            new Key("java.lang.String", "charAt", 1), true);

    private KotlinMappedMembers() {
    }

    /** The Kotlin property that replaces this call ({@code size()} -> {@code size}); null if none. */
    static String property(MethodInfo methodInfo) {
        return lookup(PROPERTY, methodInfo);
    }

    /** The Kotlin function that replaces this call, called without arguments ({@code intValue()} -> {@code toInt()}). */
    static String function(MethodInfo methodInfo) {
        return lookup(FUNCTION, methodInfo);
    }

    /** The Kotlin name of a call that keeps its arguments ({@code getFirst()} -> {@code first()}); null if none. */
    static String renamed(MethodInfo methodInfo) {
        return lookup(RENAMED, methodInfo);
    }

    static Regex regex(MethodInfo methodInfo) {
        return lookup(REGEX, methodInfo);
    }

    static boolean isEqualsIgnoreCase(MethodInfo methodInfo) {
        return lookup(EQUALS_IGNORE_CASE, methodInfo) != null;
    }

    /** {@code list.remove(int)}: Kotlin's {@code remove} takes the element, {@code removeAt} the index. */
    static boolean isRemoveAt(MethodInfo methodInfo) {
        return "remove".equals(methodInfo.name()) && methodInfo.parameters().size() == 1
               && methodInfo.parameters().getFirst().parameterizedType().isInt()
               && Stream.concat(Stream.of(methodInfo), methodInfo.overrides().stream())
                       .anyMatch(m -> "java.util.List".equals(m.typeInfo().fullyQualifiedName()));
    }

    /** {@code charAt(i)}: Kotlin's indexing operator. */
    static boolean isIndexGet(MethodInfo methodInfo) {
        return lookup(INDEX_GET, methodInfo) != null;
    }

    private static <V> V lookup(Map<Key, V> map, MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.isConstructor()) return null;
        return Stream.concat(Stream.of(methodInfo), methodInfo.overrides().stream())
                .map(m -> map.get(new Key(m.typeInfo().fullyQualifiedName(), m.name(), m.parameters().size())))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
