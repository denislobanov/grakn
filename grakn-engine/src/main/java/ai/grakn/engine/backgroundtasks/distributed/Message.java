package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.TaskStatus;
import org.json.JSONObject;

import java.io.Serializable;

public class Message implements Serializable {
    private TaskStatus status;
    private JSONObject configuration;

    public Message status(TaskStatus status) {
        this.status = status;
        return this;
    }

    public TaskStatus status() {
        return status;
    }

    public Message configuration(JSONObject configuration) {
        this.configuration = configuration;
        return this;
    }

    public JSONObject configuration() {
        return configuration;
    }


    public String toString() {
        JSONObject out = new JSONObject()
                .put("status", status.toString())
                .put("configuration", configuration);

        return out.toString();
    }

    public Message(String serialised) {
        JSONObject o = new JSONObject(serialised);
        status = TaskStatus.valueOf(o.getString("status"));
        configuration = o.getJSONObject("configuration");
    }

    public Message() {}
}
