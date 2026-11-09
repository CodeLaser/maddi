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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * The Java side of the behavioural check ({@link CommonJavaToCSharp#behaves}): a sample compiled with javac in memory
 * and its {@code main} run, its printed lines and the exception it ended with kept, to compare with the translation's
 * run on .NET ({@link CSharpCheck#run}).
 */
final class JavaRun {

    /** What a run printed, one string per line, and the simple name of the exception it ended with, or null. */
    record Result(List<String> output, String exception, String exceptionMessage) {
    }

    private JavaRun() {
    }

    /** Compiles {@code source}, the compilation unit of {@code fqn}, and runs that type's {@code main}. */
    static Result run(String fqn, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<String, ByteArrayOutputStream> classes = new HashMap<>();
        StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        JavaFileManager files = new ForwardingJavaFileManager<>(standard) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                                                       FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension),
                        kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        return classes.computeIfAbsent(className, k -> new ByteArrayOutputStream());
                    }
                };
            }
        };
        JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///" + fqn.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        boolean compiled = compiler.getTask(null, files, diagnostics, List.of("-proc:none", "-Xlint:none"), null,
                List.of(unit)).call();
        if (!compiled) {
            List<String> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) errors.add(d.getLineNumber() + ": " + d.getMessage(null));
            }
            throw new AssertionError("javac rejects the sample:\n" + String.join("\n", errors));
        }
        ClassLoader loader = new ClassLoader(JavaRun.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                ByteArrayOutputStream bytes = classes.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                byte[] b = bytes.toByteArray();
                return defineClass(name, b, 0, b.length);
            }
        };
        return invoke(loader, fqn);
    }

    /** {@code main} run in its own thread with System.out caught, at most ten seconds. */
    private static synchronized Result invoke(ClassLoader loader, String fqn) {
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        PrintStream out = System.out;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String exception = null;
        String message = null;
        try {
            Method main = Class.forName(fqn, true, loader).getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
            Future<?> future = executor.submit(() -> {
                main.invoke(null, (Object) new String[0]);
                return null;
            });
            future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            exception = "Timeout";
            message = "more than ten seconds";
        } catch (Exception e) {
            Throwable t = e;
            while ((t instanceof java.util.concurrent.ExecutionException || t instanceof InvocationTargetException
                    || t instanceof ExceptionInInitializerError) && t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof ClassNotFoundException || t instanceof NoSuchMethodException) {
                throw new AssertionError("no main in " + fqn, t);
            }
            exception = t.getClass().getSimpleName();
            message = String.valueOf(t.getMessage());
        } finally {
            System.out.flush();
            System.setOut(out);
            executor.shutdownNow();
        }
        String text = printed.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
        List<String> lines = printed.size() == 0 ? List.of() : List.of(text.split("\n", -1));
        return new Result(lines, exception, message);
    }
}
