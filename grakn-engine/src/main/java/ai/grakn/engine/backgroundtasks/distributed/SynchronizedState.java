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

import ai.grakn.engine.backgroundtasks.TaskState;
import ai.grakn.engine.backgroundtasks.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static ai.grakn.engine.backgroundtasks.distributed.DistributedTaskManager.zookeeperClient;
import static ai.grakn.engine.backgroundtasks.distributed.ZookeeperConfig.STATE_PATH;
import static ai.grakn.engine.backgroundtasks.distributed.ZookeeperConfig.EXECUTOR_PATH;
import static java.lang.String.format;

/**
 * State to be stored in Zookeeper
 */
public class SynchronizedState {

    private static final Logger LOG = LoggerFactory.getLogger(SynchronizedState.class);

    private String taskId;
    private TaskStatus state;
    private String acceptedExecutor;

    public SynchronizedState(String taskId, String executor, TaskStatus state){
        this.taskId = taskId;
        this.acceptedExecutor = executor;
        this.state = state;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskState getState() {
        return state;
    }

    public String getAcceptedExecutor() {
        return acceptedExecutor;
    }

    public void setAcceptedExecutor(String acceptedExecutor) {
        this.acceptedExecutor = acceptedExecutor;
    }

    /**
     *
     */
    public void storeState(){

    }

    /**
     * Return a SynchronizedState representing this state stored in zookeeper
     * @param id id of tasks to retrieve
     * @return SynchrionizedState for given ID
     */
    public static Optional<SynchronizedState> getStateFromZookeeper(String id){

        try {
            String executor = getStringFromZookeeper(format(EXECUTOR_PATH, id));
//            TaskStatus state = getStringFromZookeeper(format(STATE_PATH, id));

            return Optional.of(new SynchronizedState(id, executor, null));
        } catch (Exception e){
            LOG.error("Could to retrieve state of " + id);
        }

        return Optional.empty();
    }

    private static String getStringFromZookeeper(String path) throws Exception {
        return new String(zookeeperClient.getData().forPath(path));
    }
}
