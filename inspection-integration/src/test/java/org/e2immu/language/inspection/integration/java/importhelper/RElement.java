package org.e2immu.language.inspection.integration.java.importhelper;

public interface RElement {

    enum DescendMode {
        NO,
        YES,
        YES_INCLUDE_THIS
    }

    void doSomething(DescendMode descendMode);
}
