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

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The module declaration through the real formatter. The directive printers themselves are covered by
 * {@code TestModuleInfoPrint} in maddi-cst-impl, which cannot reach the formatter (cst-print depends on cst-impl,
 * not the other way round), and the shape below is exactly the one a minimal rendering cannot show.
 */
public class TestFormatterModuleInfo {

    private static final Runtime RUNTIME = new RuntimeImpl();

    /*
    A SHORT declaration, which is the case the round trip against a real module-info cannot catch: that file is
    long enough that the formatter splits it whatever the separator says.

    The directives are joined with NEWLINE rather than NONE. A statement block uses NONE and collapses onto one
    line when it fits -- the house style for code -- and `module m { requires a; exports b; }` is legal Java that
    nobody writes. It also matters for editing rather than taste: a lever that rewrites one directive of a short
    descriptor would otherwise reformat the whole file around it.
     */
    @DisplayName("a short module declaration still prints one directive per line")
    @Test
    public void shortDeclarationIsNotCollapsed() {
        ModuleInfo m = build(b -> {
            b.addRequires(null, List.of(), "m.a", false, false);
            b.addExports(null, List.of(), "p.b", List.of());
        });
        Formatter formatter = new Formatter2Impl(RUNTIME, new FormattingOptionsImpl.Builder().build());
        assertEquals("""
                module m.example {
                    requires m.a;
                    exports p.b;
                }
                """, formatter.write(m.print(QualificationImpl.FULLY_QUALIFIED_NAMES)));
    }


    private static ModuleInfo build(java.util.function.Consumer<ModuleInfo.Builder> directives) {
        ModuleInfo.Builder builder = RUNTIME.newModuleInfoBuilder().setName("m.example");
        directives.accept(builder);
        return builder.build();
    }

    @DisplayName("a long declaration wraps the same way, one directive per line")
    @Test
    public void longDeclaration() {
        ModuleInfo m = build(b -> {
            for (int i = 0; i < 12; i++) b.addExports(null, List.of(), "some.rather.long.package.name" + i,
                    List.of());
        });
        Formatter formatter = new Formatter2Impl(RUNTIME, new FormattingOptionsImpl.Builder().build());
        String printed = formatter.write(m.print(QualificationImpl.FULLY_QUALIFIED_NAMES));
        assertEquals(14, printed.lines().count(), printed);
    }
}
