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
import ai.grakn.exception.GraknValidationException;
import ai.grakn.factory.GraphFactory;
import ai.grakn.graql.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static ai.grakn.graql.Graql.var;

public class GraknTaskStateStorage implements TaskStateStorage {
    private final static String SYSTEM_KEYSPACE = "grakn-system";

    private final Logger LOG = LoggerFactory.getLogger(InGraphTaskManager.class);
    private GraknGraph graph;

    public GraknTaskStateStorage {
        graph = GraphFactory.getInstance().getGraph(SYSTEM_KEYSPACE);
    }

    public String newState(String taskName, String createdBy, Date runAt, Boolean recurring, long interval, String custom) {
        if(taskName == null || createdBy == null || runAt == null || recurring == null)
            return null;

        Var state = var("task").isa("scheduled-task")
                                      .has("task-name", taskName)
                                      .has("created-by", createdBy)
                                      .has("run-at", runAt.getTime())
                                      .has("recurring", recurring)
                                      .has("recur-interval", interval);

        if(custom != null)
            state.has("custom-state", custom);

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
                            Throwable failure, String custom) {
        if (id == null || status == null)
            return;

        graph.graql().
    }

}
