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

import com.imagehistorica.activator.util.LogHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JWindow;
import javax.swing.SwingWorker;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class SplashMaker {

    private final JWindow window = new JWindow();
    private final ImageIcon image = new ImageIcon(getClass().getResource("Splash.png"));
    private final Messenger messenger;

    private String header = null;
    private boolean showingEx = false;
    private boolean isFinished = false;
    private long finishedTime;
    private final ConcurrentLinkedDeque<UpdatedString> updateStrings = new ConcurrentLinkedDeque<>();

    private final LogHandler logger = LogHandler.getInstance();

    public SplashMaker(String header) {
        int width = image.getIconWidth();
        int height = image.getIconHeight();
        messenger = new Messenger(image, width, height, header);
        window.getContentPane().add(messenger);
        window.setBounds(0, 0, width, height);
//        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public void showSplash() {
        Path activateCmp = Paths.get("activateCmp");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.info("Start bg process for splash...");
                while (true) {
                    while (!isFinished) {
                        while (updateStrings.peekFirst() != null) {
                            UpdatedString us = updateStrings.pollFirst();
                            if (showingEx) {
                                if (us.isEx()) {
                                    messenger.update(header, us.getUpdateString());
                                    Thread.sleep(5000);
                                    showingEx = false;
                                }
                            } else {
                                messenger.update(header, us.getUpdateString());
                            }
                        }
                        if (showingEx) {
                            showingEx = false;
                        }
                        Thread.sleep(100);
                    }

                    if (Files.exists(activateCmp)) {
                        try {
                            Files.delete(activateCmp);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Could not delete activateCmp...", e);
                        }
                        break;
                    }

                    long now = (new Date()).getTime();
                    if (now - finishedTime > 10000) {
                        break;
                    }
                    Thread.sleep(100);
                }
                return null;
            }

            @Override
            protected void done() {
                logger.info("End bg process for splash...");
                logger.close();
                window.setVisible(false);
                window.dispose();
            }
        };
        worker.execute();
    }

    public void showProgress(String progress) {
        updateStrings.offerLast(new UpdatedString(false, progress));
    }

    public void showEx(String ex) {
        updateStrings.offerLast(new UpdatedString(true, ex));
        showingEx = true;
    }

    public boolean isShowingEx() {
        return this.showingEx;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
        finishedTime = (new Date()).getTime();
    }
}
