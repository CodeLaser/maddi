package org.e2immu.analyzer.aapi.archive.jdk;
import org.e2immu.annotation.Commutable;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.method.GetSet;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.AttributedCharacterIterator;

public class JavaAwt {
    public static final String PACKAGE_NAME = "java.awt";

    interface Container$ {
        @Commutable
        @Modified
        Component add(Component comp);

        @Commutable
        @Modified
        void add(Component comp, Object constraints);

        @Commutable
        @Modified
        void setLayout(LayoutManager mgr);
    }

    interface Component$ {
        @Commutable
        @Modified
        void addMouseListener(MouseListener l);
    }

    @Immutable(hc = true)
    class Color$ {
        static final Color white = null;
        static final Color WHITE = null;
        static final Color lightGray = null;
        static final Color LIGHT_GRAY = null;
        static final Color gray = null;
        static final Color GRAY = null;
        static final Color darkGray = null;
        static final Color DARK_GRAY = null;
        static final Color black = null;
        static final Color BLACK = null;
        static final Color red = null;
        static final Color RED = null;
        static final Color pink = null;
        static final Color PINK = null;
        static final Color orange = null;
        static final Color ORANGE = null;
        static final Color yellow = null;
        static final Color YELLOW = null;
        static final Color green = null;
        static final Color GREEN = null;
        static final Color magenta = null;
        static final Color MAGENTA = null;
        static final Color cyan = null;
        static final Color CYAN = null;
        static final Color blue = null;
        static final Color BLUE = null;
        Color$(int r, int g, int b) { }
        Color$(int r, int g, int b, int a) { }
        Color$(int rgb) { }
        Color$(int rgba, boolean hasalpha) { }
        Color$(float r, float g, float b) { }
        Color$(float r, float g, float b, float a) { }
        Color$(ColorSpace cspace, float[] components, float alpha) { }
        int getRed() { return 0; }
        int getGreen() { return 0; }
        int getBlue() { return 0; }
        int getAlpha() { return 0; }
        int getRGB() { return 0; }
        Color brighter() { return null; }
        Color darker() { return null; }
        public int hashCode() { return 0; }
        public boolean equals(Object obj) { return false; }
        public String toString() { return null; }
        static Color decode(String nm) { return null; }
        static Color getColor(String nm) { return null; }
        static Color getColor(String nm, Color v) { return null; }
        static Color getColor(String nm, int v) { return null; }
        static int HSBtoRGB(float hue, float saturation, float brightness) { return 0; }
        static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) { return null; }
        static Color getHSBColor(float h, float s, float b) { return null; }
        @Independent // independent of the Color object, but dependent on 'compArray'
        float[] getRGBComponents(@Independent(dependentReturnValue = true) float[] compArray) { return null; }
        @Independent
        float[] getRGBColorComponents(@Independent(dependentReturnValue = true) float[] compArray) { return null; }
        float[] getComponents(float[] compArray) { return null; }
        float[] getColorComponents(float[] compArray) { return null; }
        float[] getComponents(ColorSpace cspace, float[] compArray) { return null; }
        float[] getColorComponents(ColorSpace cspace, float[] compArray) { return null; }
        ColorSpace getColorSpace() { return null; }

        PaintContext createContext(
            ColorModel cm,
            Rectangle r,
            Rectangle2D r2d,
            AffineTransform xform,
            RenderingHints hints) { return null; }

        int getTransparency() { return 0; }
    }

    class Graphics$ {
        Graphics create() { return null; }
        Graphics create(int x, int y, int width, int height) { return null; }
        void translate(int i, int i1) { }
        Color getColor() { return null; }
        @Modified
        void setColor(Color color) { }
        @Modified
        void setPaintMode() { }
        @Modified
        void setXORMode(Color color) { }
        @GetSet
        Font getFont() { return null; }
        @GetSet
        void setFont(Font font) { }
        FontMetrics getFontMetrics() { return null; }
        FontMetrics getFontMetrics(Font font) { return null; }
        Rectangle getClipBounds() { return null; }
        @Modified
        void clipRect(int i, int i1, int i2, int i3) { }
        @Modified
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
        void drawPolyline(int[] i, int[] i1, int i2) { }
        void drawPolygon(int[] i, int[] i1, int i2) { }
        void drawPolygon(Polygon p) { }
        void fillPolygon(int[] i, int[] i1, int i2) { }
        void fillPolygon(Polygon p) { }
        void drawString(String string, int i, int i1) { }
        void drawString(AttributedCharacterIterator attributedCharacterIterator, int i, int i1) { }
        void drawChars(char[] data, int offset, int length, int x, int y) { }
        void drawBytes(byte[] data, int offset, int length, int x, int y) { }
        @Modified
        boolean drawImage(Image image, int i, int i1, ImageObserver imageObserver) { return false; }
        @Modified
        boolean drawImage(Image image, int i, int i1, int i2, int i3, ImageObserver imageObserver) { return false; }
        @Modified
        boolean drawImage(Image image, int i, int i1, Color color, ImageObserver imageObserver) { return false; }
        @Modified
        boolean drawImage(Image image, int i, int i1, int i2, int i3, Color color, ImageObserver imageObserver) {
            return false;
        }
        @Modified
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
        @Modified
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
            Color color,
            ImageObserver imageObserver) { return false; }

        @Modified
        void dispose() { }
        Rectangle getClipRect() { return null; }
        @Modified
        boolean hitClip(int x, int y, int width, int height) { return false; }
        Rectangle getClipBounds(Rectangle r) { return null; }
    }
}
