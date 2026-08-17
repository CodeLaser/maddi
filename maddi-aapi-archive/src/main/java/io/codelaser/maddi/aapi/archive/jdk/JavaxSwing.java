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
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.Commutable;

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
        void setHideActionText(boolean hideActionText) { }
        @NotModified boolean getHideActionText() { return false; }
        @NotModified String getText() { return null; }
        void setText(String text) { }
        @NotModified boolean isSelected() { return false; }
        void setSelected(boolean b) { }
        void doClick() { }
        void doClick(int pressTime) { }
        void setMargin(Insets m) { }
        @NotModified Insets getMargin() { return null; }
        @NotModified Icon getIcon() { return null; }
        void setIcon(Icon defaultIcon) { }
        @NotModified Icon getPressedIcon() { return null; }
        void setPressedIcon(Icon pressedIcon) { }
        @NotModified Icon getSelectedIcon() { return null; }
        void setSelectedIcon(Icon selectedIcon) { }
        @NotModified Icon getRolloverIcon() { return null; }
        void setRolloverIcon(Icon rolloverIcon) { }
        @NotModified Icon getRolloverSelectedIcon() { return null; }
        void setRolloverSelectedIcon(Icon rolloverSelectedIcon) { }
        @NotModified Icon getDisabledIcon() { return null; }
        void setDisabledIcon(Icon disabledIcon) { }
        @NotModified Icon getDisabledSelectedIcon() { return null; }
        void setDisabledSelectedIcon(Icon disabledSelectedIcon) { }
        @NotModified int getVerticalAlignment() { return 0; }
        void setVerticalAlignment(int alignment) { }
        @NotModified int getHorizontalAlignment() { return 0; }
        void setHorizontalAlignment(int alignment) { }
        @NotModified int getVerticalTextPosition() { return 0; }
        void setVerticalTextPosition(int textPosition) { }
        @NotModified int getHorizontalTextPosition() { return 0; }
        void setHorizontalTextPosition(int textPosition) { }
        @NotModified int getIconTextGap() { return 0; }
        void setIconTextGap(int iconTextGap) { }
        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        void removeNotify() { }
        void setActionCommand(String actionCommand) { }
        @NotModified String getActionCommand() { return null; }
        void setAction(Action a) { }
        @NotModified Action getAction() { return null; }
        @NotModified boolean isBorderPainted() { return false; }
        void setBorderPainted(boolean b) { }
        @NotModified boolean isFocusPainted() { return false; }
        void setFocusPainted(boolean b) { }
        @NotModified boolean isContentAreaFilled() { return false; }
        void setContentAreaFilled(boolean b) { }
        @NotModified boolean isRolloverEnabled() { return false; }
        void setRolloverEnabled(boolean b) { }
        @NotModified int getMnemonic() { return 0; }
        void setMnemonic(int mnemonic) { }
        void setMnemonic(char mnemonic) { }
        void setDisplayedMnemonicIndex(int index) { }
        @NotModified int getDisplayedMnemonicIndex() { return 0; }
        void setMultiClickThreshhold(long threshold) { }
        @NotModified long getMultiClickThreshhold() { return 0L; }
        @NotModified ButtonModel getModel() { return null; }
        void setModel(ButtonModel newModel) { }
        //override from javax.swing.JComponent
        @NotModified ButtonUI getUI() { return null; }
        void setUI(ButtonUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from java.awt.Container
        void setLayout(LayoutManager mgr) { }
        void addChangeListener(ChangeListener l) { }
        void removeChangeListener(ChangeListener l) { }
        @NotModified ChangeListener [] getChangeListeners() { return null; }
        void addActionListener(ActionListener l) { }
        void removeActionListener(ActionListener l) { }
        @NotModified ActionListener [] getActionListeners() { return null; }
        //override from java.awt.Component, javax.swing.JComponent
        void setEnabled(boolean b) { }
        @NotModified String getLabel() { return null; }
        void setLabel(String label) { }
        //override from java.awt.ItemSelectable
        void addItemListener(ItemListener l) { }

        //override from java.awt.ItemSelectable
        void removeItemListener(ItemListener l) { }
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
        void setSelectedItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object anObject) { }

        //override from javax.swing.ComboBoxModel
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        @NotModified Object getSelectedItem() { return null; }

        //override from javax.swing.ListModel
        @NotModified int getSize() { return 0; }

        //override from javax.swing.ListModel
        //@Independent(hc=true)[H]
        @NotModified E getElementAt(int index) { return null; }

        int getIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { return 0; }

        //override from javax.swing.MutableComboBoxModel
        void addElement(/*@Independent(hc=true)[H] @NotModified[T]*/ E anObject) { }

        //override from javax.swing.MutableComboBoxModel
        void insertElementAt(/*@Independent(hc=true)[H] @NotModified[T]*/ E anObject, int index) { }

        //override from javax.swing.MutableComboBoxModel
        void removeElementAt(int index) { }

        //override from javax.swing.MutableComboBoxModel
        void removeElement(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object anObject) { }
        void removeAllElements() { }
        void addAll(Collection<? extends E> c) { }
        void addAll(int index, Collection<? extends E> c) { }
    }

    //public class JComboBox extends JComponent implements ItemSelectable, ListDataListener, ActionListener, Accessible
    class JComboBox$<E> {
        //public interface KeySelectionManager
        class KeySelectionManager {int selectionForKey(char c, ComboBoxModel<?> comboBoxModel) { return 0; } }
        JComboBox$(ComboBoxModel<E> aModel) { }
        JComboBox$(E [] items) { }
        JComboBox$(Vector<E> items) { }
        JComboBox$() { }
        void setUI(ComboBoxUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }

        //override from javax.swing.JComponent
        @NotModified ComboBoxUI getUI() { return null; }
        void setModel(ComboBoxModel<E> aModel) { }
        @NotModified ComboBoxModel<E> getModel() { return null; }
        void setLightWeightPopupEnabled(boolean aFlag) { }
        @NotModified boolean isLightWeightPopupEnabled() { return false; }
        void setEditable(boolean aFlag) { }
        @NotModified boolean isEditable() { return false; }
        void setMaximumRowCount(int count) { }
        @NotModified int getMaximumRowCount() { return 0; }
        void setRenderer(ListCellRenderer<? super E> aRenderer) { }
        @NotModified ListCellRenderer<? super E> getRenderer() { return null; }
        void setEditor(ComboBoxEditor anEditor) { }
        @NotModified ComboBoxEditor getEditor() { return null; }
        void setSelectedItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getSelectedItem() { return null; }
        void setSelectedIndex(int anIndex) { }
        @NotModified int getSelectedIndex() { return 0; }
        //@Independent(hc=true)[T]
        @NotModified E getPrototypeDisplayValue() { return null; }
        void setPrototypeDisplayValue(/*@Independent(hc=true)[T] @NotModified[T]*/ E prototypeDisplayValue) { }
        void addItem(/*@Independent(hc=true)[T] @NotModified[T]*/ E item) { }
        void insertItemAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E item, int index) { }
        void removeItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        void removeItemAt(int anIndex) { }
        void removeAllItems() { }
        void showPopup() { }
        void hidePopup() { }
        void setPopupVisible(boolean v) { }
        @NotModified boolean isPopupVisible() { return false; }
        //override from java.awt.ItemSelectable
        void addItemListener(ItemListener aListener) { }

        //override from java.awt.ItemSelectable
        void removeItemListener(ItemListener aListener) { }
        @NotModified ItemListener [] getItemListeners() { return null; }
        void addActionListener(ActionListener l) { }
        void removeActionListener(ActionListener l) { }
        @NotModified ActionListener [] getActionListeners() { return null; }
        void addPopupMenuListener(PopupMenuListener l) { }
        void removePopupMenuListener(PopupMenuListener l) { }
        @NotModified PopupMenuListener [] getPopupMenuListeners() { return null; }
        void firePopupMenuWillBecomeVisible() { }
        void firePopupMenuWillBecomeInvisible() { }
        void firePopupMenuCanceled() { }
        void setActionCommand(String aCommand) { }
        @NotModified String getActionCommand() { return null; }
        void setAction(Action a) { }
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
        void setEnabled(boolean b) { }

        void configureEditor(
            ComboBoxEditor anEditor,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anItem) { }

        //override from java.awt.Component, javax.swing.JComponent
        void processKeyEvent(KeyEvent e) { }
        void setKeySelectionManager(JComboBox.KeySelectionManager aManager) { }
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
        void setUI(LabelUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }
        @NotModified String getText() { return null; }
        void setText(String text) { }
        @NotModified Icon getIcon() { return null; }
        void setIcon(Icon icon) { }
        @NotModified Icon getDisabledIcon() { return null; }
        void setDisabledIcon(Icon disabledIcon) { }
        void setDisplayedMnemonic(int key) { }
        void setDisplayedMnemonic(char aChar) { }
        @NotModified int getDisplayedMnemonic() { return 0; }
        void setDisplayedMnemonicIndex(int index) { }
        @NotModified int getDisplayedMnemonicIndex() { return 0; }
        @NotModified int getIconTextGap() { return 0; }
        void setIconTextGap(int iconTextGap) { }
        @NotModified int getVerticalAlignment() { return 0; }
        void setVerticalAlignment(int alignment) { }
        @NotModified int getHorizontalAlignment() { return 0; }
        void setHorizontalAlignment(int alignment) { }
        @NotModified int getVerticalTextPosition() { return 0; }
        void setVerticalTextPosition(int textPosition) { }
        @NotModified int getHorizontalTextPosition() { return 0; }
        void setHorizontalTextPosition(int textPosition) { }
        //override from java.awt.Component, java.awt.image.ImageObserver
        boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) { return false; }
        @NotModified Component getLabelFor() { return null; }
        void setLabelFor(Component c) { }
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
        void addNotify() { }

        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        void removeNotify() { }

        //@Independent[T]
        static JScrollPane createScrollPaneForTable(JTable aTable) { return null; }
        void setTableHeader(JTableHeader tableHeader) { }
        @NotModified JTableHeader getTableHeader() { return null; }
        void setRowHeight(int rowHeight) { }
        @NotModified int getRowHeight() { return 0; }
        void setRowHeight(int row, int rowHeight) { }
        @NotModified int getRowHeight(int row) { return 0; }
        void setRowMargin(int rowMargin) { }
        @NotModified int getRowMargin() { return 0; }
        void setIntercellSpacing(Dimension intercellSpacing) { }
        @NotModified Dimension getIntercellSpacing() { return null; }
        void setGridColor(Color gridColor) { }
        @NotModified Color getGridColor() { return null; }
        void setShowGrid(boolean showGrid) { }
        void setShowHorizontalLines(boolean showHorizontalLines) { }
        void setShowVerticalLines(boolean showVerticalLines) { }
        @NotModified boolean getShowHorizontalLines() { return false; }
        @NotModified boolean getShowVerticalLines() { return false; }
        void setAutoResizeMode(int mode) { }
        @NotModified int getAutoResizeMode() { return 0; }
        void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) { }
        @NotModified boolean getAutoCreateColumnsFromModel() { return false; }
        void createDefaultColumnsFromModel() { }
        @Commutable(seq="class,0")
        void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) { }
        @NotModified TableCellRenderer getDefaultRenderer(Class<?> columnClass) { return null; }
        void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) { }
        @NotModified TableCellEditor getDefaultEditor(Class<?> columnClass) { return null; }
        void setDragEnabled(boolean b) { }
        @NotModified boolean getDragEnabled() { return false; }
        void setDropMode(DropMode dropMode) { }
        @NotModified DropMode getDropMode() { return null; }
        @NotModified JTable.DropLocation getDropLocation() { return null; }
        void setAutoCreateRowSorter(boolean autoCreateRowSorter) { }
        @NotModified boolean getAutoCreateRowSorter() { return false; }
        void setUpdateSelectionOnSort(boolean update) { }
        @NotModified boolean getUpdateSelectionOnSort() { return false; }
        void setRowSorter(RowSorter<? extends TableModel> sorter) { }
        @NotModified RowSorter<? extends TableModel> getRowSorter() { return null; }
        void setSelectionMode(int selectionMode) { }
        @Commutable
        void setRowSelectionAllowed(boolean rowSelectionAllowed) { }
        @NotModified boolean getRowSelectionAllowed() { return false; }
        void setColumnSelectionAllowed(boolean columnSelectionAllowed) { }
        @NotModified boolean getColumnSelectionAllowed() { return false; }
        void setCellSelectionEnabled(boolean cellSelectionEnabled) { }
        @NotModified boolean getCellSelectionEnabled() { return false; }
        void selectAll() { }
        void clearSelection() { }
        void setRowSelectionInterval(int index0, int index1) { }
        void setColumnSelectionInterval(int index0, int index1) { }
        void addRowSelectionInterval(int index0, int index1) { }
        void addColumnSelectionInterval(int index0, int index1) { }
        void removeRowSelectionInterval(int index0, int index1) { }
        void removeColumnSelectionInterval(int index0, int index1) { }
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
        void setSelectionForeground(Color selectionForeground) { }
        @NotModified Color getSelectionBackground() { return null; }
        void setSelectionBackground(Color selectionBackground) { }
        TableColumn getColumn(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object identifier) {
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

        void setValueAt(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object aValue,
            int row,
            int column) { }
        @NotModified boolean isCellEditable(int row, int column) { return false; }
        void addColumn(TableColumn aColumn) { }
        void removeColumn(TableColumn aColumn) { }
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
        void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) { }
        @NotModified boolean getSurrendersFocusOnKeystroke() { return false; }
        boolean editCellAt(int row, int column) { return false; }
        boolean editCellAt(int row, int column, EventObject e) { return false; }
        @NotModified boolean isEditing() { return false; }
        @NotModified Component getEditorComponent() { return null; }
        @NotModified int getEditingColumn() { return 0; }
        @NotModified int getEditingRow() { return 0; }
        //override from javax.swing.JComponent
        @NotModified TableUI getUI() { return null; }
        void setUI(TableUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from javax.swing.JComponent
        @NotModified String getUIClassID() { return null; }
        void setModel(TableModel dataModel) { }
        @NotModified TableModel getModel() { return null; }
        void setColumnModel(TableColumnModel columnModel) { }
        @NotModified TableColumnModel getColumnModel() { return null; }
        void setSelectionModel(ListSelectionModel selectionModel) { }
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
        void setPreferredScrollableViewportSize(Dimension size) { }
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
        @Commutable
        void setFillsViewportHeight(boolean fillsViewportHeight) { }
        @NotModified boolean getFillsViewportHeight() { return false; }
        @NotModified TableCellEditor getCellEditor() { return null; }
        void setCellEditor(TableCellEditor anEditor) { }
        void setEditingColumn(int aColumn) { }
        void setEditingRow(int aRow) { }
        @NotModified TableCellRenderer getCellRenderer(int row, int column) { return null; }
        Component prepareRenderer(TableCellRenderer renderer, int row, int column) { return null; }
        @NotModified TableCellEditor getCellEditor(int row, int column) { return null; }
        Component prepareEditor(TableCellEditor editor, int row, int column) { return null; }
        void removeEditor() { }
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
}
