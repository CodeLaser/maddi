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
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Map;
import java.util.List;
import java.util.Set;
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
            Map.entry(new Key("java.lang.String", "getBytes", 1), "toByteArray"),
            Map.entry(new Key("java.lang.String", "formatted", 1), "format"),
            Map.entry(new Key("java.lang.String", "concat", 1), "plus"),
            Map.entry(new Key("java.util.List", "sort", 1), "sortWith"));

    /** The unboxing calls: in Kotlin the value already is its primitive. */
    private static final Map<Key, Boolean> UNBOXED = Map.of(
            new Key("java.lang.Boolean", "booleanValue", 0), true,
            new Key("java.lang.Character", "charValue", 0), true);

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

    /** The mapped getters a class can override: as Kotlin properties ({@code override val size: Int get() = …}). */
    private static final Set<String> OVERRIDABLE_PROPERTIES = Set.of("size", "keys", "values", "entries", "key", "value",
            "message", "cause");

    /**
     * The parameters Java declares as {@code Object} where Kotlin's mapped member takes a type parameter of its owner:
     * the index of that type parameter ({@code Collection.contains(Object)} is {@code contains(element: E)}).
     */
    private static final Map<Key, Integer> OBJECT_PARAMETER = Map.ofEntries(
            Map.entry(new Key("java.util.Collection", "contains", 1), 0),
            Map.entry(new Key("java.util.Collection", "remove", 1), 0),
            Map.entry(new Key("java.util.List", "indexOf", 1), 0),
            Map.entry(new Key("java.util.List", "lastIndexOf", 1), 0),
            Map.entry(new Key("java.util.Map", "containsKey", 1), 0),
            Map.entry(new Key("java.util.Map", "containsValue", 1), 1),
            Map.entry(new Key("java.util.Map", "get", 1), 0),
            Map.entry(new Key("java.util.Map", "remove", 1), 0));

    /** The {@code Collection<?>}/{@code Collection<? extends E>} parameters Kotlin declares {@code Collection<E>}. */
    private static final Set<Key> COLLECTION_PARAMETER = Set.of(
            new Key("java.util.Collection", "containsAll", 1), new Key("java.util.Collection", "addAll", 1),
            new Key("java.util.Collection", "removeAll", 1), new Key("java.util.Collection", "retainAll", 1),
            new Key("java.util.List", "addAll", 2));

    /**
     * The Kotlin property a translated override of a mapped getter declares ({@code public int size()} in an
     * ArrayList subclass is {@code override val size: Int}); null otherwise. Kotlin has no {@code size()} to override.
     */
    static String overriddenProperty(MethodInfo methodInfo) {
        if (methodInfo.overrides().isEmpty() || !methodInfo.parameters().isEmpty() || methodInfo.isStatic()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) {
            return null;
        }
        String property = property(methodInfo);
        return property != null && OVERRIDABLE_PROPERTIES.contains(property) ? property : null;
    }

    /**
     * The Kotlin type of parameter {@code i} of a translated override of a mapped collection member, where it is not
     * Java's: {@code remove(element: E)} for {@code remove(Object)}, {@code addAll(elements: Collection<E>)} for
     * {@code addAll(Collection<? extends E>)}. Null when the parameter keeps its own type.
     */
    static ParameterizedType overriddenParameterType(MethodInfo methodInfo, int i) {
        if (methodInfo.overrides().isEmpty() || methodInfo.isStatic()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) {
            return null;
        }
        for (MethodInfo m : methodInfo.overrides()) {
            Key key = new Key(m.typeInfo().fullyQualifiedName(), m.name(), m.parameters().size());
            Integer index = OBJECT_PARAMETER.get(key);
            boolean collection = COLLECTION_PARAMETER.contains(key) && i == m.parameters().size() - 1;
            if (index == null && !collection) continue;
            ParameterizedType asOwner;
            try {
                asOwner = methodInfo.typeInfo().asParameterizedType()
                        .concreteSuperType(m.typeInfo().asParameterizedType());
            } catch (RuntimeException e) {
                return null;
            }
            if (asOwner == null || asOwner.parameters().size() <= (index == null ? 0 : index)) return null;
            if (index != null) return asOwner.parameters().get(index);
            ParameterizedType collectionType = m.parameters().getLast().parameterizedType();
            return collectionType.typeInfo() == null ? null
                    : collectionType.typeInfo().asParameterizedType().withParameters(List.of(asOwner.parameters().getFirst()));
        }
        return null;
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

    /** {@code b.booleanValue()} is {@code b}. */
    static boolean isUnboxing(MethodInfo methodInfo) {
        return lookup(UNBOXED, methodInfo) != null;
    }

    /**
     * {@code wait}, {@code notify}, {@code notifyAll}: members of {@code java.lang.Object} that Kotlin's {@code Any}
     * does not have; reached through a cast to {@code java.lang.Object}.
     */
    /**
     * {@code Objects.requireNonNull(x)}: Kotlin infers its {@code T} from the argument, so for a nullable {@code x} the
     * result is nullable too; {@code x!!} throws the same NullPointerException and is non-null.
     */
    static boolean isRequireNonNull(MethodInfo methodInfo) {
        return methodInfo.isStatic() && "requireNonNull".equals(methodInfo.name()) && methodInfo.parameters().size() == 1
               && "java.util.Objects".equals(methodInfo.typeInfo().fullyQualifiedName());
    }

    static boolean isMonitorMethod(MethodInfo methodInfo) {
        return !methodInfo.isStatic() && "java.lang.Object".equals(methodInfo.typeInfo().fullyQualifiedName())
               && java.util.Set.of("wait", "notify", "notifyAll").contains(methodInfo.name());
    }

    /** {@code s.split(regex)}: Kotlin's {@code split(String)} splits on the literal string, and returns a list. */
    static boolean isSplit(MethodInfo methodInfo) {
        return "split".equals(methodInfo.name()) && methodInfo.parameters().size() == 1
               && "java.lang.String".equals(methodInfo.typeInfo().fullyQualifiedName());
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
