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

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test7 {

    @Test
    public void test() {
        GuideImpl.GuideGenerator block = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator def = GuideImpl.defaultGuideGenerator();
        Runtime runtime = new RuntimeImpl();
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        OutputBuilder ob = new OutputBuilderImpl()
                .add(SymbolEnum.LEFT_BRACE)
                .add(block.start())
                .add(gg.start())
                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("@NotModified"))
                .add(SpaceEnum.NEWLINE)
                .add(gg.mid())
                .add(def.start())
                .add(KeywordImpl.STATIC)
                .add(SpaceEnum.ONE)
                .add(def.mid())
                .add(KeywordImpl.FINAL)
                .add(def.end())
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("Set", "j.l.Set", "Set",
                        TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, false))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("EMPTY_SET"))
                .add(SpaceEnum.ONE_IS_NICE_EASY_L)
                .add(gg.end())
                .add(block.end());
        assertEquals("""
                {
                    //@NotModified
                    static final Set EMPTY_SET
                """, formatter.write(ob));
    }
}
