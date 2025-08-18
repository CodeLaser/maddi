package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class DetailedSourcesImpl implements DetailedSources {
    private final IdentityHashMap<Object, Object> identityHashMap;
    private final IdentityHashMap<Object, Object> association;

    private DetailedSourcesImpl(IdentityHashMap<Object, Object> identityHashMap,
                                IdentityHashMap<Object, Object> association) {
        this.identityHashMap = identityHashMap;
        this.association = association;
    }

    public static class BuilderImpl implements DetailedSources.Builder {
        private final IdentityHashMap<Object, Object> identityHashMap = new IdentityHashMap<>();
        private IdentityHashMap<Object, Object> association;

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
            return this;
        }

        @Override
        public Builder copy() {
            BuilderImpl copy = new BuilderImpl();
            copy.identityHashMap.putAll(identityHashMap);
            if (association != null) {
                copy.association = new IdentityHashMap<>(association);
            }
            return copy;
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
        public DetailedSourcesImpl build() {
            return new DetailedSourcesImpl(identityHashMap, association);
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
        return (Source) identityHashMap.get(object);
    }

    @Override
    public DetailedSources merge(DetailedSources other) {
        IdentityHashMap<Object, Object> copy = new IdentityHashMap<>(this.identityHashMap);
        copy.putAll(((DetailedSourcesImpl) other).identityHashMap);
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
        return new DetailedSourcesImpl(copy, copyAssociation);
    }

    @Override
    public DetailedSources withSources(Object o, List<Source> sources) {
        IdentityHashMap<Object, Object> copyAssociation = this.association == null ? null
                : new IdentityHashMap<>(this.association);
        IdentityHashMap<Object, Object> copy = new IdentityHashMap<>(identityHashMap.size());
        copy.putAll(identityHashMap);
        copy.put(o, sources);
        return new DetailedSourcesImpl(copy, copyAssociation);
    }
}
