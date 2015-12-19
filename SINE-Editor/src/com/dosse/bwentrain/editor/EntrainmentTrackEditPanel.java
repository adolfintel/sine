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

import com.dosse.bwentrain.core.Preset;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * editor for EntrainmenTracks (the one with 3 envelopes and the slider)
 *
 * @author dosse
 */
public abstract class EntrainmentTrackEditPanel extends JPanel implements IEditable {

    private final Graph vol, baseF, entF; //3 envelopes' graphs
    private JScrollPane scrollVol, scrollBaseF, scrollEntF; //container for graphs
    private final JLabel volText, baseFText, entFText, trackVolText, trackVolValue;
    private JSlider trackVol;
    protected int trackId;
    private float scale = 1f; //changed with mouse wheel

    private static final float MAX_SCALE = 8, MIN_SCALE = 0.125f;
    private static final int MIN_GRAPH_HEIGHT = (int) (150 * Main.SCALE), MAX_GRAPH_HEIGHT = (int) (350 * Main.SCALE);
    private static final int LABEL_HEIGHT = (int) (16 * Main.SCALE), GRAPH_VMARGIN = (int) (12 * Main.SCALE), SLIDER_HEIGHT = (int) (32 * Main.SCALE), SLIDER_WIDTH = (int) (150 * Main.SCALE);

    public EntrainmentTrackEditPanel(final Preset p, final int n) {
        super();
        trackId = n;
        setLayout(null);
        volText = new JLabel(Utils.getLocString("GRAPH_VOLUME_LABEL"));
        baseFText = new JLabel(Utils.getLocString("GRAPH_BASE_FREQUENCY_LABEL"));
        entFText = new JLabel(Utils.getLocString("GRAPH_ENTRAINMENT_FREQUENCY_LABEL"));
        trackVolText = new JLabel(Utils.getLocString("GRAPH_TRACK_VOLUME_LABEL"));
        trackVolValue = new JLabel("");
        volText.setFont(volText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        baseFText.setFont(baseFText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        entFText.setFont(entFText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        trackVolText.setFont(entFText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        trackVolValue.setFont(entFText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        add(volText);
        add(baseFText);
        add(entFText);
        add(trackVolText);
        add(trackVolValue);
        vol = new Graph(p, n, Graph.ENV_VOLUME, 1, -1) {

            @Override
            public void onEdit() {
                EntrainmentTrackEditPanel.this.onEdit();
            }
        };
        baseF = new Graph(p, n, Graph.ENV_BASE_FREQUENCY, Main.MAX_BASE_FREQUENCY, -1) {

            @Override
            public void onEdit() {
                EntrainmentTrackEditPanel.this.onEdit();
            }
        };
        entF = new Graph(p, n, Graph.ENV_ENTRAINMENT_FREQUENCY, Main.MAX_ENTRAINMENT_FREQUENCY, -1) {
            @Override
            public void onEdit() {
                EntrainmentTrackEditPanel.this.onEdit();
            }
        };
        scrollVol = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollBaseF = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollEntF = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollVol.setViewportView(vol);
        scrollBaseF.setViewportView(baseF);
        scrollEntF.setViewportView(entF);
        scrollVol.setBorder(null);
        scrollBaseF.setBorder(null);
        scrollEntF.setBorder(null);
        scrollVol.setWheelScrollingEnabled(false);
        scrollBaseF.setWheelScrollingEnabled(false);
        scrollEntF.setWheelScrollingEnabled(false);
        //the 3 listeners below keep the scrollbars synchronized
        scrollVol.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                scrollBaseF.getHorizontalScrollBar().setValue(e.getValue());
                scrollEntF.getHorizontalScrollBar().setValue(e.getValue());
            }
        });
        scrollBaseF.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                scrollEntF.getHorizontalScrollBar().setValue(e.getValue());
                scrollVol.getHorizontalScrollBar().setValue(e.getValue());
            }
        });
        scrollEntF.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                scrollBaseF.getHorizontalScrollBar().setValue(e.getValue());
                scrollVol.getHorizontalScrollBar().setValue(e.getValue());
            }
        });
        add(scrollVol);
        add(scrollBaseF);
        add(scrollEntF);
        trackVol = new JSlider(0, 100);
        trackVol.setValue((int) (p.getEntrainmentTrack(n).getTrackVolume() * trackVol.getMaximum()));
        trackVol.setFocusable(false);
        trackVol.addChangeListener(new ChangeListener() { //track volume changed, update Preset and JLabel next to slider

            @Override
            public void stateChanged(ChangeEvent e) {
                p.getEntrainmentTrack(n).setTrackVolume((float) trackVol.getValue() / (float) (trackVol.getMaximum()));
                trackVolValue.setText("" + (int) (p.getEntrainmentTrack(n).getTrackVolume() * 100) + "%");
                if (!trackVol.getValueIsAdjusting()) {
                    onEdit();
                }
            }
        });
        add(trackVol);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }

        });
        //the 4 listeners below receive mouse wheel events to change zoom
        vol.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        baseF.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        entF.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        fixupLayout();
    }

    private void scroll(MouseWheelEvent e) {
        if (entF.isBusy() || vol.isBusy() || baseF.isBusy()) { //it's busy when something is being dragged
            return;
        }
        float pos = (float) e.getX() / (float) e.getComponent().getWidth(); //zoom to current mouse position
        if (e.getUnitsToScroll() < 0) {
            if (scale >= MAX_SCALE) {
                return;
            }
            scale *= 2; //zoom in
        } else {
            if (scale <= MIN_SCALE) {
                return;
            }
            scale /= 2; //zoom out
        }
        scale = scale < MIN_SCALE ? MIN_SCALE : scale > MAX_SCALE ? MAX_SCALE : scale; //enforce graph scale limits
        fixupLayout();
        //apply new scale
        scrollVol.getHorizontalScrollBar().setValue((int) (scrollVol.getHorizontalScrollBar().getMaximum() * pos - scrollVol.getWidth() / 2));
        scrollBaseF.repaint();
        scrollEntF.repaint();
        scrollVol.repaint();
    }

    private void fixupLayout() {

        Preset preset = vol.getPreset();

        //track volume slider and its labels at the bottom
        int y = getHeight() - SLIDER_HEIGHT - LABEL_HEIGHT;
        trackVolText.setBounds(4, y, getWidth() - 8, LABEL_HEIGHT);
        y += trackVolText.getHeight();
        trackVol.setBounds(4, y, SLIDER_WIDTH, SLIDER_HEIGHT);
        int x = trackVol.getX() + trackVol.getWidth() + 8;
        trackVolValue.setBounds(x, y, getWidth() - 8 - x, SLIDER_HEIGHT);
        trackVolValue.setText("" + (int) (preset.getEntrainmentTrack(trackId).getTrackVolume() * 100) + "%");
        //calculate graph height: it's all the space that's left divided by 3
        int graphHeight = (getHeight() - trackVol.getX() - trackVol.getHeight() - 3 * LABEL_HEIGHT - 4 * GRAPH_VMARGIN) / 3;
        graphHeight = graphHeight < MIN_GRAPH_HEIGHT ? MIN_GRAPH_HEIGHT : graphHeight > MAX_GRAPH_HEIGHT ? MAX_GRAPH_HEIGHT : graphHeight;

        //shitty workaround for shitty swing bug, forces layout to recalculate sizes
        entF.setSize(0, 0);
        vol.setSize(0, 0);
        baseF.setSize(0, 0);

        //the 3 blocks below just lay labels and graphs. nothing interesting
        y = 4;
        entFText.setBounds(4, y, getWidth() - 8, LABEL_HEIGHT);
        y += entFText.getHeight();
        int graphW = (int) (preset.getLength() * scale * Main.SCALE);
        int scrollW = getWidth() > graphW ? graphW : getWidth();
        scrollEntF.setBounds(0, y, scrollW, graphHeight);
        y += scrollEntF.getHeight() + GRAPH_VMARGIN;
        entF.setPreferredSize(new Dimension(graphW, entF.getHeight()));
        JScrollBar bar = scrollEntF.getHorizontalScrollBar();
        float v = (float) bar.getValue() / (float) bar.getMaximum();
        scrollEntF.setViewportView(entF);
        bar.setValue((int) (v * bar.getMaximum()));

        volText.setBounds(4, y, getWidth() - 8, LABEL_HEIGHT);
        y += volText.getHeight();
        scrollVol.setBounds(0, y, scrollW, graphHeight);
        y += scrollVol.getHeight() + GRAPH_VMARGIN;
        vol.setPreferredSize(new Dimension(graphW, vol.getHeight()));
        bar = scrollVol.getHorizontalScrollBar();
        v = (float) bar.getValue() / (float) bar.getMaximum();
        scrollVol.setViewportView(vol);
        bar.setValue((int) (v * bar.getMaximum()));

        baseFText.setBounds(4, y, getWidth() - 8, LABEL_HEIGHT);
        y += baseFText.getHeight();
        scrollBaseF.setBounds(0, y, scrollW, graphHeight);
        y += scrollBaseF.getHeight() + GRAPH_VMARGIN;
        baseF.setPreferredSize(new Dimension(graphW, baseF.getHeight()));
        bar = scrollBaseF.getHorizontalScrollBar();
        v = (float) bar.getValue() / (float) bar.getMaximum();
        scrollBaseF.setViewportView(baseF);
        bar.setValue((int) (v * bar.getMaximum()));

    }

    public void forceLayoutUpdate() {
        fixupLayout();
        vol.repaint();
        baseF.repaint();
        entF.repaint();
    }
}
