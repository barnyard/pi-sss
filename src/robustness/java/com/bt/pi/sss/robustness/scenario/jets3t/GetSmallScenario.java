/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.scenario.jets3t;

import java.io.InputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jets3t.service.model.S3Object;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.sss.robustness.EmptyInputStream;
import com.bt.pi.sss.robustness.PisssCommandBase;
import com.bt.pi.sss.robustness.commands.jets3t.GetObjectCommand;
import com.bt.pi.sss.robustness.commands.jets3t.Jets3tCommand;
import com.bt.pi.sss.robustness.scenario.PisssScenarioBase;
import com.ragstorooks.testrr.ScenarioCommand;

@DependsOn(value = { "scenarioConfiguration", "pisssHttpServer" })
@Component
public class GetSmallScenario extends PisssScenarioBase {
    private static final long SIZE = 50000;

    private String object;
    private String bucket;

    public GetSmallScenario() {
        super(new ScheduledThreadPoolExecutor(5));

        object = getObject();
        bucket = getBucket();
    }

    public void setup() throws Exception {
        Jets3tCommand.getWalrusService(getAccessKey(), getSecretKey(), getPisssHost(), getPisssPort(), getProxyHost(), getProxyPort()).createBucket(bucket);
        S3Object s3Object = new S3Object(object);
        InputStream dataInputStream = new EmptyInputStream(SIZE);
        s3Object.setDataInputStream(dataInputStream);
        Jets3tCommand.getWalrusService(getAccessKey(), getSecretKey(), getPisssHost(), getPisssPort(), getProxyHost(), getProxyPort()).putObject(bucket, s3Object);
    }

    public void destroy() throws Exception {
        Jets3tCommand.getWalrusService(getAccessKey(), getSecretKey(), getPisssHost(), getPisssPort(), getProxyHost(), getProxyPort()).deleteObject(bucket, object);
        Jets3tCommand.getWalrusService(getAccessKey(), getSecretKey(), getPisssHost(), getPisssPort(), getProxyHost(), getProxyPort()).deleteBucket(bucket);
    }

    @Override
    protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
        params = setupParams(params);

        commandQueue.add(new GetObjectCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
    }

    private Map<String, Object> setupParams(Map<String, Object> params) {
        params.put(Jets3tCommand.SECRET_KEY, getSecretKey());
        params.put(Jets3tCommand.ACCESS_KEY, getAccessKey());
        params.put(Jets3tCommand.BUCKET_NAME, bucket);
        params.put(Jets3tCommand.OBJECT_KEY, object);
        params.put(Jets3tCommand.DATA_SIZE, SIZE + "B");
        params.put(Jets3tCommand.TMP_DIR, ".");
        params.put(PisssCommandBase.ENDPOINT_HOST, getPisssHost());
        params.put(PisssCommandBase.ENDPOINT_PORT, getPisssPort());
        params.put(PisssCommandBase.PROXY_HOST, getProxyHost());
        params.put(PisssCommandBase.PROXY_PORT, getProxyPort());

        return params;
    }

}
