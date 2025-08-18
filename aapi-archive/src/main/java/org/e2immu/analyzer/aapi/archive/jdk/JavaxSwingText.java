package org.e2immu.analyzer.aapi.archive.jdk;
import java.awt.*;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputMethodRequests;
import java.awt.print.Printable;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import javax.accessibility.*;
import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
public class JavaxSwingText {
    public static final String PACKAGE_NAME = "javax.swing.text";
    class JTextComponent$ {
        static final String FOCUS_ACCELERATOR_KEY = null;
        static final String DEFAULT_KEYMAP = null;

        class AccessibleJTextComponent {
            AccessibleJTextComponent(JTextComponent this$0) { }
            void caretUpdate(CaretEvent e) { }
            void insertUpdate(DocumentEvent e) { }
            void removeUpdate(DocumentEvent e) { }
            void changedUpdate(DocumentEvent e) { }
            AccessibleStateSet getAccessibleStateSet() { return null; }
            AccessibleRole getAccessibleRole() { return null; }
            AccessibleText getAccessibleText() { return null; }
            int getIndexAtPoint(Point p) { return 0; }
            Rectangle getCharacterBounds(int i) { return null; }
            int getCharCount() { return 0; }
            int getCaretPosition() { return 0; }
            AttributeSet getCharacterAttribute(int i) { return null; }
            int getSelectionStart() { return 0; }
            int getSelectionEnd() { return 0; }
            String getSelectedText() { return null; }
            String getAtIndex(int part, int index) { return null; }
            String getAfterIndex(int part, int index) { return null; }
            String getBeforeIndex(int part, int index) { return null; }
            AccessibleEditableText getAccessibleEditableText() { return null; }
            void setTextContents(String s) { }
            void insertTextAtIndex(int index, String s) { }
            String getTextRange(int startIndex, int endIndex) { return null; }
            void delete(int startIndex, int endIndex) { }
            void cut(int startIndex, int endIndex) { }
            void paste(int startIndex) { }
            void replaceText(int startIndex, int endIndex, String s) { }
            void selectText(int startIndex, int endIndex) { }
            void setAttributes(int startIndex, int endIndex, AttributeSet as) { }
            AccessibleTextSequence getTextSequenceAt(int part, int index) { return null; }
            AccessibleTextSequence getTextSequenceAfter(int part, int index) { return null; }
            AccessibleTextSequence getTextSequenceBefore(int part, int index) { return null; }
            Rectangle getTextBounds(int startIndex, int endIndex) { return null; }
            AccessibleAction getAccessibleAction() { return null; }
            int getAccessibleActionCount() { return 0; }
            String getAccessibleActionDescription(int i) { return null; }
            boolean doAccessibleAction(int i) { return false; }
        }

        class DropLocation {
            int getIndex() { return 0; }
            Position.Bias getBias() { return null; }
            public String toString() { return null; }
        }

        class KeyBinding { KeyStroke key; String actionName; KeyBinding(KeyStroke key, String actionName) { } }
        JTextComponent$() { }
        TextUI getUI() { return null; }
        void setUI(TextUI ui) { }
        void updateUI() { }
        void addCaretListener(CaretListener listener) { }
        void removeCaretListener(CaretListener listener) { }
        CaretListener[] getCaretListeners() { return null; }
        void setDocument(Document doc) { }
        Document getDocument() { return null; }
        void setComponentOrientation(ComponentOrientation o) { }
        Action[] getActions() { return null; }
        void setMargin(Insets m) { }
        Insets getMargin() { return null; }
        void setNavigationFilter(NavigationFilter filter) { }
        NavigationFilter getNavigationFilter() { return null; }
        Caret getCaret() { return null; }
        void setCaret(Caret c) { }
        Highlighter getHighlighter() { return null; }
        void setHighlighter(Highlighter h) { }
        void setKeymap(Keymap map) { }
        void setDragEnabled(boolean b) { }
        boolean getDragEnabled() { return false; }
        void setDropMode(DropMode dropMode) { }
        DropMode getDropMode() { return null; }
        JTextComponent.DropLocation getDropLocation() { return null; }
        Keymap getKeymap() { return null; }
        static Keymap addKeymap(String nm, Keymap parent) { return null; }
        static Keymap removeKeymap(String nm) { return null; }
        static Keymap getKeymap(String nm) { return null; }
        static void loadKeymap(Keymap map, JTextComponent.KeyBinding[] bindings, Action[] actions) { }
        Color getCaretColor() { return null; }
        void setCaretColor(Color c) { }
        Color getSelectionColor() { return null; }
        void setSelectionColor(Color c) { }
        Color getSelectedTextColor() { return null; }
        void setSelectedTextColor(Color c) { }
        Color getDisabledTextColor() { return null; }
        void setDisabledTextColor(Color c) { }
        void replaceSelection(String content) { }
        String getText(int offs, int len) { return null; }
        Rectangle modelToView(int pos) { return null; }
        Rectangle2D modelToView2D(int pos) { return null; }
        int viewToModel(Point pt) { return 0; }
        int viewToModel2D(Point2D pt) { return 0; }
        void cut() { }
        void copy() { }
        void paste() { }
        void moveCaretPosition(int pos) { }
        void setFocusAccelerator(char aKey) { }
        char getFocusAccelerator() { return '\0'; }
        void read(Reader in, Object desc) { }
        void write(Writer out) { }
        void removeNotify() { }
        void setCaretPosition(int position) { }
        int getCaretPosition() { return 0; }
        void setText(String t) { }
        String getText() { return null; }
        String getSelectedText() { return null; }
        boolean isEditable() { return false; }
        void setEditable(boolean b) { }
        int getSelectionStart() { return 0; }
        void setSelectionStart(int selectionStart) { }
        int getSelectionEnd() { return 0; }
        void setSelectionEnd(int selectionEnd) { }
        void select(int selectionStart, int selectionEnd) { }
        void selectAll() { }
        String getToolTipText(MouseEvent event) { return null; }
        Dimension getPreferredScrollableViewportSize() { return null; }
        int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }
        int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 0; }
        boolean getScrollableTracksViewportWidth() { return false; }
        boolean getScrollableTracksViewportHeight() { return false; }
        boolean print() { return false; }
        boolean print(MessageFormat headerFormat, MessageFormat footerFormat) { return false; }

        boolean print(
            MessageFormat headerFormat,
            MessageFormat footerFormat,
            boolean showPrintDialog,
            PrintService service,
            PrintRequestAttributeSet attributes,
            boolean interactive) { return false; }

        Printable getPrintable(MessageFormat headerFormat, MessageFormat footerFormat) { return null; }
        AccessibleContext getAccessibleContext() { return null; }
        InputMethodRequests getInputMethodRequests() { return null; }
        void addInputMethodListener(InputMethodListener l) { }
    }
}
