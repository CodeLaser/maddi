package org.e2immu.language.inspection.integration.java.importhelper.a;

public abstract class Serializer<X> {
    protected X element;

    @Override
    public String toString() {
        return element.toString(); // there are no type bounds here
    }
}