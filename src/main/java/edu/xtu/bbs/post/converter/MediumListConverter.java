package edu.xtu.bbs.post.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.post.model.Medium;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class MediumListConverter implements AttributeConverter<List<Medium>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Medium>> TYPE_REF = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Medium> attribute) {
        try {
            if (attribute == null) return null;
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert Medium list to JSON", e);
        }
    }

    @Override
    public List<Medium> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return Collections.emptyList();
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert JSON to Medium list", e);
        }
    }
}
