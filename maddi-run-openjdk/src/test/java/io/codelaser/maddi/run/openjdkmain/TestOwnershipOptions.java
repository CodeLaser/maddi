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

package io.codelaser.maddi.run.openjdkmain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.run.config.GeneralConfiguration;

/**
 * The ownership pass ({@code @Fresh}, {@code @Borrowed}, {@code @Owned}) from the command line and from the
 * key/value map the build plugins write, rather than only through the {@code OWNERSHIP} environment gate.
 */
public class TestOwnershipOptions {

    private static GeneralConfiguration parse(String... args) throws ParseException {
        CommandLine cmd = DefaultParser.builder().get().parse(Main.createOptions(), args);
        return Main.parseGeneralConfiguration(cmd);
    }

    @Test
    public void offByDefault() throws ParseException {
        GeneralConfiguration g = parse("--source", "src");
        assertFalse(g.ownership());
        assertFalse(g.closedWorld());
    }

    @Test
    public void commandLine() throws ParseException {
        GeneralConfiguration open = parse("--source", "src", "--" + Main.OWNERSHIP);
        assertTrue(open.ownership());
        assertFalse(open.closedWorld());
        GeneralConfiguration closed = parse("--source", "src", "--" + Main.OWNERSHIP, "--" + Main.CLOSED_WORLD);
        assertTrue(closed.ownership());
        assertTrue(closed.closedWorld());
    }

    @Test
    public void propertyMap() {
        GeneralConfiguration g = Main.generalConfiguration(Map.of(Main.OWNERSHIP, "true", Main.CLOSED_WORLD, "true"));
        assertTrue(g.ownership());
        assertTrue(g.closedWorld());
        assertFalse(Main.generalConfiguration(Map.of()).ownership());
    }
}
