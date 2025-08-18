package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.Info;

public class AnalyzerException extends RuntimeException {
    private final Info info;
    public AnalyzerException(Info info, Throwable throwable) {
        super(throwable);
        this.info = info;
    }

    public Info getInfo() {
        return info;
    }
}
