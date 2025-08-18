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
