/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.commands.timkay;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bt.pi.sss.robustness.PisssCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public abstract class TimKayCommandBase extends PisssCommandBase {

    public TimKayCommandBase(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    protected String getTimKayBaseCommand(Map<String, Object> params) {
        String http_proxy = params.get(PisssCommandBase.PROXY_HOST) == null ? "unset http_proxy" : String.format("export http_proxy=%s:%s", params.get(PisssCommandBase.PROXY_HOST), params.get(PisssCommandBase.PROXY_PORT));
        return String.format("touch ~/.awsrc;%s;AWS_HOST=%s AWS_PORT=%s AWS_ACCESS_KEY_ID=%s AWS_SECRET_ACCESS_KEY=%s etc/timkay-aws/aws --simple --insecure-aws", http_proxy, getEndPointHost(params), getEndPointPort(params), getAccessKey(params),
                getSecretKey(params));
    }

    protected void assertResponse(List<String> lines, String target, String errorMessage) {
        for (String line : lines)
            if (line.contains(target))
                return;
        getScenarioListener().scenarioFailure(getScenarioId(), errorMessage + lines);
    }
}
