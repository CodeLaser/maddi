package org.e2immu.analyzer.modification.link;

public enum LinkNature {
    NONE("X"), // there cannot be a link
    EMPTY("?"), // start of algorithms
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

    /*
    used to find the "best" relation among the different paths from one variable to another.
     */
    public LinkNature best(LinkNature other) {
        if (this == IS_IDENTICAL_TO || other == IS_IDENTICAL_TO) return IS_IDENTICAL_TO;
        if (this == INTERSECTION_NOT_EMPTY || other == INTERSECTION_NOT_EMPTY) return INTERSECTION_NOT_EMPTY;
        if (this == IS_ELEMENT_OF || other == IS_ELEMENT_OF) return IS_ELEMENT_OF;
        if (this == CONTAINS || other == CONTAINS) return CONTAINS;
        return this;
    }

    public LinkNature reverse() {
        if (this == IS_ELEMENT_OF) return CONTAINS;
        if (this == CONTAINS) return IS_ELEMENT_OF;
        return this;
    }

    @Override
    public String toString() {
        return label;
    }

    public LinkNature combine(LinkNature other) {
        if (this == other) return this;
        if (this == EMPTY) return other;
        if (other == EMPTY) return this;
        if (this == NONE || other == NONE) return NONE;
        if (other == IS_IDENTICAL_TO) return this;
        if (this == IS_IDENTICAL_TO) return other;
        if (this == IS_ELEMENT_OF) {
            return switch (other) {
                case INTERSECTION_NOT_EMPTY -> IS_ELEMENT_OF;
                case CONTAINS -> NONE; // asymmetric!
                default -> throw new UnsupportedOperationException();
            };
        }
        if (this == CONTAINS) {
            return switch (other) {
                case INTERSECTION_NOT_EMPTY -> IS_ELEMENT_OF;
                case IS_ELEMENT_OF -> INTERSECTION_NOT_EMPTY; // asymmetric
                default -> throw new UnsupportedOperationException();
            };
        }
        return NONE;
    }

    public boolean valid() {
        return this != NONE && this != EMPTY;
    }
}
