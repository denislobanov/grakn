/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Ltd
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
import ai.grakn.engine.backgroundtasks.distributed.ZooKeeperStateStorage;
import javafx.util.Pair;
import org.apache.curator.test.TestingServer;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ai.grakn.engine.backgroundtasks.TaskStatus.CREATED;
import static ai.grakn.engine.backgroundtasks.TaskStatus.SCHEDULED;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ZooKeeperStateStorageTest extends GraknEngineTestBase {
    private ZooKeeperStateStorage stateStorage;
    private TestingServer testingServer;

    @Before
    public void setUp() throws Exception {
        testingServer = new TestingServer(2181, true);
        stateStorage = new ZooKeeperStateStorage();
    }

    @After
    public void tearDown() throws IOException {
        testingServer.stop();
    }

    @Test
    public void testStoreRetrieve() {
        Date runAt = new Date();
        JSONObject configuration = new JSONObject().put("test key", "test value");

        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(runAt)
                .isRecurring(false)
                .interval(0)
                .configuration(configuration);

        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        // Retrieve
        TaskState state = stateStorage.getState(id);
        assertNotNull(state);

        assertEquals(TestTask.class.getName(), state.taskClassName());
        assertEquals(this.getClass().getName(), state.creator());
        assertEquals(runAt, state.runAt());
        assertFalse(state.isRecurring());
        assertEquals(0, state.interval());
        assertEquals(configuration.toString(), state.configuration().toString());
    }

    @Test
    public void testUpdate() {
        Date runAt = new Date();
        JSONObject configuration = new JSONObject().put("key", "test value");

        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .statusChangedBy(this.getClass().getName())
                .runAt(runAt)
                .isRecurring(false)
                .interval(0)
                .configuration(configuration);

        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        // Get current values
        TaskState state = stateStorage.getState(id);

        // Change.
        stateStorage.updateState(id, SCHEDULED, "bla", "example.com", new UnsupportedOperationException(), "blabla", null);

        TaskState newState = stateStorage.getState(id);

        assertNotEquals("the object itself", state, newState);
        assertNotEquals("status", state.status(), newState.status());
        assertNotEquals("status changed by", state.statusChangedBy(), newState.statusChangedBy());
        assertNotEquals("hostname", state.executingHostname(), newState.executingHostname());
        assertNotEquals("stack trace", state.stackTrace(), newState.stackTrace());
        assertNotEquals("checkpoint", state.checkpoint(), newState.checkpoint());
        assertEquals("configuration", state.configuration().toString(), newState.configuration().toString());
    }

    @Test
    public void testUpdateInvalid() {
        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .isRecurring(false)
                .interval(0);

        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        stateStorage.updateState(null, SCHEDULED, "bla", "example.com", new UnsupportedOperationException(), "blabla", null);
        TaskState state = stateStorage.getState(id);
        assertEquals(CREATED, state.status());

        stateStorage.updateState(id, null, null, null, null, null, null);
        state = stateStorage.getState(id);
        assertEquals(CREATED, state.status());
    }

    @Test
    public void testGetByStatus() {
        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(new Date())
                .isRecurring(false)
                .interval(0);
        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        Set<Pair<String, TaskState>> res = stateStorage.getTasks(CREATED, null, null, 0, 0);
        assertTrue(res.parallelStream()
                        .map(Pair::getKey)
                        .filter(x -> x.equals(id))
                        .collect(Collectors.toList())
                        .size() == 1);
    }

    @Test
    public void testGetByCreator() {
        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(new Date())
                .isRecurring(false)
                .interval(0);
        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        Set<Pair<String, TaskState>> res = stateStorage.getTasks(null, null, this.getClass().getName(), 0, 0);
        assertTrue(res.parallelStream()
                        .map(Pair::getKey)
                        .filter(x -> x.equals(id))
                        .collect(Collectors.toList())
                        .size() == 1);
    }

    @Test
    public void testGetByClassName() {
        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(new Date())
                .isRecurring(false)
                .interval(0);
        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        Set<Pair<String, TaskState>> res = stateStorage.getTasks(null, TestTask.class.getName(), null, 0, 0);
        assertTrue(res.parallelStream()
                        .map(Pair::getKey)
                        .filter(x -> x.equals(id))
                        .collect(Collectors.toList())
                        .size() == 1);
    }

    @Test
    public void testGetAll() {
        TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(new Date())
                .isRecurring(false)
                .interval(0);
        String id = UUID.randomUUID().toString();
        stateStorage.saveExisting(id, saveState);

        Set<Pair<String, TaskState>> res = stateStorage.getTasks(null, null, null, 0, 0);
        assertTrue(res.parallelStream()
                        .map(Pair::getKey)
                        .filter(x -> x.equals(id))
                        .collect(Collectors.toList())
                        .size() == 1);
    }

    @Test
    public void testPagination() {
        for (int i = 0; i < 20; i++) {
            TaskState saveState = new TaskState(TestTask.class.getName())
                .creator(this.getClass().getName())
                .runAt(new Date())
                .isRecurring(false)
                .interval(0);
            stateStorage.saveExisting(UUID.randomUUID().toString(), saveState);
        }

        Set<Pair<String, TaskState>> setA = stateStorage.getTasks(null, null, null, 10, 0);
        Set<Pair<String, TaskState>> setB = stateStorage.getTasks(null, null, null, 10, 10);

        setA.forEach(x -> assertFalse(setB.contains(x)));
    }
}
