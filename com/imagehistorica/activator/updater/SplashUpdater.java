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

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class SplashUpdater {

    private final SplashMaker sm;
    private boolean isJP = true;
    private static final LogHandler logger = LogHandler.getInstance();

    private static final SplashUpdater su = new SplashUpdater();

    public SplashUpdater() {
        String lang = System.getProperty("user.language");
        if (lang != null && !lang.equals("ja")) {
            isJP = false;
        }
        if (isJP) {
            sm = new SplashMaker("イメージヒストリカを起動しています...");
        } else {
            sm = new SplashMaker("Activating Image-Historica...");
        }
    }

    public static SplashUpdater getInstance() {
        return su;
    }

    public void showSplash() {
        sm.showSplash();
    }
    
    public void setHeader(String ja, String en) {
        if (isJP) {
            sm.setHeader(ja);
        } else {
            sm.setHeader(en);
        }
    }

    public void updateSplash(String ja, String en, boolean isException) throws Exception {
        if (isException) {
            if (isJP) {
                sm.showEx(ja);
                while (sm.isShowingEx()) {
                    Thread.sleep(1000);
                }
                throw new Exception(ja);
            } else {
                sm.showEx(en);
                while (sm.isShowingEx()) {
                    Thread.sleep(1000);
                }
                throw new Exception(en);
            }
        } else {
            if (isJP) {
                sm.showProgress(ja);
//                logger.fine(ja);
                logger.info(ja);
            } else {
                sm.showProgress(en);
//                logger.fine(en);
                logger.info(en);
            }
        }
    }

    public void writeLog(String log) {
        logger.info(log);
    }

    public void setFinished(boolean isFinished) {
        sm.setFinished(isFinished);
    }
}
