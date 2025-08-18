package org.e2immu.language.inspection.integration.java.importhelper;


public class RMultiLevel {

    public enum Effective {
        E1, E2;

        public static Effective of(int index) {
            return index == 1 ? E1: E2;
        }
    }
    public enum Level {
        ONE, TWO, THREE
    }
}
