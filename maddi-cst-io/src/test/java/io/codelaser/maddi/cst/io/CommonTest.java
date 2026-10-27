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

package io.codelaser.maddi.cst.io;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyProviderImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.parsers.json.JSONParser;
import org.parsers.json.Node;

public abstract class CommonTest {

    final Runtime runtime = new RuntimeImpl() {
        @Override
        public TypeInfo getFullyQualified(String name, boolean complain) {
            if ("int".equals(name)) return intTypeInfo();
            if ("java.lang.String".equals(name)) return stringTypeInfo();
            if ("a.b.C".equals(name)) return typeInfo;
            throw new UnsupportedOperationException("Unknown type '" + name + "'");
        }
    };

    final Codec.DecoderProvider decoderProvider = ValueImpl::decoder;
    final Codec codec = new CodecImpl(runtime, PropertyProviderImpl::get,
            decoderProvider, fqn -> runtime.getFullyQualified(fqn, true), null);
    final Codec.Context context = new CodecImpl.ContextImpl();
    final CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b").build();
    final TypeInfo typeInfo = runtime.newTypeInfo(cu, "C");
    final FieldInfo f = runtime.newFieldInfo("f", false, runtime.intParameterizedType(), typeInfo);
    final TypeInfo sub = runtime.newTypeInfo(typeInfo, "Sub");
    final MethodInfo max = runtime.newMethod(sub, "max", runtime.methodTypeAbstractMethod());
    final ParameterInfo max0 = max.builder().addParameter("p0", runtime.intParameterizedType());
    final ParameterInfo max1 = max.builder().addParameter("p1", runtime.intParameterizedType());

    CommonTest() {
        typeInfo.builder().addField(f);
        typeInfo.builder().addSubType(sub);
        sub.builder().addMethod(max);
        max0.builder().commit();
        max1.builder().commit();
    }

    static CodecImpl.D makeD(String intArray) {
        JSONParser parser = new JSONParser(intArray);
        parser.Root();
        Node root = parser.rootNode();
        return new CodecImpl.D(root.getFirstChild());
    }
}
