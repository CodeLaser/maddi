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

package io.codelaser.maddi.cst.print;

import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.element.Guide;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOutputBuilder {

    @Test
    public void test() {
        OutputBuilder[] outputBuilders = { new OutputBuilderImpl().add(new TextImpl("a")),
                new OutputBuilderImpl().add(new TextImpl("b")),
                new OutputBuilderImpl().add(new TextImpl("c")), };
        OutputBuilder all = Arrays.stream(outputBuilders).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
        System.out.println(all.list());
        assertEquals(9, all.list().size());
        assertTrue(all.list().get(0) instanceof GuideImpl guide && guide.position() == GuideImpl.Position.START);
        assertTrue(all.list().get(3) instanceof GuideImpl guide && guide.position() == GuideImpl.Position.MID);
        assertTrue(all.list().get(6) instanceof GuideImpl guide && guide.position() == GuideImpl.Position.MID);
        assertTrue(all.list().get(8) instanceof GuideImpl guide && guide.position() == GuideImpl.Position.END);
    }
}
