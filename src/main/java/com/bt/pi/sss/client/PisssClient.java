package com.bt.pi.sss.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.BucketManager;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.AccessDeniedException;

/*
 * helper class for accessing pi-sss
 */
@Component
public class PisssClient {
    private static final String UNABLE_TO_RETRIEVE_BUCKET_OBJECT_S = "unable to retrieve bucket object: %s";

    public enum BucketAccess {
        PUBLIC, PRIVATE, NONE;
    };

    private static final Log LOG = LogFactory.getLog(PisssClient.class);
    @Resource
    private BucketManager bucketManager;

    public PisssClient() {
        this.bucketManager = null;
    }

    public String getObjectFromBucket(final String bucketName, final String objectName, final User user) {
        LOG.debug(String.format("getObjectFromBucket(bucket: %s, object: %s, user: %s)", bucketName, objectName, user.getUsername()));
        try {
            ObjectMetaData objectMetaData = getS3ObjectFromBucket(bucketName, objectName, user);
            InputStream dataInputStream = objectMetaData.getInputStream();
            return IOUtils.toString(dataInputStream);
        } catch (IOException e) {
            String errorMessage = String.format(UNABLE_TO_RETRIEVE_BUCKET_OBJECT_S, e.getMessage());
            LOG.error(errorMessage);
            throw new PisssClientException(errorMessage, e);
        }
    }

    public File getFileFromBucket(final String bucketName, final String objectName, final User user, final String filename) {
        LOG.debug(String.format("getFileFromBucket(bucket: %s, object: %s, user: %s, file: %s)", bucketName, objectName, user.getUsername(), filename));
        try {
            ObjectMetaData objectMetaData = getS3ObjectFromBucket(bucketName, objectName, user);
            File result = new File(filename);
            long copied = IOUtils.copyLarge(objectMetaData.getInputStream(), new FileOutputStream(result));
            LOG.debug(String.format("%s retrieved from %s, %d bytes", objectName, bucketName, copied));
            return result;
        } catch (IOException e) {
            String errorMessage = String.format(UNABLE_TO_RETRIEVE_BUCKET_OBJECT_S, e.getMessage());
            LOG.error(errorMessage);
            throw new PisssClientException(errorMessage, e);
        }
    }

    private ObjectMetaData getS3ObjectFromBucket(final String bucketName, final String objectName, final User user) {
        LOG.debug(String.format("getS3ObjectFromBucket(bucket: %s, object: %s, user: %s)", bucketName, objectName, user.getUsername()));
        try {
            return bucketManager.readObject(user.getUsername(), bucketName, objectName);

        } catch (AccessDeniedException e) {
            throw new NotAuthorizedException();
        }
    }

    public BucketAccess getBucketAccess(final String bucketName, final User user) {
        LOG.debug(String.format("getBucketAccess(bucket: %s, user: %s)", bucketName, user.getUsername()));
        CannedAclPolicy cannedAclPolicy = bucketManager.getCannedAclPolicy(user.getUsername(), bucketName);
        switch (cannedAclPolicy) {
        case PUBLIC_READ:
        case PUBLIC_READ_WRITE:
            return BucketAccess.PUBLIC;
        case PRIVATE:
        case BUCKET_OWNER_FULL_CONTROL:
        case BUCKET_OWNER_READ:
        case AWS_EXEC_READ:
            return BucketAccess.PRIVATE;
        default:
            return BucketAccess.NONE;
        }
    }
}
