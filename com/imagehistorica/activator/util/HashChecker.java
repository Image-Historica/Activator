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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Kazuhito Kojima, kojima@image-historica.com
 */
public class HashChecker {

    private final MessageDigest md;

    public HashChecker() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
    }
    
    public String createDigest(File file) throws NoSuchAlgorithmException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            md.reset();
            byte[] buff = new byte[4096];
            int len = 0;
            while ((len = fis.read(buff, 0, buff.length)) >= 0) {
                md.update(buff, 0, len);
            }
        }

        return convertString(md.digest());
    }

    private String convertString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String tmp = Integer.toHexString(digest[i] & 0xff);
            if (tmp.length() == 1) {
                sb.append('0').append(tmp);
            } else {
                sb.append(tmp);
            }
        }
        return sb.toString();
    }
}
