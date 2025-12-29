package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Links from one variable (primary) and its constituent parts ("fields") to other variables.
This is the main object stored in the analysis() of VariableInfo, and the main constituent of
MethodLinkedVariables.
 */
public interface Links extends Iterable<Link>, Value {

    boolean containsVirtualFields();

    Variable primary();

    boolean isEmpty();

    Iterable<Link> linkSet();

    Stream<Link> stream();

    Links merge(Links links);

    Links translate(TranslationMap translationMap);

    Iterable<Links> removeThisAsPrimary();

    interface Builder extends Iterable<Link> {
        Links build();

        Builder addAll(Links other);

        Builder add(LinkNature linkNature, Variable to);

        Builder add(Variable from, LinkNature linkNature, Variable to);

        boolean contains(Variable from, LinkNature reverse, Variable to);

        void prepend(LinkNature linkNature, Variable to);

        Variable primary();

        void removeIf(Predicate<Link> link);

        void replaceSubsetSuperset(Variable modified);
    }

    // methods that do something
    // used by LVC
    Links changePrimaryTo(Runtime runtime, Variable newPrimary);

    default boolean overwriteAllowed(Links linkedVariables) {
        // the primary changes to/from null
        return isEmpty() && linkedVariables.isEmpty();
    }

    default Set<Variable> toPrimaries() {
        return stream().map(l -> Util.primary(l.to())).collect(Collectors.toUnmodifiableSet());
    }

    List<Variable> primaryAssigned();
}
