package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Overrides Kotlin wants where Java chooses by itself. guava's {@code WrappedSet extends WrappedCollection implements
 * Set<V>} inherits {@code spliterator()} twice: WrappedCollection's own, and Set's default through the interface.
 * Java takes a superclass's own method before any default; Kotlin refuses a class that "inherits multiple
 * implementations" and wants the choice written:
 * {@code override fun spliterator(): Spliterator<V> = super<WrappedCollection>.spliterator()}.
 * <p>
 * Only where the superclass chain's own method overrides another declaration than the interface's default
 * (WrappedCollection's spliterator overrides Collection's, not Set's). Parameters read as the interface sees them
 * (guava's FlatMapSpliteratorOfDouble: {@code tryAdvance(action: Consumer<in Double>)}), with the nullability of the
 * superclass's translated method, which the override must match; varargs are left alone.
 */
final class KotlinInheritedDefaults {

    private KotlinInheritedDefaults() {
    }

    static List<OutputBuilder> overrides(TypeInfo typeInfo, Qualification q) {
        if (typeInfo.isInterface() || typeInfo.parentClass() == null || typeInfo.parentClass().typeInfo() == null
            || typeInfo.parentClass().isJavaLangObject()) {
            return List.of();
        }
        List<OutputBuilder> result = new ArrayList<>();
        Set<String> done = new HashSet<>();
        // OfPrimitive's forEachRemaining(T_CONS) and OfDouble's forEachRemaining(DoubleConsumer) read the same in Kotlin
        Set<String> printed = new HashSet<>();
        for (ParameterizedType iface : typeInfo.interfacesImplemented()) {
            if (iface.typeInfo() == null) continue;
            for (MethodInfo d : defaults(iface.typeInfo(), new HashSet<>())) {
                String key = key(d);
                if (done.contains(key) || declares(typeInfo, d)) continue;
                MethodInfo fromSuper = fromSuperclass(typeInfo, d);
                // a superclass's own method that overrides the default's (or an interface's override Kotlin sees
                // through): no conflict. Java takes a class's method before any default
                if (fromSuper == null || fromSuper == d || fromSuper.typeInfo().isInterface()
                    || isSubtype(fromSuper.typeInfo(), d.typeInfo())) {
                    continue;
                }
                String via = superName(typeInfo.parentClass(), q);
                done.add(key);
                String text = override(d, iface, fromSuper, via, q);
                if (printed.add(text)) result.add(new OutputBuilderImpl().add(new TextImpl(text)));
            }
        }
        return result;
    }

    private static String override(MethodInfo d, ParameterizedType iface, MethodInfo fromSuper, String via,
                                   Qualification q) {
        boolean translated = KotlinNullability.translated(fromSuper.typeInfo());
        String parameters = d.parameters().stream()
                .map(p -> KotlinNames.name(name(fromSuper, p)) + ": " + KotlinTypeName.of(
                        seenThrough(p.parameterizedType(), d.typeInfo(), iface).withNullable(translated
                                && KotlinNullability.isNullable(KotlinNullability.parameterType(fromSuper.parameters().get(p.index())))
                                ? NullableState.NULLABLE : NullableState.NONNULL), q))
                .collect(Collectors.joining(", "));
        String arguments = d.parameters().stream().map(p -> KotlinNames.name(name(fromSuper, p)))
                .collect(Collectors.joining(", "));
        String name = KotlinNames.name(d.name());
        ParameterizedType rt = d.returnType();
        String returns = rt == null || rt.isVoid() ? ""
                : ": " + KotlinTypeName.of(seenThrough(rt, d.typeInfo(), iface).withNullable(translated
                        && KotlinNullability.isNullable(KotlinNullability.returnType(fromSuper))
                        ? NullableState.NULLABLE : NullableState.NONNULL), q);
        return "override fun " + name + "(" + parameters + ")" + returns + " = super<" + via + ">." + name + "("
               + arguments + ")";
    }

    /** The superclass method's parameter name: a library default's may be {@code arg0}. */
    private static String name(MethodInfo fromSuper, io.codelaser.maddi.cst.api.info.ParameterInfo p) {
        return fromSuper.parameters().get(p.index()).name();
    }

    /**
     * As {@link KotlinNullability#seenThrough}, keeping the projections of the declaration's type arguments:
     * {@code Consumer<? super T>} is a {@code Consumer<in Double>} through {@code Spliterator<Double>}, which an
     * override's signature must repeat.
     */
    private static ParameterizedType seenThrough(ParameterizedType declared, TypeInfo declaring, ParameterizedType via) {
        if (declared.arrays() > 0 || declared.parameters().isEmpty()) {
            return KotlinNullability.seenThrough(declared, declaring, via);
        }
        List<ParameterizedType> arguments = new ArrayList<>();
        for (ParameterizedType a : declared.parameters()) {
            if (a == null || a.isUnboundWildcard()) {
                arguments.add(a);
                continue;
            }
            ParameterizedType seen = seenThrough(a.wildcard() == null ? a : a.withWildcard(null), declaring, via);
            arguments.add(a.wildcard() != null && seen.wildcard() == null ? seen.withWildcard(a.wildcard()) : seen);
        }
        return declared.withParameters(arguments);
    }

    private static String superName(ParameterizedType type, Qualification q) {
        String name = KotlinTypeName.of(type.withNullable(NullableState.NONNULL), q);
        int lt = name.indexOf('<');
        return lt < 0 ? name : name.substring(0, lt);
    }

    /** The defaults an interface offers: its own, and its superinterfaces' that no declaration of its own hides. */
    private static List<MethodInfo> defaults(TypeInfo iface, Set<TypeInfo> visited) {
        if (!visited.add(iface)) return List.of();
        List<MethodInfo> result = new ArrayList<>();
        for (MethodInfo m : iface.methods()) {
            if (m.isDefault() && !m.isStatic() && m.parameters().stream().noneMatch(p -> p.isVarArgs())) result.add(m);
        }
        for (ParameterizedType sup : iface.interfacesImplemented()) {
            if (sup.typeInfo() == null) continue;
            for (MethodInfo m : defaults(sup.typeInfo(), visited)) {
                if (!declares(iface, m) && result.stream().noneMatch(r -> sameSignature(r, m))) result.add(m);
            }
        }
        return result;
    }

    /**
     * The implementation the superclass chain gives: a class's own non-abstract method, else the most specific
     * default of its interfaces. Null when there is none, or an abstract declaration hides them.
     */
    private static MethodInfo fromSuperclass(TypeInfo typeInfo, MethodInfo shape) {
        List<MethodInfo> defaults = new ArrayList<>();
        for (ParameterizedType s = typeInfo.parentClass(); s != null && s.typeInfo() != null && !s.isJavaLangObject();
             s = s.typeInfo().parentClass()) {
            MethodInfo own = own(s.typeInfo(), shape, typeInfo.parentClass());
            if (own != null) return own.isAbstract() ? null : own;
            for (ParameterizedType i : s.typeInfo().interfacesImplemented()) {
                if (i.typeInfo() == null) continue;
                defaults(i.typeInfo(), new HashSet<>()).stream().filter(m -> sameSignature(m, shape)).forEach(defaults::add);
            }
        }
        MethodInfo best = null;
        for (MethodInfo m : defaults) {
            if (best == null || m != best && isSubtype(m.typeInfo(), best.typeInfo())) best = m;
        }
        return best;
    }

    private static MethodInfo own(TypeInfo t, MethodInfo shape) {
        return t.methods().stream().filter(m -> !m.isStatic() && sameSignature(m, shape)).findFirst().orElse(null);
    }

    /**
     * As {@link #own(TypeInfo, MethodInfo)}, a superclass's parameter of its own type variable seen through
     * {@code via}, the class's parent: guava's FlatMapSpliteratorOfPrimitive.forEachRemaining(OutConsumerT) is a
     * forEachRemaining(DoubleConsumer) to FlatMapSpliteratorOfDouble, as Spliterator.OfDouble's default.
     */
    private static MethodInfo own(TypeInfo t, MethodInfo shape, ParameterizedType via) {
        MethodInfo plain = own(t, shape);
        if (plain != null) return plain;
        return t.methods().stream().filter(m -> !m.isStatic() && m.name().equals(shape.name())
                && m.parameters().size() == shape.parameters().size() && sameSeenThrough(m, shape, via))
                .findFirst().orElse(null);
    }

    private static boolean sameSeenThrough(MethodInfo m, MethodInfo shape, ParameterizedType via) {
        for (int i = 0; i < m.parameters().size(); i++) {
            ParameterizedType pa = m.parameters().get(i).parameterizedType();
            ParameterizedType pb = shape.parameters().get(i).parameterizedType();
            if (pa.typeParameter() != null && pa.arrays() == 0) {
                pa = KotlinNullability.seenThrough(pa, m.typeInfo(), via);
            }
            if (pa.typeInfo() == null || pa.typeInfo() != pb.typeInfo() || pa.arrays() != pb.arrays()) return false;
        }
        return true;
    }

    private static boolean declares(TypeInfo t, MethodInfo shape) {
        return own(t, shape) != null;
    }

    private static boolean sameSignature(MethodInfo a, MethodInfo b) {
        if (!a.name().equals(b.name()) || a.parameters().size() != b.parameters().size()) return false;
        for (int i = 0; i < a.parameters().size(); i++) {
            ParameterizedType pa = a.parameters().get(i).parameterizedType();
            ParameterizedType pb = b.parameters().get(i).parameterizedType();
            if (pa.typeInfo() != pb.typeInfo() || pa.arrays() != pb.arrays()) return false;
        }
        return true;
    }

    private static String key(MethodInfo m) {
        return m.name() + "/" + m.parameters().stream().map(p -> String.valueOf(p.parameterizedType().typeInfo()))
                .collect(Collectors.joining(","));
    }

    private static boolean isSubtype(TypeInfo sub, TypeInfo sup) {
        return isSubtype(sub, sup, new HashSet<>());
    }

    private static boolean isSubtype(TypeInfo sub, TypeInfo sup, Set<TypeInfo> visited) {
        if (sub == null || !visited.add(sub)) return false;
        if (sub == sup) return true;
        if (sub.parentClass() != null && isSubtype(sub.parentClass().typeInfo(), sup, visited)) return true;
        return sub.interfacesImplemented().stream().anyMatch(i -> isSubtype(i.typeInfo(), sup, visited));
    }
}
