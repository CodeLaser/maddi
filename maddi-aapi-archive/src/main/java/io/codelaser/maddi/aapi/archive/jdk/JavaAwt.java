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
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.*;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.AttributedCharacterIterator;
import java.util.EventListener;
import java.util.Locale;
import java.util.Set;
import javax.accessibility.AccessibleContext;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Commutable;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.method.GetSet;

public class JavaAwt {
    public static final String PACKAGE_NAME = "java.awt";
    //public class Color implements Paint, Serializable
    @Immutable(hc = true)
    class Color$ {
        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color white = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color WHITE = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color lightGray = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color LIGHT_GRAY = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color gray = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color GRAY = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color darkGray = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color DARK_GRAY = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color black = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color BLACK = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color red = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color RED = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color pink = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color PINK = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color orange = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color ORANGE = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color yellow = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color YELLOW = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color green = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color GREEN = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color magenta = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color MAGENTA = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color cyan = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color CYAN = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color blue = null;

        //@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]
        static final Color BLUE = null;
        Color$(int r, int g, int b) { }
        Color$(int r, int g, int b, int a) { }
        Color$(int rgb) { }
        Color$(int rgba, boolean hasalpha) { }
        Color$(float r, float g, float b) { }
        Color$(float r, float g, float b, float a) { }
        Color$(
            /*@Independent(hc=true)[T]*/ ColorSpace cspace,
            /*@Independent(hc=true)[T]*/ float [] components,
            float alpha) { }

        //@NotModified[T]
        @NotModified int getRed() { return 0; }

        //@NotModified[T]
        @NotModified int getGreen() { return 0; }

        //@NotModified[T]
        @NotModified int getBlue() { return 0; }

        //@NotModified[T]
        @NotModified int getAlpha() { return 0; }

        //@NotModified[T]
        @NotModified int getRGB() { return 0; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Color brighter() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Color darker() { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static Color decode(String nm) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotModified static Color getColor(String nm) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static Color getColor(String nm, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color v) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotModified static Color getColor(String nm, int v) { return null; }

        //@NotModified[T]
        static int HSBtoRGB(float hue, float saturation, float brightness) { return 0; }

        //@Independent[T] @NotModified[T]
        static float [] RGBtoHSB(int r, int g, int b, /*@Independent[M]*/ float [] hsbvals) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotModified static Color getHSBColor(float h, float s, float b) { return null; }

        //@NotModified[T]
        @Independent
        @NotModified float [] getRGBComponents(@Independent(dependentReturnValue = true) float [] compArray) { return null; }

        //@NotModified[T]
        @Independent
        @NotModified float [] getRGBColorComponents(@Independent(dependentReturnValue = true) float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        @NotModified float [] getComponents(/*@Independent[M]*/ float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        @NotModified float [] getColorComponents(/*@Independent[M]*/ float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        @NotModified float [] getComponents(/*@Independent[M]*/ ColorSpace cspace, /*@Independent[M]*/ float [] compArray) {
            return null;
        }

        //@Independent(hc=true)[O] @NotModified[T]
        @NotModified float [] getColorComponents(/*@Independent[M]*/ ColorSpace cspace, /*@Independent[M]*/ float [] compArray) {
            return null;
        }

        //@Independent(hc=true)[O] @NotModified[T]
        @NotModified ColorSpace getColorSpace() { return null; }

        //override from java.awt.Paint
        //@Independent(hc=true)[O] @NotModified[T]

        PaintContext createContext(
            /*@Independent[M]*/ ColorModel cm,
            /*@Independent[M]*/ Rectangle r,
            /*@Independent[M]*/ Rectangle2D r2d,
            /*@Independent[M]*/ AffineTransform xform,
            /*@Independent[M]*/ RenderingHints hints) { return null; }

        //override from java.awt.Transparency
        //@NotModified[T]
        @NotModified int getTransparency() { return 0; }
    }

    //public abstract class Component implements ImageObserver, MenuContainer, Serializable
    class Component$ {
        static final float TOP_ALIGNMENT = 0.0F;
        static final float CENTER_ALIGNMENT = 0.0F;
        static final float BOTTOM_ALIGNMENT = 0.0F;
        static final float LEFT_ALIGNMENT = 0.0F;
        static final float RIGHT_ALIGNMENT = 0.0F;
        //public enum BaselineResizeBehavior extends Enum<BaselineResizeBehavior>
        class BaselineResizeBehavior {
            //@NotNull[O]
            static final Component.BaselineResizeBehavior CONSTANT_ASCENT = null;

            //@NotNull[O]
            static final Component.BaselineResizeBehavior CONSTANT_DESCENT = null;

            //@NotNull[O]
            static final Component.BaselineResizeBehavior CENTER_OFFSET = null;

            //@NotNull[O]
            static final Component.BaselineResizeBehavior OTHER = null;
            static Component.BaselineResizeBehavior [] values() { return null; }
            static Component.BaselineResizeBehavior valueOf(String name) { return null; }
        }

        //public class BltBufferStrategy extends BufferStrategy
        class BltBufferStrategy {
            //override from java.awt.image.BufferStrategy
            void dispose() { }

            //override from java.awt.image.BufferStrategy
            @NotModified BufferCapabilities getCapabilities() { return null; }

            //override from java.awt.image.BufferStrategy
            @NotModified Graphics getDrawGraphics() { return null; }

            //override from java.awt.image.BufferStrategy
            void show() { }

            //override from java.awt.image.BufferStrategy
            boolean contentsLost() { return false; }

            //override from java.awt.image.BufferStrategy
            boolean contentsRestored() { return false; }
        }

        //public class FlipBufferStrategy extends BufferStrategy
        class FlipBufferStrategy {
            //override from java.awt.image.BufferStrategy
            @NotModified BufferCapabilities getCapabilities() { return null; }

            //override from java.awt.image.BufferStrategy
            @NotModified Graphics getDrawGraphics() { return null; }

            //override from java.awt.image.BufferStrategy
            boolean contentsLost() { return false; }

            //override from java.awt.image.BufferStrategy
            boolean contentsRestored() { return false; }

            //override from java.awt.image.BufferStrategy
            void show() { }

            //override from java.awt.image.BufferStrategy
            void dispose() { }
        }
        @NotModified String getName() { return null; }
        @Modified void setName(String name) { }
        @NotModified Container getParent() { return null; }
        @Modified void setDropTarget(DropTarget dt) { }
        @NotModified DropTarget getDropTarget() { return null; }
        @NotModified GraphicsConfiguration getGraphicsConfiguration() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Object getTreeLock() { return null; }
        @NotModified Toolkit getToolkit() { return null; }
        @NotModified boolean isValid() { return false; }
        @NotModified boolean isDisplayable() { return false; }
        @NotModified boolean isVisible() { return false; }
        @NotModified Point getMousePosition() { return null; }
        @NotModified boolean isShowing() { return false; }
        @NotModified boolean isEnabled() { return false; }
        @Modified void setEnabled(boolean b) { }
        void enable() { }
        void enable(boolean b) { }
        void disable() { }
        @NotModified boolean isDoubleBuffered() { return false; }
        void enableInputMethods(boolean enable) { }
        @Modified void setVisible(boolean b) { }
        void show() { }
        void show(boolean b) { }
        void hide() { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Color getForeground() { return null; }
        @Modified void setForeground(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color c) { }
        @NotModified boolean isForegroundSet() { return false; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Color getBackground() { return null; }
        @Modified void setBackground(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color c) { }
        @NotModified boolean isBackgroundSet() { return false; }
        //override from java.awt.MenuContainer
        @NotModified Font getFont() { return null; }
        @Modified void setFont(Font f) { }
        @NotModified boolean isFontSet() { return false; }
        @NotModified Locale getLocale() { return null; }
        @Modified void setLocale(Locale l) { }
        @NotModified ColorModel getColorModel() { return null; }
        @NotModified Point getLocation() { return null; }
        @NotModified Point getLocationOnScreen() { return null; }
        Point location() { return null; }
        @Modified void setLocation(int x, int y) { }
        void move(int x, int y) { }
        @Modified void setLocation(Point p) { }
        @NotModified Dimension getSize() { return null; }
        Dimension size() { return null; }
        @Modified void setSize(int width, int height) { }
        void resize(int width, int height) { }
        @Modified void setSize(Dimension d) { }
        void resize(Dimension d) { }
        @NotModified Rectangle getBounds() { return null; }
        Rectangle bounds() { return null; }
        @Modified void setBounds(int x, int y, int width, int height) { }
        void reshape(int x, int y, int width, int height) { }
        @Modified void setBounds(Rectangle r) { }
        @NotModified int getX() { return 0; }
        @NotModified int getY() { return 0; }
        @NotModified int getWidth() { return 0; }
        @NotModified int getHeight() { return 0; }
        @NotModified Rectangle getBounds(Rectangle rv) { return null; }
        @NotModified Dimension getSize(Dimension rv) { return null; }
        @NotModified Point getLocation(Point rv) { return null; }
        @NotModified boolean isOpaque() { return false; }
        @NotModified boolean isLightweight() { return false; }
        @Modified void setPreferredSize(Dimension preferredSize) { }
        @NotModified boolean isPreferredSizeSet() { return false; }
        @NotModified Dimension getPreferredSize() { return null; }
        Dimension preferredSize() { return null; }
        @Modified void setMinimumSize(Dimension minimumSize) { }
        @NotModified boolean isMinimumSizeSet() { return false; }
        @NotModified Dimension getMinimumSize() { return null; }
        Dimension minimumSize() { return null; }
        @Modified void setMaximumSize(Dimension maximumSize) { }
        @NotModified boolean isMaximumSizeSet() { return false; }
        @NotModified Dimension getMaximumSize() { return null; }
        @NotModified float getAlignmentX() { return 0.0F; }
        @NotModified float getAlignmentY() { return 0.0F; }
        @NotModified int getBaseline(int width, int height) { return 0; }
        @NotModified Component.BaselineResizeBehavior getBaselineResizeBehavior() { return null; }
        void doLayout() { }
        void layout() { }
        void validate() { }
        void invalidate() { }
        void revalidate() { }
        @NotModified Graphics getGraphics() { return null; }
        @NotModified FontMetrics getFontMetrics(Font font) { return null; }
        @Modified void setCursor(Cursor cursor) { }
        @NotModified Cursor getCursor() { return null; }
        @NotModified boolean isCursorSet() { return false; }
        void paint(Graphics g) { }
        @Modified void update(Graphics g) { }
        void paintAll(Graphics g) { }
        void repaint() { }
        void repaint(long tm) { }
        void repaint(int x, int y, int width, int height) { }
        void repaint(long tm, int i, int x, int y, int width) { }
        void print(Graphics g) { }
        void printAll(Graphics g) { }
        //override from java.awt.image.ImageObserver
        boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) { return false; }
        Image createImage(ImageProducer producer) { return null; }
        Image createImage(int width, int height) { return null; }
        VolatileImage createVolatileImage(int width, int height) { return null; }
        VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps) { return null; }
        boolean prepareImage(Image image, ImageObserver observer) { return false; }
        boolean prepareImage(Image image, int width, int height, ImageObserver observer) { return false; }
        int checkImage(Image image, ImageObserver observer) { return 0; }
        int checkImage(Image image, int width, int height, ImageObserver observer) { return 0; }
        @Modified void setIgnoreRepaint(boolean ignoreRepaint) { }
        @NotModified boolean getIgnoreRepaint() { return false; }
        boolean contains(int x, int y) { return false; }
        boolean inside(int x, int y) { return false; }
        boolean contains(Point p) { return false; }
        @NotModified Component getComponentAt(int x, int y) { return null; }
        Component locate(int x, int y) { return null; }
        @NotModified Component getComponentAt(Point p) { return null; }
        void deliverEvent(Event e) { }
        void dispatchEvent(AWTEvent e) { }
        //override from java.awt.MenuContainer
        boolean postEvent(Event e) { return false; }
        @Modified void addComponentListener(ComponentListener l) { }
        @Modified void removeComponentListener(ComponentListener l) { }
        @NotModified ComponentListener [] getComponentListeners() { return null; }
        @Modified void addFocusListener(FocusListener l) { }
        @Modified void removeFocusListener(FocusListener l) { }
        @NotModified FocusListener [] getFocusListeners() { return null; }
        @Modified void addHierarchyListener(HierarchyListener l) { }
        @Modified void removeHierarchyListener(HierarchyListener l) { }
        @NotModified HierarchyListener [] getHierarchyListeners() { return null; }
        @Modified void addHierarchyBoundsListener(HierarchyBoundsListener l) { }
        @Modified void removeHierarchyBoundsListener(HierarchyBoundsListener l) { }
        @NotModified HierarchyBoundsListener [] getHierarchyBoundsListeners() { return null; }
        @Modified void addKeyListener(KeyListener l) { }
        @Modified void removeKeyListener(KeyListener l) { }
        @NotModified KeyListener [] getKeyListeners() { return null; }
        @Modified @Commutable void addMouseListener(MouseListener l) { }
        @Modified void removeMouseListener(MouseListener l) { }
        @NotModified MouseListener [] getMouseListeners() { return null; }
        @Modified void addMouseMotionListener(MouseMotionListener l) { }
        @Modified void removeMouseMotionListener(MouseMotionListener l) { }
        @NotModified MouseMotionListener [] getMouseMotionListeners() { return null; }
        @Modified void addMouseWheelListener(MouseWheelListener l) { }
        @Modified void removeMouseWheelListener(MouseWheelListener l) { }
        @NotModified MouseWheelListener [] getMouseWheelListeners() { return null; }
        @Modified void addInputMethodListener(InputMethodListener l) { }
        @Modified void removeInputMethodListener(InputMethodListener l) { }
        @NotModified InputMethodListener [] getInputMethodListeners() { return null; }
        @NotModified <T extends EventListener> T [] getListeners(Class<T> listenerType) { return null; }
        @NotModified InputMethodRequests getInputMethodRequests() { return null; }
        @NotModified InputContext getInputContext() { return null; }
        boolean handleEvent(Event evt) { return false; }
        boolean mouseDown(Event evt, int x, int y) { return false; }
        boolean mouseDrag(Event evt, int x, int y) { return false; }
        boolean mouseUp(Event evt, int x, int y) { return false; }
        boolean mouseMove(Event evt, int x, int y) { return false; }
        boolean mouseEnter(Event evt, int x, int y) { return false; }
        boolean mouseExit(Event evt, int x, int y) { return false; }
        boolean keyDown(Event evt, int key) { return false; }
        boolean keyUp(Event evt, int key) { return false; }
        boolean action(Event evt, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object what) {
            return false;
        }
        @Modified void addNotify() { }
        @Modified void removeNotify() { }
        boolean gotFocus(Event evt, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object what) {
            return false;
        }

        boolean lostFocus(Event evt, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object what) {
            return false;
        }
        @NotModified boolean isFocusTraversable() { return false; }
        @NotModified boolean isFocusable() { return false; }
        @Modified void setFocusable(boolean focusable) { }
        @Modified void setFocusTraversalKeys(int id, @NotModified Set<? extends AWTKeyStroke> keystrokes) { }
        @NotModified Set<AWTKeyStroke> getFocusTraversalKeys(int id) { return null; }
        boolean areFocusTraversalKeysSet(int id) { return false; }
        @Modified void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) { }
        @NotModified boolean getFocusTraversalKeysEnabled() { return false; }
        void requestFocus() { }
        void requestFocus(FocusEvent.Cause cause) { }
        boolean requestFocusInWindow() { return false; }
        boolean requestFocusInWindow(FocusEvent.Cause cause) { return false; }
        @NotModified Container getFocusCycleRootAncestor() { return null; }
        @NotModified boolean isFocusCycleRoot(Container container) { return false; }
        @Modified void transferFocus() { }
        @Modified void nextFocus() { }
        @Modified void transferFocusBackward() { }
        @Modified void transferFocusUpCycle() { }
        @NotModified boolean hasFocus() { return false; }
        @NotModified boolean isFocusOwner() { return false; }
        @Modified void add(PopupMenu popup) { }
        //override from java.awt.MenuContainer
        @Modified void remove(MenuComponent popup) { }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        void list() { }
        void list(PrintStream out) { }
        void list(PrintStream out, int indent) { }
        void list(PrintWriter out) { }
        void list(PrintWriter out, int indent) { }
        @Modified void addPropertyChangeListener(PropertyChangeListener listener) { }
        @Modified void removePropertyChangeListener(PropertyChangeListener listener) { }
        @NotModified PropertyChangeListener [] getPropertyChangeListeners() { return null; }
        @Modified void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
        @Modified void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
        @NotModified PropertyChangeListener [] getPropertyChangeListeners(String propertyName) { return null; }
        void firePropertyChange(String propertyName, byte oldValue, byte newValue) { }
        void firePropertyChange(String propertyName, char oldValue, char newValue) { }
        void firePropertyChange(String propertyName, short oldValue, short newValue) { }
        void firePropertyChange(String propertyName, long oldValue, long l) { }
        void firePropertyChange(String propertyName, float oldValue, float newValue) { }
        void firePropertyChange(String propertyName, double oldValue, double d) { }
        @Modified void setComponentOrientation(ComponentOrientation o) { }
        @NotModified ComponentOrientation getComponentOrientation() { return null; }
        void applyComponentOrientation(ComponentOrientation orientation) { }
        @NotModified AccessibleContext getAccessibleContext() { return null; }
        @Modified void setMixingCutoutShape(Shape shape) { }
    }

    //public class Container extends Component
    class Container$ {
        Container$() { }
        @NotModified int getComponentCount() { return 0; }
        int countComponents() { return 0; }
        @NotModified Component getComponent(int n) { return null; }
        @NotModified Component [] getComponents() { return null; }
        @NotModified Insets getInsets() { return null; }
        Insets insets() { return null; }
        // FIXME is this correct?
        @Modified @Commutable
        Component add(Component comp) { return null; }
        @Modified Component add(String name, Component comp) { return null; }
        @Modified Component add(Component comp, int index) { return null; }
        @Modified void setComponentZOrder(Component comp, int index) { }
        @NotModified int getComponentZOrder(Component comp) { return 0; }
        @Modified @Commutable
        void add(Component comp, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object constraints) { }

        @Modified void add(
            Component comp,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object constraints,
            int index) { }
        @Modified void remove(int index) { }
        @Modified void remove(Component comp) { }
        @Modified void removeAll() { }
        @NotModified LayoutManager getLayout() { return null; }
        @Modified @Commutable void setLayout(LayoutManager mgr) { }
        //override from java.awt.Component
        void doLayout() { }

        //override from java.awt.Component
        void layout() { }
        @NotModified boolean isValidateRoot() { return false; }
        //override from java.awt.Component
        void invalidate() { }

        //override from java.awt.Component
        void validate() { }

        //override from java.awt.Component
        @Modified void setFont(Font f) { }

        //override from java.awt.Component
        @NotModified Dimension getPreferredSize() { return null; }

        //override from java.awt.Component
        Dimension preferredSize() { return null; }

        //override from java.awt.Component
        @NotModified Dimension getMinimumSize() { return null; }

        //override from java.awt.Component
        Dimension minimumSize() { return null; }

        //override from java.awt.Component
        @NotModified Dimension getMaximumSize() { return null; }

        //override from java.awt.Component
        @NotModified float getAlignmentX() { return 0.0F; }

        //override from java.awt.Component
        @NotModified float getAlignmentY() { return 0.0F; }

        //override from java.awt.Component
        void paint(Graphics g) { }

        //override from java.awt.Component
        @Modified void update(Graphics g) { }

        //override from java.awt.Component
        void print(Graphics g) { }
        void paintComponents(Graphics g) { }
        void printComponents(Graphics g) { }
        @Modified void addContainerListener(ContainerListener l) { }
        @Modified void removeContainerListener(ContainerListener l) { }
        @NotModified ContainerListener [] getContainerListeners() { return null; }
        //override from java.awt.Component
        @NotModified <T extends EventListener> T [] getListeners(Class<T> listenerType) { return null; }

        //override from java.awt.Component
        void deliverEvent(Event e) { }

        //override from java.awt.Component
        @NotModified Component getComponentAt(int x, int y) { return null; }

        //override from java.awt.Component
        Component locate(int x, int y) { return null; }

        //override from java.awt.Component
        @NotModified Component getComponentAt(Point p) { return null; }
        @NotModified Point getMousePosition(boolean allowChildren) { return null; }
        Component findComponentAt(int x, int y) { return null; }
        Component findComponentAt(Point p) { return null; }
        //override from java.awt.Component
        @Modified void addNotify() { }

        //override from java.awt.Component
        @Modified void removeNotify() { }
        @NotModified boolean isAncestorOf(Component c) { return false; }
        //override from java.awt.Component
        void list(PrintStream out, int indent) { }

        //override from java.awt.Component
        void list(PrintWriter out, int indent) { }

        //override from java.awt.Component
        @Modified void setFocusTraversalKeys(int id, @NotModified Set<? extends AWTKeyStroke> keystrokes) { }

        //override from java.awt.Component
        @NotModified Set<AWTKeyStroke> getFocusTraversalKeys(int id) { return null; }

        //override from java.awt.Component
        boolean areFocusTraversalKeysSet(int id) { return false; }

        //override from java.awt.Component
        @NotModified boolean isFocusCycleRoot(Container container) { return false; }
        @Modified void setFocusTraversalPolicy(FocusTraversalPolicy policy) { }
        @NotModified FocusTraversalPolicy getFocusTraversalPolicy() { return null; }
        @NotModified boolean isFocusTraversalPolicySet() { return false; }
        @Modified void setFocusCycleRoot(boolean focusCycleRoot) { }
        @NotModified boolean isFocusCycleRoot() { return false; }
        @Modified void setFocusTraversalPolicyProvider(boolean provider) { }
        @NotModified boolean isFocusTraversalPolicyProvider() { return false; }
        @Modified void transferFocusDownCycle() { }
        //override from java.awt.Component
        void applyComponentOrientation(ComponentOrientation o) { }

        //override from java.awt.Component
        @Modified void addPropertyChangeListener(PropertyChangeListener listener) { }

        //override from java.awt.Component
        @Modified void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
    }

    //public abstract class Graphics
    class Graphics$ {
        Graphics create() { return null; }
        Graphics create(int x, int y, int width, int height) { return null; }
        void translate(int i, int i1) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotModified Color getColor() { return null; }
        @Modified void setColor(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color) { }
        @Modified void setPaintMode() { }
        @Modified void setXORMode(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color) { }
        @GetSet
        @NotModified Font getFont() { return null; }
        @Modified @GetSet
        void setFont(Font font) { }
        @NotModified FontMetrics getFontMetrics() { return null; }
        @NotModified FontMetrics getFontMetrics(Font font) { return null; }
        @NotModified Rectangle getClipBounds() { return null; }
        void clipRect(int i, int i1, int i2, int i3) { }
        @Modified void setClip(int i, int i1, int i2, int i3) { }
        @GetSet
        @NotModified Shape getClip() { return null; }
        @Modified @GetSet
        void setClip(Shape shape) { }
        void copyArea(int i, int i1, int i2, int i3, int i4, int i5) { }
        void drawLine(int i, int i1, int i2, int i3) { }
        @Modified void fillRect(int i, int i1, int i2, int i3) { }
        void drawRect(int x, int y, int width, int height) { }
        @Modified void clearRect(int i, int i1, int i2, int i3) { }
        void drawRoundRect(int i, int i1, int i2, int i3, int i4, int i5) { }
        @Modified void fillRoundRect(int i, int i1, int i2, int i3, int i4, int i5) { }
        void draw3DRect(int x, int y, int width, int height, boolean raised) { }
        @Modified void fill3DRect(int x, int y, int width, int height, boolean raised) { }
        void drawOval(int i, int i1, int i2, int i3) { }
        @Modified void fillOval(int i, int i1, int i2, int i3) { }
        void drawArc(int i, int i1, int i2, int i3, int i4, int i5) { }
        @Modified void fillArc(int i, int i1, int i2, int i3, int i4, int i5) { }
        void drawPolyline(int [] i, int [] i1, int i2) { }
        void drawPolygon(int [] i, int [] i1, int i2) { }
        void drawPolygon(Polygon p) { }
        @Modified void fillPolygon(int [] i, int [] i1, int i2) { }
        @Modified void fillPolygon(Polygon p) { }
        void drawString(String string, int i, int i1) { }
        void drawString(AttributedCharacterIterator attributedCharacterIterator, int i, int i1) { }
        void drawChars(char [] data, int offset, int length, int x, int y) { }
        void drawBytes(byte [] data, int offset, int length, int x, int y) { }
        boolean drawImage(Image image, int i, int i1, ImageObserver imageObserver) { return false; }
        boolean drawImage(Image image, int i, int i1, int i2, int i3, ImageObserver imageObserver) { return false; }
        boolean drawImage(
            Image image,
            int i,
            int i1,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color,
            ImageObserver imageObserver) { return false; }

        boolean drawImage(
            Image image,
            int i,
            int i1,
            int i2,
            int i3,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color,
            ImageObserver imageObserver) { return false; }

        boolean drawImage(
            Image image,
            int i,
            int i1,
            int i2,
            int i3,
            int i4,
            int i5,
            int i6,
            int i7,
            ImageObserver imageObserver) { return false; }

        boolean drawImage(
            Image image,
            int i,
            int i1,
            int i2,
            int i3,
            int i4,
            int i5,
            int i6,
            int i7,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color,
            ImageObserver imageObserver) { return false; }
        void dispose() { }
        //override from java.lang.Object
        protected void finalize() { }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified Rectangle getClipRect() { return null; }
        boolean hitClip(int x, int y, int width, int height) { return false; }
        @NotModified Rectangle getClipBounds(Rectangle r) { return null; }
    }

    class ItemSelectable$ {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addItemListener(java.awt.event.ItemListener p0) { }
        @Modified void removeItemListener(java.awt.event.ItemListener p0) { }
    }

    class MenuContainer$ {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void remove(java.awt.MenuComponent p0) { }
    }
}
