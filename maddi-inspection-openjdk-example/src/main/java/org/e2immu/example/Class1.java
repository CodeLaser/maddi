package org.e2immu.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Class1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Class1.class);

    private int method() {
        LOGGER.info("I'm here!");
        // return a constant
        return 3;
    }

    // a comment on a method
    protected void voidMethod() {
        /* and one one a statement */
        System.out.println();
    }
}
