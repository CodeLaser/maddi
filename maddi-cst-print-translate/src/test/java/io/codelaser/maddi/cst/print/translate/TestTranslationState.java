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
