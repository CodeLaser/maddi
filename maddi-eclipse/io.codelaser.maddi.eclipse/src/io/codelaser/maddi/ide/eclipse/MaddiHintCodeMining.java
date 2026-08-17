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

package org.e2immu.analyzer.ide.eclipse;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineContentCodeMining;

/**
 * One inline annotation label, drawn in the line's content at the declaration it belongs to — the closest
 * Eclipse equivalent of an IntelliJ inline inlay.
 * <p>
 * The label is known at construction (it is computed from an analysis result already in memory), so there is
 * no deferred work: {@code AbstractCodeMining.doResolve} defaults to a completed future, which is what we
 * want. Resolving lazily would only matter if the text were expensive to produce.
 */
public class MaddiHintCodeMining extends LineContentCodeMining {

    public MaddiHintCodeMining(Position position, ICodeMiningProvider provider, String label) {
        super(position, provider);
        setLabel(label);
    }
}
