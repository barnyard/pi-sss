/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BucketAlreadyExistsException;
import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.exception.NoSuchBucketException;
import com.bt.pi.sss.filter.AuthenticationFilter;

/**
 * helper for access to DHT BucketMetaData
 */
@Component
public class BucketMetaDataHelper {
    private static final String DEFAULT_BUCKET_LOCATION = "UK";
    private static final String USER_RECORD_NOT_FOUND_FOR_USER_S = "User record not found for user %s";
    private static final Log LOG = LogFactory.getLog(BucketMetaDataHelper.class);
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private String defaultBucketLocation = DEFAULT_BUCKET_LOCATION;
    private BlockingDhtCache blockingDhtCache;
    private int maxBucketsPerUser;

    public BucketMetaDataHelper() {
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
        this.blockingDhtCache = null;
    }

    @Property(key = "max.buckets.per.user", defaultValue = "30")
    public void setMaxBucketsPerUser(int max) {
        maxBucketsPerUser = max;
    }

    public boolean updateCannedAcl(final String bucketName, final CannedAclPolicy cannedAclPolicy) {
        LOG.debug(String.format("updateCannedAcl(%s, %s)", bucketName, cannedAclPolicy));

        PId bucketId = piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        blockingDhtCache.update(bucketId, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                existingEntity.setCannedAccessPolicy(cannedAclPolicy);
                return existingEntity;
            }
        });

        return true;
    }

    public void addBucket(final BucketMetaData bucketMetaData) {
        LOG.debug(String.format("addBucketMetaDataEntity(%s)", bucketMetaData));

        PId bucketId = piIdBuilder.getPId(bucketMetaData);

        // create BucketMetaData

        blockingDhtCache.update(bucketId, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                if (existingEntity == null)
                    return bucketMetaData;

                if (!existingEntity.isDeleted()) {
                    throw new BucketAlreadyExistsException();
                }
                // TODO create a copy constructor in BucketMetaData
                existingEntity.setCannedAccessPolicy(bucketMetaData.getCannedAclPolicy());
                existingEntity.setLocation(bucketMetaData.getLocation());
                existingEntity.setUsername(bucketMetaData.getUsername());
                existingEntity.resetCreationDate();
                existingEntity.setDeleted(false);
                existingEntity.setName(bucketMetaData.getName());

                return existingEntity;
            }
        });
    }

    public BucketMetaData createBucketMetaData(final String userName, final String bucketName, final String bucketLocation, final CannedAclPolicy cannedAclPolicy) {
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, cannedAclPolicy, bucketLocation == null ? defaultBucketLocation : bucketLocation);
        bucketMetaData.setUsername(userName);
        return bucketMetaData;
    }

    public void addBucketToUser(final String userName, final String bucketName, final CannedAclPolicy cannedAclPolicy) {
        LOG.debug(String.format("addBucketToUser(%s, %s, %s)", userName, bucketName, cannedAclPolicy));

        // update user record

        PId userId = piIdBuilder.getPId(User.getUrl(userName));
        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.getBucketNames().add(bucketName);
                return existingEntity;
            }
        });
    }

    public void removeBucketFromUser(final String userName, final String bucketName) {
        LOG.debug(String.format("removeBucketFromUser(%s, %s)", userName, bucketName));
        PId userId = piIdBuilder.getPId(User.getUrl(userName));

        blockingDhtCache.update(userId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.getBucketNames().remove(bucketName);
                return existingEntity;
            }
        });
    }

    public void deleteBucket(final String bucketName) {
        LOG.debug(String.format("deleteBucket(%s)", bucketName));
        PId id = piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        blockingDhtCache.update(id, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                existingEntity.setDeleted(true);
                return existingEntity;
            }
        });
    }

    public void checkBucketWriteAccess(final String userName, final String bucketName) {
        LOG.debug(String.format("checkBucketWriteAccess(%s, %s)", bucketName, userName));
        if (userOwnsBucket(bucketName, userName))
            return;
        if (bucketHasUser(bucketName, userName))
            return;
        if (userCanWriteBucket(bucketName, userName))
            return;
        throw new AccessDeniedException();
    }

    public boolean userCanWriteBucket(final String bucketName, final String userName) {
        LOG.debug(String.format("userCanWriteBucket(%s, %s)", bucketName, userName));
        CannedAclPolicy cannedAclPolicy = getCannedAclForBucket(bucketName);
        if (null == cannedAclPolicy)
            throw new NoSuchBucketException();
        LOG.debug(String.format("CannedAclPolicy %s for bucket %s", cannedAclPolicy, bucketName));
        return CannedAclPolicy.PUBLIC_READ_WRITE.equals(cannedAclPolicy);
    }

    public CannedAclPolicy getCannedAclForBucket(final String bucketName) {
        LOG.debug(String.format("getCannedAclForBucket(%s)", bucketName));
        BucketMetaData bucketMetaData = getBucketMetaData(bucketName);
        if (null == bucketMetaData)
            throw new NoSuchBucketException();

        return bucketMetaData.getCannedAclPolicy();
    }

    private BucketMetaData getBucketMetaData(final String bucketName) {
        LOG.debug(String.format("getBucketMetaData(%s)", bucketName));

        PId bucketId = piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));
        BucketMetaData result = (BucketMetaData) blockingDhtCache.get(bucketId);
        if (result != null && result.isDeleted()) {
            LOG.debug("Returning null as the bucket is deleted:" + bucketId);
            return null;
        }

        LOG.debug("Returning bucket:" + result);
        return result;
    }

    public List<BucketMetaData> getBucketList(final Set<String> listOfBucketNames) {
        LOG.debug(String.format("getBucketList(%s)", listOfBucketNames));

        final List<BucketMetaData> results = new ArrayList<BucketMetaData>();

        for (String bucketName : listOfBucketNames) {
            BucketMetaData bucketMetaData = getBucketMetaData(bucketName);
            if (bucketMetaData != null)
                results.add(bucketMetaData);
            else
                LOG.debug(String.format("Not adding bucket: %s as it is null", bucketName));
        }

        return results;
    }

    public void checkBucketReadAccess(final String userName, final String bucketName) {
        LOG.debug(String.format("checkBucketReadAccess(%s, %s)", bucketName, userName));
        if (userOwnsBucket(bucketName, userName))
            return;
        if (bucketHasUser(bucketName, userName))
            return;
        if (userCanReadBucket(bucketName, userName))
            return;
        throw new AccessDeniedException();
    }

    private boolean bucketHasUser(final String bucketName, final String userName) {
        LOG.debug(String.format("bucketHasUser(%s, %s)", bucketName, userName));
        BucketMetaData bucketMetaData = getBucketMetaData(bucketName);
        return (null == bucketMetaData || null == bucketMetaData.getUsername()) ? false : bucketMetaData.getUsername().equals(userName);
    }

    private boolean userCanReadBucket(final String bucketName, final String userName) {
        LOG.debug(String.format("userCanReadBucket(%s, %s)", bucketName, userName));
        CannedAclPolicy cannedAclPolicy = getCannedAclForBucket(bucketName);
        if (null == cannedAclPolicy)
            throw new NoSuchBucketException();
        LOG.debug(String.format("Canned acl policy for %s is %s", bucketName, cannedAclPolicy.getName()));
        if (CannedAclPolicy.PUBLIC_READ.equals(cannedAclPolicy))
            return true;
        if (CannedAclPolicy.PUBLIC_READ_WRITE.equals(cannedAclPolicy))
            return true;
        if (CannedAclPolicy.AUTHENTICATED_READ.equals(cannedAclPolicy) && !userName.equals(AuthenticationFilter.ANONYMOUS_USER))
            return true;
        return false;
    }

    public String getLocationForBucket(final String bucketName) {
        LOG.debug(String.format("getLocationForBucket(%s)", bucketName));
        BucketMetaData bucketMetaData = getBucketMetaData(bucketName);
        if (null == bucketMetaData)
            throw new NoSuchBucketException();

        return bucketMetaData.getLocation();
    }

    public boolean bucketAlreadyExistsForUser(String bucketName, String userName) {
        LOG.debug(String.format("bucketAlreadyExistsForUser(%s, %s)", bucketName, userName));
        BucketMetaData bucketMetaData = getBucketMetaData(bucketName);

        if (bucketMetaData == null) {
            LOG.debug(String.format("Bucket %s doesn't exist.", bucketName));
            return false;
        }

        if (bucketMetaData.getUsername() != null && bucketMetaData.getUsername().equals(userName)) {
            LOG.debug(String.format("Bucket %s exists for user %s: %s", bucketName, userName, bucketMetaData));
            return true;
        }

        return false;
    }

    public boolean userOwnsBucket(final String bucketName, final String userName) {
        User user = getUser(userName);

        if (user == null) {
            String noUserRecordMessage = String.format(USER_RECORD_NOT_FOUND_FOR_USER_S, userName);
            LOG.error(noUserRecordMessage); // should never happen if we get here
            return false;
        }

        return userOwnsBucket(bucketName, user);
    }

    public boolean userOwnsBucket(final String bucketName, final User user) {
        LOG.debug(String.format("userOwnsBucket(%s, %s)", bucketName, user.getUsername()));

        StringBuilder sb = new StringBuilder("Trying to find user bucket ").append(bucketName).append(" for user ").append(user.getUsername()).append(". Found: [");
        for (String userBucket : user.getBucketNames()) {
            sb.append(userBucket).append(", ");
            if (userBucket.equalsIgnoreCase(bucketName))
                return true;
        }
        LOG.debug(sb.append("]").toString());

        return false;
    }

    public User getUser(final String userName) {
        BlockingDhtReader blockingReader = this.dhtClientFactory.createBlockingReader();
        PId id = this.piIdBuilder.getPId(User.getUrl(userName));
        return (User) blockingReader.get(id);
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource(name = "generalBlockingCache")
    public void setBlockingDhtCache(BlockingDhtCache aBlockingDhtCache) {
        this.blockingDhtCache = aBlockingDhtCache;
    }

    /**
     * Once set, this value should not be changed.
     * 
     * @param value
     */
    @Property(key = "default.bucket.location", defaultValue = DEFAULT_BUCKET_LOCATION)
    public void setDefaultBucketLocation(String value) {
        this.defaultBucketLocation = value;
    }

    /**
     * This method validates regionName against the available region names in the DHT Cache. The comparison is done in a
     * case-insensitive way.
     * 
     * @param regionName
     *            The name of a region.
     * @return
     */
    public boolean isValidRegion(String regionName) {

        Regions regions = getRegions();
        return regions != null && getRegions().getRegions().containsKey(regionName);

    }

    public void validateRegion(String regionName) {
        // This is the final location of the bucket.
        String internalRegionName = regionName == null ? defaultBucketLocation : regionName;
        if (!isValidRegion(internalRegionName))
            throw new InvalidArgumentException("Invalid Region Name: " + internalRegionName);
    }

    public Regions getRegions() {
        return (Regions) blockingDhtCache.get(piIdBuilder.getRegionsId());
    }

    public void checkUserBucketsCount(User user) {
        if (user.getBucketNames().size() > maxBucketsPerUser) {
            throw new IllegalStateException(String.format("Unable to create bucket as user %s currently has %s buckets when the maximum is %s.", user.getUsername(), user.getBucketNames().size(), maxBucketsPerUser));
        }
    }
}
