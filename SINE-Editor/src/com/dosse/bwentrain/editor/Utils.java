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
package com.dosse.bwentrain.editor;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * some common functions used throughout the application
 *
 * @author dosse
 */
public class Utils {

    /**
     * converts time to an HH:MM:SS String
     *
     * @param t time in seconds
     * @return HMS String
     */
    public static final String toHMS(float t) {
        int h = (int) (t / 3600);
        t %= 3600;
        int m = (int) (t / 60);
        t %= 60;
        int s = (int) t;
        return "" + (h < 10 ? ("0" + h) : h) + ":" + (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s); //bloody hell, code salad
    }

    private static final BufferedImage nullImage; //empty Image, mostly used for errors

    static {
        nullImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        nullImage.setRGB(0, 0, 0);
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
     * load image from classpath as ImageIcon and scales it according to screen DPI
     *
     * @param pathInClasspath path in classpath
     * @return ImageIcon, or an empty ImageIcon of dimension WxH if something
     * went wrong
     */
    public static final ImageIcon loadAndScaleImage(String pathInClasspath) {
        try {
            Image i = ImageIO.read(Utils.class.getResource(pathInClasspath));
            return new ImageIcon(i.getScaledInstance((int)(i.getWidth(null)*Main.SCALE),(int)(i.getHeight(null)*Main.SCALE), Image.SCALE_SMOOTH));
        } catch (Throwable ex) {
            return new ImageIcon(nullImage);
        }
    }

    private static final ResourceBundle locBundle = ResourceBundle.getBundle("com/dosse/bwentrain/editor/locale");

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

    /**
     * loads default Sans Serif font. If not available, loads the first font
     * available
     *
     * @return loaded font
     */
    public static Font findSansSerifFont() {
        try {
            return new Font("SansSerif", Font.PLAIN, (int) Main.TEXT_SIZE); //try to load default Sans Serif font
        } catch (Throwable t) {
            //something went wrong, use the first font available
            GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] families = e.getAvailableFontFamilyNames();
            return new Font(families[0], Font.PLAIN, (int) Main.TEXT_SIZE);
        }
    }
    
    /**
     * loads default Monospaced font. If not available, loads the first font
     * available
     *
     * @return loaded font
     */
    public static Font findMonospacedFont() {
        try {
            return new Font("Monospaced", Font.PLAIN, (int) Main.TEXT_SIZE); //try to load default Sans Serif font
        } catch (Throwable t) {
            //something went wrong, use the first font available
            GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] families = e.getAvailableFontFamilyNames();
            return new Font(families[0], Font.PLAIN, (int) Main.TEXT_SIZE);
        }
    }
}
