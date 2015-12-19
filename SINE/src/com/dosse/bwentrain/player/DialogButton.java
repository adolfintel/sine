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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

/**
 * buttons used in all dialogs
 *
 * @author dosse
 */
public abstract class DialogButton extends JLabel {

    public static final int DIALOG_BUTTON_WIDTH = (int) (80f * Main.SCALE), DIALOG_BUTTON_HEIGHT = (int) (28f * Main.SCALE); //default size

    public abstract void actionPerformed();

    public DialogButton(String text) {
        super(text);
        setForeground(Main.TEXT);
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setBorder(new LineBorder(Main.CONTROL_BORDER));
        setSize(DIALOG_BUTTON_WIDTH, DIALOG_BUTTON_HEIGHT);
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
    }

}
