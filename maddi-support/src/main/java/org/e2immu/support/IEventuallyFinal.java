package org.e2immu.support;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;
public interface IEventuallyFinal<T> {
    @TestMark(value = "isFinal", before = true) boolean isVariable();
    @Mark("isFinal") void setFinal(T value);
    T get();
    @TestMark("isFinal") boolean isFinal();
    @Only(before = "isFinal") void setVariable(T value);
}
