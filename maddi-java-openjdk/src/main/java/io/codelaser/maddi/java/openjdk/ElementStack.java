package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.Element;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ElementStack {
    private final Deque<Map<String, Element>> elementStack = new ArrayDeque<>();

    public Map<String, Element> push() {
        Map<String, Element> map = new HashMap<>();
        elementStack.addLast(map);
        return map;
    }

    public void pop() {
        elementStack.removeLast();
    }

    public void put(String string, Element element) {
        elementStack.getLast().put(string, element);
    }

    public Element find(String name) {
        Element element = findOrNull(name);
        if (element == null) throw new UnsupportedOperationException("Cannot find element '" + name + "' on stack");
        return element;
    }

    /**
     * The same lookup, {@code null} rather than an exception when the name is absent.
     * <p>
     * ⚠ For callers that MAY legitimately miss. A caller written as
     * {@code if (find(name) instanceof TypeInfo t)} reads like it degrades gracefully and does not: {@code find}
     * throws before {@code instanceof} is ever evaluated, so the fallback branch is dead and the miss aborts the
     * whole compilation unit. That is precisely how a member class of a method-local class ended the parse with
     * {@code Cannot find element 'Sub' on stack}.
     */
    public Element findOrNull(String name) {
        assert name != null && !name.isBlank();
        for (Map<String, Element> map : elementStack.reversed()) {
            Element v = map.get(name);
            if (v != null) return v;
        }
        return null;
    }
}
