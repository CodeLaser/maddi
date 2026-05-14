package org.e2immu.language.inspection.openjdk;

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
        for (Map<String, Element> map : elementStack.reversed()) {
            Element v = map.get(name);
            if (v != null) return v;
        }
        throw new UnsupportedOperationException("Cannot find element '" + name + "' on stack");
    }
}
