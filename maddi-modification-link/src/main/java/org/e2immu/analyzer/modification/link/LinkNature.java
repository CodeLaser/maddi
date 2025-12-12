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
    CONTAINS(">"),

    IS_FIELD_OF(":"),
    HAS_FIELD(".");
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
        if (this == IS_FIELD_OF || other == IS_FIELD_OF) return IS_FIELD_OF;
        if (this == HAS_FIELD || other == HAS_FIELD) return HAS_FIELD;
        return this;
    }

    public LinkNature reverse() {
        if (this == IS_ELEMENT_OF) return CONTAINS;
        if (this == CONTAINS) return IS_ELEMENT_OF;
        if (this == IS_FIELD_OF) return HAS_FIELD;
        if (this == HAS_FIELD) return IS_FIELD_OF;
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
                case INTERSECTION_NOT_EMPTY, IS_FIELD_OF -> IS_ELEMENT_OF;
                case CONTAINS, HAS_FIELD -> NONE; // asymmetric!
                default -> throw new UnsupportedOperationException();
            };
        }

        if (this == CONTAINS) {
            return switch (other) {
                case IS_FIELD_OF -> NONE;
                case HAS_FIELD, INTERSECTION_NOT_EMPTY -> CONTAINS;
                case IS_ELEMENT_OF -> INTERSECTION_NOT_EMPTY; // asymmetric
                default -> throw new UnsupportedOperationException();
            };
        }

        if (this == IS_FIELD_OF) {
            return switch (other) {
                case IS_ELEMENT_OF -> IS_ELEMENT_OF;
                case INTERSECTION_NOT_EMPTY -> INTERSECTION_NOT_EMPTY;
                case CONTAINS, HAS_FIELD -> NONE; // asymmetric!
                default -> throw new UnsupportedOperationException();
            };
        }

        if (this == HAS_FIELD) {
            return switch (other) {
                case INTERSECTION_NOT_EMPTY, IS_ELEMENT_OF, IS_FIELD_OF -> INTERSECTION_NOT_EMPTY; // asymmetric
                case CONTAINS -> CONTAINS;
                default -> throw new UnsupportedOperationException();
            };
        }
        return NONE;
    }

    public boolean valid() {
        return this != NONE && this != EMPTY;
    }
}
