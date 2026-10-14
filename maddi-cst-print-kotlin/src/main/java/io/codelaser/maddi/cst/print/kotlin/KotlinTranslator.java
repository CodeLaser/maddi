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
            } catch (RuntimeException | StackOverflowError e) {
                crashes.add(type.fullyQualifiedName() + ": " + e);
            }
        }
        return new Result(files, crashes, messages);
    }
}
