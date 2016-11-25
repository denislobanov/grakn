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

import ai.grakn.engine.backgroundtasks.TaskState;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ai.grakn.engine.backgroundtasks.TaskStatus.SCHEDULED;

import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.NEW_TASKS_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.POLL_FREQUENCY;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.WORK_QUEUE_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueConsumer;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;

//TODO read recurring tasks from the graph on init
//TODO persist state to zookeeper and graph
//TODO polling task runners to see who is alive and pickup their tasks
/**
 * Handle execution of recurring tasks.
 * Monitor new tasks queue to add them to ScheduledExecutorService.
 * ScheduledExecutorService will be given a function to add the task in question to the work queue.
 */
public class Scheduler implements Runnable {

    private boolean running = true;
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
        while(running) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(POLL_FREQUENCY);

                for (ConsumerRecord record : records) {
                    String taskId = record.key().toString();
                    TaskState task = TaskState.deserialize(record.value().toString());

                    scheduleTask(taskId, task);
                }

                Thread.sleep(500);
            } catch (InterruptedException e){
                e.printStackTrace();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        consumer.close();
    }

    public void setRunning(boolean running){
       this.running = running;
    }

    //TODO update status in zookeeper and graph
    /**
     * Schedule a task to be submitted to the work queue when it is supposed to be run
     * @param taskId id of the task to be scheduled
     * @param task task to be scheduled
     */
    private void scheduleTask(String taskId, TaskState task){
        task.status(SCHEDULED);

        Runnable submit = () -> submitToWorkQueue(taskId, task);
        long delay = new Date().getTime() - task.runAt().getTime();

        if(task.isRecurring()) {
            schedulingService.scheduleAtFixedRate(submit, delay, task.interval(), MILLISECONDS);
        } else {
            schedulingService.schedule(submit, delay, MILLISECONDS);
        }
    }

    /**
     * Submit a task to the work queue
     * @param taskId id of the task to be submitted
     * @param task task to be submitted
     */
    private void submitToWorkQueue(String taskId, TaskState task){
        System.out.println("Scheduled " + taskId);
        producer.send(new ProducerRecord<>(WORK_QUEUE_TOPIC, taskId, task.serialize()));
    }
}

