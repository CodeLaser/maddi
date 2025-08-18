package org.e2immu.language.cst.impl.type;

import org.e2immu.language.cst.api.type.Diamond;

public enum DiamondEnum implements Diamond {
    NO, YES, SHOW_ALL;

    @Override
    public boolean isYes() {
        return this == YES;
    }

    @Override
    public boolean isNo() {
        return this == NO;
    }

    @Override
    public boolean isShowAll() {
        return this == SHOW_ALL;
    }
}
