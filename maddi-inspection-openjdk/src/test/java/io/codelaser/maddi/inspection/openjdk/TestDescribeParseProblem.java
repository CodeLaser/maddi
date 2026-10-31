package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.inspection.api.parser.Summary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The parse's INFO line names its first problem on one line: the file as a path, where in it, and javac's message
 * with its continuation lines joined.
 */
public class TestDescribeParseProblem {

    @DisplayName("a javac error carried as a warning: path, line and column, message")
    @Test
    public void javacError() {
        Summary.ParseException pe = new Summary.ParseException(URI.create("file:/p/src/demo/Inventory.java"),
                "line 3, col 8", "class Inventor is public, should be declared in a file named Inventor.java",
                null, Message.Severity.WARN);
        assertEquals("/p/src/demo/Inventory.java (line 3, col 8): class Inventor is public, should be declared in a"
                     + " file named Inventor.java", JavaInspectorImpl.describe(pe));
    }

    @DisplayName("javac's continuation lines join; a whole-unit problem names only the file")
    @Test
    public void multiLine() {
        Summary.ParseException pe = new Summary.ParseException(URI.create("file:/p/A.java"), "compilation unit",
                "cannot find symbol\n  symbol:   class Foo\n  location: class A", null, Message.Severity.WARN);
        assertEquals("/p/A.java: cannot find symbol; symbol:   class Foo; location: class A",
                JavaInspectorImpl.describe(pe));
    }
}
