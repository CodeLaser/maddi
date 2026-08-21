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

package io.codelaser.maddi.gradleplugin.inputconfig;

import java.util.List;

/**
 * What a source set's {@code JavaCompile} task says about its compilation: the Java release it targets, its
 * {@code --add-modules}, and its warning policy.
 *
 * <p>⛔⛔ <b>THESE ARE THE THREE FACTS A SIBLING PROJECT DOES NOT GET, AND THE REASON IS STRUCTURAL.</b> They
 * live on a {@code JavaCompile} task; a sibling's task belongs to another project, and reading it is the
 * cross-project access this plugin's whole source-variant mechanism exists to avoid.
 * {@code ComputeSourceSets.dependentProjectResult} therefore hands a sibling {@code 0} and two empty lists.
 *
 * <p>⚠ <b>MEASURED, on OpenSearch (2026-08-20, {@code applyTo=all}): 2 source sets of 21 carried any warning
 * flag</b> — the analysed project's own two. The other nineteen also arrived with {@code sourceRelease=0},
 * which silently reinstates "whatever JDK maddi happens to run on" for each of them.
 *
 * <p>⛔ <b>AND THE OBVIOUS REPAIR WAS WRITTEN AND TAKEN BACK OUT.</b> Publishing them as an artifact on the
 * {@code maddiSourceElements} variant, beside the source directories, cannot work: {@code ComputeSourceSets}
 * runs at CONFIGURATION time, and a file only a task can produce does not exist then. The consumer would read
 * an absent file and get precisely the empty lists it has today. The channels that could work — writing the
 * facts during configuration, or a shared {@code BuildService} keyed by project path — are decisions rather
 * than repairs, and neither has met a corpus.
 */
public record SourceFacts(int sourceRelease, List<String> addModules, List<String> warningFlags) {

    public SourceFacts {
        addModules = addModules == null ? List.of() : List.copyOf(addModules);
        warningFlags = warningFlags == null ? List.of() : List.copyOf(warningFlags);
    }
}
