package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;

public class LinkNatureImpl implements LinkNature {
    public static final LinkNatureImpl NONE = new LinkNatureImpl("X", -2, false);
    public static final LinkNatureImpl EMPTY = new LinkNatureImpl("∅", -1, false);

    public static final LinkNatureImpl IS_IDENTICAL_TO = new LinkNatureImpl("≡", 0, false);

    public static final LinkNatureImpl SHARES_ELEMENTS = new LinkNatureImpl("~", 1, false);
    public static final LinkNatureImpl SHARES_FIELDS = new LinkNatureImpl("≈", 2, false);

    public static final LinkNatureImpl IS_SUBSET_OF = new LinkNatureImpl("⊆", 3, false);
    public static final LinkNatureImpl IS_SUPERSET_OF = new LinkNatureImpl("⊇", 4, false);

    public static final LinkNatureImpl IS_ELEMENT_OF = new LinkNatureImpl("∈", 5, true);
    public static final LinkNatureImpl CONTAINS_AS_MEMBER = new LinkNatureImpl("∋", 6, true);

    // precedes, succeeds
    // element inclusion in formal objects
    public static final LinkNatureImpl IS_FIELD_OF = new LinkNatureImpl("≺", 7, true);
    public static final LinkNatureImpl CONTAINS_AS_FIELD = new LinkNatureImpl("≻", 8, true);

    private final String symbol;
    private final int rank;
    private final boolean multiply;

    private LinkNatureImpl(String symbol, int rank, boolean multiply) {
        this.symbol = symbol;
        this.rank = rank;
        this.multiply = multiply;
    }

    @Override
    public boolean isIdenticalTo() {
        return this == IS_IDENTICAL_TO;
    }

    @Override
    public int rank() {
        return rank;
    }

    @Override
    public boolean multiplySymbols() {
        return multiply;
    }

    @Override
    public LinkNature best(LinkNature other) {
        return rank < other.rank() ? other : this;
    }

    @Override
    public boolean valid() {
        return rank >= 0;
    }

    @Override
    public LinkNature reverse() {
        if (this == IS_ELEMENT_OF) return CONTAINS_AS_MEMBER;
        if (this == CONTAINS_AS_MEMBER) return IS_ELEMENT_OF;
        if (this == IS_FIELD_OF) return CONTAINS_AS_FIELD;
        if (this == CONTAINS_AS_FIELD) return IS_FIELD_OF;
        if (this == IS_SUBSET_OF) return IS_SUPERSET_OF;
        if (this == IS_SUPERSET_OF) return IS_SUBSET_OF;
        return this;
    }

    @Override
    public String toString() {
        return symbol;
    }

    public LinkNature combine(LinkNature other) {
        if (this == other) return this;
        if (this == EMPTY) return other;
        if (other == EMPTY) return this;
        if (this == NONE || other == NONE) return NONE;
        if (other == IS_IDENTICAL_TO) return this;
        if (this == IS_IDENTICAL_TO) return other;

        if (this == IS_ELEMENT_OF) {
            if (other == IS_SUBSET_OF) return IS_ELEMENT_OF;
            return NONE;
        }

        if (this == CONTAINS_AS_MEMBER) {
            // A ∋ a, a ∈ B -> A ~ B
            if (other == IS_ELEMENT_OF) return SHARES_ELEMENTS;
            return NONE;
        }

        if (this == IS_SUPERSET_OF) {
            if (other == CONTAINS_AS_MEMBER) return CONTAINS_AS_MEMBER;
        }

        if (this == CONTAINS_AS_FIELD) {
            // the equivalent of a ∈ A, a ∈ B -> A ~ B
            if (other == IS_FIELD_OF) return SHARES_FIELDS;
        }
        return NONE;
    }

}
