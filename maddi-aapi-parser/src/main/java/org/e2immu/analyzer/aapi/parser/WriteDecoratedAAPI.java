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

package org.e2immu.analyzer.aapi.parser;

import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WriteDecoratedAAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteDecoratedAAPI.class);
    private final JavaInspector javaInspector;
    private final Function<Element, AnnotatedApiParser.Data> dataProvider;
    private final Function<Element, ShallowAnalyzer.InfoData> infoDataProvider;

    public WriteDecoratedAAPI(JavaInspector javaInspector,
                              Function<Element, AnnotatedApiParser.Data> dataProvider,
                              Function<Element, ShallowAnalyzer.InfoData> infoDataProvider) {
        this.javaInspector = javaInspector;
        this.dataProvider = dataProvider;
        this.infoDataProvider = infoDataProvider;
    }

    public void write(String destinationDirectory, Trie<TypeInfo> typeTrie, String destinationPackage) throws IOException {
        File directory = new File(destinationDirectory);
        if (directory.mkdirs()) {
            LOGGER.info("Created directory {}", directory.getAbsolutePath());
        }
        try {
            typeTrie.visitThrowing(new String[]{}, (parts, list)
                    -> write(directory, parts, list, destinationPackage));
        } catch (RuntimeException re) {
            if (re.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw re;
        }
    }

    private void write(File directory, String[] packageParts, List<TypeInfo> list, String destinationPackage) throws IOException {
        if (list.isEmpty()) return;
        String compressedPackages = Arrays.stream(packageParts).map(WriteDecoratedAAPI::capitalize)
                .collect(Collectors.joining());
        File outputFile = new File(directory, compressedPackages + ".json");
        LOGGER.info("Writing {} type(s) to {}", list.size(), outputFile.getAbsolutePath());
        Composer composer = new Composer(javaInspector, set -> destinationPackage, w -> true);
        Collection<TypeInfo> apiTypes = composer.compose(list);

        Map<Element, Element> dollarMap = composer.translateFromDollarToReal();
        composer.write(apiTypes, directory, new DecoratorWithComments(javaInspector.runtime(),
                javaInspector.mainSources(), dollarMap, infoDataProvider, dataProvider));

    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
