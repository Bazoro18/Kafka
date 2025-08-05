import org.apache.kafka.clients.producer.*;
import java.util.Properties;
import java.time.Instant;
import java.util.Random;

public class BookingEventProducer {
    public static void main(String[] args) throws Exception{
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "BookingEventSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "BookingEventSerializer");
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "TheatrePartitioner");

        //Tuning Properties
        props.put(ProducerConfig.ACKS_CONFIG,"all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        KafkaProducer<String, BookingEvent> producer = new KafkaProducer<>(props);

        String[] theatres = {"PVR-MUMBAI", "INOX-DELHI", "CINEPOLIS-BANGALORE", "CINEWORLD-PUNE"};
        String[] movies = {"Avengers", "Inception", "Titanic", "Jurassic Park", "The Dark Knight", "Interstellar", "The Matrix"};
        Random rand = new Random();

        long start = System.currentTimeMillis();

        for(int i=0; i<3000; i++){
            String bookingId = "BID-" + (i + 1);
            String userId = "UID-" + (rand.nextInt(100) + 1);
            String movie = movies[rand.nextInt(movies.length)];
            String theaterId = theatres[rand.nextInt(theatres.length)];
            String timestamp = Instant.now().toString();
            int seats = rand.nextInt(10) + 1; // Random seats between 1 and 10
            double amount = seats * (rand.nextDouble() * 100 + 50); // Random amount between 50 and 150 per seat

            BookingEvent event = new BookingEvent(bookingId, userId, movie, theaterId, timestamp, seats, amount);

            ProducerRecord<String, BookingEvent> record = new ProducerRecord<>("ticket-booking", event.getTheaterID(), event);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Error sending message: " + exception.getMessage());
                } else {
                    System.out.println("Sent message with key: " + userId + ", partition: " + metadata.partition() + ", offset: " + metadata.offset());
                }
            });

            Thread.sleep(rand.nextInt(3));
        }
        producer.flush(); 
        long duration = System.currentTimeMillis() - start;
        System.out.println("Produced event messages in " + duration + " ms");
        producer.close();
    }
}
