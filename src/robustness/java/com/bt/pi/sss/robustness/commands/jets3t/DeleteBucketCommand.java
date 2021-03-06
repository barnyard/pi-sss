/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.commands.jets3t;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public class DeleteBucketCommand extends Jets3tCommand {

	public DeleteBucketCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	protected void cleanup(Map<String, Object> params) throws Throwable {
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		getWalrusService(params).deleteBucket(getBucketName(params));
		getScenarioListener().scenarioSuccess(getScenarioId());
	}

	@Override
	protected ScenarioCommandBase getCompensationCommand() {
		return null;
	}

	@Override
	public long getDelayMillis() {
		return 0;
	}
}
