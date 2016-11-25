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

import ai.grakn.engine.backgroundtasks.distributed.scheduler.Scheduler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ai.grakn.engine.backgroundtasks.TaskStatus.CREATED;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;

public class SchedulerTest {

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Before
    public void initScheduler(){
        executor.submit(() -> new Scheduler().run());
    }

    @Test
    public void testInstantaneousOneTimeTasks(){
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


    private void addNewTasksToQueue(int n){
        KafkaProducer<String, String> producer = new KafkaProducer<>(workQueueProducer());

        for(int i=0; i < n; i++) {

//            producer.send(new ProducerRecord<>(NEW_TASKS_TOPIC, message.toString()));
        }
    }
}
