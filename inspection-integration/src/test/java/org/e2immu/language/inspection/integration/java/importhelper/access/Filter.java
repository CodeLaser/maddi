package org.e2immu.language.inspection.integration.java.importhelper.access;

public interface Filter {

    Result filter(String s);

    enum Result {
        ACCEPT, NEUTRAL, DENY;
    }
}
