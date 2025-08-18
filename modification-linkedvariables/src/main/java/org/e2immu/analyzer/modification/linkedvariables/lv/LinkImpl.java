package org.e2immu.analyzer.modification.linkedvariables.lv;

import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.Link;

import java.util.Map;


public record LinkImpl(Indices to, boolean mutable) implements Link {
    public Link correctTo(Map<Indices, Indices> correctionMap) {
        return new LinkImpl(correctionMap.getOrDefault(to, to), mutable);
    }

    @Override
    public Link merge(Link l2) {
        return new LinkImpl(to.merge(l2.to()), mutable || l2.mutable());
    }

    @Override
    public Link prefixTheirs(int index) {
        return new LinkImpl(to.prefix(index), mutable);
    }

    @Override
    public String toString() {
        String toStr = to == null ? "NULL" : to.isAll() ? "*" : to.toString();
        return "LinkImpl[" + toStr + ",mutable=" + mutable + "]";
    }
}
