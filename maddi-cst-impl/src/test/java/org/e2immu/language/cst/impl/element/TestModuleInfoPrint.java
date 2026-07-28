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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every module directive used to answer {@code print(Qualification)} with {@code null}, so the only way to produce
 * the text of a directive was to concatenate it by hand — which is what the refactoring levers did, indentation
 * included. A lever that has to add one target to {@code exports p to a, b;} should be able to hand back a
 * directive rather than a rewritten substring.
 * <p>
 * The cases below are the ones that differ in shape, not merely in name: the modifier ORDER on requires (JLS 7.7.1
 * fixes {@code static} before {@code transitive}), the presence or absence of a {@code to} clause, and a
 * {@code provides} with more than one implementation.
 * <p>
 * The expected strings carry NO space after a comma. That is not an oversight: {@code OutputBuilder.toString()}
 * renders {@code OutputElement::minimal}, and {@code SymbolEnum.COMMA} leaves the following space to the formatter
 * ({@code Formatter2Impl}, in maddi-cst-print). {@code exports p to a,b,c;} is the minimal rendering and is valid
 * Java. A caller that writes a directive INTO AN EXISTING FILE should therefore format rather than call toString,
 * or it will not match the surrounding style — and a long {@code to} list, like Elasticsearch's five-target
 * {@code exports org.elasticsearch.simdvec}, is exactly where the formatter's line splitting is wanted.
 */
public class TestModuleInfoPrint {

    private static final Runtime RUNTIME = new RuntimeImpl();

    private static ModuleInfo build(java.util.function.Consumer<ModuleInfo.Builder> directives) {
        ModuleInfo.Builder builder = RUNTIME.newModuleInfoBuilder().setName("m.example");
        directives.accept(builder);
        return builder.build();
    }

    private static String printed(ModuleInfo.Requires r) {
        return r.print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString();
    }

    @DisplayName("requires: the modifiers print in the order JLS 7.7.1 fixes, and only when present")
    @Test
    public void requiresModifiers() {
        ModuleInfo m = build(b -> {
            b.addRequires(null, List.of(), "m.plain", false, false);
            b.addRequires(null, List.of(), "m.stat", true, false);
            b.addRequires(null, List.of(), "m.trans", false, true);
            b.addRequires(null, List.of(), "m.both", true, true);
        });
        assertEquals("requires m.plain;", printed(m.requires().get(0)));
        assertEquals("requires static m.stat;", printed(m.requires().get(1)));
        assertEquals("requires transitive m.trans;", printed(m.requires().get(2)));
        assertEquals("requires static transitive m.both;", printed(m.requires().get(3)));
    }

    @DisplayName("exports: an unqualified export has no `to` clause at all, a qualified one lists every target")
    @Test
    public void exportsQualifiedAndNot() {
        ModuleInfo m = build(b -> {
            b.addExports(null, List.of(), "p.open", List.of());
            b.addExports(null, List.of(), "p.one", List.of("m.a"));
            b.addExports(null, List.of(), "p.many", List.of("m.a", "m.b", "m.c"));
        });
        assertEquals("exports p.open;",
                m.exports().get(0).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString(),
                "an empty target list is NOT `to ;`");
        assertEquals("exports p.one to m.a;",
                m.exports().get(1).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
        assertEquals("exports p.many to m.a,m.b,m.c;",
                m.exports().get(2).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
    }

    @DisplayName("opens prints like exports, with its own keyword")
    @Test
    public void opens() {
        ModuleInfo m = build(b -> {
            b.addOpens(null, List.of(), "p.reflect", List.of());
            b.addOpens(null, List.of(), "p.to", List.of("m.a", "m.b"));
        });
        assertEquals("opens p.reflect;",
                m.opens().get(0).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
        assertEquals("opens p.to to m.a,m.b;",
                m.opens().get(1).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
    }

    @DisplayName("uses, and provides with EVERY implementation rather than just the first")
    @Test
    public void usesAndProvides() {
        ModuleInfo m = build(b -> {
            b.addUses(null, List.of(), "p.Service");
            b.addProvides(null, List.of(), "p.Service", List.of("p.OnlyImpl"));
            b.addProvides(null, List.of(), "p.Other", List.of("p.A", "p.B", "p.C"));
        });
        assertEquals("uses p.Service;",
                m.uses().getFirst().print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
        assertEquals("provides p.Service with p.OnlyImpl;",
                m.provides().get(0).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString());
        assertEquals("provides p.Other with p.A,p.B,p.C;",
                m.provides().get(1).print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString(),
                "keeping only the head is the shape that stranded implementations in the ES carve");
    }
}
