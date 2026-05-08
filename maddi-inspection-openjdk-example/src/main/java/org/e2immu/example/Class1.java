package org.e2immu.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Class1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Class1.class);

    private int method() {
        LOGGER.info("I'm here!");
        return 3;
    }

    protected void voidMethod() {
        System.out.println();
    }
}
