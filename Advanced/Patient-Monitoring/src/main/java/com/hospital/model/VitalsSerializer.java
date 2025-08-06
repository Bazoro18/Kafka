package main.java.com.hospital.model;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VitalsSerializer implements Serializer<Vitals>{
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public byte[] serialize(String topic, Vitals data) {
        if (data == null) {
            return null;
        }
        try {
            //ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing Vitals object", e);
        }
    }
}
