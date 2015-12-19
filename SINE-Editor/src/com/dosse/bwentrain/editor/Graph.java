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

import com.dosse.bwentrain.core.EntrainmentTrack;
import com.dosse.bwentrain.core.Envelope;
import com.dosse.bwentrain.core.Preset;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Envelope editor
 *
 * @author dosse
 */
public abstract class Graph extends JPanel implements IEditable {

    private static final DecimalFormat FIRST_DECIMAL_FORMAT = new DecimalFormat("###.#"), NO_DECIMAL_FORMAT = new DecimalFormat("###");

    private static final boolean ANTIALIASING = true;

    private static final Font TOOLTIP_FONT = MetalLookAndFeel.getSubTextFont();

    private static final int UNIT_PERCENT = 1, UNIT_HZ = 2;

    private static final Color BACKGROUND = new Color(16, 16, 16), GRAPH_LINE = new Color(96, 96, 96), LINE = new Color(96, 255, 96), POINT = new Color(96, 255, 96, 128), WARNING = new Color(255, 48, 48, 128), LOOP = new Color(48, 48, 255, 128),
            TEXT = new Color(255, 255, 255), TEXT_LOCKED_ON_POINT = new Color(192, 192, 255);
    private static final int POINT_SIZE = (int) (8 * Main.SCALE);

    public static final int NOISE_TRACK = -1, NOISE_ENV = -1, ENV_VOLUME = 0, ENV_BASE_FREQUENCY = 1, ENV_ENTRAINMENT_FREQUENCY = 2;

    private final Preset p;
    private Envelope toEdit;
    private final float maxValue, warningThreshold;
    private int unit;
    private final int selectedTrack, selectedEnv;

    private int selection = -1;
    private int mouseX = -1, mouseY = -1;

    /**
     *
     * @param p Preset
     * @param track track to edit (NOISE_TRACK for noise, 0,1,2,... for
     * entrainment tracks)
     * @param env envelope (NOISE_ENV for noise track; ENV_VOLUME,
     * ENV_BASE_FREQUENCY, ENV_ENTRAINMENT_FREQUENCY for entrainment tracks)
     * @param maxValue max value that can be accepted on Y axis
     * @param warningThreshold values above will be shown in red
     */
    public Graph(final Preset p, final int track, final int env, final float maxValue, float warningThreshold) {
        super();
        this.p = p;
        selectedEnv = env;
        selectedTrack = track;
        if (selectedTrack == NOISE_TRACK) {
            toEdit = p.getNoiseEnvelope();
            unit = UNIT_PERCENT;
        } else {
            EntrainmentTrack et = p.getEntrainmentTrack(selectedTrack);
            if (selectedEnv == ENV_VOLUME) {
                toEdit = et.getVolumeEnvelope();
                unit = UNIT_PERCENT;
            } else if (selectedEnv == ENV_BASE_FREQUENCY) {
                toEdit = et.getBaseFrequencyEnvelope();
                unit = UNIT_HZ;
            } else if (selectedEnv == ENV_ENTRAINMENT_FREQUENCY) {
                toEdit = et.getEntrainmentFrequencyEnvelope();
                unit = UNIT_HZ;
            }
        }
        this.maxValue = maxValue;
        this.warningThreshold = warningThreshold;
        //the 2 listeners below receives and manage clicks, drags, etc. in the graph
        addMouseListener(new MouseListener() {
            private long lastClickT = 0;
            private int lastClickX = -1, lastClickY = -1;

            @Override
            public void mouseClicked(MouseEvent e) {
                if (lastClickX == e.getX() && lastClickY == e.getY() && e.getButton() == MouseEvent.BUTTON1 && System.nanoTime() - lastClickT <= 500000000L) { //double clicked a point, open manual input dialog
                    selection = getPointAt(e.getX(), e.getY());
                    if (selection != -1) {
                        manualInput();
                    }
                    selection = -1;
                    repaint();
                }
                lastClickT = System.nanoTime();
                lastClickX = e.getX();
                lastClickY = e.getY();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                selection = getPointAt(e.getX(), e.getY());
                if (e.getButton() == MouseEvent.BUTTON1) { //LEFT CLICK
                    if (selection == -1) { //not on a point, create new point
                        float t = (float) e.getX() / getScale();
                        toEdit.addPoint(t, toEdit.get(t));
                        selection = -1;
                    }
                    repaint();
                } else if (e.getButton() == MouseEvent.BUTTON3) { //RIGHT CLICK
                    if (selection != -1 && selection != 0) { //remove point, unless it's point number 0 which can't be removed
                        toEdit.removePoint(selection);
                    }
                    selection = -1;
                    repaint();
                } else if (e.getButton() == MouseEvent.BUTTON2) { //MIDDLE CLICK, if on a point, open manual input dialog (same as double-click)
                    if (selection != -1) {
                        manualInput();
                    }
                    selection = -1;
                    repaint();
                }

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selection = -1;
                if (e.getX() < 0 || e.getX() > getWidth() || e.getY() < 0 || e.getY() > getHeight()) {
                    mouseX = -1;
                    mouseY = -1;
                }
                repaint();
                onEdit();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1;
                mouseY = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selection != -1) { //on a point, move it
                    float t = (float) e.getX() / getScale();
                    float minT = selection == 0 ? 0 : toEdit.getT(selection - 1);
                    float maxT = selection == toEdit.getPointCount() - 1 ? (float) (getWidth() - 1) / getScale() : toEdit.getT(selection + 1);
                    float val = 1 - ((float) e.getY() / (float) (getHeight() - 1));
                    val = val < 0 ? 0 : val > 1 ? 1 : val;
                    toEdit.setVal(selection, maxValue * val);
                    toEdit.setT(selection, t < minT ? minT : t > maxT ? maxT : t);
                }
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
        });
    }

    private void manualInput() {
        do {
            Float[] newVal = PointEditDialog.edit(toEdit.getT(selection), toEdit.getVal(selection), selection == 0);
            if (newVal == null) { //user cancelled
                break;
            }
            float val = newVal[1];
            if (val < 0 || val > maxValue) {
                JOptionPane.showMessageDialog(null, Utils.getLocString("GRAPH_MANUALINPUT_INVALID_VALUE") + " 0 - " + maxValue, Utils.getLocString("PointEditDialog.title"), JOptionPane.ERROR_MESSAGE);
                continue;
            }
            float t = newVal[0];
            if (t < 0 || t > toEdit.getLength()) {
                JOptionPane.showMessageDialog(null, Utils.getLocString("GRAPH_MANUALINPUT_INVALID_TIME") + " 00:00:00 - " + Utils.toHMS(toEdit.getLength()), Utils.getLocString("PointEditDialog.title"), JOptionPane.ERROR_MESSAGE);
                continue;
            }
            toEdit.setVal(selection, val);
            if (selection != 0) {
                toEdit.setT(selection, t);
            }
            onEdit();
            break;
        } while (true);
    }

    private float getScale() {
        return getWidth() / p.getLength();
    }

    private int getPointAt(int x, int y) {
        for (int i = 0; i < toEdit.getPointCount(); i++) {
            int px = (int) (toEdit.getT(i) * getScale()), py = (int) ((1 - toEdit.getVal(i) / maxValue) * (getHeight() - 1));
            if (x >= px - POINT_SIZE / 2 && x <= px + POINT_SIZE / 2 && y >= py - POINT_SIZE / 2 && y <= py + POINT_SIZE / 2) {
                return i;
            }
        }
        return -1;
    }

    public Preset getPreset() {
        return p;
    }

    @Override
    public void paint(Graphics g) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, ANTIALIASING ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(GRAPH_LINE);
        for (int i = 0; i < getWidth() - 1; i += 60 * getScale()) {
            g.drawLine(i, 0, i, getHeight());
        }
        int hLines = selectedTrack == NOISE_TRACK || selectedEnv == ENV_VOLUME ? 5 : selectedEnv == ENV_BASE_FREQUENCY ? 15 : 10;
        for (int i = 0; i < hLines; i++) {
            int v = (int) (((float) i / (float) hLines) * (getHeight() - 1));
            g.drawLine(0, v, getWidth() - 1, v);
        }
        if (p.loops()) { //DRAW LOOPING ZONE
            int lx = (int) ((p.getLoop() / p.getLength()) * (float) getWidth());
            g.setColor(LOOP);
            g.fillRect(lx, 0, getWidth() - lx, getHeight());
        }
        if (warningThreshold >= 0 && warningThreshold <= 1) {
            g.setColor(WARNING);
            g.fillRect(0, 0, getWidth(), (int) (getHeight() * (1 - warningThreshold)));
        }
        int[] t, v;
        t = new int[toEdit.getPointCount() + 1];
        v = new int[toEdit.getPointCount() + 1];
        g.setColor(POINT);
        for (int i = 0; i < toEdit.getPointCount(); i++) {
            t[i] = (int) (toEdit.getT(i) * getScale());
            v[i] = (int) ((1 - (toEdit.getVal(i) / maxValue)) * (getHeight() - 1));
            g.fillRect(t[i] - POINT_SIZE / 2, v[i] - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
        }
        t[toEdit.getPointCount()] = getWidth() - 1;
        v[toEdit.getPointCount()] = v[toEdit.getPointCount() - 1];
        g.setColor(LINE);
        g.drawPolyline(t, v, toEdit.getPointCount() + 1);
        //DRAW MOUSE COORDINATES IF INSIDE GRAPH. if the mouse is over a point, its coordinates are shown instead.
        if (mouseX != -1 && mouseY != -1) {
            int sel = selection == -1 ? getPointAt(mouseX, mouseY) : selection;
            float tooltipTextSize = MetalLookAndFeel.getSubTextFont().getSize2D();
            int x = mouseX + 12, y = (int) (mouseY < tooltipTextSize ? tooltipTextSize : mouseY > getHeight() ? getHeight() : mouseY);
            mouseX = mouseX < 0 ? 0 : mouseX >= getWidth() ? getWidth() - 1 : mouseX;
            mouseY = mouseY < 0 ? 0 : mouseY >= getHeight() ? getHeight() - 1 : mouseY;
            g.setFont(TOOLTIP_FONT);
            String text = "";
            float time = sel == -1 ? ((float) mouseX / getScale()) : toEdit.getT(sel);
            float val = sel == -1 ? ((1 - ((float) mouseY / (float) (getHeight() - 1))) * maxValue) : toEdit.getVal(sel);
            if (unit == UNIT_HZ) {
                text += Utils.toHMS(time) + " , " + FIRST_DECIMAL_FORMAT.format(val) + "Hz";
            } else if (unit == UNIT_PERCENT) {
                text += Utils.toHMS(time) + " , " + NO_DECIMAL_FORMAT.format(val * 100) + "%";
            }
            Rectangle2D textBounds = g.getFontMetrics().getStringBounds(text, g);
            if (x + textBounds.getWidth() >= getWidth() - 1) {
                x = (int) (getWidth() - textBounds.getWidth() - 1);
            }
            if (x < 0) {
                x = 0;
            }
            if (sel == -1) {
                g.setColor(TEXT);
            } else { //mouse over a point, use different color
                g.setColor(TEXT_LOCKED_ON_POINT);
            }
            g.drawString(text, x, y);
        }
    }

    public boolean isBusy() {
        return selection != -1;
    }

}
