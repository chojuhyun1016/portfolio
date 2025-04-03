package org.example.order.common.utils.encode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipBase64Utils {

    private static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder();
    private static final Base64.Decoder MIME_DECODER = Base64.getMimeDecoder();
    private static final Base64.Encoder STANDARD_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder STANDARD_DECODER = Base64.getDecoder();

    // ===== URL SAFE =====
    public static String compressAndEncodeUrlSafe(String input) {
        return URL_ENCODER.encodeToString(gzipCompress(input.getBytes(CHARSET)));
    }

    public static String compressAndEncodeUrlSafe(byte[] inputBytes) {
        return URL_ENCODER.encodeToString(gzipCompress(inputBytes));
    }

    public static String decodeAndDecompressUrlSafe(String base64Input) {
        return new String(gzipDecompress(URL_DECODER.decode(base64Input)), CHARSET);
    }

    public static byte[] decodeAndDecompressToBytesUrlSafe(String base64Input) {
        return gzipDecompress(URL_DECODER.decode(base64Input));
    }

    // ===== MIME =====
    public static String compressAndEncodeMime(String input) {
        return MIME_ENCODER.encodeToString(gzipCompress(input.getBytes(CHARSET)));
    }

    public static String decodeAndDecompressMime(String base64Input) {
        return new String(gzipDecompress(MIME_DECODER.decode(base64Input)), CHARSET);
    }

    // ===== STANDARD =====
    public static String compressAndEncodeStandard(String input) {
        return STANDARD_ENCODER.encodeToString(gzipCompress(input.getBytes(CHARSET)));
    }

    public static String decodeAndDecompressStandard(String base64Input) {
        return new String(gzipDecompress(STANDARD_DECODER.decode(base64Input)), CHARSET);
    }

    // ===== COMMON INTERNAL =====
    private static byte[] gzipCompress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(bos)) {

            gzipOut.write(data);
            gzipOut.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("GZIP compression failed", e);
        }
    }

    private static byte[] gzipDecompress(byte[] compressedData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bis);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("GZIP decompression failed", e);
        }
    }
}
