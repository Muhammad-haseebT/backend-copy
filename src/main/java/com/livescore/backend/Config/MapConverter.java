package com.livescore.backend.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class MapConverter implements AttributeConverter<Map<Long, Integer>, String> {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Long, Integer> map) {
        try { return om.writeValueAsString(map); }
        catch (Exception e) { return "{}"; }
    }

    @Override
    public Map<Long, Integer> convertToEntityAttribute(String json) {
        try { return om.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }
}
