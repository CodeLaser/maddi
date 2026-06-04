package org.e2immu.language.java.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.*;

public class MaddiDiagnosticCollector implements DiagnosticListener<JavaFileObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaddiDiagnosticCollector.class);
    private final boolean ignoreErrors;

    public MaddiDiagnosticCollector(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public enum DiagnosticKind {MISSING_CLASS, ERROR}

    public record MaddiDiagnostic(DiagnosticKind diagnosticKind,
                                  String msg,
                                  String path,
                                  long line,
                                  long col) {
    }

    private final List<MaddiDiagnostic> diagnostics = new ArrayList<>();
    private final Set<String> missingTypes = new HashSet<>();

    public void reportMissingClassFile(Symbol.CompletionFailure completionFailure) {
        String msg = completionFailure.getLocalizedMessage();
        diagnostics.add(new MaddiDiagnostic(DiagnosticKind.MISSING_CLASS,
                msg, "?", 0, 0));
        if (missingTypes.add(msg)) {
            LOGGER.warn(msg);
        }
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
            String msg = diagnostic.getMessage(Locale.ENGLISH);
            String path = diagnostic.getSource() != null
                    ? diagnostic.getSource().toUri().getPath() : null;
            if (msg.contains("class file for") && msg.contains("not found")) {
                // Missing classpath entry — log and continue
                diagnostics.add(new MaddiDiagnostic(
                        DiagnosticKind.MISSING_CLASS,
                        msg,
                        path,
                        diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber()
                ));
                return; // swallow — don't let it abort compilation
            }
            // other errors
            diagnostics.add(new MaddiDiagnostic(DiagnosticKind.ERROR, msg, path, diagnostic.getLineNumber(),
                    diagnostic.getColumnNumber()));
        }
    }

    public boolean isHalt() {
        return !ignoreErrors
               && diagnostics.stream().anyMatch(md -> DiagnosticKind.ERROR == md.diagnosticKind);
    }

    public List<MaddiDiagnostic> diagnostics() {
        return diagnostics;
    }
}
