import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BookingEventSerializer implements Serializer<BookingEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public byte[] serialize(String topic, BookingEvent data){
        try{
            return objectMapper.writeValueAsBytes(data);
        }
        catch(Exception e){
            throw new RuntimeException("Error serializing BookingEvent", e);
        }
    }
}
