package org.e2immu.language.cst.api.analysis;

public interface Property {
    Class<? extends Value> classOfValue();

    Value defaultValue();

    String key();
}
