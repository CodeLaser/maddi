/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test4 {
    private final Runtime runtime = new RuntimeImpl();

    static OutputBuilder createExample0() {

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
    public void test1() {
        OutputBuilder outputBuilder = createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                EnumMap<K extends Enum<K>, V>
                """;
        assertEquals(expect, string);
    }

}
