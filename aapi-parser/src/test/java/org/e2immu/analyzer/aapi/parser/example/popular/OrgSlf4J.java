package org.e2immu.analyzer.aapi.parser.example.popular;

import org.e2immu.annotation.*;

public class OrgSlf4J {
    public static final String PACKAGE_NAME = "org.slf4j";

    @ImmutableContainer
    interface Logger$ {

        @NotModified
        void info(@NotNull String s, Object... objects);

        @NotModified
        void warn(@NotNull String s, Object... objects);

        @NotModified
        void error(@NotNull String s);

        @NotModified
        void error(@NotNull String s, Object object);

        @NotModified
        void error(@NotNull String s, Object object1, Object object2);

        @NotModified
        void error(@NotNull String s, Object... objects);

        @NotModified
        void debug(@NotNull String s, Object object);

        @NotModified
        void debug(@NotNull String s, Object object1, Object object2);

        @NotModified
        void debug(@NotNull String s, Object... objects);

        @NotModified
        void debug(@NotNull String s);
    }

    @Container
    @Independent
    interface ILoggerFactory$ {
        @NotNull
        @NotModified
        @ImmutableContainer
        org.slf4j.Logger getLogger(String name);
    }

    @Container
    @Independent
    interface LoggerFactory$ {
        @NotNull
        @NotModified
        @ImmutableContainer
        org.slf4j.Logger getLogger(@NotNull Class<?> clazz);

        @NotNull
        @ImmutableContainer
        @NotModified
        org.slf4j.Logger getLogger(@NotNull String string);

        @NotNull
        @NotModified
        org.slf4j.ILoggerFactory getILoggerFactory();
    }

    @Container
    interface Marker$ {

    }
}
