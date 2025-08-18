package org.e2immu.language.cst.api.analysis;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;

public interface Message {

    interface Level {
        boolean isWarning();

        boolean isError();
    }

    Source source();

    Info info();

    String message();

    Level level();
}
