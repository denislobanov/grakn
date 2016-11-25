package ai.grakn.test.engine;

import ai.grakn.engine.backgroundtasks.BackgroundTask;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TestTask implements BackgroundTask {

    public static final AtomicInteger startedCounter = new AtomicInteger(0);

    public void start(Consumer<String> saveCheckpoint, JSONObject config) {
        System.out.println(config.getString("name"));

        startedCounter.incrementAndGet();
    }

    public void stop() {}

    public void pause() {
    }

    public void resume(Consumer<String> c, String s) {}
}
