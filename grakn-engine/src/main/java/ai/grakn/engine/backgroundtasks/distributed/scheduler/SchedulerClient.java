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

package ai.grakn.engine.backgroundtasks.distributed.scheduler;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Scheduler will be constantly running on the "Leader" machine. The "takeLeadership"
 * function in this class will be called if it is needed to take over.
 */
public class SchedulerClient extends LeaderSelectorListenerAdapter implements Closeable {

    private final static Logger LOG = LoggerFactory.getLogger(SchedulerClient.class);

    private final String name;
    private final LeaderSelector leaderSelector;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SchedulerClient(CuratorFramework client, String path, String name) {
        this.name = name;

        leaderSelector = new LeaderSelector(client, path, this);
        leaderSelector.autoRequeue();
    }

    public void start() throws IOException {
        // the selection for this instance doesn't start until the leader selector is started
        // leader selection is done in the background so this call to leaderSelector.start() returns immediately
        leaderSelector.start();
    }

    public void close() throws IOException {
        leaderSelector.close();
    }

    /**
     * When you take over leadership start a new Scheduler instance and wait for it to complete
     * @param curatorFramework
     * @throws Exception
     */
    public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
        LOG.info(name + " has taken over the scheduler");

        waitOnScheduler(executor.submit(() -> new Scheduler().run()));
    }

    private void waitOnScheduler(Future future){
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(ExceptionUtils.getFullStackTrace(e));
        }
    }
}
