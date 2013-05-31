/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.scenario.timkay;

import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.sss.robustness.PisssCommandBase;
import com.bt.pi.sss.robustness.commands.timkay.CreateBucketCommand;
import com.bt.pi.sss.robustness.commands.timkay.DeleteBucketCommand;
import com.bt.pi.sss.robustness.commands.timkay.DeleteObjectCommand;
import com.bt.pi.sss.robustness.commands.timkay.GetObjectCommand;
import com.bt.pi.sss.robustness.commands.timkay.PutObjectCommand;
import com.bt.pi.sss.robustness.scenario.PisssScenarioBase;
import com.ragstorooks.testrr.ScenarioCommand;

@DependsOn(value = { "scenarioConfiguration" })
@Component
public class PutGetDeleteLargeScenario extends PisssScenarioBase {
    private String tmpDir;

    public PutGetDeleteLargeScenario() {
        super(new ScheduledThreadPoolExecutor(5));
    }

    @Override
    protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
        params.put(PisssCommandBase.SECRET_KEY, getSecretKey());
        params.put(PisssCommandBase.ACCESS_KEY, getAccessKey());
        params.put(PisssCommandBase.BUCKET_NAME, getBucket());
        params.put(PisssCommandBase.OBJECT_KEY, getObject());
        params.put(PisssCommandBase.DATA_SIZE, "25M");
        params.put(PisssCommandBase.TMP_DIR, this.tmpDir);
        params.put(PisssCommandBase.ENDPOINT_HOST, getPisssHost());
        params.put(PisssCommandBase.ENDPOINT_PORT, getPisssPort());
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

    @Resource
    public void setProperties(Properties p) {
        this.tmpDir = p.getProperty("fileSystemBucketUtils.tmpDir");
        System.out.println("tmpDir: " + this.tmpDir);
    }

    @Override
    public void setup() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }
}
