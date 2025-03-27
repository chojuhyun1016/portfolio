package org.example.order.common.utils;

import java.util.Base64;

public class Base64Utils {

    public static String encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public static String encodeUrlSafe(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    public static byte[] decodeUrlSafe(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    public static String encodeMime(byte[] input) {
        return Base64.getMimeEncoder().encodeToString(input);
    }

    public static byte[] decodeMime(String base64Mime) {
        return Base64.getMimeDecoder().decode(base64Mime);
    }
}
