package com.bt.pi.sss.robustness.commands.timkay;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;
import com.ragstorooks.testrr.cli.CommandExecutor;

public class GetObjectCommand extends TimKayCommandBase {

    private static final Log log = LogFactory.getLog(GetObjectCommand.class);

    public GetObjectCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    @Override
    protected void cleanup(Map<String, Object> params) throws Throwable {
    }

    @Override
    protected void execute(Map<String, Object> params) throws Throwable {
        // setup
        log.info("getting command foo");

        File tmpFile = File.createTempFile("robustness", null, new File(getTmpDir(params)));
        tmpFile.deleteOnExit();

        String command = String.format("%s -vvv get %s/%s %s", getTimKayBaseCommand(params), getBucketName(params), getObjectKey(params), tmpFile.getAbsolutePath());
        String[] commands = new String[] { "/bin/bash", "-c", command };

        CommandExecutor commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(5));

        log.info("starting command " + command);
        // act
        commandExecutor.executeScript(commands, Runtime.getRuntime());

        log.info("about to assert " + command);

        // assert
        assertResponse(commandExecutor.getErrorLines(), "200 OK", "get object failed: ");
        if (getDataSize(params) != tmpFile.length())
            getScenarioListener().scenarioFailure(getScenarioId(), String.format("file from pisss is different in length, %d != %d", getDataSize(params), tmpFile.length()));

        tmpFile.delete();
    }

    @Override
    protected ScenarioCommandBase getCompensationCommand() {
        return null;
    }

    @Override
    public long getDelayMillis() {
        return 1000;
    }
}
