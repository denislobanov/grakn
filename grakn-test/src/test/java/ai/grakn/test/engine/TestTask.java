package ai.grakn.test.engine;

import ai.grakn.engine.backgroundtasks.BackgroundTask;
import org.json.JSONObject;

import java.util.function.Consumer;

public class TestTask implements BackgroundTask {
    public void start(Consumer<String> saveCheckpoint, JSONObject config) {
        System.out.println(config.getString("name"));
    }

    public void stop() {}

    public void pause() {
    }

    public void resume(Consumer<String> c, String s) {}
}
