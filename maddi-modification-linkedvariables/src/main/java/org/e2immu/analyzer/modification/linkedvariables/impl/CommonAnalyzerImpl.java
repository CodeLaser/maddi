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

package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommonAnalyzerImpl {
    public static final Logger DECIDE = LoggerFactory.getLogger("e2immu.modanalyzer.decide");
    public static final Logger UNDECIDED = LoggerFactory.getLogger("e2immu.modanalyzer.delay");

    protected final IteratingAnalyzer.Configuration configuration;

    protected CommonAnalyzerImpl(IteratingAnalyzer.Configuration configuration) {
        this.configuration = configuration;
    }

    protected static String highlight(String content) {
        return "\033[31;1;4m" + content + "\033[0m";
    }

    protected static final String CYCLE_BREAKING = highlight("cycle breaking");
}
