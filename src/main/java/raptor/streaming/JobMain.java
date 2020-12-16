package raptor.streaming;


import static raptor.streaming.mirror.Constants.defaultJobName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.shaded.guava18.com.google.common.base.Splitter;
import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raptor.streaming.mirror.Constants;
import raptor.streaming.mirror.MirrorMap;


public class JobMain {

  private static final Logger logger = LoggerFactory.getLogger(JobMain.class);

  public static void main(String[] args) throws Exception {

    ParameterTool parameterTool = ParameterTool.fromArgs(args);

    String brokers = parameterTool.get("brokers", Constants.defaultBrokers);
    String consumerGroup = parameterTool.get("group", Constants.defaultSourceTopicGroup);
    String sourceTopics = parameterTool.get("source", Constants.defaultSourceTopics);
    String sinkTopic = parameterTool.get("sink", Constants.defaultSinkTopic);
    String uuid = parameterTool.get("uuid", UUID.randomUUID().toString());

    /**
     * ============================================================================================
     */

    final Properties consumerProperties = new Properties();
    consumerProperties.put("bootstrap.servers", brokers);
    consumerProperties.put("group.id", consumerGroup);
    consumerProperties.put("max.partition.fetch.bytes", 7 * 1024 * 1024);

    Iterable<String> it = Splitter.on(",")
        .omitEmptyStrings()
        .trimResults()
        .split(sourceTopics);

    List<String> topics = Lists.newArrayList(it);

    FlinkKafkaConsumer<String> source = new FlinkKafkaConsumer<String>(
        topics, new SimpleStringSchema(), consumerProperties);

    source.setStartFromGroupOffsets();

    /**
     * ============================================================================================
     */

    final Properties producerProperties = new Properties();
    producerProperties.put("bootstrap.servers", brokers);
    producerProperties.put("max.request.size", 7 * 1024 * 1024);
    producerProperties.put("batch.size", 1000);
    producerProperties.put("linger.ms", 300);

    FlinkKafkaProducer<String> sink = new FlinkKafkaProducer<String>(
        sinkTopic, new SerializationSchema<String>() {
      @Override
      public byte[] serialize(String input) {
        return input.getBytes();
      }
    }, producerProperties, Optional.of(new RandomPartitioner<String>())
    );

    /**
     * ============================================================================================
     */

    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
    env.addSource(source)
        .map(new MirrorMap())
        .filter(tuple -> !"NULL".equals(tuple.f0))
        .map(tuple -> new String(tuple.f1, StandardCharsets.UTF_8))
        .addSink(sink);

    env.execute(defaultJobName + "-" + uuid);

  }


}
