package ai.grakn.test;

import ai.grakn.test.engine.backgroundtasks.SchedulerTest;
import ai.grakn.test.engine.backgroundtasks.TaskManagerTest;
import ai.grakn.test.engine.controller.AuthControllerTest;
import ai.grakn.test.engine.postprocessing.PostProcessingTest;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class CustomTestGroups {
    public static void main(final String args[]) {
        JUnitCore junit = new JUnitCore();
        Result result = null;
        result = junit.run(
                Request.classes(TaskManagerTest.class, SchedulerTest.class, AuthControllerTest.class, PostProcessingTest.class));

        if(result.getFailureCount() > 0) {
            for (Failure failure : result.getFailures()) {
                failure.getException().printStackTrace();
            }
        }

        System.exit(0);
    }
}
