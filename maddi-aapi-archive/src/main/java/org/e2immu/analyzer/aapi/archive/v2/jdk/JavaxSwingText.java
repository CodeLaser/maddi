package org.e2immu.analyzer.aapi.archive.v2.jdk;
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
    //public abstract class JTextComponent extends JComponent implements Scrollable, Accessible
    class JTextComponent$ {
        static final String FOCUS_ACCELERATOR_KEY = null;
        static final String DEFAULT_KEYMAP = null;
        //public class AccessibleJTextComponent extends AccessibleJComponent implements AccessibleText, CaretListener, DocumentListener, AccessibleAction, AccessibleEditableText, AccessibleExtendedText
        class AccessibleJTextComponent {
            AccessibleJTextComponent(JTextComponent this$0) { }
            //override from javax.swing.event.CaretListener
            void caretUpdate(CaretEvent e) { }

            //override from javax.swing.event.DocumentListener
            void insertUpdate(DocumentEvent e) { }

            //override from javax.swing.event.DocumentListener
            void removeUpdate(DocumentEvent e) { }

            //override from javax.swing.event.DocumentListener
            void changedUpdate(DocumentEvent e) { }

            //override from java.awt.Component.AccessibleAWTComponent, javax.accessibility.AccessibleContext, javax.swing.JComponent.AccessibleJComponent
            AccessibleStateSet getAccessibleStateSet() { return null; }

            //override from java.awt.Component.AccessibleAWTComponent, javax.accessibility.AccessibleContext, javax.swing.JComponent.AccessibleJComponent
            AccessibleRole getAccessibleRole() { return null; }

            //override from javax.accessibility.AccessibleContext
            AccessibleText getAccessibleText() { return null; }

            //override from javax.accessibility.AccessibleText
            int getIndexAtPoint(Point p) { return 0; }

            //override from javax.accessibility.AccessibleText
            Rectangle getCharacterBounds(int i) { return null; }

            //override from javax.accessibility.AccessibleText
            int getCharCount() { return 0; }

            //override from javax.accessibility.AccessibleText
            int getCaretPosition() { return 0; }

            //override from javax.accessibility.AccessibleText
            AttributeSet getCharacterAttribute(int i) { return null; }

            //override from javax.accessibility.AccessibleText
            int getSelectionStart() { return 0; }

            //override from javax.accessibility.AccessibleText
            int getSelectionEnd() { return 0; }

            //override from javax.accessibility.AccessibleText
            String getSelectedText() { return null; }

            //override from javax.accessibility.AccessibleText
            String getAtIndex(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleText
            String getAfterIndex(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleText
            String getBeforeIndex(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleContext
            AccessibleEditableText getAccessibleEditableText() { return null; }

            //override from javax.accessibility.AccessibleEditableText
            void setTextContents(String s) { }

            //override from javax.accessibility.AccessibleEditableText
            void insertTextAtIndex(int index, String s) { }

            //override from javax.accessibility.AccessibleEditableText, javax.accessibility.AccessibleExtendedText
            String getTextRange(int startIndex, int endIndex) { return null; }

            //override from javax.accessibility.AccessibleEditableText
            void delete(int startIndex, int endIndex) { }

            //override from javax.accessibility.AccessibleEditableText
            void cut(int startIndex, int endIndex) { }

            //override from javax.accessibility.AccessibleEditableText
            void paste(int startIndex) { }

            //override from javax.accessibility.AccessibleEditableText
            void replaceText(int startIndex, int endIndex, String s) { }

            //override from javax.accessibility.AccessibleEditableText
            void selectText(int startIndex, int endIndex) { }

            //override from javax.accessibility.AccessibleEditableText
            void setAttributes(int startIndex, int endIndex, AttributeSet as) { }

            //override from javax.accessibility.AccessibleExtendedText
            AccessibleTextSequence getTextSequenceAt(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleExtendedText
            AccessibleTextSequence getTextSequenceAfter(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleExtendedText
            AccessibleTextSequence getTextSequenceBefore(int part, int index) { return null; }

            //override from javax.accessibility.AccessibleExtendedText
            Rectangle getTextBounds(int startIndex, int endIndex) { return null; }

            //override from javax.accessibility.AccessibleContext
            AccessibleAction getAccessibleAction() { return null; }

            //override from javax.accessibility.AccessibleAction
            int getAccessibleActionCount() { return 0; }

            //override from javax.accessibility.AccessibleAction
            String getAccessibleActionDescription(int i) { return null; }

            //override from javax.accessibility.AccessibleAction
            boolean doAccessibleAction(int i) { return false; }
        }

        //public static final class DropLocation extends DropLocation
        class DropLocation {
            int getIndex() { return 0; }
            Position.Bias getBias() { return null; }
            //override from java.lang.Object, javax.swing.TransferHandler.DropLocation
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }

        //public static class KeyBinding
        class KeyBinding { KeyStroke key; String actionName;KeyBinding(KeyStroke key, String actionName) { } }
        JTextComponent$() { }
        //override from javax.swing.JComponent
        TextUI getUI() { return null; }
        void setUI(TextUI ui) { }
        //override from javax.swing.JComponent
        void updateUI() { }
        void addCaretListener(CaretListener listener) { }
        void removeCaretListener(CaretListener listener) { }
        CaretListener [] getCaretListeners() { return null; }
        void setDocument(Document doc) { }
        Document getDocument() { return null; }
        //override from java.awt.Component
        void setComponentOrientation(ComponentOrientation o) { }
        Action [] getActions() { return null; }
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
        //@Independent[T]
        static Keymap addKeymap(String nm, Keymap parent) { return null; }

        //@Independent[T]
        static Keymap removeKeymap(String nm) { return null; }

        //@Independent[T]
        static Keymap getKeymap(String nm) { return null; }
        static void loadKeymap(Keymap map, JTextComponent.KeyBinding [] bindings, Action [] actions) { }
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
        void read(Reader in, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object desc) { }
        void write(Writer out) { }
        //override from java.awt.Component, java.awt.Container, javax.swing.JComponent
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
        //override from javax.swing.JComponent
        String getToolTipText(MouseEvent event) { return null; }

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
        //override from java.awt.Component, javax.accessibility.Accessible
        AccessibleContext getAccessibleContext() { return null; }

        //override from java.awt.Component
        InputMethodRequests getInputMethodRequests() { return null; }

        //override from java.awt.Component
        void addInputMethodListener(InputMethodListener l) { }
    }
}
