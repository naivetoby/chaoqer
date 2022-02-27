package com.chaoqer.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.random.MersenneTwister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.*;

/**
 * 数值工具类
 *
 * @author toby
 */
public class DigestUtil {

    private final static Logger logger = LoggerFactory.getLogger(DigestUtil.class);

    private static String toHex(byte[] data) {
        final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            sb.append(HEX_DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }

    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return toHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public static String sha1(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return toHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public static String sha512(String data) {
        return sha512(data, "", 1);
    }

    public static String sha512(String data, String salt, int iterations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] result = digest.digest((salt + data).getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < iterations; i++) {
                digest.reset();
                result = digest.digest(result);
            }
            return toHex(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String base64Encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Decode(String data) {
        return new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
    }

    // 获得指定长度的随机字符串
    public static String getRandString(int length) {
        try {
            byte[] bytes = new byte[length / 2];
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
            secureRandom.nextBytes(bytes);
            return toHex(bytes);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    // 获得指定长度的随机数字
    public static String getRandNumber(int length) {
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
            MersenneTwister mt = new MersenneTwister(secureRandom.nextLong());
            return String.valueOf(mt.nextLong((long) Math.pow(10, length) - 1) % ((long) Math.pow(10, length) - (long) Math.pow(10, length - 1)) + (long) Math.pow(10, length - 1));
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    // 获得UUID
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 在比较哈希值的时候，经过固定的时间才返回结果,避免攻击
    public static boolean slowEquals(String oldPwd, String newPwd) {
        char[] a = oldPwd.toCharArray();
        char[] b = newPwd.toCharArray();
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    public static String getGenerateWord() {
        // 将数字读入list
        List<String> list = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        // 打乱
        Collections.shuffle(list);
        // 重组成字符串
        return StringUtils.join(list, "").substring(0, 6);
    }

    public static String fileToMD5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // 通过通道读取文件
            FileInputStream in = new FileInputStream(file);
            FileChannel ch = in.getChannel();
            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            digest.update(byteBuffer);
            // 关闭流和通道
            ch.close();
            in.close();
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

}
