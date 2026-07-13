package com.cliuno.api.support;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Minimal RFC 6238 TOTP (sha1, 6 digits, 30s) — no extra dependency needed. */
public final class TotpService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpService() {}

    public static String generateSecret() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(32)));
        }
        return sb.toString();
    }

    public static String provisioningUri(String secret, String label) {
        return "otpauth://totp/CLIuno:" + label + "?secret=" + secret
                + "&issuer=CLIuno&algorithm=SHA1&digits=6&period=30";
    }

    public static boolean verify(String secret, String code, int window) {
        if (code == null || code.isBlank()) {
            return false;
        }
        long counter = System.currentTimeMillis() / 1000 / 30;
        for (long i = -window; i <= window; i++) {
            if (generate(secret, counter + i).equals(code.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String generate(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] base32Decode(String secret) {
        String clean = secret.toUpperCase().replaceAll("=+$", "");
        int bits = 0;
        int value = 0;
        ByteBuffer out = ByteBuffer.allocate(clean.length());
        for (char c : clean.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                continue;
            }
            value = (value << 5) | index;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) ((value >>> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        byte[] result = new byte[out.position()];
        out.rewind();
        out.get(result);
        return result;
    }
}
