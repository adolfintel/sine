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

/**
 * preset description (gray area in the middle)
 *
 * @author dosse
 */
public class PresetDescriptionPanel extends HTMLPanel {

    public static final Color PRESET_DESCRIPTION_BACKGROUND = new Color(235, 235, 235);
    private String title = "", description = "", author = "";

    public PresetDescriptionPanel() {
        super();
        setBackground(PRESET_DESCRIPTION_BACKGROUND);
        setForeground(Main.TEXT);
        setHTML(Utils.getLocString("PRESDESC_INIT_TEXT"));
    }

    //used to escape all html characters, because this is an HTMLPanel
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public void setTitle(String title) {
        this.title = escape(title);
        generateText();
    }

    public void setDescription(String description) {
        this.description = escape(description);
        generateText();
    }

    public void setAuthor(String author) {
        this.author = escape(author);
        generateText();
    }

    private void generateText() {
        setHTML("<table style='font-family:" + Main.BASE_FONT.getFamily() + "; font-size:" + (int) (Main.BASE_FONT_PX * 0.8) + "px'><tr><td valign='top'>" + Utils.getLocString("PRESDESC_TITLE_TEXT") + "</td><td>" + title + "</td></tr><tr><td valign='top'>" + Utils.getLocString("PRESDESC_AUTHOR_TEXT") + "</td><td>" + author + "</td></tr><tr><td valign='top'>" + Utils.getLocString("PRESDESC_DESCRIPTION_TEXT") + "</td><td>" + description + "</td></tr></table>");
    }
}
