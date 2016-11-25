/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.BackgroundTask;
import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.TaskManager;
import ai.grakn.engine.backgroundtasks.TaskState;
import ai.grakn.engine.backgroundtasks.distributed.scheduler.SchedulerClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;

import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.zookeeper.ZookeeperConfig.ZOOKEEPER_URL;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;
import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;

/**
 * Class to manage tasks distributed using Kafka.
 */
public class DistributedTaskManager implements TaskManager, AutoCloseable {

    private static CuratorFramework zookeeperClient;
    private static SchedulerClient schedulerClient;

    private KafkaProducer producer;

    private StateStorage storage;

    /**
     * Instantiate connection with Zookeeper.
     */
    public DistributedTaskManager(){
        try {
            zookeeperClient = newClient(ZOOKEEPER_URL, new ExponentialBackoffRetry(1000, 0));
            zookeeperClient.start();

            // start scheduler client to add self to leader election pool
            schedulerClient = new SchedulerClient(zookeeperClient);
            schedulerClient.start();

            // Kafka producer to add things to new tasks
            producer = new KafkaProducer<>(workQueueProducer());

            // Persisted storage in grakn graph
            storage = new GraknStateStorage();

            // Start TaskRunner
            Executors.newSingleThreadExecutor().execute(new TaskRunner());
        } catch (IOException e){
            throw new RuntimeException("Count not start scheduler client");
        }
    }

    @Override
    public void close(){
        zookeeperClient.close();
    }

    @Override
    public String scheduleTask(BackgroundTask task, String createdBy, Date runAt, long period, JSONObject configuration) {
        Boolean recurring = period > 0;

        String id = storage.newState(task.getClass().getName(), createdBy, runAt, recurring, period, configuration);
        TaskState state = new TaskState(task.getClass().getName())
                .creator(createdBy)
                .runAt(runAt)
                .isRecurring(recurring)
                .interval(period)
                .configuration(configuration);

        producer.send(new ProducerRecord<>(NEW_TASKS_TOPIC, id, state.serialize()));
        producer.flush();

        return id;
    }

    @Override
    public TaskManager stopTask(String id, String requesterName) {
        throw new UnsupportedOperationException(this.getClass().getName()+" currently doesnt support stopping tasks");
        return null;
    }

    @Override
    public StateStorage storage() {
        return storage;
    }
}
