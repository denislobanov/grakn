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

public interface TaskManager {
    /**
     * Schedule a single shot/one off BackgroundTask to run after a @delay in milliseconds.
     * @param task Any object implementing the BackgroundTask interface that is to be scheduled for later execution.
     * @param runAt Date when task should run.
     * @return Assigned ID of task scheduled for later execution.
     */
    String scheduleTask(BackgroundTask task, Date runAt);

    /**
     * Schedule a task for recurring execution at every @period interval and after an initial @delay.
     * @param task Any object implementing the BackgroundTask interface that is to be scheduled for later execution.
     * @param runAt Date when task should run.
     * @param period Long interval between subsequent calls to @task.start().
     * @return Assigned ID of task scheduled for later execution.
     */
    String scheduleRecurringTask(BackgroundTask task, Date runAt, long period);

    /**
     * Stop a Scheduled, Paused or Running task. Task's .stop() method will be called to perform any cleanup and the
     * task is killed afterwards.
     * @param id String of task to stop.
     * @param requesterName Optional String to denote who requested this call; used for status reporting and may be null.
     * @return Instance of the class implementing TaskManager.
     */
    TaskManager stopTask(String id, String requesterName);

    /**
     * Return a full copy of a stored TaskState. This may be the state of a task yet to be scheduled or completed, etc.
     * The state is only guaranteed to be accurate at the time of copying and may change at a time after that, the returned
     * TaskState object will not reflect this.
     * @param id String ID of task.
     * @return A deep copy of the stored TaskState object.
     */
    TaskState getState(String id);

    /**
     * Returns a Set of all tasks in the system - this includes Completed, Running, Dead, etc.
     * @return Set<> of task String's
     */
    Set<String> getAllTasks();

    /**
     * Return a Set of all tasks with a matching @TaskStatus.
     * Example:
     *  // Return all tasks which failed to complete execution.
     *  Set<String> failedTasks = myTaskManager.getTasks(TaskStatus.STOPPED);
     *
     * @param taskStatus See TaskStatus enum.
     * @return Set<> of task String's matching the given @taskStatus.
     */
    Set<String> getTasks(TaskStatus taskStatus);
}
