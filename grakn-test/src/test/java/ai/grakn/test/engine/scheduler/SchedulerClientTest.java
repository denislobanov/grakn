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

package ai.grakn.test.engine.scheduler;

import ai.grakn.engine.backgroundtasks.distributed.scheduler.Scheduler;
import ai.grakn.engine.backgroundtasks.distributed.scheduler.SchedulerClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.BeforeClass;
import org.junit.Test;

import static ai.grakn.engine.backgroundtasks.distributed.zookeeper.ZookeeperConfig.ZOOKEEPER_URL;
import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;
import static org.junit.Assert.assertNotEquals;

public class SchedulerClientTest {

    public static CuratorFramework zookeeperClient = newClient(ZOOKEEPER_URL, new ExponentialBackoffRetry(1000, 0));

    @BeforeClass
    public static void startZookeeperClient(){
        zookeeperClient.start();
    }

    @Test
    public void testSchedulerRestartsAfterKilled() throws Exception {
        SchedulerClient schedulerClient = new SchedulerClient(zookeeperClient);
        schedulerClient.start();

        Thread.sleep(3000);

        Scheduler scheduler1 = schedulerClient.getScheduler();

        // Kill scheduler- client should create a new one
        scheduler1.setRunning(false);
        Thread.sleep(3000);

        Scheduler scheduler2 = schedulerClient.getScheduler();
        assertNotEquals(scheduler1, scheduler2);
    }
}
