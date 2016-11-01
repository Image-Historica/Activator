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
package com.imagehistorica.activator.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class LogHandler {

    private FileHandler fh = null;
    private static Logger logger = null;
    private static final LogHandler logHandler = new LogHandler();

    private LogHandler() {
        try {
            File dir = new File("log");
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new IOException();
                }
            }
            String pattern = "java.util.logging.FileHandler.pattern = log/activator.log";
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(pattern.getBytes("UTF-8")));
            fh = new FileHandler();
            fh.setFormatter(new SimpleFormatter());
            logger = Logger.getLogger("LogHandler");
            logger.addHandler(fh);
            logger.info("Open log file...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static LogHandler getInstance() {
        return logHandler;
    }

    public void reopen() {
        if (fh == null) {
            try {
                fh = new FileHandler("log/Activator.log", true);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
                logger.info("Reopen log file...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void info(String s) {
        logger.info(s);
    }

    public synchronized void log(Level l, String s, Exception e) {
        logger.log(l, s, e);
    }

    public synchronized void close() {
        if (fh != null) {
            logger.info("Close log file...");
            fh.flush();
            fh.close();
            fh = null;
        }
    }
}
