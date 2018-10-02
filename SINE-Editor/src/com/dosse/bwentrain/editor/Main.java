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

import com.dosse.binaural.HBXConverter;
import com.dosse.bwentrain.core.EntrainmentTrack;
import com.dosse.bwentrain.core.Envelope;
import com.dosse.bwentrain.core.Preset;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * the editor main class. start reading the code from here.
 *
 * @author dosse
 */
public class Main extends javax.swing.JFrame {

    private static final boolean IS_MACOS = System.getProperty("os.name").toLowerCase().startsWith("mac")
            || System.getProperty("os.name").toLowerCase().contains("os x"); //mac uses "events" for file opens instead of standard command line args because it's a special snowflake
    public static final float SCALE = calculateScale(); //used for DPI scaling. multiply each size by this factor.
    //calculates SCALE based on screen DPI. target DPI is 96, so if DPI=96, SCALE=1. Min DPI is 72.

    private static final float calculateScale() {
        if (IS_MACOS)
            return 1;
        float dpi = (float) Toolkit.getDefaultToolkit().getScreenResolution();
        return (dpi < 72 ? 72 : dpi) / 96f;
    }

    //used for swing stuff
    public static final ColorUIResource METAL_PRIMARY1 = new ColorUIResource(64, 64, 64),
            METAL_PRIMARY2 = new ColorUIResource(192, 192, 192),
            METAL_PRIMARY3 = new ColorUIResource(220, 220, 220),
            METAL_SECONDARY1 = new ColorUIResource(160, 160, 160),
            METAL_SECONDARY2 = new ColorUIResource(192, 192, 192),
            DEFAULT_BACKGROUND = new ColorUIResource(240, 240, 240); //background color for all windows

    public static final float BIG_TEXT_SIZE = 21 * SCALE, TEXT_SIZE = 12 * SCALE, SUB_TEXT_SIZE = 12 * SCALE, SMALL_TEXT_SIZE = 10 * SCALE;

    private static final int DEFAULT_WIDTH = 900, DEFAULT_HEIGHT = 600; //initial size of this window. will be scaled
    private static final int LIST_WIDTH = 200;

    private Preset preset; //preset currently being edited

    private DefaultListModel<String> listModel; //contents of the list on the left
    private ArrayList<EntrainmentTrackEditPanel> etEditors; //EntrainmentTrackEdiPanels for each EntrainmentTrack in the Preset
    private NoiseEditPanel noiseEdit; //NoiseEditPanel for the only noise Envelope in the Preset
    private DetailsEditPanel detailsEdit; //editor for preset details
    private int selection; //currently selected EntrainmentTrack or -1 if none is selected
    private boolean modified = false; //used to show the "you will lose all unsaved changes" message only when needed

    /**
     * begin undo stack implementation
     */
    private List<Preset> undoStack = new ArrayList<Preset>();
    private int undoStackPointer = 0;

    private void undo() {
        int sel = list.getSelectedIndex();
        loadPreset(undoStack.get(--undoStackPointer - 1).clone());
        list.setSelectedIndex(sel == listModel.getSize() ? sel - 1 : sel);
        undo.setEnabled(undoStackPointer > 1);
        redo.setEnabled(true);
        modified = true;
    }

    private void redo() {
        int sel = list.getSelectedIndex();
        loadPreset(undoStack.get(undoStackPointer++).clone());
        list.setSelectedIndex(sel == listModel.getSize() ? sel - 1 : sel);
        undo.setEnabled(true);
        redo.setEnabled(undoStackPointer < undoStack.size());
        modified = true;
    }

    private void saveToUndoStack() {
        if (undoStackPointer > 0 && preset.equals(undoStack.get(undoStackPointer - 1))) {
            return; //nothing has changed from the previous save
        }
        while (undoStack.size() > undoStackPointer) {
            undoStack.remove(undoStackPointer);
        }
        undoStack.add(preset.clone());
        undoStackPointer++;
        undo.setEnabled(undoStackPointer > 1);
        redo.setEnabled(false);
    }

    private void clearUndoStack() {
        undoStack.clear();
        undoStackPointer = 0;
        undo.setEnabled(false);
        redo.setEnabled(false);
    }

    /**
     * end undo stack implementation
     */
    /**
     * this method is used to set the current preset in the player. it is called
     * after each opereation. IsochronicBackend will clone it to avoid
     * concurrent modification
     */
    private void updatePlayer() {
        playerPanel.setPreset(preset);
    }

    //file filter for flac files, used when exporting a preset
    public static final FileFilter FLAC_FILE_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".flac");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("MAIN_FLAC_FILE_DESCRIPTION");
        }
    };
    //file filter for wav files, used when exporting a preset
    public static final FileFilter WAV_FILE_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("MAIN_WAV_FILE_DESCRIPTION");
        }
    };
    //file filter for mp3 files, used when exporting a preset
    public static final FileFilter MP3_FILE_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".mp3");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("MAIN_MP3_FILE_DESCRIPTION");
        }
    };

    //file filter for preset files, used in load/save dialogs
    private static final FileFilter PRESET_FILE_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".sin");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("PRESET_FILE_FILTER_DESCRIPTION");
        }
    };

    //HBX preset file filter. used in the import from hbx dialog
    private static final FileFilter HBX_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            String n = f.getName().toLowerCase();
            return f.isDirectory() || n.endsWith(".hbx") || n.endsWith(".hbs") || n.endsWith(".hbl");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("HBX_PRESET_FILE_FILTER_DESCRIPTION");
        }
    };

    private static final float OPTIMIZE_TOLERANCE = 0.05f; //when running optimize, if a point has the same value as the one before it and the one after it, it is removed. 2 values are considered identical if they differ by less than OPTIMIZE_TOLERANCE

    private File lastFile = null, //last opened file (used for quick save)
            lastDir = null; //last directory browsed with a file chooser

    private static final int COMPLEXITY_1 = 500, COMPLEXITY_2 = 1000, COMPLEXITY_3 = 2000, COMPLEXITY_4 = 6000; //levels of complexity from lowest to highest

    public static final float MAX_BASE_FREQUENCY = 1500, //editor limitations
            MAX_ENTRAINMENT_FREQUENCY = 40;

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        setSize((int) (SCALE * DEFAULT_WIDTH + getInsets().left + getInsets().right), (int) (SCALE * DEFAULT_HEIGHT + getInsets().top + getInsets().bottom)); //set size to default size, properly scaled
        setMinimumSize(getSize()); //initial size is also minimum size
        newPreset(); //editor opens with an empty preset loaded
        leftPanel.setPreferredSize(new Dimension((int) (LIST_WIDTH * SCALE), leftPanel.getHeight())); //left panel is always LIST_WIDTH pixels wide. there was no better way to do it
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!isFocused()) {
                    return false;
                }
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE && e.getID() == KeyEvent.KEY_PRESSED) { //CTRL+Space toggles preview
                    playerPanel.togglePlay();
                }
                return false;
            }
        });
        setLocationRelativeTo(null); //center the window on the screen
    }

    private void forceLayoutUpdate() {
        noiseEdit.forceLayoutUpdate();
        if (selection != -1) {
            etEditors.get(selection).forceLayoutUpdate();
        }
        detailsEdit.forceLayoutUpdate();
    }

    private void newPreset() {
        //empty preset
        Preset p = new Preset(300, -1, Utils.getLocString("NEW_PRESET_TITLE"), Utils.getLocString("NEW_PRESET_AUTHOR"), Utils.getLocString("NEW_PRESET_DESCRIPTION"));
        //set noise track to 1 point at 35%
        p.getNoiseEnvelope().setVal(0, 0.35f);
        //set first entrainment track at 10hz, 440hz carrier, 100%volume
        EntrainmentTrack et = p.getEntrainmentTrack(0);
        et.getBaseFrequencyEnvelope().setVal(0, 440);
        et.getVolumeEnvelope().setVal(0, 1);
        et.getEntrainmentFrequencyEnvelope().setVal(0, 10);
        //load it in the editor
        loadPreset(p);
        if (playerPanel.isPlaying()) {
            playerPanel.stop();
        }
        clearUndoStack();
        saveToUndoStack();
        lastFile = null; //disable quick saving (we're not working on a file)
        modified = false;
    }

    /**
     * load preset from .sin file
     *
     * @param x file to load
     */
    private void loadPreset(File x) {
        try {
            Preset p;
            try {
                //read XML document
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);
                factory.setValidating(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(x.getAbsolutePath()));
                Element e = doc.getDocumentElement();
                p = new Preset(e); //loads and does a lot of checks
            } catch (Throwable t) {
                throw new Exception(Utils.getLocString("MAIN_ERROR_INVALID_PRESET") + ": " + t);
            }
            try {
                //check for editor compatibility
                if (p.getLength() < 10 || p.getLength() >= 36000) { //too short or too long (10 seconds to 9:59:59)
                    throw new Exception();
                }
                Envelope noise = p.getNoiseEnvelope();
                for (int i = 0; i < noise.getPointCount(); i++) {
                    if (noise.getVal(i) < 0 || noise.getVal(i) > 1) { //invalid noise volumes
                        throw new Exception();
                    }
                }
                for (int t = 0; t < p.getEntrainmentTrackCount(); t++) {
                    EntrainmentTrack et = p.getEntrainmentTrack(t);
                    Envelope vol = et.getVolumeEnvelope(), baseF = et.getBaseFrequencyEnvelope(), ent = et.getEntrainmentFrequencyEnvelope();
                    for (int i = 0; i < baseF.getPointCount(); i++) {
                        if (baseF.getVal(i) < 0 || baseF.getVal(i) > MAX_BASE_FREQUENCY) { //invalid base frequencies
                            throw new Exception();
                        }
                    }
                    for (int i = 0; i < ent.getPointCount(); i++) {
                        if (ent.getVal(i) < 0 || ent.getVal(i) > MAX_ENTRAINMENT_FREQUENCY) { //invalid entrainment frequencies
                            throw new Exception();
                        }
                    }
                    for (int i = 0; i < vol.getPointCount(); i++) {
                        if (vol.getVal(i) < 0 || vol.getVal(i) > 1) { //invalid volumes
                            throw new Exception();
                        }
                    }
                }
                //everything is fine, load it in the editor
                loadPreset(p);
                if (playerPanel.isPlaying()) {
                    playerPanel.stop();
                }
                modified = false; //disable you will lose unsaved changes dialog
                clearUndoStack();
                saveToUndoStack();
                lastFile = x; //we're editing a preset saved on a file, activate quick saving
            } catch (Throwable t) {
                //something went wrong
                throw new Exception(Utils.getLocString("MAIN_ERROR_UNSUPPORTED_FEATURES"));
            }
        } catch (Throwable t) {
            //something went wrong, show the error to the user
            JOptionPane.showMessageDialog(rootPane, t.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * load preset instance in the editor WITHOUT CLONING IT!
     *
     * @param p
     */
    private void loadPreset(Preset p) {
        preset = p;
        //create all the elements in the list on the left and all the needed editor panels
        listModel = new DefaultListModel<String>();
        list.setModel(listModel);
        listModel.addElement(Utils.getLocString("LIST_PRESET_DETAILS"));
        detailsEdit = new DetailsEditPanel(p) {

            @Override
            public void lengthChangeRequested() {
                //user clicked set duration in details panel. same action as preset>set duration
                setDurationActionPerformed(null);
            }

            @Override
            public void onEdit() {
                modified = true;
                saveToUndoStack();
                updatePlayer();
            }
        };
        listModel.addElement(Utils.getLocString("LIST_NOISE"));
        noiseEdit = new NoiseEditPanel(p) {

            @Override
            public void onEdit() {
                modified = true;
                saveToUndoStack();
                updatePlayer();
            }
        };
        etEditors = new ArrayList<>();
        for (int i = 0; i < p.getEntrainmentTrackCount(); i++) {
            etEditors.add(new EntrainmentTrackEditPanel(p, i) {

                @Override
                public void onEdit() {
                    modified = true;
                    saveToUndoStack();
                    updatePlayer();
                }
            });
            listModel.addElement(Utils.getLocString("LIST_ENTTRACK") + " " + (i + 1));
        }
        list.setSelectedIndex(0); //select details editor
        updatePlayer();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editPanel = new javax.swing.JPanel();
        leftPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        playerPanel = new com.dosse.bwentrain.editor.PlayerPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newPreset = new javax.swing.JMenuItem();
        load = new javax.swing.JMenuItem();
        save = new javax.swing.JMenuItem();
        saveAs = new javax.swing.JMenuItem();
        export = new javax.swing.JMenuItem();
        share = new javax.swing.JMenuItem();
        importHBX = new javax.swing.JMenuItem();
        quit = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undo = new javax.swing.JMenuItem();
        redo = new javax.swing.JMenuItem();
        presetMenu = new javax.swing.JMenu();
        addTrack = new javax.swing.JMenuItem();
        cloneTrack = new javax.swing.JMenuItem();
        removeTrack = new javax.swing.JMenuItem();
        setDuration = new javax.swing.JMenuItem();
        complexity = new javax.swing.JMenuItem();
        optimize = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        help = new javax.swing.JMenuItem();
        tutorial = new javax.swing.JMenuItem();
        about = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/dosse/bwentrain/editor/locale"); // NOI18N
        setTitle(bundle.getString("Main.title")); // NOI18N
        setAutoRequestFocus(false);
        setIconImage(Utils.loadUnscaled("/com/dosse/bwentrain/editor/images/logoIcon.png").getImage()
        );
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        javax.swing.GroupLayout editPanelLayout = new javax.swing.GroupLayout(editPanel);
        editPanel.setLayout(editPanelLayout);
        editPanelLayout.setHorizontalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 522, Short.MAX_VALUE)
        );
        editPanelLayout.setVerticalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 533, Short.MAX_VALUE)
        );

        list.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "This", "list", "is", "automatically", "generated" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list);

        playerPanel.setFocusable(false);

        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(playerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        fileMenu.setText(bundle.getString("Main.fileMenu.text")); // NOI18N

        newPreset.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newPreset.setText(bundle.getString("Main.newPreset.text")); // NOI18N
        newPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPresetActionPerformed(evt);
            }
        });
        fileMenu.add(newPreset);

        load.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        load.setText(bundle.getString("Main.load.text")); // NOI18N
        load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadActionPerformed(evt);
            }
        });
        fileMenu.add(load);

        save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        save.setText(bundle.getString("Main.save.text")); // NOI18N
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        fileMenu.add(save);

        saveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        saveAs.setText(bundle.getString("Main.saveAs.text")); // NOI18N
        saveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsActionPerformed(evt);
            }
        });
        fileMenu.add(saveAs);

        export.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        export.setText(bundle.getString("Main.export.text")); // NOI18N
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportActionPerformed(evt);
            }
        });
        fileMenu.add(export);

        share.setText(bundle.getString("Main.share.text")); // NOI18N
        share.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shareActionPerformed(evt);
            }
        });
        fileMenu.add(share);

        importHBX.setText(bundle.getString("Main.importHBX.text")); // NOI18N
        importHBX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importHBXActionPerformed(evt);
            }
        });
        fileMenu.add(importHBX);

        quit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        quit.setText(bundle.getString("Main.quit.text")); // NOI18N
        quit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitActionPerformed(evt);
            }
        });
        fileMenu.add(quit);

        jMenuBar1.add(fileMenu);

        editMenu.setText(bundle.getString("Main.editMenu.text")); // NOI18N

        undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undo.setText(bundle.getString("Main.undo.text")); // NOI18N
        undo.setEnabled(false);
        undo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoActionPerformed(evt);
            }
        });
        editMenu.add(undo);

        redo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        redo.setText(bundle.getString("Main.redo.text")); // NOI18N
        redo.setEnabled(false);
        redo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoActionPerformed(evt);
            }
        });
        editMenu.add(redo);

        jMenuBar1.add(editMenu);

        presetMenu.setText(bundle.getString("Main.presetMenu.text")); // NOI18N

        addTrack.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        addTrack.setText(bundle.getString("Main.addTrack.text")); // NOI18N
        addTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTrackActionPerformed(evt);
            }
        });
        presetMenu.add(addTrack);

        cloneTrack.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        cloneTrack.setText(bundle.getString("Main.cloneTrack.text")); // NOI18N
        cloneTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cloneTrackActionPerformed(evt);
            }
        });
        presetMenu.add(cloneTrack);

        removeTrack.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        removeTrack.setText(bundle.getString("Main.removeTrack.text")); // NOI18N
        removeTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeTrackActionPerformed(evt);
            }
        });
        presetMenu.add(removeTrack);

        setDuration.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        setDuration.setText(bundle.getString("Main.setDuration.text")); // NOI18N
        setDuration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setDurationActionPerformed(evt);
            }
        });
        presetMenu.add(setDuration);

        complexity.setText(bundle.getString("Main.complexity.text")); // NOI18N
        complexity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                complexityActionPerformed(evt);
            }
        });
        presetMenu.add(complexity);

        optimize.setText(bundle.getString("Main.optimize.text")); // NOI18N
        optimize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optimizeActionPerformed(evt);
            }
        });
        presetMenu.add(optimize);

        jMenuBar1.add(presetMenu);

        helpMenu.setText("?"); // NOI18N

        help.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        help.setText(bundle.getString("Main.help.text")); // NOI18N
        help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpActionPerformed(evt);
            }
        });
        helpMenu.add(help);

        tutorial.setText(bundle.getString("Main.tutorial.text")); // NOI18N
        tutorial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tutorialActionPerformed(evt);
            }
        });
        helpMenu.add(tutorial);

        about.setText(bundle.getString("Main.about.text")); // NOI18N
        about.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutActionPerformed(evt);
            }
        });
        helpMenu.add(about);

        jMenuBar1.add(helpMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(leftPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(leftPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(3, 3, 3))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private int lastListSelectedIndex = -1;
    private void listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listValueChanged
        if (list.getSelectedIndex() == lastListSelectedIndex) { //selected the same track?
            return;
        } else {
            lastListSelectedIndex = list.getSelectedIndex();
        }
        if (list.getSelectedIndex() < 0) { //can happen when deleting an item from the list
            selection = -1;
            return;
        }
        //empty the editor area on the right and put a different panel in
        editPanel.removeAll();
        GroupLayout editPanelLayout = new GroupLayout(editPanel);
        editPanel.setLayout(editPanelLayout);
        if (list.getSelectedIndex() == 0) {
            selection = -1;
            editPanelLayout.setHorizontalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(detailsEdit, javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getWidth(), Short.MAX_VALUE)
            );
            editPanelLayout.setVerticalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(detailsEdit, javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getHeight(), Short.MAX_VALUE)
            );
        } else if (list.getSelectedIndex() == 1) {
            selection = -1;
            editPanelLayout.setHorizontalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(noiseEdit, javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getWidth(), Short.MAX_VALUE)
            );
            editPanelLayout.setVerticalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(noiseEdit, javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getHeight(), Short.MAX_VALUE)
            );
        } else {
            selection = list.getSelectedIndex() - 2;
            editPanelLayout.setHorizontalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(etEditors.get(selection), javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getWidth(), Short.MAX_VALUE)
            );
            editPanelLayout.setVerticalGroup(
                    editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(etEditors.get(selection), javax.swing.GroupLayout.DEFAULT_SIZE, editPanel.getHeight(), Short.MAX_VALUE)
            );
        }
        //disable/enable some elements of the track menu if necessary
        if (selection == -1) { //preset details or noise track selected, disable cloning and removing
            cloneTrack.setEnabled(false);
            removeTrack.setEnabled(false);
        } else {//entrainment track selected, enable cloning
            cloneTrack.setEnabled(true);
            if (preset.getEntrainmentTrackCount() > 1) { //1+ entrainment tracks are present, enable remove track
                removeTrack.setEnabled(true);
            } else {
                removeTrack.setEnabled(false);
            }
        }

        pack();
    }//GEN-LAST:event_listValueChanged

    private void newPresetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newPresetActionPerformed
        if (showLoseAllUnsavedChangesDialog()) {
            newPreset();
        }
    }//GEN-LAST:event_newPresetActionPerformed

    private void removeTrackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeTrackActionPerformed
        int sel = selection;
        list.setSelectedIndex(0); //remove focus from that track to avoid nullpointerexceptions
        //fix track names in list and track ids in editor panels
        for (int i = sel + 2; i < listModel.getSize(); i++) {
            listModel.set(i, Utils.getLocString("LIST_ENTTRACK") + " " + (i - 2));
            etEditors.get(i - 2).trackId--;
        }
        //remove track from list, preset and editors
        etEditors.remove(sel);
        listModel.remove(sel + 2);
        preset.removeEntrainmentTrack(sel);
        modified = true;
        //select next track in the list
        list.setSelectedIndex(sel + 2 == listModel.getSize() ? sel + 1 : sel + 2);
        saveToUndoStack();
        updatePlayer();
    }//GEN-LAST:event_removeTrackActionPerformed

    private void addTrackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTrackActionPerformed
        //add new empty entrainment track
        preset.addEntrainmentTrack();
        //set at 10hz entrainment, 1.0 volume, 440hz carrier
        EntrainmentTrack t = preset.getEntrainmentTrack(preset.getEntrainmentTrackCount() - 1);
        t.getEntrainmentFrequencyEnvelope().setVal(0, 10);
        t.getVolumeEnvelope().setVal(0, 1);
        t.getBaseFrequencyEnvelope().setVal(0, 440);
        etEditors.add(new EntrainmentTrackEditPanel(preset, preset.getEntrainmentTrackCount() - 1) {

            @Override
            public void onEdit() {
                modified = true;
                saveToUndoStack();
                updatePlayer();
            }
        });
        listModel.addElement(Utils.getLocString("LIST_ENTTRACK") + " " + preset.getEntrainmentTrackCount());
        list.setSelectedIndex(listModel.getSize() - 1); //select it
        saveToUndoStack();
        updatePlayer();
    }//GEN-LAST:event_addTrackActionPerformed

    private void cloneTrackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cloneTrackActionPerformed
        //clone track
        preset.cloneTrack(selection);
        etEditors.add(new EntrainmentTrackEditPanel(preset, preset.getEntrainmentTrackCount() - 1) {

            @Override
            public void onEdit() {
                modified = true;
                saveToUndoStack();
                updatePlayer();
            }
        });
        listModel.addElement(Utils.getLocString("LIST_ENTTRACK") + " " + preset.getEntrainmentTrackCount());
        list.setSelectedIndex(listModel.getSize() - 1); //select cloned track
        saveToUndoStack();
        updatePlayer();
    }//GEN-LAST:event_cloneTrackActionPerformed

    private void setDurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setDurationActionPerformed
        if (LengthEditDialog.setLength(preset)) { //change length
            modified = true; //setLength returns true if user changed the length
            saveToUndoStack();
            updatePlayer();
            //redraw editors
            forceLayoutUpdate();
        }
    }//GEN-LAST:event_setDurationActionPerformed

    private void loadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadActionPerformed
        if (!showLoseAllUnsavedChangesDialog()) {
            return;
        }
        JFileChooser c = new JFileChooser();
        c.setFileFilter(PRESET_FILE_FILTER);
        c.setMultiSelectionEnabled(false);
        c.setAcceptAllFileFilterUsed(true);
        if (lastDir != null) {
            c.setCurrentDirectory(lastDir);
        }
        c.showOpenDialog(rootPane);
        File x = c.getSelectedFile();
        lastDir = c.getCurrentDirectory();
        if (x == null) { //no file selected
            return;
        }
        loadPreset(x);
        playerPanel.stop();
    }//GEN-LAST:event_loadActionPerformed

    private void saveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsActionPerformed
        JFileChooser c = new JFileChooser();
        c.setFileFilter(PRESET_FILE_FILTER);
        if (lastDir != null) {
            c.setCurrentDirectory(lastDir);
        }
        c.setAcceptAllFileFilterUsed(false);
        c.showSaveDialog(rootPane);
        File x = c.getSelectedFile();
        lastDir = c.getCurrentDirectory();
        if (x == null) { //no file selected
            return;
        }
        if (!x.getName().toLowerCase().endsWith(".sin")) { //must add extension
            x = new File(x.getAbsolutePath() + ".sin");
        }
        if (x.exists()) { //overwrite?
            int sel = JOptionPane.showConfirmDialog(rootPane, Utils.getLocString("MAIN_CONFIRM_OVERWRITE"), getTitle(), JOptionPane.YES_NO_OPTION);
            if (sel == -1) {
                return; //cancelled dialog
            }
            if (sel == JOptionPane.NO_OPTION) {
                saveAsActionPerformed(null);
                return;
            }
        }
        savePreset(x);
        lastFile = x; //the preset is on a file, enable quick saving
    }//GEN-LAST:event_saveAsActionPerformed
    private void savePreset(File x) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(x));
            out.write(preset.toString() + "\n"); //write preset as xml
            out.flush();
            out.close();
            modified = false;
        } catch (Throwable t) {
            //something went wrong, show error
            JOptionPane.showMessageDialog(rootPane, Utils.getLocString("MAIN_SAVE_FAIL") + ": " + t.toString(), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (showLoseAllUnsavedChangesDialog()) {
            System.exit(0);
        }
    }//GEN-LAST:event_formWindowClosing

    private void quitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitActionPerformed
        formWindowClosing(null);
    }//GEN-LAST:event_quitActionPerformed

    private void complexityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_complexityActionPerformed
        int complexity = preset.complexity();
        //show complexity
        String s = "Complexity: " + complexity + "\n";
        if (complexity < COMPLEXITY_1) {
            s += Utils.getLocString("COMPLEXITY_1");
        } else if (complexity < COMPLEXITY_2) {
            s += Utils.getLocString("COMPLEXITY_2");
        } else if (complexity < COMPLEXITY_3) {
            s += Utils.getLocString("COMPLEXITY_3");
        } else if (complexity < COMPLEXITY_4) {
            s += Utils.getLocString("COMPLEXITY_4");
        } else {
            s += Utils.getLocString("COMPLEXITY_5");
        }
        if (complexity >= COMPLEXITY_3) {
            s += "\n" + Utils.getLocString("COMPLEXITY_HINTS");
        }
        JOptionPane.showMessageDialog(rootPane, s, getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_complexityActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        if (lastFile != null) {
            savePreset(lastFile);
        } else {
            saveAsActionPerformed(evt);
        }
    }//GEN-LAST:event_saveActionPerformed

    private void helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpActionPerformed
        try {
            Utils.openInBrowser(new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath() + "/" + Utils.getLocString("HELP_PATH")).toURI()); //look for manual in program directory
        } catch (Exception e) {
            if (!(e instanceof IOException)) { //no browser
                JOptionPane.showMessageDialog(rootPane, Utils.getLocString("HELP_ERROR"), getTitle(), JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    Utils.openInBrowser(new File(Utils.getLocString("HELP_PATH")).toURI()); //look for manual in working directory
                } catch (Exception e2) {
                    //file not found (both methods failed)
                    JOptionPane.showMessageDialog(rootPane, Utils.getLocString("MANUAL_MISSING"), getTitle(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_helpActionPerformed

    private void aboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutActionPerformed
        AboutDialog.about();
    }//GEN-LAST:event_aboutActionPerformed

    private void tutorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tutorialActionPerformed
        try {
            Utils.openInBrowser(new URI(Utils.getLocString("VIDEO_URL")));
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, Utils.getLocString("BROWSER_ERROR") + " " + Utils.getLocString("PRESETS_URL"), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_tutorialActionPerformed

    private void shareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shareActionPerformed
        try {
            Utils.openInBrowser(new URI(Utils.getLocString("PRESETS_URL")));
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, Utils.getLocString("BROWSER_ERROR") + " " + Utils.getLocString("PRESETS_URL"), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_shareActionPerformed

    private void importHBXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importHBXActionPerformed
        if (!showLoseAllUnsavedChangesDialog()) {
            return;
        }
        JFileChooser c = new JFileChooser();
        c.setFileFilter(HBX_FILE_FILTER);
        if (lastDir != null) {
            c.setCurrentDirectory(lastDir);
        }
        c.setMultiSelectionEnabled(false);
        c.showOpenDialog(this);
        File x = c.getSelectedFile();
        lastDir = c.getCurrentDirectory();
        if (x == null) {
            return;
        }
        try {
            loadPreset(HBXConverter.convert(x));
            playerPanel.stop();
            modified = true;
            clearUndoStack();
            saveToUndoStack();
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, Utils.getLocString("MAIN_ERROR_INVALID_PRESET") + ": " + t.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_importHBXActionPerformed

    private void optimizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeActionPerformed
        int removedPoints = preset.optimizePoints(OPTIMIZE_TOLERANCE), removedTracks = preset.removeUselessTracks();
        int sel = list.getSelectedIndex();
        loadPreset(preset);
        list.setSelectedIndex(sel >= listModel.getSize() ? listModel.getSize() - 1 : sel);
        JOptionPane.showMessageDialog(rootPane, Utils.getLocString("OPTIMIZE_COMPLETE") + "\n" + Utils.getLocString("OPTIMIZE_REMOVED") + " " + removedPoints + " " + Utils.getLocString("OPTIMIZE_POINTS") + ".\n" + Utils.getLocString("OPTIMIZE_REMOVED") + " " + removedTracks + " " + Utils.getLocString("OPTIMIZE_TRACKS") + ".", getTitle(), JOptionPane.INFORMATION_MESSAGE);
        modified = true;
        saveToUndoStack();
    }//GEN-LAST:event_optimizeActionPerformed

    private void undoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoActionPerformed
        undo();
    }//GEN-LAST:event_undoActionPerformed

    private void redoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoActionPerformed
        redo();
    }//GEN-LAST:event_redoActionPerformed

    private void exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportActionPerformed
        JFileChooser c = new JFileChooser(lastDir);
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(MP3_FILE_FILTER);
        c.addChoosableFileFilter(FLAC_FILE_FILTER);
        c.addChoosableFileFilter(WAV_FILE_FILTER);
        c.showSaveDialog(this);
        File x = c.getSelectedFile();
        if (x == null) {
            return;
        }
        if (c.getFileFilter() == Main.MP3_FILE_FILTER && !x.getName().toLowerCase().endsWith(".mp3")) {
            x = new File(x.getAbsolutePath() + ".mp3");
        }
        if (c.getFileFilter() == Main.FLAC_FILE_FILTER && !x.getName().toLowerCase().endsWith(".flac")) {
            x = new File(x.getAbsolutePath() + ".flac");
        }
        if (c.getFileFilter() == Main.WAV_FILE_FILTER && !x.getName().toLowerCase().endsWith(".wav")) {
            x = new File(x.getAbsolutePath() + ".wav");
        }
        if (x.exists()) { //overwrite?
            int sel = JOptionPane.showConfirmDialog(rootPane, Utils.getLocString("MAIN_CONFIRM_OVERWRITE"), getTitle(), JOptionPane.YES_NO_OPTION);
            if (sel == -1) {
                return; //cancelled dialog
            }
            if (sel == JOptionPane.NO_OPTION) {
                exportActionPerformed(null);
                return;
            }
        }
        ExportDialog.export(preset, x, c.getFileFilter());
    }//GEN-LAST:event_exportActionPerformed
    /**
     * this method is called before creating a new preset, loading, or quitting.
     * it also takes care of showing the save dialog.
     *
     * @return true if the user wants to quit, false otherwise
     */
    private boolean showLoseAllUnsavedChangesDialog() {
        if (!modified) { //not modified, no need to show anything
            return true;
        } else {
            //show "do you want to save changes" dialog
            int choice = JOptionPane.showConfirmDialog(rootPane, Utils.getLocString("MAIN_CONFIRM_LOSE_CHANGES"), getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.YES_OPTION) { //save changes
                saveActionPerformed(null);
                if (modified) { //user pressed cancel
                    return false;
                }
                return true; //saved
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            return true; //user pressed no
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            //<editor-fold defaultstate="collapsed" desc="MetalTheme (for swing stuff)">
            MetalLookAndFeel.setCurrentTheme(new MetalTheme() {
                @Override
                protected ColorUIResource getPrimary1() {
                    return METAL_PRIMARY1;
                }

                @Override
                protected ColorUIResource getPrimary2() {
                    return METAL_PRIMARY2;
                }

                @Override
                protected ColorUIResource getPrimary3() {
                    return METAL_PRIMARY3;
                }

                @Override
                protected ColorUIResource getSecondary1() {
                    return METAL_SECONDARY1;
                }

                @Override
                protected ColorUIResource getSecondary2() {
                    return METAL_SECONDARY2;
                }

                @Override
                protected ColorUIResource getSecondary3() {
                    return DEFAULT_BACKGROUND;
                }

                @Override
                public String getName() {
                    return "SINE Metal Theme";
                }

                private final FontUIResource SANS_SERIF_FONT = new FontUIResource(Utils.findSansSerifFont());

                @Override
                public FontUIResource getControlTextFont() {
                    return SANS_SERIF_FONT;
                }

                @Override
                public FontUIResource getSystemTextFont() {
                    return SANS_SERIF_FONT;
                }

                @Override
                public FontUIResource getUserTextFont() {
                    return SANS_SERIF_FONT;
                }

                @Override
                public FontUIResource getMenuTextFont() {
                    return SANS_SERIF_FONT;
                }

                @Override
                public FontUIResource getWindowTitleFont() {
                    return SANS_SERIF_FONT;
                }

                @Override
                public FontUIResource getSubTextFont() {
                    return new FontUIResource(Utils.findMonospacedFont().deriveFont(Font.BOLD, SUB_TEXT_SIZE));
                }
            });
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Throwable t) {
            }
            //</editor-fold>
        } catch (Throwable ex) {
        }
        Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH); //allows using the dot instead of the comma when inputting numbers
        Main e = new Main();
        e.setVisible(true);
        if (args.length == 1) { //if a file was specified via command line parameter, load it
            e.loadPreset(new File(args[0]));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem about;
    private javax.swing.JMenuItem addTrack;
    private javax.swing.JMenuItem cloneTrack;
    private javax.swing.JMenuItem complexity;
    private javax.swing.JMenu editMenu;
    private javax.swing.JPanel editPanel;
    private javax.swing.JMenuItem export;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem help;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem importHBX;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JList list;
    private javax.swing.JMenuItem load;
    private javax.swing.JMenuItem newPreset;
    private javax.swing.JMenuItem optimize;
    private com.dosse.bwentrain.editor.PlayerPanel playerPanel;
    private javax.swing.JMenu presetMenu;
    private javax.swing.JMenuItem quit;
    private javax.swing.JMenuItem redo;
    private javax.swing.JMenuItem removeTrack;
    private javax.swing.JMenuItem save;
    private javax.swing.JMenuItem saveAs;
    private javax.swing.JMenuItem setDuration;
    private javax.swing.JMenuItem share;
    private javax.swing.JMenuItem tutorial;
    private javax.swing.JMenuItem undo;
    // End of variables declaration//GEN-END:variables

}
