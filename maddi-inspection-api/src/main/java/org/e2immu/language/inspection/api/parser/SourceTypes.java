package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;
import java.util.function.BiConsumer;

/*
Information about the .java files in the different packages, stored in a Trie.

Is not exposed to the actual parsing process.
 */
public interface SourceTypes {
    void add(String[] parts, TypeInfo typeInfo);

    void freeze();

    TypeInfo get(String fqn);

    TypeInfo getFindSubTypes(String fqn);

    boolean isKnown(TypeInfo typeInfo);

    void visit(String[] split, BiConsumer<String[], List<TypeInfo>> biConsumer);
}
