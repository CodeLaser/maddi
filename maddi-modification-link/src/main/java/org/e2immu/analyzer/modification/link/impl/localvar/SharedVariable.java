package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedVariable extends LocalVariableImpl implements LinkVariable {
    public static final String PREFIX = "$__sv_";

    // a directed assignment 'from ← to' (from IS_ASSIGNED_FROM to) that folded these two members into the group.
    // Kept so the group's intra-member relation can be reconstructed at summary extraction (the collapse only
    // stores it once; cf. VirtualModificationIdenticals.Group for the ≡ analogue). statementIndex records where the
    // assignment happened, so a genuine reassignment (a later statement) can be told apart from a multi-valued
    // assignment (two arms of one statement, e.g. 'm = cond ? a : b' produces 'm ← a' and 'm ← b' at the same index).
    public record Assignment(Variable from, Variable to, String statementIndex) {
    }

    private final Set<Variable> variables = new LinkedHashSet<>();
    private final List<Assignment> assignments = new ArrayList<>();

    /**
     * Memo for {@code SharedVariables.assignmentSources}, which is a pure function of {@link #assignments} and
     * {@link #variables} — it rebuilds a forward adjacency map from the assignment list, walks it, and intersects
     * with the members. It is called from a triple-nested loop in {@code FollowGraph.followGraph} (per group
     * sibling, per rep expansion, per graph vertex) and that whole walk runs once per method call in a statement.
     * Recomputing it made a long straight-line method CUBIC in its statement count: Elasticsearch's
     * {@code RestIndicesAction} builds one table with 294 {@code table.addCell(...)} calls, each adding an
     * assignment to the same group, and MODIFICATION_LINK never finished on it (toolkit task #22).
     * <p>
     * Cached here rather than in the caller because this is where every mutation of the group passes.
     */
    private final java.util.Map<Variable, Set<Variable>> assignmentSourcesCache = new java.util.HashMap<>();

    /**
     * {@code from -> tos} over {@link #assignments}. It does not depend on the variable being asked about, so
     * building it inside each query was the second half of the same blow-up: with the memo above in place the
     * remaining cost was one full rebuild per distinct member, i.e. still quadratic in a long method. Built once
     * per group version, dropped by {@link #invalidate()} alongside the memo.
     */
    private java.util.Map<Variable, List<Variable>> forwardAssignments;

    public SharedVariable(String name, ParameterizedType parameterizedType, Runtime runtime) {
        super(name, parameterizedType, runtime.newEmptyExpression());
    }

    /**
     * The memoized {@code assignmentSources}. {@code compute} must be a pure function of this group's assignments
     * and members; it never re-enters this method, so {@code computeIfAbsent} is safe.
     */
    public Set<Variable> assignmentSources(Variable variable,
                                           java.util.function.Function<Variable, Set<Variable>> compute) {
        return assignmentSourcesCache.computeIfAbsent(variable, compute);
    }

    /** The forward adjacency of {@link #assignments}, shared by every query against this group. */
    public java.util.Map<Variable, List<Variable>> forwardAssignments() {
        if (forwardAssignments == null) {
            java.util.Map<Variable, List<Variable>> fwd = new java.util.HashMap<>();
            for (Assignment a : assignments) {
                fwd.computeIfAbsent(a.from(), k -> new ArrayList<>()).add(a.to());
            }
            forwardAssignments = fwd;
        }
        return forwardAssignments;
    }

    private void invalidate() {
        assignmentSourcesCache.clear();
        forwardAssignments = null;
    }

    public boolean add(Variable variable) {
        boolean added = variables.add(variable);
        if (added) invalidate();
        return added;
    }

    public void addAssignment(Variable from, Variable to, String statementIndex) {
        assignments.add(new Assignment(from, to, statementIndex));
        invalidate();
    }

    /** Fold another group's assignments in; goes through here so the memo is dropped (see {@link #merge}'s caller). */
    public void addAssignments(List<Assignment> toAdd) {
        if (toAdd.isEmpty()) return;
        assignments.addAll(toAdd);
        invalidate();
    }

    // 'from' is the recipient of an assignment recorded at a statement OTHER than 'statementIndex': a genuine
    // reassignment, as opposed to a second arm of the same (multi-valued) assignment.
    public boolean recipientAtOtherStatement(Variable from, String statementIndex) {
        return assignments.stream().anyMatch(a -> a.from().equals(from) && !a.statementIndex().equals(statementIndex));
    }

    public List<Assignment> assignments() {
        return assignments;
    }

    @Override
    public boolean acceptForLinkedVariables() {
        // a shared-variable rep is a synthetic group representative ($__sv_); it must never surface directly in
        // the output — its real members are expanded (iterateOverShared) and filtered individually. If a rep
        // reaches this filter unexpanded, drop it rather than emit the synthetic name (mirrors IntermediateVariable).
        return false;
    }

    public Set<Variable> variables() {
        return variables;
    }

    public void removeAll(Set<Variable> variables) {
        this.variables.removeAll(variables);
        assignments.removeIf(a -> variables.contains(a.from()) || variables.contains(a.to()));
        invalidate();
    }

    public void remove(Variable variable) {
        variables.remove(variable);
        assignments.removeIf(a -> variable.equals(a.from()) || variable.equals(a.to()));
        invalidate();
    }
}
