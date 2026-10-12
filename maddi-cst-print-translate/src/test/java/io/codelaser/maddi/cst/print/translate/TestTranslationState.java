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
package io.codelaser.maddi.cst.print.translate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTranslationState {

    enum TestCode implements PrintMessage.Code {
        LOST(PrintMessage.Severity.LOSS),
        SAID_OTHERWISE(PrintMessage.Severity.INFO);

        private final PrintMessage.Severity severity;

        TestCode(PrintMessage.Severity severity) {
            this.severity = severity;
        }

        @Override
        public PrintMessage.Severity severity() {
            return severity;
        }
    }

    @Test
    public void messageText() {
        assertEquals("LOSS LOST a.B:3:7 x.y()", new PrintMessage<>(TestCode.LOST, "a.B", 3, 7, "x.y()").toString());
        assertEquals("INFO SAID_OTHERWISE a.B:3", new PrintMessage<>(TestCode.SAID_OTHERWISE, "a.B", 3, -1, "").toString());
        assertEquals("INFO SAID_OTHERWISE a.B", new PrintMessage<>(TestCode.SAID_OTHERWISE, "a.B", -1, -1, null).toString());
    }

    /** Outside a type, a message has no type and no position; the same message twice is recorded once. */
    @Test
    public void messagesAreTakenOnce() {
        TranslationState<TestCode> state = new TranslationState<>();
        state.message(TestCode.LOST, null, "d");
        state.message(TestCode.LOST, null, "d");
        state.message(TestCode.SAID_OTHERWISE, null, "e");
        List<PrintMessage<TestCode>> taken = state.takeMessages();
        assertEquals("[LOSS LOST ? d, INFO SAID_OTHERWISE ? e]", taken.toString());
        assertTrue(state.takeMessages().isEmpty());
        assertTrue(state.messages().isEmpty());
    }

    @Test
    public void emptyStacks() {
        TranslationState<TestCode> state = new TranslationState<>();
        assertNull(state.currentType());
        assertNull(state.currentMethod());
        assertNull(state.currentStatement());
        assertNull(state.currentCall());
        assertNull(state.enclosingCall());
        assertTrue(state.takeReferenced().isEmpty());
        assertEquals("", TranslationState.describe(null));
    }
}
