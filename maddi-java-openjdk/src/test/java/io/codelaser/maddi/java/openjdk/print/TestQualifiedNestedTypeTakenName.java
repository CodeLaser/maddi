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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * CodeLaser/maddi#114. In a unit whose own type is {@code s.Try}, the import computer reserves {@code Try} and
 * refuses to import {@code r.Try} (fully qualified). A nested {@code r.Try.Data} was still printed from its primary
 * type, {@code Try.Data}, which then means {@code s.Try.Data}: "cannot find symbol". The decision, driven the way
 * ImportComputerImpl drives it.
 */
public class TestQualifiedNestedTypeTakenName extends CommonTest {

    @DisplayName("a nested type whose primary type's name is taken prints fully qualified")
    @Test
    public void test() {
        TypeInfo rTry = scan("r.Try", """
                package r;
                public class Try {
                    public interface Data { }
                }
                """);
        TypeInfo rTryData = rTry.findSubType("Data");
        QualificationImpl qualification = new QualificationImpl(false,
                TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, null);
        qualification.reserveSimpleNameAgainstImport("Try"); // the unit's own s.Try
        assertFalse(qualification.addTypeReturnImport(rTry));
        assertEquals(TypeNameImpl.Required.FQN, qualification.qualifierRequired(rTry));
        assertEquals(TypeNameImpl.Required.FQN, qualification.qualifierRequired(rTryData));

        // control: where 'Try' is free, the nested type is still printed from its imported primary type
        QualificationImpl free = new QualificationImpl(false, TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, null);
        free.addTypeReturnImport(rTry);
        assertEquals(TypeNameImpl.Required.SIMPLE, free.qualifierRequired(rTry));
        assertEquals(TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, free.qualifierRequired(rTryData));
    }
}
