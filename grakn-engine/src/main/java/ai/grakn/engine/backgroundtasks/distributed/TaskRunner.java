package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.*;
import ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaMessage;
import ai.grakn.engine.util.ConfigProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.function.Consumer;

import static ai.grakn.engine.backgroundtasks.TaskStatus.RUNNING;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.*;

public class TaskRunner implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private StateStorage stateStorage;
    private KafkaConsumer<String, String> consumer;

    public TaskRunner() {
        stateStorage  = new GraknStateStorage();

        consumer = new KafkaConsumer<>(workQueueConsumer());
        consumer.subscribe(Arrays.asList(WORK_QUEUE_TOPIC));
    }

    public void run() {
        while(true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(500);

                if(!records.isEmpty()) {
                    LOG.debug("got "+records.count()+"records");
                    System.out.println("got "+records.count()+"records");

                    // TODO: use ZK to mark entries as executing, find first thats not being run.
                    for(ConsumerRecord<String, String> r: records) {

                        System.out.println("record key: "+r.key());

                        if(markAsRunning(r.key())) {
                            runTask(r.key(), new KafkaMessage(r.value()));
                        }
                    }
                }
                else {
                    Thread.sleep(500);
                }

            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
                // Try next task
            }
            catch (InterruptedException e) {
                break;
            }

        }
    }

    private Boolean markAsRunning(String id) {
        // Check if already in ZK

        // Create ZK entry

        // Update in graph
        String hostname = ConfigProperties.getInstance().getProperty(ConfigProperties.SERVER_HOST_NAME);
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {}

        LOG.debug("Marking task "+id+" as RUNNING, "+hostname);
        System.out.println("Marking task "+id+" as RUNNING, "+hostname);
        stateStorage.updateState(id, RUNNING, this.getClass().getName(), hostname, null, null, null);

        return true;
    }

    private void runTask(String id, KafkaMessage message) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // State contains class name
        TaskState state = stateStorage.getState(id);

        System.out.println("running task: "+state.taskClassName());
        LOG.debug("running task "+state.taskClassName());

        // Instantiate task
        Class<?> c = Class.forName(state.taskClassName());
        BackgroundTask task = (BackgroundTask) c.newInstance();

        task.start(saveCheckpoint(id), message.configuration());
    }

    private Consumer<String> saveCheckpoint(String id) {
        return checkpoint -> {
            // Update ZK

            // Update Graph
            stateStorage.updateState(id, null, null, null, null, checkpoint, null);
        };
    }
}
