package com.example.lotto.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldCipherTest {

    private static final String KEY_A = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String KEY_B = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final FieldCipher cipher = new FieldCipher(KEY_A, KEY_B);

    @Test
    void roundTripsUnicode() {
        String encrypted = cipher.encrypt("홍길동 hong@example.com");
        assertThat(encrypted).startsWith("v1:").doesNotContain("홍길동");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("홍길동 hong@example.com");
    }

    @Test
    void sameValueEncryptsDifferentlyButHashesTheSame() {
        assertThat(cipher.encrypt("a@b.c")).isNotEqualTo(cipher.encrypt("a@b.c"));
        assertThat(cipher.hash("a@b.c")).isEqualTo(cipher.hash("a@b.c")).hasSize(64);
    }

    @Test
    void nullStaysNull() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    void tamperedCiphertextFails() {
        String encrypted = cipher.encrypt("secret");
        byte[] raw = Base64.getDecoder().decode(encrypted.substring(3));
        raw[raw.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyFails() {
        String encrypted = cipher.encrypt("secret");
        FieldCipher other = new FieldCipher(KEY_B, KEY_B);
        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsShortKey() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new FieldCipher(shortKey, KEY_B)).isInstanceOf(IllegalArgumentException.class);
    }
}
