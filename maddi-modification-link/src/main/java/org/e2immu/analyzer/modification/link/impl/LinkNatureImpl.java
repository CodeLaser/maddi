package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.info.MethodInfo;

import java.util.*;

//https://unicodemap.com/range/47/Mathematical_Operators/

public class LinkNatureImpl implements LinkNature {
    // rank is from least interesting to most interesting


    public static final LinkNature NONE = new LinkNatureImpl("X", 0);
    public static final LinkNature EMPTY = new LinkNatureImpl("∅", 1);

    public static final LinkNature IS_FIELD_OF = new LinkNatureImpl("≺", 2);
    public static final LinkNature CONTAINS_AS_FIELD = new LinkNatureImpl("≻", 3);
    public static final LinkNature SHARES_FIELDS = new LinkNatureImpl("≈", 4);

    public static final LinkNature OBJECT_GRAPH_OVERLAPS = new LinkNatureImpl("∩", 5);
    public static final LinkNature IS_IN_OBJECT_GRAPH = new LinkNatureImpl("≤", 6);
    public static final LinkNature OBJECT_GRAPH_CONTAINS = new LinkNatureImpl("≥", 7);

    public static final LinkNature SHARES_ELEMENTS = new LinkNatureImpl("~", 8);

    public static final LinkNature IS_SUBSET_OF = new LinkNatureImpl("⊆", 9);
    public static final LinkNature IS_SUPERSET_OF = new LinkNatureImpl("⊇", 10);

    public static final LinkNature IS_ELEMENT_OF = new LinkNatureImpl("∈", 11);
    public static final LinkNature CONTAINS_AS_MEMBER = new LinkNatureImpl("∋", 12);

    public static final LinkNature IS_DECORATED_WITH = new LinkNatureImpl("↗", 13);
    public static final LinkNature CONTAINS_DECORATION = new LinkNatureImpl("↖", 14);

    private static final String IDENTICAL_TO_PASS_SYMBOL = "☷"; // tri-gram for earth, 0x2637
    private static final int IDENTICAL_TO_RANK = 15;

    private static final LinkNature IS_IDENTICAL_TO = new LinkNatureImpl("≡", IDENTICAL_TO_RANK);

    // java a=b implies a ← b
    public static final LinkNature IS_ASSIGNED_FROM = new LinkNatureImpl("←", 16);
    public static final LinkNature IS_ASSIGNED_TO = new LinkNatureImpl("→", 17);
    private static final int N = IS_ASSIGNED_TO.rank() + 1;


    private static final LinkNature LN_THIS = new LinkNatureImpl("this", -1);
    private static final LinkNature LN_OTHER = new LinkNatureImpl("other", -2);
    private static final LinkNature LN_INTERSECT = new LinkNatureImpl("other", -3);


    // ← → are more important than ≡
    // they follow the flow of assignments

    private final String symbol;
    private final int rank;
    private final Set<MethodInfo> pass;

    private LinkNatureImpl(String symbol, int rank) {
        this(symbol, rank, Set.of());
    }

    private LinkNatureImpl(String symbol, int rank, Set<MethodInfo> pass) {
        this.symbol = symbol;
        this.rank = rank;
        this.pass = pass;
    }

    private static final Map<String, LinkNature> stringMap = new HashMap<>();

    private static final LinkNature[] linkNatures = {
            NONE, EMPTY, IS_FIELD_OF, CONTAINS_AS_FIELD, SHARES_FIELDS, OBJECT_GRAPH_OVERLAPS, IS_IN_OBJECT_GRAPH,
            OBJECT_GRAPH_CONTAINS, SHARES_ELEMENTS, IS_SUBSET_OF, IS_SUPERSET_OF, IS_ELEMENT_OF,
            CONTAINS_AS_MEMBER, IS_DECORATED_WITH, CONTAINS_DECORATION, IS_ASSIGNED_FROM,
            IS_ASSIGNED_TO, IS_IDENTICAL_TO};

    static {
        for (LinkNature ln : linkNatures) {
            stringMap.put(ln.toString(), ln);
        }
    }

    public static LinkNature decode(String s) {
        return Objects.requireNonNull(stringMap.get(s), "Unknown symbol " + s);
    }

    private static final LinkNature[][] combinations = new LinkNature[N][N];
    private static final LinkNature[] reverse = new LinkNature[N];
    private static final int[] hashCode = new int[N];

    static {
        for (LinkNature ln1 : linkNatures) {
            LinkNature r = ((LinkNatureImpl) ln1).computeReverse();
            int r1 = ln1.rank();
            if (r != ln1) reverse[r1] = r;
            for (LinkNature ln2 : linkNatures) {
                LinkNature c = ((LinkNatureImpl) ln1).computeCombine(ln2);
                LinkNature v = c == ln1 ? LN_THIS : c == ln2 ? LN_OTHER : c;
                combinations[r1][ln2.rank()] = v;
            }
            hashCode[r1] = Objects.hash(((LinkNatureImpl) ln1).symbol, r1);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LinkNatureImpl that)) return false;
        return rank == that.rank && Objects.equals(symbol, that.symbol) && Objects.equals(pass, that.pass);
    }

    @Override
    public int hashCode() {
        if (pass == null) return hashCode[rank];
        return Objects.hash(symbol, rank, pass);
    }

    public static LinkNature makeIdenticalTo(Collection<MethodInfo> exceptionsToPass) {
        if (exceptionsToPass == null || exceptionsToPass.isEmpty()) return IS_IDENTICAL_TO;
        return new LinkNatureImpl(IDENTICAL_TO_PASS_SYMBOL, IDENTICAL_TO_RANK, Set.copyOf(exceptionsToPass));
    }

    @Override
    public Set<MethodInfo> pass() {
        return pass;
    }

    @Override
    public boolean multiplySymbols() {
        return this == IS_ELEMENT_OF || this == CONTAINS_AS_MEMBER;
    }

    @Override
    public boolean isIdenticalToOrAssignedFromTo() {
        return isIdenticalTo() || this == IS_ASSIGNED_FROM || this == IS_ASSIGNED_TO;
    }

    @Override
    public boolean isIdenticalTo() {
        return IDENTICAL_TO_RANK == rank;
    }

    @Override
    public boolean known() {
        return rank >= IS_FIELD_OF.rank();
    }

    @Override
    public boolean isDecoration() {
        return this == IS_DECORATED_WITH || this == CONTAINS_DECORATION;
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
        return rank >= SHARES_FIELDS.rank();
    }

    @Override
    public LinkNature replaceSubsetSuperset() {
        if (this == IS_SUBSET_OF || this == IS_SUPERSET_OF) return SHARES_ELEMENTS;
        return this;
    }

    @Override
    public boolean isAssignedFrom() {
        return this == IS_ASSIGNED_FROM;
    }

    @Override
    public LinkNature reverse() {
        LinkNature r = reverse[rank];
        return r == null ? this : r;
    }

    private LinkNature computeReverse() {
        if (this == IS_ELEMENT_OF) return CONTAINS_AS_MEMBER;
        if (this == CONTAINS_AS_MEMBER) return IS_ELEMENT_OF;
        if (this == IS_FIELD_OF) return CONTAINS_AS_FIELD;
        if (this == CONTAINS_AS_FIELD) return IS_FIELD_OF;
        if (this == IS_SUBSET_OF) return IS_SUPERSET_OF;
        if (this == IS_SUPERSET_OF) return IS_SUBSET_OF;
        if (this == OBJECT_GRAPH_CONTAINS) return IS_IN_OBJECT_GRAPH;
        if (this == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_CONTAINS;
        if (this == IS_ASSIGNED_FROM) return IS_ASSIGNED_TO;
        if (this == IS_ASSIGNED_TO) return IS_ASSIGNED_FROM;
        if (this == IS_DECORATED_WITH) return CONTAINS_DECORATION;
        if (this == CONTAINS_DECORATION) return IS_DECORATED_WITH;
        return this;
    }

    @Override
    public String toString() {
        return symbol;
    }

    private LinkNature intersection(LinkNature other) {
        Set<MethodInfo> intersection = new HashSet<>(pass);
        intersection.retainAll(other.pass());
        return makeIdenticalTo(Set.copyOf(intersection));
    }

    @Override
    public LinkNature combine(LinkNature other) {
        if (this == other) return this;
        if (this == EMPTY) return other;
        if (other == EMPTY) return this;
        if (this == NONE || other == NONE) return NONE;
        if (other == IS_IDENTICAL_TO) return this;
        if (this == IS_IDENTICAL_TO) return other;
        if (other.isIdenticalTo()) {
            if (this.isIdenticalTo()) {
                return intersection(other);
            }
            return this;
        }
        LinkNature c = combinations[rank][other.rank()];
        if (c == LN_THIS) return this;
        if (c == LN_OTHER) return other;
        return c;
    }

    private LinkNature computeCombine(LinkNature other) {
        if (this == other) return this;
        if (this == EMPTY) return other;
        if (other == EMPTY) return this;
        if (this == NONE || other == NONE) return NONE;
        if (other == IS_IDENTICAL_TO) return this;
        if (this == IS_IDENTICAL_TO) return other;

        if (other == IS_ASSIGNED_TO) {
            if (this == IS_ASSIGNED_FROM) return makeIdenticalTo(null);
            return this; // a R b → c implies a R c;
        }
        if (this == IS_ASSIGNED_FROM) {
            //assert other != IS_ASSIGNED_TO;
            return other; // a ← b R c implies a R c
        }

        // no other interactions with IS_DECORATED_WITH
        if (this == IS_DECORATED_WITH ||
            other == IS_DECORATED_WITH ||
            this == CONTAINS_DECORATION ||
            other == CONTAINS_DECORATION) return NONE;

        if (other == IS_ASSIGNED_FROM && this != IS_ASSIGNED_TO
            && this != CONTAINS_AS_FIELD) {
            return this; //a R b ← c implies a R c, but not for →
        }
        if (this == IS_ASSIGNED_TO && other != IS_ASSIGNED_FROM
            && other != IS_FIELD_OF) {
            return other; // a → b R c implies a R c, but not for ←
        }

        if (this == IS_ELEMENT_OF) {
            if (other == IS_SUBSET_OF) return IS_ELEMENT_OF;
            if (other == IS_IN_OBJECT_GRAPH || other == IS_FIELD_OF) return IS_IN_OBJECT_GRAPH;
            // (*)
            if (other == OBJECT_GRAPH_OVERLAPS) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == IS_FIELD_OF) {
            if (other == IS_ELEMENT_OF
                || other == IS_SUBSET_OF
                || other == IS_IN_OBJECT_GRAPH) return IS_IN_OBJECT_GRAPH;
        }

        if (this == IS_SUBSET_OF) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_IN_OBJECT_GRAPH) return IS_IN_OBJECT_GRAPH;
            if (other == SHARES_ELEMENTS || other == SHARES_FIELDS) return OBJECT_GRAPH_OVERLAPS;

            // (*)
            if (other == OBJECT_GRAPH_CONTAINS || other == OBJECT_GRAPH_OVERLAPS) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == CONTAINS_AS_MEMBER) {
            // A ∋ a, a ∈ B -> A ~ B
            if (other == IS_ELEMENT_OF) return SHARES_ELEMENTS;
            if (other == IS_FIELD_OF
                || other == IS_SUBSET_OF
                || other == SHARES_ELEMENTS
                || other == SHARES_FIELDS
                || other == IS_IN_OBJECT_GRAPH
                || other == OBJECT_GRAPH_OVERLAPS) return OBJECT_GRAPH_OVERLAPS;
            if (other == CONTAINS_AS_FIELD
                || other == IS_SUPERSET_OF
                || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
        }

        if (this == CONTAINS_AS_FIELD) {
            // the equivalent of a ∈ A, a ∈ B -> A ~ B
            if (other == IS_FIELD_OF) return SHARES_FIELDS;
            if (other == IS_SUBSET_OF
                || other == IS_ELEMENT_OF
                || other == SHARES_ELEMENTS
                || other == SHARES_FIELDS
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
            if (other == CONTAINS_AS_MEMBER
                || other == IS_SUPERSET_OF
                || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
        }

        if (this == IS_SUPERSET_OF) {
            if (other == IS_SUBSET_OF || other == SHARES_ELEMENTS) return SHARES_ELEMENTS;
            if (other == CONTAINS_AS_FIELD || other == OBJECT_GRAPH_CONTAINS) return OBJECT_GRAPH_CONTAINS;
            if (other == CONTAINS_AS_MEMBER) return CONTAINS_AS_MEMBER;
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == SHARES_FIELDS
                || other == OBJECT_GRAPH_OVERLAPS // (*)
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == IS_IN_OBJECT_GRAPH) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_SUBSET_OF) return IS_IN_OBJECT_GRAPH;
            if (other == IS_SUPERSET_OF) return OBJECT_GRAPH_OVERLAPS; // (*)
        }

        if (this == OBJECT_GRAPH_CONTAINS) {
            if (other == CONTAINS_AS_MEMBER
                || other == IS_SUPERSET_OF
                || other == CONTAINS_AS_FIELD) return OBJECT_GRAPH_CONTAINS;
            if (other == IS_IN_OBJECT_GRAPH
                || other == SHARES_ELEMENTS
                || other == SHARES_FIELDS
                || other == IS_SUBSET_OF
                || other == IS_ELEMENT_OF
                || other == OBJECT_GRAPH_OVERLAPS // (*)
                || other == IS_FIELD_OF) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == SHARES_ELEMENTS) {
            if (other == IS_SUBSET_OF) return SHARES_ELEMENTS;
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_IN_OBJECT_GRAPH
                || other == OBJECT_GRAPH_OVERLAPS
                || other == IS_SUPERSET_OF) return OBJECT_GRAPH_OVERLAPS;
        }

        if (this == SHARES_FIELDS) {
            if (other == IS_ELEMENT_OF
                || other == IS_FIELD_OF
                || other == IS_SUBSET_OF
                || other == IS_SUPERSET_OF
                || other == IS_IN_OBJECT_GRAPH)
                return OBJECT_GRAPH_OVERLAPS;
        }

        /* (*)
           "object graph overlaps" is one less accurate than "~", shares elements.
           The latter degrades into the former when a ⊆ and ⊇ reversal is encountered (e.g. TestConstructor,1, method A)
           or when the hidden content is wrapped in some record (TestMap,2, TestStream,testWrap2)
           In this context, ⊆ ⊇ ∈ ∋ is seen as spanning all internal structures, while ∩ either selects or wraps one.
           Therefore, we can maintain that the outcome is again ∩.
         */
        if (this == OBJECT_GRAPH_OVERLAPS) {
            if (other == IS_SUBSET_OF
                || other == IS_SUPERSET_OF
                || other == CONTAINS_AS_MEMBER
                || other == IS_ELEMENT_OF
                || other == SHARES_ELEMENTS
                || other == IS_IN_OBJECT_GRAPH) return OBJECT_GRAPH_OVERLAPS;
        }
        return NONE;
    }

    @Override
    public List<LinkNature> redundantFromUp() {
        if (this == IS_ELEMENT_OF || this == IS_SUBSET_OF || this == IS_IN_OBJECT_GRAPH) {
            // a.b ∈⊆≤ c => a ≤ c
            return List.of(IS_IN_OBJECT_GRAPH, OBJECT_GRAPH_OVERLAPS);
        }
        if (this == CONTAINS_AS_MEMBER || this == IS_SUPERSET_OF || this == OBJECT_GRAPH_CONTAINS) {
            // a.b ∋⊇≥ c => a ≥ c
            return List.of(OBJECT_GRAPH_CONTAINS, OBJECT_GRAPH_OVERLAPS);
        }
        if (isIdenticalTo()
            || this == IS_ASSIGNED_FROM || this == IS_ASSIGNED_TO
            || this == SHARES_ELEMENTS || this == SHARES_FIELDS) {
            return List.of(OBJECT_GRAPH_OVERLAPS, IS_FIELD_OF, CONTAINS_AS_FIELD);
        }
        return List.of(OBJECT_GRAPH_OVERLAPS);
    }

    @Override
    public List<LinkNature> redundantToUp() {
        if (this == IS_ELEMENT_OF || this == IS_SUBSET_OF || this == IS_IN_OBJECT_GRAPH) {
            // a ∈⊆≤~ b.c => a ≤ c
            return List.of(IS_IN_OBJECT_GRAPH, OBJECT_GRAPH_OVERLAPS);
        }
        if (this == CONTAINS_AS_MEMBER || this == IS_SUPERSET_OF || this == OBJECT_GRAPH_CONTAINS) {
            // a ∋⊇≥ b.c => a ≥ c
            return List.of(OBJECT_GRAPH_CONTAINS, OBJECT_GRAPH_OVERLAPS);
        }
        if (isIdenticalTo() || this == IS_ASSIGNED_FROM || this == IS_ASSIGNED_TO
            || this == SHARES_ELEMENTS || this == SHARES_FIELDS) {
            return List.of(OBJECT_GRAPH_OVERLAPS, CONTAINS_AS_FIELD, IS_FIELD_OF);
        }
        return List.of(OBJECT_GRAPH_OVERLAPS);
    }


    @Override
    public List<LinkNature> redundantUp() {
        if (isIdenticalTo() || this == IS_ASSIGNED_FROM || this == IS_ASSIGNED_TO) {
            return List.of(SHARES_FIELDS, OBJECT_GRAPH_OVERLAPS);
        }
        return List.of(OBJECT_GRAPH_OVERLAPS);
    }
}
