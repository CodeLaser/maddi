package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.Result;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;

public class AppliedFunctionalInterfaceVariable extends MarkerVariable implements LocalVariable, LinkVariable {
    public static final String LABEL = "afi";

    private final List<Result> params;
    private final ParameterInfo sourceOfFunctionalInterface;


    public AppliedFunctionalInterfaceVariable(Runtime runtime,
                                              int index,
                                              ParameterizedType parameterizedType,
                                              ParameterInfo sourceOfFunctionalInterface,
                                              List<Result> params) {
        super(LABEL + index, parameterizedType, runtime.newEmptyExpression());
        this.params = params;
        this.sourceOfFunctionalInterface = sourceOfFunctionalInterface;
    }

    @Override
    public boolean acceptForLinkedVariables() {
        return params().stream().allMatch(AppliedFunctionalInterfaceVariable::acceptForLinkedVariables);
    }

    private static boolean acceptForLinkedVariables(Result p) {
        return p.links() == null
               || p.links().primary() == null
               || p.links().isEmpty() && LinkVariable.acceptForLinkedVariables(p.links().primary())
               || p.links().stream().allMatch(l -> LinkVariable.acceptForLinkedVariables(l.to()));
    }

    public List<Result> params() {
        return params;
    }

    public ParameterInfo sourceOfFunctionalInterface() {
        return sourceOfFunctionalInterface;
    }
}
