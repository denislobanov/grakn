package ai.grakn.engine.backgroundtasks.distributed;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;

import static ai.grakn.engine.backgroundtasks.distributed.KafkaConfig.*;

public class TaskRunner implements Runnable {
    private KafkaConsumer<String, String> consumer;

    public TaskRunner() {
        consumer = new KafkaConsumer<>(workQueueConsumer());
        consumer.subscribe(Arrays.asList(WORK_QUEUE_TOPIC));
    }

    public void run() {
        while(true) {
            try {
                // Use 0 timeout to restrict how many messages poll returns
                ConsumerRecords records = consumer.poll(500);

                records.forEach(System.out::println);
            }
            catch (Exception e) {
                e.printStackTrace();
                break;
            }

        }


    }
}
