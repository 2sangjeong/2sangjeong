package com.lge.asr.extractor.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class LogCryptor {

    public static byte[] encode(byte[] data) {
        //
        // trap door
        //
        if (data == null) {
            return null;
        }
        //
        // declaration
        //
        byte[] result = null;
        Cipher cipher = null;
        String key = "|9E |o95y5+Em*Ey";
        String initialVector = "1234567890123456";
        SecretKeySpec keyspec = null;
        IvParameterSpec ivspec = null;
        //
        // encode
        //
        try {
            keyspec = new SecretKeySpec(key.getBytes(), "AES");
            ivspec = new IvParameterSpec(initialVector.getBytes());
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            result = Base64.encodeBase64(cipher.doFinal(data));
        } catch (Exception e) {
            //e.printStackTrace();
            result = retryEncode(data);
        }
        //
        // done
        //
        return result;
    }

    public static byte[] decrypt(String data) {
        byte[] result = null;
        try {
            //
            // trap door
            //
            if (data == null || data.length() == 0) {
                return null;
            }
            //
            // declaration
            //
            byte[] decoded = null;
            final String key = "|9E |o95y5+Em*Ey";
            final String initialVector = "1234567890123456";
            Cipher cipher = null;
            SecretKeySpec sks = null;
            IvParameterSpec ips = null;
            //
            // decode & decrypt
            //

            decoded = Base64.decodeBase64(data);
            sks = new SecretKeySpec(key.getBytes(), "AES");
            ips = new IvParameterSpec(initialVector.getBytes());
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sks, ips);
            result = cipher.doFinal(decoded);
        } catch (Exception e) {
            //e.printStackTrace();
            result = retryDecrypt(data);
        }
        //
        // done
        //
        return result;
    }

    public static String messageToCryp(String message) throws Exception {

        if (message == null || message.trim().equals("")) {
            return "";
        }

        MessageDigest md;
        byte[] mb = null;

        try {
            md = MessageDigest.getInstance("SHA-512");

            md.update(message.getBytes());
            mb = md.digest();

        } catch (NoSuchAlgorithmException nsae) {
            throw nsae;
        } catch (Exception e) {
            throw e;
        }

        String out = "";

        try {
            for (int i = 0; i < mb.length; i++) {
                byte temp = mb[i];
                String s = Integer.toHexString(new Byte(temp));
                while (s.length() < 2) {
                    s = "0" + s;
                }
                s = s.substring(s.length() - 2);
                out += s;
            }
        } catch (Exception e) {
            throw e;
        }

        return out;
    }


    private static final String newKey = "F5E8FFAAE76CCA4772B809971622494AD8E1819961B01F2061EA6C5365077450"; //"|9E |o95y5+Em*Ey";
    private static final String newInitialVector = "01020304050607080900010203040506";

    private static byte[] retryEncode(byte[] data) {
        if (data == null) {
            return null;
        }
        System.out.println("retryEncode with newKey.");
        byte[] result;
        Cipher cipher;
        SecretKeySpec keyspec;
        IvParameterSpec ivspec;
        try {
            keyspec = new SecretKeySpec(hexStringToByteArray(newKey), "AES");
            ivspec = new IvParameterSpec(hexStringToByteArray(newInitialVector));
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            result = Base64.encodeBase64(cipher.doFinal(data));
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    private static byte[] retryDecrypt(String data) {
        byte[] result;
        try {
            if (data == null || data.length() == 0) {
                return null;
            }
            System.out.println("retryDecrypt with newKey.");
            byte[] decoded;
            Cipher cipher;
            SecretKeySpec sks;
            IvParameterSpec ips;

            decoded = Base64.decodeBase64(data);
            sks = new SecretKeySpec(hexStringToByteArray(newKey), "AES");
            ips = new IvParameterSpec(hexStringToByteArray(newInitialVector));
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sks, ips);
            result = cipher.doFinal(decoded);
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
