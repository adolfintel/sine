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

import java.awt.Color;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

/**
 * the about screen
 *
 * @author dosse
 */
public class AboutDialog extends JDialog {

    private static final int DIALOG_WIDTH = (int) (500f * Main.SCALE), DIALOG_HEIGHT = (int) (550f * Main.SCALE); //window size. as usual, there's all the code for resizing the window, but not the event listener

    private final JPanel panel; //container
    private TitleBar logo; //color-changing logo and draggable titlebar
    private final HTMLPanel scroll; //area with info
    private static Image unscaledLogo = Utils.loadUnscaled("/com/dosse/bwentrain/player/images/logoXL.png").getImage();
    private final DialogButton quit;

    private Timer t; //timer used for changing colors
    private float[] hsl = new float[3]; //also used for changing colors

    public AboutDialog() {
        super(new JFrame(), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setLayout(null);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        Color.RGBtoHSB(Main.BASE_COLOR.getRed(), Main.BASE_COLOR.getGreen(), Main.BASE_COLOR.getBlue(), hsl); //needed to initialize color changing
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new LineBorder(Main.WINDOW_BORDER));
        add(panel);
        logo = new TitleBar(this);
        logo.setOpaque(true);
        panel.add(logo);
        quit = new DialogButton(Utils.getLocString("ABOUT_QUIT")) { //close button

            @Override
            public void actionPerformed() {
                t.stop();
                dispose();
            }
        };
        addWindowListener(new WindowAdapter() { //on alt+f4, stop che color changing timer (prevents memory leak) then dispose (automatically)

            @Override
            public void windowClosing(WindowEvent e) {
                t.stop();
            }

        });
        panel.add(quit);
        scroll = new HTMLPanel();
        scroll.setHTML("<div style='font-family:" + Main.BASE_FONT.getFamily() + "; font-size:" + (int) (Main.BASE_FONT_PX * 0.8) + "px;'>" + Utils.getLocString("ABOUT_TEXT") + "</div>");
        scroll.setBorder(null);
        scroll.setForeground(Main.TEXT);
        scroll.setBackground(Main.DEFAULT_BACKGROUND);
        panel.add(scroll);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }

        });
        fixupLayout();
        t = new Timer(20, new ActionListener() { //technicolor!!!
            @Override
            public void actionPerformed(ActionEvent e) {
                hsl[0] = (hsl[0] + 0.001f) % 1;
                logo.setBackground(new Color(Color.HSBtoRGB(hsl[0], hsl[1], hsl[2])));

            }
        });
        t.setRepeats(true);
        t.start();
        //create listener for keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if(!isFocused()) return false;
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED) { //esc = close
                    quit.actionPerformed();
                }
                return false;
            }
        });
    }

    private void fixupLayout() {
        panel.setBounds(0, 0, getWidth(), getHeight()); //strech the container over the entire window
        //scale logo and add inside container
        float logoAR = (float) unscaledLogo.getHeight(null) / (float) unscaledLogo.getWidth(null);
        int logoW = getWidth() - 2, logoH = (int) (logoW * logoAR);
        logo.setIcon(new ImageIcon(unscaledLogo.getScaledInstance(logoW, logoH, Image.SCALE_SMOOTH)));
        logo.setBounds(1, 1, logoW, logoH);
        quit.setBounds(getWidth() - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_WIDTH, getHeight() - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT); //close button in lower-right corner
        scroll.setBounds(1 + Main.GENERIC_MARGIN, logo.getX() + logo.getHeight() + Main.GENERIC_MARGIN, getWidth() - 2 - 2 * Main.GENERIC_MARGIN, quit.getY() - logo.getX() - logo.getHeight() - 2 * Main.GENERIC_MARGIN); //scrollable text in between
        repaint(); //must force repaint on some systems
    }

    //creates dialog and waits for it to be closed
    public static void about() {
        AboutDialog d = new AboutDialog();
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - DIALOG_WIDTH / 2), y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - DIALOG_HEIGHT / 2);
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

}
