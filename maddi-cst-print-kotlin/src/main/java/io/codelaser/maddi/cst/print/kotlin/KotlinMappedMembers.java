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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
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
            // Kotlin's Enum has no getDeclaringClass(); kotlin.jvm's extension property gives Java's
            Map.entry(new Key("java.lang.Enum", "getDeclaringClass", 0), "declaringJavaClass"),
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
            "message", "cause", "length");

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

    /**
     * As {@link #OBJECT_PARAMETER}, per parameter, for members with more than one: the owner's type parameter for
     * each, -1 where the parameter keeps its own type ({@code Map.remove(Object, Object)} is
     * {@code remove(key: K, value: V)}; {@code getOrDefault(Object, V)} is {@code getOrDefault(key: K, defaultValue: V)}).
     */
    private static final Map<Key, List<Integer>> OBJECT_PARAMETERS = Map.of(
            new Key("java.util.Map", "remove", 2), List.of(0, 1),
            new Key("java.util.Map", "getOrDefault", 2), List.of(0, -1));

    /** {@code Map.putAll(Map<? extends K, ? extends V>)}, which Kotlin declares {@code putAll(from: Map<out K, V>)}. */
    private static final Set<Key> MAP_PARAMETER = Set.of(new Key("java.util.Map", "putAll", 1));

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
        if (enumProperty(methodInfo)) return methodInfo.name();
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
    /**
     * The parameter {@link #overriddenParameterType} found is a member's collection or map parameter
     * ({@code addAll(elements: Collection<E>)}, {@code putAll(from: Map<out K, V>)}), not one of the owner's type
     * arguments: guava's {@code Map<K, Collection<V>>.remove(key)} or ArrayTable's {@code put(key, value: V)} with a
     * V that is a {@code Map<R, V?>} itself takes the type argument as it is.
     */
    static boolean overriddenCollectionParameter(MethodInfo methodInfo, int i) {
        if (methodInfo.overrides().isEmpty() || methodInfo.isStatic()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) {
            return false;
        }
        for (MethodInfo m : methodInfo.overrides()) {
            Key key = new Key(m.typeInfo().fullyQualifiedName(), m.name(), m.parameters().size());
            List<Integer> perParameter = OBJECT_PARAMETERS.get(key);
            Integer index = perParameter == null ? OBJECT_PARAMETER.get(key)
                    : i < perParameter.size() && perParameter.get(i) >= 0 ? perParameter.get(i) : null;
            boolean collection = COLLECTION_PARAMETER.contains(key) && i == m.parameters().size() - 1;
            boolean map = MAP_PARAMETER.contains(key);
            Integer ownVariable = index == null && !collection && !map ? ownTypeVariable(m, i) : null;
            if (ownVariable != null) index = ownVariable;
            if (index == null && !collection && !map) continue;
            return index == null;
        }
        return false;
    }

    static ParameterizedType overriddenParameterType(MethodInfo methodInfo, int i) {
        if (methodInfo.overrides().isEmpty() || methodInfo.isStatic()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) {
            return null;
        }
        for (MethodInfo m : methodInfo.overrides()) {
            Key key = new Key(m.typeInfo().fullyQualifiedName(), m.name(), m.parameters().size());
            List<Integer> perParameter = OBJECT_PARAMETERS.get(key);
            Integer index = perParameter == null ? OBJECT_PARAMETER.get(key)
                    : i < perParameter.size() && perParameter.get(i) >= 0 ? perParameter.get(i) : null;
            boolean collection = COLLECTION_PARAMETER.contains(key) && i == m.parameters().size() - 1;
            boolean map = MAP_PARAMETER.contains(key);
            Integer ownVariable = index == null && !collection && !map ? ownTypeVariable(m, i) : null;
            if (ownVariable != null) index = ownVariable;
            if (index == null && !collection && !map) continue;
            ParameterizedType asOwner = declaredSupertype(methodInfo.typeInfo(), m.typeInfo());
            if (asOwner == null) {
                try {
                    asOwner = methodInfo.typeInfo().asParameterizedType()
                            .concreteSuperType(m.typeInfo().asParameterizedType());
                } catch (RuntimeException e) {
                    return null;
                }
            }
            if (asOwner == null || asOwner.parameters().size() <= (index == null ? 0 : index)) return null;
            if (index != null) return asOwner.parameters().get(index);
            if (map) return asOwner.parameters().size() == 2 ? asOwner : null;
            ParameterizedType collectionType = m.parameters().getLast().parameterizedType();
            return collectionType.typeInfo() == null ? null
                    : collectionType.typeInfo().asParameterizedType().withParameters(List.of(asOwner.parameters().getFirst()));
        }
        return null;
    }

    /**
     * {@code owner}, seen from {@code typeInfo}'s declared supertypes, with their type arguments'
     * type-use annotations: guava's ArrayTable.ColumnMap extends ArrayMap<C, Map<R, @Nullable V>>, whose put(key,
     * value) takes a Map<R, V?>, as the class header writes it. Null when no direct supertype leads there.
     */
    private static ParameterizedType declaredSupertype(io.codelaser.maddi.cst.api.info.TypeInfo typeInfo,
                                                       io.codelaser.maddi.cst.api.info.TypeInfo owner) {
        List<ParameterizedType> direct = new java.util.ArrayList<>();
        if (typeInfo.parentClass() != null) direct.add(typeInfo.parentClass());
        direct.addAll(typeInfo.interfacesImplemented());
        for (ParameterizedType st : direct) {
            if (st == null || st.typeInfo() == null) continue;
            ParameterizedType declared = KotlinTypePrinter.declaredArguments(st);
            if (declared.typeInfo() == owner) return declared;
            try {
                ParameterizedType seen = declared.concreteSuperType(owner.asParameterizedType());
                if (seen != null) return seen;
            } catch (RuntimeException | AssertionError e) {
                // not this one
            }
        }
        return null;
    }

    /**
     * Parameter {@code i} of a member of a type Kotlin maps to its own, declared as one of the owner's type parameters:
     * that parameter's index, null otherwise. Kotlin declares it exactly so, non-null ({@code MutableCollection.add(
     * element: E)}), and an override with a nullable verdict there overrides nothing (guava's ForwardingCollection
     * printed {@code add(element: E?)}).
     */
    private static Integer ownTypeVariable(MethodInfo m, int i) {
        if (i >= m.parameters().size() || !KotlinTypeName.isMapped(m.typeInfo().fullyQualifiedName())) return null;
        ParameterizedType declared = m.parameters().get(i).parameterizedType();
        if (declared.typeParameter() == null || declared.arrays() > 0) return null;
        int index = m.typeInfo().typeParameters().indexOf(declared.typeParameter());
        return index < 0 ? null : index;
    }

    /**
     * Map's default methods as Kotlin sees the JDK's: definitely non-null value types where Java's nullness
     * annotations say so, and a nullable result. {K}, {V} and {V!} (V & Any) stand for the owner's type arguments.
     * An override printed with Java's types overrides nothing (guava's LocalCache, Maps, ForwardingMap).
     */
    private record DefaultSignature(List<String> parameters, String result) {
    }

    private static final Map<Key, DefaultSignature> MAP_DEFAULTS = Map.of(
            new Key("java.util.Map", "computeIfPresent", 2),
            new DefaultSignature(List.of("{K}", "BiFunction<in {K}, in {V!}, out {V}?>"), "{V}?"),
            new Key("java.util.Map", "merge", 3),
            new DefaultSignature(List.of("{K}", "{V!}", "BiFunction<in {V!}, in {V!}, out {V}?>"), "{V}?"),
            new Key("java.util.Map", "compute", 2),
            new DefaultSignature(List.of("{K}", "BiFunction<in {K}, in {V}?, out {V}?>"), "{V}?"));

    /** Parameter {@code i}'s Kotlin type in an override of one of Map's default methods; null otherwise. */
    static String mapDefaultParameter(MethodInfo methodInfo, int i, Qualification q) {
        String[] kv = mapDefaultTypeArguments(methodInfo, q);
        if (kv == null) return null;
        DefaultSignature signature = mapDefault(methodInfo);
        return i < signature.parameters().size() ? substitute(signature.parameters().get(i), kv) : null;
    }

    /** The result's Kotlin type in an override of one of Map's default methods; null otherwise. */
    static String mapDefaultResult(MethodInfo methodInfo, Qualification q) {
        String[] kv = mapDefaultTypeArguments(methodInfo, q);
        return kv == null ? null : substitute(mapDefault(methodInfo).result(), kv);
    }

    private static DefaultSignature mapDefault(MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.overrides().isEmpty()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) {
            return null;
        }
        for (MethodInfo m : methodInfo.overrides()) {
            DefaultSignature signature = MAP_DEFAULTS.get(new Key(m.typeInfo().fullyQualifiedName(), m.name(),
                    m.parameters().size()));
            if (signature != null) return signature;
        }
        return null;
    }

    /** K, V and V & Any for the method's class as a Map, printed; null when it is not one of Map's defaults. */
    private static String[] mapDefaultTypeArguments(MethodInfo methodInfo, Qualification q) {
        if (mapDefault(methodInfo) == null) return null;
        TypeInfo map = methodInfo.overrides().stream().map(MethodInfo::typeInfo)
                .filter(t -> "java.util.Map".equals(t.fullyQualifiedName())).findFirst().orElse(null);
        if (map == null) return null;
        ParameterizedType asMap;
        try {
            asMap = methodInfo.typeInfo().asParameterizedType().concreteSuperType(map.asParameterizedType());
        } catch (RuntimeException | AssertionError | StackOverflowError e) {
            return null;
        }
        if (asMap == null || asMap.parameters().size() != 2) return null;
        ParameterizedType v = asMap.parameters().get(1);
        String vName = KotlinTypeName.of(v.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL), q);
        return new String[]{KotlinTypeName.of(asMap.parameters().get(0), q), vName,
                v.typeParameter() != null ? vName + " & Any" : vName};
    }

    private static String substitute(String template, String[] kv) {
        return template.replace("{K}", kv[0]).replace("{V!}", kv[2]).replace("{V}", kv[1]);
    }

    /** The Kotlin property that replaces this call ({@code size()} -> {@code size}); null if none. */
    static String property(MethodInfo methodInfo) {
        String property = lookup(PROPERTY, methodInfo);
        return property != null ? property : enumProperty(methodInfo) ? methodInfo.name() : null;
    }

    /**
     * An interface's {@code int ordinal()} or {@code String name()}, which an enum implements by java.lang.Enum's
     * (guava's BloomFilter.Strategy, implemented by the BloomFilterStrategies enum): Kotlin's Enum has the properties,
     * and a {@code fun ordinal()} in the enum would clash with them on the JVM. A property in the interface too, which
     * Enum's implements; an override of it is one.
     */
    static boolean enumProperty(MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.isConstructor() || !methodInfo.parameters().isEmpty()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())) return false;
        // an interface being translated: a library's (java.nio's FileAttribute.name()) keeps its function
        return Stream.concat(Stream.of(methodInfo), methodInfo.overrides().stream()).anyMatch(m -> m.typeInfo().isInterface()
                && KotlinNullability.translated(m.typeInfo()) && m.parameters().isEmpty()
                && ("ordinal".equals(m.name()) && m.returnType().isInt()
                    || "name".equals(m.name()) && m.returnType().typeInfo() != null && m.returnType().arrays() == 0
                       && "java.lang.String".equals(m.returnType().typeInfo().fullyQualifiedName())));
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

    /**
     * Java's {@code String} members that Kotlin's {@code String} does not have, reached through
     * {@code java.lang.String}: {@code strip} keeps a no-break space that Kotlin's {@code trim} removes.
     */
    static boolean isJavaStringOnly(MethodInfo methodInfo) {
        return !methodInfo.isStatic() && "java.lang.String".equals(methodInfo.typeInfo().fullyQualifiedName())
               && java.util.Set.of("strip", "stripLeading", "stripTrailing", "stripIndent", "indent",
                "translateEscapes", "getChars").contains(methodInfo.name());
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
