package raptor.streaming;

import java.util.Random;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;


public class RandomPartitioner<T> extends FlinkKafkaPartitioner<T> {

  private static final long serialVersionUID = 1L;
  Random random = new Random();

  @Override
  public void open(int parallelInstanceId, int parallelInstances) {

  }

  @Override
  public int partition(T record, byte[] key, byte[] value, String targetTopic, int[] partitions) {
    return random.nextInt(partitions.length);
  }


}
