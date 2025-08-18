package org.e2immu.language.cst.api.type;

public interface Wildcard {
    boolean isUnbound();

    boolean isExtends();

    boolean isSuper();
}
