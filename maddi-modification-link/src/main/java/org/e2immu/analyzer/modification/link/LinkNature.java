package org.e2immu.analyzer.modification.link;

public enum LinkNature {
    NONE("X", -1), // there cannot be a link
    EMPTY("?", -2), // start of algorithms
    // more than 0-0, object identity
    IS_IDENTICAL_TO("==", 0),

    //0-0
    INTERSECTION_NOT_EMPTY("~", 1),

    //*-0
    IS_ELEMENT_OF("<", 2),
    //0-*
    CONTAINS(">", 3);

    private final String label;
    private final long longValue;

    LinkNature(String label, long longValue) {
        this.label = label;
        this.longValue = longValue;
    }

    public static LinkNature of(int i) {
        return switch (i) {
            case -2 -> EMPTY;
            case -1 -> NONE;
            case 0 -> IS_IDENTICAL_TO;
            case 1 -> INTERSECTION_NOT_EMPTY;
            case 2 -> IS_ELEMENT_OF;
            case 3 -> CONTAINS;
            default -> throw new UnsupportedOperationException();
        };
    }

    public long longValue() {
        return this.longValue;
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
                case CONTAINS -> INTERSECTION_NOT_EMPTY;
                default -> throw new UnsupportedOperationException();
            };
        }
        if (this == CONTAINS) {
            return switch (other) {
                case INTERSECTION_NOT_EMPTY -> IS_ELEMENT_OF;
                case IS_ELEMENT_OF -> INTERSECTION_NOT_EMPTY;
                default -> throw new UnsupportedOperationException();
            };
        }
        return NONE;
    }

    public static long combineLongs(long l1, long l2) {
        if (l1 < 0 || l2 < 0) return -1;
        LinkNature ln1 = values()[(int) l1];
        LinkNature ln2 = values()[(int) l2];
        LinkNature combined = ln1.combine(ln2);
        return combined == null ? -1 : combined.longValue;
    }
}
