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
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.json.JSONObject;

import java.util.Date;

import static ai.grakn.engine.backgroundtasks.distributed.ZookeeperConfig.ZOOKEEPER_URL;
import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;

/**
 * Class to manage tasks distributed using Kafka.
 */
public class DistributedTaskManager implements TaskManager, AutoCloseable {

    public static CuratorFramework zookeeperClient;

    /**
     * Instantiate connection with Zookeeper.
     */
    public DistributedTaskManager(){
        zookeeperClient = newClient(ZOOKEEPER_URL, new ExponentialBackoffRetry(1000, 0));
    }

    @Override
    public void close(){
        zookeeperClient.close();
    }

    @Override
    public String scheduleTask(BackgroundTask task, String createdBy, Date runAt, long period, JSONObject configuration) {
        return null;
    }

    @Override
    public TaskManager stopTask(String id, String requesterName) {
        return null;
    }

    @Override
    public StateStorage storage() {
        return null;
    }
}
