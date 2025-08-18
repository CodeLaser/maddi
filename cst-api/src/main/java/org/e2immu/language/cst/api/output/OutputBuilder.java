package org.e2immu.language.cst.api.output;

import org.e2immu.language.cst.api.output.element.Symbol;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interface to hold the output that's being accumulated while printing.
 */
public interface OutputBuilder {

    default OutputBuilder addIfNotNull(OutputBuilder outputBuilder) {
        return outputBuilder != null ? add(outputBuilder) : this;
    }

    OutputBuilder add(OutputElement... outputElements);

    OutputBuilder add(OutputBuilder... outputBuilders);

    List<OutputElement> list();

    // remove the first
    void removeLast();

    Stream<OutputElement> stream();

    boolean isEmpty();

    boolean notStart();

    default String generateJavaForDebugging() {
        return list().stream().map(OutputElement::generateJavaForDebugging).collect(Collectors.joining("\n"));
    }
}
