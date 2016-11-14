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

import ai.grakn.engine.util.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static ai.grakn.engine.backgroundtasks.TaskStatus.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class InMemoryTaskManager implements TaskManager {
    private static String RUN_ONCE_NAME = "One off task scheduler.";
    private static String RUN_RECURRING_NAME = "Recurring task scheduler.";
    private static String EXCEPTION_CATCHER_NAME = "Task Exception Catcher.";

    private static InMemoryTaskManager instance = null;

    private final Logger LOG = LoggerFactory.getLogger(InMemoryTaskManager.class);

    private Map<String, ScheduledFuture<BackgroundTask>> scheduledFutures;
    private Map<String, BackgroundTask> instantiatedTasks;
    private TaskStorage taskStorage;

    private ExecutorService executorService;
    private ScheduledExecutorService schedulingService;

    private InMemoryTaskManager() {
        scheduledFutures = new ConcurrentHashMap<>();
        instantiatedTasks = new ConcurrentHashMap<>();
        taskStorage = InMemoryTaskStorage.getInstance();

        ConfigProperties properties = ConfigProperties.getInstance();
        schedulingService = Executors.newScheduledThreadPool(1);
        executorService = Executors.newFixedThreadPool(properties.getAvailableThreads());
    }

    public static synchronized InMemoryTaskManager getInstance() {
        if (instance == null)
            instance = new InMemoryTaskManager();
        return instance;
    }

    public String scheduleTask(BackgroundTask task, String createdBy, Date runAt) {
        String id = taskStorage.newState(task.getClass().getName(), createdBy, runAt, false, 0, null);

        // Schedule task to run.
        Date now = new Date();
        long delay = now.getTime() - runAt.getTime();

        ScheduledFuture<BackgroundTask> f = (ScheduledFuture<BackgroundTask>) schedulingService.schedule(
                runSingleTask(id, task::start), delay, MILLISECONDS);

        instantiatedTasks.put(id, task);
        scheduledFutures.put(id, f);
        taskStorage.updateState(id, SCHEDULED, this.getClass().getName(), null, null, null);
        return id;
    }

    public String scheduleRecurringTask(BackgroundTask task, String createdBy, Date runAt, long period) {
        String id = taskStorage.newState(task.getClass().getName(), createdBy, runAt, true, period, null);

        Date now = new Date();
        long delay = now.getTime() - runAt.getTime();

        ScheduledFuture<BackgroundTask> f = (ScheduledFuture<BackgroundTask>) schedulingService.scheduleAtFixedRate(
                runRecurringTask(id, task::start), delay, period, MILLISECONDS);

        instantiatedTasks.put(id, task);
        scheduledFutures.put(id, f);
        taskStorage.updateState(id, SCHEDULED, this.getClass().getName(), null, null, null);
        return id;
    }

    public TaskManager stopTask(String id, String requesterName) {
        // Deep copy of TaskState
        TaskState state = taskStorage.getState(id);
        if(state == null)
            return this;

        ScheduledFuture<BackgroundTask> future = scheduledFutures.get(id);
        String name = this.getClass().getName();

        synchronized (future) {
            System.out.println("stopping..");
            if(state.status() == SCHEDULED || (state.status() == COMPLETED && state.isRecurring())) {
                System.out.println("curretnly scheduled, cancelling");
                future.cancel(true);
                taskStorage.updateState(id, STOPPED, name,null, null, null);
            }
            else if(state.status() == RUNNING) {
                System.out.println("currently running, calling stop");

                BackgroundTask task = instantiatedTasks.get(id);
                synchronized (task) {
                    task.stop();
                }

                taskStorage.updateState(id, STOPPED, name, null, null, null);
            }
            else {
                System.out.println("not running");
                LOG.warn("Task not running - "+id);
            }
        }
        System.out.println("done");

        return this;
    }

    public TaskStorage storage() {
        return taskStorage;
    }

    private Runnable exceptionCatcher(String id, Runnable fn) {
        return () -> {
            try {
                fn.run();
                taskStorage.updateState(id, COMPLETED, EXCEPTION_CATCHER_NAME, null, null, null);
            } catch (Throwable t) {
                taskStorage.updateState(id, FAILED, EXCEPTION_CATCHER_NAME, null, t, null);
            } finally {
                BackgroundTask task = instantiatedTasks.get(id);
                synchronized (task) {
                    instantiatedTasks.remove(id);
                }
            }
        };
    }

    private Runnable runSingleTask(String id, Runnable fn) {
        return () -> {
            TaskState state = taskStorage.getState(id);
            if(state.status() == SCHEDULED) {
                taskStorage.updateState(id, RUNNING, RUN_ONCE_NAME, null, null, null);
                executorService.submit(exceptionCatcher(id, fn));
            }
        };
    }

    private Runnable runRecurringTask(String id, Runnable fn) {
        return () -> {
            TaskState state = taskStorage.getState(id);
            if(state.status() == SCHEDULED || state.status() == COMPLETED) {
                taskStorage.updateState(id, RUNNING, RUN_RECURRING_NAME, null, null, null);
                executorService.submit(exceptionCatcher(id, fn));
            }
        };
    }
}
