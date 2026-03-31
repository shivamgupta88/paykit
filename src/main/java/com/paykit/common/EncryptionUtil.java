package com.paykit.common;

import com.paykit.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EncryptionUtil() {}

    public static String encrypt(String plaintext, String base64Key) {
        try {
            SecretKey key = decodeKey(base64Key);
            byte[] iv = new byte[AppConstants.GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AppConstants.AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key,
                    new GCMParameterSpec(AppConstants.GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    public static String decrypt(String encrypted, String base64Key) {
        try {
            SecretKey key = decodeKey(base64Key);
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[AppConstants.GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AppConstants.AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(AppConstants.GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }

    private static SecretKey decodeKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
