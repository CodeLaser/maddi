package org.e2immu.analyzer.aapi.archive.v2.libs.test;
import org.e2immu.annotation.Independent;
public class OrgJunitJupiterApiFunction {
    public static final String PACKAGE_NAME = "org.junit.jupiter.api.function";
    //public interface Executable
    //@Container[M]
    @Independent class Executable$ {void execute() { } }

    //public interface ThrowingSupplier
    //@Container[M]
    class ThrowingSupplier$<T> {
        //@Independent(hc=true)[T]
        T get() { return null; }
    }
}
