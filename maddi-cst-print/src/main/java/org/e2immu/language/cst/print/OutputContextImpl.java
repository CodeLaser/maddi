package org.e2immu.language.cst.print;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputContext;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;

public record OutputContextImpl(Runtime runtime) implements OutputContext {

    @Override
    public FormattingOptions defaultFormattingOptions() {
        return FormattingOptionsImpl.DEFAULT;
    }

    @Override
    public Formatter newFormatter(FormattingOptions formattingOptions) {
        return new FormatterImpl(runtime, formattingOptions);
    }
}
