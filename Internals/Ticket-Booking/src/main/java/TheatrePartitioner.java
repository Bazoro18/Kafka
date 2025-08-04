import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import java.util.Map;

public class TheatrePartitioner implements Partitioner{
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster){
        int numPartitions = cluster.partitionCountForTopic(topic);
        if (key == null){
            return 0;
        }
        return Math.abs(key.hashCode()) % numPartitions;
    }
    @Override
    public void close(){
        System.out.println("Closing TheatrePartitioner");
    }
    @Override
    public void configure(Map<String, ?> configs){
        System.out.println("Configuring TheatrePartitioner with configs: " + configs);
    }
}
