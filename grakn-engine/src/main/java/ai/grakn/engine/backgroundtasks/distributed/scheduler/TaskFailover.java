/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Ltd
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.engine.backgroundtasks.distributed.scheduler;

import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.distributed.GraknStateStorage;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONArray;

import java.util.Map;

import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.WORK_QUEUE_TOPIC;
import static ai.grakn.engine.backgroundtasks.distributed.kafka.KafkaConfig.workQueueProducer;

public class TaskFailover implements TreeCacheListener {
    public static final String WATCH_PATH = "/runners/watch";
    public static final String TASK_RUNNER_STATE_PATH = "/runners/last_state";

    private Map<String, ChildData> current;
    private TreeCache cache;
    private KafkaProducer<String, String> producer;
    private StateStorage stateStorage;

    public TaskFailover(TreeCache cache) {
        this.cache = cache;
        current = cache.getCurrentChildren(WATCH_PATH);

        producer = new KafkaProducer<>(workQueueProducer());
        stateStorage = new GraknStateStorage();
    }

    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        Map<String, ChildData> nodes = cache.getCurrentChildren(WATCH_PATH);

        switch (event.getType()) {
            case NODE_ADDED:
                current = nodes;
                break;
            case NODE_REMOVED:
                failover(client, nodes);
                current = nodes;
                break;
            default:
                break;
        }
    }

    private void failover(CuratorFramework client, Map<String, ChildData> nodes) throws Exception {
        for(String name: current.keySet()) {
            // Dead TaskRunner
            if(!nodes.containsKey(name)) {
                reQueue(client, name);
            }
        }
    }

    private void reQueue(CuratorFramework client, String name) throws Exception {
        // Get list of task last processed by this TaskRunner
        byte[] b = client.getData().forPath(TASK_RUNNER_STATE_PATH + "/" + name);

        // Re-queue all of the IDs
        JSONArray ids = new JSONArray(new String(b));
        for(Object o: ids) {
            String id = (String)o;

            String configuration = stateStorage.getState(id)
                                               .configuration()
                                               .toString();
            producer.send(new ProducerRecord<>(WORK_QUEUE_TOPIC, id, configuration));
        }
    }
}
