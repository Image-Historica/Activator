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

import com.imagehistorica.activator.updater.SplashUpdater;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class Downloader {

    private final SplashUpdater su = SplashUpdater.getInstance();

    public String[] doGetUrls(String activateUrls) throws Exception {
        su.updateSplash("ライブラリのダウンロードURLを取得します...", "Getting download urls...", false);

        String[] urls = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(activateUrls);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "close");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                        BufferedReader br = new BufferedReader(isr)) {
                    urls = br.readLine().split("\\^");
                }
            }
        } catch (IOException e) {
            su.updateSplash("ダウンロードURLを取得できませんでした。\n申し訳ございませんが、しばらくした後に再度ご利用頂けますようお願いいたします。",
                    "Could not get update urls.. I'm sorry, but please retry after a while because central server could not respond...", true);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return urls;
    }

    public File doGetFile(String downloadUrl, String file) throws Exception {
        File jar = new File(file);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(downloadUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "close");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                        FileOutputStream fos = new FileOutputStream(jar, false);
                        BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                    byte[] b = new byte[4096];
                    int read = 0;
                    while ((read = is.read(b)) != -1) {
                        bos.write(b, 0, read);
                    }
                    bos.flush();
                }
            }
        } catch (IOException e) {
            su.updateSplash("ファイル: " + file + " を取得できませんでした。\n申し訳ございませんが、しばらくした後に再度ご利用頂けますようお願いいたします。",
                    "Could not get a file...'" + file + "' ...\nI'm sorry, but please retry after a while because central server could not respond...", true);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return jar;
    }

    public Map<String, String> doGetHash(String downloadUrl) throws Exception {
        System.out.println("download: " + downloadUrl);
        su.updateSplash("ライブラリのハッシュデータを取得します...", "Getting hash info of each library...", false);

        Map<String, String> latestLibs = new HashMap<>();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(downloadUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "close");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                        BufferedReader br = new BufferedReader(isr)) {

                    String libs = br.readLine();
                    String[] fileAndHashes;
                    if (libs != null) {
                        su.updateSplash("libs: " + libs, "Getting hash info of each library...", false);
                        fileAndHashes = libs.split(",");
                    } else {
                        throw new IOException();
                    }

                    for (String fileAndHash : fileAndHashes) {
                        su.updateSplash("fileAndHash: " + fileAndHash, "Getting hash info of each library...", false);
                        String[] values = fileAndHash.split("=");
                        latestLibs.put(values[0], values[1]);
                    }
                }
            }
        } catch (IOException e) {
            su.updateSplash("ライブラリのハッシュデータを取得できませんでした。\n申し訳ございませんが、しばらくした後に再度ご利用頂けますようお願いいたします。",
                    "Could not get lib's hash... \nI'm sorry, but please retry after a while because central server could not respond...", true);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return latestLibs;
    }
}
