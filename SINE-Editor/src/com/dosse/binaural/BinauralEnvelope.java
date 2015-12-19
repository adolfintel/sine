/*
 * Copyright (C) 2013 Federico Dossena
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this class represents a whole "song". each point in the envelope has its
 * time, binaural frequency (+interpolation factor), binaural beats volume
 * (+interpolation factor) and pink noise volume (+interpolation factor). you
 * may also set a base frequency for the binaural beats (default is 220Hz). you
 * can set as many points as you wish
 *
 * @author dosse
 */
public class BinauralEnvelope implements Serializable {

    public static final long serialVersionUID = -7240722749720590430L;

    /**
     * this class is used to represent an envelope for a single variable. you
     * can set as many points as you wish, and also specify how they should be
     * interpolated
     *
     * @author dosse
     */
    protected class Envelope implements Serializable {

        public class EnvelopeEntry implements Comparable<EnvelopeEntry>, Serializable {

            /**
             * t is the time (in seconds)
             */
            public double t;
            /**
             * val is the value
             */
            public double val;
            /**
             * f is the interpolation factor (0.5=square root, 1=linear,
             * 2=square, ...)
             */
            public double f;

            /**
             * @param t time
             * @param val value
             * @param f interpolation factor (0.5=square root, 1=linear,
             * 2=square, ...)
             */
            public EnvelopeEntry(double t, double val, double f) {
                this.t = t;
                this.val = val;
                this.f = f;
            }

            @Override
            public int compareTo(EnvelopeEntry o) {
                double diff = t - o.t;
                return diff == 0 ? 0 : diff > 0 ? 1 : -1;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof EnvelopeEntry)) {
                    return false;
                }
                EnvelopeEntry e = (EnvelopeEntry) o;
                return e.t == t && e.f == f && e.val == val;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 23 * hash + (int) (Double.doubleToLongBits(this.t) ^ (Double.doubleToLongBits(this.t) >>> 32));
                hash = 23 * hash + (int) (Double.doubleToLongBits(this.val) ^ (Double.doubleToLongBits(this.val) >>> 32));
                hash = 23 * hash + (int) (Double.doubleToLongBits(this.f) ^ (Double.doubleToLongBits(this.f) >>> 32));
                return hash;
            }

            @Override
            public String toString() {
                return "EnvelopeEntry: t=" + t + ", val=" + val + ", f=" + f;
            }
        }
        private TreeSet<EnvelopeEntry> points;

        /**
         * use this constructor to duplicate an envelope
         *
         * @param e pre-existing envelope
         */
        public Envelope(Envelope e) {
            points = new TreeSet<>(e.points);
        }

        /**
         * creates a new envelope without points
         */
        public Envelope() {
            points = new TreeSet<>();
        }

        private TreeSet<EnvelopeEntry> getPoints() {
            return points;
        }

        private EnvelopeEntry floor(double t) {
            if (points.isEmpty()) {
                return null;
            }
            if (points.first().t > t) {
                return new EnvelopeEntry(t, 0, 1);
            }
            return points.floor(new EnvelopeEntry(t, 0, 1));
        }

        private EnvelopeEntry ceiling(double t) {
            if (points.isEmpty()) {
                return null;
            }
            if (points.last().t <= t) {
                return points.last();
            }
            return points.ceiling(new EnvelopeEntry(t, 0, 1));
        }

        private double getFAt(double t) {
            return floor(t).f;
        }

        /**
         *
         * @param t time
         * @return value in envelope at specified t. returns 0 if envelope is
         * empty.
         */
        public double getValueAt(double t) {
            if (points.isEmpty()) {
                return 0;
            }
            if (points.size() == 1) {
                return points.first().val;
            }
            EnvelopeEntry a = floor(t);
            EnvelopeEntry b = ceiling(t);
            double pow = getFAt(t);
            double f = (t - a.t) / (b.t - a.t);
            return lerpWithPow(a.val, b.val, f, pow);
        }

        /**
         * adds a point in the envelope at specified t, with specified value and
         * interpolation factor. if another point with the same t already
         * exists, it is replaced by the new point
         *
         * @param t time
         * @param val value
         * @param f interpolation factor (note: when interpolating from point A
         * to point B, the interpolation factor is the one specified in point A)
         */
        public void setPoint(double t, double val, double f) {
            Object[] pts = points.toArray();
            for (int i = 0; i < pts.length; i++) {
                if (((EnvelopeEntry) pts[i]).t == t) {
                    points.remove(pts[i]);
                }
            }
            points.add(new EnvelopeEntry(t, val, f));
        }

        /**
         * removes all points
         */
        public void clearPoints() {
            points.clear();
        }

        /**
         *
         * @return the number of points in the envelope
         */
        public int getPointCount() {
            return points.size();
        }

        /**
         *
         * @return the length (in whatever time unit you used for the time) the
         * envelope lasts
         */
        public double getLength() {
            if (points.isEmpty()) {
                return 0;
            } else {
                return points.last().t - points.first().t;
            }
        }

        /**
         *
         * @return start time
         */
        public double getStartT() {
            if (points.isEmpty()) {
                return 0;
            } else {
                return points.first().t;
            }
        }

        /**
         *
         * @return end time
         */
        public double getEndT() {
            if (points.isEmpty()) {
                return 0;
            } else {
                return points.last().t;
            }
        }
    }

    /**
     * utility. interpolates between 2 values a and b
     *
     * @param a value a
     * @param b value b
     * @param f double 0-1. represents how close we are to point b. (0=a, 1=b)
     * @param pow also known as interpolation factor (0.5=square root, 1=linear,
     * 2=square, ...)
     * @return interpolated value
     */
    private static double lerpWithPow(double a, double b, double f, double pow) {
        double fn = Math.pow(f > 1 ? 1 : f < 0 ? 0 : f, pow);
        return a * (1 - fn) + b * fn;
    }
    private Envelope binauralF, binauralV, noiseV;
    private double baseF;
    /**
     * use this to "attach" an object, such as a string with a comment
     */
    public Object attachment;

    /**
     * creates a new binaural envelope with no points. by default the base
     * frequency is 220Hz, but it can be changed with setBaseF
     */
    public BinauralEnvelope() {
        binauralF = new Envelope();
        binauralV = new Envelope();
        noiseV = new Envelope();
        baseF = 220;
    }

    /**
     *
     * @return length in seconds (or whatever time unit you're using) of the
     * envelope
     */
    public double getLength() {
        return binauralF.getLength();
    }

    /**
     *
     * @return start time
     */
    public double getStartT() {
        return binauralF.getStartT();
    }

    /**
     *
     * @return end time
     */
    public double getEndT() {
        return binauralF.getEndT();
    }

    /**
     *
     * @return Envelope containing all points for binaural frequency. note: this
     * is a copy of the actual array so changing the content is pointless
     */
    public Envelope getBinauralF() {
        return new Envelope(binauralF);
    }

    /**
     *
     * @return Envelope containing all points for volume of binaural beats.
     * note: this is a copy of the actual array so changing the content is
     * pointless
     */
    public Envelope getBinauralV() {
        return new Envelope(binauralV);
    }

    /**
     *
     * @return Envelope containing all points for pink noise volume. note: this
     * is a copy of the actual array so changing the content is pointless
     */
    public Envelope getNoiseV() {
        return new Envelope(noiseV);
    }

    /**
     *
     * @return base frequency (220Hz by default)
     */
    public double getBaseF() {
        return baseF;
    }

    /**
     *
     * @param baseF new base frequency (220Hz by default)
     */
    public void setBaseF(double baseF) {
        this.baseF = baseF;
    }

    /**
     * adds a point in the envelope at specified t with specified values. if a
     * point already exists at the specified time it is replaced by the newest
     * one.
     *
     * @param t time
     * @param binF binaural frequecy
     * @param binFF interpolation factor of binaural frequency
     * @param binV volume of binaural beats
     * @param binVF interpolation factor of volume of binaural beats
     * @param nV pink noise volume
     * @param nVF interpolation factor of pink noise volume
     *
     * ideal volume values go from 0 to 1. ideal binaural frequencies go from 0
     * to 30. interpolation factors represent how the 2 values should be
     * interpolated (0.5=square root, 1=linear, 2=square, ...)
     */
    public void setPoint(double t, double binF, double binFF, double binV, double binVF, double nV, double nVF) {
        binauralF.setPoint(t, binF, binFF);
        binauralV.setPoint(t, binV, binVF);
        noiseV.setPoint(t, nV, nVF);
    }

    @Override
    public String toString() {
        Object[] ef = binauralF.getPoints().toArray();
        Object[] ev = binauralV.getPoints().toArray();
        Object[] en = noiseV.getPoints().toArray();
        String s = "" + baseF + "\n";
        for (int i = 0; i < ef.length; i++) {
            Envelope.EnvelopeEntry efe = (Envelope.EnvelopeEntry) ef[i];
            Envelope.EnvelopeEntry eve = (Envelope.EnvelopeEntry) ev[i];
            Envelope.EnvelopeEntry ene = (Envelope.EnvelopeEntry) en[i];
            s += efe.t + "," + efe.val + "," + efe.f + "," + eve.val + "," + eve.f + "," + ene.val + "," + ene.f + "\n";
        }
        return s.replace(".0,", ",").replace(".0\n", "\n");
    }

    public String toXML() {
        String x = "<BinauralEnvelope baseFrequency=\"" + baseF + "\">\n";
        Object[] ef = binauralF.getPoints().toArray();
        Object[] ev = binauralV.getPoints().toArray();
        Object[] en = noiseV.getPoints().toArray();
        for (int i = 0; i < ef.length; i++) {
            Envelope.EnvelopeEntry efe = (Envelope.EnvelopeEntry) ef[i];
            Envelope.EnvelopeEntry eve = (Envelope.EnvelopeEntry) ev[i];
            Envelope.EnvelopeEntry ene = (Envelope.EnvelopeEntry) en[i];
            x += "\t<Point t=\"" + efe.t + "\" binauralFrequency=\"" + efe.val + "\" binauralFrequencyInterpolationF=\"" + efe.f + "\" binauralVolume=\"" + eve.val + "\" binauralVolumeInterpolationF=\"" + eve.f + "\" noiseVolume=\"" + ene.val + "\" noiseVolumeInterpolationF=\"" + ene.f + "\"/>\n";
        }
        return x + "</BinauralEnvelope>";
    }

    public static BinauralEnvelope fromXML(String x) throws Exception {
        try {
            Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(x.getBytes("UTF-8")));
            BinauralEnvelope toReturn=new BinauralEnvelope();
            Node env = xml.getElementsByTagName("BinauralEnvelope").item(0);
            toReturn.baseF=Double.parseDouble(env.getAttributes().getNamedItem("baseFrequency").getNodeValue());
            NodeList points=env.getChildNodes();
            for(int i=0;i<points.getLength();i++){
                Node n=points.item(i);
                if(n.getNodeName().equalsIgnoreCase("Point")){ //ignore other tags
                    NamedNodeMap attributes=n.getAttributes();
                    Node t=attributes.getNamedItem("t");
                    Node bf=attributes.getNamedItem("binauralFrequency");
                    Node bff=attributes.getNamedItem("binauralFrequencyInterpolationF");
                    Node bfv=attributes.getNamedItem("binauralVolume");
                    Node bfvf=attributes.getNamedItem("binauralVolumeInterpolationF");
                    Node nv=attributes.getNamedItem("noiseVolume");
                    Node nvf=attributes.getNamedItem("noiseVolumeInterpolationF");
                    toReturn.setPoint(Double.parseDouble(t.getNodeValue()), Double.parseDouble(bf.getNodeValue()), Double.parseDouble(bff.getNodeValue()), Double.parseDouble(bfv.getNodeValue()), Double.parseDouble(bfvf.getNodeValue()), Double.parseDouble(nv.getNodeValue()), Double.parseDouble(nvf.getNodeValue()));
                }
            }
            return toReturn;
        } catch (Throwable t) {
            throw new Exception("Invalid XML");
        }
        
    }

    public static BinauralEnvelope fromString(String s) throws Exception {
        try {
            BinauralEnvelope be = new BinauralEnvelope();
            StringTokenizer st = new StringTokenizer(s, "\n");
            ArrayList<String> ss = new ArrayList<>();
            while (st.hasMoreTokens()) {
                ss.add(st.nextToken());
            }
            be.setBaseF(Double.parseDouble(ss.get(0)));
            ss.remove(0);
            for (String x : ss) {
                x = x.trim();
                if (x.isEmpty()) {
                    continue;
                }
                StringTokenizer xt = new StringTokenizer(x, ",\n");
                try {
                    be.setPoint(Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()), Double.parseDouble(xt.nextToken()));
                    if (xt.hasMoreTokens()) {
                        throw new Exception("");
                    }
                } catch (Exception e) {
                    throw new Exception("For input string: " + x);
                }
            }
            return be;
        } catch (Exception e) {
            throw e;
        }
    }
    /**
     * saves the envelope in a kinda efficient format (much smaller than
     * serializated instance). useful if you're gonna save a preset to something
     * that has a very small memory, like an nfc tag.
     *
     * @return the result of toString() represented with 4 bits per character.
     * 0-9=0-9 A=. B=, C=\n D=- E-F=null
     */
    public byte[] toHES() {
        String s = toString();
        byte[] hef = new byte[(s.length() + 1) / 2];
        for (int i = 0; i < s.length(); i += 2) {
            char c1 = s.charAt(i), c2 = (i + 1) < s.length() ? s.charAt(i + 1) : 0;
            int nibble1 = 0xE, nibble2 = 0xE;
            //<editor-fold defaultstate="collapsed" desc="cheap-ass conversion (bunch of if)">
            if (c1 == '0') {
                nibble1 = 0x0;
            }
            if (c1 == '1') {
                nibble1 = 0x1;
            }
            if (c1 == '2') {
                nibble1 = 0x2;
            }
            if (c1 == '3') {
                nibble1 = 0x3;
            }
            if (c1 == '4') {
                nibble1 = 0x4;
            }
            if (c1 == '5') {
                nibble1 = 0x5;
            }
            if (c1 == '6') {
                nibble1 = 0x6;
            }
            if (c1 == '7') {
                nibble1 = 0x7;
            }
            if (c1 == '8') {
                nibble1 = 0x8;
            }
            if (c1 == '9') {
                nibble1 = 0x9;
            }
            if (c1 == '.') {
                nibble1 = 0xA;
            }
            if (c1 == ',') {
                nibble1 = 0xB;
            }
            if (c1 == '\n') {
                nibble1 = 0xC;
            }
            if (c1 == '-') {
                nibble1 = 0xD;
            }
            if (c2 == '0') {
                nibble2 = 0x0;
            }
            if (c2 == '1') {
                nibble2 = 0x1;
            }
            if (c2 == '2') {
                nibble2 = 0x2;
            }
            if (c2 == '3') {
                nibble2 = 0x3;
            }
            if (c2 == '4') {
                nibble2 = 0x4;
            }
            if (c2 == '5') {
                nibble2 = 0x5;
            }
            if (c2 == '6') {
                nibble2 = 0x6;
            }
            if (c2 == '7') {
                nibble2 = 0x7;
            }
            if (c2 == '8') {
                nibble2 = 0x8;
            }
            if (c2 == '9') {
                nibble2 = 0x9;
            }
            if (c2 == '.') {
                nibble2 = 0xA;
            }
            if (c2 == ',') {
                nibble2 = 0xB;
            }
            if (c2 == '\n') {
                nibble2 = 0xC;
            }
            if (c2 == '-') {
                nibble2 = 0xD;
            }
            //</editor-fold>
            hef[i / 2] = (byte) ((nibble1 << 4) | nibble2);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            gos.write(hef);
            gos.flush();
            gos.close();
            baos.flush();
            baos.close();
        } catch (Exception exception) {
        }
        return baos.toByteArray();
    }

    /**
     * converts the output of toHES() back into a BinauralEnvelope
     *
     * @return the converted BinauralEnvelope
     */
    public static BinauralEnvelope fromHES(byte[] hes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(hes);
        GZIPInputStream gis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int in = gis.read();
            if (in == -1) {
                break;
            } else {
                baos.write(in);
            }
        }
        baos.flush();
        baos.close();
        byte[] hef = baos.toByteArray();
        gis.read(hef);
        gis.close();
        char[] s = new char[hef.length * 2];
        for (int i = 0; i < hef.length; i++) {
            int nibble1 = (hef[i] & 0xF0) >> 4, nibble2 = hef[i] & 0xF;
            //<editor-fold defaultstate="collapsed" desc="cheap-ass conversion (bunch of if)">
            if (nibble1 == 0x0) {
                s[2 * i] = '0';
            }
            if (nibble1 == 0x1) {
                s[2 * i] = '1';
            }
            if (nibble1 == 0x2) {
                s[2 * i] = '2';
            }
            if (nibble1 == 0x3) {
                s[2 * i] = '3';
            }
            if (nibble1 == 0x4) {
                s[2 * i] = '4';
            }
            if (nibble1 == 0x5) {
                s[2 * i] = '5';
            }
            if (nibble1 == 0x6) {
                s[2 * i] = '6';
            }
            if (nibble1 == 0x7) {
                s[2 * i] = '7';
            }
            if (nibble1 == 0x8) {
                s[2 * i] = '8';
            }
            if (nibble1 == 0x9) {
                s[2 * i] = '9';
            }
            if (nibble1 == 0xA) {
                s[2 * i] = '.';
            }
            if (nibble1 == 0xB) {
                s[2 * i] = ',';
            }
            if (nibble1 == 0xC) {
                s[2 * i] = '\n';
            }
            if (nibble1 == 0xD) {
                s[2 * i] = '-';
            }
            if (nibble2 == 0x0) {
                s[2 * i + 1] = '0';
            }
            if (nibble2 == 0x1) {
                s[2 * i + 1] = '1';
            }
            if (nibble2 == 0x2) {
                s[2 * i + 1] = '2';
            }
            if (nibble2 == 0x3) {
                s[2 * i + 1] = '3';
            }
            if (nibble2 == 0x4) {
                s[2 * i + 1] = '4';
            }
            if (nibble2 == 0x5) {
                s[2 * i + 1] = '5';
            }
            if (nibble2 == 0x6) {
                s[2 * i + 1] = '6';
            }
            if (nibble2 == 0x7) {
                s[2 * i + 1] = '7';
            }
            if (nibble2 == 0x8) {
                s[2 * i + 1] = '8';
            }
            if (nibble2 == 0x9) {
                s[2 * i + 1] = '9';
            }
            if (nibble2 == 0xA) {
                s[2 * i + 1] = '.';
            }
            if (nibble2 == 0xB) {
                s[2 * i + 1] = ',';
            }
            if (nibble2 == 0xC) {
                s[2 * i + 1] = '\n';
            }
            if (nibble2 == 0xD) {
                s[2 * i + 1] = '-';
            }
            //</editor-fold>
        }
        return BinauralEnvelope.fromString(String.valueOf(s));
    }
}
