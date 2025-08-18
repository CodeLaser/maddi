package org.e2immu.language.cst.print;

import org.e2immu.language.cst.api.output.element.TypeName;
import org.e2immu.language.cst.impl.output.TypeNameImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeName {

    @Test
    public void test() {
        TypeName typeName1 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE,false);
        assertEquals("Bar.Bar2", typeName1.minimal());
        TypeName typeName2 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.FQN, false);
        assertEquals("com.foo.Bar.Bar2", typeName2.minimal());
        TypeName typeName3 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.DOLLARIZED_FQN, false);
        assertEquals("com.foo.Bar$Bar2", typeName3.minimal());

        TypeName typeName4 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.DOLLARIZED_FQN, true);
        assertEquals("@com.foo.Bar$Bar2", typeName4.minimal());
    }
}
