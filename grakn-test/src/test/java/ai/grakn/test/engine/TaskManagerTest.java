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

package ai.grakn.test.engine;

import ai.grakn.engine.backgroundtasks.StateStorage;
import ai.grakn.engine.backgroundtasks.TaskManager;
import ai.grakn.engine.backgroundtasks.distributed.DistributedTaskManager;
import ai.grakn.test.AbstractEngineTest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import static ai.grakn.engine.backgroundtasks.TaskStatus.COMPLETED;
import static ai.grakn.engine.backgroundtasks.TaskStatus.CREATED;
import static ai.grakn.engine.backgroundtasks.TaskStatus.RUNNING;
import static ai.grakn.engine.backgroundtasks.TaskStatus.SCHEDULED;
import static java.lang.System.currentTimeMillis;

public class TaskManagerTest extends AbstractEngineTest {

    private TaskManager manager;

    @Before
    public void setup(){
        manager = new DistributedTaskManager();
    }


    private void waitToFinish(String id) {
        StateStorage storage = manager.storage();
        final long initial = new Date().getTime();

        while ((new Date().getTime())-initial < 10000) {
            System.out.println("created: "+storage.getTasks(CREATED, null, null, 0, 0).size());
            System.out.println("scheduled: "+storage.getTasks(SCHEDULED, null, null,0, 0).size());
            System.out.println("completed: "+storage.getTasks(COMPLETED, null, null,0, 0).size());
            System.out.println("running: "+storage.getTasks(RUNNING, null, null,0, 0).size());

            if (storage.getState(id).status() == COMPLETED)
                break;

            try {
                Thread.sleep(5000);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void endToEndTest(){
        Collection<String> ids = new HashSet<>();

        for(int i = 0; i <= 20; i++){

            JSONObject config = new JSONObject().put("name", "task "+Integer.toString(i));
            
            TestTask task = new TestTask();
            String taskId = manager.scheduleTask(task, TaskManagerTest.class.getName(),
                    new Date(currentTimeMillis()), 0, config);

            ids.add(taskId);
        }

        ids.forEach(this::waitToFinish);
    }
}
