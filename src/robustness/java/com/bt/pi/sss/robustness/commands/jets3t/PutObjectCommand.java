/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.commands.jets3t;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jets3t.service.model.S3Object;

import com.bt.pi.sss.robustness.EmptyInputStream;
import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public class PutObjectCommand extends Jets3tCommand {

    public PutObjectCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    @Override
    protected void cleanup(Map<String, Object> params) throws Throwable {
    }

    @Override
    protected void execute(Map<String, Object> params) throws Throwable {
        S3Object object = getS3Object(params);
        InputStream dataInputStream = new EmptyInputStream(getDataSize(params));
        object.setDataInputStream(dataInputStream);
        getWalrusService(params).putObject(getBucketName(params), object);
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
