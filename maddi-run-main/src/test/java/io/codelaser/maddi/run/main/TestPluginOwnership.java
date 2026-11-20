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

package io.codelaser.maddi.run.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.run.config.GeneralConfiguration;

/** The build plugins' key/value map carries the ownership pass and its world into the run's configuration. */
public class TestPluginOwnership {

    @Test
    public void theMapCarriesOwnership() {
        GeneralConfiguration on = Main.generalConfiguration(PluginOptions.generalConfigMap(false, null,
                new File("build/e2immu"), false, null, null, false, false, false, true, true));
        assertTrue(on.ownership());
        assertTrue(on.closedWorld());
        GeneralConfiguration off = Main.generalConfiguration(PluginOptions.generalConfigMap(false, null,
                new File("build/e2immu"), false, null, null, false, false, false));
        assertFalse(off.ownership());
        assertFalse(off.closedWorld());
    }
}
