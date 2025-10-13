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

package org.e2immu.language.inspection.integration;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDetectJREs {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDetectJREs.class);

    @Test
    public void test() {
        List<ToolChain.JRE> jres = DetectJREs.runSystemCommand();
        assertTrue(jres.size() > 1);
        boolean have17 = false;
        for (ToolChain.JRE jre : jres) {
            LOGGER.info("JRE = {}", jre);
            if(jre.mainVersion() == 17) have17 = true;
        }
        assertTrue(have17);
    }

    @Test
    public void test2() throws IOException, ParserConfigurationException, SAXException {
       String xml = Files.readString(Path.of("src/test/resources/e2immu.java_home_example.xml"));
       List<ToolChain.JRE> jres = DetectJREs.parseMacOsXml(xml);
       assertEquals(6, jres.size());
    }
}
