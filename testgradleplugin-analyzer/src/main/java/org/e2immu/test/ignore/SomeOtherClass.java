package org.e2immu.test.ignore;

import org.e2immu.annotation.ImmutableContainer;

@ImmutableContainer
public abstract class SomeOtherClass<T> {

    public abstract T make();

}
