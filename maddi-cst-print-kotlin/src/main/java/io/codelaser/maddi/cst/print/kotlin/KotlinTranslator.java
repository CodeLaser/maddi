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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Java to Kotlin for a list of types: each compilation unit as a {@code .kt} file under a directory, by package. What
 * the CLI's {@code --translate kotlin} writes and the J2K ratchets judge, so that the ratchets measure what ships.
 * <p>
 * A crash of the printer on one compilation unit is recorded and the others go on.
 */
public final class KotlinTranslator {

    public record Result(List<Path> files, List<String> crashes, List<PrintMessage<KotlinPrintCode>> messages) {
    }

    private KotlinTranslator() {
    }

    /** Each compilation unit of {@code types} as a Kotlin file under {@code src}. */
    public static Result translate(Runtime runtime, List<TypeInfo> types, KotlinPrintOptions options, Path src)
            throws IOException {
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<PrintMessage<KotlinPrintCode>> messages = new ArrayList<>();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        Set<Object> printed = new HashSet<>();
        for (TypeInfo type : types) {
            if (!printed.add(type.compilationUnit())) continue; // a file with two primary types prints once
            try {
                KotlinCompilationUnitPrinter.Result result = new KotlinCompilationUnitPrinter(type.compilationUnit(),
                        true, options).printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
                messages.addAll(result.messages());
                String kotlin = formatter.write(result.output()) + "\n";
                Path file = src.resolve(type.packageName().replace('.', '/')).resolve(type.simpleName() + ".kt");
                Files.createDirectories(file.getParent());
                Files.writeString(file, kotlin);
                files.add(file.toAbsolutePath().normalize());
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                crashes.add(type.fullyQualifiedName() + ": " + e);
            }
        }
        return new Result(files, crashes, messages);
    }
}
