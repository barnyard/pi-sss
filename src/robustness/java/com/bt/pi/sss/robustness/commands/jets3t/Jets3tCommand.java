/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.commands.jets3t;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import com.bt.pi.sss.robustness.PisssCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public abstract class Jets3tCommand extends PisssCommandBase {
    public Jets3tCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
    }

    public static RestS3Service getWalrusService(String accessKey, String secretKey, String endpointHost, String endpointPort, String proxyHost, String proxyPort) {
        String jets3tPropertiesFile = "src/robustness/resources/jets3t.properties";
        RestS3Service service = null;
        try {
            Jets3tProperties jets3tProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
            jets3tProperties.loadAndReplaceProperties(new FileInputStream(jets3tPropertiesFile), "jets3t.properties in Cockpit's home folder ");
            jets3tProperties.setProperty("s3service.s3-endpoint", endpointHost);
            jets3tProperties.setProperty("s3service.s3-endpoint-http-port", endpointPort);

            if (proxyHost != null) {
                jets3tProperties.setProperty("httpclient.proxy-host", proxyHost);
                jets3tProperties.setProperty("httpclient.proxy-port", proxyPort);
            }

            AWSCredentials awsCredentials = new AWSCredentials(accessKey, secretKey);

            service = new RestS3Service(awsCredentials);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (S3ServiceException e) {
            e.printStackTrace();
        }
        if (service == null) {
            throw new RuntimeException("Unable to create a S3 service object");
        }

        return service;
    }

    protected RestS3Service getWalrusService(Map<String, Object> params) {
        return getWalrusService(getAccessKey(params), getSecretKey(params), getEndPointHost(params), getEndPointPort(params), getProxyHost(params), getProxyPort(params));
    }

    protected static S3Object getS3Object(Map<String, Object> params) {
        S3Object object = new S3Object(getObjectKey(params));
        return object;
    }
}
