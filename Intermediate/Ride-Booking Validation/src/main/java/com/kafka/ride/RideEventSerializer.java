package com.kafka.ride;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class RideEventSerializer implements Serializer<RideRequest> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, RideRequest data) {
        try {
            if (data == null) return null;
            String jsonString = objectMapper.writeValueAsString(data);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing RideRequest to JSON", e);
        }
    }
}
