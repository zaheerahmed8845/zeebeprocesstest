package com.demo.zeebeprocesstest;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.DeploymentAssert;
import io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;
import io.camunda.zeebe.process.test.filters.RecordStream;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@ZeebeProcessTest
class ZeebeProcessExampleTest {

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
		ProcessInstanceEvent event = client.newCreateInstanceCommand()
				.bpmnProcessId("zeebe_test")
				.latestVersion()
				.send()
				.join();
		return BpmnAssert.assertThat(event);
	}

	@Test
	public void test_deployment() {
		DeploymentEvent event = client.newDeployResourceCommand()
				.addResourceFromClasspath("zeebe-test.bpmn")
				.send()
				.join();
		DeploymentAssert assertions = BpmnAssert.assertThat(event);
	}

	@Test
	public void testProcessInstanceStart() {
		initDeployment();

		ProcessInstanceEvent processInstanceEvent = client.newCreateInstanceCommand()
				.bpmnProcessId("zeebe_test")
				.latestVersion()
				.send()
				.join();

		ProcessInstanceAssert processInstanceAssert = BpmnAssert.assertThat(processInstanceEvent);
		processInstanceAssert.hasPassedElement("StartEvent_1");
	}

	@Test
	public void testJobAssertion() {
		initDeployment();
		initProcessInstanceStart();

		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("zeebe_test_task")
				.maxJobsToActivate(1)
				.send()
				.join();

		ActivatedJob activatedJob = response.getJobs().get(0);
		BpmnAssert.assertThat(activatedJob);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
	}

	@Test
	public void testCompletionOfInstance() throws InterruptedException, TimeoutException {
		//Given
		initDeployment();
		ProcessInstanceAssert instanceAssert = initProcessInstanceStart();

		//When
		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("zeebe_test_task")
				.maxJobsToActivate(1)
				.send()
				.join();
		ActivatedJob activatedJob = response.getJobs().get(0);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
		engine.waitForIdleState(Duration.ofMinutes(1));

		//then
		instanceAssert.isCompleted();
	}

}
