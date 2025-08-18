package org.e2immu.language.cst.api.output.element;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputElement;

public interface Space extends OutputElement {
    ElementarySpace elementarySpace(FormattingOptions options);

    ElementarySpace nice();

    Split split();
}
