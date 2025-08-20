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

    interface Builder extends Element.Builder<Builder> {
        Builder addExports(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        Builder addOpens(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        Builder addUses(Source source, List<Comment> comments, String api);

        Builder addProvides(Source source, List<Comment> comments, String api, String implementation);

        Builder addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive);

        Builder setCompilationUnit(CompilationUnit compilationUnit);

        Builder setName(String name);

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
