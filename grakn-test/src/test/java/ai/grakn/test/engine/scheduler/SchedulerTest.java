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

package ai.grakn.test.engine.scheduler;

import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.TaskState;
import ai.grakn.engine.backgroundtasks.TaskStatus;
import ai.grakn.engine.backgroundtasks.distributed.GraknStateStorage;
import ai.grakn.engine.backgroundtasks.distributed.scheduler.Scheduler;
import ai.grakn.test.AbstractEngineTest;
import ai.grakn.test.engine.TestTask;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.POLL_FREQUENCY;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.WORK_QUEUE_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueConsumer;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;
import static ai.grakn.engine.backgroundtasks.TaskStatus.SCHEDULED;
import static junit.framework.Assert.assertEquals;

/**
 * Each test needs to be run with a clean Kafka to pass
 */
public class SchedulerTest extends AbstractEngineTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private StateStorage stateStorage = new GraknStateStorage();
    private Scheduler scheduler;
    private Future future;

    @Before
    public void initScheduler() throws Exception {
        scheduler =  new Scheduler();
        future = executor.submit(scheduler::run);
    }

    @Test
    public void testInstantaneousOneTimeTasks() throws Exception {
        Collection<String> tasks = addNewTasksToQueue(10);

        Thread.sleep(6000);

        System.out.println("setting false");
        scheduler.setRunning(false);

        checkScheduledInGraph(tasks);
        assertEquals(10, countMessagesInWorkQueue());
    }

    @Test
    public void testRecurringTasksAdded(){

    }

    @Test
    public void testRecurringTasksFromGraphOnStartup(){

    }


    private Collection<String> addNewTasksToQueue(int n) throws Exception {
        KafkaProducer<String, String> producer = new KafkaProducer<>(workQueueProducer());
        Collection<String> tasks = new HashSet<>();

        for(int i=0; i < n; i++) {

            String taskId = stateStorage.newState(TestTask.class.getName(),
                    SchedulerTest.class.getName(),
                    new Date(System.currentTimeMillis()),
                    false, 0, null);

            TaskState state = stateStorage.getState(taskId);

            producer.send(new ProducerRecord<>(NEW_TASKS_TOPIC, taskId, state.serialize()));
            producer.flush();

            System.out.println("Sent: " + state.serialize());
        }

        producer.close();
        return tasks;
    }

    private int countMessagesInWorkQueue(){
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(workQueueConsumer());
        consumer.subscribe(Collections.singletonList(WORK_QUEUE_TOPIC));

        ConsumerRecords<String, String> records = consumer.poll(POLL_FREQUENCY);
        consumer.close();
        return records.count();
    }

    private void checkScheduledInGraph(Collection<String> tasks){
        for(String id:tasks){
            TaskState state = stateStorage.getState(id);
            TaskStatus status = state.status();
            assertEquals(status, SCHEDULED);
        }
    }
}
