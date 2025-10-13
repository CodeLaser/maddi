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

package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

public interface ModuleInfo extends Info {
    List<Requires> requires();

    List<Exports> exports();

    List<Opens> opens();

    List<Uses> uses();

    List<Provides> provides();

    String name();

    boolean open();

    interface Builder extends Element.Builder<Builder> {
        Builder addExports(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        Builder addOpens(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        Builder addUses(Source source, List<Comment> comments, String api);

        Builder addProvides(Source source, List<Comment> comments, String api, String implementation);

        Builder addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive);

        Builder setCompilationUnit(CompilationUnit compilationUnit);

        Builder setName(String name);

        Builder setOpen(boolean openModule);

        ModuleInfo build();
    }

    interface Requires extends Element {

        boolean isTransitive();

        boolean isStatic();

        String name();
    }

    interface Exports extends Element {
        String packageName();

        String toPackageNameOrNull();
    }

    interface Opens extends Element {
        String packageName();

        String toPackageNameOrNull();
    }

    interface Uses extends Element {
        String api();

        // set once
        void setApiResolved(TypeInfo typeInfo);

        TypeInfo apiResolved();
    }

    interface Provides extends Element {
        String api();

        // set once
        void setApiResolved(TypeInfo typeInfo);

        TypeInfo apiResolved();

        String implementation();

        // set once
        void setImplementationResolved(TypeInfo typeInfo);

        TypeInfo implementationResolved();
    }
}
