package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;

//https://unicodemap.com/range/47/Mathematical_Operators/

public class LinkNatureImpl implements LinkNature {
    public static final LinkNatureImpl NONE = new LinkNatureImpl("X", -2);
    public static final LinkNatureImpl EMPTY = new LinkNatureImpl("∅", -1);

    public static final LinkNatureImpl OBJECT_GRAPH_OVERLAPS = new LinkNatureImpl("∩", 0);
    public static final LinkNatureImpl IS_IN_OBJECT_GRAPH = new LinkNatureImpl("≤", 1);
    public static final LinkNatureImpl OBJECT_GRAPH_CONTAINS = new LinkNatureImpl("≥", 2);

    public static final LinkNatureImpl SHARES_ELEMENTS = new LinkNatureImpl("~", 3);
    public static final LinkNatureImpl SHARES_FIELDS = new LinkNatureImpl("≈", 4);

    public static final LinkNatureImpl IS_SUBSET_OF = new LinkNatureImpl("⊆", 5);
    public static final LinkNatureImpl IS_SUPERSET_OF = new LinkNatureImpl("⊇", 6);

    public static final LinkNatureImpl IS_ELEMENT_OF = new LinkNatureImpl("∈", 7);
    public static final LinkNatureImpl CONTAINS_AS_MEMBER = new LinkNatureImpl("∋", 7);

    public static final LinkNatureImpl IS_IDENTICAL_TO = new LinkNatureImpl("≡", 9);

    // precedes, succeeds
    // element inclusion in formal objects
    public static final LinkNatureImpl IS_FIELD_OF = new LinkNatureImpl("≺", 10);
    public static final LinkNatureImpl CONTAINS_AS_FIELD = new LinkNatureImpl("≻", 11);


    private final String symbol;
    private final int rank;

    private LinkNatureImpl(String symbol, int rank) {
        this.symbol = symbol;
        this.rank = rank;
    }

    @Override
    public boolean multiplySymbols() {
        return this == IS_ELEMENT_OF || this == CONTAINS_AS_MEMBER;
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
            if (other == IS_IN_OBJECT_GRAPH || other == IS_FIELD_OF
                || other == SHARES_ELEMENTS || other == SHARES_FIELDS) return IS_IN_OBJECT_GRAPH;
        }

        if (this == IS_FIELD_OF) {
            if (other == IS_ELEMENT_OF
                || other == IS_SUBSET_OF
                || other == IS_IN_OBJECT_GRAPH
                || other == SHARES_ELEMENTS || other == SHARES_FIELDS) return IS_IN_OBJECT_GRAPH;
        }

        if (this == IS_SUBSET_OF) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_IN_OBJECT_GRAPH
                || other == SHARES_ELEMENTS || other == SHARES_FIELDS) return IS_IN_OBJECT_GRAPH;
        }

        if (this == CONTAINS_AS_MEMBER) {
            // A ∋ a, a ∈ B -> A ~ B
            if (other == IS_ELEMENT_OF) return SHARES_ELEMENTS;
            if (other == IS_FIELD_OF
                || other == IS_SUBSET_OF
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
            if (other == CONTAINS_AS_FIELD
                || other == IS_SUPERSET_OF
                || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
        }

        if (this == CONTAINS_AS_FIELD) {
            // the equivalent of a ∈ A, a ∈ B -> A ~ B
            if (other == IS_FIELD_OF) return SHARES_FIELDS;
            if (other == IS_SUBSET_OF
                || other == IS_ELEMENT_OF
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
            if (other == CONTAINS_AS_MEMBER
                || other == IS_SUPERSET_OF
                || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
        }

        if (this == IS_SUPERSET_OF) {
            if (other == CONTAINS_AS_FIELD || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
            if (other == CONTAINS_AS_MEMBER) return CONTAINS_AS_MEMBER;
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_SUBSET_OF
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
            if (other == SHARES_ELEMENTS) return SHARES_ELEMENTS;
            if (other == SHARES_FIELDS) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == IS_IN_OBJECT_GRAPH) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_SUBSET_OF) return IS_IN_OBJECT_GRAPH;
        }

        if (this == OBJECT_GRAPH_CONTAINS) {
            if (other == CONTAINS_AS_MEMBER
                || other == IS_SUPERSET_OF
                || other == CONTAINS_AS_FIELD) return OBJECT_GRAPH_CONTAINS;
            if (other == IS_IN_OBJECT_GRAPH
                || other == IS_SUBSET_OF
                || other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
            ) return OBJECT_GRAPH_OVERLAPS;

        }

        if (this == SHARES_ELEMENTS) {
            if (other == IS_SUBSET_OF) return SHARES_ELEMENTS;
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_IN_OBJECT_GRAPH)
                return IS_IN_OBJECT_GRAPH;
        }
        if (this == SHARES_FIELDS) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_SUBSET_OF
                || other == IS_IN_OBJECT_GRAPH)
                return IS_IN_OBJECT_GRAPH;
        }
        if (this == OBJECT_GRAPH_OVERLAPS) {
            if (other != CONTAINS_AS_FIELD) return OBJECT_GRAPH_OVERLAPS;
        }
        return NONE;
    }

}
