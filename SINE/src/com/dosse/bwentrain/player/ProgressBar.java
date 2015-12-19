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

import java.awt.Graphics;
import javax.swing.JProgressBar;

/**
 * progress bar used in player and export
 *
 * @author dosse
 */
public class ProgressBar extends JProgressBar {

    public ProgressBar() {
        super();
    }

    public ProgressBar(int min, int max) {
        super(min, max);
    }

    @Override
    public void paint(Graphics g) {
        g.clearRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
        g.setColor(Main.CONTROL_BORDER);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g.setColor(Main.CONTROL_ACCENT);
        float p = (float) (getValue() - getMinimum()) / (float) (getMaximum() - getMinimum());
        g.fillRect(1, 1, (int) ((getWidth() - 2) * p), getHeight() - 2);
    }
}
