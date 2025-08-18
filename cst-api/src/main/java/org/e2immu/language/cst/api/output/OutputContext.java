package org.e2immu.language.cst.api.output;

public interface OutputContext {
    FormattingOptions defaultFormattingOptions();

    Formatter newFormatter(FormattingOptions formattingOptions);
}
