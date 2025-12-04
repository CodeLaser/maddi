package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodLinkedVariablesImpl extends LinkedVariablesImpl implements MethodLinkedVariables {
    private final Link ofReturnValue;
    private final List<Link> ofParameters;

    private MethodLinkedVariablesImpl(Map<Variable, Link> map, Link ofReturnValue, List<Link> ofParameters) {
        super(map);
        this.ofParameters = ofParameters;
        this.ofReturnValue = ofReturnValue;
    }

    public MethodLinkedVariables create(MethodInfo methodInfo, Link ofReturnValue, List<Link> ofParameters) {
        Map<Variable, Link> map = new HashMap<>();
        map.put(new ReturnVariableImpl(methodInfo), ofReturnValue);
        for (ParameterInfo pi : methodInfo.parameters()) {
            map.put(pi, ofParameters.get(pi.index()));
        }
        return new MethodLinkedVariablesImpl(Map.copyOf(map), ofReturnValue, ofParameters);
    }

    @Override
    public Link ofReturnValue() {
        return ofReturnValue;
    }

    @Override
    public List<Link> ofParameters() {
        return ofParameters;
    }
}
