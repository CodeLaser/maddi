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

/**
 * Represents the contents of a Java 9+ {@code module-info.java} file.
 * <p>
 * A module declaration has a name ({@link #name()}), an optional {@code open} modifier
 * ({@link #open()}), and a set of directives: {@code requires}, {@code exports},
 * {@code opens}, {@code uses}, and {@code provides}.
 */
public interface ModuleInfo extends Info {

    /** Returns the {@code requires} directives of this module, in declaration order. */
    List<Requires> requires();

    /** Returns the {@code exports} directives of this module, in declaration order. */
    List<Exports> exports();

    /** Returns the {@code opens} directives of this module, in declaration order. */
    List<Opens> opens();

    /** Returns the {@code uses} directives of this module, in declaration order. */
    List<Uses> uses();

    /** Returns the {@code provides} directives of this module, in declaration order. */
    List<Provides> provides();

    /** Returns the module name as declared in the {@code module} statement. */
    String name();

    /** Returns {@code true} if this is an {@code open module} declaration. */
    boolean open();

    /** Builder for constructing a {@link ModuleInfo} during parsing. */
    interface Builder extends Element.Builder<Builder> {

        /**
         * Adds an {@code exports <packageName> [to <toPackageNameOrNull>]} directive.
         *
         * @param toPackageNameOrNull the target module, or {@code null} for an unconditional export
         */
        Builder addExports(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        /**
         * Adds an {@code opens <packageName> [to <toPackageNameOrNull>]} directive.
         *
         * @param toPackageNameOrNull the target module, or {@code null} for unconditional opening
         */
        Builder addOpens(Source source, List<Comment> comments, String packageName, String toPackageNameOrNull);

        /** Adds a {@code uses <api>} directive, declaring a service dependency. */
        Builder addUses(Source source, List<Comment> comments, String api);

        /** Adds a {@code provides <api> with <implementation>} directive. */
        Builder addProvides(Source source, List<Comment> comments, String api, String implementation);

        /**
         * Adds a {@code requires [static] [transitive] <name>} directive.
         *
         * @param isStatic    {@code true} if the {@code static} modifier is present
         * @param isTransitive {@code true} if the {@code transitive} modifier is present
         */
        Builder addRequires(Source source, List<Comment> comments, String name, boolean isStatic, boolean isTransitive);

        /** Sets the compilation unit that contains this module declaration. */
        Builder setCompilationUnit(CompilationUnit compilationUnit);

        /** Sets the module name. */
        Builder setName(String name);

        /** Sets whether this is an {@code open module}. */
        Builder setOpen(boolean openModule);

        /** Finalises and returns the built {@link ModuleInfo}. */
        ModuleInfo build();
    }

    /**
     * A {@code requires [static] [transitive] <name>} directive,
     * declaring a dependency on another module.
     */
    interface Requires extends Element {

        /** Returns {@code true} if the {@code transitive} modifier is present. */
        boolean isTransitive();

        /** Returns {@code true} if the {@code static} modifier is present (compile-time-only dependency). */
        boolean isStatic();

        /** Returns the name of the required module. */
        String name();
    }

    /**
     * An {@code exports <packageName> [to <module>]} directive,
     * making a package's public API visible to other modules.
     */
    interface Exports extends Element {

        /** Returns the name of the exported package. */
        String packageName();

        /**
         * Returns the name of the module to which the package is exported,
         * or {@code null} for an unconditional (all-modules) export.
         */
        String toPackageNameOrNull();
    }

    /**
     * An {@code opens <packageName> [to <module>]} directive,
     * granting reflective access to a package's types.
     */
    interface Opens extends Element {

        /** Returns the name of the opened package. */
        String packageName();

        /**
         * Returns the name of the module to which the package is opened,
         * or {@code null} for unconditional (all-modules) opening.
         */
        String toPackageNameOrNull();
    }

    /**
     * A {@code uses <api>} directive, declaring that this module consumes a service
     * via {@link java.util.ServiceLoader}.
     */
    interface Uses extends Element {

        /** Returns the fully qualified name of the service interface or abstract class. */
        String api();

        /**
         * Resolves and records the {@link TypeInfo} for the service interface.
         * Called once during type resolution; subsequent calls throw.
         */
        void setApiResolved(TypeInfo typeInfo);

        /** Returns the resolved {@link TypeInfo} for the service interface, or {@code null} if not yet resolved. */
        TypeInfo apiResolved();
    }

    /**
     * A {@code provides <api> with <implementation>} directive,
     * registering a service implementation for use via {@link java.util.ServiceLoader}.
     */
    interface Provides extends Element {

        /** Returns the fully qualified name of the service interface or abstract class. */
        String api();

        /**
         * Resolves and records the {@link TypeInfo} for the service interface.
         * Called once during type resolution; subsequent calls throw.
         */
        void setApiResolved(TypeInfo typeInfo);

        /** Returns the resolved {@link TypeInfo} for the service interface, or {@code null} if not yet resolved. */
        TypeInfo apiResolved();

        /** Returns the fully qualified name of the service implementation class. */
        String implementation();

        /**
         * Resolves and records the {@link TypeInfo} for the implementation class.
         * Called once during type resolution; subsequent calls throw.
         */
        void setImplementationResolved(TypeInfo typeInfo);

        /** Returns the resolved {@link TypeInfo} for the implementation class, or {@code null} if not yet resolved. */
        TypeInfo implementationResolved();
    }
}
