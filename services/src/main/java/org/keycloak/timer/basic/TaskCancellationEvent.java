package org.keycloak.timer.basic;

import org.keycloak.cluster.ClusterEvent;

public class TaskCancellationEvent implements ClusterEvent {
    private final String taskName;

    public TaskCancellationEvent(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }
}
