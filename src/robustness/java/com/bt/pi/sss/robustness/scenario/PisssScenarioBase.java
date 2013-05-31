package com.bt.pi.sss.robustness.scenario;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import com.bt.pi.sss.robustness.scenario.conf.ScenarioConfiguration;
import com.ragstorooks.testrr.ScenarioCommanderBase;

public abstract class PisssScenarioBase extends ScenarioCommanderBase {
    private ScenarioConfiguration scenarioConfiguration;

    public PisssScenarioBase(ScheduledExecutorService executor) {
        super(executor);
    }

    @Resource
    public void setScenarioConfiguration(ScenarioConfiguration scenarioConfiguration) {
        this.scenarioConfiguration = scenarioConfiguration;
    }

    protected String getAccessKey() {
        return scenarioConfiguration.getAccessKey();
    }

    protected String getSecretKey() {
        return scenarioConfiguration.getSecretKey();
    }

    protected String getPisssHost() {
        return scenarioConfiguration.getPisssHost();
    }

    protected String getPisssPort() {
        return scenarioConfiguration.getPisssPort();
    }

    protected String getProxyHost() {
        return scenarioConfiguration.getProxyHost();
    }

    protected String getProxyPort() {
        return scenarioConfiguration.getProxyPort();
    }

    protected String getObject() {
        return "test-object-" + UUID.randomUUID();
    }

    protected String getBucket() {
        return "test-bucket-" + UUID.randomUUID();
    }

    public abstract void setup() throws Exception;

    public abstract void destroy() throws Exception;
}
