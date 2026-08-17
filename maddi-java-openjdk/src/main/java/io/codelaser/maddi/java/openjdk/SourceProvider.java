package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Source;

import java.util.List;

public interface SourceProvider {
    Source sourceForNode(Tree node);

    // the type-argument-list commas of the parameterized type at the given source, or null if none; used by
    // ConvertType to attach DetailedSources.TYPE_ARGUMENT_COMMAS to each parameterized type's source
    List<Source> typeArgumentCommas(Source typeSource);

    // the CompilationUnit of the source file currently being scanned (null before it has been built); lets the
    // symbol scanner reuse it for a forward-referenced type of the same file instead of minting a duplicate
    CompilationUnit currentCompilationUnit();
}
