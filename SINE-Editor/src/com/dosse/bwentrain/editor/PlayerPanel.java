/*
 * Copyright (C) 2015 Federico
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
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.backends.pc.PCSoundBackend;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 *
 * @author Federico
 */
public class PlayerPanel extends javax.swing.JPanel {    
    
    private static final Color BAR_COLOR=new Color(96,96,96);
    
    private final Timer t; //periodically updates status and progress
    private IRenderer player;
    
    //play, pause, rewind icons. scaled.
    private static final ImageIcon play=new ImageIcon(Utils.loadUnscaled("/com/dosse/bwentrain/editor/images/play.png").getImage().getScaledInstance((int)Main.TEXT_SIZE, (int)Main.TEXT_SIZE, Image.SCALE_SMOOTH)),
            pause=new ImageIcon(Utils.loadUnscaled("/com/dosse/bwentrain/editor/images/pause.png").getImage().getScaledInstance((int)Main.TEXT_SIZE, (int)Main.TEXT_SIZE, Image.SCALE_SMOOTH)),
            rewindIcon=new ImageIcon(Utils.loadUnscaled("/com/dosse/bwentrain/editor/images/rewind.png").getImage().getScaledInstance((int)Main.TEXT_SIZE, (int)Main.TEXT_SIZE, Image.SCALE_SMOOTH));

    public void setPreset(Preset p) {
        Float time = null, vol = null;
        Boolean playing=null;
        try {
            if (player != null) {
                time = player.getPosition();
                vol=player.getVolume();
                playing=player.isPlaying();
                player.stopPlaying();
            }
        } catch (Throwable err) {
        }
        try {
            player = new IsochronicRenderer(p.clone(), new PCSoundBackend(44100, 1), -1);
            if(time!=null) player.setPosition(time);
            if(vol!=null) player.setVolume(vol);
            if(playing!=null&&playing) player.play();
        } catch (Exception ex) {
        }
    }

    /*
        all the methods below have try-catches to ignore concurrent operation errors (extremely unlikely to happen, may bug the application. still better than crashing)
    */
    
    public void stop() {
        try {
            pause();
            rewind();
        } catch (Throwable err) {
        }
    }

    public void rewind() {
        try {
            player.setPosition(0);
        } catch (Throwable err) {
        }
    }

    public void play() {
        try {
            if (!player.isPlaying()) {
                player.play();
            }
        } catch (Throwable err) {
        }
    }

    public void pause() {
        try {
            if (player.isPlaying()) {
                player.pause();
            }
        } catch (Throwable err) {
        }
    }

    public void togglePlay() {
        try {
            if (player.isPlaying()) {
                pause();
            } else {
                play();
            }
        } catch (Throwable err) {
        }
    }
    
    public boolean isPlaying(){
        try{
            return player.isPlaying();
        }catch(Throwable err){
            return false;
        }
    }

    /**
     * Creates new form PlayerPanel
     */
    public PlayerPanel() {
        initComponents();
        player = null;
        t = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    progress.setValue((int) (progress.getMaximum() * (player.getPosition() / player.getLength())));
                    volume.setValue((int) (volume.getMaximum() * player.getVolume()));
                    progress.setString(Utils.toHMS(player.getPosition()));
                    if (player.isPlaying()) {
                        playPause.setIcon(pause);
                    } else {
                        playPause.setIcon(play);
                    }
                } catch (Throwable err) {
                }
            }
        });
        t.setRepeats(true);
        t.start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progress = new javax.swing.JProgressBar();
        volume = new javax.swing.JProgressBar();
        rewind = new javax.swing.JButton();
        playPause = new javax.swing.JButton();

        progress.setFont(MetalLookAndFeel.getSubTextFont());
        progress.setForeground(BAR_COLOR);
        progress.setMaximum(100000);
        progress.setString("..."); // NOI18N
        progress.setStringPainted(true);
        progress.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                progressMouseDragged(evt);
            }
        });
        progress.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                progressMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                progressMouseReleased(evt);
            }
        });

        volume.setFont(MetalLookAndFeel.getSubTextFont());
        volume.setForeground(BAR_COLOR);
        volume.setMaximum(100000);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/dosse/bwentrain/editor/locale"); // NOI18N
        volume.setString(bundle.getString("PlayerPanel.volume.string")); // NOI18N
        volume.setStringPainted(true);
        volume.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                volumeMouseDragged(evt);
            }
        });
        volume.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                volumeMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                volumeMouseReleased(evt);
            }
        });

        rewind.setFont(MetalLookAndFeel.getSubTextFont().deriveFont(Main.SMALL_TEXT_SIZE));
        rewind.setIcon(rewindIcon);
        rewind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rewindActionPerformed(evt);
            }
        });

        playPause.setFont(MetalLookAndFeel.getSubTextFont().deriveFont(Main.SMALL_TEXT_SIZE));
        playPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(rewind)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playPause)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(volume, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
            .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(volume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(rewind)
                        .addComponent(playPause))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void progressMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_progressMouseDragged
        progressMousePressed(evt);
    }//GEN-LAST:event_progressMouseDragged

    private void progressMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_progressMousePressed
        try {
            player.setPosition(player.getLength() * ((float) evt.getX() / (float) progress.getWidth()));
        } catch (Throwable err) {
        }
    }//GEN-LAST:event_progressMousePressed

    private void progressMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_progressMouseReleased
        progressMousePressed(evt);
    }//GEN-LAST:event_progressMouseReleased

    private void volumeMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_volumeMouseDragged
        volumeMousePressed(evt);
    }//GEN-LAST:event_volumeMouseDragged

    private void volumeMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_volumeMousePressed
        try {
            player.setVolume((float) evt.getX() / (float) volume.getWidth());
        } catch (Throwable err) { //concurrent access (extremely unlikely)
        }
    }//GEN-LAST:event_volumeMousePressed

    private void volumeMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_volumeMouseReleased
        volumeMousePressed(evt);
    }//GEN-LAST:event_volumeMouseReleased

    private void rewindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rewindActionPerformed
        rewind();
    }//GEN-LAST:event_rewindActionPerformed

    private void playPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playPauseActionPerformed
        togglePlay();
    }//GEN-LAST:event_playPauseActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton playPause;
    private javax.swing.JProgressBar progress;
    private javax.swing.JButton rewind;
    private javax.swing.JProgressBar volume;
    // End of variables declaration//GEN-END:variables
}
