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

package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.Commutable;

import java.beans.PropertyChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class JavaxSwingTable {
    public static final String PACKAGE_NAME = "javax.swing.table";
    //public class TableColumn implements Serializable
    class TableColumn$ {
        static final String COLUMN_WIDTH_PROPERTY = null;
        static final String HEADER_VALUE_PROPERTY = null;
        static final String HEADER_RENDERER_PROPERTY = null;
        static final String CELL_RENDERER_PROPERTY = null;
        TableColumn$() { }
        TableColumn$(int modelIndex) { }
        TableColumn$(int modelIndex, int width) { }
        TableColumn$(int modelIndex, int width, TableCellRenderer cellRenderer, TableCellEditor cellEditor) { }
        void setModelIndex(int modelIndex) { }
        int getModelIndex() { return 0; }
        void setIdentifier(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object identifier) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getIdentifier() { return null; }
        void setHeaderValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object headerValue) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getHeaderValue() { return null; }
        void setHeaderRenderer(TableCellRenderer headerRenderer) { }
        TableCellRenderer getHeaderRenderer() { return null; }
        void setCellRenderer(TableCellRenderer cellRenderer) { }
        TableCellRenderer getCellRenderer() { return null; }
        void setCellEditor(TableCellEditor cellEditor) { }
        TableCellEditor getCellEditor() { return null; }
        void setWidth(int width) { }
        int getWidth() { return 0; }
        @Commutable
        void setPreferredWidth(int preferredWidth) { }
        int getPreferredWidth() { return 0; }
        void setMinWidth(int minWidth) { }
        int getMinWidth() { return 0; }
        void setMaxWidth(int maxWidth) { }
        int getMaxWidth() { return 0; }
        void setResizable(boolean isResizable) { }
        boolean getResizable() { return false; }
        void sizeWidthToFit() { }
        void disableResizedPosting() { }
        void enableResizedPosting() { }
        void addPropertyChangeListener(PropertyChangeListener listener) { }
        void removePropertyChangeListener(PropertyChangeListener listener) { }
        PropertyChangeListener [] getPropertyChangeListeners() { return null; }
    }
}
