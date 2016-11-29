package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.*;
import ai.grakn.engine.backgroundtasks.distributed.zookeeper.ZookeeperConfig;
import ai.grakn.engine.util.ConfigProperties;
import ai.grakn.engine.util.SystemOntologyElements;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.function.Consumer;

import static ai.grakn.engine.backgroundtasks.TaskStatus.*;
import static ai.grakn.engine.backgroundtasks.distributed.ZooKeeperStateStorage.PATH_PREFIX;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class TaskRunner implements Runnable {
    private final static String LOCK_SUFFIX = "/lock";
    private final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private boolean running;
    private StateStorage graknStorage;
    private StateStorage zkStorage;
    private KafkaConsumer<String, String> consumer;
    private CuratorFramework client;

    public TaskRunner() throws Exception {
        running = true;
        graknStorage = new GraknStateStorage();
        zkStorage = new ZooKeeperStateStorage();

        consumer = new KafkaConsumer<>(workQueueConsumer());
        consumer.subscribe(Arrays.asList(WORK_QUEUE_TOPIC));
        client = CuratorFrameworkFactory.newClient(ZookeeperConfig.ZOOKEEPER_URL, new ExponentialBackoffRetry(1000, 3));
    }

    /**
     * Start the main loop, this will block until a call to stop().
     */
    public void run() {
        // Make sure we are connected to zookeeper
        try {
            client.start();
            client.blockUntilConnected();
        }
        catch (InterruptedException e) {
            System.out.println(e);
            Arrays.asList(e.getStackTrace()).forEach(x -> LOG.error(x.toString()));
            return;
        }

        // Poll on Kafka
        while(running) {
            ConsumerRecords<String, String> records = consumer.poll(500);

            try {
                if (!records.isEmpty()) {
                    System.out.println("got " + records.count() + " records");

                    for (ConsumerRecord<String, String> r : records) {
                        if (markAsRunning(r.key()))
                            runTask(r.key(), TaskState.deserialize(r.value()));
                    }
                }
                else {
                    Thread.sleep(500);
                }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Arrays.asList(e.getStackTrace()).forEach(x -> LOG.error(x.toString()));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stop the main loop, causing run() to exit.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Checks to see if task can be marked as running, and does so if possible. Updates TaskState in ZK & Grakn.
     * @param id String id
     * @return Boolean, true if task could be marked as running (and we should run), false otherwise.
     */
    private Boolean markAsRunning(String id) {
        Boolean shouldRun = false;

        try {
            InterProcessMutex mutex = newMutex(id);

            // Move on to next task if cant lock
            if(!mutex.acquire(1000, MILLISECONDS))
                return false;

            // Read state
            TaskState state = zkStorage.getState(id);
            if(state == null) {
                System.out.println("Failed to mark task: " + id + " as running");
                return null;
            }

            // Old or duplicate messages
            if((state.status() == SCHEDULED) ||
               (state.status() == RUNNING && state.executingHostname() == null) ||
               (state.status() == RUNNING && state.executingHostname().isEmpty()))
            {
                shouldRun = true;
                zkStorage.updateState(id, RUNNING, this.getClass().getName(), getHostname(), null, null, null);
                graknStorage.updateState(id, RUNNING, this.getClass().getName(), getHostname(), null, null, null);
                System.out.println("all good");
            }
            else {
                System.out.println(" cant mark as running because\n\t\tstatus:" + state.status() + "\n\t\t executing hostname: " + state.executingHostname());
            }

            mutex.release();
        }
        catch(Exception e) {
            System.out.println("Failed to mark task: " + id + " as running");
        }

        System.out.println("markAsRunning -> "+shouldRun);
        return shouldRun;
    }

    /**
     * Instantiate a BackgroundTask object and run it, catching any thrown Exceptions.
     * @param id String ID of task as used *both* in ZooKeeper and GraknGraph. This must be the ID generated by Grakn Graph.
     * @param state TaskState for task @id.
     * @throws ClassNotFoundException Could not instantiate Object from TaskState.taskClassName().
     * @throws InstantiationException Could not instantiate Object from TaskState.taskClassName().
     * @throws IllegalAccessException Could not instantiate Object from TaskState.taskClassName().
     */
    private void runTask(String id, TaskState state) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        System.out.println("Running task "+state.taskClassName());

        // Instantiate task
        Class<?> c = Class.forName(state.taskClassName());
        BackgroundTask task = (BackgroundTask) c.newInstance();

        try {
            task.start(saveCheckpoint(id), state.configuration());
        }
        catch(Throwable t) {
            updateState(id, FAILED, this.getClass().getName(), null, t, null, null);
            return;
        }

        updateState(id, COMPLETED, this.getClass().getName(), null, null, null, null);
    }

    /**
     * Get hostname of running OS, falling back to config file hostname otherwise.
     * @return String hostname
     */
    private String getHostname() {
        String hostname = ConfigProperties.getInstance().getProperty(ConfigProperties.SERVER_HOST_NAME);
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            Arrays.asList(e.getStackTrace()).forEach(x -> LOG.error(x.toString()));
        }

        return hostname;
    }

    /**
     * Returns a new InterProcessMutex object, creating ZNodes if needed
     * @param id String id of task that this lock should be associated to.
     * @return InterProcessMutex object
     * @throws Exception as throw by CuratorFramework
     */
    private InterProcessMutex newMutex(String id) throws Exception {
        if(client.checkExists().forPath(PATH_PREFIX+id+LOCK_SUFFIX) == null)
            client.create().creatingParentContainersIfNeeded().forPath(PATH_PREFIX+id+LOCK_SUFFIX);

        return new InterProcessMutex(client, PATH_PREFIX+id+LOCK_SUFFIX);
    }

    /**
     * Persists a Background Task's checkpoint to ZK and graph.
     * @param id ID of task
     * @return A Consumer<String> function that can be called by the background task on demand to save its checkpoint.
     */
    private Consumer<String> saveCheckpoint(String id) {
        return checkpoint -> {
            System.out.println("Writing checkpoint");
            updateState(id, null, null, null, null, checkpoint, null);

        };
    }

    private void updateState(String id, TaskStatus status, String statusChangeBy, String executingHostname,
                             Throwable failure, String checkpoint, JSONObject configuration) {
        try {
            // Update ZK
            InterProcessMutex mutex = new InterProcessMutex(client, PATH_PREFIX+id+LOCK_SUFFIX);
            mutex.acquire();

            zkStorage.updateState(id, status, statusChangeBy, executingHostname, failure, checkpoint, configuration);

            mutex.release();
        }
        catch (Exception e) {
            System.out.println("Could not write to ZooKeeper! "+e);
            Arrays.asList(e.getStackTrace()).forEach(x -> LOG.error(x.toString()));
        }
        finally {
            // Update Graph
            graknStorage.updateState(id, status, statusChangeBy, executingHostname, failure, checkpoint, configuration);
        }
    }
}
