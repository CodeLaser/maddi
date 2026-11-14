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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

/**
 * Java to Kotlin for a list of types: each compilation unit as a {@code .kt} file under a directory, by package. What
 * the CLI's {@code --translate kotlin} writes and the J2K ratchets judge, so that the ratchets measure what ships.
 * <p>
 * A crash of the printer on one compilation unit is recorded and the others go on.
 * <p>
 * With {@link KotlinPrintOptions.Bodies#STUB}, the bodies are {@code TODO("maddi:<id>")} and {@code maddi-bodies.json}
 * beside the packages holds a {@link KotlinBodyStub} for each.
 */
public final class KotlinTranslator {

    /** The file in the source directory that holds the stubs of {@link KotlinPrintOptions.Bodies#STUB}. */
    public static final String BODIES_FILE = "maddi-bodies.json";

    /** @param stubs the stubbed bodies, in {@link KotlinPrintOptions.Bodies#STUB}; else empty */
    public record Result(List<Path> files, List<String> crashes, List<PrintMessage<KotlinPrintCode>> messages,
                         List<KotlinBodyStub> stubs) {
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
        List<KotlinBodies.Declared> declared = new ArrayList<>();
        List<KotlinBodies.DeclaredField> declaredFields = new ArrayList<>();
        Map<MethodInfo, Path> kotlinFiles = new HashMap<>();
        for (TypeInfo type : types) {
            if (!printed.add(type.compilationUnit())) continue; // a file with two primary types prints once
            try {
                KotlinCompilationUnitPrinter.Result result = new KotlinCompilationUnitPrinter(type.compilationUnit(),
                        true, options).printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
                messages.addAll(result.messages());
                String kotlin = formatter.write(result.output()) + "\n";
                Path file = src.resolve(type.packageName().replace('.', '/')).resolve(type.simpleName() + ".kt");
                declaredFields.addAll(result.bodies().fields());
                for (KotlinBodies.Declared d : result.bodies().declared()) {
                    declared.add(d);
                    kotlinFiles.put(d.method(), src.relativize(file));
                }
                Files.createDirectories(file.getParent());
                Files.writeString(file, kotlin);
                files.add(file.toAbsolutePath().normalize());
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                crashes.add(type.fullyQualifiedName() + ": " + e);
            }
        }
        List<KotlinBodyStub> stubs = stubs(declared, declaredFields, kotlinFiles, formatter);
        if (options.bodies() == KotlinPrintOptions.Bodies.STUB) KotlinBodyStub.write(stubs, src.resolve(BODIES_FILE));
        return new Result(files, crashes, messages, stubs);
    }

    /**
     * The stubbed bodies, each with the declarations of the translated methods it calls: those of every file, which
     * is why this waits for the last one.
     */
    private static List<KotlinBodyStub> stubs(List<KotlinBodies.Declared> declared,
                                              List<KotlinBodies.DeclaredField> declaredFields,
                                              Map<MethodInfo, Path> kotlinFiles, Formatter2Impl formatter) {
        Map<MethodInfo, String> declarations = new HashMap<>();
        declared.forEach(d -> declarations.put(d.method(), formatter.write(d.declaration()).strip()));
        Map<FieldInfo, String> fieldDeclarations = new HashMap<>();
        declaredFields.forEach(f -> fieldDeclarations.put(f.field(), formatter.write(f.declaration()).strip()));
        List<KotlinBodyStub> stubs = new ArrayList<>();
        for (KotlinBodies.Declared d : declared) {
            if (!d.stubbed()) continue;
            MethodInfo m = d.method();
            Set<MethodInfo> called = new LinkedHashSet<>();
            Set<FieldInfo> read = new LinkedHashSet<>();
            calls(m, called, read, new HashSet<>());
            List<KotlinBodyStub.Callee> callees = called.stream().filter(declarations::containsKey)
                    .map(c -> new KotlinBodyStub.Callee(KotlinBodyStub.id(c), declarations.get(c))).toList();
            Source source = m.source();
            boolean lines = source != null && !source.isNoSource();
            stubs.add(new KotlinBodyStub(KotlinBodyStub.id(m), kotlinFiles.get(m).toString(),
                    String.valueOf(m.typeInfo().primaryType().compilationUnit().uri()),
                    lines ? source.beginLine() : -1, lines ? source.endLine() : -1,
                    declarations.get(m), formatter.write(d.printed()).strip(),
                    d.messages().stream().map(Object::toString).toList(), callees,
                    read.stream().filter(fieldDeclarations::containsKey)
                            .map(f -> new KotlinBodyStub.Callee(f.fullyQualifiedName(), fieldDeclarations.get(f)))
                            .toList()));
        }
        return stubs;
    }

    /**
     * The methods and constructors {@code m}'s body calls or references, and the fields it reads or writes, those of
     * the anonymous classes, local classes and lambdas in it included: they are part of the body the stub stands for.
     */
    private static void calls(MethodInfo m, Set<MethodInfo> called, Set<FieldInfo> read, Set<MethodInfo> seen) {
        if (m.methodBody() == null || !seen.add(m)) return;
        m.methodBody().visit(e -> {
            switch (e) {
                case MethodCall mc -> called.add(mc.methodInfo());
                case ConstructorCall cc -> {
                    if (cc.callsConstructorOfCreatedType()) called.add(cc.constructor());
                    if (cc.anonymousClass() != null) nested(cc.anonymousClass(), called, read, seen);
                }
                case MethodReference mr -> called.add(mr.methodInfo());
                case VariableExpression ve when ve.variable() instanceof FieldReference fr -> read.add(fr.fieldInfo());
                case Lambda lambda -> calls(lambda.methodInfo(), called, read, seen);
                case LocalTypeDeclaration local -> nested(local.typeInfo(), called, read, seen);
                default -> {
                }
            }
            return true;
        });
    }

    private static void nested(TypeInfo type, Set<MethodInfo> called, Set<FieldInfo> read, Set<MethodInfo> seen) {
        type.constructorAndMethodStream().forEach(n -> calls(n, called, read, seen));
    }
}
