package com.livescore.backend.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Converter
public class SetConverter implements AttributeConverter<Set<Long>, String> {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Set<Long> set) {
        try { return om.writeValueAsString(set); }
        catch (Exception e) { return "[]"; }
    }

    @Override
    public Set<Long> convertToEntityAttribute(String json) {
        try { return om.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashSet<>(); }
    }
}

