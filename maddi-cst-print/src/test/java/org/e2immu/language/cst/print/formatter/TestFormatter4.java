/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.print.formatter;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFormatter4 {
    private final Runtime runtime = new RuntimeImpl();

    public static OutputBuilder createExample0() {

        GuideImpl.GuideGenerator gg2044 = GuideImpl.defaultGuideGenerator();

        return new OutputBuilderImpl().add(new TypeNameImpl("EnumMap"))
                .add(SymbolEnum.LEFT_ANGLE_BRACKET)
                .add(gg2044.start()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("K"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("extends"))
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("Enum"))
                .add(SymbolEnum.LEFT_ANGLE_BRACKET)
                .add(new TextImpl("K"))
                .add(SymbolEnum.RIGHT_ANGLE_BRACKET)
                .add(SymbolEnum.COMMA)
                .add(gg2044.mid()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("V"))
                .add(gg2044.end()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.RIGHT_ANGLE_BRACKET);
    }


    @Test
    public void testExample0() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        Formatter formatter = new FormatterImpl(runtime, options);
        OutputBuilder example = createExample0();

        assertEquals("EnumMap<K extends Enum<K>, V>\n", formatter.write(example));
    }
}
