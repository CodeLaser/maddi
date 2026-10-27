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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole-program policies ({@link CSharpProgram.Policy}) besides the default: functional interfaces as adapters,
 * inheritance open, printing without a program, the facts' factories, and a nullable project file.
 */
public class TestJavaToCSharpPolicies extends CommonJavaToCSharp {

    @Language("java")
    private static final String SHAPES = """
            package org.example.policies;
            import java.util.List;
            public class Shapes {
                interface Area { double of(double side); }
                public static class Square { public double side() { return 1; } protected int corners() { return 4; } }
                static class Hidden { int n() { return 0; } }
                static double total(List<Double> sides, Area area) {
                    double t = 0;
                    for (double s : sides) t += area.of(s);
                    return t;
                }
                static double squares(List<Double> sides) { return total(sides, s -> s * s); }
            }
            """;

    /** By default, a functional interface the program only lambdas is a delegate, and nothing extended is sealed. */
    @Test
    public void closedWorld() {
        String cs = csharp(SHAPES);
        contains(cs, "internal delegate double Area(double side);");
        contains(cs, "t += area(s);");
        contains(cs, "public sealed class Square {");
        contains(cs, "public double Side() => 1;");
    }

    /** ADAPTER: an interface with a lambda adapter; OPEN: no class sealed, every method virtual. */
    @Test
    public void adapterOpen() {
        policy = new CSharpProgram.Policy(CSharpProgram.FunctionalInterfaces.ADAPTER, CSharpProgram.Inheritance.OPEN);
        assertAdapterOpen(csharp(SHAPES));
    }

    /** Without a program ({@link CSharpProgram#NONE}) nothing is known of the other files: ADAPTER and OPEN. */
    @Test
    public void withoutProgram() {
        assertAdapterOpen(csharpWithoutProgram(SHAPES));
    }

    private static void assertAdapterOpen(String cs) {
        contains(cs, "internal interface IArea {");
        contains(cs, "public sealed class Lambda(Func<double, double> f) : IArea { public double Of(double side) => f(side); }");
        contains(cs, "Total(sides, new IArea.Lambda(s => s * s))");
        contains(cs, "public class Square {");
        contains(cs, "public virtual double Side() => 1;");
        contains(cs, "protected internal virtual int Corners() => 4;");
        contains(cs, "internal class Hidden { internal virtual int N() => 0; }");
    }

    /**
     * {@link CSharpFacts#of}: the modification analysis's verdict makes a parameter read-only; without a nullability
     * analysis, C#'s nullable reference types stay off. {@link CSharpFacts#STORED_NULL_MARKED} enables them.
     */
    @Test
    public void facts() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(CSharpFacts.of(NullabilityFacts.NONE, p -> true));
        String cs = csharp(SHAPES);
        contains(cs, "internal static double Total(IReadOnlyList<double> sides, Area area) {");
        assertFalse(CSharpFacts.of(NullabilityFacts.NONE, p -> true).nullabilityAnalysed());
        assertFalse(CSharpFacts.STORED.nullabilityAnalysed());
        assertTrue(CSharpFacts.STORED_NULL_MARKED.nullabilityAnalysed());
    }

    /** A project file for a null-marked translation enables C#'s nullable reference types. */
    @Test
    public void nullableProjectFile() {
        contains(CSharpLibraries.DEFAULT.projectFile("net10.0", true), "<Nullable>enable</Nullable>");
        contains(CSharpLibraries.DEFAULT.projectFile("net10.0", false), "<Nullable>disable</Nullable>");
    }
}
