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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.TypeContext;
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
