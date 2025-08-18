package org.e2immu.language.cst.impl.variable;

import org.e2immu.language.cst.api.variable.DescendMode;

public enum DescendModeEnum implements DescendMode {
    YES,
    NO;

    @Override
    public boolean isNo() {
        return NO == this;
    }

    @Override
    public boolean isYes() {
        return YES == this;
    }
}
