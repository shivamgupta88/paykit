package com.paykit.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final String secretKey;

    public EncryptedStringConverter(
            @Value("${paykit.encryption.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return EncryptionUtil.encrypt(attribute, secretKey);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return EncryptionUtil.decrypt(dbData, secretKey);
    }
}
