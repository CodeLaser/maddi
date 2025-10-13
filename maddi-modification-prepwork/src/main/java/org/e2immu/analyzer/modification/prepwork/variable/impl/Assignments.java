/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.variable.impl;


import org.e2immu.analyzer.modification.prepwork.StatementIndex;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.language.cst.api.statement.*;

import java.util.*;
import java.util.stream.Stream;

public class Assignments {

    public boolean hasNotYetBeenAssigned() {
        return assignmentIndices.length == 0;
    }

    private final String indexOfDefinition;
    private final String[] assignmentIndices;

    public Assignments(String indexOfDefinition) {
        this.indexOfDefinition = indexOfDefinition;
        assignmentIndices = new String[0];
    }

    private Assignments(Assignments previous, String[] assignmentIndices) {
        this.assignmentIndices = assignmentIndices;
        this.indexOfDefinition = previous.indexOfDefinition;
    }

    private Assignments(Assignments previous, List<String> indices) {
        this.assignmentIndices = Stream.concat(Arrays.stream(previous.assignmentIndices),
                indices.stream()).toArray(String[]::new);
        this.indexOfDefinition = previous.indexOfDefinition;
    }

    public boolean isDefinedInEnclosingMethod() {
        return StatementIndex.ENCLOSING_METHOD.equals(indexOfDefinition);
    }

    public boolean isEmpty() {
        return assignmentIndices.length == 0;
    }

    public int size() {
        return assignmentIndices.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assignments that)) return false;
        return Objects.equals(indexOfDefinition, that.indexOfDefinition)
               && Objects.deepEquals(assignmentIndices, that.assignmentIndices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexOfDefinition, Arrays.hashCode(assignmentIndices));
    }

    @Override
    public String toString() {
        return "D:" + indexOfDefinition + ", A:" + Arrays.toString(assignmentIndices);
    }

    public static Assignments newAssignment(List<String> indices, Assignments previous) {
        return new Assignments(previous, indices);
    }

    // for testing
    public static Assignments newAssignment(String index, Assignments previous) {
        return new Assignments(previous, List.of(index));
    }

    public interface CompleteMerge {
        void add(String subIndex);

        boolean complete();
    }

    static class CompleteMergeByCounting implements CompleteMerge {
        int count;
        final int target;

        CompleteMergeByCounting(int target) {
            this.target = target;
        }

        @Override
        public void add(String subIndex) {
            ++count;
        }

        @Override
        public boolean complete() {
            return count >= target;
        }
    }

    static class CompleteMergeForTry implements CompleteMerge {
        int count;
        boolean hitFinally;
        final int target;
        final String indexFinally;

        CompleteMergeForTry(int target, String indexFinally) {
            this.target = target;
            this.indexFinally = indexFinally;
        }

        @Override
        public void add(String subIndex) {
            if (subIndex.equals(indexFinally)) {
                hitFinally = true;
            } else {
                ++count;
            }
        }

        @Override
        public boolean complete() {
            return hitFinally || count >= target;
        }
    }

    /*
    There are different "modes" depending on the statement.

    1- If, For, ForEach, While: 1 block, conditionally
    2- IfElse: either block 1, or block 2
    3- Synchronized, Block, Do-While, While(true), For(..;;..): 1 block, unconditionally
    4- Try: 1 block unconditionally, n blocks conditionally, exclusive, 0-1 block unconditionally
     */
    public static Assignments mergeBlocks(String index,
                                          CompleteMerge completeMerge,
                                          Map<String, Assignments> assignmentsInBlocks,
                                          Map<String, List<Assignments>> assignmentsToAddFromFallThrough) {
        List<String> list = new ArrayList<>();
        Assignments aFirst = null;
        boolean haveAtLeastOneAssignment = false;
        for (Map.Entry<String, Assignments> entry : assignmentsInBlocks.entrySet()) {
            Assignments a = entry.getValue();
            String subIndex = entry.getKey();

            List<Assignments> fromFallThrough = assignmentsToAddFromFallThrough.get(subIndex);
            if (fromFallThrough != null) {
                fromFallThrough.forEach(ff -> list.addAll(Arrays.asList(ff.assignmentIndices)));
            }

            boolean haveAssignment = false;
            for (String s : a.assignmentIndices) {
                if (s.compareTo(index) >= 0) {
                    haveAssignment |= Util.atSameLevel(subIndex, s);
                }
                list.add(s);
            }
            if (aFirst == null) aFirst = a;
            if (haveAssignment) {
                // nothing was added, so no assignment
                completeMerge.add(subIndex);
            }
            haveAtLeastOneAssignment |= haveAssignment;
        }
        assert aFirst != null;
        if (completeMerge.complete() && haveAtLeastOneAssignment) {
            String mergeIndex = index + StatementIndex.MERGE;
            list.add(mergeIndex);
        }
        return new Assignments(aFirst, list.stream().distinct().sorted().toArray(String[]::new));
    }

    public static CompleteMerge assignmentsRequiredForMerge(Statement statement,
                                                            Map<String, VariableData> lastOfEachStatement,
                                                            ReturnVariable returnVariable,
                                                            boolean noBreakStatements) {
        int blocksWithReturn;
        if (returnVariable == null) {
            // when we're computing this for the return variable, no corrections allowed
            blocksWithReturn = 0;
        } else {
            blocksWithReturn = (int) lastOfEachStatement.entrySet().stream()
                    .filter(e -> {
                        String firstStatementIndex = e.getKey();
                        VariableData vd = e.getValue();
                        VariableInfoContainer vic = vd.variableInfoContainerOrNull(returnVariable.fullyQualifiedName());
                        if (vic == null) return false;
                        VariableInfo vi = vic.best();
                        return vi.assignments().lastAssignmentIsMergeInBlockOf(firstStatementIndex);
                    })
                    .count();
        }
        if (statement instanceof TryStatement ts) {
            int target = 1 + ts.catchClauses().size() - blocksWithReturn;
            int finallyIndex = (ts.resources().isEmpty() ? 1 : 2) + ts.catchClauses().size();
            boolean haveFinally = !ts.finallyBlock().isEmpty();
            String haveFinallyIndex = haveFinally
                    ? ts.source().index() + StatementIndex.DOT + finallyIndex + StatementIndex.DOT_ZERO
                    : null;
            return new CompleteMergeForTry(target, haveFinallyIndex);
        }
        int n;
        if (statement instanceof IfElseStatement) {
            n = 2;
        } else if (statement instanceof SwitchStatementNewStyle) {
            n = (int) statement.subBlockStream().count();
        } else if (statement instanceof Block
                   || statement instanceof SynchronizedStatement
                   || statement instanceof DoStatement && !statement.expression().isBoolValueTrue()) {
            n = 1;
        } else if (statement instanceof LoopStatement) {
            if ((statement.expression().isEmpty() || statement.expression().isBoolValueTrue())
                && noBreakStatements) {
                n = 0; // we'll always return, or never continue
            } else {
                n = Integer.MAX_VALUE;// there can be no merge
            }
        } else if (statement instanceof SwitchStatementOldStyle) {
            n = lastOfEachStatement.size();
        } else {
            throw new UnsupportedOperationException("NYI");
        }
        return new CompleteMergeByCounting(n - blocksWithReturn);
    }

    /*
    first statement: 1.0.0, last assignment: 1.0.5-> OK, 1.0.4.0.1 not OK!
     */
    public boolean lastAssignmentIsMergeInBlockOf(String firstStatementIndex) {
        if (assignmentIndices.length == 0) return false;
        String last = assignmentIndices[assignmentIndices.length - 1];
        return Util.atSameLevel(firstStatementIndex, last);
    }

    public String indexOfDefinition() {
        return indexOfDefinition;
    }

    /*
    has the variable been assigned at this index?
     */
    public boolean hasAValueAt(String index) {
        if (indexOfDefinition.compareTo(index) > 0) {
            return false;
        }
        if (assignmentIndices.length == 0) return false;
        int pos = Arrays.binarySearch(assignmentIndices, index);
        if (pos >= 0) return true; // we're hitting a definition point
        int insert = -(pos + 1);
        if (insert == 0) return false;
        for (int k = insert - 1; k >= 0; k--) {
            String s = assignmentIndices[k];
            if (StatementIndex.seenBy(s, index)) return true;
        }
        return false;
    }

    public boolean isReassignment(String index) {
        int pos = Arrays.binarySearch(assignmentIndices, index);
        for (int k = pos - 1; k >= 0; k--) {
            String s = stripMerge(assignmentIndices[k]);
            if (Util.atSameLevel(s, index)) return true;
            int lastDot = index.lastIndexOf('.');
            while (lastDot > 0) {
                String sub = index.substring(0, lastDot);
                if (Util.atSameLevel(s, sub)) return true;
                lastDot = sub.lastIndexOf('.');
            }
        }
        return false;
    }

    public static String stripMerge(String s) {
        if (s.endsWith("=M")) return s.substring(0, s.length() - 2);
        return s;
    }

    public static String stripEvalOrMerge(String s) {
        if (s.endsWith("=M") || s.endsWith("-E")) return s.substring(0, s.length() - 2);
        return s;
    }

    public static boolean isMerge(String s) {
        return s.endsWith("=M");
    }

    /*
    do we have an assignment after 'after', seen by 'seenBy'?


    examples:
    0 - 1 - 2 --> yes
    0 - 1.0.0 - 1.1.0 --> no
    1.1.0 - 2 - 2.0.1 --> yes
     */
    public boolean hasBeenAssignedAfterFor(String after, String seenBy, boolean inclusive) {
        if (indexOfDefinition.compareTo(after) >= 0) {
            return true;
        }
        if (assignmentIndices.length == 0) return false;
        int pos = Arrays.binarySearch(assignmentIndices, after);
        int start;
        if (pos >= 0) {
            start = pos + (inclusive ? 0 : 1);
        } else {
            start = -(pos + 1);
        }
        for (int i = start; i < assignmentIndices.length; i++) {
            if (StatementIndex.seenBy(assignmentIndices[i], seenBy)) return true;
        }
        return false;
    }

    public List<String> allIndicesStripStage() {
        return Arrays.stream(assignmentIndices).map(Util::stripStage).toList();
    }

    public Stream<String> definitionAndAssignmentsBefore(String upper) {
        if (indexOfDefinition.compareTo(upper) >= 0) return Stream.of();
        return Stream.concat(Stream.of(indexOfDefinition),
                Arrays.stream(assignmentIndices).takeWhile(s -> s.compareTo(upper) < 0));
    }

    public boolean between(String fromIncl, String toExcl) {
        if (fromIncl.compareTo(toExcl) >= 0) return false;

        int pos0 = Arrays.binarySearch(assignmentIndices, fromIncl);
        int p0 = pos0 >= 0 ? pos0 : -(pos0 + 1);
        if (p0 >= assignmentIndices.length) return false;
        String s0 = assignmentIndices[p0];
        if (s0.compareTo(fromIncl) < 0) return false;
        return s0.compareTo(toExcl) < 0;
    }

    public boolean contains(String index) {
        return Arrays.binarySearch(assignmentIndices, index) >= 0;
    }

    public Iterable<String> from(String from, boolean includeFrom) {
        return from(from, includeFrom, false);
    }

    public Iterable<String> from(String from, boolean includeFrom, boolean flexible) {
        int k1 = Arrays.binarySearch(assignmentIndices, from);
        boolean notFound = k1 < 0;
        int k;
        if (flexible) {
            k = notFound ? -k1 - 1 : k1;
        } else {
            k = k1;
            assert k >= 0;
        }
        return () -> new Iterator<>() {
            int i = k + (includeFrom || notFound ? 0 : 1);

            @Override
            public boolean hasNext() {
                return i < assignmentIndices.length;
            }

            @Override
            public String next() {
                return assignmentIndices[i++];
            }
        };
    }

    public boolean assignedAtCreation() {
        return indexOfDefinition.equals(assignmentIndices[0]);
    }
}

