package com.vehicle.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ChecklistCatalog {
    private final Map<String, String> sections = new HashMap<>();

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    void load() {
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("checklist.json")) {
            if (input == null) {
                throw new IllegalStateException("checklist.json is missing from the application resources");
            }
            JsonNode points = objectMapper.readTree(input).path("vehicle_types").path("CAR");
            for (JsonNode point : points) {
                sections.put(point.path("point_code").asText(), point.path("section").asText());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load checklist.json", exception);
        }
    }

    public boolean contains(String pointCode) {
        return sections.containsKey(pointCode);
    }

    public boolean isEnginePoint(String pointCode) {
        return "ENGINE".equals(sections.get(pointCode));
    }

    public Set<String> pointCodes() {
        return Set.copyOf(sections.keySet());
    }
}
