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

package io.codelaser.maddi.aapi.archive.jdk;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Commutable;

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
        @NotModified int getModelIndex() { return 0; }
        void setIdentifier(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object identifier) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getIdentifier() { return null; }
        void setHeaderValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object headerValue) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getHeaderValue() { return null; }
        void setHeaderRenderer(TableCellRenderer headerRenderer) { }
        @NotModified TableCellRenderer getHeaderRenderer() { return null; }
        void setCellRenderer(TableCellRenderer cellRenderer) { }
        @NotModified TableCellRenderer getCellRenderer() { return null; }
        void setCellEditor(TableCellEditor cellEditor) { }
        @NotModified TableCellEditor getCellEditor() { return null; }
        void setWidth(int width) { }
        @NotModified int getWidth() { return 0; }
        @Commutable
        void setPreferredWidth(int preferredWidth) { }
        @NotModified int getPreferredWidth() { return 0; }
        void setMinWidth(int minWidth) { }
        @NotModified int getMinWidth() { return 0; }
        void setMaxWidth(int maxWidth) { }
        @NotModified int getMaxWidth() { return 0; }
        void setResizable(boolean isResizable) { }
        @NotModified boolean getResizable() { return false; }
        void sizeWidthToFit() { }
        void disableResizedPosting() { }
        void enableResizedPosting() { }
        void addPropertyChangeListener(PropertyChangeListener listener) { }
        void removePropertyChangeListener(PropertyChangeListener listener) { }
        @NotModified PropertyChangeListener [] getPropertyChangeListeners() { return null; }
    }
}
