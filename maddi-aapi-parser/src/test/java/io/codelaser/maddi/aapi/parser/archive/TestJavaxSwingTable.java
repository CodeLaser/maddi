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

package io.codelaser.maddi.aapi.parser.archive;

import io.codelaser.maddi.aapi.parser.CommonTest;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.COMMUTABLE_METHODS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaxSwingTable extends CommonTest {

    @Test
    public void setTableColumnSetPreferredWidth() {
        TypeInfo typeInfo = compiledTypesManager().type(TableColumn.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("setPreferredWidth", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    private void testCommutable(MethodInfo methodInfo) {
        Value.CommutableData cd = methodInfo.analysis().getOrNull(COMMUTABLE_METHODS, ValueImpl.CommutableData.class);
        assertTrue(cd.isBlankMultiParSeq());
    }
}
