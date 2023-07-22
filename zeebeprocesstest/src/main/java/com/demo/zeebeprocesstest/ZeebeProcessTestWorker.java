package com.demo.zeebeprocesstest;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import java.util.Map;

public class ZeebeProcessTestWorker {

    public void handleWorker(final JobClient client, final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        System.out.println("Worker Executed with details : " + variables.get("name"));
        client.newCompleteCommand(job.getKey()).send().join();
    }
}
