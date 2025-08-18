package org.e2immu.language.cst.api.output.element;

import org.e2immu.language.cst.api.output.OutputElement;

public interface Guide extends OutputElement {
    boolean allowNewLineBefore();

    boolean endWithNewLine();

    int index();

    boolean positionIsEnd();

    boolean positionIsMid();

    boolean positionIsStart();

    boolean prioritySplit();

    boolean startWithNewLine();

    int tabs();
}
