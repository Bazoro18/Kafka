package com.kafka.ride;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class RideRequestProducer {

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TOPIC = "ride_requests";
    public static final Random random = new Random();

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "com.kafka.ride.RideEventSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        KafkaProducer<String, RideRequest> producer = new KafkaProducer<>(props);

        for (int i = 0; i < 10000; i++) {
            RideRequest ride = generateRandomRideRequest(i);

            ProducerRecord<String, RideRequest> record =
                    new ProducerRecord<>(TOPIC, ride.getRideId(), ride);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("✅ Sent: %s | Partition: %d | Offset: %d%n",
                            ride.getRideId(), metadata.partition(), metadata.offset());
                } else {
                    System.err.println("❌ Error: " + exception.getMessage());
                }
            });

            Thread.sleep(10);
        }

        producer.flush();
        producer.close();
    }

    public static RideRequest generateRandomRideRequest(int index) {
        String rideId = "RIDE_" + UUID.randomUUID();
        String userId = "USER_" + (1000 + index);
        String paymentMethod = (index % 3 == 0) ? null : "card";
        double amount = (index % 4 == 0) ? 0.0 : 100 + random.nextInt(50);
        String pickup = "12.9" + random.nextInt(10) + ",77.6" + random.nextInt(10);
        String drop = (index % 5 == 0) ? pickup : "12.9" + random.nextInt(10) + ",77.6" + random.nextInt(10);

        return new RideRequest(rideId, userId, pickup, drop, paymentMethod, amount);
    }
}
