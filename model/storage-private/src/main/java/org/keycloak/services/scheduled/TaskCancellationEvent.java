package org.keycloak.services.scheduled;

import org.keycloak.cluster.ClusterEvent;

public class TaskCancellationEvent implements ClusterEvent {

    public static final String CANCEL_TASK = "cancelTask";
    private final String taskName;

    public TaskCancellationEvent(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }
}
