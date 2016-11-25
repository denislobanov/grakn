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

import ai.grakn.engine.backgroundtasks.TaskStatus;

/**
 * State to be stored in Zookeeper
 */
public class SynchronizedState {
    private String taskId;
    private TaskStatus status;
    private String executingHostname;

    public SynchronizedState taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String taskId() {
        return taskId;
    }

    public SynchronizedState status(TaskStatus status) {
        this.status = status;
        return this;
    }

    public TaskStatus status() {
        return status;
    }

    public SynchronizedState executingHostname(String executingHostname) {
        this.executingHostname = executingHostname;
        return this;
    }

    public String executingHostname() {
        return executingHostname;
    }
}
