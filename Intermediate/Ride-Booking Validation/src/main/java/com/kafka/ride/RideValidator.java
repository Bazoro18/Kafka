package com.kafka.ride;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RideValidator {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String INPUT_TOPIC = "ride_requests";
    private static final String VALID_TOPIC = "valid_rides";
    private static final String INVALID_TOPIC = "rejected_rides";
    private static final String GROUP_ID = "ride-validator-group";
    private static final String TXN_ID = "ride-validator-txn-1";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        // Producer Configs
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, TXN_ID);

        Producer<String, String> producer = new KafkaProducer<>(producerProps);
        producer.initTransactions();

        // Consumer Configs
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        // Rebalance Listener to handle partition reassignments
        consumer.subscribe(Collections.singletonList(INPUT_TOPIC), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println("Partitions revoked: " + partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("Partitions assigned: " + partitions);
            }
        });

        System.out.println("RideValidator started. Listening for messages...");

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) continue;

                Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

                producer.beginTransaction();

                try {
                    for (ConsumerRecord<String, String> record : records) {
                        String value = record.value();
                        RideRequest ride = objectMapper.readValue(value, RideRequest.class);

                        boolean isValid = ValidatorUtils.isValid(ride);
                        String targetTopic = isValid ? VALID_TOPIC : INVALID_TOPIC;

                        producer.send(new ProducerRecord<>(targetTopic, ride.getRideId(), value));

                        // Optional: also log to audit topic
                        producer.send(new ProducerRecord<>("ride_audit", ride.getRideId(), "Processed"));

                        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                        currentOffsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
                    }

                    // Attach offsets to transaction
                    producer.sendOffsetsToTransaction(currentOffsets, GROUP_ID);
                    producer.commitTransaction();
                } catch (Exception e) {
                    System.err.println("Error during processing: " + e.getMessage());
                    producer.abortTransaction();
                }
            }

        } finally {
            consumer.close();
            producer.close();
        }
    }
}
