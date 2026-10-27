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
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
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
                .add(new TypeNameImpl("Set", "j.l.Set", "Set", "Set",
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
