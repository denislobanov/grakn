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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskStateStorage implements TaskStateStorage {
    private static InMemoryTaskStateStorage instance = null;

    private Map<String, TaskState> storage;

    private InMemoryTaskStateStorage() {
        storage = new ConcurrentHashMap<>();
    }

    public static synchronized InMemoryTaskStateStorage getInstance() {
        if(instance == null)
            instance = new InMemoryTaskStateStorage();
        return instance;
    }

    public String newState(String taskName, String createdBy, Date runAt, Boolean recurring, long interval) {
        if(taskName == null || createdBy == null || runAt == null || recurring == null)
            return null;

        TaskState state = new TaskState(taskName);
        state.creator(createdBy)
             .runAt(runAt)
             .isRecurring(recurring)
             .interval(interval);

        String id = UUID.randomUUID().toString();
        storage.put(id, state);

        return id;
    }

    public void updateState(String id, TaskStatus status, String statusChangeBy, String executingHostname,
                            Throwable failure, String checkpoint) {
        if(id == null || status == null)
            return;

        TaskState state = storage.get(id);
        synchronized (state) {
            state.status(status)
                 .statusChangedBy(statusChangeBy)
                 .executingHostname(executingHostname)
                 .failure(failure)
                 .checkpoint(checkpoint);
        }
    }

    public TaskState getState(String id) {
        if(id == null || !storage.containsKey(id))
            return null;

        TaskState state = storage.get(id);
        TaskState newState = null;

        synchronized (state) {
            try {
                newState = state.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        return newState;
    }

    public Set<String> getAllTasks() {
        return storage.keySet();
    }

    public Set<String> getTasks(TaskStatus taskStatus) {
        return storage.entrySet().stream()
                      .filter(x -> x.getValue().status() == taskStatus)
                      .map(Map.Entry::getKey)
                      .collect(Collectors.toSet());
    }
}
