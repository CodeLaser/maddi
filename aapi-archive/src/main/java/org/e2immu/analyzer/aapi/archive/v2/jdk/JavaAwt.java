package org.e2immu.analyzer.aapi.archive.v2.jdk;
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
import org.e2immu.annotation.Commutable;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.method.GetSet;

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
        int getRed() { return 0; }

        //@NotModified[T]
        int getGreen() { return 0; }

        //@NotModified[T]
        int getBlue() { return 0; }

        //@NotModified[T]
        int getAlpha() { return 0; }

        //@NotModified[T]
        int getRGB() { return 0; }

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
        static Color getColor(String nm) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static Color getColor(String nm, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color v) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static Color getColor(String nm, int v) { return null; }

        //@NotModified[T]
        static int HSBtoRGB(float hue, float saturation, float brightness) { return 0; }

        //@Independent[T] @NotModified[T]
        static float [] RGBtoHSB(int r, int g, int b, /*@Independent[M]*/ float [] hsbvals) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static Color getHSBColor(float h, float s, float b) { return null; }

        //@NotModified[T]
        @Independent
        float [] getRGBComponents(@Independent(dependentReturnValue = true) float [] compArray) { return null; }

        //@NotModified[T]
        @Independent
        float [] getRGBColorComponents(@Independent(dependentReturnValue = true) float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        float [] getComponents(/*@Independent[M]*/ float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        float [] getColorComponents(/*@Independent[M]*/ float [] compArray) { return null; }

        //@Independent(hc=true)[O] @NotModified[T]
        float [] getComponents(/*@Independent[M]*/ ColorSpace cspace, /*@Independent[M]*/ float [] compArray) {
            return null;
        }

        //@Independent(hc=true)[O] @NotModified[T]
        float [] getColorComponents(/*@Independent[M]*/ ColorSpace cspace, /*@Independent[M]*/ float [] compArray) {
            return null;
        }

        //@Independent(hc=true)[O] @NotModified[T]
        ColorSpace getColorSpace() { return null; }

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
        int getTransparency() { return 0; }
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
            BufferCapabilities getCapabilities() { return null; }

            //override from java.awt.image.BufferStrategy
            Graphics getDrawGraphics() { return null; }

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
            BufferCapabilities getCapabilities() { return null; }

            //override from java.awt.image.BufferStrategy
            Graphics getDrawGraphics() { return null; }

            //override from java.awt.image.BufferStrategy
            boolean contentsLost() { return false; }

            //override from java.awt.image.BufferStrategy
            boolean contentsRestored() { return false; }

            //override from java.awt.image.BufferStrategy
            void show() { }

            //override from java.awt.image.BufferStrategy
            void dispose() { }
        }
        String getName() { return null; }
        void setName(String name) { }
        Container getParent() { return null; }
        void setDropTarget(DropTarget dt) { }
        DropTarget getDropTarget() { return null; }
        GraphicsConfiguration getGraphicsConfiguration() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getTreeLock() { return null; }
        Toolkit getToolkit() { return null; }
        boolean isValid() { return false; }
        boolean isDisplayable() { return false; }
        boolean isVisible() { return false; }
        Point getMousePosition() { return null; }
        boolean isShowing() { return false; }
        boolean isEnabled() { return false; }
        void setEnabled(boolean b) { }
        void enable() { }
        void enable(boolean b) { }
        void disable() { }
        boolean isDoubleBuffered() { return false; }
        void enableInputMethods(boolean enable) { }
        void setVisible(boolean b) { }
        void show() { }
        void show(boolean b) { }
        void hide() { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Color getForeground() { return null; }
        void setForeground(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color c) { }
        boolean isForegroundSet() { return false; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Color getBackground() { return null; }
        void setBackground(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color c) { }
        boolean isBackgroundSet() { return false; }
        //override from java.awt.MenuContainer
        Font getFont() { return null; }
        void setFont(Font f) { }
        boolean isFontSet() { return false; }
        Locale getLocale() { return null; }
        void setLocale(Locale l) { }
        ColorModel getColorModel() { return null; }
        Point getLocation() { return null; }
        Point getLocationOnScreen() { return null; }
        Point location() { return null; }
        void setLocation(int x, int y) { }
        void move(int x, int y) { }
        void setLocation(Point p) { }
        Dimension getSize() { return null; }
        Dimension size() { return null; }
        void setSize(int width, int height) { }
        void resize(int width, int height) { }
        void setSize(Dimension d) { }
        void resize(Dimension d) { }
        Rectangle getBounds() { return null; }
        Rectangle bounds() { return null; }
        void setBounds(int x, int y, int width, int height) { }
        void reshape(int x, int y, int width, int height) { }
        void setBounds(Rectangle r) { }
        int getX() { return 0; }
        int getY() { return 0; }
        int getWidth() { return 0; }
        int getHeight() { return 0; }
        Rectangle getBounds(Rectangle rv) { return null; }
        Dimension getSize(Dimension rv) { return null; }
        Point getLocation(Point rv) { return null; }
        boolean isOpaque() { return false; }
        boolean isLightweight() { return false; }
        void setPreferredSize(Dimension preferredSize) { }
        boolean isPreferredSizeSet() { return false; }
        Dimension getPreferredSize() { return null; }
        Dimension preferredSize() { return null; }
        void setMinimumSize(Dimension minimumSize) { }
        boolean isMinimumSizeSet() { return false; }
        Dimension getMinimumSize() { return null; }
        Dimension minimumSize() { return null; }
        void setMaximumSize(Dimension maximumSize) { }
        boolean isMaximumSizeSet() { return false; }
        Dimension getMaximumSize() { return null; }
        float getAlignmentX() { return 0.0F; }
        float getAlignmentY() { return 0.0F; }
        int getBaseline(int width, int height) { return 0; }
        Component.BaselineResizeBehavior getBaselineResizeBehavior() { return null; }
        void doLayout() { }
        void layout() { }
        void validate() { }
        void invalidate() { }
        void revalidate() { }
        Graphics getGraphics() { return null; }
        FontMetrics getFontMetrics(Font font) { return null; }
        void setCursor(Cursor cursor) { }
        Cursor getCursor() { return null; }
        boolean isCursorSet() { return false; }
        void paint(Graphics g) { }
        void update(Graphics g) { }
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
        void setIgnoreRepaint(boolean ignoreRepaint) { }
        boolean getIgnoreRepaint() { return false; }
        boolean contains(int x, int y) { return false; }
        boolean inside(int x, int y) { return false; }
        boolean contains(Point p) { return false; }
        Component getComponentAt(int x, int y) { return null; }
        Component locate(int x, int y) { return null; }
        Component getComponentAt(Point p) { return null; }
        void deliverEvent(Event e) { }
        void dispatchEvent(AWTEvent e) { }
        //override from java.awt.MenuContainer
        boolean postEvent(Event e) { return false; }
        void addComponentListener(ComponentListener l) { }
        void removeComponentListener(ComponentListener l) { }
        ComponentListener [] getComponentListeners() { return null; }
        void addFocusListener(FocusListener l) { }
        void removeFocusListener(FocusListener l) { }
        FocusListener [] getFocusListeners() { return null; }
        void addHierarchyListener(HierarchyListener l) { }
        void removeHierarchyListener(HierarchyListener l) { }
        HierarchyListener [] getHierarchyListeners() { return null; }
        void addHierarchyBoundsListener(HierarchyBoundsListener l) { }
        void removeHierarchyBoundsListener(HierarchyBoundsListener l) { }
        HierarchyBoundsListener [] getHierarchyBoundsListeners() { return null; }
        void addKeyListener(KeyListener l) { }
        void removeKeyListener(KeyListener l) { }
        KeyListener [] getKeyListeners() { return null; }
        @Commutable void addMouseListener(MouseListener l) { }
        void removeMouseListener(MouseListener l) { }
        MouseListener [] getMouseListeners() { return null; }
        void addMouseMotionListener(MouseMotionListener l) { }
        void removeMouseMotionListener(MouseMotionListener l) { }
        MouseMotionListener [] getMouseMotionListeners() { return null; }
        void addMouseWheelListener(MouseWheelListener l) { }
        void removeMouseWheelListener(MouseWheelListener l) { }
        MouseWheelListener [] getMouseWheelListeners() { return null; }
        void addInputMethodListener(InputMethodListener l) { }
        void removeInputMethodListener(InputMethodListener l) { }
        InputMethodListener [] getInputMethodListeners() { return null; }
        <T extends EventListener> T [] getListeners(Class<T> listenerType) { return null; }
        InputMethodRequests getInputMethodRequests() { return null; }
        InputContext getInputContext() { return null; }
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
        void addNotify() { }
        void removeNotify() { }
        boolean gotFocus(Event evt, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object what) {
            return false;
        }

        boolean lostFocus(Event evt, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object what) {
            return false;
        }
        boolean isFocusTraversable() { return false; }
        boolean isFocusable() { return false; }
        void setFocusable(boolean focusable) { }
        void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) { }
        Set<AWTKeyStroke> getFocusTraversalKeys(int id) { return null; }
        boolean areFocusTraversalKeysSet(int id) { return false; }
        void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) { }
        boolean getFocusTraversalKeysEnabled() { return false; }
        void requestFocus() { }
        void requestFocus(FocusEvent.Cause cause) { }
        boolean requestFocusInWindow() { return false; }
        boolean requestFocusInWindow(FocusEvent.Cause cause) { return false; }
        Container getFocusCycleRootAncestor() { return null; }
        boolean isFocusCycleRoot(Container container) { return false; }
        void transferFocus() { }
        void nextFocus() { }
        void transferFocusBackward() { }
        void transferFocusUpCycle() { }
        boolean hasFocus() { return false; }
        boolean isFocusOwner() { return false; }
        void add(PopupMenu popup) { }
        //override from java.awt.MenuContainer
        void remove(MenuComponent popup) { }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        void list() { }
        void list(PrintStream out) { }
        void list(PrintStream out, int indent) { }
        void list(PrintWriter out) { }
        void list(PrintWriter out, int indent) { }
        void addPropertyChangeListener(PropertyChangeListener listener) { }
        void removePropertyChangeListener(PropertyChangeListener listener) { }
        PropertyChangeListener [] getPropertyChangeListeners() { return null; }
        void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
        void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
        PropertyChangeListener [] getPropertyChangeListeners(String propertyName) { return null; }
        void firePropertyChange(String propertyName, byte oldValue, byte newValue) { }
        void firePropertyChange(String propertyName, char oldValue, char newValue) { }
        void firePropertyChange(String propertyName, short oldValue, short newValue) { }
        void firePropertyChange(String propertyName, long oldValue, long l) { }
        void firePropertyChange(String propertyName, float oldValue, float newValue) { }
        void firePropertyChange(String propertyName, double oldValue, double d) { }
        void setComponentOrientation(ComponentOrientation o) { }
        ComponentOrientation getComponentOrientation() { return null; }
        void applyComponentOrientation(ComponentOrientation orientation) { }
        AccessibleContext getAccessibleContext() { return null; }
        void setMixingCutoutShape(Shape shape) { }
    }

    //public class Container extends Component
    class Container$ {
        Container$() { }
        int getComponentCount() { return 0; }
        int countComponents() { return 0; }
        Component getComponent(int n) { return null; }
        Component [] getComponents() { return null; }
        Insets getInsets() { return null; }
        Insets insets() { return null; }
        // FIXME is this correct?
        @Commutable
        Component add(Component comp) { return null; }
        Component add(String name, Component comp) { return null; }
        Component add(Component comp, int index) { return null; }
        void setComponentZOrder(Component comp, int index) { }
        int getComponentZOrder(Component comp) { return 0; }
        @Commutable
        void add(Component comp, /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object constraints) { }

        void add(
            Component comp,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object constraints,
            int index) { }
        void remove(int index) { }
        void remove(Component comp) { }
        void removeAll() { }
        LayoutManager getLayout() { return null; }
        @Commutable void setLayout(LayoutManager mgr) { }
        //override from java.awt.Component
        void doLayout() { }

        //override from java.awt.Component
        void layout() { }
        boolean isValidateRoot() { return false; }
        //override from java.awt.Component
        void invalidate() { }

        //override from java.awt.Component
        void validate() { }

        //override from java.awt.Component
        void setFont(Font f) { }

        //override from java.awt.Component
        Dimension getPreferredSize() { return null; }

        //override from java.awt.Component
        Dimension preferredSize() { return null; }

        //override from java.awt.Component
        Dimension getMinimumSize() { return null; }

        //override from java.awt.Component
        Dimension minimumSize() { return null; }

        //override from java.awt.Component
        Dimension getMaximumSize() { return null; }

        //override from java.awt.Component
        float getAlignmentX() { return 0.0F; }

        //override from java.awt.Component
        float getAlignmentY() { return 0.0F; }

        //override from java.awt.Component
        void paint(Graphics g) { }

        //override from java.awt.Component
        void update(Graphics g) { }

        //override from java.awt.Component
        void print(Graphics g) { }
        void paintComponents(Graphics g) { }
        void printComponents(Graphics g) { }
        void addContainerListener(ContainerListener l) { }
        void removeContainerListener(ContainerListener l) { }
        ContainerListener [] getContainerListeners() { return null; }
        //override from java.awt.Component
        <T extends EventListener> T [] getListeners(Class<T> listenerType) { return null; }

        //override from java.awt.Component
        void deliverEvent(Event e) { }

        //override from java.awt.Component
        Component getComponentAt(int x, int y) { return null; }

        //override from java.awt.Component
        Component locate(int x, int y) { return null; }

        //override from java.awt.Component
        Component getComponentAt(Point p) { return null; }
        Point getMousePosition(boolean allowChildren) { return null; }
        Component findComponentAt(int x, int y) { return null; }
        Component findComponentAt(Point p) { return null; }
        //override from java.awt.Component
        void addNotify() { }

        //override from java.awt.Component
        void removeNotify() { }
        boolean isAncestorOf(Component c) { return false; }
        //override from java.awt.Component
        void list(PrintStream out, int indent) { }

        //override from java.awt.Component
        void list(PrintWriter out, int indent) { }

        //override from java.awt.Component
        void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) { }

        //override from java.awt.Component
        Set<AWTKeyStroke> getFocusTraversalKeys(int id) { return null; }

        //override from java.awt.Component
        boolean areFocusTraversalKeysSet(int id) { return false; }

        //override from java.awt.Component
        boolean isFocusCycleRoot(Container container) { return false; }
        void setFocusTraversalPolicy(FocusTraversalPolicy policy) { }
        FocusTraversalPolicy getFocusTraversalPolicy() { return null; }
        boolean isFocusTraversalPolicySet() { return false; }
        void setFocusCycleRoot(boolean focusCycleRoot) { }
        boolean isFocusCycleRoot() { return false; }
        void setFocusTraversalPolicyProvider(boolean provider) { }
        boolean isFocusTraversalPolicyProvider() { return false; }
        void transferFocusDownCycle() { }
        //override from java.awt.Component
        void applyComponentOrientation(ComponentOrientation o) { }

        //override from java.awt.Component
        void addPropertyChangeListener(PropertyChangeListener listener) { }

        //override from java.awt.Component
        void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { }
    }

    //public abstract class Graphics
    class Graphics$ {
        Graphics create() { return null; }
        Graphics create(int x, int y, int width, int height) { return null; }
        void translate(int i, int i1) { }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Color getColor() { return null; }
        void setColor(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color) { }
        void setPaintMode() { }
        void setXORMode(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Color color) { }
        @GetSet
        Font getFont() { return null; }
        @GetSet
        void setFont(Font font) { }
        FontMetrics getFontMetrics() { return null; }
        FontMetrics getFontMetrics(Font font) { return null; }
        Rectangle getClipBounds() { return null; }
        void clipRect(int i, int i1, int i2, int i3) { }
        void setClip(int i, int i1, int i2, int i3) { }
        @GetSet
        Shape getClip() { return null; }
        @GetSet
        void setClip(Shape shape) { }
        void copyArea(int i, int i1, int i2, int i3, int i4, int i5) { }
        void drawLine(int i, int i1, int i2, int i3) { }
        void fillRect(int i, int i1, int i2, int i3) { }
        void drawRect(int x, int y, int width, int height) { }
        void clearRect(int i, int i1, int i2, int i3) { }
        void drawRoundRect(int i, int i1, int i2, int i3, int i4, int i5) { }
        void fillRoundRect(int i, int i1, int i2, int i3, int i4, int i5) { }
        void draw3DRect(int x, int y, int width, int height, boolean raised) { }
        void fill3DRect(int x, int y, int width, int height, boolean raised) { }
        void drawOval(int i, int i1, int i2, int i3) { }
        void fillOval(int i, int i1, int i2, int i3) { }
        void drawArc(int i, int i1, int i2, int i3, int i4, int i5) { }
        void fillArc(int i, int i1, int i2, int i3, int i4, int i5) { }
        void drawPolyline(int [] i, int [] i1, int i2) { }
        void drawPolygon(int [] i, int [] i1, int i2) { }
        void drawPolygon(Polygon p) { }
        void fillPolygon(int [] i, int [] i1, int i2) { }
        void fillPolygon(Polygon p) { }
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
        Rectangle getClipRect() { return null; }
        boolean hitClip(int x, int y, int width, int height) { return false; }
        Rectangle getClipBounds(Rectangle r) { return null; }
    }
}
