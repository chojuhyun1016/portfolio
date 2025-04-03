package org.example.order.common.utils.encode;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class Base64Utils {

    // Base64 Encoder/Decoder constants
    private static final Base64.Encoder STANDARD_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder STANDARD_DECODER = Base64.getDecoder();

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder();
    private static final Base64.Decoder MIME_DECODER = Base64.getMimeDecoder();

    // Default charset
    private static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;

    // Standard Base64
    public static String encode(byte[] input) {
        return STANDARD_ENCODER.encodeToString(input);
    }

    public static byte[] decode(String base64) {
        return STANDARD_DECODER.decode(base64);
    }

    public static String encode(String input) {
        return encode(input.getBytes(CHARSET));
    }

    public static String decodeToString(String base64) {
        return new String(decode(base64), CHARSET);
    }

    // URL-safe Base64
    public static String encodeUrlSafe(byte[] input) {
        return URL_ENCODER.encodeToString(input);
    }

    public static byte[] decodeUrlSafe(String base64Url) {
        return URL_DECODER.decode(base64Url);
    }

    public static String encodeUrlSafe(String input) {
        return encodeUrlSafe(input.getBytes(CHARSET));
    }

    public static String decodeUrlSafeToString(String base64Url) {
        return new String(decodeUrlSafe(base64Url), CHARSET);
    }

    // MIME Base64
    public static String encodeMime(byte[] input) {
        return MIME_ENCODER.encodeToString(input);
    }

    public static byte[] decodeMime(String base64Mime) {
        return MIME_DECODER.decode(base64Mime);
    }

    public static String encodeMime(String input) {
        return encodeMime(input.getBytes(CHARSET));
    }

    public static String decodeMimeToString(String base64Mime) {
        return new String(decodeMime(base64Mime), CHARSET);
    }
}
