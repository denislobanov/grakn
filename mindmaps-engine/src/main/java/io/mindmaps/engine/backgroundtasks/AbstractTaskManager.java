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

package io.mindmaps.engine.backgroundtasks;

import java.util.Date;
import java.util.UUID;

public abstract class AbstractTaskManager implements TaskManager {
    private static String STATUS_MESSAGE_SCHEDULED = "Task scheduled.";

    public UUID scheduleTask(BackgroundTask task, long delay) {
        TaskState state = new TaskState(task.getClass().getName())
                .setRecurring(false)
                .setDelay(delay)
                .setStatus(TaskStatus.SCHEDULED)
                .setStatusChangeMessage(STATUS_MESSAGE_SCHEDULED)
                .setStatusChangedBy(InMemoryTaskManager.class.getName())
                .setQueuedTime(new Date());

        UUID uuid = saveNewState(state);
        executeSingle(uuid, task, delay);
        return uuid;
    }

    public UUID scheduleRecurringTask(BackgroundTask task, long delay, long interval) {
        TaskState state = new TaskState(task.getClass().getName())
                .setRecurring(true)
                .setDelay(delay)
                .setInterval(interval);

        UUID uuid = saveNewState(state);
        executeRecurring(uuid, task, delay, interval);
        return uuid;
    }





    protected abstract UUID saveNewState(TaskState state);
    protected abstract void executeSingle(UUID uuid, BackgroundTask task, long delay);
    protected abstract void executeRecurring(UUID uuid, BackgroundTask task, long delay, long interval);
}
