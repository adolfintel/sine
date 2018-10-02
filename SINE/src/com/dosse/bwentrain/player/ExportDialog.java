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
import com.dosse.bwentrain.sound.ISoundDevice;
import com.dosse.bwentrain.sound.backends.flac.FLACFileSoundBackend;
import com.dosse.bwentrain.sound.backends.mp3.MP3FileSoundBackend;
import com.dosse.bwentrain.sound.backends.wav.WavFileSoundBackend;
import java.awt.Dialog;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

/**
 * the export window (not the file selector)
 *
 * @author dosse
 */
public class ExportDialog extends JDialog {
    
    private static final int DIALOG_WIDTH = (int) (580f * Main.SCALE), DIALOG_HEIGHT = (int) (180f * Main.SCALE); //size of the window, as usual, there's all the code for resizing the window, but not the event listener
    private static final int BAR_HEIGHT = (int) (24f * Main.SCALE); //height of the progress bar
    private static final int PLUS_MINUS_WIDTH = (int) (20f * Main.SCALE), PLUS_MINUS_HEIGHT = (int) (20f * Main.SCALE); //size of +/- buttons (used to choose how many times a looping part should be looped if the preset loops)

    public static final int FORMAT_MP3=0, FORMAT_FLAC = 1, FORMAT_WAV = 2;
    
    private JPanel panel; //container
    private ProgressBar progress; //progress bar
    private JLabel status, loop, //status and number of loops
            plus, minus; //controls for increasing/decreasing number of loops
    private DialogButton start, cancel;
    private Timer t; //periodically refreshes the progress bar and status to show progress
    private TitleBar titleBar;
    private int nLoops = 3; //how many times to repeat the loop in a looping preset

    private IRenderer player;
    
    public ExportDialog(final Preset p, final File f, final int format) {
        super(new JFrame(), true);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLayout(null);
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new LineBorder(Main.WINDOW_BORDER));
        add(panel);
        titleBar = new TitleBar(this);
        titleBar.setHorizontalAlignment(TitleBar.CENTER);
        titleBar.setText(Utils.getLocString("EXPORT_TITLE"));
        setTitle(titleBar.getText());
        panel.add(titleBar);
        status = new JLabel(Utils.getLocString("EXPORT_READY1") + " " + Utils.getLocString("EXPORT_START") + " " + Utils.getLocString("EXPORT_READY2")); //status shows Press start to begin
        status.setForeground(Main.TEXT);
        panel.add(status);
        progress = new ProgressBar(0, 100000);
        panel.add(progress);
        t = new Timer(20, new ActionListener() { //timer to update progress bar and status
            @Override
            public void actionPerformed(ActionEvent e) {
                if (player == null) { //export not started or already finished
                    return;
                }
                if (player.isPlaying()) { //exporting
                    status.setText(Utils.getLocString("EXPORT_EXPORT_IN_PROGRESS"));
                    progress.setValue((int) (progress.getMaximum() * player.getPosition() / player.getLength()));
                } else { //export finished
                    player.stopPlaying(); //stop player to save resources
                    player = null;
                    t.stop(); //stop this timer, no longer needed
                    dispose(); //close the window
                    MessageBox.showMessage(Utils.getLocString("EXPORT_EXPORT_COMPLETE")); //alert the user
                }
            }
        });
        t.setRepeats(true);
        t.start();
        cancel = new DialogButton(Utils.getLocString("EXPORT_CANCEL")) { //cancel button

            @Override
            public void actionPerformed() {
                if (player == null) { //not exporting, just close the window
                    dispose();
                } else { //was exporting
                    if (MessageBox.showYesNoDialog(Utils.getLocString("EXPORT_EXPORT_CANCEL")) == MessageBox.SELECTION_YES) { //do you want to cancel the export?
                        t.stop(); //stop the timer, no longer needed
                        if (player != null) {
                            player.stopPlaying(); //stop the player (but only if it didn't already finish while we were waiting for the user to confirm the cancel)
                        }
                        player=null;
                        f.delete(); //delete the incomplete file
                        dispose();
                    }
                }
            }
        };
        panel.add(cancel);
        addWindowListener(new WindowAdapter() { //alt+f4 = pressing cancel

            @Override
            public void windowClosing(WindowEvent e) {
                cancel.actionPerformed();
            }
        });
        start = new DialogButton(Utils.getLocString("EXPORT_START")) { //start button
            @Override
            public void actionPerformed() {
                try {
                    ISoundDevice dev = null;
                    if (format == FORMAT_FLAC) {
                        dev = new FLACFileSoundBackend(f.getAbsolutePath(), 44100, 1);
                    }
                    if (format == FORMAT_WAV) {
                        dev = new WavFileSoundBackend(f.getAbsolutePath(), 44100, 1);
                    }
                    if(format==FORMAT_MP3){
                        dev=new MP3FileSoundBackend(f.getAbsolutePath(), 44100, 1, 96);
                    }
                    player = new IsochronicRenderer(p, dev, p.loops() ? nLoops : 0); //initialize player with selected output format
                    player.play(); //start exporting
                    start.setEnabled(false); //disable start button
                    if (p.loops()) { //disable loop +/-
                        loop.setEnabled(false);
                        plus.setEnabled(false);
                        minus.setEnabled(false);
                    }
                } catch (Throwable ex) {
                    //something went wrong, notify the user, stop everything and close
                    if (player != null) {
                        player.stopPlaying();
                    }
                    t.stop();
                    dispose();
                    MessageBox.showError(Utils.getLocString("EXPORT_FAIL"));
                }
            }
        };
        panel.add(start);
        addComponentListener(new ComponentAdapter() {
            
            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }
        });
        if (p.loops()) { //if the preset loops, the user can choose how many times to repeat the loop
            loop = new JLabel(Utils.getLocString("EXPORT_LOOP") + "  " + nLoops + "  " + Utils.getLocString("EXPORT_TIMES")); //shows Repeat loop 3 times
            loop.setForeground(Main.TEXT);
            loop.setHorizontalAlignment(JLabel.TRAILING);
            panel.add(loop);
            plus = new JLabel("+"); //+ button
            plus.setForeground(Main.TEXT);
            plus.setFont(Main.LARGE_FONT);
            plus.setHorizontalAlignment(JLabel.CENTER);
            plus.addMouseListener(new MouseAdapter() { //increase nLoop by 1 and update JLabel loop text

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (plus.isEnabled()) {
                        loop.setText(Utils.getLocString("EXPORT_LOOP") + "  " + (++nLoops) + "  " + Utils.getLocString("EXPORT_TIMES"));
                    }
                }
                
            });
            panel.add(plus);
            minus = new JLabel("-"); //-button
            minus.setForeground(Main.TEXT);
            minus.setFont(Main.LARGE_FONT);
            minus.setHorizontalAlignment(JLabel.CENTER);
            minus.addMouseListener(new MouseAdapter() { //decrease nLoop by 1 and update JLabel loop text

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (minus.isEnabled() && nLoops > 0) {
                        loop.setText(Utils.getLocString("EXPORT_LOOP") + "  " + (--nLoops) + "  " + Utils.getLocString("EXPORT_TIMES"));
                    }
                }
                
            });
            panel.add(minus);
        }
        //create listener for keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if(!isFocused()) return false;
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED) { //esc = close
                    cancel.actionPerformed();
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) { //enter = confirm
                    start.actionPerformed();
                }
                if(p.loops()){
                    if (e.getKeyCode() == KeyEvent.VK_PLUS && e.getID() == KeyEvent.KEY_PRESSED) { //+ = increase loop count
                        plus.getMouseListeners()[0].mouseReleased(null);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_MINUS && e.getID() == KeyEvent.KEY_PRESSED) { //- = decrease loop count
                        minus.getMouseListeners()[0].mouseReleased(null);
                    }
                }
                return false;
            }
        });
        fixupLayout();
    }
    
    private void fixupLayout() {
        panel.setSize(getWidth(), getHeight()); //stretch the container over the entire window
        titleBar.setBounds(1, 1, getWidth() - 2, Main.TITLE_BAR_HEIGHT); //draggable titlebar at the top
        int y = titleBar.getY() + titleBar.getHeight();
        status.setBounds(1 + Main.GENERIC_MARGIN, y, getWidth() - 2 - 2 * Main.GENERIC_MARGIN, (int) (Main.BASE_FONT_PX + 2 * Main.GENERIC_MARGIN)); //status right below it
        y += status.getHeight();
        progress.setBounds(1 + Main.GENERIC_MARGIN, y, getWidth() - 2 - 2 * Main.GENERIC_MARGIN, BAR_HEIGHT); //progress bar below the status, takes the whole line
        y += BAR_HEIGHT + Main.GENERIC_MARGIN;
        if (loop != null) { //optionally show loop controls at the right
            int x = getWidth() - 1 - Main.GENERIC_MARGIN - PLUS_MINUS_WIDTH;
            plus.setBounds(x, y, PLUS_MINUS_WIDTH, PLUS_MINUS_HEIGHT); //+button on the right
            x -= PLUS_MINUS_WIDTH;
            minus.setBounds(x, y, PLUS_MINUS_WIDTH, PLUS_MINUS_HEIGHT); //-button at its left
            loop.setBounds(1 + Main.GENERIC_MARGIN, y, x - 1 - Main.GENERIC_MARGIN, PLUS_MINUS_HEIGHT); //JLabel loop takes up the rest of the space on that line
        }
        int x = getWidth() - 1 - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_WIDTH;
        cancel.setBounds(x, getHeight() - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT); //cancel button in lower-right corner
        x -= Main.GENERIC_MARGIN + DialogButton.DIALOG_BUTTON_WIDTH;
        start.setBounds(x, getHeight() - Main.GENERIC_MARGIN - DialogButton.DIALOG_BUTTON_HEIGHT, DialogButton.DIALOG_BUTTON_WIDTH, DialogButton.DIALOG_BUTTON_HEIGHT); //start button at its left
        repaint(); //must force repaint on some systems
    }

    //shows the dialog and waits for it to close
    public static void export(Preset p, File f, int format) {
        ExportDialog d = new ExportDialog(p, f, format);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - DIALOG_WIDTH / 2), y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - DIALOG_HEIGHT / 2);
        d.setLocation(x, y);
        d.setVisible(true);
        while (d.isVisible()) {
            try {
                Thread.sleep(100); 
            } catch (InterruptedException ex) {
            }
        }
    }
}
