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

import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableNature;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.support.Either;
import org.e2immu.support.SetOnce;

public class VariableInfoContainerImpl implements VariableInfoContainer {
    private final Variable variable;
    private final VariableNature variableNature;
    private final Either<VariableInfoContainer, VariableInfoImpl> previousOrInitial;
    private final VariableInfoImpl evaluation;
    private final SetOnce<VariableInfoImpl> merge;

    public VariableInfoContainerImpl(Variable variable,
                                     VariableNature variableNature,
                                     Either<VariableInfoContainer, VariableInfoImpl> previousOrInitial,
                                     VariableInfoImpl evaluation,
                                     boolean haveMerge) {
        this.variable = variable;
        this.variableNature = variableNature;
        this.previousOrInitial = previousOrInitial;
        this.evaluation = evaluation;
        this.merge = haveMerge ? new SetOnce<>() : null;
        assert evaluation == null || evaluation.variable() == variable;
        assert previousOrInitial.isLeft() && previousOrInitial.getLeft().variable() == variable
               || previousOrInitial.isRight() && previousOrInitial.getRight().variable() == variable;
    }

    public void setMerge(VariableInfoImpl merge) {
        this.merge.set(merge);
    }

    @Override
    public Variable variable() {
        return variable;
    }

    @Override
    public VariableNature variableNature() {
        return variableNature;
    }

    @Override
    public boolean hasEvaluation() {
        return evaluation != null;
    }

    @Override
    public boolean hasMerge() {
        return merge != null;
    }

    @Override
    public boolean isPrevious() {
        return previousOrInitial.isLeft();
    }

    @Override
    public boolean has(Stage stage) {
        return switch (stage) {
            case MERGE -> merge != null;
            case EVALUATION -> evaluation != null;
            case INITIAL -> isInitial();
        };
    }

    @Override
    public VariableInfo best() {
        return merge != null ? merge.get() : evaluation != null ? evaluation : getPreviousOrInitial();
    }

    @Override
    public VariableInfo best(Stage level) {
        if (level == Stage.MERGE && merge != null) {
            return merge.get();
        }
        if ((level == Stage.MERGE || level == Stage.EVALUATION) && evaluation != null) {
            return evaluation;
        }
        return getPreviousOrInitial();
    }

    @Override
    public VariableInfo getPreviousOrInitial() {
        return previousOrInitial.isLeft() ? previousOrInitial.getLeft().best() : previousOrInitial.getRight();
    }

    @Override
    public boolean isInitial() {
        return previousOrInitial.isRight();
    }

    @Override
    public boolean isRecursivelyInitial() {
        if (previousOrInitial.isRight()) return true;
        VariableInfoContainer previous = previousOrInitial.getLeft();
        if (!previous.hasEvaluation() && (stageForPrevious() == Stage.EVALUATION || !previous.hasMerge())) {
            return previous.isRecursivelyInitial();
        }
        return false;
    }

    @Override
    public VariableInfo getRecursiveInitial() {
        if (previousOrInitial.isRight()) return previousOrInitial.getRight();
        VariableInfoContainer previous = previousOrInitial.getLeft();
        return previous.getRecursiveInitial();
    }

    private Stage stageForPrevious() {
        VariableInfoContainer prev = previousOrInitial.getLeft();
        return prev.hasMerge() ? Stage.MERGE : Stage.EVALUATION;
    }

    @Override
    public VariableInfo bestCurrentlyComputed() {
        if (merge != null && merge.isSet()) return merge.get();
        return evaluation;
    }

    @Override
    public String indexOfDefinition() {
        if (evaluation != null) return evaluation.assignments().indexOfDefinition();
        if (merge != null && merge.isSet()) return merge.get().assignments().indexOfDefinition();
        if (previousOrInitial.isRight()) return previousOrInitial.getRight().assignments().indexOfDefinition();
        return previousOrInitial.getLeft().indexOfDefinition();
    }

    @Override
    public VariableInfoContainer flattened() {
        if (previousOrInitial.isRight()) return this; // already back-reference-free
        // Collapse the Either.left(previousVic) chain link to a value snapshot of what
        // getPreviousOrInitial() currently resolves to. best()/best(stage)/has(stage)/
        // bestCurrentlyComputed() are unchanged because this container's own evaluation and merge are
        // carried over verbatim; only the "came from a previous statement" structure is dropped. The
        // captured VariableInfoImpl holds this variable's value (assignments/reads/links), not the
        // previous containers, so the intermediate chain becomes collectible once its statements'
        // VARIABLE_DATA is dropped too.
        VariableInfoImpl previousSnapshot = (VariableInfoImpl) getPreviousOrInitial();
        VariableInfoContainerImpl copy = new VariableInfoContainerImpl(variable, variableNature,
                Either.right(previousSnapshot), evaluation, merge != null);
        if (merge != null && merge.isSet()) copy.setMerge(merge.get());
        return copy;
    }
}
