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

package org.e2immu.language.cst.impl.output;


import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.OutputElement;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OutputBuilderImpl implements OutputBuilder {

    private final List<OutputElement> list = new LinkedList<>();

    @Override
    public Stream<OutputElement> stream() {
        return list.stream();
    }

    @Override
    public OutputBuilder add(OutputElement... outputElements) {
        Collections.addAll(list, outputElements);
        return this;
    }

    @Override
    public OutputBuilder add(OutputBuilder... outputBuilders) {
        Arrays.stream(outputBuilders).flatMap(ob -> ob.list().stream()).forEach(list::add);
        return this;
    }

    @Override
    public List<OutputElement> list() {
        return list;
    }

    @Override
    public void removeLast() {
        if (!list.isEmpty()) {
            list.remove(list.size() - 1);
        }
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean notStart() {
        return !list.stream().allMatch(outputElement -> outputElement instanceof GuideImpl);
    }

    @Override
    public String toString() {
        return list.stream().map(OutputElement::minimal).collect(Collectors.joining());
    }

    public static Collector<OutputBuilder, OutputBuilder, OutputBuilder> joining() {
        return joining(SpaceEnum.NONE, SpaceEnum.NONE, SpaceEnum.NONE, GuideImpl.defaultGuideGenerator());
    }

    public static Collector<OutputBuilder, OutputBuilder, OutputBuilder> joining(OutputElement separator) {
        return joining(separator, SpaceEnum.NONE, SpaceEnum.NONE, GuideImpl.defaultGuideGenerator());
    }

    public static Collector<OutputBuilder, OutputBuilder, OutputBuilder> joining(OutputElement separator,
                                                                                 GuideImpl.GuideGenerator guideGenerator) {
        return joining(separator, SpaceEnum.NONE, SpaceEnum.NONE, guideGenerator);
    }

    public static Collector<OutputBuilder, OutputBuilder, OutputBuilder> joining(OutputElement separator,
                                                                                 OutputElement start,
                                                                                 OutputElement end,
                                                                                 GuideImpl.GuideGenerator guideGenerator) {
        return new Collector<>() {
            private final AtomicInteger countMid = new AtomicInteger();

            @Override
            public Supplier<OutputBuilder> supplier() {
                return OutputBuilderImpl::new;
            }

            @Override
            public BiConsumer<OutputBuilder, OutputBuilder> accumulator() {
                return (a, b) -> {
                    if (!b.isEmpty()) {
                        if (a.notStart()) { // means: not empty, not only guides
                            if (separator != SpaceEnum.NONE) a.add(separator);
                            a.add(guideGenerator.mid());
                            countMid.incrementAndGet();
                        }
                        a.add(b);
                    }
                };
            }

            @Override
            public BinaryOperator<OutputBuilder> combiner() {
                return (a, b) -> {
                    if (a.isEmpty()) return b;
                    if (b.isEmpty()) return a;
                    if (separator != SpaceEnum.NONE) a.add(separator);
                    countMid.incrementAndGet();
                    return a.add(guideGenerator.mid()).add(b);
                };
            }

            @Override
            public Function<OutputBuilder, OutputBuilder> finisher() {
                return t -> {
                    OutputBuilder result = new OutputBuilderImpl();
                    if (start != SpaceEnum.NONE) result.add(start);
                    if (countMid.get() > 0 || guideGenerator.keepGuidesWithoutMid()) {
                        result.add(guideGenerator.start());
                        result.add(t);
                        result.add(guideGenerator.end());
                    } else {
                        result.add(t); // without the guides
                    }
                    if (end != SpaceEnum.NONE) result.add(end);
                    return result;
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.CONCURRENT);
            }
        };
    }

    public OutputBuilderImpl addIf(boolean b, OutputElement outputElement) {
        if (b) {
            add(outputElement);
        }
        return this;
    }
}
