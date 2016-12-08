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

package ai.grakn.test.engine.backgroundtasks;

import ai.grakn.engine.backgroundtasks.distributed.Scheduler;
import ai.grakn.engine.backgroundtasks.distributed.ClusterManager;
import ai.grakn.test.AbstractEngineTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class ClusterManagerTest extends AbstractEngineTest {

    @Test
    public void testSchedulerRestartsAfterKilled() throws Exception {
        ClusterManager clusterManager = ClusterManager.getInstance();

        Scheduler scheduler1 = clusterManager.getScheduler();

        // Kill scheduler- client should create a new one
        scheduler1.close();
        Thread.sleep(3000);

        Scheduler scheduler2 = clusterManager.getScheduler();
        assertNotEquals(scheduler1, scheduler2);
    }
}
