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
package io.codelaser.maddi.cst.print.kotlin;

import java.util.List;
import java.util.Objects;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

/**
 * One file's members as {@link KotlinPrintOptions.Bodies#STUB} printed them: each method's Kotlin declaration and, where
 * its body became a stub, the printer's own translation of the whole method, kept aside. KotlinTranslator turns them
 * into {@link KotlinBodyStub}s.
 *
 * @param declared in the order printed
 */
public record KotlinBodies(List<Declared> declared) {

    public static final KotlinBodies NONE = new KotlinBodies(List.of());

    /**
     * @param method      the Java method
     * @param declaration its Kotlin declaration: annotations, modifiers, name, parameters, result; no body
     * @param printed     the printer's translation of the method, body included; null when the body was not stubbed
     *                    (abstract, a constructor)
     * @param messages    the printer's messages about that body
     */
    public record Declared(MethodInfo method, OutputBuilder declaration, OutputBuilder printed,
                           List<PrintMessage<KotlinPrintCode>> messages) {
        public Declared {
            Objects.requireNonNull(method);
            Objects.requireNonNull(declaration);
            messages = List.copyOf(messages);
        }

        public boolean stubbed() {
            return printed != null;
        }
    }
}
