package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/*
Links from one variable (primary) and its constituent parts ("fields") to other variables.
This is the main object stored in the analysis() of VariableInfo, and the main constituent of
MethodLinkedVariables.
 */
public interface Links extends Iterable<Link>, Value {

    Variable primary();

    boolean isEmpty();

    Set<Link> linkSet();

    Stream<Link> stream();

    Links merge(Links links);

    Links translate(TranslationMap translationMap);

    interface Builder {
        Links build();

        Builder addAll(Links other);

        Builder add(LinkNature linkNature, Variable to);

        Builder add(Variable from, LinkNature linkNature, Variable to);

        Variable primary();

        void removeIf(Predicate<Link> link);
    }

    // methods that do something
    // used by LVC
    Links changePrimaryTo(Runtime runtime, Variable newPrimary);


    default boolean isDelayed() {
        throw new UnsupportedOperationException();
    }

    default boolean isNotYetSet() {
        throw new UnsupportedOperationException();
    }

    default boolean overwriteAllowed(Links linkedVariables) {
        throw new UnsupportedOperationException();
    }

    List<Variable> primaryAssigned();
}
