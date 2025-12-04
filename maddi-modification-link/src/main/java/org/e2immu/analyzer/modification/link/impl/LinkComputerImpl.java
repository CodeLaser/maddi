package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import static org.e2immu.analyzer.modification.link.impl.LinkedVariablesImpl.LINKS;

/*
convention:
The LINKS are written to each method, regardless of whether they're empty or not.
They act as a marker for computation as well.
Links of a parameter are only written when non-empty.

Synchronization: ensure that each thread runs in its own instance of this class, as we're not synchronizing.
Secondary synchronization takes place in PropertyValueMapImpl.getOrCreate().
 */

public class LinkComputerImpl implements LinkComputer, LinkComputerRecursion {

    @Override
    public void doPrimaryType(TypeInfo primaryType) {
        doType(primaryType);
    }

    private void doType(TypeInfo typeInfo) {
        typeInfo.subTypes().forEach(this::doType);
        typeInfo.constructorAndMethodStream().forEach(mi -> mi.analysis().getOrCreate(LINKS, ()-> doMethod(mi)));
    }

    @Override
    public Link doField(FieldInfo fieldInfo) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void doAnonymousType(TypeInfo typeInfo) {

    }

    @Override
    public MethodLinkedVariables doMethod(MethodInfo methodInfo) {
        return null;
    }

    @Override
    public MethodLinkedVariables recurseMethod(MethodInfo methodInfo) {
        return null;
    }

    @Override
    public MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo) {
        return null;
    }
}
