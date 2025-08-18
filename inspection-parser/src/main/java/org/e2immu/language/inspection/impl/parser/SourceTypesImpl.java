package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.util.internal.util.Trie;

import java.util.List;
import java.util.function.BiConsumer;

public class SourceTypesImpl implements SourceTypes {

    private final Trie<TypeInfo> trie = new Trie<>();

    @Override
    public TypeInfo get(String fqn) {
        List<TypeInfo> types = trie.get(fqn.split("\\."));
        return types == null ? null : types.get(0);
    }

    @Override
    public TypeInfo getFindSubTypes(String fqn) {
        String[] split = fqn.split("\\.");
        List<TypeInfo> types = trie.get(split);
        if (types == null) {
            for (int i = split.length - 1; i >= 0; i--) {
                List<TypeInfo> parentTypes = trie.get(split, i);
                if (parentTypes != null && !parentTypes.isEmpty()) {
                    TypeInfo parent = parentTypes.get(0);
                    for (int j = i; j < split.length; j++) {
                        parent = parent.findSubType(split[j], true);
                    }
                    return parent;
                }
            }
        }
        return types == null ? null : types.get(0);
    }

    @Override
    public boolean isKnown(TypeInfo typeInfo) {
        return false;
    }

    @Override
    public void visit(String[] split, BiConsumer<String[], List<TypeInfo>> biConsumer) {
        trie.visit(split, biConsumer);
    }

    @Override
    public void add(String[] parts, TypeInfo typeInfo) {
        trie.add(parts, typeInfo);
    }

    @Override
    public void freeze() {
        trie.freeze();
    }
}
