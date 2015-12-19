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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * editor for noise envelope
 *
 * @author dosse
 */
public abstract class NoiseEditPanel extends JPanel implements IEditable {

    private final Graph graph; //envelope's graph
    private final JScrollPane scroll; //graph container
    private final JLabel noiseVolumeText;
    private float scale = 1f; //changed with mouse wheel

    private static final float MAX_SCALE = 8, MIN_SCALE = 0.125f;
    private static final float NOISE_WARNING_LEVEL = 0.65f;
    private static final int LABEL_HEIGHT = (int) (16 * Main.SCALE), MIN_GRAPH_HEIGHT = (int) (175 * Main.SCALE), MAX_GRAPH_HEIGHT = (int) (350 * Main.SCALE);

    public NoiseEditPanel(Preset p) {
        super();
        setLayout(null);
        noiseVolumeText = new JLabel(Utils.getLocString("GRAPH_NOISE_LABEL"));
        noiseVolumeText.setFont(noiseVolumeText.getFont().deriveFont(Main.SMALL_TEXT_SIZE));
        add(noiseVolumeText);
        graph = new Graph(p, Graph.NOISE_TRACK, Graph.NOISE_ENV, 1, NOISE_WARNING_LEVEL) {
            @Override
            public void onEdit() {
                NoiseEditPanel.this.onEdit();
            }
        };
        scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setViewportView(graph);
        scroll.setBorder(null);
        scroll.setWheelScrollingEnabled(false);
        add(scroll);
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
        //the 2 listeners below receive mouse wheel events and zoom in/out
        addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        graph.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(e);
            }
        });
        fixupLayout();
    }

    private void scroll(MouseWheelEvent e) {
        if (graph.isBusy()) { //it's busy if moving a point
            return;
        }
        float pos = (float) e.getX() / (float) e.getComponent().getWidth(); //zoom to current mouse position
        if (e.getUnitsToScroll() < 0) {
            if (scale >= MAX_SCALE) {
                return;
            }
            scale *= 2; //zooom in
        } else {
            if (scale <= MIN_SCALE) {
                return;
            }
            scale /= 2; //zoom out
        }
        scale = scale < MIN_SCALE ? MIN_SCALE : scale > MAX_SCALE ? MAX_SCALE : scale; //enforce scale min/max
        fixupLayout();
        scroll.getHorizontalScrollBar().setValue((int) (scroll.getHorizontalScrollBar().getMaximum() * pos - scroll.getWidth() / 2));
        graph.repaint();
    }

    private void fixupLayout() {
        graph.setSize(0, 0); //shitty workaround for shitty swing bug, forces layout to recalculate sizes
        //the code below lays the label and the graph at the top of the panel
        int y = 4;
        noiseVolumeText.setBounds(4, y, getWidth() - 8, LABEL_HEIGHT);
        y += noiseVolumeText.getHeight();
        int graphW = (int) (graph.getPreset().getLength() * scale * Main.SCALE);
        int scrollW = getWidth() > graphW ? graphW : getWidth();
        int graphH = getHeight() - LABEL_HEIGHT;
        scroll.setBounds(0, y, scrollW, graphH < MIN_GRAPH_HEIGHT ? MIN_GRAPH_HEIGHT : graphH > MAX_GRAPH_HEIGHT ? MAX_GRAPH_HEIGHT : graphH);
        graph.setPreferredSize(new Dimension(graphW, graph.getHeight()));
        JScrollBar bar = scroll.getHorizontalScrollBar();
        float v = (float) bar.getValue() / (float) bar.getMaximum();
        scroll.setViewportView(graph);
        bar.setValue((int) (v * bar.getMaximum()));
    }

    public void forceLayoutUpdate() {
        fixupLayout();
        graph.repaint();
    }
}
