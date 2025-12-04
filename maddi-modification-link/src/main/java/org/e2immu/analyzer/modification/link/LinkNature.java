package org.e2immu.analyzer.modification.link;

public enum LinkNature {
    // more than 0-0, object identity
    IS_IDENTICAL_TO("=="),

    //0-0
    INTERSECTION_NOT_EMPTY("~"),

    //*-0
    IS_ELEMENT_OF("<"),
    //0-*
    CONTAINS(">");

    private final String label;

    LinkNature(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
