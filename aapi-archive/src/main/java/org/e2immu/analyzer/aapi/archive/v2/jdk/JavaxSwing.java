package org.e2immu.analyzer.aapi.archive.v2.jdk;
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
        boolean getHideActionText() { return false; }
        String getText() { return null; }
        void setText(String text) { }
        boolean isSelected() { return false; }
        void setSelected(boolean b) { }
        void doClick() { }
        void doClick(int pressTime) { }
        void setMargin(Insets m) { }
        Insets getMargin() { return null; }
        Icon getIcon() { return null; }
        void setIcon(Icon defaultIcon) { }
        Icon getPressedIcon() { return null; }
        void setPressedIcon(Icon pressedIcon) { }
        Icon getSelectedIcon() { return null; }
        void setSelectedIcon(Icon selectedIcon) { }
        Icon getRolloverIcon() { return null; }
        void setRolloverIcon(Icon rolloverIcon) { }
        Icon getRolloverSelectedIcon() { return null; }
        void setRolloverSelectedIcon(Icon rolloverSelectedIcon) { }
        Icon getDisabledIcon() { return null; }
        void setDisabledIcon(Icon disabledIcon) { }
        Icon getDisabledSelectedIcon() { return null; }
        void setDisabledSelectedIcon(Icon disabledSelectedIcon) { }
        int getVerticalAlignment() { return 0; }
        void setVerticalAlignment(int alignment) { }
        int getHorizontalAlignment() { return 0; }
        void setHorizontalAlignment(int alignment) { }
        int getVerticalTextPosition() { return 0; }
        void setVerticalTextPosition(int textPosition) { }
        int getHorizontalTextPosition() { return 0; }
        void setHorizontalTextPosition(int textPosition) { }
        int getIconTextGap() { return 0; }
        void setIconTextGap(int iconTextGap) { }
        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
        void removeNotify() { }
        void setActionCommand(String actionCommand) { }
        String getActionCommand() { return null; }
        void setAction(Action a) { }
        Action getAction() { return null; }
        boolean isBorderPainted() { return false; }
        void setBorderPainted(boolean b) { }
        boolean isFocusPainted() { return false; }
        void setFocusPainted(boolean b) { }
        boolean isContentAreaFilled() { return false; }
        void setContentAreaFilled(boolean b) { }
        boolean isRolloverEnabled() { return false; }
        void setRolloverEnabled(boolean b) { }
        int getMnemonic() { return 0; }
        void setMnemonic(int mnemonic) { }
        void setMnemonic(char mnemonic) { }
        void setDisplayedMnemonicIndex(int index) { }
        int getDisplayedMnemonicIndex() { return 0; }
        void setMultiClickThreshhold(long threshold) { }
        long getMultiClickThreshhold() { return 0L; }
        ButtonModel getModel() { return null; }
        void setModel(ButtonModel newModel) { }
        //override from javax.swing.JComponent
        ButtonUI getUI() { return null; }
        void setUI(ButtonUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from java.awt.Container
        void setLayout(LayoutManager mgr) { }
        void addChangeListener(ChangeListener l) { }
        void removeChangeListener(ChangeListener l) { }
        ChangeListener [] getChangeListeners() { return null; }
        void addActionListener(ActionListener l) { }
        void removeActionListener(ActionListener l) { }
        ActionListener [] getActionListeners() { return null; }
        //override from java.awt.Component, javax.swing.JComponent
        void setEnabled(boolean b) { }
        String getLabel() { return null; }
        void setLabel(String label) { }
        //override from java.awt.ItemSelectable
        void addItemListener(ItemListener l) { }

        //override from java.awt.ItemSelectable
        void removeItemListener(ItemListener l) { }
        ItemListener [] getItemListeners() { return null; }
        //override from java.awt.ItemSelectable
        Object [] getSelectedObjects() { return null; }

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
        Object getSelectedItem() { return null; }

        //override from javax.swing.ListModel
        int getSize() { return 0; }

        //override from javax.swing.ListModel
        //@Independent(hc=true)[H]
        E getElementAt(int index) { return null; }

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
        String getUIClassID() { return null; }

        //override from javax.swing.JComponent
        ComboBoxUI getUI() { return null; }
        void setModel(ComboBoxModel<E> aModel) { }
        ComboBoxModel<E> getModel() { return null; }
        void setLightWeightPopupEnabled(boolean aFlag) { }
        boolean isLightWeightPopupEnabled() { return false; }
        void setEditable(boolean aFlag) { }
        boolean isEditable() { return false; }
        void setMaximumRowCount(int count) { }
        int getMaximumRowCount() { return 0; }
        void setRenderer(ListCellRenderer<? super E> aRenderer) { }
        ListCellRenderer<? super E> getRenderer() { return null; }
        void setEditor(ComboBoxEditor anEditor) { }
        ComboBoxEditor getEditor() { return null; }
        void setSelectedItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getSelectedItem() { return null; }
        void setSelectedIndex(int anIndex) { }
        int getSelectedIndex() { return 0; }
        //@Independent(hc=true)[T]
        E getPrototypeDisplayValue() { return null; }
        void setPrototypeDisplayValue(/*@Independent(hc=true)[T] @NotModified[T]*/ E prototypeDisplayValue) { }
        void addItem(/*@Independent(hc=true)[T] @NotModified[T]*/ E item) { }
        void insertItemAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E item, int index) { }
        void removeItem(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object anObject) { }
        void removeItemAt(int anIndex) { }
        void removeAllItems() { }
        void showPopup() { }
        void hidePopup() { }
        void setPopupVisible(boolean v) { }
        boolean isPopupVisible() { return false; }
        //override from java.awt.ItemSelectable
        void addItemListener(ItemListener aListener) { }

        //override from java.awt.ItemSelectable
        void removeItemListener(ItemListener aListener) { }
        ItemListener [] getItemListeners() { return null; }
        void addActionListener(ActionListener l) { }
        void removeActionListener(ActionListener l) { }
        ActionListener [] getActionListeners() { return null; }
        void addPopupMenuListener(PopupMenuListener l) { }
        void removePopupMenuListener(PopupMenuListener l) { }
        PopupMenuListener [] getPopupMenuListeners() { return null; }
        void firePopupMenuWillBecomeVisible() { }
        void firePopupMenuWillBecomeInvisible() { }
        void firePopupMenuCanceled() { }
        void setActionCommand(String aCommand) { }
        String getActionCommand() { return null; }
        void setAction(Action a) { }
        Action getAction() { return null; }
        //override from java.awt.ItemSelectable
        Object [] getSelectedObjects() { return null; }

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
        JComboBox.KeySelectionManager getKeySelectionManager() { return null; }
        int getItemCount() { return 0; }
        //@Independent(hc=true)[T]
        E getItemAt(int index) { return null; }

        //override from java.awt.Component, javax.accessibility.Accessible
        AccessibleContext getAccessibleContext() { return null; }
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
        LabelUI getUI() { return null; }
        void setUI(LabelUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from javax.swing.JComponent
        String getUIClassID() { return null; }
        String getText() { return null; }
        void setText(String text) { }
        Icon getIcon() { return null; }
        void setIcon(Icon icon) { }
        Icon getDisabledIcon() { return null; }
        void setDisabledIcon(Icon disabledIcon) { }
        void setDisplayedMnemonic(int key) { }
        void setDisplayedMnemonic(char aChar) { }
        int getDisplayedMnemonic() { return 0; }
        void setDisplayedMnemonicIndex(int index) { }
        int getDisplayedMnemonicIndex() { return 0; }
        int getIconTextGap() { return 0; }
        void setIconTextGap(int iconTextGap) { }
        int getVerticalAlignment() { return 0; }
        void setVerticalAlignment(int alignment) { }
        int getHorizontalAlignment() { return 0; }
        void setHorizontalAlignment(int alignment) { }
        int getVerticalTextPosition() { return 0; }
        void setVerticalTextPosition(int textPosition) { }
        int getHorizontalTextPosition() { return 0; }
        void setHorizontalTextPosition(int textPosition) { }
        //override from java.awt.Component, java.awt.image.ImageObserver
        boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) { return false; }
        Component getLabelFor() { return null; }
        void setLabelFor(Component c) { }
        //override from java.awt.Component, javax.accessibility.Accessible
        AccessibleContext getAccessibleContext() { return null; }
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
            int getRow() { return 0; }
            int getColumn() { return 0; }
            boolean isInsertRow() { return false; }
            boolean isInsertColumn() { return false; }
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
        JTableHeader getTableHeader() { return null; }
        void setRowHeight(int rowHeight) { }
        int getRowHeight() { return 0; }
        void setRowHeight(int row, int rowHeight) { }
        int getRowHeight(int row) { return 0; }
        void setRowMargin(int rowMargin) { }
        int getRowMargin() { return 0; }
        void setIntercellSpacing(Dimension intercellSpacing) { }
        Dimension getIntercellSpacing() { return null; }
        void setGridColor(Color gridColor) { }
        Color getGridColor() { return null; }
        void setShowGrid(boolean showGrid) { }
        void setShowHorizontalLines(boolean showHorizontalLines) { }
        void setShowVerticalLines(boolean showVerticalLines) { }
        boolean getShowHorizontalLines() { return false; }
        boolean getShowVerticalLines() { return false; }
        void setAutoResizeMode(int mode) { }
        int getAutoResizeMode() { return 0; }
        void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) { }
        boolean getAutoCreateColumnsFromModel() { return false; }
        void createDefaultColumnsFromModel() { }
        @Commutable(seq="class,0")
        void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) { }
        TableCellRenderer getDefaultRenderer(Class<?> columnClass) { return null; }
        void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) { }
        TableCellEditor getDefaultEditor(Class<?> columnClass) { return null; }
        void setDragEnabled(boolean b) { }
        boolean getDragEnabled() { return false; }
        void setDropMode(DropMode dropMode) { }
        DropMode getDropMode() { return null; }
        JTable.DropLocation getDropLocation() { return null; }
        void setAutoCreateRowSorter(boolean autoCreateRowSorter) { }
        boolean getAutoCreateRowSorter() { return false; }
        void setUpdateSelectionOnSort(boolean update) { }
        boolean getUpdateSelectionOnSort() { return false; }
        void setRowSorter(RowSorter<? extends TableModel> sorter) { }
        RowSorter<? extends TableModel> getRowSorter() { return null; }
        void setSelectionMode(int selectionMode) { }
        @Commutable
        void setRowSelectionAllowed(boolean rowSelectionAllowed) { }
        boolean getRowSelectionAllowed() { return false; }
        void setColumnSelectionAllowed(boolean columnSelectionAllowed) { }
        boolean getColumnSelectionAllowed() { return false; }
        void setCellSelectionEnabled(boolean cellSelectionEnabled) { }
        boolean getCellSelectionEnabled() { return false; }
        void selectAll() { }
        void clearSelection() { }
        void setRowSelectionInterval(int index0, int index1) { }
        void setColumnSelectionInterval(int index0, int index1) { }
        void addRowSelectionInterval(int index0, int index1) { }
        void addColumnSelectionInterval(int index0, int index1) { }
        void removeRowSelectionInterval(int index0, int index1) { }
        void removeColumnSelectionInterval(int index0, int index1) { }
        int getSelectedRow() { return 0; }
        int getSelectedColumn() { return 0; }
        int [] getSelectedRows() { return null; }
        int [] getSelectedColumns() { return null; }
        int getSelectedRowCount() { return 0; }
        int getSelectedColumnCount() { return 0; }
        boolean isRowSelected(int row) { return false; }
        boolean isColumnSelected(int column) { return false; }
        boolean isCellSelected(int row, int column) { return false; }
        void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) { }
        Color getSelectionForeground() { return null; }
        void setSelectionForeground(Color selectionForeground) { }
        Color getSelectionBackground() { return null; }
        void setSelectionBackground(Color selectionBackground) { }
        TableColumn getColumn(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object identifier) {
            return null;
        }
        int convertColumnIndexToModel(int viewColumnIndex) { return 0; }
        int convertColumnIndexToView(int modelColumnIndex) { return 0; }
        int convertRowIndexToView(int modelRowIndex) { return 0; }
        int convertRowIndexToModel(int viewRowIndex) { return 0; }
        int getRowCount() { return 0; }
        int getColumnCount() { return 0; }
        String getColumnName(int column) { return null; }
        Class<?> getColumnClass(int column) { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getValueAt(int row, int column) { return null; }

        void setValueAt(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object aValue,
            int row,
            int column) { }
        boolean isCellEditable(int row, int column) { return false; }
        void addColumn(TableColumn aColumn) { }
        void removeColumn(TableColumn aColumn) { }
        void moveColumn(int column, int targetColumn) { }
        int columnAtPoint(Point point) { return 0; }
        int rowAtPoint(Point point) { return 0; }
        Rectangle getCellRect(int row, int column, boolean includeSpacing) { return null; }
        //override from java.awt.Component, java.awt.Container
        void doLayout() { }
        void sizeColumnsToFit(boolean lastColumnOnly) { }
        void sizeColumnsToFit(int resizingColumn) { }
        //override from javax.swing.JComponent
        String getToolTipText(MouseEvent event) { return null; }
        void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) { }
        boolean getSurrendersFocusOnKeystroke() { return false; }
        boolean editCellAt(int row, int column) { return false; }
        boolean editCellAt(int row, int column, EventObject e) { return false; }
        boolean isEditing() { return false; }
        Component getEditorComponent() { return null; }
        int getEditingColumn() { return 0; }
        int getEditingRow() { return 0; }
        //override from javax.swing.JComponent
        TableUI getUI() { return null; }
        void setUI(TableUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }

        //override from javax.swing.JComponent
        String getUIClassID() { return null; }
        void setModel(TableModel dataModel) { }
        TableModel getModel() { return null; }
        void setColumnModel(TableColumnModel columnModel) { }
        TableColumnModel getColumnModel() { return null; }
        void setSelectionModel(ListSelectionModel selectionModel) { }
        ListSelectionModel getSelectionModel() { return null; }
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
        Dimension getPreferredScrollableViewportSize() { return null; }

        //override from javax.swing.Scrollable
        int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }

        //override from javax.swing.Scrollable
        int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }

        //override from javax.swing.Scrollable
        boolean getScrollableTracksViewportWidth() { return false; }

        //override from javax.swing.Scrollable
        boolean getScrollableTracksViewportHeight() { return false; }
        @Commutable
        void setFillsViewportHeight(boolean fillsViewportHeight) { }
        boolean getFillsViewportHeight() { return false; }
        TableCellEditor getCellEditor() { return null; }
        void setCellEditor(TableCellEditor anEditor) { }
        void setEditingColumn(int aColumn) { }
        void setEditingRow(int aRow) { }
        TableCellRenderer getCellRenderer(int row, int column) { return null; }
        Component prepareRenderer(TableCellRenderer renderer, int row, int column) { return null; }
        TableCellEditor getCellEditor(int row, int column) { return null; }
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

        Printable getPrintable(JTable.PrintMode printMode, MessageFormat headerFormat, MessageFormat footerFormat) {
            return null;
        }

        //override from java.awt.Component, javax.accessibility.Accessible
        AccessibleContext getAccessibleContext() { return null; }
    }
}
