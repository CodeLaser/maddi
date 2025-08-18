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
