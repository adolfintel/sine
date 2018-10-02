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

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.backends.pc.PCSoundBackend;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * player
 *
 * @author dosse
 */
public class PlayerPanel extends JPanel {

    private static final int BAR_HEIGHT = (int) (26f * Main.SCALE), //progress and volume bar height
            PLAY_PAUSE_BUTTON_WIDTH = (int) (26f * Main.SCALE), PLAY_PAUSE_BUTTON_HEIGHT = (int) (26f * Main.SCALE), //size of play/pause button
            VOL_BAR_WIDTH = (int) (180f * Main.SCALE), //width of volume bar
            VOL_TEXT_WIDTH = (int) (60f * Main.SCALE); //width of JLabel at the left of the volume bar

    private static final ImageIcon PLAY = Utils.loadAndScale("/com/dosse/bwentrain/player/images/play.png", PLAY_PAUSE_BUTTON_WIDTH, PLAY_PAUSE_BUTTON_HEIGHT), PAUSE = Utils.loadAndScale("/com/dosse/bwentrain/player/images/pause.png", PLAY_PAUSE_BUTTON_WIDTH, PLAY_PAUSE_BUTTON_HEIGHT);

    private JLabel playPauseButton, status, volBarText;
    private ProgressBar pBar, volBar;
    private final Timer t; //updates status and progress periodically

    private IRenderer p;

    //converts time to HH:MM:SS String
    private String toHMS(float t) {
        int h = (int) (t / 3600);
        t %= 3600;
        int m = (int) (t / 60);
        t %= 60;
        int s = (int) t;
        return "" + (h < 10 ? ("0" + h) : h) + ":" + (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s); //bloody hell, code salad
    }

    public PlayerPanel() {
        super();
        setLayout(null);
        playPauseButton = new JLabel(PLAY);
        playPauseButton.addMouseListener(new MouseAdapter() { //play/payse

            @Override
            public void mouseReleased(MouseEvent e) {
                if (p == null) {
                    return; //no preset loaded, don't try playing it
                } else { //a preset is loaded
                    if (p.isPlaying()) { //it's playing, pause
                        p.pause();
                    } else { //it's paused, resume
                        p.play();
                    }
                }
            }

        });
        status = new JLabel("");
        status.setForeground(Main.TEXT);
        volBarText = new JLabel(Utils.getLocString("PLAYER_VOLUME"));
        volBarText.setForeground(Main.TEXT);
        volBarText.setHorizontalAlignment(JLabel.RIGHT);
        pBar = new ProgressBar(0, 100000);
        pBar.addMouseListener(new MouseAdapter() { //sets position when mouse is pressed/released

            @Override
            public void mousePressed(MouseEvent e) {
                mouseReleased(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (p != null) {
                    p.setPosition(((float) e.getX() / (float) pBar.getWidth()) * p.getLength());
                }
            }

        });
        pBar.addMouseMotionListener(new MouseMotionAdapter() { //sets position when mouse is dragged
            @Override
            public void mouseDragged(MouseEvent e) {
                if (p != null) {
                    p.setPosition(((float) e.getX() / (float) pBar.getWidth()) * p.getLength());
                }
            }
        });
        volBar = new ProgressBar(0, 100000);
        volBar.setValue(volBar.getMaximum());
        volBar.addMouseListener(new MouseAdapter() { //sets volume when mouse is pressed/released

            @Override
            public void mousePressed(MouseEvent e) {
                mouseReleased(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                float prog = (float) e.getX() / (float) volBar.getWidth();
                if (p != null) {
                    p.setVolume(prog);
                }
                volBar.setValue((int) (prog * volBar.getMaximum()));
            }
        });
        volBar.addMouseMotionListener(new MouseMotionAdapter() { //sets volume when mouse is dragged
            @Override
            public void mouseDragged(MouseEvent e) {
                float prog = (float) e.getX() / (float) volBar.getWidth();
                if (p != null) {
                    p.setVolume(prog);
                }
                volBar.setValue((int) (prog * volBar.getMaximum()));
            }
        });
        add(playPauseButton);
        add(status);
        add(pBar);
        add(volBarText);
        add(volBar);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }
        });
        t = new Timer(20, new ActionListener() { //updates status and progress
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (p == null) {
                        status.setText("");
                        return;
                    }
                    pBar.setValue((int) ((p.getPosition() / p.getLength()) * pBar.getMaximum()));
                    if (p.isPlaying()) {
                        playPauseButton.setIcon(PAUSE);
                    } else {
                        playPauseButton.setIcon(PLAY);
                    }
                    status.setText(toHMS(p.getPosition()) + "/" + toHMS(p.getLength()));
                } catch (Throwable th) {
                }
            }
        });
        t.setRepeats(true);
        t.start();
    }

    public Preset getCurrentPreset() {
        return p!=null ? p.getPreset() : null;
    }

    public void setPreset(Preset preset) throws Exception {
        if (p != null) { //stop previous player
            p.stopPlaying();
            p = null;
        }
        p = new IsochronicRenderer(preset, new PCSoundBackend(44100, 1), -1);
        p.setVolume((float) volBar.getValue() / (float) volBar.getMaximum());
    }

    public void pause() {
        if (p == null) {
            return;
        }
        p.pause();
    }

    public void play() {
        if (p == null) {
            return;
        }
        p.play();
    }

    public boolean isPlaying() {
        if (p == null) {
            return false;
        } else {
            return p.isPlaying();
        }
    }

    public void fixupLayout() {
        int y = 0;
        int x = getWidth() - PLAY_PAUSE_BUTTON_WIDTH;
        playPauseButton.setBounds(x, y, PLAY_PAUSE_BUTTON_WIDTH, PLAY_PAUSE_BUTTON_HEIGHT); //play/pause on the right
        pBar.setBounds(0, y, x, BAR_HEIGHT); //progress takes up rest of the line
        y += BAR_HEIGHT + Main.GENERIC_MARGIN;
        x = getWidth() - VOL_BAR_WIDTH;
        volBar.setBounds(x, y, VOL_BAR_WIDTH, BAR_HEIGHT); //volume bar on the right, below progress
        volBarText.setBounds(volBar.getX() - Main.GENERIC_MARGIN - VOL_TEXT_WIDTH, y, VOL_TEXT_WIDTH, BAR_HEIGHT); //volume bar text at its left
        status.setBounds(0, y, x - Main.GENERIC_MARGIN, BAR_HEIGHT); //status takes up rest of the line
        repaint(); //must force repaint on some systems
    }
    
    public void dispose(){
        if(p!=null){
            p.stopPlaying();
            p=null;
        }
    }
}
