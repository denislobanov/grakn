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

import ai.grakn.engine.backgroundtasks.TaskState;
import ai.grakn.engine.backgroundtasks.TaskStatus;
import ai.grakn.engine.backgroundtasks.distributed.scheduler.Scheduler;
import ai.grakn.test.AbstractEngineTest;
import ai.grakn.test.engine.TestTask;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ai.grakn.engine.backgroundtasks.TaskStatus.CREATED;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueConsumer;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;

public class SchedulerTest extends AbstractEngineTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future future;

    @Before
    public void initScheduler() throws Exception {
        future = executor.submit(() -> new Scheduler().run());
    }

    @Test
    public void testInstantaneousOneTimeTasks() throws Exception {
        addNewTasksToQueue(10);
    }

    @Test
    public void testScheduledOneTimeTasks(){

    }

    @Test
    public void testRecurringTasksAdded(){

    }

    @Test
    public void testRecurringTasksFromGraphOnStartup(){

    }


    private void addNewTasksToQueue(int n) throws Exception {
        KafkaProducer<String, String> producer = new KafkaProducer<>(workQueueProducer());

        for(int i=0; i < n; i++) {

            TaskState state = new TaskState(TestTask.class.getName());
            state.runAt(new Date(System.currentTimeMillis()));

            System.out.println("Sending: " + state.serialize());

            producer.send(new ProducerRecord<>(NEW_TASKS_TOPIC, UUID.randomUUID().toString(), state.serialize()));
            producer.flush();
        }

        producer.close();

        while(!future.isDone()) {
            Thread.sleep(1000);
        }
    }
}
