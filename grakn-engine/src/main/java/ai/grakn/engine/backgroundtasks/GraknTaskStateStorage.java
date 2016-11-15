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

package ai.grakn.engine.backgroundtasks;

import ai.grakn.GraknGraph;
import ai.grakn.concept.Concept;
import ai.grakn.concept.Instance;
import ai.grakn.concept.Resource;
import ai.grakn.exception.GraknValidationException;
import ai.grakn.factory.GraphFactory;
import ai.grakn.graql.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.function.Function;

import static ai.grakn.graql.Graql.var;

public class GraknTaskStateStorage implements TaskStateStorage {
    private final static String SYSTEM_KEYSPACE = "grakn-system";
    private final static String TASK_VAR = "task";

    private final Logger LOG = LoggerFactory.getLogger(InGraphTaskManager.class);
    private GraknGraph graph;

    public GraknTaskStateStorage() {
        graph = GraphFactory.getInstance().getGraph(SYSTEM_KEYSPACE);
    }

    public String newState(String taskName, String createdBy, Date runAt, Boolean recurring, long interval) {
        if(taskName == null || createdBy == null || runAt == null || recurring == null)
            return null;

        Var state = var("task").isa("scheduled-task")
                                      .has("task-name", taskName)
                                      .has("created-by", createdBy)
                                      .has("run-at", runAt.getTime())
                                      .has("recurring", recurring)
                                      .has("recur-interval", interval);

        graph.graql().insert(state).execute();

        try {
            graph.commit();
        } catch (GraknValidationException e) {
            LOG.error("Could not commit task to graph: "+e.getMessage());
            return null;
        }

        return graph.graql().match(state).execute()
                    .get(0).values()
                    .stream().findFirst()
                    .map(Concept::getId)
                    .orElse(null);
    }

    public void updateState(String id, TaskStatus status, String statusChangeBy, String executingHostname,
                            Throwable failure, String checkpoint) {
        if(id == null || status == null)
            return;

        // Existing resource relations to remove
        ArrayList<Var> deleters = new ArrayList<>();

        // New resources to add
        ArrayList<Var> resources = new ArrayList<>();
        resources.add(var(TASK_VAR).id(id));

        if(statusChangeBy != null) {
            deleters.add(var(TASK_VAR).has("status-change-by"));
            resources.add(var(TASK_VAR).has("status-change-by", statusChangeBy));
        }

        if(executingHostname != null) {
            deleters.add(var(TASK_VAR).has("executing-hostname"));
            resources.add(var(TASK_VAR).has("executing-hostname", executingHostname));
        }

        if(failure != null) {
            deleters.add(var(TASK_VAR).has("task-failure"));
            resources.add(var(TASK_VAR).has("task-failure", failure));
        }

        if(checkpoint != null) {
            deleters.add(var(TASK_VAR).has("task-checkpoint"));
            resources.add(var(TASK_VAR).has("task-checkpoint", checkpoint));
        }

        if(deleters.size() > 0) {
            graph.graql().match(var(TASK_VAR).id(id))
                    .delete(deleters)
                    .execute();

            graph.graql().insert(resources)
                    .execute();

            try {
                graph.commit();
            } catch(GraknValidationException e) {
                e.printStackTrace();
            }
        }
    }

    public TaskState getState(String id) {
        if(id == null)
            return null;

        Instance instance = graph.getInstance(id);
        if(instance == null)
            return null;

        return new TaskState(getResourceWith(instance, "task-class-name", Object::toString))
                .status(TaskStatus.valueOf(getResourceWith(instance, "task-status", Object::toString)))
                .statusChangeTime(new Date(getResourceWith(instance, "status-change-time", x -> (Long)x )))
                .statusChangedBy(getResourceWith(instance, "status-change-by", Object::toString))
                .creator(getResourceWith(instance, "created-by", Object::toString))
                .executingHostname(getResourceWith(instance, "executing-hostname", Object::toString))
                .runAt(new Date(getResourceWith(instance, "run-at", x -> Long.

    }


    private<T> T getResourceWith(Instance instance, String resourceType, Function<Object, T> transform) {
        return instance.asEntity()
                       .resources(graph.getResourceType(resourceType))
                       .stream().findFirst()
                       .map(x -> x.getValue()
                       .orElse(null);
    }
}
