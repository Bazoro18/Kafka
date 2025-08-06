package main.java.com.hospital.model;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.time.Instant;
import java.util.Random;

public class VitalsProducer {
    private static final String TOPIC = "patient-vitals";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private static final Random random = new Random();

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VitalsSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        KafkaProducer<String, Vitals> producer = new KafkaProducer<>(props);
        boolean flag = true; // Control flag to keep the producer running
        try{
            while(flag){
                for(int i=1; i<=10000; i++){
                    Vitals vitals = generateVitals("patient-" + i, i % 3 == 0);
                    String patientID = vitals.getPatientId();

                    ProducerRecord<String, Vitals> record = new ProducerRecord<>(TOPIC, patientID, vitals);
                    ProducerRecord<String, Vitals> archive = new ProducerRecord<>("vitals-archive", patientID, vitals);
                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            System.err.println("Error sending to " + TOPIC + ": " + exception.getMessage());
                        } else {
                            System.out.printf("Sent message with key %s to partition %d at offset %d%n",
                                    patientID, metadata.partition(), metadata.offset());
                        }
                    });

                    Thread.sleep(10); // Sleep for a second before sending the next record
                }
                flag = false; // Set flag to false to stop after one batch
            }
        }
        catch(Exception e){
            System.err.println("Error in producer: " + e.getMessage());
        }
        finally {
            producer.close();
        }
    }
    private static Vitals generateVitals(String patientId, boolean oldData) {
        Instant timestamp = oldData ? Instant.now().minusSeconds(8*24*3600) : Instant.now();
        double heartRate = 60 + random.nextDouble() * 40; // 60 to 100
        double spo2 = 95 + random.nextDouble() * 5; // 95 to 100
        double temperature = 36.5 + random.nextDouble(); // 36.5 to 37.5
        double bloodPressure = 110 + random.nextDouble() * 20; // 110 to 130

        return new Vitals(patientId, timestamp.toString(), heartRate, spo2, temperature, bloodPressure);
    }
}
