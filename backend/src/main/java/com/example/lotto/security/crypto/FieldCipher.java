package com.example.lotto.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 개인정보 컬럼 암호화.
 * <ul>
 *   <li>encrypt/decrypt: AES-256-GCM. 같은 값도 매번 다른 암호문이 나오고, 변조되면 복호화가 실패한다.
 *       저장 형식은 "v1:" + Base64(IV 12바이트 + 암호문 + 태그)이며, 키를 교체할 때 버전으로 구분한다.</li>
 *   <li>hash: HMAC-SHA256. 암호문으로는 검색할 수 없으므로 이메일 중복 확인처럼 같은 값을 찾을 때 쓴다.</li>
 * </ul>
 */
public class FieldCipher {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public FieldCipher(String aesKeyBase64, String hmacKeyBase64) {
        byte[] aes = Base64.getDecoder().decode(aesKeyBase64);
        byte[] hmac = Base64.getDecoder().decode(hmacKeyBase64);
        if (aes.length != 32) {
            throw new IllegalArgumentException("AES 키는 32바이트(256비트)여야 합니다.");
        }
        if (hmac.length < 32) {
            throw new IllegalArgumentException("HMAC 키는 32바이트 이상이어야 합니다.");
        }
        this.aesKey = new SecretKeySpec(aes, "AES");
        this.hmacKey = new SecretKeySpec(hmac, "HmacSHA256");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIX)) {
            throw new IllegalStateException("알 수 없는 암호문 형식입니다.");
        }
        try {
            byte[] data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("복호화에 실패했습니다. 키가 바뀌었거나 데이터가 변조되었습니다.", e);
        }
    }

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("해시 계산에 실패했습니다.", e);
        }
    }
}
