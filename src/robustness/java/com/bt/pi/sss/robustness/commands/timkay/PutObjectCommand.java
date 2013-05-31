package com.bt.pi.sss.robustness.commands.timkay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;

import com.bt.pi.sss.robustness.EmptyInputStream;
import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;
import com.ragstorooks.testrr.cli.CommandExecutor;

public class PutObjectCommand extends TimKayCommandBase {

    public PutObjectCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    @Override
    protected void cleanup(Map<String, Object> params) throws Throwable {
    }

    @Override
    protected void execute(Map<String, Object> params) throws Throwable {
        // setup
        InputStream inputStream = new EmptyInputStream(getDataSize(params));
        File tmpFile = File.createTempFile("robustness", null, new File(getTmpDir(params)));
        tmpFile.deleteOnExit();
        IOUtils.copyLarge(inputStream, new FileOutputStream(tmpFile));
        String command = String.format("%s -vvv put %s/%s %s", getTimKayBaseCommand(params), getBucketName(params), getObjectKey(params), tmpFile.getAbsolutePath());
        String[] commands = new String[] { "/bin/bash", "-c", command };
        CommandExecutor commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));

        // act
        commandExecutor.executeScript(commands, Runtime.getRuntime());

        // assert
        assertResponse(commandExecutor.getErrorLines(), "200 OK", "put object failed: ");
        tmpFile.delete();
    }

    @Override
    protected ScenarioCommandBase getCompensationCommand() {
        return new DeleteObjectCommand(getScenarioId(), null, null, getExecutor(), getParams());
    }

    @Override
    public long getDelayMillis() {
        return 0;
    }
}
