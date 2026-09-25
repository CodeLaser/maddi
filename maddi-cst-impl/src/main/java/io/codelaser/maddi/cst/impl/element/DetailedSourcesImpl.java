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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class DetailedSourcesImpl implements DetailedSources {
    private final IdentityHashMap<Object, Object> identityHashMap;
    private final IdentityHashMap<Object, Object> association;
    // putReference: the declarations this element's text names, and where; null when none were recorded
    private final IdentityHashMap<Info, List<Source>> references;

    private DetailedSourcesImpl(IdentityHashMap<Object, Object> identityHashMap,
                                IdentityHashMap<Object, Object> association,
                                IdentityHashMap<Info, List<Source>> references) {
        this.identityHashMap = identityHashMap;
        this.association = association;
        this.references = references;
    }

    // build() hands the object its OWN maps, with every List value immutable: details() returns those lists, and
    // before 2026-09-25 they were the builder's live ArrayLists, still appended to by any later put() -- so the
    // analyzer (correctly) read DetailedSources as @Dependent, and through Source.detailedSources() and
    // Element.source() that capped the whole Element family (#51). Right-sizing the copies also cut the retained
    // heap of a detailed-sources parse by ~12% (Guava 211 -> 186 MB, dogfood 83 -> 74 MB; parse time unchanged).
    private static IdentityHashMap<Object, Object> frozenCopy(IdentityHashMap<Object, Object> map) {
        IdentityHashMap<Object, Object> copy = new IdentityHashMap<>(map.size());
        map.forEach((k, v) -> copy.put(k, v instanceof List<?> list ? List.copyOf(list) : v));
        return copy;
    }

    private static IdentityHashMap<Info, List<Source>> frozenReferences(IdentityHashMap<Info, List<Source>> map) {
        if (map == null) return null;
        IdentityHashMap<Info, List<Source>> copy = new IdentityHashMap<>(map.size());
        map.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return copy;
    }

    private static IdentityHashMap<Info, List<Source>> mergeReferences(IdentityHashMap<Info, List<Source>> a,
                                                                       IdentityHashMap<Info, List<Source>> b) {
        if (a == null && b == null) return null;
        IdentityHashMap<Info, List<Source>> merged = new IdentityHashMap<>();
        for (IdentityHashMap<Info, List<Source>> m : Arrays.asList(a, b)) {
            if (m != null) m.forEach((k, v) -> merged.computeIfAbsent(k, _ -> new ArrayList<>()).addAll(v));
        }
        return merged;
    }

    public static class BuilderImpl implements DetailedSources.Builder {
        private final IdentityHashMap<Object, Object> identityHashMap = new IdentityHashMap<>();
        private IdentityHashMap<Object, Object> association;
        private IdentityHashMap<Info, List<Source>> references;

        @Override
        public Object getAssociated(Object pt) {
            if (association == null) throw new UnsupportedOperationException();
            return association.get(pt);
        }

        @Override
        public Builder addAll(DetailedSources detailedSources) {
            DetailedSourcesImpl dsi = (DetailedSourcesImpl) detailedSources;
            identityHashMap.putAll(dsi.identityHashMap);
            if (dsi.association != null) {
                if (association == null) association = new IdentityHashMap<>();
                association.putAll(dsi.association);
            }
            references = mergeReferences(references, dsi.references);
            return this;
        }

        @Override
        public Builder copy() {
            BuilderImpl copy = new BuilderImpl();
            copy.identityHashMap.putAll(identityHashMap);
            if (association != null) {
                copy.association = new IdentityHashMap<>(association);
            }
            copy.references = mergeReferences(references, null);
            return copy;
        }

        @Override
        public Builder putReference(Info target, Source identifier) {
            if (references == null) references = new IdentityHashMap<>();
            references.computeIfAbsent(target, _ -> new ArrayList<>()).add(identifier);
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Builder put(Object object, Source source) {
            Object current = identityHashMap.get(object);
            if (current == null) {
                identityHashMap.put(object, source);
            } else if (current instanceof List list) {
                list.add(source);
            } else if (current instanceof Source s) {
                List<Source> list = new ArrayList<>();
                list.add(s);
                list.add(source);
                identityHashMap.put(object, list);
            }
            return this;
        }

        @Override
        public Builder putList(Object object, List<Source> sourceList) {
            identityHashMap.put(object, sourceList);
            return this;
        }

        @Override
        public DetailedSourcesImpl build() {
            return new DetailedSourcesImpl(frozenCopy(identityHashMap),
                    association == null ? null : new IdentityHashMap<>(association),
                    frozenReferences(references));
        }

        // used for the type without array [] [] parts
        @Override
        public Builder putWithArrayToWithoutArray(ParameterizedType withArray, ParameterizedType withoutArray) {
            if (association == null) association = new IdentityHashMap<>();
            assert withArray.arrays() > 0;
            assert withoutArray.arrays() == 0;
            association.put(withArray, withoutArray);
            return this;
        }

        @Override
        public Builder putTypeQualification(TypeInfo typeInfo, List<TypeInfoSource> associatedList) {
            if (association == null) association = new IdentityHashMap<>();
            association.put(typeInfo, associatedList);
            return this;
        }
    }

    @Override
    public Object associatedObject(Object object) {
        if (association == null) return null;
        return association.get(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Source> details(Object object) {
        Object o = identityHashMap.get(object);
        if (o == null) return List.of();
        if (o instanceof List) {
            return (List<Source>) o;
        }
        return List.of((Source) o);
    }

    public Source detail(Object object) {
        Object o = identityHashMap.get(object);
        if (o instanceof List<?> list) {
            return list.isEmpty() ? null : (Source) list.getFirst();
        }
        return (Source) o;
    }

    @Override
    public List<Source> references(Info target) {
        if (references == null) return List.of();
        List<Source> list = references.get(target);
        return list == null ? List.of() : List.copyOf(list);
    }

    @Override
    public void forEachReference(BiConsumer<Info, Source> consumer) {
        if (references != null) references.forEach((target, list) -> list.forEach(s -> consumer.accept(target, s)));
    }

    @Override
    public void forEach(BiConsumer<Object, Source> consumer) {
        identityHashMap.forEach((key, value) -> {
            if (value instanceof List<?> list) {
                list.forEach(source -> consumer.accept(key, (Source) source));
            } else {
                consumer.accept(key, (Source) value);
            }
        });
    }

    @Override
    public DetailedSources merge(DetailedSources other) {
        IdentityHashMap<Object, Object> copy = new IdentityHashMap<>(this.identityHashMap);
        IdentityHashMap<Object, Object> otherMap = ((DetailedSourcesImpl) other).identityHashMap;
        otherMap.forEach((k, v) -> copy.merge(k, v, (o1, o2) -> {
            if (o1 instanceof List<?> l1 && o2 instanceof List<?> l2) {
                return Stream.concat(l1.stream(), l2.stream()).toList();
            }
            return o2;
        }));
        IdentityHashMap<Object, Object> copyAssociation;
        IdentityHashMap<Object, Object> otherAssociation = ((DetailedSourcesImpl) other).association;
        if (this.association == null) {
            if (otherAssociation != null) {
                copyAssociation = new IdentityHashMap<>(otherAssociation);
            } else {
                copyAssociation = null;
            }
        } else {
            copyAssociation = new IdentityHashMap<>(this.association);
            if (otherAssociation != null) {
                copyAssociation.putAll(otherAssociation);
            }
        }
        return new DetailedSourcesImpl(copy, copyAssociation,
                mergeReferences(references, ((DetailedSourcesImpl) other).references));
    }

    @Override
    public DetailedSources withSources(Object o, List<Source> sources) {
        IdentityHashMap<Object, Object> copyAssociation = this.association == null ? null
                : new IdentityHashMap<>(this.association);
        IdentityHashMap<Object, Object> copy = new IdentityHashMap<>(identityHashMap.size());
        copy.putAll(identityHashMap);
        copy.put(o, List.copyOf(sources));
        return new DetailedSourcesImpl(copy, copyAssociation, references);
    }

    @Override
    public TypeInfo qualifier(TypeInfo typeInfo) {
        Source s = detail(typeInfo);
        if (s == null) return typeInfo;
        int posDiff = s.posDiff();
        if (posDiff == typeInfo.simpleName().length()) return typeInfo; // written without qualification
        Prefix prefix = writtenPrefix(s, typeInfo);
        if (prefix.fullyQualified) return null;
        if (prefix.typeInfo != null) return prefix.typeInfo;
        return qualifier(posDiff, typeInfo);
    }

    /**
     * The written qualification of one type reference: its immediate qualifier, or the fact that the author
     * wrote the reference out in full.
     */
    private record Prefix(TypeInfo typeInfo, boolean fullyQualified) {
    }

    /**
     * Recovers the qualification from the prefixes recorded beside the reference. They all BEGIN where the
     * reference itself begins and each is strictly shorter -- {@code a.b.X.Y.Z} records {@code a.b.X.Y},
     * {@code a.b.X} and the package name {@code a.b} at one and the same begin position -- so the immediate
     * qualifier is the longest strictly-shorter type among them, and a package among them says the reference
     * needs no import at all.
     * <p>
     * This is what makes {@code HashMap.Entry} work: the prefix recorded there is {@code java.util.HashMap},
     * the type the author wrote, whereas {@link #qualifier(int, TypeInfo)} can only walk the DECLARING chain
     * and would answer {@code java.util.Map}, which the text does not name.
     */
    private Prefix writtenPrefix(Source source, TypeInfo typeInfo) {
        TypeInfo longest = null;
        Source longestSource = null;
        List<String> strings = null;
        for (Map.Entry<Object, Object> entry : identityHashMap.entrySet()) {
            Object key = entry.getKey();
            boolean isType = key instanceof TypeInfo && key != typeInfo;
            if (!isType && !(key instanceof String)) continue;
            for (Source candidate : sourcesOf(entry.getValue())) {
                if (!strictlyShorterAtSameStart(candidate, source)) continue;
                if (isType) {
                    if (longestSource == null || endsAfter(candidate, longestSource)) {
                        longest = (TypeInfo) key;
                        longestSource = candidate;
                    }
                } else {
                    if (strings == null) strings = new ArrayList<>(1);
                    strings.add((String) key);
                }
            }
        }
        // a String key is not necessarily a package name (member names are recorded by name too), so accept one
        // as evidence of full qualification only when it IS the package of a type in play
        TypeInfo outermost = longest;
        boolean fullyQualified = strings != null && strings.stream().anyMatch(str -> !str.isEmpty()
                && (str.equals(typeInfo.packageName())
                    || outermost != null && str.equals(outermost.packageName())));
        return new Prefix(longest, fullyQualified);
    }

    @SuppressWarnings("unchecked")
    private static List<Source> sourcesOf(Object value) {
        if (value instanceof Source s) return List.of(s);
        if (value instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Source) {
            return (List<Source>) list;
        }
        return List.of();
    }

    private static boolean strictlyShorterAtSameStart(Source candidate, Source source) {
        return candidate.beginLine() == source.beginLine() && candidate.beginPos() == source.beginPos()
               && !endsAfter(candidate, source) && (candidate.endLine() != source.endLine()
                                                    || candidate.endPos() != source.endPos());
    }

    private static boolean endsAfter(Source candidate, Source other) {
        return candidate.endLine() != other.endLine() ? candidate.endLine() > other.endLine()
                : candidate.endPos() > other.endPos();
    }

    // >= because the dots can be surrounded by spaces (highly unusual, but possible)
    //  s.posDiff() >= typeInfo.fullyQualifiedName().length();
    private TypeInfo qualifier(int posDiff, TypeInfo typeInfo) {
        if (posDiff == typeInfo.simpleName().length()) return typeInfo;
        if (posDiff >= typeInfo.fullyQualifiedName().length()) return null;
        if (typeInfo.compilationUnitOrEnclosingType().isLeft()) return typeInfo; // fallback in case of spaces
        TypeInfo enclosing = typeInfo.compilationUnitOrEnclosingType().getRight();
        int minLength = typeInfo.simpleName().length() + 1; // but there could be more spaces; unlikely
        return qualifier(posDiff - minLength, enclosing);
    }
}
