package io.codelaser.maddi.test.main;

import io.codelaser.maddi.util.IntUtil;

import java.util.HashSet;
import java.util.Set;

public abstract class SomeClass<T> {

    private final Set<T> ts = new HashSet<>();

    public abstract T make();

    public void modifying(T t) {
        ts.add(t);
    }

    public Set<T> getTs() {
        return ts;
    }


    public boolean go(Double x) {
        return IntUtil.isMathematicalInteger(x);
    }
}
