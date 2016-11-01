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
import com.imagehistorica.activator.updater.Updater;
import com.imagehistorica.activator.util.LogHandler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class Activator {

    private static final SplashUpdater su = SplashUpdater.getInstance();
    private static final LogHandler logger = LogHandler.getInstance();
    private static final PreProcessor pre = new PreProcessor();
    private static final Map<String, String> map = new HashMap<>();
    private static final String activateUrl = "https://api.image-historica.com/activate/";

    private static String libraries = null;
    private static URL[] urls;

    public static void main(String[] args) {
        try {
            try {
                su.showSplash();
                su.updateSplash("アクティベータを起動しました...", "Start Activator...", false);
                if (pre.isInitialize(args)) {
                    su.setHeader("初期化中です...", "Initializing now...");
                    su.updateSplash("初期化しています...", "Initializing...", false);
                    map.putAll(pre.getKey());
                    map.putAll(pre.getUpdateUrls(activateUrl, map.get("ACCESS_KEY")));
                    if (pre.fetchActivator(map.get("downloadUrl"))) {
                        map.put("isInitializing", "true");
                    }
                } else {
                    map.put("isInitializing", "false");
                }

                map.putAll(pre.getActivator());
                libraries = map.get("libraries");
                if (libraries == null) {
                    su.updateSplash("'activator.properties' を読み込めませんでした。バックアップを取得してから、以下のコマンドで初期化してください。\n'java -jar Activator.jar initialize'",
                            "Could not parse 'activator.properties'. Please initialize by following command after taking backup.\n'java -jar Activator.jar initialize'", true);
                }

                if (Boolean.valueOf(map.get("isUpdateRequested")) || Boolean.valueOf(map.get("isForcedUpdateRequested"))) {
                    su.setHeader("アップデートしています...", "Updating now...");
                    su.updateSplash("アップデートリクエストフラグが有効なことを確認しました...", "Confirmed update request flag...", false);
                    String latestLibs = getLibraries();
                    if (latestLibs.isEmpty()) {
                        su.updateSplash("リクエストに応じてアップデートを実行しましたが、ライブラリを取得できませんでした...", "Try to update according to the request, but libraries is empty...", true);
                    }
                    logger.info("latest libraries: " + latestLibs);
                    if (!makeUrls(latestLibs)) {
                        if (Boolean.valueOf(map.get("isForcedUpdateRequested"))) {
                            su.updateSplash("ライブラリのURLを生成できませんでした。", "Could not make urls and try to update.", false);
                            if (pre.fetchActivator(map.get("downloadUrl"))) {
                                map.putAll(pre.getActivator());
                                libraries = map.get("libraries");
                                if (!makeUrls(libraries)) {
                                    su.updateSplash("'activator.properties' を更新してアップデートを実行しましたが起動に失敗しました。大変申し訳ございませんが、image-historica.com にお問い合わせ頂けますようお願いいたします。",
                                            "Failed to activate after renewal of 'activator.properties'. I'm very sorry, but please contact image-historica.com and get support.", true);
                                }
                            }
                        } else {
                            su.updateSplash("ライブラリのURLを生成できませんでした。", "Could not make urls and try to update.", true);
                        }
                    } else {
                        map.put("libraries", latestLibs);
                    }
                } else {
                    su.setHeader("通常起動中です...", "Activating now...");
                    if (!makeUrls(libraries)) {
                        su.updateSplash("ライブラリのURLを生成できませんでしたので、アップデートします...", "Could not make urls and try to update...", false);
                        String latestLibs = getLibraries();
                        if (latestLibs.isEmpty()) {
                            su.updateSplash("アップデートを実行しましたが、ライブラリを取得できませんでした...", "Try to update, but libraries is empty...", true);
                        } else {
                            logger.info("libraries: " + latestLibs);
                            if (!makeUrls(latestLibs)) {
                                su.updateSplash("起動可能なアプリケーションがありません...", "No applications...", true);
                            } else {
                                map.put("libraries", latestLibs);
                            }
                        }
                    }
                }

                map.forEach((k, v) -> logger.info("Parameter: " + k + ", Value: " + v));

                su.updateSplash("イメージヒストリカを起動します...", "Activate Image-Historica...", false);
                su.setFinished(true);
                activate();

            } catch (Exception e) {
                su.setFinished(false);
                logger.reopen();
                su.updateSplash(e.getMessage(), e.getMessage(), false);
                logger.log(Level.WARNING, "", e);
                map.put("isUpdateRequested", "false");

                if ((e.getMessage()) != null) {
                    if (e.getMessage().contains("activator.properties") || e.getMessage().contains("key.properties")) {
                        su.setFinished(true);
                        logger.close();
                        System.exit(1);
                    }
                }

                if (libraries == null) {
                    map.putAll(pre.getActivator());
                    map.put("isInitializing", "false");
                    libraries = map.get("libraries");
                }

                throw new Exception("Could not update...");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "", e);
            try {
                if (makeUrls(libraries)) {
                    su.updateSplash("イメージヒストリカの起動に失敗しました。\n起動可能な構成で実行します...",
                            "Failed to activate Image-Historica...\nTry to activate it by available libraries...", false);
                    su.setFinished(true);
                    map.put("libraries", libraries);
                    activate();
                } else {
                    su.updateSplash("イメージヒストリカの起動に失敗しました。",
                            "Failed to activate Image-Historica...", true);
                }
            } catch (Exception exc) {
                logger.reopen();
                logger.log(Level.SEVERE, "Could not activate Image-Historica...", exc);
            }
        } finally {
            su.setFinished(true);
            logger.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void activate() throws Exception {
        URLClassLoader loader = new URLClassLoader(urls);
        String isUpdateRequested = map.get("isUpdateRequested");
        String isForcedUpdateRequested = map.get("isForcedUpdateRequested");
        String oldVersion = map.get("oldVersion");
        String newVersion = map.get("newVersion");
        String application = map.get("application");
        String libraries = map.get("libraries");
        if (isUpdateRequested == null || (!isUpdateRequested.matches("true|false"))) {
            su.setFinished(false);
            su.updateSplash("'activator.properties' の 'isUpdateRequested' の値が不正です。", "Invalid parameter 'isUpdateRequested' of 'activator.properties'.", true);
        }
        if (isForcedUpdateRequested == null || (!isForcedUpdateRequested.matches("true|false"))) {
            su.setFinished(false);
            su.updateSplash("'activator.properties' の 'isForcedUpdateRequested' の値が不正です。", "Invalid parameter 'isForcedUpdateRequested' of 'activator.properties'.", true);
        }
        if (oldVersion == null || newVersion == null) {
            su.setFinished(false);
            su.updateSplash("'activator.properties' の 'version' の値が不正です。", "Invalid parameter 'version' of 'activator.properties'.", true);
        }
        if (application == null) {
            su.setFinished(false);
            su.updateSplash("'activator.properties' の 'application' の値が不正です。", "Invalid parameter 'application' of 'activator.properties'.", true);
        }
        if (libraries == null) {
            su.setFinished(false);
            su.updateSplash("'activator.properties' の 'libraries' の値が不正です。", "Invalid parameter 'libraries' of 'activator.properties'.", true);
        }

        String[] args = {"--application=" + application, "--oldVersion=" + oldVersion, "--newVersion=" + newVersion, "--libraries=" + libraries,
            "--isUpdateRequested=" + isUpdateRequested, "--isForcedUpdateRequested=" + isForcedUpdateRequested, "--isInitializing=" + map.get("isInitializing")};
        Class clazz = Class.forName("com.imagehistorica.ImageHistorica", true, loader);
        Method method = clazz.getMethod("main", new Class[]{args.getClass()});
        Thread.currentThread().setContextClassLoader(loader);
        method.invoke(null, new Object[]{args});
    }

    private static boolean makeUrls(String libraries) throws Exception {
        if (libraries == null || libraries.isEmpty()) {
            logger.info("No library data...");
            return false;
        }
        String[] libs = libraries.split(",");
        urls = new URL[libs.length];
        for (int i = 0; i < libs.length; i++) {
            File file = new File("lib/" + libs[i]);
            if (!file.exists()) {
                su.updateSplash("'lib' ディレクトリに以下のファイルがありません。\n" + file.getName(), "File not found under 'lib' directory.\n" + file.getName(), false);
            }
            urls[i] = file.toURI().toURL();
        }

        if (urls.length != libs.length) {
            logger.info("Could not make urls...");
        }
        return true;
    }

    private static String getLibraries() throws Exception {
        if (!map.containsKey("latestUrl") || !map.containsKey("downloadUrl")) {
            map.putAll(pre.getKey());
            map.putAll(pre.getUpdateUrls(activateUrl, map.get("ACCESS_KEY")));
        }
        Updater up = new Updater(map.get("latestUrl"), map.get("downloadUrl"));
        return up.update(map);
    }
}
