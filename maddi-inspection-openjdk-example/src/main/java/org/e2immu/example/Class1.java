package org.e2immu.example;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Class1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Class1.class);

    private int method() {
        LOGGER.info("I'm here!");
        // return a constant
        return 3;
    }

    // a comment on a method
    protected void voidMethod() {
        int j = method();
        /* and one one a statement */
        System.out.println(j);
    }

    static class Enclosed<T> implements Comparable<Enclosed<T>> {
        List<T> list;

        @Override
        public int compareTo(@NotNull Enclosed<T> o) {
            return list.size() - o.list.size();
        }
    }
}
