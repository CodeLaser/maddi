package org.e2immu.analyzer.aapi.parser;

import java.util.List;

public interface AnnotatedAPIConfiguration {

    // use case 1: input for normal analyzer, and for use cases 2, 3

    List<String> analyzedAnnotatedApiDirs();

    // use case 2: read AAPI, write AAAPI

    String analyzedAnnotatedApiTargetDir();

    // use case 3: read source code or byte code, write AAPI skeleton

    String annotatedApiTargetDir();

    List<String> annotatedApiPackages();

    String annotatedApiTargetPackage();
}
