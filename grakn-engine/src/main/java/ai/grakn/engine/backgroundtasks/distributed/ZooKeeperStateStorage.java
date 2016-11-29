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

package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.TaskState;
import ai.grakn.engine.backgroundtasks.TaskStatus;
import ai.grakn.engine.backgroundtasks.distributed.zookeeper.ZookeeperConfig;
import javafx.util.Pair;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.json.JSONObject;

import java.util.*;

public class ZooKeeperStateStorage implements StateStorage {
    public static final String PATH_PREFIX = "/tasks/";
    private static final String TASKSTATE_SUFFIX = "/state";

    private CuratorFramework client;

    public ZooKeeperStateStorage() throws Exception {
        client = CuratorFrameworkFactory.newClient(ZookeeperConfig.ZOOKEEPER_URL, new ExponentialBackoffRetry(1000, 3));
        client.start();
        client.blockUntilConnected();
    }

    public String newState(String taskName, String createdBy, Date runAt, Boolean recurring, long interval, JSONObject configuration) {
        throw new UnsupportedOperationException(this.getClass().getName()+" Cannot be used to create brand new TaskStates");
    }

    public void saveExisting(String id, TaskState state) {
        if(id == null || state == null)
            return;

        try {
            client.create()
                  .creatingParentContainersIfNeeded()
                  .forPath(PATH_PREFIX+id+TASKSTATE_SUFFIX, state.serialize().getBytes());
        }
        catch (Exception e) {
            System.out.println(this.getClass().getName() + " Could not write to ZooKeeper! " + e);
        }
    }

    public Boolean updateState(String id, TaskStatus status, String statusChangeBy, String executingHostname,
                               Throwable failure, String checkpoint, JSONObject configuration) {
        if(id == null)
            return false;

        if(status == null && statusChangeBy == null && executingHostname == null && failure == null
                && checkpoint == null && configuration == null)
            return false;

        try {
            TaskState state = getState(id);
            if(state == null)
                return false;

            // Update values
            if (status != null)
                state.status(status);
            if (statusChangeBy != null)
                state.statusChangedBy(statusChangeBy);
            if (executingHostname != null)
                state.executingHostname(executingHostname);
            if (failure != null) {
                state.exception(failure.getMessage())
                        .stackTrace(Arrays.toString(failure.getStackTrace()));
            }
            if (checkpoint != null)
                state.checkpoint(checkpoint);
            if (configuration != null)
                state.configuration(configuration);

            // Save to ZK
            client.setData().forPath(PATH_PREFIX + id + TASKSTATE_SUFFIX, state.serialize().getBytes());
        }
        catch (Exception e) {
            System.out.println(this.getClass().getName()+" Could not write to ZooKeeper! - "+e);
            return false;
        }

        return true;
    }

    public TaskState getState(String id) {
        if (id == null)
            return null;

        TaskState state = null;
        try {
            byte[] b = client.getData().forPath(PATH_PREFIX + id + TASKSTATE_SUFFIX);
            state = TaskState.deserialize(new String(b));
        }
        catch (Exception e) {
            System.out.println(this.getClass().getName()+" Could not read from ZooKeeper! "+e);
        }

        return state;
    }

    public Set<Pair<String, TaskState>> getTasks(TaskStatus taskStatus, String taskClassName, String createdBy, int limit, int offset) {
        Set<Pair<String, TaskState>> res = new HashSet<>();

        try {
            List<String> tasks = client.getChildren().forPath("/tasks");

            int count = 0;
            for(String id: tasks) {
                byte[] b = client.getData().forPath(PATH_PREFIX+id+TASKSTATE_SUFFIX);
                TaskState state = TaskState.deserialize(new String(b));

                // AND
                if(taskStatus != null && state.status() != taskStatus)
                    continue;
                if(taskClassName != null && !Objects.equals(state.taskClassName(), taskClassName))
                    continue;
                if(createdBy != null && !Objects.equals(state.creator(), createdBy))
                    continue;

                if(count < offset) {
                    count++;
                    continue;
                }
                else if(limit > 0 && count >= (limit+offset)) {
                    break;
                }
                count++;

                res.add(new Pair<>(id, state));
            }

        }
        catch (Exception e) {
            System.out.println(e);
        }

        return res;
    }
}
