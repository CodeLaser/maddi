package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;

public interface MethodPrinter {
    OutputBuilder print(Qualification qualification);
}
