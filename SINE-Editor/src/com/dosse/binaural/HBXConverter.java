/*
 * Copyright (C) 2013-2014 Federico Dossena
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
package com.dosse.binaural;

import com.dosse.bwentrain.core.EntrainmentTrack;
import com.dosse.bwentrain.core.Envelope;
import com.dosse.bwentrain.core.Preset;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author dosse
 */
public class HBXConverter {

    /**
     * headers (magic numbers actually) of HBX and HBS files are calculated from
     * 2 simple strings (HBX and HBS respectively)
     *
     */
    public static final byte[] HBX_HEADER, HBS_HEADER;

    static {
        String h = "HBX";
        HBX_HEADER = new byte[h.length()];
        char[] arr = h.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            HBX_HEADER[i] = (byte) arr[i];
        }
        h = "HBS";
        HBS_HEADER = new byte[h.length()];
        arr = h.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            HBS_HEADER[i] = (byte) arr[i];
        }
    }

    /**
     * loads an HBX preset from a file
     *
     * @param x input file
     * @return the BinauralEnvelope stored in the file
     * @throws FileNotFoundException if an IO error occurs
     * @throws IOException if an IO error occurs
     * @throws Exception if an IO error occurs
     */
    private static BinauralEnvelope loadPreset(File x) throws FileNotFoundException, IOException, Exception {
        if (x == null) {
            throw new Exception("Invalid file");
        }
        if (x.getName().toLowerCase().endsWith(".hbl")) {
            BufferedReader xmlFile = new BufferedReader(new FileReader(x));
            String xml = "";
            for (;;) {
                try {
                    String line = xmlFile.readLine();
                    if (line == null) {
                        break;
                    } else {
                        xml += line + "\n";
                    }
                } catch (IOException e) {
                    break;
                }
            }
            xmlFile.close();
            return BinauralEnvelope.fromXML(xml);
        } else {
            FileInputStream fis = new FileInputStream(x);
            boolean hbx = true, hbs = true;
            byte[] headerX = new byte[HBX_HEADER.length];
            byte[] headerS = new byte[HBS_HEADER.length];
            fis.read(headerX);
            fis.close();
            fis = new FileInputStream(x);
            fis.read(headerS);
            for (int i = 0; i < headerX.length; i++) {
                if (headerX[i] != HBX_HEADER[i]) {
                    hbx = false;
                }
            }
            for (int i = 0; i < headerS.length; i++) {
                if (headerS[i] != HBS_HEADER[i]) {
                    hbs = false;
                }
            }
            fis.close();
            fis = new FileInputStream(x);
            if (!hbx && !hbs) {
                throw new Exception("Invalid file");
            }
            if (hbx) {
                fis.read(headerX);
                ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(fis));
                BinauralEnvelope be = (BinauralEnvelope) (ois.readObject());
                ois.close();
                return be;
            }
            if (hbs) {
                fis.read(headerS);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (;;) {
                    int in = fis.read();
                    if (in == -1) {
                        break;
                    } else {
                        baos.write(in);
                    }
                }
                BinauralEnvelope be = BinauralEnvelope.fromHES(baos.toByteArray());
                fis.close();
                return be;
            }
        }
        throw new Exception("Invalid file");
    }
    /**
     * converts an HBX Preset into a SINE Preset. The main limitation is that HBX Interpolation Factors are not supported, and will be treated as 1, always.
     * @param f file containing HBX Preset (hbx, hbs and hbl are supported)
     * @return SINE Preset
     * @throws Exception if anything goes wrong
     */
    public static Preset convert(File f) throws Exception {
        BinauralEnvelope hbx = loadPreset(f);
        Preset p = new Preset((float) hbx.getLength(), -1, f.getName(), "", "Imported from HBX Binaural Player");
        EntrainmentTrack tone = p.getEntrainmentTrack(0);
        Envelope noise = p.getNoiseEnvelope(), volume = tone.getVolumeEnvelope(), ent = tone.getEntrainmentFrequencyEnvelope();
        //the easiest way to convert the HBX preset is to start from the output of the toXML method of the HBX preset
        Element hXML = getXMLDocumentFromString(hbx.toXML());
        tone.getBaseFrequencyEnvelope().setVal(0, Float.parseFloat(hXML.getAttribute("baseFrequency")));
        NodeList points = hXML.getChildNodes();
        for (int i = 0, pi = 0; i < points.getLength(); i++) {
            NamedNodeMap point = points.item(i).getAttributes();
            if (point == null) {
                continue;
            }
            float t = (float) (Float.parseFloat(point.getNamedItem("t").getNodeValue()) - hbx.getStartT()),
                    binauralFrequency = Float.parseFloat(point.getNamedItem("binauralFrequency").getNodeValue()),
                    binauralVolume = Float.parseFloat(point.getNamedItem("binauralVolume").getNodeValue()),
                    noiseVolume = Float.parseFloat(point.getNamedItem("noiseVolume").getNodeValue());
            if (pi++ == 0) {
                noise.setVal(0, noiseVolume);
                volume.setVal(0, binauralVolume);
                ent.setVal(0, binauralFrequency);
            } else {
                noise.addPoint(t, noiseVolume);
                volume.addPoint(t, binauralVolume);
                ent.addPoint(t, binauralFrequency);
            }
        }
        return p;
    }

    public static Element getXMLDocumentFromString(String xml) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(new InputSource(new StringReader(xml)));
        return xmlDoc.getDocumentElement();
    }
}
