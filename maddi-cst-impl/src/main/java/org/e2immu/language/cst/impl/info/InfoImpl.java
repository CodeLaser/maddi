package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.variable.DescendModeEnum;

import java.util.stream.Stream;

public abstract class InfoImpl implements Info {

    private final PropertyValueMap propertyValueMap = new PropertyValueMapImpl();

    @Override
    public Stream<Variable> variableStreamDescend() {
        return variables(DescendModeEnum.YES);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return variables(DescendModeEnum.NO);
    }

    @Override
    public PropertyValueMap analysis() {
        return propertyValueMap;
    }

    @Override
    public boolean hasBeenAnalyzed() {
        // TODO should add computational analyzer too, later
        return analysis().getOrDefault(PropertyImpl.DEFAULTS_ANALYZER, ValueImpl.BoolImpl.FALSE).isTrue();
    }
}
