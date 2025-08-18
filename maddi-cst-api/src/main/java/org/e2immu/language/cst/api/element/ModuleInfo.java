package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

public interface ModuleInfo extends Element {
    List<Requires> requires();

    List<Exports> exports();

    List<Opens> opens();

    List<Uses> uses();

    List<Provides> provides();

    String name();

    interface Builder extends Element.Builder<Builder> {
        void addExports(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        void addOpens(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        void addUses(Source source, List<Comment> comments, String api);

        void addProvides(Source source, List<Comment> comments, String api, String implementation);

        void addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive);

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
