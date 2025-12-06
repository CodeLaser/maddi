package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Set;

/*
Links from one variable (primary) and its constituent parts ("fields") to other variables.
This is the main object stored in the analysis() of VariableInfo, and the main constituent of
MethodLinkedVariables.
 */
public interface Links extends Iterable<Link>, Value {

    Variable primary();

    boolean isEmpty();

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
