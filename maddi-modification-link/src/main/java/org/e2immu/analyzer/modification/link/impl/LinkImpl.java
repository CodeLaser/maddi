package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Iterator;
import java.util.Map;

public class LinkImpl implements Link {

    private final Map<Variable, LinkNature> map;

    public LinkImpl(Map<Variable, LinkNature> map) {
        this.map = map;
    }

    @Override
    public Iterator<Map.Entry<Variable, LinkNature>> iterator() {
        return map.entrySet().iterator();
    }
}
