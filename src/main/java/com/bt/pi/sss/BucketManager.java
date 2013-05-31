/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.core.util.Blocking;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.BucketAlreadyExistsException;
import com.bt.pi.sss.exception.BucketAlreadyOwnedByUserException;
import com.bt.pi.sss.util.FileSystemBucketUtils;
import com.bt.pi.sss.util.NameValidator;

/**
 * Manager for Buckets and Objects
 */
@Component
public class BucketManager {
    private static final String USER_RECORD_NOT_FOUND_FOR_USER_S = "User record not found for user %s";
    private static final Log LOG = LogFactory.getLog(BucketManager.class);
    private NameValidator nameValidator;
    private FileSystemBucketUtils fileSystemBucketUtils;
    private BucketMetaDataHelper bucketMetaDataHelper;

    public BucketManager() {
        this.nameValidator = null;
        this.fileSystemBucketUtils = null;
        this.bucketMetaDataHelper = null;
    }

    public void createBucket(final String userName, final String bucketName, final CannedAclPolicy cannedAclPolicy, String regionName) {
        LOG.debug(String.format("createBucket(%s, %s, %s, %s)", userName, bucketName, cannedAclPolicy, regionName));
        this.nameValidator.validateBucketName(bucketName);
        // Region Name validation and update.
        this.bucketMetaDataHelper.validateRegion(regionName);

        User user = this.bucketMetaDataHelper.getUser(userName);

        if (user == null) {
            String noUserRecordMessage = String.format(USER_RECORD_NOT_FOUND_FOR_USER_S, userName);
            LOG.error(noUserRecordMessage); // should never happen if we get here
            throw new IllegalArgumentException(noUserRecordMessage);
        }

        this.bucketMetaDataHelper.checkUserBucketsCount(user);

        if (this.bucketMetaDataHelper.userOwnsBucket(bucketName, user)) {
            LOG.debug(String.format("user %s already owns bucket %s", userName, bucketName));
            if (null == cannedAclPolicy)
                throw new BucketAlreadyOwnedByUserException();

            boolean updated = this.bucketMetaDataHelper.updateCannedAcl(bucketName, cannedAclPolicy);
            if (updated) {
                LOG.debug(String.format("... but updated CannedAclPolicy to %s", cannedAclPolicy));
                return;
            }
            throw new BucketAlreadyOwnedByUserException();
        }

        if (this.bucketMetaDataHelper.bucketAlreadyExistsForUser(bucketName, userName)) {
            this.bucketMetaDataHelper.addBucketToUser(userName, bucketName, cannedAclPolicy);
            return;
        }

        try {
            this.fileSystemBucketUtils.create(bucketName);
        } catch (BucketAlreadyExistsException e) {
            // maybe another user is trying to update the bucket ACL?
            if (null != cannedAclPolicy && this.bucketMetaDataHelper.userCanWriteBucket(bucketName, userName)) {
                boolean updated = this.bucketMetaDataHelper.updateCannedAcl(bucketName, cannedAclPolicy);
                if (updated) {
                    LOG.debug(String.format("updated canned ACL to %s for bucket %s", cannedAclPolicy, bucketName));
                }
                return;
            }

            throw e;
        }

        BucketMetaData bucketMetaData = bucketMetaDataHelper.createBucketMetaData(userName, bucketName, regionName, cannedAclPolicy);
        this.bucketMetaDataHelper.addBucket(bucketMetaData);
        this.bucketMetaDataHelper.addBucketToUser(userName, bucketName, cannedAclPolicy);
    }

    public void deleteBucket(final String userName, final String bucketName) {
        LOG.debug(String.format("deleteBucket(%s, %s)", userName, bucketName));
        this.nameValidator.validateBucketName(bucketName);

        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);

        this.fileSystemBucketUtils.delete(bucketName);
        this.bucketMetaDataHelper.deleteBucket(bucketName);
        this.bucketMetaDataHelper.removeBucketFromUser(userName, bucketName);
    }

    public SortedSet<ObjectMetaData> getListOfFilesInBucket(final String userName, final String bucketName) {
        LOG.debug(String.format("getListOfFilesInBucket(%s, %s)", userName, bucketName));
        this.nameValidator.validateBucketName(bucketName);

        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);

        SortedSet<ObjectMetaData> result = new TreeSet<ObjectMetaData>();
        for (File file : this.fileSystemBucketUtils.listFiles(bucketName)) {
            LOG.debug(String.format("adding %s", file.getName()));
            result.add(new ObjectMetaData(file));
        }
        return result;
    }

    public String storeObject(final String userName, final String bucketName, final String objectName, final InputStream inputStream, final String contentType, final String contentMd5, final String contentDisposition,
            final Map<String, List<String>> xAmzMetaHeaders) {
        LOG.debug(String.format("storeObject(%s, %s, %s, %s, %s, %s, %s, %s)", userName, bucketName, objectName, inputStream, contentType, contentMd5, contentDisposition, xAmzMetaHeaders));
        this.nameValidator.validateBucketName(bucketName);
        this.nameValidator.validateObjectName(objectName);

        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);

        return this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, contentType, contentMd5, contentDisposition, xAmzMetaHeaders);
    }

    public ObjectMetaData readObject(final String userName, final String bucketName, final String objectName) {
        LOG.debug(String.format("readObject(%s, %s, %s)", userName, bucketName, objectName));
        this.nameValidator.validateBucketName(bucketName);

        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);

        File file = this.fileSystemBucketUtils.readObject(bucketName, objectName);
        ObjectMetaData result = new ObjectMetaData(file);
        return result;
    }

    public void deleteObject(final String userName, final String bucketName, final String objectName) {
        LOG.debug(String.format("deleteObject(%s, %s, %s)", userName, bucketName, objectName));
        this.nameValidator.validateBucketName(bucketName);
        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);
        this.fileSystemBucketUtils.deleteObject(bucketName, objectName);
    }

    public List<BucketMetaData> getBucketList(final String userName) {
        LOG.debug(String.format("getBucketList(%s)", userName));
        User user = this.bucketMetaDataHelper.getUser(userName);
        if (user == null) {
            String message = String.format(USER_RECORD_NOT_FOUND_FOR_USER_S, userName);
            LOG.error(message); // should never happen if we get here in the first place
            return new ArrayList<BucketMetaData>();
        }
        return this.bucketMetaDataHelper.getBucketList(user.getBucketNames());
    }

    public CannedAclPolicy getCannedAclPolicy(final String userName, final String bucketName) {
        LOG.debug(String.format("getCannedAclPolicy(%s, %s)", userName, bucketName));
        this.nameValidator.validateBucketName(bucketName);
        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);
        return this.bucketMetaDataHelper.getCannedAclForBucket(bucketName);
    }

    public String getBucketLocation(final String userName, final String bucketName) {
        LOG.debug(String.format("getLocation(%s, %s)", userName, bucketName));
        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);
        return this.bucketMetaDataHelper.getLocationForBucket(bucketName);
    }

    @Blocking
    public String archiveBucket(final String bucketName) {
        return fileSystemBucketUtils.archiveBucket(bucketName);
    }

    @Resource
    public void setNameValidator(NameValidator validator) {
        this.nameValidator = validator;
    }

    @Resource
    public void setFileSystemBucketUtils(FileSystemBucketUtils utils) {
        this.fileSystemBucketUtils = utils;
    }

    @Resource
    public void setBucketMetaDataHelper(BucketMetaDataHelper aBucketMetaDataHelper) {
        this.bucketMetaDataHelper = aBucketMetaDataHelper;
    }
}
