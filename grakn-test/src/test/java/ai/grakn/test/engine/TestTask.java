package ai.grakn.test.engine;

import ai.grakn.engine.backgroundtasks.BackgroundTask;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TestTask implements BackgroundTask {
    private AtomicInteger runCount = new AtomicInteger(0);

    public void start(Consumer<String> saveCheckpoint, JSONObject config) {
        runCount.incrementAndGet();
    }

    public void stop() {}

    public void pause() {
    }

    public void resume(Consumer<String> c, String s) {}

    public int getRunCount() {
        return runCount.get();
    }
}
