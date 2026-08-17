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

package io.codelaser.maddi.cst.api.element;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Simple name → fully qualified name, for every single-type import of this descriptor.
     * <p>
     * ⭐ <b>A MODULE DECLARATION HAS NO PACKAGE, so a short name in a directive can only come from an import</b>
     * (JLS 7.7) — and the imports are right there in the same compilation unit. This is the whole resolution
     * rule, and it lives here so that everything answering <i>"which type does this directive name?"</i> answers
     * it the same way.
     * <p>
     * ⚠ Neither a static import nor an on-demand ({@code .*}) import can name a service type, so both are
     * skipped. Empty when the descriptor has no written compilation unit — a short name is then unresolvable,
     * and {@link #resolveDirectiveName} says so by returning it unchanged rather than guessing a package.
     */
    default Map<String, String> importedShortNames() {
        if (compilationUnit() == null) return Map.of();
        Map<String, String> map = new HashMap<>();
        for (ImportStatement is : compilationUnit().importStatements()) {
            if (is.isStatic() || is.isStar()) continue;
            String fqn = is.importString();
            int dot = fqn.lastIndexOf('.');
            if (dot > 0) map.put(fqn.substring(dot + 1), fqn);
        }
        return map;
    }

    /**
     * The fully qualified name a directive names, whatever it is <em>written</em> as. A name that is already
     * qualified is returned unchanged; a short name is looked up among {@link #importedShortNames()}; anything
     * else is returned as written.
     * <p>
     * ⛔ <b>THE WRITTEN FORM IS NOT A MISTAKE TO BE NORMALISED AWAY.</b> {@code Uses#api()} and
     * {@code Provides#api()} deliberately return the source text, because a refactoring that retargets a
     * directive must be able to rewrite it as its author wrote it. Resolution is therefore a separate question,
     * asked here, rather than something the parse decides on everyone's behalf.
     */
    default String resolveDirectiveName(String written) {
        return resolveDirectiveName(written, importedShortNames());
    }

    /**
     * {@link #resolveDirectiveName(String)} against an import map the caller already built — the map is per
     * descriptor and a caller typically resolves every directive of one descriptor in a row.
     * <p>
     * ⚠ It exists so that the convenience and the loop cannot drift: <b>this is the only place the rule is
     * written</b>, and the instance method delegates to it. A second copy "for performance" is how two readers
     * of one question start disagreeing.
     */
    static String resolveDirectiveName(String written, Map<String, String> importedShortNames) {
        if (written == null || written.indexOf('.') >= 0) return written;
        return importedShortNames.getOrDefault(written, written);
    }

    /** Builder for constructing a {@link ModuleInfo} during parsing. */
    interface Builder extends Element.Builder<Builder> {

        /**
         * Adds an {@code exports <packageName> [to <toModules>]} directive.
         *
         * @param toModules the target modules of a qualified export, or an empty list for an unconditional export
         */
        Builder addExports(Source source, List<Comment> comments, String packageName, List<String> toModules);

        /**
         * Adds an {@code opens <packageName> [to <toModules>]} directive.
         *
         * @param toModules the target modules of a qualified opens, or an empty list for unconditional opening
         */
        Builder addOpens(Source source, List<Comment> comments, String packageName, List<String> toModules);

        /** Adds a {@code uses <api>} directive, declaring a service dependency. */
        Builder addUses(Source source, List<Comment> comments, String api);

        /** Adds a {@code provides <api> with <implementations>} directive (one or more service implementations). */
        Builder addProvides(Source source, List<Comment> comments, String api, List<String> implementations);

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
         * Returns the modules to which the package is exported (a qualified {@code exports p to a, b, c}),
         * or an empty list for an unconditional (all-modules) export.
         */
        List<String> toModulesOrEmpty();

        /**
         * Returns the first target module of a qualified export, or {@code null} for an unconditional export.
         * Prefer {@link #toModulesOrEmpty()}; this convenience keeps the first target only.
         */
        default String toPackageNameOrNull() {
            return toModulesOrEmpty().isEmpty() ? null : toModulesOrEmpty().getFirst();
        }

        /**
         * Returns a copy of this directive exporting to {@code toModules} instead. Source and comments are kept,
         * so the copy still knows where the original was written and can be printed back over it — which is what
         * a refactoring that adds one target to {@code exports p to a, b;} needs.
         */
        Exports withToModules(List<String> toModules);
    }

    /**
     * An {@code opens <packageName> [to <module>]} directive,
     * granting reflective access to a package's types.
     */
    interface Opens extends Element {

        /** Returns the name of the opened package. */
        String packageName();

        /**
         * Returns the modules to which the package is opened (a qualified {@code opens p to a, b, c}),
         * or an empty list for unconditional (all-modules) opening.
         */
        List<String> toModulesOrEmpty();

        /**
         * Returns the first target module of a qualified opens, or {@code null} for unconditional opening.
         * Prefer {@link #toModulesOrEmpty()}; this convenience keeps the first target only.
         */
        default String toPackageNameOrNull() {
            return toModulesOrEmpty().isEmpty() ? null : toModulesOrEmpty().getFirst();
        }
    }

    /**
     * A {@code uses <api>} directive, declaring that this module consumes a service
     * via {@link java.util.ServiceLoader}.
     */
    interface Uses extends Element {

        /**
         * Returns the service interface <b>exactly as written</b> in the directive — which may be a simple name
         * resolved through one of the descriptor's imports, and on Elasticsearch is, 2 times in 19.
         * <p>
         * ⚠ This javadoc used to promise <i>"the fully qualified name"</i>, and the parse never did that: it
         * stores {@code apiNode.getSource()}. A DOCUMENTED BEHAVIOUR IS A CLAIM LIKE ANY OTHER. The written form
         * is kept on purpose — a refactoring rewrites the directive as its author wrote it — so ask
         * {@link ModuleInfo#resolveDirectiveName} for the qualified name, or {@link #apiResolved()} for the type.
         */
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

        /**
         * Returns the service interface <b>exactly as written</b>. See {@link Uses#api()}: the parse stores the
         * source text, and on Elasticsearch 17 of 70 {@code provides} directives name their api short.
         */
        String api();

        /**
         * Resolves and records the {@link TypeInfo} for the service interface.
         * Called once during type resolution; subsequent calls throw.
         */
        void setApiResolved(TypeInfo typeInfo);

        /** Returns the resolved {@link TypeInfo} for the service interface, or {@code null} if not yet resolved. */
        TypeInfo apiResolved();

        /**
         * Returns the service implementation classes <b>exactly as written</b>, in declaration order.
         * A {@code provides <api> with A, B, C} directive lists more than one; a plain {@code provides <api> with A}
         * lists exactly one. ⚠ As {@link Uses#api()}, these are source text and not necessarily qualified — 18 of
         * Elasticsearch's 126 implementation names are short.
         */
        List<String> implementations();

        /**
         * Records the resolved {@link TypeInfo}s of the implementation classes, in declaration order.
         * Called once during type resolution, after every implementation has been looked up; subsequent calls
         * throw — commit-once, like {@link #setApiResolved(TypeInfo)}, so the directive stays eventually final.
         */
        void setImplementationsResolved(List<TypeInfo> typeInfos);

        /** Returns the resolved {@link TypeInfo}s for the implementation classes (may be shorter than {@link
         *  #implementations()} if some did not resolve). */
        List<TypeInfo> implementationsResolved();
    }
}
