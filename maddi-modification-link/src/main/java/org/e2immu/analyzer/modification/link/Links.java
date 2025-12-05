package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Set;

/*
links from one variable and its constituent parts to other variables.
The list is sorted, with the primary links coming first.
 */
public interface Links extends Iterable<Link>, Value {

    Variable primary();

    default boolean isEmpty() {
        return links().isEmpty();
    }

    Set<Link> links();

    Links merge(Links links);

    interface Builder {
        Links build();

        Builder add(LinkNature linkNature, Variable to);

        Builder add(Variable from, LinkNature linkNature, Variable to);
    }

    // methods that do something
    // used by LVC
    Links changePrimaryTo(Runtime runtime, Variable newPrimary, TranslationMap translationMap);

}
