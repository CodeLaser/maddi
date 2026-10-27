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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.TypeContext;
import org.parsers.java.ast.TypeDeclaration;

public class ParseLocalTypeDeclaration extends CommonParse {
    public ParseLocalTypeDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public LocalTypeDeclaration parse(Context context, String index, TypeDeclaration classDeclaration) {
        assert context.enclosingMethod() != null;
        Context newContext = context.newLocalTypeDeclaration();

        TypeInfo typeInfo = parsers.parseTypeDeclaration().parseLocal(newContext, context.enclosingMethod(), classDeclaration);
        newContext.resolver().resolve(false);
        context.typeContext().addToContext(typeInfo, TypeContext.CURRENT_TYPE_PRIORITY);
        return runtime.newLocalTypeDeclarationBuilder()
                .setTypeInfo(typeInfo)
                .setSource(source(index, classDeclaration))
                .build();
    }
}
