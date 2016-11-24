package ai.grakn.engine.backgroundtasks;

import ai.grakn.engine.GraknEngineTestBase;
import ai.grakn.engine.backgroundtasks.distributed.KafkaConfig;
import ai.grakn.engine.backgroundtasks.distributed.WorkItem;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;

import static ai.grakn.engine.backgroundtasks.distributed.KafkaConfig.WORK_QUEUE_TOPIC;

public class TaskRunnerKafkaTest extends GraknEngineTestBase {
    private KafkaProducer<String, String> producer;

    @Before
    public void setUp() {
        producer = new KafkaProducer<>(KafkaConfig.workQueueProducer());
    }

    @Test
    public void testSendRecieve() {
        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(WORK_QUEUE_TOPIC,
                    "TASK-ID-"+Integer.toString(i),
                    new WorkItem()
                            .status(TaskStatus.SCHEDULED)
                            .assignedWorker(Integer.toString(i))
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
