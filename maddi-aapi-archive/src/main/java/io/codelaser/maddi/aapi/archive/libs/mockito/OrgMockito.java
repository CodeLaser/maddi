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

package io.codelaser.maddi.aapi.archive.libs.mockito;

import org.mockito.BDDMockito;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.mockito.verification.VerificationMode;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

/**
 * Mockito's verification calls (mockito-core 5.23), from the library-call census (TestLibraryCallCensus, 2026-10-12),
 * after weighing what "modified" should mean for a mock.
 * <p>
 * Verifying does change something inside the mock: Mockito marks the matched invocations as verified, which
 * {@code verifyNoMoreInteractions} and {@code InOrder} read later. Nothing the code can observe changes, though: the
 * mock answers exactly as before. That is hidden bookkeeping, which the analysis does not count elsewhere either;
 * stubbing with {@code when(..).thenReturn(..)} changes the mock's answers far more, and the mock is not even an
 * argument there. So:
 * <ul>
 * <li>{@code verify}, {@code InOrder.verify} and BDD's {@code then(..).should(..)} take the mock {@code @NotModified}
 *     and return it {@code @Independent}, method and parameter, as AssertJ's entry points do. The call made on
 *     what they return re-states an expected interaction and is not a mutation: without this,
 *     {@code verify(list).add("x")} made {@code list} modified, and through the reachability pass whatever holds it.</li>
 * <li>{@code verifyNoMoreInteractions} and {@code verifyNoInteractions} read their mocks.</li>
 * <li>{@code reset} changes what a mock does (its stubbing is gone) and is {@code @Modified}.</li>
 * </ul>
 * {@code clearInvocations} is left at the default.
 * <p>
 * Stubbing (2026-10-13, the census leftovers; {@code org.mockito.stubbing} is in OrgMockitoStubbing): the values and
 * throwables a stub is given are stored and handed out later, never changed by the call, so they are
 * {@code @NotModified}; {@code inOrder} reads its mocks. An {@code Answer} is the callback the mock runs: it is not
 * a {@code @FunctionalInterface}, so the functional-type rule misses it, and it gets {@code @IgnoreModifications}
 * explicitly. Names types in full and imports none, like libs/assertj.
 */
public class OrgMockito {
    public static final String PACKAGE_NAME = "org.mockito";

    //public class org.mockito.Mockito extends ArgumentMatchers
    class Mockito$ {
        @Independent
        static <T> T verify(@Independent @NotModified T mock) { return null; }
        @Independent
        static <T> T verify(@Independent @NotModified T mock, @NotModified VerificationMode mode) { return null; }
        static void verifyNoMoreInteractions(@NotModified Object... mocks) { }
        static void verifyNoInteractions(@NotModified Object... mocks) { }
        @SafeVarargs
        static <T> void reset(@Modified T... mocks) { }

        static <T> T mock(Class<T> classToMock, @IgnoreModifications Answer defaultAnswer) { return null; }
        @SafeVarargs
        static <T> T mock(@IgnoreModifications Answer defaultAnswer, T... reified) { return null; }
        static Stubber doThrow(@NotModified Throwable... toBeThrown) { return null; }
        static Stubber doAnswer(@IgnoreModifications Answer answer) { return null; }
        static Stubber doReturn(Object toBeReturned, @NotModified Object... toBeReturnedNext) { return null; }
        static InOrder inOrder(@NotModified Object... mocks) { return null; }
    }

    //public interface org.mockito.InOrder
    class InOrder$ {
        @Independent
        <T> T verify(@Independent @NotModified T mock) { return null; }
        @Independent
        <T> T verify(@Independent @NotModified T mock, @NotModified VerificationMode mode) { return null; }
    }

    //public class org.mockito.BDDMockito extends Mockito
    class BDDMockito$ {
        @Independent
        static <T> BDDMockito.Then<T> then(@Independent @NotModified T mock) { return null; }

        static BDDMockito.BDDStubber willThrow(@NotModified Throwable... toBeThrown) { return null; }
        static BDDMockito.BDDStubber willAnswer(@IgnoreModifications Answer<?> answer) { return null; }
        static BDDMockito.BDDStubber will(@IgnoreModifications Answer<?> answer) { return null; }
        static BDDMockito.BDDStubber willReturn(Object toBeReturned, @NotModified Object... toBeReturnedNext) { return null; }

        //public static interface BDDMyOngoingStubbing<T>
        class BDDMyOngoingStubbing<T> {
            BDDMockito.BDDMyOngoingStubbing<T> willAnswer(@IgnoreModifications Answer<?> answer) { return null; }
            BDDMockito.BDDMyOngoingStubbing<T> will(@IgnoreModifications Answer<?> answer) { return null; }
            BDDMockito.BDDMyOngoingStubbing<T> willReturn(T value, @NotModified T... values) { return null; }
            BDDMockito.BDDMyOngoingStubbing<T> willThrow(@NotModified Throwable... throwables) { return null; }
        }

        //public static interface BDDStubber
        class BDDStubber {
            BDDMockito.BDDStubber willAnswer(@IgnoreModifications Answer<?> answer) { return null; }
            BDDMockito.BDDStubber will(@IgnoreModifications Answer<?> answer) { return null; }
            BDDMockito.BDDStubber willReturn(Object toBeReturned, @NotModified Object... toBeReturnedNext) { return null; }
            BDDMockito.BDDStubber willThrow(@NotModified Throwable... toBeThrown) { return null; }
        }

        //public static interface Then<T>
        class Then<T> {
            @Independent
            T should() { return null; }
            @Independent
            T should(@NotModified VerificationMode mode) { return null; }
            @Independent
            T should(@NotModified InOrder inOrder) { return null; }
            @Independent
            T should(@NotModified InOrder inOrder, @NotModified VerificationMode mode) { return null; }
        }
    }
}
