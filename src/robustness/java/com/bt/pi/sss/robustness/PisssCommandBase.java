/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public abstract class PisssCommandBase extends ScenarioCommandBase {
    public static final String ENDPOINT_HOST = "endpoint.host";
    public static final String ENDPOINT_PORT = "endpoint.port";
    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";
    public static final String OBJECT_KEY = "objectKey";
    public static final String BUCKET_NAME = "bucketName";
    public static final String SECRET_KEY = "secretKey";
    public static final String ACCESS_KEY = "accessKey";
    public static final String DATA_SIZE = "dataSize";
    public static final String TMP_DIR = "tmpDir";

    public PisssCommandBase(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    protected static String getAccessKey(Map<String, Object> params) {
        return (String) params.get(ACCESS_KEY);
    }

    protected static String getSecretKey(Map<String, Object> params) {
        return (String) params.get(SECRET_KEY);
    }

    protected static String getTmpDir(Map<String, Object> params) {
        return (String) params.get(TMP_DIR);
    }

    protected static String getBucketName(Map<String, Object> params) {
        return (String) params.get(BUCKET_NAME);
    }

    protected static String getObjectKey(Map<String, Object> params) {
        return (String) params.get(OBJECT_KEY);
    }

    protected static String getEndPointHost(Map<String, Object> params) {
        return (String) params.get(ENDPOINT_HOST);
    }

    protected static String getEndPointPort(Map<String, Object> params) {
        return (String) params.get(ENDPOINT_PORT);
    }

    protected static String getProxyHost(Map<String, Object> params) {
        return (String) params.get(PROXY_HOST);
    }

    protected static String getProxyPort(Map<String, Object> params) {
        return (String) params.get(PROXY_PORT);
    }

    protected static long getDataSize(Map<String, Object> params) {
        String str = (String) params.get(DATA_SIZE);
        String lastDigit = str.substring(str.length() - 1);
        long val = Long.parseLong(str.substring(0, str.length() - 1));
        if (lastDigit.equalsIgnoreCase("K")) {
            val *= 1024;
        } else if (lastDigit.equalsIgnoreCase("M")) {
            val *= 1048576;
        } else if (lastDigit.equalsIgnoreCase("G")) {
            val *= 1073741824;
        }

        return val;
    }
}
