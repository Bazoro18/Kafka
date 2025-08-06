package com.kafka.ride;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RideRequestProducer {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "ride_requests";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        Producer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 0; i < 10; i++) {
            RideRequest ride = generateRandomRideRequest(i);
            String rideJson = objectMapper.writeValueAsString(ride);

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, ride.getRideId(), rideJson);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("✅ Sent: %s | Partition: %d, Offset: %d%n",
                            ride.getRideId(), metadata.partition(), metadata.offset());
                } else {
                    System.err.println("❌ Error sending ride: " + exception.getMessage());
                }
            });

            Thread.sleep(500); // simulate delay
        }

        producer.flush();
        producer.close();
    }

    private static RideRequest generateRandomRideRequest(int index) {
        String rideId = "RIDE_" + UUID.randomUUID();
        String userId = "USER_" + (1000 + index);
        String paymentMethod = (index % 3 == 0) ? null : "card";
        double amount = (index % 4 == 0) ? 0.0 : 100 + random.nextInt(50);
        String pickup = "12.9" + random.nextInt(10) + ",77.6" + random.nextInt(10);
        String drop = (index % 5 == 0) ? pickup : "12.9" + random.nextInt(10) + ",77.6" + random.nextInt(10);

        return new RideRequest(rideId, userId, pickup, drop, paymentMethod, amount);
    }
}
