/*
 * Copyright (C) 2016 Image-Historica.com
 *
 * This file is part of the ImageHistorica: https://image-historica.com
 * ImageHistorica is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.imagehistorica.activator.updater;

import java.awt.Color;
import java.awt.Font;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class Messenger extends JLayeredPane {

    private final JLabel bgImage;
    private final JLabel header = new JLabel();
    private final JTextArea message = new JTextArea();

    public Messenger(ImageIcon image, int width, int height, String headerTxt) {
        bgImage = new JLabel("", image, SwingConstants.CENTER);
//        bgImage.setLayout(null);
        bgImage.setSize(width, height);
        add(bgImage);

        header.setText(headerTxt);
        header.setFont(new Font("SansSerif", Font.PLAIN, 22));
        header.setForeground(Color.WHITE);
        header.setBackground(Color.BLACK);
        header.setLocation(70, 290);
        header.setSize(400, 30);
        bgImage.add(header);

        message.setFont(new Font("SansSerif", Font.PLAIN, 20));
        message.setForeground(Color.WHITE);
        message.setBackground(Color.BLACK);
        message.setLocation(80, 320);
        message.setSize(650, 110);
        message.setLineWrap(true);
        message.setEditable(false);
        bgImage.add(message);
    }

    protected void update(String header, String message) {
        this.header.setText(header);
        this.message.setText(message);
    }
}
