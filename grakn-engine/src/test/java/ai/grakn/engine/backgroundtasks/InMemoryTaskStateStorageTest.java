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

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static ai.grakn.engine.backgroundtasks.TaskStatus.SCHEDULED;
import static org.junit.Assert.*;

public class InMemoryTaskStateStorageTest {
    private TaskStateStorage taskStateStorage;

    @Before
    public void setUp() {
        taskStateStorage = InMemoryTaskStateStorage.getInstance();
    }

    @Test
    public void testNewState() {
        TestTask task = new TestTask();

        Date runAt = new Date();
        String custom = "blablablalba";
        String id = taskStateStorage.newState(task.getClass().getName(), this.getClass().getName(), runAt, false, 0, custom);
        assertNotNull(id);

        TaskState state = taskStateStorage.getState(id);
        assertEquals("name", task.getClass().getName(), state.taskClassName());
        assertEquals("creator", this.getClass().getName(), state.creator());
        assertEquals("runAt", runAt, state.runAt());
        assertEquals("recurring", false, state.isRecurring());
        assertEquals("interval", 0, state.interval());
        assertEquals("custom", custom, state.customState());
    }

    @Test
    public void testUpdateState() {
        TestTask task = new TestTask();
        Date runAt = new Date();

        String id = taskStateStorage.newState(task.getClass().getName(), this.getClass().getName(), runAt, false, 0, null);
        assertNotNull(id);

        // Get current values
        TaskState state = taskStateStorage.getState(id);
        TaskStatus status = state.status();
        String changedBy = state.statusChangedBy();
        String executingHostname = state.executingHostname();
        Throwable failure = state.failure();
        String custom = state.customState();

        // Change.
        taskStateStorage.updateState(id, SCHEDULED, "bla", "example.com", new UnsupportedOperationException(), "blabla");

        TaskState newState = taskStateStorage.getState(id);
        assertNotEquals("the object itself", state, newState);
        assertNotEquals("status", state.status(), newState.status());
        assertNotEquals("status changed by", state.statusChangedBy(), newState.statusChangedBy());
        assertNotEquals("hostname", state.executingHostname(), newState.executingHostname());
        assertNotEquals("isFailure", state.isFailed(), newState.isFailed());
        assertNotEquals("failure (throwable)", state.failure(), newState.failure());
        assertNotEquals("custorm", state.customState(), newState.customState());
    }
}
