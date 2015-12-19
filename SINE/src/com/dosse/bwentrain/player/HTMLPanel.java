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

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * shows HTML text inside a scrollable panel.
 *
 * @author dosse
 */
public class HTMLPanel extends JPanel {

    private final JScrollPane scroll;
    private final JTextPane text;

    public HTMLPanel() {
        super();
        setLayout(null);
        text = new JTextPane();
        text.setOpaque(true);
        text.setBorder(null);
        text.setContentType("text/html");
        text.setText("<html></html>");
        text.setEditable(false);
        text.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) { //a link was clicked, open in browser
                    try {
                        Utils.openInBrowser(e.getURL().toURI());
                    } catch (Throwable t) {
                    }
                }
            }
        });
        scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setViewportView(text);
        add(scroll);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixupLayout();
            }
        });
        fixupLayout();
    }

    private void fixupLayout() {
        scroll.setBounds(0, 0, getWidth(), getHeight());
        scroll.setViewportView(text);
        repaint(); //must force repaint on some systems
    }

    public void setHTML(String html) {
        text.setText("<html>" + html + "</html>");
        text.setCaretPosition(0);
    }

    @Override
    public void setBackground(Color c) {
        if (text != null) {
            text.setBackground(c);
        } else {
            super.setBackground(c);
        }
    }

    @Override
    public Color getBackground() {
        return text == null ? super.getBackground() : text.getBackground();
    }

    @Override
    public void setForeground(Color c) {
        if (text != null) {
            text.setForeground(c);
        } else {
            super.setForeground(c);
        }
    }

    @Override
    public Color getForeground() {
        return text == null ? super.getForeground() : text.getForeground();
    }

}
