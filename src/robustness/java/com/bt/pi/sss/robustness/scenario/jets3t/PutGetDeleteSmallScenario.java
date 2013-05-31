/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.scenario.jets3t;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.sss.robustness.PisssCommandBase;
import com.bt.pi.sss.robustness.commands.jets3t.CreateBucketCommand;
import com.bt.pi.sss.robustness.commands.jets3t.DeleteBucketCommand;
import com.bt.pi.sss.robustness.commands.jets3t.DeleteObjectCommand;
import com.bt.pi.sss.robustness.commands.jets3t.GetObjectCommand;
import com.bt.pi.sss.robustness.commands.jets3t.Jets3tCommand;
import com.bt.pi.sss.robustness.commands.jets3t.PutObjectCommand;
import com.bt.pi.sss.robustness.scenario.PisssScenarioBase;
import com.ragstorooks.testrr.ScenarioCommand;

@DependsOn(value = { "scenarioConfiguration" })
@Component
public class PutGetDeleteSmallScenario extends PisssScenarioBase {

    public PutGetDeleteSmallScenario() {
        super(new ScheduledThreadPoolExecutor(5));
    }

    @Override
    protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
        params.put(Jets3tCommand.SECRET_KEY, getSecretKey());
        params.put(Jets3tCommand.ACCESS_KEY, getAccessKey());
        params.put(Jets3tCommand.BUCKET_NAME, getBucket());
        params.put(Jets3tCommand.OBJECT_KEY, getObject());
        params.put(Jets3tCommand.DATA_SIZE, "1M");
        params.put(Jets3tCommand.ENDPOINT_HOST, getPisssHost());
        params.put(Jets3tCommand.ENDPOINT_PORT, getPisssPort());
        params.put(PisssCommandBase.PROXY_HOST, getProxyHost());
        params.put(PisssCommandBase.PROXY_PORT, getProxyPort());

        System.out.println("scenario: " + scenarioId);
        System.out.println("params: " + params);

        commandQueue.add(new CreateBucketCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
        commandQueue.add(new PutObjectCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
        commandQueue.add(new GetObjectCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
        commandQueue.add(new DeleteObjectCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
        commandQueue.add(new DeleteBucketCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
    }

    @Override
    public void setup() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }
}
