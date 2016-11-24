package ai.grakn.engine.backgroundtasks.distributed;

import ai.grakn.engine.backgroundtasks.TaskStatus;
import org.json.JSONObject;

import java.io.Serializable;

public class WorkItem implements Serializable {
    private TaskStatus status;
    private String assignedWorker;

    public WorkItem status(TaskStatus status) {
        this.status = status;
        return this;
    }

    public TaskStatus status() {
        return status;
    }

    public WorkItem assignedWorker(String assignedWorker) {
        this.assignedWorker = assignedWorker;
        return this;
    }

    public String assignedWorker() {
        return assignedWorker;
    }

    public String toString() {
        JSONObject out = new JSONObject()
                .put("status", status.toString())
                .put("assignedWorker", assignedWorker);

        return out.toString();
    }

    public WorkItem(String serialised) {
        JSONObject o = new JSONObject(serialised);
        status = TaskStatus.valueOf(o.getString("status"));
        assignedWorker = o.getString("assignedWorker");
    }

    public WorkItem() {}
}
