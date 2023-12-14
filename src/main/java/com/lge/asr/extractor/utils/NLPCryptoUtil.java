package com.lge.asr.extractor.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.lge.asr.common.constants.CommonConsts;
import org.apache.commons.codec.binary.Base64;

public class NLPCryptoUtil {
    private static final String key = "197448";
    private static final byte[] keyByte = key.getBytes();

    public static String encode(String data) {
        if (data == null) {
            return "";
        }

        byte[] dataByte = null;
        dataByte = data.getBytes(CommonConsts.CHARSET_UTF_8);
        byte[] encodedByte = XOR(dataByte, keyByte);
        String encodedString = bytesToHex(encodedByte);

        return encodedString;
    }

    /**
     * Legacy 로그 중 AppName: ASR 인경우
     * 
     * @param encodedString
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decodeMetaAppNameASR(String encodedString) throws UnsupportedEncodingException {
        if (encodedString == null) {
            return ""; // 어떤 입력값은 인코딩 없이 입력 됨.
        } else if (encodedString.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣:]+.*")) {
            return encodedString;
        }
        return new String(Base64.decodeBase64(encodedString), CommonConsts.CHARSET_UTF_8);
    }

    /**
     * AppName: starttv 인 경우
     *
     * @param data
     * @return
     */
    public static byte[] decodeAppNameSmartTV(String data) {
        //
        // trap door
        //
        if (data.length() == 0) {
            return null;
        }
        //
        // declaration
        //
        byte[] result = null;
        byte[] decoded = null;
        final String key = "dpfwlwjswktmakxmxlqldmatjddlstlr";
        final byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher cipher = null;
        SecretKeySpec sks = null;
        IvParameterSpec ips = null;
        //
        // decode & decrypt
        //
        try {
            decoded = Base64.decodeBase64(data);
            sks = new SecretKeySpec(key.getBytes(), "AES");
            ips = new IvParameterSpec(iv);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sks, ips);
            result = cipher.doFinal(decoded);
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        //
        // done
        //
        return result;
    }

    public static String decode(String encodedString) {
        if (encodedString == null)
            return "";
        // 어떤 입력값은 인코딩 없이 입력 됨.
        else if (encodedString.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣:]+.*")) {
            return encodedString;
        }

        byte[] encodedByte = null;

        try {
            encodedByte = hexToBytes(encodedString);
        } catch (NumberFormatException e) {
            try {
                // base64 encode 케이스 // unknown+asr
                return new String(Base64.decodeBase64(encodedString), CommonConsts.CHARSET_UTF_8);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        byte[] decodedByte = XOR(encodedByte, keyByte);

        // 2015.03.26. JIYEON, ambiguous encoding defense
        String decodedString = new String(decodedByte, CommonConsts.CHARSET_UTF_8);

        if (decodedString.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*") == false) {
            try {
                decodedString = new String(decodedByte, "KSC5601");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // END, 2015.03.26 JIYEON

        //CustomLog.d(CustomLog.NLP, "[Decoding String]\n" + decodedString);
        return decodedString;
    }

    public static byte[] encode(byte[] data) {
        return XOR(data, keyByte);
    }

    public static byte[] decode(byte[] data) {
        return XOR(data, keyByte);
    }

    private static byte[] XOR(byte[] org, byte[] keybuf) {
        byte[] converted = new byte[org.length];
        for (int i = 0, j = 0; i < org.length; i++, j++) {
            converted[i] = (byte) (org[i] ^ keybuf[j]);

            if (j >= (keybuf.length - 1)) {
                j = 0;
            }
        }
        return converted;
    }

    private static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        }

        int len = data.length;
        String str = "";

        // 0x는 16진수를 나타낸다.
        // 0xff는 10진수로 치면 255
        // 1111 1111 => 128 + 64 + 32 +16 + 8 + 4 + 2 + 1 = 255
        for (int i = 0; i < len; i++) {
            if ((data[i] & 0xFF) < 16) { // 2자리 포맷 맞춰주기
                str = str + "0" + Integer.toHexString(data[i] & 0xFF);
            } else {
                str = str + Integer.toHexString(data[i] & 0xFF);
            }
        }

        return str;
    }

    // hex string -> byte
    private static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        } else if (str.length() < 2) {
            return null;
        } else {
            int len = str.length() / 2;
            byte[] buffer = new byte[len];
            try {
                for (int i = 0; i < len; i++) {
                    // 16진수 이므로 숫자 변환시 radix를 명시 한다.
                    buffer[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
                }
            } catch (Exception e) {
                throw e;
            }
            return buffer;
        }
    }

    // MD5
    public static String encodeMD5(String str) {
        if (str == null)
            return "";
        String MD5 = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            MD5 = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            MD5 = null;
        }
        return MD5;
    }

    // SHA-256
    public static String encodeSHA256(String str) {
        if (str == null)
            return "";
        String SHA = "";
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            SHA = null;
        }
        return SHA;
    }

}