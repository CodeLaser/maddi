/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
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
