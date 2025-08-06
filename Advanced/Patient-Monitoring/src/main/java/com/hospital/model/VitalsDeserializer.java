package main.java.com.hospital.model;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class VitalsDeserializer implements Deserializer<Vitals> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Vitals deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, Vitals.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing Vitals object", e);
        }
    }

    @Override
    public void close() {}   
}
