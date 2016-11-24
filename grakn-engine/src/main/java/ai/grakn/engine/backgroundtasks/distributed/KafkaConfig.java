package ai.grakn.engine.backgroundtasks.distributed;

import java.util.Properties;

public class KafkaConfig {
    public final static String WORK_QUEUE_TOPIC = "work-queue";
    public final static String NEW_TASKS_TOPIC = "new-tasks";
    public final static int POLL_FREQUENCY = 1000;

    public static Properties workQueueConsumer() {
        Properties properties = new Properties();

        //TODO: get from config
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("group.id", "task-runners");
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("session.timeout.ms", "30000");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return properties;
    }

    public static Properties workQueueProducer() {
        Properties properties = new Properties();

        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer",  "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return properties;
    }
}
