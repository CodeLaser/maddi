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

package org.e2immu.analyzer.aapi.archive.jdk;

import org.e2immu.annotation.Commutable;
import org.e2immu.annotation.Modified;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionListener;
import java.util.Collection;

public class JavaxSwing {
    public static final String PACKAGE_NAME = "javax.swing";

    interface AbstractButton$ {
        @Modified
        void addActionListener(ActionListener l);
    }

    interface JTable$ {
        @Commutable(seq="class,0")
        @Modified
        void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer);

        @Commutable
        @Modified
        void setFillsViewportHeight(boolean fillsViewportHeight);

        @Commutable
        @Modified
        void setRowSelectionAllowed(boolean rowSelectionAllowed);
    }

    interface JComboBox$ {
        @Modified
        void addActionListener(ActionListener l);
    }

    interface JLabel$ {
        @Modified
        void setText(String text);
    }

    interface DefaultComboBoxModel$<E> {
        @Modified
        void removeAllElements();

        @Modified
        void addAll(int index, Collection<? extends E> c);
    }
}
