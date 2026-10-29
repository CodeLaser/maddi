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
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Commutable;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.Printable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EventObject;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.table.*;

public class JavaxSwing {
    public static final String PACKAGE_NAME = "javax.swing";
    //public abstract class AbstractButton extends JComponent implements ItemSelectable, SwingConstants
    class AbstractButton$ {
        static final String MODEL_CHANGED_PROPERTY = null;
        static final String TEXT_CHANGED_PROPERTY = null;
        static final String MNEMONIC_CHANGED_PROPERTY = null;
        static final String MARGIN_CHANGED_PROPERTY = null;
        static final String VERTICAL_ALIGNMENT_CHANGED_PROPERTY = null;
        static final String HORIZONTAL_ALIGNMENT_CHANGED_PROPERTY = null;
        static final String VERTICAL_TEXT_POSITION_CHANGED_PROPERTY = null;
        static final String HORIZONTAL_TEXT_POSITION_CHANGED_PROPERTY = null;
        static final String BORDER_PAINTED_CHANGED_PROPERTY = null;
        static final String FOCUS_PAINTED_CHANGED_PROPERTY = null;
        static final String ROLLOVER_ENABLED_CHANGED_PROPERTY = null;
        static final String CONTENT_AREA_FILLED_CHANGED_PROPERTY = null;
        static final String ICON_CHANGED_PROPERTY = null;
        static final String PRESSED_ICON_CHANGED_PROPERTY = null;
        static final String SELECTED_ICON_CHANGED_PROPERTY = null;
        static final String ROLLOVER_ICON_CHANGED_PROPERTY = null;
        static final String ROLLOVER_SELECTED_ICON_CHANGED_PROPERTY = null;
        static final String DISABLED_ICON_CHANGED_PROPERTY = null;
        static final String DISABLED_SELECTED_ICON_CHANGED_PROPERTY = null;
        @Modified void setHideActionText(boolean hideActionText) { }
        @NotModified boolean getHideActionText() { return false; }
        @NotModified String getText() { return null; }
        @Modified void setText(String text) { }
        @NotModified boolean isSelected() { return false; }
        @Modified void setSelected(boolean b) { }
        void doClick() { }
        void doClick(int pressTime) { }
        @Modified void setMargin(Insets m) { }
        @NotModified Insets getMargin() { return null; }
        @NotModified Icon getIcon() { return null; }
        @Modified void setIcon(Icon defaultIcon) { }
        @NotModified Icon getPressedIcon() { return null; }
        @Modified void setPressedIcon(Icon pressedIcon) { }
        @NotModified Icon getSelectedIcon() { return null; }
        @Modified void setSelectedIcon(Icon selectedIcon) { }
        @NotModified Icon getRolloverIcon() { return null; }
        @Modified void setRolloverIcon(Icon rolloverIcon) { }
        @NotModified Icon getRolloverSelectedIcon() { return null; }
        @Modified void setRolloverSelectedIcon(Icon rolloverSelectedIcon) { }
        @NotModified Icon getDisabledIcon() { return null; }
        @Modified void setDisabledIcon(Icon disabledIcon) { }
        @NotModified Icon getDisabledSelectedIcon() { return null; }
        @Modified void setDisabledSelectedIcon(Icon disabledSelectedIcon) { }
        @NotModified int getVerticalAlignment() { return 0; }
        @Modified void setVerticalAlignment(int alignment) { }
        @NotModified int getHorizontalAlignment() { return 0; }
        @Modified void setHorizontalAlignment(int alignment) { }
        @NotModified int getVerticalTextPosition() { return 0; }
        @Modified void setVerticalTextPosition(int textPosition) { }
        @NotModified int getHorizontalTextPosition() { return 0; }
        @Modified void setHorizontalTextPosition(int textPosition) { }
        @NotModified int getIconTextGap() { return 0; }
        @Modified void setIconTextGap(int iconTextGap) { }
        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        @Modified void removeNotify() { }
        @Modified void setActionCommand(String actionCommand) { }
        @NotModified String getActionCommand() { return null; }
        @Modified void setAction(Action a) { }
        @NotModified Action getAction() { return null; }
        @NotModified boolean isBorderPainted() { return false; }
        @Modified void setBorderPainted(boolean b) { }
        @NotModified boolean isFocusPainted() { return false; }
        @Modified void setFocusPainted(boolean b) { }
        @NotModified boolean isContentAreaFilled() { return false; }
        @Modified void setContentAreaFilled(boolean b) { }
        @NotModified boolean isRolloverEnabled() { return false; }
        @Modified void setRolloverEnabled(boolean b) { }
        @NotModified int getMnemonic() { return 0; }
        @Modified void setMnemonic(int mnemonic) { }
        @Modified void setMnemonic(char mnemonic) { }
        @Modified void setDisplayedMnemonicIndex(int index) { }
        @NotModified int getDisplayedMnemonicIndex() { return 0; }
        @Modified void setMultiClickThreshhold(long threshold) { }
        @NotModified long getMultiClickThreshhold() { return 0L; }
        @NotModified ButtonModel getModel() { return null; }
        @Modified void setModel(ButtonModel newModel) { }
        //override from javax.swing.JComponent
        @NotModified ButtonUI getUI() { return null; }
        @Modified void setUI(ButtonUI ui) { }
        //override from javax.swing.JComponent
        @Modified void updateUI() { }

        //override from java.awt.Container
        @Modified void setLayout(LayoutManager mgr) { }
        @Modified void addChangeListener(ChangeListener l) { }
        @Modified void removeChangeListener(ChangeListener l) { }
        @NotModified ChangeListener [] getChangeListeners() { return null; }
        @Modified void addActionListener(ActionListener l) { }
        @Modified void removeActionListener(ActionListener l) { }
        @NotModified ActionListener [] getActionListeners() { return null; }
        //override from java.awt.Component, javax.swing.JComponent
        @Modified void setEnabled(boolean b) { }
        @NotModified String getLabel() { return null; }
        @Modified void setLabel(String label) { }
        //override from java.awt.ItemSelectable
        @Modified void addItemListener(ItemListener l) { }

        //override from java.awt.ItemSelectable
        @Modified void removeItemListener(ItemListener l) { }
        @NotModified ItemListener [] getItemListeners() { return null; }
        //override from java.awt.ItemSelectable
        @NotModified Object [] getSelectedObjects() { return null; }

        //override from java.awt.Component, java.awt.image.ImageObserver
        boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) { return false; }
    }

    //public class DefaultComboBoxModel extends AbstractListModel<E> implements MutableComboBoxModel<E>, Serializable
    class DefaultComboBoxModel$<E> {
        DefaultComboBoxModel$() { }
        DefaultComboBoxModel$(E [] items) { }
        DefaultComboBoxModel$(Vector<E> v) { }
        //override from javax.swing.ComboBoxModel
        @Modified void setSelectedItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object anObject) { }

        //override from javax.swing.ComboBoxModel
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        @NotModified Object getSelectedItem() { return null; }

        //override from javax.swing.ListModel
        @NotModified int getSize() { return 0; }

        //override from javax.swing.ListModel
        //@Independent(hc=true)[H]
        @NotModified E getElementAt(int index) { return null; }

        @NotModified int getIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { return 0; }

        //override from javax.swing.MutableComboBoxModel
        @Modified void addElement(/*@Independent(hc=true)[H] @NotModified[T]*/ E anObject) { }

        //override from javax.swing.MutableComboBoxModel
        @Modified void insertElementAt(/*@Independent(hc=true)[H] @NotModified[T]*/ E anObject, int index) { }

        //override from javax.swing.MutableComboBoxModel
        @Modified void removeElementAt(int index) { }

        //override from javax.swing.MutableComboBoxModel
        @Modified void removeElement(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object anObject) { }
        @Modified void removeAllElements() { }
        @Modified void addAll(@NotModified Collection<? extends E> c) { }
        @Modified void addAll(int index, @NotModified Collection<? extends E> c) { }
    }

    //public class JComboBox extends JComponent implements ItemSelectable, ListDataListener, ActionListener, Accessible
    class JComboBox$<E> {
        //public interface KeySelectionManager
        class KeySelectionManager {int selectionForKey(char c, ComboBoxModel<?> comboBoxModel) { return 0; } }
        JComboBox$(ComboBoxModel<E> aModel) { }
        JComboBox$(E [] items) { }
        JComboBox$(Vector<E> items) { }
        JComboBox$() { }
        @Modified void setUI(ComboBoxUI ui) { }
        //override from javax.swing.JComponent
        @Modified void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }

        //override from javax.swing.JComponent
        @NotModified ComboBoxUI getUI() { return null; }
        @Modified void setModel(ComboBoxModel<E> aModel) { }
        @NotModified ComboBoxModel<E> getModel() { return null; }
        @Modified void setLightWeightPopupEnabled(boolean aFlag) { }
        @NotModified boolean isLightWeightPopupEnabled() { return false; }
        @Modified void setEditable(boolean aFlag) { }
        @NotModified boolean isEditable() { return false; }
        @Modified void setMaximumRowCount(int count) { }
        @NotModified int getMaximumRowCount() { return 0; }
        @Modified void setRenderer(ListCellRenderer<? super E> aRenderer) { }
        @NotModified ListCellRenderer<? super E> getRenderer() { return null; }
        @Modified void setEditor(ComboBoxEditor anEditor) { }
        @NotModified ComboBoxEditor getEditor() { return null; }
        @Modified void setSelectedItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getSelectedItem() { return null; }
        @Modified void setSelectedIndex(int anIndex) { }
        @NotModified int getSelectedIndex() { return 0; }
        //@Independent(hc=true)[T]
        @NotModified E getPrototypeDisplayValue() { return null; }
        @Modified void setPrototypeDisplayValue(/*@Independent(hc=true)[T] @NotModified[T]*/ E prototypeDisplayValue) { }
        @Modified void addItem(/*@Independent(hc=true)[T] @NotModified[T]*/ E item) { }
        @Modified void insertItemAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E item, int index) { }
        @Modified void removeItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        @Modified void removeItemAt(int anIndex) { }
        @Modified void removeAllItems() { }
        void showPopup() { }
        void hidePopup() { }
        @Modified void setPopupVisible(boolean v) { }
        @NotModified boolean isPopupVisible() { return false; }
        //override from java.awt.ItemSelectable
        @Modified void addItemListener(ItemListener aListener) { }

        //override from java.awt.ItemSelectable
        @Modified void removeItemListener(ItemListener aListener) { }
        @NotModified ItemListener [] getItemListeners() { return null; }
        @Modified void addActionListener(ActionListener l) { }
        @Modified void removeActionListener(ActionListener l) { }
        @NotModified ActionListener [] getActionListeners() { return null; }
        @Modified void addPopupMenuListener(PopupMenuListener l) { }
        @Modified void removePopupMenuListener(PopupMenuListener l) { }
        @NotModified PopupMenuListener [] getPopupMenuListeners() { return null; }
        void firePopupMenuWillBecomeVisible() { }
        void firePopupMenuWillBecomeInvisible() { }
        void firePopupMenuCanceled() { }
        @Modified void setActionCommand(String aCommand) { }
        @NotModified String getActionCommand() { return null; }
        @Modified void setAction(Action a) { }
        @NotModified Action getAction() { return null; }
        //override from java.awt.ItemSelectable
        @NotModified Object [] getSelectedObjects() { return null; }

        //override from java.awt.event.ActionListener
        void actionPerformed(ActionEvent e) { }

        //override from javax.swing.event.ListDataListener
        void contentsChanged(ListDataEvent e) { }

        //override from javax.swing.event.ListDataListener
        void intervalAdded(ListDataEvent e) { }

        //override from javax.swing.event.ListDataListener
        void intervalRemoved(ListDataEvent e) { }
        boolean selectWithKeyChar(char keyChar) { return false; }
        //override from java.awt.Component, javax.swing.JComponent
        @Modified void setEnabled(boolean b) { }

        void configureEditor(
            ComboBoxEditor anEditor,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anItem) { }

        //override from java.awt.Component, javax.swing.JComponent
        void processKeyEvent(KeyEvent e) { }
        @Modified void setKeySelectionManager(JComboBox.KeySelectionManager aManager) { }
        @NotModified JComboBox.KeySelectionManager getKeySelectionManager() { return null; }
        @NotModified int getItemCount() { return 0; }
        //@Independent(hc=true)[T]
        @NotModified E getItemAt(int index) { return null; }

        //override from java.awt.Component, javax.accessibility.Accessible
        @NotModified AccessibleContext getAccessibleContext() { return null; }
    }

    //public class JLabel extends JComponent implements SwingConstants, Accessible
    class JLabel$ {
        JLabel$(String text, Icon icon, int horizontalAlignment) { }
        JLabel$(String text, int horizontalAlignment) { }
        JLabel$(String text) { }
        JLabel$(Icon image, int horizontalAlignment) { }
        JLabel$(Icon image) { }
        JLabel$() { }
        //override from javax.swing.JComponent
        @NotModified LabelUI getUI() { return null; }
        @Modified void setUI(LabelUI ui) { }
        //override from javax.swing.JComponent
        @Modified void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }
        @NotModified String getText() { return null; }
        @Modified void setText(String text) { }
        @NotModified Icon getIcon() { return null; }
        @Modified void setIcon(Icon icon) { }
        @NotModified Icon getDisabledIcon() { return null; }
        @Modified void setDisabledIcon(Icon disabledIcon) { }
        @Modified void setDisplayedMnemonic(int key) { }
        @Modified void setDisplayedMnemonic(char aChar) { }
        @NotModified int getDisplayedMnemonic() { return 0; }
        @Modified void setDisplayedMnemonicIndex(int index) { }
        @NotModified int getDisplayedMnemonicIndex() { return 0; }
        @NotModified int getIconTextGap() { return 0; }
        @Modified void setIconTextGap(int iconTextGap) { }
        @NotModified int getVerticalAlignment() { return 0; }
        @Modified void setVerticalAlignment(int alignment) { }
        @NotModified int getHorizontalAlignment() { return 0; }
        @Modified void setHorizontalAlignment(int alignment) { }
        @NotModified int getVerticalTextPosition() { return 0; }
        @Modified void setVerticalTextPosition(int textPosition) { }
        @NotModified int getHorizontalTextPosition() { return 0; }
        @Modified void setHorizontalTextPosition(int textPosition) { }
        //override from java.awt.Component, java.awt.image.ImageObserver
        boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) { return false; }
        @NotModified Component getLabelFor() { return null; }
        @Modified void setLabelFor(Component c) { }
        //override from java.awt.Component, javax.accessibility.Accessible
        @NotModified AccessibleContext getAccessibleContext() { return null; }
    }

    //public class JTable extends JComponent implements TableModelListener, Scrollable, TableColumnModelListener, ListSelectionListener, CellEditorListener, Accessible, RowSorterListener
    class JTable$ {
        static final int AUTO_RESIZE_OFF = 0;
        static final int AUTO_RESIZE_NEXT_COLUMN = 0;
        static final int AUTO_RESIZE_SUBSEQUENT_COLUMNS = 0;
        static final int AUTO_RESIZE_LAST_COLUMN = 0;
        static final int AUTO_RESIZE_ALL_COLUMNS = 0;
        //public static final class DropLocation extends DropLocation
        class DropLocation {
            @NotModified int getRow() { return 0; }
            @NotModified int getColumn() { return 0; }
            @NotModified boolean isInsertRow() { return false; }
            @NotModified boolean isInsertColumn() { return false; }
            //override from java.lang.Object, javax.swing.TransferHandler.DropLocation
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }

        //public enum PrintMode extends Enum<PrintMode>
        class PrintMode {
            //@NotNull[O]
            static final JTable.PrintMode NORMAL = null;

            //@NotNull[O]
            static final JTable.PrintMode FIT_WIDTH = null;
            static JTable.PrintMode [] values() { return null; }
            static JTable.PrintMode valueOf(String name) { return null; }
        }
        JTable$() { }
        JTable$(TableModel dm) { }
        JTable$(TableModel dm, TableColumnModel cm) { }
        JTable$(TableModel dm, TableColumnModel cm, ListSelectionModel sm) { }
        JTable$(int numRows, int numColumns) { }
        JTable$(Vector<? extends Vector> rowData, Vector<?> columnNames) { }
        JTable$(Object [][] rowData, Object [] columnNames) { }
        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        @Modified void addNotify() { }

        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        @Modified void removeNotify() { }

        //@Independent[T]
        static JScrollPane createScrollPaneForTable(JTable aTable) { return null; }
        @Modified void setTableHeader(JTableHeader tableHeader) { }
        @NotModified JTableHeader getTableHeader() { return null; }
        @Modified void setRowHeight(int rowHeight) { }
        @NotModified int getRowHeight() { return 0; }
        @Modified void setRowHeight(int row, int rowHeight) { }
        @NotModified int getRowHeight(int row) { return 0; }
        @Modified void setRowMargin(int rowMargin) { }
        @NotModified int getRowMargin() { return 0; }
        @Modified void setIntercellSpacing(Dimension intercellSpacing) { }
        @NotModified Dimension getIntercellSpacing() { return null; }
        @Modified void setGridColor(Color gridColor) { }
        @NotModified Color getGridColor() { return null; }
        @Modified void setShowGrid(boolean showGrid) { }
        @Modified void setShowHorizontalLines(boolean showHorizontalLines) { }
        @Modified void setShowVerticalLines(boolean showVerticalLines) { }
        @NotModified boolean getShowHorizontalLines() { return false; }
        @NotModified boolean getShowVerticalLines() { return false; }
        @Modified void setAutoResizeMode(int mode) { }
        @NotModified int getAutoResizeMode() { return 0; }
        @Modified void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) { }
        @NotModified boolean getAutoCreateColumnsFromModel() { return false; }
        void createDefaultColumnsFromModel() { }
        @Modified @Commutable(seq="class,0")
        void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) { }
        @NotModified TableCellRenderer getDefaultRenderer(Class<?> columnClass) { return null; }
        @Modified void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) { }
        @NotModified TableCellEditor getDefaultEditor(Class<?> columnClass) { return null; }
        @Modified void setDragEnabled(boolean b) { }
        @NotModified boolean getDragEnabled() { return false; }
        @Modified void setDropMode(DropMode dropMode) { }
        @NotModified DropMode getDropMode() { return null; }
        @NotModified JTable.DropLocation getDropLocation() { return null; }
        @Modified void setAutoCreateRowSorter(boolean autoCreateRowSorter) { }
        @NotModified boolean getAutoCreateRowSorter() { return false; }
        @Modified void setUpdateSelectionOnSort(boolean update) { }
        @NotModified boolean getUpdateSelectionOnSort() { return false; }
        @Modified void setRowSorter(RowSorter<? extends TableModel> sorter) { }
        @NotModified RowSorter<? extends TableModel> getRowSorter() { return null; }
        @Modified void setSelectionMode(int selectionMode) { }
        @Modified @Commutable
        void setRowSelectionAllowed(boolean rowSelectionAllowed) { }
        @NotModified boolean getRowSelectionAllowed() { return false; }
        @Modified void setColumnSelectionAllowed(boolean columnSelectionAllowed) { }
        @NotModified boolean getColumnSelectionAllowed() { return false; }
        @Modified void setCellSelectionEnabled(boolean cellSelectionEnabled) { }
        @NotModified boolean getCellSelectionEnabled() { return false; }
        void selectAll() { }
        @Modified void clearSelection() { }
        @Modified void setRowSelectionInterval(int index0, int index1) { }
        @Modified void setColumnSelectionInterval(int index0, int index1) { }
        @Modified void addRowSelectionInterval(int index0, int index1) { }
        @Modified void addColumnSelectionInterval(int index0, int index1) { }
        @Modified void removeRowSelectionInterval(int index0, int index1) { }
        @Modified void removeColumnSelectionInterval(int index0, int index1) { }
        @NotModified int getSelectedRow() { return 0; }
        @NotModified int getSelectedColumn() { return 0; }
        @NotModified int [] getSelectedRows() { return null; }
        @NotModified int [] getSelectedColumns() { return null; }
        @NotModified int getSelectedRowCount() { return 0; }
        @NotModified int getSelectedColumnCount() { return 0; }
        @NotModified boolean isRowSelected(int row) { return false; }
        @NotModified boolean isColumnSelected(int column) { return false; }
        @NotModified boolean isCellSelected(int row, int column) { return false; }
        void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) { }
        @NotModified Color getSelectionForeground() { return null; }
        @Modified void setSelectionForeground(Color selectionForeground) { }
        @NotModified Color getSelectionBackground() { return null; }
        @Modified void setSelectionBackground(Color selectionBackground) { }
        @NotModified TableColumn getColumn(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object identifier) {
            return null;
        }
        int convertColumnIndexToModel(int viewColumnIndex) { return 0; }
        int convertColumnIndexToView(int modelColumnIndex) { return 0; }
        int convertRowIndexToView(int modelRowIndex) { return 0; }
        int convertRowIndexToModel(int viewRowIndex) { return 0; }
        @NotModified int getRowCount() { return 0; }
        @NotModified int getColumnCount() { return 0; }
        @NotModified String getColumnName(int column) { return null; }
        @NotModified Class<?> getColumnClass(int column) { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getValueAt(int row, int column) { return null; }

        @Modified void setValueAt(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object aValue,
            int row,
            int column) { }
        @NotModified boolean isCellEditable(int row, int column) { return false; }
        @Modified void addColumn(TableColumn aColumn) { }
        @Modified void removeColumn(TableColumn aColumn) { }
        void moveColumn(int column, int targetColumn) { }
        int columnAtPoint(Point point) { return 0; }
        int rowAtPoint(Point point) { return 0; }
        @NotModified Rectangle getCellRect(int row, int column, boolean includeSpacing) { return null; }
        //override from java.awt.Component, java.awt.Container
        void doLayout() { }
        void sizeColumnsToFit(boolean lastColumnOnly) { }
        void sizeColumnsToFit(int resizingColumn) { }
        //override from javax.swing.JComponent
        @NotModified String getToolTipText(MouseEvent event) { return null; }
        @Modified void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) { }
        @NotModified boolean getSurrendersFocusOnKeystroke() { return false; }
        boolean editCellAt(int row, int column) { return false; }
        boolean editCellAt(int row, int column, EventObject e) { return false; }
        @NotModified boolean isEditing() { return false; }
        @NotModified Component getEditorComponent() { return null; }
        @NotModified int getEditingColumn() { return 0; }
        @NotModified int getEditingRow() { return 0; }
        //override from javax.swing.JComponent
        @NotModified TableUI getUI() { return null; }
        @Modified void setUI(TableUI ui) { }
        //override from javax.swing.JComponent
        @Modified void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }
        @Modified void setModel(TableModel dataModel) { }
        @NotModified TableModel getModel() { return null; }
        @Modified void setColumnModel(TableColumnModel columnModel) { }
        @NotModified TableColumnModel getColumnModel() { return null; }
        @Modified void setSelectionModel(ListSelectionModel selectionModel) { }
        @NotModified ListSelectionModel getSelectionModel() { return null; }
        //override from javax.swing.event.RowSorterListener
        void sorterChanged(RowSorterEvent e) { }

        //override from javax.swing.event.TableModelListener
        void tableChanged(TableModelEvent e) { }

        //override from javax.swing.event.TableColumnModelListener
        void columnAdded(TableColumnModelEvent e) { }

        //override from javax.swing.event.TableColumnModelListener
        void columnRemoved(TableColumnModelEvent e) { }

        //override from javax.swing.event.TableColumnModelListener
        void columnMoved(TableColumnModelEvent e) { }

        //override from javax.swing.event.TableColumnModelListener
        void columnMarginChanged(ChangeEvent e) { }

        //override from javax.swing.event.TableColumnModelListener
        void columnSelectionChanged(ListSelectionEvent e) { }

        //override from javax.swing.event.ListSelectionListener
        void valueChanged(ListSelectionEvent e) { }

        //override from javax.swing.event.CellEditorListener
        void editingStopped(ChangeEvent e) { }

        //override from javax.swing.event.CellEditorListener
        void editingCanceled(ChangeEvent e) { }
        @Modified void setPreferredScrollableViewportSize(Dimension size) { }
        //override from javax.swing.Scrollable
        @NotModified Dimension getPreferredScrollableViewportSize() { return null; }

        //override from javax.swing.Scrollable
        @NotModified int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }

        //override from javax.swing.Scrollable
        @NotModified int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }

        //override from javax.swing.Scrollable
        @NotModified boolean getScrollableTracksViewportWidth() { return false; }

        //override from javax.swing.Scrollable
        @NotModified boolean getScrollableTracksViewportHeight() { return false; }
        @Modified @Commutable
        void setFillsViewportHeight(boolean fillsViewportHeight) { }
        @NotModified boolean getFillsViewportHeight() { return false; }
        @NotModified TableCellEditor getCellEditor() { return null; }
        @Modified void setCellEditor(TableCellEditor anEditor) { }
        @Modified void setEditingColumn(int aColumn) { }
        @Modified void setEditingRow(int aRow) { }
        @NotModified TableCellRenderer getCellRenderer(int row, int column) { return null; }
        Component prepareRenderer(TableCellRenderer renderer, int row, int column) { return null; }
        @NotModified TableCellEditor getCellEditor(int row, int column) { return null; }
        Component prepareEditor(TableCellEditor editor, int row, int column) { return null; }
        @Modified void removeEditor() { }
        boolean print() { return false; }
        boolean print(JTable.PrintMode printMode) { return false; }
        boolean print(JTable.PrintMode printMode, MessageFormat headerFormat, MessageFormat footerFormat) { return false; }

        boolean print(
            JTable.PrintMode printMode,
            MessageFormat headerFormat,
            MessageFormat footerFormat,
            boolean showPrintDialog,
            PrintRequestAttributeSet attr,
            boolean interactive) { return false; }

        boolean print(
            JTable.PrintMode printMode,
            MessageFormat headerFormat,
            MessageFormat footerFormat,
            boolean showPrintDialog,
            PrintRequestAttributeSet attr,
            boolean interactive,
            PrintService service) { return false; }

        @NotModified Printable getPrintable(JTable.PrintMode printMode, MessageFormat headerFormat, MessageFormat footerFormat) {
            return null;
        }

        //override from java.awt.Component, javax.accessibility.Accessible
        @NotModified AccessibleContext getAccessibleContext() { return null; }
    }

    class AbstractListModel$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addListDataListener(javax.swing.event.ListDataListener p0) { }
        @Modified void removeListDataListener(javax.swing.event.ListDataListener p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified javax.swing.event.ListDataListener[] getListDataListeners() { return null; }
        @NotModified <T extends java.util.EventListener> T[] getListeners(Class<T> p0) { return null; }
    }

    class ComboBoxModel$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setSelectedItem(Object p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Object getSelectedItem() { return null; }
    }

    class JComponent$ {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addAncestorListener(javax.swing.event.AncestorListener p0) { }
        @Modified void addVetoableChangeListener(java.beans.VetoableChangeListener p0) { }
        @Modified void computeVisibleRect(java.awt.Rectangle p0) { }
        @Modified void putClientProperty(Object p0, Object p1) { }
        @Modified void registerKeyboardAction(java.awt.event.ActionListener p0, String p1, javax.swing.KeyStroke p2, int p3) { }
        @Modified void registerKeyboardAction(java.awt.event.ActionListener p0, javax.swing.KeyStroke p1, int p2) { }
        @Modified void removeAncestorListener(javax.swing.event.AncestorListener p0) { }
        @Modified void removeVetoableChangeListener(java.beans.VetoableChangeListener p0) { }
        @Modified void resetKeyboardActions() { }
        @Modified void setActionMap(javax.swing.ActionMap p0) { }
        @Modified void setAlignmentX(float p0) { }
        @Modified void setAlignmentY(float p0) { }
        @Modified void setAutoscrolls(boolean p0) { }
        @Modified void setBorder(javax.swing.border.Border p0) { }
        @Modified void setComponentPopupMenu(javax.swing.JPopupMenu p0) { }
        @Modified void setDebugGraphicsOptions(int p0) { }
        @Modified void setDoubleBuffered(boolean p0) { }
        @Modified void setInheritsPopupMenu(boolean p0) { }
        @Modified void setInputMap(int p0, javax.swing.InputMap p1) { }
        @Modified void setInputVerifier(javax.swing.InputVerifier p0) { }
        @Modified void setNextFocusableComponent(java.awt.Component p0) { }
        @Modified void setOpaque(boolean p0) { }
        @Modified void setRequestFocusEnabled(boolean p0) { }
        @Modified void setToolTipText(String p0) { }
        @Modified void setTransferHandler(javax.swing.TransferHandler p0) { }
        @Modified void setVerifyInputWhenFocusTarget(boolean p0) { }
        @Modified void unregisterKeyboardAction(javax.swing.KeyStroke p0) { }
        @Modified void updateUI() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean contains(int p0, int p1) { return false; }
        @NotModified java.awt.event.ActionListener getActionForKeyStroke(javax.swing.KeyStroke p0) { return null; }
        @NotModified javax.swing.ActionMap getActionMap() { return null; }
        @NotModified javax.swing.event.AncestorListener[] getAncestorListeners() { return null; }
        @NotModified boolean getAutoscrolls() { return false; }
        @NotModified javax.swing.border.Border getBorder() { return null; }
        @NotModified Object getClientProperty(Object p0) { return null; }
        @NotModified javax.swing.JPopupMenu getComponentPopupMenu() { return null; }
        @NotModified int getConditionForKeyStroke(javax.swing.KeyStroke p0) { return 0; }
        @NotModified int getDebugGraphicsOptions() { return 0; }
        @NotModified boolean getInheritsPopupMenu() { return false; }
        @NotModified javax.swing.InputMap getInputMap() { return null; }
        @NotModified javax.swing.InputMap getInputMap(int p0) { return null; }
        @NotModified javax.swing.InputVerifier getInputVerifier() { return null; }
        @NotModified java.awt.Insets getInsets(java.awt.Insets p0) { return null; }
        @NotModified java.awt.Component getNextFocusableComponent() { return null; }
        @NotModified java.awt.Point getPopupLocation(java.awt.event.MouseEvent p0) { return null; }
        @NotModified javax.swing.KeyStroke[] getRegisteredKeyStrokes() { return null; }
        @NotModified javax.swing.JRootPane getRootPane() { return null; }
        @NotModified java.awt.Point getToolTipLocation(java.awt.event.MouseEvent p0) { return null; }
        @NotModified String getToolTipText() { return null; }
        @NotModified String getToolTipText(java.awt.event.MouseEvent p0) { return null; }
        @NotModified java.awt.Container getTopLevelAncestor() { return null; }
        @NotModified javax.swing.TransferHandler getTransferHandler() { return null; }
        @NotModified javax.swing.plaf.ComponentUI getUI() { return null; }
        @NotModified String getUIClassID() { return null; }
        @NotModified boolean getVerifyInputWhenFocusTarget() { return false; }
        @NotModified java.beans.VetoableChangeListener[] getVetoableChangeListeners() { return null; }
        @NotModified java.awt.Rectangle getVisibleRect() { return null; }
        @NotModified boolean isManagingFocus() { return false; }
        @NotModified boolean isOptimizedDrawingEnabled() { return false; }
        @NotModified boolean isPaintingForPrint() { return false; }
        @NotModified boolean isPaintingTile() { return false; }
        @NotModified boolean isRequestFocusEnabled() { return false; }
    }

    class ListModel$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addListDataListener(javax.swing.event.ListDataListener p0) { }
        @Modified void removeListDataListener(javax.swing.event.ListDataListener p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified E getElementAt(int p0) { return null; }
        @NotModified int getSize() { return 0; }
    }

    class MutableComboBoxModel$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addElement(E p0) { }
        @Modified void insertElementAt(E p0, int p1) { }
        @Modified void removeElement(Object p0) { }
        @Modified void removeElementAt(int p0) { }
    }

    class Scrollable$ {
        // a query, declared so (generated from the assumed-defaults list)
        @NotModified java.awt.Dimension getPreferredScrollableViewportSize() { return null; }
        @NotModified int getScrollableBlockIncrement(java.awt.Rectangle p0, int p1, int p2) { return 0; }
        @NotModified boolean getScrollableTracksViewportHeight() { return false; }
        @NotModified boolean getScrollableTracksViewportWidth() { return false; }
        @NotModified int getScrollableUnitIncrement(java.awt.Rectangle p0, int p1, int p2) { return 0; }
    }
}
