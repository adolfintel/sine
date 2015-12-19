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

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * dialog boxes (confirm, error and infos)
 *
 * @author dosse
 */
public class MessageBox extends JDialog {

    private static final int DIALOG_WIDTH = (int) (580f * Main.SCALE), DIALOG_HEIGHT = (int) (200f * Main.SCALE); //window size. as usual, there's all the code for resizing the window, but not the event listener
    private static final int DIALOG_ICON_WIDTH = (int) (84f * Main.SCALE), DIALOG_ICON_HEIGHT = (int) (84f * Main.SCALE);

    public static final int TYPE_INFORMATION = 0, TYPE_YES_NO = 1, TYPE_ERROR = 2; //types
    public static final int SELECTION_NULL = -1, SELECTION_OK = 0, SELECTION_YES = 1, SELECTION_NO = 2; //return values

    private static final ImageIcon ERROR_ICON = Utils.loadAndScale("/com/dosse/bwentrain/player/images/error.png", DIALOG_ICON_WIDTH, DIALOG_ICON_HEIGHT), INFO_ICON = Utils.loadAndScale("/com/dosse/bwentrain/player/images/info.png", DIALOG_ICON_WIDTH, DIALOG_ICON_HEIGHT), QUESTION_ICON = Utils.loadAndScale("/com/dosse/bwentrain/player/images/question.png", DIALOG_ICON_WIDTH, DIALOG_ICON_HEIGHT);

    private final JPanel panel; //container
    private final TitleBar titleBar;
    private final JLabel text;
    private final DialogButton b1, b2; //buttons at the bottom (b2 will be null unless it's a confirm (yes/no) dialog)

    private int selection = -1;

    public MessageBox(String html, final int type) {
        super(new JFrame(), true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLayout(null);
        addWindowListener(new WindowAdapter() { //alt+f4 = pressing no in confirm dialogs or ok in info/error dialogs

            @Override
            public void windowClosing(WindowEvent e) {
                selection = type == TYPE_YES_NO ? SELECTION_NO : SELECTION_OK;
                dispose();
            }
        });
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new LineBorder(Main.WINDOW_BORDER));
        add(panel);
        titleBar = new TitleBar(this);
        panel.add(titleBar);
        text = new JLabel();
        text.setForeground(Main.TEXT);
        text.setIcon(type == TYPE_ERROR ? ERROR_ICON : type == TYPE_YES_NO ? QUESTION_ICON : INFO_ICON);
        text.setText("<html>" + html + "</html>");
        panel.add(text);
        if (type == TYPE_ERROR || type == TYPE_INFORMATION) { //for error/info dialogs, show only OK button
            b1 = new DialogButton(Utils.getLocString("MBOX_OK")) {
                @Override
                public void actionPerformed() {
                    selection = SELECTION_OK;
                    dispose();
                }
            };
            panel.add(b1);
            b2 = null;
        } else { //for confirm dialogs show YES and NO buttons
            b1 = new DialogButton(Utils.getLocString("MBOX_NO")) {
                @Override
                public void actionPerformed() {
                    selection = SELECTION_NO;
                    dispose();
                }
            };
            panel.add(b1);
            b2 = new DialogButton(Utils.getLocString("MBOX_YES")) {
                @Override
                public void actionPerformed() {
                    selection = SELECTION_YES;
                    dispose();
                }
            };
            panel.add(b2);
        }
        fixupLayout();
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }
        });
        //create listener for keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if(!isFocused()) return false;
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED) { //esc = close
                    b1.actionPerformed(); //press ok for info/error messageboxes, no for yes/no messageboxes
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) { //enter = ok/yes
                    if(type==TYPE_INFORMATION||type==TYPE_ERROR) b1.actionPerformed(); //press ok for info/error messageboxes
                    if(type==TYPE_YES_NO) b2.actionPerformed(); //press yes for yes/no messageboxes
                }
                return false;
            }
        });
    }

    private void fixupLayout() {
        panel.setBounds(0, 0, getWidth(), getHeight()); //stretch container over entire window
        titleBar.setBounds(1, 1, panel.getWidth() - 2, Main.TITLE_BAR_HEIGHT); //title bar at the top
        text.setBounds(Main.GENERIC_MARGIN * 3, titleBar.getX() + titleBar.getHeight() + Main.GENERIC_MARGIN, getWidth() - Main.GENERIC_MARGIN * 6, getHeight() - 2 - titleBar.getHeight() - DialogButton.DIALOG_BUTTON_HEIGHT - 3 * Main.GENERIC_MARGIN); //text in the middle, leaving space for the 2 buttons
        int controlsX = getWidth() - 2, controlsY = getHeight() - 2 - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT;
        controlsX -= DialogButton.DIALOG_BUTTON_WIDTH + Main.GENERIC_MARGIN;
        //the buttons
        b1.setBounds(controlsX, controlsY, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT);
        if (b2 != null) {
            controlsX -= DialogButton.DIALOG_BUTTON_WIDTH + Main.GENERIC_MARGIN;
            b2.setBounds(controlsX, controlsY, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT);
        }
        repaint(); //must force repaint on some systems
    }

    public static int showError(String html) {
        MessageBox d = new MessageBox(html, TYPE_ERROR);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - DIALOG_WIDTH / 2), y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - DIALOG_HEIGHT / 2);
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return d.selection;
    }

    public static int showMessage(String html) {
        MessageBox d = new MessageBox(html, TYPE_INFORMATION);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - DIALOG_WIDTH / 2), y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - DIALOG_HEIGHT / 2);
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return d.selection;
    }

    public static int showYesNoDialog(String html) {
        MessageBox d = new MessageBox(html, TYPE_YES_NO);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - DIALOG_WIDTH / 2), y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - DIALOG_HEIGHT / 2);
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return d.selection;
    }
}
