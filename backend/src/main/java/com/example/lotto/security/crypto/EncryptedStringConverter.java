package com.example.lotto.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 엔티티 필드에 {@code @Convert(converter = EncryptedStringConverter.class)}를 붙이면
 * DB에는 암호문으로 저장되고 코드에서는 평문으로 다룬다.
 * Spring Boot가 Hibernate에 빈 컨테이너를 연결해 두어 생성자 주입이 동작한다.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldCipher cipher;

    public EncryptedStringConverter(FieldCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
