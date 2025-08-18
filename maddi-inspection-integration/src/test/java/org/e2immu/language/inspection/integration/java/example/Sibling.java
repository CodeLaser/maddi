package org.e2immu.language.inspection.integration.java.example;

import org.e2immu.language.inspection.integration.java.example.sub.Parent;

public class Sibling {
    class SubSibling extends Parent {
        void method(SubInterface subInterface) {
            subInterface.method();
        }
    }
}
