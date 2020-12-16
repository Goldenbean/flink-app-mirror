package raptor.streaming.mirror;

public class Constants {

  public static String defaultBrokers = "kafka-1:9092";
  public static String defaultSinkTopic = "test-mirror";
  public static String defaultSourceTopics = "test,test-source";
  public static String defaultSourceTopicGroup = "flink-app-mirror-consumer";
  public static String defaultJobName = "flink-app-mirror";

}
