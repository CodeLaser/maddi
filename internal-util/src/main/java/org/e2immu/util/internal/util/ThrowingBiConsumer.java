package org.e2immu.util.internal.util;

import org.e2immu.annotation.Modified;

@FunctionalInterface
public interface ThrowingBiConsumer<S, T> {

    @Modified
    void accept(S s, T t) throws Exception;
}

