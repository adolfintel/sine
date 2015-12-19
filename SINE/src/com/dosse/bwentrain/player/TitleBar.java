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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JLabel;

/**
 * draggable title bar. when dragged, drags the window it's attached to. can
 * show text like any other JLabel
 *
 * @author dosse
 */
public class TitleBar extends JLabel {

    private int dragStartX = -1, dragStartY = -1;

    public TitleBar(final Component window) {
        super();
        setForeground(Main.TEXT);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getXOnScreen();
                dragStartY = e.getYOnScreen();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartX = -1;
                dragStartY = -1;
            }

        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dX = e.getXOnScreen() - dragStartX, dY = e.getYOnScreen() - dragStartY;
                window.setLocation(window.getLocation().x + dX, window.getLocation().y + dY);
                dragStartX = e.getXOnScreen();
                dragStartY = e.getYOnScreen();
            }
        });
    }

}
