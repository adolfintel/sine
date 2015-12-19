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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * the menu that appears when clicking the menu button
 *
 * @author dosse
 */
public class MainMenu extends JPanel {

    public static final int MENU_WIDTH = (int) (180f * Main.SCALE);

    /**
     * an item in this menu
     */
    public static abstract class MenuItem extends JLabel {

        public static final int MENU_ITEM_HEIGHT = (int) (32f * Main.SCALE);

        public abstract void actionPerformed();

        public MenuItem(String text, String iconPathInClasspath) {
            super(text);
            setForeground(Main.TEXT);
            setOpaque(true);
            addMouseListener(new MouseAdapter() { //changes colors and calls actionPerformed if needed

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) {
                        setBackground(null);
                        setForeground(Main.TEXT);
                        return;
                    }
                    setBackground(Main.CONTROL_ACCENT);
                    setForeground(Main.TEXT_ACCENT);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!isEnabled()) {
                        setBackground(null);
                        setForeground(Main.TEXT);
                        return;
                    }
                    setBackground(Main.CONTROL_SELECTED);
                    setForeground(Main.TEXT_SELECTED);
                    actionPerformed();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isEnabled()) {
                        setBackground(null);
                        setForeground(Main.TEXT);
                        return;
                    }
                    setBackground(Main.CONTROL_SELECTED);
                    setForeground(Main.TEXT_SELECTED);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isEnabled()) {
                        setBackground(null);
                        setForeground(Main.TEXT);
                        return;
                    }
                    setBackground(null);
                    setForeground(Main.TEXT);
                }
            });
            setIcon(Utils.loadAndScale(iconPathInClasspath, MENU_ITEM_HEIGHT, MENU_ITEM_HEIGHT)); //if icon==null, icon will be empty (BUT NOT NULL!)
        }

    }
    private ArrayList<MenuItem> items = new ArrayList<>();

    public MainMenu() {
        super();
        setLayout(null);
        setBorder(new LineBorder(Main.CONTROL_BORDER));
        setVisible(false);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                fixupLayout();
            }
            
        });
    }

    public void addItem(MenuItem i) {
        items.add(i);
        add(i);
    }

    private void fixupLayout() {
        int y = 1;
        //show all the items in order
        for (MenuItem i : items) {
            i.setBounds(1, y, MENU_WIDTH - 2, MenuItem.MENU_ITEM_HEIGHT);
            y += MenuItem.MENU_ITEM_HEIGHT;
        }
        setSize(MENU_WIDTH, y + 1);
        repaint(); //must force repaint on some systems
    }
}
