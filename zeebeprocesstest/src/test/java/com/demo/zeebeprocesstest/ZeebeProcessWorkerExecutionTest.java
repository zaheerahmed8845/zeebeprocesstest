package com.demo.zeebeprocesstest;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.JobAssert;
import io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;
import io.camunda.zeebe.process.test.filters.RecordStream;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@ZeebeProcessTest
public class ZeebeProcessWorkerExecutionTest {

    private ZeebeTestEngine engine;
    private ZeebeClient client;
    private RecordStream recordStream;

    private void initDeployment() {
        client.newDeployResourceCommand()
                .addResourceFromClasspath("zeebe-test.bpmn")
                .send()
                .join();
    }

    private ProcessInstanceAssert initProcessInstanceStart() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test 1");

        ProcessInstanceEvent event = client.newCreateInstanceCommand()
                .bpmnProcessId("zeebe_test")
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        return BpmnAssert.assertThat(event);
    }

    @Test
    public void testWorker() {
        initDeployment();
        ProcessInstanceAssert instanceAssert = initProcessInstanceStart();

        //The below lines fetches one job of task type "zeebe_test_task"
        ActivateJobsResponse response = client.newActivateJobsCommand()
                .jobType("zeebe_test_task")
                .maxJobsToActivate(1)
                .send()
                .join();

        ActivatedJob activatedJob = response.getJobs().get(0);
        JobAssert jobAssert = BpmnAssert.assertThat(activatedJob);

        ZeebeProcessTestWorker zeebeProcessTestWorker = new ZeebeProcessTestWorker();
        //Explicitly calling the worker, to test the actual worker.
        zeebeProcessTestWorker.handleWorker(client, activatedJob);
        //This checks that there are no incidents received after the worker is executed.
        jobAssert.hasNoIncidents();

        //then
        //This is to check if the process is completed.
        instanceAssert.isCompleted();
    }
}
