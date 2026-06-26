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
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGreedyFill {
    private final Runtime runtime = new RuntimeImpl();

    /*
    Build a method-like header: `String m(int aaa, int bbb, int ccc, int ddd)`.
    The parameter list is wrapped in a parameter-declaration guide so the formatter
    treats commas as candidate split positions.
     */
    private static OutputBuilder parameterListHeader() {
        GuideImpl.GuideGenerator params = GuideImpl.generatorForParameterDeclaration();
        return new OutputBuilderImpl()
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("m"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(params.start())
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("aaa"))
                .add(SymbolEnum.COMMA)
                .add(params.mid())
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("bbb"))
                .add(SymbolEnum.COMMA)
                .add(params.mid())
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("ccc"))
                .add(SymbolEnum.COMMA)
                .add(params.mid())
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("ddd"))
                .add(params.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    @Test
    public void chopDownIsTheDefault() {
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(25).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(parameterListHeader());
        // signature is 38 chars; with line length 25 every parameter goes onto its own line.
        String expect = """
                String m(
                    int aaa,
                    int bbb,
                    int ccc,
                    int ddd)
                """;
        assertEquals(expect, out);
    }

    @Test
    public void greedyFillsTheLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(25).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(parameterListHeader());
        // greedy: as many params as fit per line. After "String m(int aaa, " (18) we still have
        // room for "int bbb," (8) -> 26, over the 25 limit -> wrap. Then "int ccc, int ddd"
        // fits on a 4-indented sub-line, etc.
        String expect = """
                String m(int aaa, int bbb,
                    int ccc, int ddd)
                """;
        assertEquals(expect, out);
    }

    @Test
    public void greedyDegradesToChopDownWhenSubBlockAlreadyWrapped() {
        // very small line length forces nested wrapping; the outer block has only a single chunk anyway,
        // so this mainly checks we don't crash and the output is sensible.
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(10).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(parameterListHeader());
        // tight budget — each parameter must end up on its own line; the result is the same as chop-down.
        String expect = """
                String m(
                    int aaa,
                    int bbb,
                    int ccc,
                    int ddd)
                """;
        assertEquals(expect, out);
    }

    /*
    Build a method-call argument list: `foo(arg1, arg2, arg3, arg4)`.
    Method calls join arguments with the default guide generator, so the split candidates
    sit at the start, after each comma, and at the end.
     */
    private static OutputBuilder methodCall(String name, String... args) {
        GuideImpl.GuideGenerator gg = GuideImpl.defaultGuideGenerator();
        OutputBuilderImpl ob = new OutputBuilderImpl();
        ob.add(new TextImpl(name)).add(SymbolEnum.LEFT_PARENTHESIS).add(gg.start());
        for (int i = 0; i < args.length; i++) {
            if (i > 0) ob.add(SymbolEnum.COMMA).add(gg.mid());
            ob.add(new TextImpl(args[i]));
        }
        ob.add(gg.end()).add(SymbolEnum.RIGHT_PARENTHESIS);
        return ob;
    }

    @Test
    public void greedyOnMethodCall() {
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(25).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        // foo(alpha, beta, gamma, delta) is 30 chars — needs wrapping at line length 25.
        String out = formatter.write(methodCall("foo", "alpha", "beta", "gamma", "delta"));
        // "foo(alpha, beta, gamma," = 23, fits. Next "delta" wouldn't (with the leading space => 24+5=29 > 25), wrap.
        String expect = """
                foo(alpha, beta, gamma,
                    delta)
                """;
        assertEquals(expect, out);
    }

    /*
    Nested method calls: outer(a, inner(x, y, z, w), b, c).
    With a small line length the inner call is forced to wrap, so its rendered text contains
    newlines. The outer block's chunk that holds the inner call therefore has '\n' in it, which
    triggers the 3b fallback: outer reverts to chop-down so the wrapped inner doesn't visually
    glue onto the wrong neighbour.
     */
    private static OutputBuilder nestedCall() {
        GuideImpl.GuideGenerator outer = GuideImpl.defaultGuideGenerator();
        GuideImpl.GuideGenerator inner = GuideImpl.defaultGuideGenerator();
        return new OutputBuilderImpl()
                .add(new TextImpl("outer")).add(SymbolEnum.LEFT_PARENTHESIS).add(outer.start())
                .add(new TextImpl("aaa"))
                .add(SymbolEnum.COMMA).add(outer.mid())
                .add(new TextImpl("inner")).add(SymbolEnum.LEFT_PARENTHESIS).add(inner.start())
                .add(new TextImpl("xxx"))
                .add(SymbolEnum.COMMA).add(inner.mid()).add(new TextImpl("yyy"))
                .add(SymbolEnum.COMMA).add(inner.mid()).add(new TextImpl("zzz"))
                .add(SymbolEnum.COMMA).add(inner.mid()).add(new TextImpl("www"))
                .add(inner.end()).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.COMMA).add(outer.mid())
                .add(new TextImpl("bbb"))
                .add(SymbolEnum.COMMA).add(outer.mid())
                .add(new TextImpl("ccc"))
                .add(outer.end()).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    @Test
    public void greedyFallsBackToChopDownWhenInnerCallWrapped() {
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(20).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(nestedCall());
        // Inner is formatted first; it doesn't fit on 20 chars on a single line so it wraps
        // (greedy fills each sub-line: "inner(xxx, yyy," then "zzz, www)"). Because the inner's
        // rendered text now contains '\n', the OUTER block hits the 3b fallback and chops down —
        // every outer argument lands on its own line, with the already-wrapped inner sitting
        // among them.
        String expect = """
                outer(
                    aaa,
                    inner(xxx, yyy,
                        zzz, www),
                    bbb,
                    ccc)
                """;
        assertEquals(expect, out);
    }

    @Test
    public void greedyOnOuterCallWhenInnerFits() {
        // Larger line length: the inner call fits on a single rendered line, so no '\n' in it.
        // Greedy still isolates the structured (nested-guide) argument: a forced split lands
        // both BEFORE and AFTER `inner(...)` so it has its own line, while the remaining flat
        // args after it greedy-pack onto subsequent lines.
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(35).setSpacesInTab(4)
                .setWrapStyle(FormattingOptions.WrapStyle.GREEDY_FILL).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(nestedCall());
        String expect = """
                outer(aaa,
                    inner(xxx, yyy, zzz, www),
                    bbb, ccc)
                """;
        assertEquals(expect, out);
    }
}