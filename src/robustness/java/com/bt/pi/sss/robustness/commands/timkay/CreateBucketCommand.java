package com.bt.pi.sss.robustness.commands.timkay;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;
import com.ragstorooks.testrr.cli.CommandExecutor;

public class CreateBucketCommand extends TimKayCommandBase {

    public CreateBucketCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    @Override
    protected void cleanup(Map<String, Object> params) throws Throwable {
    }

    @Override
    protected void execute(Map<String, Object> params) throws Throwable {
        // setup
        String command = getTimKayBaseCommand(params) + " -vvv mkdir " + getBucketName(params);
        String[] commands = new String[] { "/bin/bash", "-c", command };
        CommandExecutor commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));

        // act
        commandExecutor.executeScript(commands, Runtime.getRuntime());

        // assert
        assertResponse(commandExecutor.getErrorLines(), "200 OK", "create bucket failed: ");
    }

    @Override
    protected ScenarioCommandBase getCompensationCommand() {
        return new DeleteBucketCommand(getScenarioId(), null, null, getExecutor(), getParams());
    }

    @Override
    public long getDelayMillis() {
        return 0;
    }
}
