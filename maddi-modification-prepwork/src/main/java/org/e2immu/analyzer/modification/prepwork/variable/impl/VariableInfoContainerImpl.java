package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.support.Either;
import org.e2immu.support.SetOnce;

import java.util.Objects;

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
    public boolean setLinkedVariables(LinkedVariables linkedVariables, Stage level) {
        Objects.requireNonNull(linkedVariables);
        assert level != Stage.INITIAL;
        VariableInfoImpl variableInfo = getToWrite(level);
        return variableInfo.setLinkedVariables(linkedVariables);
    }

    private VariableInfoImpl getToWrite(Stage stage) {
        return switch (stage) {
            case INITIAL -> (VariableInfoImpl) getRecursiveInitialOrNull();
            case EVALUATION -> Objects.requireNonNull(evaluation);
            case MERGE -> Objects.requireNonNull(merge.get());
        };
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
    public VariableInfo getRecursiveInitialOrNull() {
        if (previousOrInitial.isRight()) return previousOrInitial.getRight();
        VariableInfoContainer previous = previousOrInitial.getLeft();
        if (!previous.hasEvaluation() && (stageForPrevious() == Stage.EVALUATION || !previous.hasMerge())) {
            return previous.getRecursiveInitialOrNull();
        }
        return null;
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
}
