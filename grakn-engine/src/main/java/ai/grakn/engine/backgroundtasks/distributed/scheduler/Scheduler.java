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

package ai.grakn.engine.backgroundtasks.distributed.scheduler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.POLL_FREQUENCY;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueConsumer;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;

/**
 *
 * Handle execution of recurring tasks.
 * Monitor new tasks queue to add them to ScheduledExecutorService.
 * ScheduledExecutorService will be given a function to add the task in question to the work queue.
 */
public class Scheduler implements Runnable {

    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;
    private ScheduledExecutorService schedulingService = Executors.newScheduledThreadPool(1);

    public Scheduler(){

        // Kafka listener
        consumer = new KafkaConsumer<>(workQueueConsumer());
        consumer.subscribe(Collections.singletonList(NEW_TASKS_TOPIC));

        // Kafka writer
        producer = new KafkaProducer<>(workQueueProducer());
    }

    /**
     *
     */
    public void run() {
        try {
            ConsumerRecords<String, String> records = consumer.poll(POLL_FREQUENCY);

            for(ConsumerRecord record:records){
                System.out.println(record);
            }
        } finally {
            consumer.close();
        }
    }
}

