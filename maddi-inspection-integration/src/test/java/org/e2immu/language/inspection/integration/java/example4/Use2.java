package org.e2immu.language.inspection.integration.java.example4;


import static org.e2immu.language.inspection.integration.java.example4.impl.StaticValuesImpl.*;

public class Use2 {
    SubValue sv;
    //not accessible: StaticValues sv;
    //not accessible: Bool bool;
    //not accessible: Immutable immutable;
    //not accessible: ImmutableImpl i;
    //not accessible: ValueImpl v;
    //not accessible: Value v;

    Object method() {
        return SV;
    }
}
