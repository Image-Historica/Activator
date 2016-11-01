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

import com.imagehistorica.activator.util.Downloader;
import com.imagehistorica.activator.util.HashChecker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class Updater {

    private final SplashUpdater su = SplashUpdater.getInstance();
    private final String latestUrl;
    private final String downloadUrl;
    private final Downloader downloader = new Downloader();
    private final File downloadCmp = new File("lib_tmp/downloadCmp");
    private final File md5hash = new File("lib_tmp/md5hash.txt");
    private final File libTmp = new File("lib_tmp");
    private final File lib = new File("lib");
    private final HashChecker hc;
    private Map<String, String> libsHash;
    private List<File> emptyDirs;

    public Updater(String latestUrl, String downloadUrl) throws Exception {
        this.latestUrl = latestUrl;
        this.downloadUrl = downloadUrl;
        this.hc = new HashChecker();
        if (!lib.exists() || !lib.isDirectory()) {
            su.writeLog("lib directory does not exist...");
            if (!lib.mkdir()) {
                su.updateSplash("libディレクトリを作成できませんでした。", "lib directory does not exist...\nCould not make lib directory...", true);
            }
        }
    }

    public String update(Map<String, String> map) throws FileNotFoundException, UnsupportedEncodingException, IOException, NoSuchAlgorithmException, Exception {
        su.updateSplash("アップデートを開始します...", "Start update...", false);
        String libraries = "";
        libsHash = downloader.doGetHash(latestUrl);

        // latestLibraries's format e.g), ImageHistorica-1.2.0.jar
        Set<String> latestLibraries = getLatestLibs(map);
        libraries = latestLibraries.stream().map((library) -> library + ",").reduce(libraries, String::concat);

        if (libTmp.exists() && md5hash.exists() && downloadCmp.exists()) {
            su.updateSplash("前回のセッションでライブラリのダウンロードが完了しています...",
                    "Completed download previous session...", false);

            // requiredLib's format e.g), ImageHistorica-1.2.0.jar=4fc683acc0a1ad4cac06a73b849b7d33
            Set<String> requiredLibs = getRequiredLibs(latestLibraries);
            updateFromMiddle(latestLibraries, requiredLibs);

        } else {
            su.updateSplash("前回のセッションでライブラリのダウンロードが完了していません。",
                    "Not completed download previous session.\nUpdate from the beginning...", false);
            if (!libTmp.exists()) {
                libTmp.mkdir();
            }

            updateFromBeginning();

            // Delete directories under lib directory including itself.
            emptyDirs = new ArrayList<>();
            emptyDirs.add(lib);
            su.updateSplash("'lib' ディレクトリとその配下のファイルを削除します...", "Remove 'lib' directory and its files...", false);
            delFiles(lib);
            if (!emptyDirs.isEmpty()) {
                delEmptyDirs(emptyDirs);
            }
            su.updateSplash("'lib_tmp' ディレクトリを 'lib' ディレクトリにリネームします...", "Rename 'lib_tmp' to 'lib' directory...", false);
            Files.move(libTmp.toPath(), lib.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }

        if (libraries.contains(",")) {
            libraries = libraries.substring(0, libraries.lastIndexOf(","));
        }
        return libraries;
    }

    private Set<String> getLatestLibs(Map<String, String> map) throws Exception {
        su.updateSplash("最新のライブラリ一覧を取得します...", "Get latest libraries info...", false);
        List<String> appOptionsLib = new ArrayList<>();
        for (String latestLib : libsHash.keySet()) {
            if (latestLib.startsWith("ImageHistoricaVer_")) {
                if (latestLib.equals("ImageHistoricaVer_" + map.get("appOptions"))) {
                    map.put("newVersion", libsHash.get(latestLib));
                    su.updateSplash("ライブラリ: " + latestLib + ", バージョン: " + map.get("newVersion"),
                            "Library: " + latestLib + ", Version: " + map.get("newVersion"), false);
                }
                appOptionsLib.add(latestLib);
                su.writeLog("Added to appOptionsLib: " + latestLib);
            }
        }
        appOptionsLib.forEach(e -> libsHash.remove(e));
        return new TreeSet<>(libsHash.keySet());
    }

    private Set<String> getRequiredLibs(Set<String> latestLibraries) throws Exception {
        su.updateSplash("必要なライブラリを確認します...", "Check required libraries...", false);

        Set<String> requiredLibs = new HashSet<>();
        File[] libs = lib.listFiles();
        for (String latestLib : latestLibraries) {
            boolean found = false;
            for (File lib : libs) {
                if (lib.getName().equals(latestLib)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                su.updateSplash("以下のライブラリが必要です。\n" + latestLib, "Required following library.\n" + latestLib, false);
                requiredLibs.add(latestLib + "=" + libsHash.get(latestLib));
            }
        }
        if (requiredLibs.isEmpty()) {
            su.updateSplash("すべてのライブラリが存在します...", "All libraries exist already...", false);
        }
        return requiredLibs;
    }

    private void delFiles(File dir) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                emptyDirs.add(file);
                delFiles(file);
            } else if (file.isFile()) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException ex) {
                    su.updateSplash("以下のファイルを削除できません。\n" + file.getAbsolutePath(), "Could not delete following the file.\n" + file.getAbsolutePath(), true);
                }
            }
        }
    }

    private void delEmptyDirs(List<File> emptyDirs) throws Exception {
        Collections.sort(emptyDirs, (File o1, File o2) -> {
            int f1 = o1.toPath().getNameCount();
            int f2 = o2.toPath().getNameCount();
            if (f1 < f2) {
                return 1;
            } else if (f1 == f2) {
                return 0;
            } else {
                return -1;
            }
        });

        for (File file : emptyDirs) {
            try {
                Files.delete(file.toPath());
            } catch (IOException ex) {
                su.updateSplash("以下のファイルを削除できません。\n" + file.getAbsolutePath(), "Could not delete following the file.\n" + file.getAbsolutePath(), true);
            }
        }
    }

    private void checkIntegrity(String downloadFile, String saveFile, String fileHash) throws Exception {
        File jar = downloader.doGetFile(downloadFile, saveFile);
        if (!jar.exists()) {
            su.updateSplash("ファイルを取得できませんでした...\n" + jar.getName(), "Could not get the file...\n" + jar.getName(), true);
        }
        String digest = hc.createDigest(jar);
        su.writeLog("Latest digest : " + fileHash);
        su.writeLog("Created digest: " + digest);
        if (!digest.equals(fileHash)) {
            su.updateSplash("ハッシュ値が一致しません...\nファイル: " + jar + "\nMD5: " + digest, "Mismatch digest...\nFile: " + jar + "\nMD5: " + digest, true);
        }
    }

    private void updateFromBeginning() throws Exception {
        su.updateSplash("最初からアップデートを行います...", "Try to update from the beginning...", false);
        // Clear lib_tmp directory.
        emptyDirs = new ArrayList<>();
        // Delete directories of under lib_tmp directory.
        delFiles(libTmp);
        if (!emptyDirs.isEmpty()) {
            delEmptyDirs(emptyDirs);
        }

        su.updateSplash("ダウンロードURL:\n" + downloadUrl, "DownloadUrl: " + downloadUrl, false);
        for (Entry<String, String> e : libsHash.entrySet()) {
            if (!e.getKey().startsWith("imageHistoricaVer_")) {
                String fileName = e.getKey();
                su.updateSplash("ファイル: " + fileName + " を取得します...", "Getting " + fileName + " ...", false);
                checkIntegrity(downloadUrl + fileName, libTmp.getName() + "/" + fileName, e.getValue());
            }
        }
        su.updateSplash("ダウンロードファイルの完全性検査(インテグリティチェック)を完了しました...", "Completed to verify the integrity of downloaded files...", false);
    }

    private void updateFromMiddle(Set<String> latestLibraries, Set<String> requiredLibs) throws Exception {
        su.updateSplash("残りのアップデート作業を行います...", "Try to update from the middle...", false);

        Set<String> existingLibs = new HashSet<>();
        try (FileInputStream fis = new FileInputStream(md5hash);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                for (String requiredLib : requiredLibs) {
                    if (requiredLib.equals(line)) {
                        su.updateSplash("ダウンローズ済みのライブラリ:\n" + requiredLib, "Added to existingLib: " + requiredLib, false);
                        existingLibs.add(requiredLib);
                    }
                }
            }
        }

        if (!existingLibs.isEmpty()) {
            requiredLibs.removeAll(existingLibs);
        }

        // "requiredLibs is not empty" means to require newly download files.
        if (!requiredLibs.isEmpty()) {
            su.updateSplash("必要なライブラリのダウンロードが前回のセッションで完了していません...", "Not complete to download required libraries previous session...", false);
            su.updateSplash("ダウンロードURL:\n" + downloadUrl, "DownloadUrl: " + downloadUrl, false);
            for (String requiredLib : requiredLibs) {
                su.writeLog("requiredLib: " + requiredLib);
                String[] values = requiredLib.split("=");
                String fileName = values[0];
                String fileHash = values[1];
                su.updateSplash("ファイル: " + fileName + " を取得します...", "Getting " + fileName + " ...", false);
                checkIntegrity(downloadUrl + fileName, libTmp.getName() + "/" + fileName, fileHash);
            }
            su.updateSplash("ダウンロードファイルのインテグリティチェックを完了しました...", "Completed to verify the integrity of downloaded files...", false);
        }

        // Check required libs exist either in lib or lib_tmp directory.
        su.updateSplash("'lib' と 'lib_tmp' ディレクトリを統合します...", "Integrate 'lib' and 'lib_tmp' directories...", false);
        File[] libFiles = lib.listFiles();
        File[] libTmpFiles = libTmp.listFiles();
        boolean isFailed = false;
        for (String latestLib : latestLibraries) {
            boolean found = false;
            for (File libFile : libFiles) {
                if (libFile.getName().equals(latestLib)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (File libTmpFile : libTmpFiles) {
                    if (libTmpFile.getName().equals(latestLib)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                isFailed = true;
            }
        }

        // "isFailed flag is true" means that could not get required libs due to any reason.
        if (isFailed) {
            su.updateSplash("必要なライブラリが何らかの理由で存在しません。",
                    "Could not find required libraries due to any reason.", false);
            updateFromBeginning();
        }

        for (File tmpFile : libTmp.listFiles()) {
            if (tmpFile.getName().equals("md5hash.txt") || tmpFile.getName().equals("downloadCmp")) {
                Files.delete(tmpFile.toPath());
                continue;
            }
            if (tmpFile.isDirectory() || !libsHash.containsKey(tmpFile.getName())) {
                su.updateSplash("'lib_tmp' ディレクトリに以下の不明なファイルが存在します。\n" + tmpFile.getAbsolutePath() + "\n移動、もしくは削除してください。",
                        "An irrelevant file with Image-Historica exists." + tmpFile.getAbsolutePath() + "\nPlease (re)move it.", true);
            }
        }

        // Move or replace one with a new or updated lib.
        for (File tmpFile : libTmp.listFiles()) {
            String artifactId = tmpFile.getName().substring(0, tmpFile.getName().lastIndexOf("-"));
            for (File jar : lib.listFiles()) {
                if (jar.getName().startsWith(artifactId)) {
                    Files.delete(jar.toPath());
                    break;
                }
            }
            Files.move(tmpFile.toPath(), Paths.get(lib.getAbsolutePath() + "/" + tmpFile.getName()), StandardCopyOption.ATOMIC_MOVE);
        }

        // Delete lib_tmp directory because it is only required for update process.
        Files.delete(libTmp.toPath());
    }
}
