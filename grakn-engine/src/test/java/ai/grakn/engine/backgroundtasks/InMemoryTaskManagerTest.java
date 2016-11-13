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

import ai.grakn.engine.GraknEngineTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static ai.grakn.engine.backgroundtasks.TaskStatus.*;
import static org.junit.Assert.*;

public class InMemoryTaskManagerTest extends GraknEngineTestBase {
    private TaskManager taskManager;

    @Before
    public void setUp() {
        taskManager = InMemoryTaskManager.getInstance();
    }

    @Test
    public void testRunSingle() throws Exception {
        TestTask task = new TestTask();

        String id = taskManager.scheduleTask(task, this.getClass().getName(), new Date());
        // Wait for task to be executed.
        Thread.sleep(1000);

        assertEquals(COMPLETED, taskManager.storage().getState(id).status());
    }

    @Test
    public void testRunRecurring() throws Exception {
        TestTask task = new TestTask();

        String id = taskManager.scheduleRecurringTask(task, this.getClass().getName(), new Date(), 100);
        Thread.sleep(2000);

        assertTrue(task.getRunCount() > 1);

        // Stop task..
        taskManager.stopTask(id, null);
    }

    @Test
    public void testStopSingle() {
        TestTask task = new TestTask();
        String id = taskManager.scheduleTask(task, this.getClass().getName(), new Date());

        TaskStatus status = taskManager.storage().getState(id).status();
        assertTrue(status == SCHEDULED || status == RUNNING);

        taskManager.stopTask(id, this.getClass().getName());

        status = taskManager.storage().getState(id).status();
        assertTrue(status == STOPPED);
    }
}
