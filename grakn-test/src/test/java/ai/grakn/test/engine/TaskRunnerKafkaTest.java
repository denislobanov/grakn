package ai.grakn.test.engine;

import ai.grakn.engine.backgroundtasks.distributed.GraknStateStorage;
import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.TaskStatus;
import ai.grakn.engine.backgroundtasks.distributed.KafkaConfig;
import ai.grakn.engine.backgroundtasks.distributed.TaskRunner;
import ai.grakn.engine.backgroundtasks.distributed.Message;
import ai.grakn.test.AbstractEngineTest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ai.grakn.engine.backgroundtasks.distributed.KafkaConfig.WORK_QUEUE_TOPIC;

public class TaskRunnerKafkaTest extends AbstractEngineTest {
    private KafkaProducer<String, String> producer;
    private StateStorage stateStorage;
    private ExecutorService service;
    private Future<?> future;

    @Before
    public void setUp() {
        Assume.assumeFalse(usingTinker());

        producer = new KafkaProducer<>(KafkaConfig.workQueueProducer());
        stateStorage = new GraknStateStorage();

        // Run executor in background
        service = Executors.newSingleThreadExecutor();
        future = service.submit(new TaskRunner());
    }

    @Test
    public void testSendReceive() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            // Create test task
            JSONObject config = new JSONObject().put("name", "task "+Integer.toString(i));
            String id = stateStorage.newState(TestTask.class.getName(), this.getClass().getName(), new Date(), false, 0, config);
            assert (id != null);

            producer.send(new ProducerRecord<>(WORK_QUEUE_TOPIC, id,
                    new Message()
                            .status(TaskStatus.SCHEDULED)
                            .configuration(config)
                            .toString()
            ));
            producer.flush();
        }

        producer.close();


//        Thread.sleep(10000);


        while(!future.isDone()) {
            Thread.sleep(1000);
        }
    }
}
