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
 * <p>⛔⛔ <b>THESE ARE THE THREE FACTS A SIBLING PROJECT COULD NOT GET, AND THE REASON WAS STRUCTURAL.</b>
 * They live on a {@code JavaCompile} task; a sibling's task belongs to another project, and reading it is
 * the cross-project access this plugin's whole source-variant mechanism exists to avoid. So
 * {@code ComputeSourceSets.dependentProjectResult} handed a sibling {@code 0} and two empty lists.
 *
 * <p>⚠ <b>MEASURED, on OpenSearch (2026-08-20, {@code applyTo=all}): 2 source sets of 21 carried any warning
 * flag</b> — the analysed project's own two. The other nineteen also arrived with {@code sourceRelease=0},
 * which silently reinstates "whatever JDK maddi happens to run on" for each of them.
 *
 * <p>⭐ <b>SINCE 2026-08-21 THEY DO ARRIVE</b>, through {@link SourceFactsFile}: a file the producing
 * project's own configuration writes, published on the {@code maddiSourceElements} variant it already has.
 * That file records the three shapes that were rejected on the way — in particular an artifact a TASK
 * produces, which was written and then taken back out because it does not exist yet at the consumer's
 * configuration time.
 *
 * @param sourceRelease javac's {@code --release}, or {@code 0} when the build states none. ⛔ {@code 0} is
 *                      not a neutral default: it means the parse runs against whatever JDK maddi is on,
 *                      where every API removed since reads as "cannot find symbol".
 * @param addModules    JDK modules outside the default root set. ⛔ An INCUBATOR module is not in the
 *                      {@code java.se} closure, so without this every type in it is "package X is not
 *                      visible" and its compilation units are dropped.
 * @param warningFlags  the resolved warning policy — {@code -Xlint:*}, {@code -Werror}, {@code -nowarn}.
 *                      ⚠ Resolved, not declared: OpenSearch adds {@code -Werror} once at the root and six
 *                      compile tasks subtract it again, and only this list knows which.
 */
public record SourceFacts(int sourceRelease, List<String> addModules, List<String> warningFlags) {

    public SourceFacts {
        addModules = addModules == null ? List.of() : List.copyOf(addModules);
        warningFlags = warningFlags == null ? List.of() : List.copyOf(warningFlags);
    }
}
