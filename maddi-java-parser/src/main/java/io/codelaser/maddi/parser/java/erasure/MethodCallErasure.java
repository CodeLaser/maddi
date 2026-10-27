/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.parser.java.erasure;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Objects;
import java.util.Set;

public class MethodCallErasure extends ErasureExpressionImpl {
    private final Set<ParameterizedType> returnTypes;
    private final String methodName;
    private final ParameterizedType commonParameterizedType;

    public MethodCallErasure(Runtime runtime, Source source,
                             Set<ParameterizedType> returnTypes,
                             ParameterizedType commonParameterizedType,
                             String methodName) {
        super(runtime, source);
        this.returnTypes = returnTypes;
        this.methodName = methodName;
        this.commonParameterizedType = commonParameterizedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodCallErasure that)) return false;
        return Objects.equals(returnTypes, that.returnTypes) && Objects.equals(methodName, that.methodName);
    }

    @Override
    public Expression withSource(Source source) {
        return new MethodCallErasure(runtime, source, returnTypes, commonParameterizedType, methodName);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return commonParameterizedType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnTypes, methodName);
    }

    @Override
    public Set<ParameterizedType> erasureTypes() {
        return returnTypes;
    }

    public String methodName() {
        return methodName;
    }

    @Override
    public Precedence precedence() {
        return runtime.precedenceArrayAccess();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String s = "<method call erasure of " + methodName + ", returning " + returnTypes + ">";
        return runtime.newOutputBuilder().add(runtime.newText(s));
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException();
    }
}
