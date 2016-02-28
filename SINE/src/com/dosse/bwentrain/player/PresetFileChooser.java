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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * preset opener
 *
 * @author dosse
 */
public class PresetFileChooser extends JDialog {

    private static final int DIALOG_WIDTH = (int) (450f * Main.SCALE), DIALOG_HEIGHT = (int) (300f * Main.SCALE); //window size. as usual, there's all the code for resizing the window, but not the event listener

    private final JPanel panel; //container
    private final TitleBar titlebar;
    private final JFileChooser c;
    private final DialogButton confirm, cancel;

    private File selected;

    public PresetFileChooser() {
        super(new JFrame(), true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLayout(null);
        addWindowListener(new WindowAdapter() { //alt+f4 = cancel

            @Override
            public void windowClosing(WindowEvent e) {
                done(false);
            }
        });
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new LineBorder(Main.WINDOW_BORDER));
        titlebar = new TitleBar(this);
        titlebar.setText(Utils.getLocString("PRESOPEN_TITLE"));
        titlebar.setHorizontalAlignment(TitleBar.CENTER);
        setTitle(titlebar.getText());
        panel.add(titlebar);
        c = new JFileChooser();
        c.setFileSelectionMode(JFileChooser.FILES_ONLY);
        c.setMultiSelectionEnabled(false);
        c.setControlButtonsAreShown(false);
        c.setFileFilter(Main.PRESET_FILE_FILTER);
        if(Main.lastDir!=null) c.setCurrentDirectory(Main.lastDir); //show last directory browsed with a file chooser
        c.addActionListener(new ActionListener() { //double-clicked a file or pressed enter
            @Override
            public void actionPerformed(ActionEvent e) {
                done(e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION));
            }
        });
        panel.add(c);
        add(panel);
        Utils.setFontRecursive(c, Main.BASE_FONT);
        confirm = new DialogButton(Utils.getLocString("PRESOPEN_CONFIRM")) { //confirm pressed

            @Override
            public void actionPerformed() {
                if (c.getSelectedFile() != null) {
                    done(true);
                }
            }
        };
        panel.add(confirm);
        cancel = new DialogButton(Utils.getLocString("PRESOPEN_CANCEL")) { //cancel pressed

            @Override
            public void actionPerformed() {
                done(false);
            }
        };
        panel.add(cancel);
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
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED) { //esc = cancel
                    cancel.actionPerformed();
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) { //enter = confirm
                    confirm.requestFocus(); //removes focus from file chooser so it commits edit
                    confirm.actionPerformed();
                }
                return false;
            }
        });
    }

    private void done(boolean ok) {
        if (ok) { //confirm. return selected file
            selected = c.getSelectedFile();
            Main.lastDir = c.getCurrentDirectory();
        } else { //canceled, return null
            selected = null;
        }
        dispose();
    }

    private void fixupLayout() {
        panel.setSize(getWidth(), getHeight()); //stretch container over the entire window
        titlebar.setBounds(1, 1, getWidth() - 2, Main.TITLE_BAR_HEIGHT); //title bar at the top
        c.setBounds(1, 1 + titlebar.getHeight(), getWidth() - 2, getHeight() - 2 - titlebar.getHeight() - 2 * Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT); //file chooser in the middle, leaving space at the bottom for the 2 buttons
        int x = getWidth() - 1 - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_WIDTH, y = getHeight() - 1 - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT;
        cancel.setBounds(x, y, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT); //cancel button in lower-right corner
        x -= Main.GENERIC_MARGIN + DialogButton.DIALOG_BUTTON_WIDTH;
        confirm.setBounds(x, y, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT); //confirm button at its left
        repaint(); //must force repaint on some systems
    }

    //show dialog and wait for result
    public static File open(int x, int y) {
        PresetFileChooser d = new PresetFileChooser();
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return d.selected;
    }
}
