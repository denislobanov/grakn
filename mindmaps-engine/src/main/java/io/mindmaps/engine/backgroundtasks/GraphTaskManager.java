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

import io.mindmaps.Mindmaps;
import io.mindmaps.MindmapsGraph;
import io.mindmaps.MindmapsGraphFactory;
import io.mindmaps.engine.util.ConfigProperties;
import io.mindmaps.factory.GraphFactory;
import io.mindmaps.graql.Graql;
import io.mindmaps.graql.QueryBuilder;
import io.mindmaps.graql.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static io.mindmaps.graql.Graql.var;

public class GraphTaskManager implements TaskManager {
    private final static String SYSTEM_KEYSPACE = "system-keyspace";
    private final static String SINGLE_HOST_IMPLEMENTATION = "single.host.implementation";
    private final Logger LOG = LoggerFactory.getLogger(GraphTaskManager.class);

    private MindmapsGraph graph;
    private Map<UUID, ScheduledFuture<BackgroundTask>> taskFutures;
    private ScheduledExecutorService executorService;

    public GraphTaskManager() {
        graph = GraphFactory.getInstance().getGraph(SYSTEM_KEYSPACE);
        taskFutures = new ConcurrentHashMap<>();

        ConfigProperties properties = ConfigProperties.getInstance();
        // One thread is reserved for the supervisor
        executorService = Executors.newScheduledThreadPool(properties.getAvailableThreads()-1);
        //FIXME: get from config
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::supervisor, 2000, 1000, TimeUnit.MILLISECONDS);
    }

    public UUID scheduleTask(BackgroundTask task, long delay) {
        TaskState state = new TaskState(task.getClass().getName())
                .setRecurring(false)
                .setDelay(delay);

        UUID uuid = saveNewState(state);



    }

    /*
    Internal Methods
     */
    private UUID saveNewState(TaskState state) {
        QueryBuilder queryBuilder = Graql.withGraph(graph);

        Var stateVar = var().isa("scheduled-task")
                            .has("delay", state.getDelay())
                            .has("interval", state.getInterval())
                            .has("status-change-time", state.getStatusChangeTime())
                            .has("status-change-by", state.getStatusChangedBy())
                            .has("status-change-message", state.getStatusChangeMessage())
                            .has("queued-time", state.getQueuedTime())
                            .has("created-by", state.getCreator())
                            .has("task-name", state.getName());

        queryBuilder.insert(stateVar,
                            var().rel("status-of-task-owner", stateVar)
                                 .rel("status-of-task-value", var().isa("task-status")
                                                                   .has("task-status-value", state.getStatus().toString()))
                                 .isa("status-of-task"),
                            var().rel("task-executing-hostname-owner", stateVar)
                                 .rel("task-executing-hostname-value", var().isa("executing-hostname")
                                                                            .has("task-executing-hostname-value", SINGLE_HOST_IMPLEMENTATION))
                                 .isa("task-executing-hostname"),
                            var().rel("task-recurrence-owner", stateVar)
                                 .rel("task-recurrence-value", var().isa("task-is-recurring"))
                                 .isa("task-recurrence"))
                    .execute();

    }



    private void executeSingle(UUID uuid, BackgroundTask task, long delay) {
        ScheduledFuture<BackgroundTask> f = (ScheduledFuture<BackgroundTask>) executorService.schedule(
                runTask(uuid, task::start), delay, TimeUnit.MILLISECONDS);
        taskStorage.put(uuid, f);
    }

    private void executeRecurring(UUID uuid, BackgroundTask task, long delay, long interval) {
        ScheduledFuture<BackgroundTask> f = (ScheduledFuture<BackgroundTask>) executorService.scheduleAtFixedRate(
                runTask(uuid, task::start), delay, interval, TimeUnit.MILLISECONDS);
        taskStorage.put(uuid, f);
    }


    private void supervisor() {}

}
