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
import com.dosse.bwentrain.sound.backends.pc.PCSoundBackend;
import com.github.axet.apple.Apple;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * the Main class of the application. start reading the code from here
 *
 * @author dosse
 */
public class Main extends JFrame {

    private static final boolean DRAG_N_DROP_ENABLED = System.getProperty("os.name").toLowerCase().contains("win"); //drag n drop is only supported on windows, sorry
    private static final boolean IS_MACOS = System.getProperty("os.name").toLowerCase().startsWith("mac")
            || System.getProperty("os.name").toLowerCase().contains("os x"); //mac uses "events" for file opens instead of standard command line args because it's a special snowflake

    public static boolean rejectAppleEvents = false; //used to block file opening while a dialog is open (like when exporting)
    public static final float SCALE = calculateScale(); //used for DPI scaling. multiply each size by this factor.

    //calculates SCALE based on screen DPI. target DPI is 80, so if DPI=80, SCALE=1. Min DPI is 64
    //for mac version, dpi / 80 returns a big window. Using 1 as multiplier you'll get a decent-sized window.
    private static final float calculateScale() {
        float dpi = (float) Toolkit.getDefaultToolkit().getScreenResolution();

        if (IS_MACOS) {
            return 1f;
        }
        return (dpi < 64 ? 64 : dpi) / 80f;
    }

    public static final Color BASE_COLOR = new Color(0xff1f74c9); //slightly desaturated and darkened azure, most colors used in the application are derived from this one

    public static final Color WINDOW_BORDER = BASE_COLOR.darker().darker(), //window borders
            CONTROL_BORDER = new Color(160, 160, 160), //borders of buttons and other controls
            CONTROL_ACCENT = BASE_COLOR, //color of active controls (active as in "they're being clicked right now") and progress bars
            CONTROL_SELECTED = new Color(192, 192, 192); //color of controls being hovered or disabled
    public static final Color MENU_BUTTON = BASE_COLOR, //color of the menu button (the one that says SINE) 
            MENU_BUTTON_HOVER = MENU_BUTTON.brighter(), //color when hovered
            MENU_BUTTON_DOWN = MENU_BUTTON.darker(), //color when pressed
            MENU_BUTTON_TEXT = new Color(255, 255, 255), //color of text in menu button
            MENU_BUTTON_BORDER = MENU_BUTTON.brighter(); //color of border of menu button
    public static final Color CAPTION_BUTTON_TEXT = new Color(128, 128, 128), //color of text in caption buttons (minimize and close)
            CAPTION_BUTTON_TEXT_SELECTED = new Color(255, 255, 255), //color of text in caption buttons when selected
            CAPTION_BUTTON_HOVER_COLOR = new Color(220, 220, 220); //color of caption buttons when hovered

    private static final int WIDTH = (int) (600f * SCALE), HEIGHT = (int) (280f * SCALE); //size of this window (there's all the code for resizing the window, but not the event listener, so if you want to write it yourself, you're welcome)

    //here follows a bunch of sizes used for the layout. I didn't use a layout manager because none of them support DPI scaling, see method fixupLayout() to see how I lay things around.
    public static final int GENERIC_MARGIN = (int) (6f * SCALE); //used for all margins
    public static final int MENU_BUTTON_WIDTH = (int) (82f * SCALE), MENU_BUTTON_HEIGHT = (int) (28f * SCALE); //size of the menu button
    public static final int CAPTION_BUTTON_WIDTH = (int) (28f * SCALE), CAPTION_BUTTON_HEIGHT = (int) (28f * SCALE); //size of caption buttons
    public static final int TITLE_BAR_HEIGHT = (int) (28f * SCALE); //size of title bar
    private static final int PLAYER_PANEL_HEIGHT = (int) (64f * Main.SCALE); //height of player area

    public static final Color TEXT = new Color(0, 0, 0), //color of most text
            TEXT_SELECTED = new Color(0, 0, 0), //color of text in selected (hovered) controls
            TEXT_ACCENT = new Color(255, 255, 255); //color of text in active controls ("being clicked")

    //used for swing stuff
    public static final ColorUIResource METAL_PRIMARY1 = new ColorUIResource(160, 160, 160);
    public static final ColorUIResource METAL_PRIMARY2 = new ColorUIResource(192, 192, 192);
    public static final ColorUIResource METAL_PRIMARY3 = new ColorUIResource(220, 220, 220);
    public static final ColorUIResource METAL_SECONDARY1 = new ColorUIResource(160, 160, 160);
    public static final ColorUIResource METAL_SECONDARY2 = new ColorUIResource(192, 192, 192);
    public static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(248, 248, 248); //background color for all windows

    public static final float BASE_FONT_PX = 12f * SCALE, //base font size in px (vertically)
            LARGE_FONT_PX = 14f * SCALE; //large font size in px (vertically)
    public static final Font BASE_FONT = Utils.loadFont("/com/dosse/bwentrain/player/fonts/OpenSans-reg.ttf").deriveFont(BASE_FONT_PX), //load base font
            LARGE_FONT = BASE_FONT.deriveFont(LARGE_FONT_PX); //and derive the large one

    public static File lastDir = null; //last folder visited with a file chooser

    //file filter for preset files, used when loading a preset
    public static final FileFilter PRESET_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".sin");
        }

        @Override
        public String getDescription() {
            return Utils.getLocString("MAIN_PRESET_FILE_DESCRIPTION");
        }
    };

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

    private final JPanel panel, //container
            dropReceiver; //receives drop events
    private final JLabel menuButton, //menu button (the blue button in the upper-left corner)
            minimize,
            quit;

    private final TitleBar titleBar;

    private final PlayerPanel playerPanel; //player area
    private final PresetDescriptionPanel desc; //description area (the gray area in the middle)
    private final MainMenu mainMenu; //the menu opened when clicking the menu button
    private final MainMenu.MenuItem export; //pointer to the export option in the menu, so it can be enabled/disabled when necessary

    public Main() {
        super();

        //initialize form
        setIconImage(Utils.loadUnscaled("/com/dosse/bwentrain/player/images/logoIcon.png").getImage());
        setLayout(null);
        setSize(WIDTH, HEIGHT);
        setUndecorated(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { //on alt+f4

            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new LineBorder(WINDOW_BORDER, 1));
        add(panel);
        playerPanel = new PlayerPanel();
        panel.add(playerPanel);
        desc = new PresetDescriptionPanel();
        panel.add(desc);
        titleBar = new TitleBar(this);
        titleBar.setText(Utils.getLocString("MAIN_TITLE"));
        setTitle(titleBar.getText());
        titleBar.setHorizontalAlignment(TitleBar.CENTER);
        titleBar.setForeground(TEXT);
        panel.add(titleBar);
        //now we'll add all the items in the main menu. each item closes the menu when clicked
        mainMenu = new MainMenu();
        mainMenu.addItem(new MainMenu.MenuItem(Utils.getLocString("MAIN_LOAD"), "/com/dosse/bwentrain/player/images/load.png") { //load preset

            @Override
            public void actionPerformed() {
                loadPreset();
                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
            }
        });
        export = new MainMenu.MenuItem(Utils.getLocString("MAIN_EXPORT"), "/com/dosse/bwentrain/player/images/export.png") { //export preset

            @Override
            public void actionPerformed() {
                export();
                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
                if (playerPanel.isPlaying()) {
                    playerPanel.pause();
                }
            }
        };
        export.setEnabled(false); //initially disabled because no preset is loaded, will be enabled later by loadPreset
        mainMenu.addItem(export);
        mainMenu.addItem(new MainMenu.MenuItem(Utils.getLocString("MAIN_DOWNLOAD"), "/com/dosse/bwentrain/player/images/download.png") { //link to preset download page

            @Override
            public void actionPerformed() {
                try {
                    Utils.openInBrowser(new URI(Utils.getLocString("PRESETS_URL")));
                } catch (Throwable t) {
                    MessageBox.showError(Utils.getLocString("BROWSER_ERROR") + " " + Utils.getLocString("PRESETS_URL"));
                }
                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
            }
        });
        mainMenu.addItem(new MainMenu.MenuItem(Utils.getLocString("MAIN_ABOUT") + " " + Utils.getLocString("MAIN_TITLE_SHORT"), "/com/dosse/bwentrain/player/images/about.png") {//about SINE

            @Override
            public void actionPerformed() {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        AboutDialog.about();
                    }
                });

                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
            }
        });
        mainMenu.addItem(new MainMenu.MenuItem(Utils.getLocString("MAIN_QUIT"), null) { //quit (with no icon)

            @Override
            public void actionPerformed() {
                quit();
                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
            }
        });

        panel.add(mainMenu);
        panel.setComponentZOrder(mainMenu, 0); //the main menu must be drawn above everything or else it will be invisible
        menuButton = new JLabel(Utils.getLocString("MAIN_MENU_BUTTON_TEXT"));
        menuButton.setOpaque(true);
        menuButton.setBackground(MENU_BUTTON);
        menuButton.setBorder(new LineBorder(MENU_BUTTON_BORDER, 1));
        menuButton.setForeground(MENU_BUTTON_TEXT);
        menuButton.setHorizontalAlignment(JLabel.CENTER);
        menuButton.addMouseListener(new MouseAdapter() { //change colors of menu button and open/close menu

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!mainMenu.isVisible()) { //open menu
                    mainMenu.setVisible(true);
                    mainMenu.requestFocus();
                } else {//close menu
                    mainMenu.setVisible(false);
                    menuButton.setBackground(MENU_BUTTON);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!mainMenu.isVisible()) {
                    menuButton.setBackground(MENU_BUTTON_HOVER);
                }

            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!mainMenu.isVisible()) {
                    menuButton.setBackground(MENU_BUTTON);
                }
            }
        });
        panel.add(menuButton);
        mainMenu.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                mainMenu.setVisible(false);
                menuButton.setBackground(MENU_BUTTON);
            }

        });
        minimize = new JLabel("-");
        minimize.setFont(LARGE_FONT);
        minimize.setHorizontalAlignment(JLabel.CENTER);
        minimize.setForeground(CAPTION_BUTTON_TEXT);
        minimize.setOpaque(true);
        minimize.addMouseListener(new MouseAdapter() { //change colors of minimize caption button and minimize window

            @Override
            public void mousePressed(MouseEvent e) {
                minimize.setBackground(CONTROL_ACCENT);
                minimize.setForeground(CAPTION_BUTTON_TEXT_SELECTED);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                minimize.setBackground(null);
                minimize.setForeground(CAPTION_BUTTON_TEXT);
                setState(JFrame.ICONIFIED);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                minimize.setBackground(CAPTION_BUTTON_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                minimize.setBackground(null);
            }
        });
        panel.add(minimize);
        quit = new JLabel("X");
        quit.setFont(LARGE_FONT);
        quit.setHorizontalAlignment(JLabel.CENTER);
        quit.setForeground(CAPTION_BUTTON_TEXT);
        quit.setOpaque(true);
        quit.addMouseListener(new MouseAdapter() { //change colors of close caption button and close application

            @Override
            public void mousePressed(MouseEvent e) {
                quit.setBackground(CONTROL_ACCENT);
                quit.setForeground(CAPTION_BUTTON_TEXT_SELECTED);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                quit.setBackground(null);
                quit.setForeground(CAPTION_BUTTON_TEXT);
                quit();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                quit.setBackground(CAPTION_BUTTON_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                quit.setBackground(null);
            }
        });
        panel.add(quit);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixUpLayout();
            }
        });
        //initialize drag n drop listener, if enabled
        if (DRAG_N_DROP_ENABLED) {
            DropTargetListener dnd = new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                }

                @Override
                public void dragOver(DropTargetDragEvent dtde) {
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent dtde) {
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                }

                @Override
                public void drop(DropTargetDropEvent dtde) {
                    try {
                        Transferable tr = dtde.getTransferable();
                        //only 1 file at a time is supported
                        if (tr.getTransferDataFlavors().length != 1) {
                            dtde.dropComplete(true);
                            return;
                        }
                        DataFlavor f = tr.getTransferDataFlavors()[0];
                        if (f.isFlavorJavaFileListType()) {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY);
                            final List files = (List) tr.getTransferData(f);
                            if (files.size() == 1) {
                                //load preset
                                File p = (File) files.get(0);
                                if (p.getName().toLowerCase().endsWith(".sin")) {
                                    loadPreset(p);
                                }
                            }
                        }
                        dtde.dropComplete(true);
                    } catch (Throwable t) {
                        dtde.dropComplete(false);
                    }
                }
            };
            //dropReceiver is basically a glasspane, except glasspanes can't receive drop events so we're gonna be a bit creative: it's just a transparent panel drawn in front of everything
            dropReceiver = new JPanel();
            dropReceiver.setOpaque(false);
            panel.add(dropReceiver);
            panel.setComponentZOrder(dropReceiver, 0);
            new DropTarget(dropReceiver, dnd);
        } else {
            dropReceiver = null;
        }
        //create listener for keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!isFocused()) {
                    return false;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getID() == KeyEvent.KEY_PRESSED) { //space = play/pause
                    if (playerPanel.getCurrentPreset() != null) {
                        if (playerPanel.isPlaying()) {
                            playerPanel.pause();
                        } else {
                            playerPanel.play();
                        }
                    }
                }
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L && e.getID() == KeyEvent.KEY_PRESSED) { //cltr+L = load preset
                    loadPreset();
                }
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_E && e.getID() == KeyEvent.KEY_PRESSED) { //cltr+E = export
                    export();
                }
                return false;
            }
        });

        if (IS_MACOS) { //listener for mac file associations

            try {
                new Apple().setOpenFileHandler(new Apple.OpenFilesHandler() {
                    @Override
                    public void openFiles(List<File> list) {
                        if (rejectAppleEvents) {
                            return;
                        }

                        try {
                            if (list.size() >= 1) {
                                loadPreset(list.get(0));

                            }
                        } catch (Throwable t) {
                            JOptionPane.showMessageDialog(null, "Error opening file");
                        }
                    }

                });
            } catch (Throwable t) {
            }
            //For some reason, opening a file while Sine is closed doesn't show the window
            //But if some other window is opened, then also the sine window opens.
            //So this will make a temp window to startup the real SINE window.
            JFrame tmp = new JFrame();
            tmp.setVisible(true);
            tmp.dispose();
        }

        //show the window centered
        setLocationRelativeTo(null);
        fixUpLayout();
    }

    private void loadPreset() {
        if (playerPanel.isPlaying() && MessageBox.showYesNoDialog(Utils.getLocString("MAIN_PRESET_LOAD_WHILE_PLAYING")) == MessageBox.SELECTION_NO) { //the user doesn't want to interrupt the playback of the current preset
            return;
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                File p = PresetFileChooser.open(getLocation().x + 20, getLocation().y + 50); //show file chooser
                if (p == null) { //no fle selected
                    return;
                }
                loadPreset(p); //load selected file
            }
        });
    }

    private void loadPreset(File p) {
        try {
            //read xml document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(p.getAbsolutePath()));
            //parse it
            Preset x = new Preset(doc.getDocumentElement());
            playerPanel.setPreset(x); //send it to player
            //update description
            desc.setTitle(x.getTitle());
            desc.setAuthor(x.getAuthor());
            desc.setDescription(x.getDescription());
            export.setEnabled(true); //enable export option (it was initially disabled)
            playerPanel.play(); //start playing the preset
        } catch (Throwable t) {
            //corrupt or not a preset file
            MessageBox.showError(Utils.getLocString("MAIN_PRESET_FILE_ERROR") + "\n\n" + p.toString() + " | " + t.toString());
        }
    }

    public void export() {
        rejectAppleEvents = true;
        final Preset p = playerPanel.getCurrentPreset();
        if (p == null) { //no preset loaded (can't happen if the export option is disabled before a preset is loaded)
            return;
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Object[] sel = ExportFileChooser.save(getLocation().x + 20, getLocation().y + 50); //ExportFileChooser returns 2 Objects: the first one is the selected File, the second one is the chosen extension (as FileFilter)
                if (sel[0] == null) { //no file selected
                    return;
                }
                File out = (File) sel[0];
                if (sel[1] == MP3_FILE_FILTER) { //chose MP3
                    if (!out.getName().toLowerCase().endsWith(".mp3")) {
                        out = new File(out.getAbsolutePath() + ".mp3");
                    }
                    if (out.exists() && MessageBox.showYesNoDialog(Utils.getLocString("MAIN_CONFIRM_OVERWRITE")) == MessageBox.SELECTION_NO) { //file already exists
                        //don't overwrite, shows the file chooser again
                        export();
                        return;
                    }
                    //export to selected file
                    ExportDialog.export(p, out, ExportDialog.FORMAT_MP3);
                }
                if (sel[1] == FLAC_FILE_FILTER) { //chose FLAC
                    if (!out.getName().toLowerCase().endsWith(".flac")) {
                        out = new File(out.getAbsolutePath() + ".flac");
                    }
                    if (out.exists() && MessageBox.showYesNoDialog(Utils.getLocString("MAIN_CONFIRM_OVERWRITE")) == MessageBox.SELECTION_NO) { //file already exists
                        //don't overwrite, shows the file chooser again
                        export();
                        return;
                    }
                    //export to selected file
                    ExportDialog.export(p, out, ExportDialog.FORMAT_FLAC);
                }
                if (sel[1] == WAV_FILE_FILTER) { //chose Wav
                    if (!out.getName().toLowerCase().endsWith(".wav")) {
                        out = new File(out.getAbsolutePath() + ".wav");
                    }
                    if (out.exists() && MessageBox.showYesNoDialog(Utils.getLocString("MAIN_CONFIRM_OVERWRITE")) == MessageBox.SELECTION_NO) { //file already exists
                        //don't overwrite, shows the file chooser again
                        export();
                        return;
                    }
                    //export to selected file
                    ExportDialog.export(p, out, ExportDialog.FORMAT_WAV);
                }
                rejectAppleEvents = false;
            }
        });
    }

    private void fixUpLayout() {
        panel.setBounds(0, 0, getWidth(), getHeight()); //stretch the container over the entire window
        int x = 1;
        menuButton.setBounds(1, 1, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT); //menu button in upper left corner at 1,1 (inside the border)
        x += MENU_BUTTON_WIDTH;
        int rx = getWidth() - 1 - CAPTION_BUTTON_WIDTH; //caption button position (from the right)
        quit.setBounds(rx, 1, CAPTION_BUTTON_WIDTH, CAPTION_BUTTON_HEIGHT); //close button 
        rx -= CAPTION_BUTTON_WIDTH;
        minimize.setBounds(rx, 1, CAPTION_BUTTON_WIDTH, CAPTION_BUTTON_HEIGHT);//minimize button at left of close button
        titleBar.setBounds(x, 1, rx - x, TITLE_BAR_HEIGHT); //title bar between menu button and caption buttons
        playerPanel.setBounds(1 + GENERIC_MARGIN, getHeight() - 1 - PLAYER_PANEL_HEIGHT, getWidth() - 2 - 2 * GENERIC_MARGIN, PLAYER_PANEL_HEIGHT); //player at the buttom
        desc.setBounds(1, 1 + MENU_BUTTON_HEIGHT + GENERIC_MARGIN, getWidth() - 2, playerPanel.getY() - 1 - MENU_BUTTON_HEIGHT - 2 * GENERIC_MARGIN); //description takes up all the remaining space
        mainMenu.setLocation(1, 1 + MENU_BUTTON_HEIGHT); //main menu (usually invisible) pops up under the menu button
        if (dropReceiver != null) {
            dropReceiver.setBounds(0, 0, getWidth(), getHeight());
        }
        repaint(); //must force repaint on some systems
    }

    private void quit() {
        System.exit(0);
    }

    public static void main(String args[]) {
        try {
            //check sound card
            PCSoundBackend test = new PCSoundBackend(44100, 1);
            test.open();
            test.write(new float[1024]);
            test.close();
        } catch (Exception ex) {
            //error, show error and close application
            JOptionPane.showMessageDialog(null, Utils.getLocString("MAIN_SOUNDCARD_ERROR"), Utils.getLocString("MAIN_TITLE"), JOptionPane.ERROR_MESSAGE);
            System.exit(3);
        }
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

            @Override
            public FontUIResource getControlTextFont() {
                return new FontUIResource(BASE_FONT);
            }

            @Override
            public FontUIResource getSystemTextFont() {
                return new FontUIResource(BASE_FONT);
            }

            @Override
            public FontUIResource getUserTextFont() {
                return new FontUIResource(BASE_FONT);
            }

            @Override
            public FontUIResource getMenuTextFont() {
                return new FontUIResource(BASE_FONT);
            }

            @Override
            public FontUIResource getWindowTitleFont() {
                return new FontUIResource(BASE_FONT);
            }

            @Override
            public FontUIResource getSubTextFont() {
                return new FontUIResource(BASE_FONT);
            }
        });
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Throwable t) {
        }
        //</editor-fold>

        //create and show form
        Main gui = new Main();
        gui.setVisible(true);

        if (!IS_MACOS) {
            if (args.length == 1) { //if a file was given via command line parameter, load it
                gui.loadPreset(new File(args[0]));
            }
        }

        if (!IS_MACOS) {
            /**
             * Thread used to move the mouse pointer 1px to prevent system
             * sleep. It moves the pointer two times to prevent not registering
             * the movement when the pointer is at the corner of the screen. Not
             * used on macOS because it requires Accessibility permissions.
             */
            new Thread() {
                @Override
                public void run() {
                    try {
                        Robot mouseRobot = new Robot();
                        while (true) {
                            Point pObj = MouseInfo.getPointerInfo().getLocation();
                            mouseRobot.mouseMove(pObj.x + 1, pObj.y + 1);
                            mouseRobot.mouseMove(pObj.x - 1, pObj.y - 1);
                            Thread.sleep(1000 * 40);   //every 40 seconds

                            //System.out.println("x: " + pObj.x + "  y: " + pObj.y);
                        }
                    } catch (Throwable ex) {
                    }
                }
            }.start();
        } else {
            try {
                /**
                 * Thread used to launch caffeinate command to prevent system
                 * sleep on macOS. -d prevents display sleep, -i prevents idle
                 * sleep, -u simulates user actions
                 */
                final Process process = Runtime.getRuntime().exec("caffeinate -di -u");
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        process.destroy();
                    }
                });
                process.waitFor();
            } catch (Throwable ex) {
            }
        }
    }
}
