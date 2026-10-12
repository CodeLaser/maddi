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

package io.codelaser.maddi.aapi.archive.libs.mockito;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;

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
 * {@code clearInvocations} is left at the default. Names types in full and imports none, like libs/assertj.
 */
public class OrgMockito {
    public static final String PACKAGE_NAME = "org.mockito";

    //public class org.mockito.Mockito extends ArgumentMatchers
    class Mockito$ {
        @Independent
        static <T> T verify(@Independent @NotModified T mock) { return null; }
        @Independent
        static <T> T verify(@Independent @NotModified T mock, @NotModified org.mockito.verification.VerificationMode mode) { return null; }
        static void verifyNoMoreInteractions(@NotModified java.lang.Object... mocks) { }
        static void verifyNoInteractions(@NotModified java.lang.Object... mocks) { }
        @SafeVarargs
        static <T> void reset(@Modified T... mocks) { }
    }

    //public interface org.mockito.InOrder
    class InOrder$ {
        @Independent
        <T> T verify(@Independent @NotModified T mock) { return null; }
        @Independent
        <T> T verify(@Independent @NotModified T mock, @NotModified org.mockito.verification.VerificationMode mode) { return null; }
    }

    //public class org.mockito.BDDMockito extends Mockito
    class BDDMockito$ {
        @Independent
        static <T> org.mockito.BDDMockito.Then<T> then(@Independent @NotModified T mock) { return null; }

        //public static interface Then<T>
        class Then<T> {
            @Independent
            T should() { return null; }
            @Independent
            T should(@NotModified org.mockito.verification.VerificationMode mode) { return null; }
            @Independent
            T should(@NotModified org.mockito.InOrder inOrder) { return null; }
            @Independent
            T should(@NotModified org.mockito.InOrder inOrder, @NotModified org.mockito.verification.VerificationMode mode) { return null; }
        }
    }
}
