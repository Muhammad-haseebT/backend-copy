package com.livescore.backend.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

// new file: Config/FeedbackMapConverter.java
@Converter
public class FeedbackMapConverter implements AttributeConverter<Map<Long, String>, String> {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Long, String> map) {
        try { return map == null ? "{}" : om.writeValueAsString(map); }
        catch (Exception e) { return "{}"; }
    }

    @Override
    public Map<Long, String> convertToEntityAttribute(String json) {
        try { return json == null ? new HashMap<>() : om.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }
}