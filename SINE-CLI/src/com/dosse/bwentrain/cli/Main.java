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
package com.dosse.bwentrain.cli;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.ISoundDevice;
import com.dosse.bwentrain.sound.backends.flac.FLACFileSoundBackend;
import com.dosse.bwentrain.sound.backends.mp3.MP3FileSoundBackend;
import com.dosse.bwentrain.sound.backends.pc.PCSoundBackend;
import com.dosse.bwentrain.sound.backends.wav.WavFileSoundBackend;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author dosse
 */
public class Main {

    //converts time to HH:MM:SS String

    private static String toHMS(float t) {
        int h = (int) (t / 3600);
        t %= 3600;
        int m = (int) (t / 60);
        t %= 60;
        int s = (int) t;
        return "" + (h < 10 ? ("0" + h) : h) + ":" + (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s); //bloody hell, code salad
    }
    
    private static Preset loadPreset(String path) {
        File p = new File(path);
        if (!p.exists()) {
            System.out.println("File not found: " + path);
            System.exit(1);
        }
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
            //preset is valid
            //print preset info
            System.out.println("Title:\t" + x.getTitle() + "\nAuthor:\t" + x.getAuthor() + "\nDescription:\t" + x.getDescription() + "\nLength:\t" + toHMS(x.getLength()) + (x.loops() ? ", loops after " + toHMS(x.getLoop()) : "") + "\n");
            return x;
        } catch (Throwable t) {
            //corrupt or not a preset file
            System.out.println("Preset not valid: " + path);
            System.exit(2);
        }
        return null;
    }
    
    private static void playPreset(String path) {
        Preset x = loadPreset(path);
        //play the Preset
        try {
            IRenderer r = new IsochronicRenderer(x, new PCSoundBackend(44100, 1), -1);
            r.play();
            while (r.isPlaying()) {
                Thread.sleep(100);
                System.out.println(toHMS(r.getPosition()) + "/" + toHMS(r.getLength()));
            }
            r.stopPlaying();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Device error");
            System.exit(4);
        }
    }
    
    private static void exportPreset(String in, String out, int loop) {
        Preset x = loadPreset(in);
        //export the Preset
        ISoundDevice s = null;
        try {
            if (out.toLowerCase().endsWith(".mp3")) {
                s = new MP3FileSoundBackend(out, 44100, 1, 96);
            }
            if (out.toLowerCase().endsWith(".wav")) {
                s = new WavFileSoundBackend(out, 44100, 1);
            }
            if (out.toLowerCase().endsWith(".flac")) {
                s = new FLACFileSoundBackend(out, 44100, 1);
            }
        } catch (Exception ex) {
            System.out.println("Can't create file " + out);
            System.exit(3);
        }
        if (s == null) {
            showHelp();
            System.exit(-1);
        }
        try {
            IRenderer r = new IsochronicRenderer(x, s, loop);
            r.play();
            while (r.isPlaying()) {
                Thread.sleep(1000);
                System.out.println((r.getPosition() / r.getLength()) * 100 + "%");
            }
            r.stopPlaying();
            System.out.println("100%\nExport complete");
            r.stopPlaying();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Device error");
            System.exit(4);
        }
    }
    
    private static void checkPreset(String path) {
        loadPreset(path);
        System.out.println("Preset valid");
        System.exit(0);
    }
    
    private static void showHelp() {
        System.out.println("SINE Isochronic Entrainer - Command Line Interface\nVersion 1.8.6\n\n"
                + "Syntax:\n"
                + "SINE-CLI presetFile [--validate|--export fileName [loopCount]]\n\n"
                + "Description:\n"
                + "-Play a Preset:  SINE-CLI presetFile\n"
                + "-Validate a Preset: SINE-CLI presetFile --validate\n"
                + "-Export a Preset: SINE-CLI presetFile --export fileName [loopCount]  fileName must end in .mp3, .wav or .flac;  LoopCount (optional) is useful when exporting looping Presets: it's the number of times the loop should be repeated (-1=repeat infinitely, 0=no repeat (default), 1=repeat once, ...)\n\n"
                + "Error codes:\n"
                + "-1\tsyntax error\n"
                + "0\tno error\n"
                + "1\tfile not found\n"
                + "2\tpreset not valid\n"
                + "3\tcan't create file\n"
                + "4\tdevice error\n");
    }
    
    public static void main(String args[]) {
        if (args.length == 0) {
            showHelp();
            System.exit(-1);
        }
        if (args.length == 1) {
            playPreset(args[0]);
        }
        if (args.length == 2) {
            if (args[1].equals("--validate")) {
                checkPreset(args[0]);
            } else {
                showHelp();
                System.exit(-1);
            }
        }
        if (args.length == 3 || args.length == 4) {
            if (args[1].equals("--export")) {
                try {
                    exportPreset(args[0], args[2], args.length == 4 ? Integer.parseInt(args[3]) : 0);
                } catch (Exception e) {
                    showHelp();
                    System.exit(-1);
                }
            } else {
                showHelp();
                System.exit(-1);
            }
        }
    }
}
