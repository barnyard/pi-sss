/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractPublicManagedAddressingPiApplication;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.entities.BucketCollectionEntity;

@Component
public class PisssApplicationManager extends AbstractPublicManagedAddressingPiApplication {
    public static final String APPLICATION_NAME = "pi-sss-manager";
    private static final Log LOG = LogFactory.getLog(PisssApplicationManager.class);
    private int appPort;
    private RegionScopedSharedRecordConditionalApplicationActivator applicationActivator;
    @Resource
    private BucketUserDeleteHelper bucketUserDeleteHelper;

    public PisssApplicationManager() {
        super(APPLICATION_NAME);
        appPort = -1;
    }

    @Resource(type = RegionScopedSharedRecordConditionalApplicationActivator.class)
    public void setSharedRecordConditionalApplicationActivator(RegionScopedSharedRecordConditionalApplicationActivator aSharedRecordConditionalApplicationActivator) {
        applicationActivator = aSharedRecordConditionalApplicationActivator;
    }

    @Override
    public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
        return applicationActivator;
    }

    @Override
    protected int getPort() {
        return appPort;
    }

    @Property(key = "pisss.http.port", defaultValue = "8080")
    public void setPisssPort(int value) {
        appPort = value;
    }

    @Property(key = "pisss.app.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Property(key = "pisss.app.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setStartTimeoutMillis(long value) {
        super.setStartTimeoutMillis(value);
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        LOG.warn(String.format("Application: %s Node: %s has left the ring!", APPLICATION_NAME, nodeId));
        removeNodeIdFromApplicationRecord(nodeId);
        forceActivationCheck();
    }

    @Override
    public List<String> getPreferablyExcludedApplications() {
        return Arrays.asList("pi-network-manager", "pi-api-manager");
    }

    @Override
    public void deliver(PId pid, ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("deliver(%s,%s)", pid, receivedMessageContext));
        PiEntity entity = receivedMessageContext.getReceivedEntity();
        LOG.debug("Received entity: " + entity);
        if (entity != null && entity instanceof BucketCollectionEntity && receivedMessageContext.getMethod().equals(EntityMethod.DELETE)) {
            BucketCollectionEntity bucketCollectionEntity = (BucketCollectionEntity) entity;
            if (bucketCollectionEntity.getBucketNames().isEmpty()) {
                LOG.info("Received an empty list of buckets to delete, skipping task");
                return;
            }
            LOG.debug("Deleting bucket list from user " + bucketCollectionEntity.getOwner());
            for (String bucketName : bucketCollectionEntity.getBucketNames()) {
                LOG.debug("Deleting bucket " + bucketName);
                bucketUserDeleteHelper.deleteFullBucket(bucketCollectionEntity.getOwner(), bucketName);
            }
        }

    }
}
