package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import org.e2immu.language.cst.api.element.Source;

public interface SourceProvider {
    Source sourceForNode(Tree node);
}
