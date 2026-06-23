package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import org.e2immu.language.cst.api.element.Source;

import java.util.List;

public interface SourceProvider {
    Source sourceForNode(Tree node);

    // the type-argument-list commas of the parameterized type at the given source, or null if none; used by
    // ConvertType to attach DetailedSources.TYPE_ARGUMENT_COMMAS to each parameterized type's source
    List<Source> typeArgumentCommas(Source typeSource);
}
