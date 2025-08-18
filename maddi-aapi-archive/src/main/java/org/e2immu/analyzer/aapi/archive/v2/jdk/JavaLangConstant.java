package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.NotNull;

import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class JavaLangConstant {
    public static final String PACKAGE_NAME = "java.lang.constant";

    @ImmutableContainer
    @Independent
    interface ConstantDesc$ {
        @NotNull
        Object resolveConstantDesc(MethodHandles.Lookup lookup);
    }

    //public interface Constable
    //@Container[M]
    @ImmutableContainer
    @Independent
    class Constable$ {
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotNull
        Optional<? extends ConstantDesc> describeConstable() { return null; }
    }
}
