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
package com.imagehistorica.activator;

import com.imagehistorica.activator.updater.SplashUpdater;
import com.imagehistorica.activator.util.Downloader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class PreProcessor {

    private final SplashUpdater su = SplashUpdater.getInstance();
    private final String keyProp = "conf/key.properties";
    private final String activatorProp = "conf/activator.properties";
    private final Downloader downloader = new Downloader();

    public boolean isInitialize(String[] args) throws Exception {
        boolean isInitializing = false;
        if (args != null && args.length > 0) {
            if (args.length == 1) {
                if (args[0].equals("initialize")) {
                    isInitializing = true;
                } else {
                    su.updateSplash("Activator はひとつの引数しか付与できません。初期化されたい場合には以下を実行してください。\n'java -jar Activator.jar initialize'",
                            "Activator can take only one argument if you want to initialize...\n'java -jar Activator.jar initialize'", true);
                }
            } else {
                su.updateSplash("Activator はひとつの引数しか付与できません。初期化されたい場合には以下を実行してください。\n'java -jar Activator.jar initialize'",
                        "Activator can take only one argument if you want to initialize...\n'java -jar Activator.jar initialize'", true);
            }
        }
        return isInitializing;
    }

    public Map<String, String> getKey() throws Exception {
        Map<String, String> map = new HashMap<>();
        Properties prop = new Properties();
        File file = new File(keyProp);
        if (!file.exists()) {
            file = new File(keyProp + "_tmp");
            if (!file.exists()) {
                su.updateSplash("confフォルダに 'key.properties' が存在しません。\n'key.properties' はあなたの認証データになります。バックアップからリストアするか、すでにご購入済みであれば、お問い合わせください。",
                        "No file of 'key.properties' in conf directory...\n'key.properties' is your credential. Please restore it from backup or contact me if you've purchased already.", true);
            }
        }
        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr)) {
            prop.load(br);
            String accessKey = prop.getProperty("ACCESS_KEY");
            if (accessKey != null) {
                map.put("ACCESS_KEY", accessKey);
            } else {
                su.updateSplash("アクセスキーを取得できませんでした。", "Could not get access key...", true);
            }
        }
        return map;
    }

    public Map<String, String> getUpdateUrls(String activateUrl, String accessKey) throws Exception {
        Map<String, String> map = new HashMap<>();
        String[] urls = downloader.doGetUrls(activateUrl + accessKey);
        if (urls == null) {
            su.updateSplash("適切なダウンロードURLを取得できませんでした。\n申し訳ございませんが、しばらくした後に再度ご利用頂けますようお願いいたします。",
                    "Could not get proper urls from image-historica.com...\nI'm sorry, but please retry after a while due to central server may be down...", true);
        }
        map.put("latestUrl", urls[0]);
        map.put("downloadUrl", urls[1]);
        return map;
    }

    public boolean fetchActivator(String downloadUrl) throws Exception {
        su.updateSplash("ダウンロードURL:\n" + downloadUrl, "DownloadUrl: " + downloadUrl, false);
        su.updateSplash("ファイル: " + activatorProp + " を取得します...", "Getting " + activatorProp + " ...", false);
        downloader.doGetFile(downloadUrl + "activator.properties", activatorProp);
        File file = new File(activatorProp);
        if (!file.exists()) {
            su.updateSplash("'activator.properties' を取得できませんでした。\n申し訳ございませんが、しばらくした後にまたのご利用をお願いします。",
                    "Could not get 'activator.properties'...\nI'm sorry, but please retry after a while...", true);
        }
        return true;
    }

    public Map<String, String> getActivator() throws Exception {
        Map<String, String> map = new HashMap<>();
        Properties prop = new Properties();
        File file = new File(activatorProp);
        if (!file.exists()) {
            file = new File(activatorProp + "_tmp");
            if (!file.exists()) {
                su.updateSplash("confフォルダに 'activator.properties' が存在しません。\n適切なファイルを用意するか、バックアップを取得してから以下のコマンドで初期化してください。\n'java -jar Activator.jar initialize'",
                        "No file of 'activator.properties' in conf directory...\nPlease restore a proper 'activator.properties' or take backup and initialize by issueing command, \n'java -jar Activator.jar initialize'", true);
            }
        }
        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr)) {
            prop.load(br);
            String[] applications_tmp = prop.getProperty("applications").split(",");
            int appOptions = 0;
            if (applications_tmp.length > 1) {
                try {
                    appOptions = Integer.parseInt(prop.getProperty("appOptions"));
                } catch (NumberFormatException e) {
                    su.updateSplash("appOptions の値が不正です。\nappOptions: " + appOptions, "appOptions is invalid.\nappOptions: " + appOptions, true);
                }
            }

            map.put("appOptions", String.valueOf(appOptions));
            map.put("application", applications_tmp[appOptions]);
            map.put("oldVersion", prop.getProperty("version_" + appOptions, "0.0.0"));
            map.put("newVersion", prop.getProperty("version_" + appOptions, "0.0.0"));
            map.put("libraries", prop.getProperty("libs_" + appOptions));
            map.put("isUpdateRequested", prop.getProperty("isUpdateRequested_" + appOptions));
            map.put("isUpdateRequested", prop.getProperty("isUpdateRequested_" + appOptions));
            map.put("isForcedUpdateRequested", prop.getProperty("isForcedUpdateRequested_" + appOptions));
        }
        return map;
    }
}
