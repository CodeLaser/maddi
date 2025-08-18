package org.e2immu.language.inspection.integration.java.example;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

public class Import4 {
    interface BB extends Bool {

    }
    Bool fieldValue;
    Immutable fieldValueImpl;
    Independent independent = INDEPENDENT;
    //FAILS:  IndependentImpl independent2;
    //FAILS:  Value value;
    //FAILS:  ValueImpl value;
    Independent i2 = from(3);
}
