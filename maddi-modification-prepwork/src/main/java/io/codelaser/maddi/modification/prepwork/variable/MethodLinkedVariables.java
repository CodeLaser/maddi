package io.codelaser.maddi.modification.prepwork.variable;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface MethodLinkedVariables extends Value {
    // content

    Set<Variable> modified();

    /**
     * Own-field slots written by this method: every field reachable through a scope chain of 'this'
     * (this.i, this.d.j, Outer.this.x) that is the target of an assignment in the method body, directly or
     * transitively through calls on 'this' or on an own-field chain. ORTHOGONAL to {@link #modified()},
     * which records variables whose OBJECT is modified: a slot write (this.i = 3, i++) does not enter
     * modified() beyond its scope chain, and a content modification (this.list.add(x)) does not enter this
     * set. Union the two for "all own fields touched". Not populated across explicit constructor
     * invocations (this(...)/super(...)), which the link engine does not evaluate.
     */
    default Set<Variable> assigned() {
        return Set.of();
    }

    Links ofReturnValue();

    List<Links> ofParameters();

    // helper

    default boolean isEmpty() {
        return ofParameters().stream().allMatch(Links::isEmpty) && ofReturnValue().isEmpty();
    }

    MethodLinkedVariables removeSomeValue();

    default String sortedModifiedString() {
        return modified().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }

    default String sortedAssignedString() {
        return assigned().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }

    MethodLinkedVariables translate(TranslationMap translationMap);

    boolean virtual();
}
