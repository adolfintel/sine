/*
 * Copyright (C) 2014 Federico Dossena
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dosse.bwentrain.player;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author dosse
 */
public class Utils {

    private static final BufferedImage nullImage; //empty Image, mostly used for errors

    static {
        nullImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        nullImage.setRGB(0, 0, 0);
    }

    public static final void setFontRecursive(Component comp, Font font) {
        comp.setFont(font);
        if (comp instanceof Container) {
            Container c = (Container) comp;
            int componentCount = c.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                setFontRecursive(c.getComponent(i), font);
            }
        }
    }

    /**
     * loads font from classpath
     *
     * @param pathInClasspath path in classpath
     * @return Font or null if it doesn't exist
     */
    public static final Font loadFont(String pathInClasspath) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, Utils.class.getResourceAsStream(pathInClasspath));
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * load image from classpath as ImageIcon. doesn't scale it
     *
     * @param pathInClasspath path in classpath
     * @return ImageIcon, or an empty ImageIcon if something went wrong
     */
    public static final ImageIcon loadUnscaled(String pathInClasspath) {
        try {
            Image i = ImageIO.read(Utils.class.getResource(pathInClasspath));
            return new ImageIcon(i);
        } catch (IOException ex) {
            return new ImageIcon(nullImage);
        }
    }

    /**
     * load image from classpath as ImageIcon and scales it to WxH
     *
     * @param pathInClasspath path in classpath
     * @param w width
     * @param h height
     * @return ImageIcon, or an empty ImageIcon of dimension WxH if something
     * went wrong
     */
    public static final ImageIcon loadAndScale(String pathInClasspath, int w, int h) {
        try {
            Image i = ImageIO.read(Utils.class.getResource(pathInClasspath));
            return new ImageIcon(i.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Throwable ex) {
            return new ImageIcon(nullImage.getScaledInstance(w, h, Image.SCALE_FAST));
        }
    }
    /**
     * recursively scans a Container to find all Objects of a given class
     * @param c container to scan
     * @param cl class to find
     * @return arraylist with matching Objects (will use generics ASAP)
     */
    public static final ArrayList recursivelyScanComponentAndReturnComponentsOfGivenClass(Container c, Class cl) {
        ArrayList matches = new ArrayList<>();
        for (Component x : c.getComponents()) {
            if (cl.isInstance(x)) {
                matches.add(x);
            }
            if (x instanceof Container) {
                matches.addAll(recursivelyScanComponentAndReturnComponentsOfGivenClass((Container) x, cl));
            }
        }
        return matches;
    }
    private static final ResourceBundle locBundle = ResourceBundle.getBundle("com/dosse/bwentrain/player/locale");

    /**
     * returns localized string
     *
     * @param s key
     * @return localized String, or null the key doesn't exist
     */
    public static final String getLocString(String s) {
        return locBundle.getString(s);
    }

    /**
     * opens an URI in the default browser
     *
     * @param uri URI to browse
     * @throws Exception if something goes wrong
     */
    public static void openInBrowser(URI uri) throws Exception {
        if(!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) throw new Exception("Not supported");
        Desktop.getDesktop().browse(uri);
    }
}
