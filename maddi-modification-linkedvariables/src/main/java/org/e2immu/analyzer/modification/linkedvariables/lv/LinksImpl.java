package org.e2immu.analyzer.modification.linkedvariables.lv;


import org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.util.internal.graph.op.DijkstraShortestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl.ALL;
import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.NO_MODIFICATION_INDICES;

/*
Modification area of a modified variable is copied into MODIFIED_COMPONENTS_VARIABLE/_PARAMETER, in terms of actual field references.
We use ALL_INDICES as the marker "everything" (on a -2- link) and "nothing" (on a -4- link).

The modification area will be used here to block certain edges in the linked variables graph from being added.
When R has components A, B, C, with links
    A *-2-0 R   mod area *, 0
    B *-2-1 R   mod area *, 1
    C *-2-0 R   mod area *, 2
    D  -2-  R   mod area *, 3

the only link that can be generated is 0-4-0 between A and C: A and C (may) share hidden content of type 0.
The important consequence is that a modification in A does imply a modification in R, but not in B, C or D.
 */

public record LinksImpl(Map<Indices, Link> map, Indices modificationAreaSource,
                        Indices modificationAreaTarget) implements Links {

    private static final Logger LOGGER = LoggerFactory.getLogger("graph-algorithm");

    // link info on -0-, -1-, -2- without extra link info. -4- must have hidden content links,
    // and its modification areas must be NO_MODIFICATION_INDICES
    public static final Links NO_LINKS = new LinksImpl(Map.of(), ALL_INDICES, ALL_INDICES);

    public LinksImpl {
        assert map.entrySet().stream().noneMatch(e -> {
            Indices from = e.getKey();
            assert from != null;
            assert e.getValue() != null;
            Indices to = e.getValue().to();
            assert to != null;
            return from.isAll() && to.isAll();
        });
    }

    public LinksImpl(int from, int to, boolean isHc) {
        this(Map.of(from == ALL ? ALL_INDICES : new IndicesImpl(Set.of(new IndexImpl(List.of(from)))),
                        new LinkImpl(new IndicesImpl(Set.of(new IndexImpl(List.of(to)))), false)),
                isHc ? NO_MODIFICATION_INDICES : ALL_INDICES, isHc ? NO_MODIFICATION_INDICES : ALL_INDICES);
    }

    public LinksImpl(Map<Indices, Link> map) {
        this(map, ALL_INDICES, ALL_INDICES);
    }

    @Override
    public Links ensureNoModification() {
        return new LinksImpl(map, NO_MODIFICATION_INDICES, NO_MODIFICATION_INDICES);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int hc) {
        List<String> from = new ArrayList<>();
        List<String> to = new ArrayList<>();
        for (Map.Entry<Indices, Link> e : map().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            boolean mutable = e.getValue().mutable();
            String f = indexToString(e.getKey()) + (mutable ? "M" : "");
            from.add(f);
            Indices i = e.getValue().to();
            assert i != null;
            String t = indexToString(i) + (mutable ? "M" : "");
            to.add(t);
        }
        assert from.size() == to.size();

        String modArea;
        if (modificationAreaSource().haveValue() || modificationAreaTarget().haveValue()) {
            modArea = "|" + indexToString(modificationAreaSource())
                      + "-" + indexToString(modificationAreaTarget());
        } else {
            modArea = "";
        }
        String hcStr = hc == 0 ? "?" : "" + hc;
        return String.join(",", from) + "-" + hcStr + "-" + String.join(",", to) + modArea;
    }

    private static String indexToString(Indices i) {
        if (ALL_INDICES.equals(i)) return "*";
        return i.toString();
    }

    /*
     this method, together with allowModified(), is key to the whole linking + modification process
     */
    @Override
    public DijkstraShortestPath.Accept next(Function<Integer, String> nodePrinter, int from, int to,
                                            DijkstraShortestPath.Connection current) {
        DijkstraShortestPath.Accept a = internalNext(current);
        LOGGER.debug("Current best to {} = {}; go to {} -> {} ? {}: {}",
                nodePrinter.apply(from),
                current, nodePrinter.apply(to), this,
                a.accept(), a.next());
        return a;
    }

    private DijkstraShortestPath.Accept internalNext(DijkstraShortestPath.Connection current) {
        if (current == NO_LINKS) {
            return new DijkstraShortestPath.Accept(true, this);
        }
        if (this == NO_LINKS) {
            return new DijkstraShortestPath.Accept(true, current);
        }
        //  boolean conflicted = false;
        LinksImpl currentLink = (LinksImpl) current;

        Indices maTarget = currentLink.modificationAreaTarget;
        Indices maSource = this.modificationAreaSource;
        if (maTarget.haveValue() && maSource.haveValue() && !maSource.intersectionNonEmpty(maTarget)) {
            return new DijkstraShortestPath.Accept(false, null);
        }
        Map<Indices, Link> res = new HashMap<>();
        for (Map.Entry<Indices, Link> entry : currentLink.map.entrySet()) {
            Indices middle = entry.getValue().to();
            boolean middleIsAll = middle.isAll();
            Link link = this.map.get(middle);
            if (link != null) {
                boolean fromAllToAll = entry.getKey().equals(ALL_INDICES) && link.to().equals(ALL_INDICES);
                if (!fromAllToAll) {
                    boolean mutable = !middleIsAll && entry.getValue().mutable() && link.mutable();
                    Link newLink = mutable == link.mutable() ? link : new LinkImpl(link.to(), false);
                    res.merge(entry.getKey(), newLink, Link::merge);
                }
            } else {
                Link allLink = this.map.get(ALL_INDICES);
                if (allLink != null) {
                    // start again from *
                    boolean mutable = entry.getValue().mutable() || allLink.mutable();
                    Link newLink = mutable == allLink.mutable() ? allLink : new LinkImpl(allLink.to(), true);
                    res.merge(entry.getKey(), newLink, Link::merge);

                    //  a -> r, r -> s  should give a -> r.s (*->1, 1->2 -> *->2.1)
                    // see TestWeightedGraph15B, start in a
                    if (this.modificationAreaSource.isAll() && !this.modificationAreaTarget.isAll()
                        && currentLink.modificationAreaSource.isAll() && !currentLink.modificationAreaTarget.isAll()) {
                        maTarget = currentLink.modificationAreaTarget.prepend(this.modificationAreaTarget);
                    }
                } else if (middleIsAll) {
                    // see TestWeightedGraph15B, start in s; x->* y->*
                    //  if (middleIsAll && map.values().stream().allMatch(l -> l.to().isAll())) {
                    if (!this.modificationAreaSource.isAll() && modificationAreaTarget.isAll()
                        && !currentLink.modificationAreaSource.isAll() && ((LinksImpl) current).modificationAreaTarget.isAll()) {
                        maSource = this.modificationAreaSource.prepend(currentLink.modificationAreaSource);
                        maTarget = ALL_INDICES;
                    }
                    boolean mutable = entry.getValue().mutable();
                    LinkImpl newLInk = new LinkImpl(ALL_INDICES, mutable);
                    res.merge(entry.getKey(), newLInk, Link::merge);
                    //  }
                }
            }
        }
        if (res.isEmpty()) {
            boolean allowNoConnection = !maSource.isNoModification()
                                        && !currentLink.modificationAreaTarget.isNoModification();
            return new DijkstraShortestPath.Accept(allowNoConnection, null);
        }
        LinksImpl next = new LinksImpl(res, maSource, maTarget);
        return new DijkstraShortestPath.Accept(true, next);
    }


    @Override
    public DijkstraShortestPath.Connection merge(DijkstraShortestPath.Connection connection) {
        Links other = (Links) connection;
        Map<Indices, Link> res = new HashMap<>(map);
        other.map().forEach((k, v) -> res.merge(k, v, Link::merge));
        return new LinksImpl(res, modificationAreaSource.merge(other.modificationAreaSource()),
                modificationAreaTarget.merge(other.modificationAreaTarget()));
    }

    public Links theirsToTheirs(Links links) {
        Map<Indices, Link> res = new HashMap<>();
        map.forEach((thisFrom, thisTo) -> {
            Link link = links.map().get(thisTo.to());
            if(link != null) {
                res.put(thisTo.to(), link);
            }
        });
        return new LinksImpl(res, modificationAreaTarget, links.modificationAreaTarget());
    }

    // use thisTo.to as the intermediary
    public Links mineToTheirs(Links links) {
        Map<Indices, Link> res = new HashMap<>();
        map.forEach((thisFrom, thisTo) -> {
            Link link = links.map().get(thisTo.to());
            if (link != null) {
                res.put(thisFrom, link);
            } // else: see Collections.addAll (TestLinkBasics,3); the * of Collection in this.map has no equivalent in links.map
        });
        return new LinksImpl(res, modificationAreaSource, links.modificationAreaTarget());
    }

    public Links reverse() {
        if (map.isEmpty()) return this;
        Map<Indices, Link> map = new HashMap<>();
        for (Map.Entry<Indices, Link> e : this.map.entrySet()) {
            map.put(e.getValue().to(), new LinkImpl(e.getKey(), e.getValue().mutable()));
        }
        return new LinksImpl(Map.copyOf(map), modificationAreaTarget, modificationAreaSource);
    }
}