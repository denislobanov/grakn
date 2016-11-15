/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
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

package ai.grakn.engine.backgroundtasks;

import java.util.Date;
import java.util.Set;

public interface TaskStateStorage {
    /**
     * Create a new task state and store it, returning an ID to later access this task state.
     * @param taskName String class name of object implementing the BackgroundTask interface. This must not be null.
     * @param createdBy String of who is creating this new state. This must not be null.
     * @param runAt Date when should this task be executed. This must not be null.
     * @param recurring Boolean marking if this task should be run again after it has finished executing successfully.
     *                  This must not be null.
     * @param interval If a task is marked as recurring, this represents the time delay between the next executing of this task.
     *                 This must not be null.
     * @return String form of the task id, which can be use later to update or retrieve the task state. Null if task could
     * not be created of mandatory fields were omitted.
     */
    String newState(String taskName,
                    String createdBy,
                    Date runAt,
                    Boolean recurring,
                    long interval);

    /**
     * Used to update task state.
     * @param id ID of task to update, this must not be null.
     * @param status New status of task. This must not be null.
     * @param statusChangeBy String identifying caller, may be null.
     * @param executingHostname String hostname of engine instance scheduling/executing this task. May be null.
     * @param failure Throwable to store any exceptions that occurred during executing. May be null.
     * @param checkpoint String to store task checkpoint, may be null.
     */
    void updateState(String id,
                     TaskStatus status,
                     String statusChangeBy,
                     String executingHostname,
                     Throwable failure,
                     String checkpoint);

    /**
     * This is a copy of the internal TaskState object. It is guaranteed to be correct at the time of call, however the actual
     * internal state may change at any time after.
     * @param id String id of task.
     * @return TaskState object or null if no TaskState with this id could be found.
     */
    TaskState getState(String id);

    /**
     * Returns a Set of all tasks in the system - this includes Completed, Running, Dead, etc.
     * @return Set<String> of task IDs
     */
    Set<String> getAllTasks();

    /**
     * Return a Set of all tasks with a matching @TaskStatus.
     * Example:
     *  // Return all tasks which failed to complete execution.
     *  Set<String> failedTasks = myTaskStorage.getTasks(TaskStatus.STOPPED);
     *
     * @param taskStatus See TaskStatus enum.
     * @return Set<String> of task IDs matching the given @taskStatus.
     */
    Set<String> getTasks(TaskStatus taskStatus);
}
