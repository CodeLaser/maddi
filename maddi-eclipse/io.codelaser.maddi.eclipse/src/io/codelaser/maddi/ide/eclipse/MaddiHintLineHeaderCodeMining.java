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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;

/**
 * The annotations of one declaration, drawn on their own line just above it — the presentation JDT itself
 * uses for its references/implementations minings.
 * <p>
 * Deliberately not a {@link MaddiHintCodeMining} (line content). A line-content mining has to borrow a real
 * character's glyph box to make room for its label, which clips that character and leaves the platform to
 * repaint it by hand; on the paint where the glyph metrics change it returns before doing so, and the
 * character is left showing the mining's own styling. That is Eclipse bug 531769, marked FIXME in
 * {@code InlinedAnnotationDrawingStrategy}, and not fixable from a plugin. A line-header mining has no host
 * character at all, so the question never arises — and a full annotation list reads better above a
 * declaration than wedged in front of it.
 */
public class MaddiHintLineHeaderCodeMining extends LineHeaderCodeMining {

    public MaddiHintLineHeaderCodeMining(int beforeLineNumber, IDocument document, ICodeMiningProvider provider,
                                         String label) throws BadLocationException {
        super(beforeLineNumber, document, provider);
        setLabel(label);
    }
}
