package ai.grakn.engine.backgroundtasks;

import ai.grakn.engine.GraknEngineTestBase;
import ai.grakn.engine.backgroundtasks.distributed.KafkaConfig;
import ai.grakn.engine.backgroundtasks.distributed.TaskRunner;
import ai.grakn.engine.backgroundtasks.distributed.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.Executors;

import static ai.grakn.engine.backgroundtasks.distributed.KafkaConfig.WORK_QUEUE_TOPIC;

public class TaskRunnerKafkaTest extends GraknEngineTestBase {
    private KafkaProducer<String, String> producer;
    private StateStorage stateStorage;

    @Before
    public void setUp() {
        producer = new KafkaProducer<>(KafkaConfig.workQueueProducer());
        stateStorage = new GraknStateStorage();

        // Run executor in background
        Executors.newSingleThreadExecutor().execute(new TaskRunner());
    }

    @Test
    public void testSendReceive() {
        for (int i = 0; i < 100; i++) {
            // Create test task
            JSONObject config = new JSONObject().put("test", "config");
            String id = stateStorage.newState(TestTask.class.getName(), this.getClass().getName(), new Date(), false, 0, config);

            producer.send(new ProducerRecord<>(WORK_QUEUE_TOPIC, id,
                    new Message()
                            .status(TaskStatus.SCHEDULED)
                            .configuration(config)
                            .toString()
            ));
            producer.flush();

            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        producer.close();
    }
}
