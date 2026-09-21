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

package io.codelaser.maddi.run.openjdkmain;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ <b>Two options shared the short form {@code -s}.</b> commons-cli keys its short-option map by the letter,
 * so the later registration ({@code --preload-analysis-results-dirs}) silently won and
 * {@code maddi -s src --analysis-steps prep} bound the source directory to the analysis-hints option:
 * measured 2026-09-21, "Running prep analyzer on <b>0 types</b>" and <b>exit 0</b> — a run that analyzed
 * nothing and reported success. {@code --source} was not even listed in {@code --help}.
 *
 * <p>A collision cannot be seen by reading the option list; it needs the assembled {@link Options} to be
 * asked. That is what this does, for every short form at once.
 */
public class TestShortOptionBindings {

    @Test
    public void dashSMeansSource() {
        Options options = Main.createOptions();
        assertEquals(Main.SOURCE, options.getOption("s").getLongOpt());
        // the one that used to steal it keeps its long form and takes no short form at all
        assertTrue(options.hasLongOption(Main.PRELOAD_ANALYSIS_RESULTS_DIRS));
        assertNull(options.getOption(Main.PRELOAD_ANALYSIS_RESULTS_DIRS).getOpt());
    }

    /** ⭐ The general rule, so the next added option cannot repeat it: no two options share a short form. */
    @Test
    public void noTwoOptionsShareAShortForm() {
        Map<String, String> byShort = new HashMap<>();
        for (Option option : Main.createOptions().getOptions()) {
            if (option.getOpt() == null) continue;
            String previous = byShort.put(option.getOpt(), option.getLongOpt());
            assertNull(previous, () -> "-" + option.getOpt() + " is both --" + previous
                                       + " and --" + option.getLongOpt() + "; the later one silently wins");
        }
    }
}
