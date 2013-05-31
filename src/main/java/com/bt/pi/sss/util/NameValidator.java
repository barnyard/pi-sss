/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.exception.InvalidBucketNameException;

/**
 * bucket name validation - see http://docs.amazonwebservices.com/AmazonS3/latest/index.html?BucketRestrictions.html
 */
@Component
public class NameValidator {
    private static final String UNDERSCORE = "_";
    private static final String DOT_DASH = ".-";
    private static final String BUCKET_NAME_MUST_NOT_CONTAIN_ADJACENT_PERIOD_AND_DASH = "bucket name must not contain adjacent period and dash";
    private static final String DASH = "-";
    private static final int THREE = 3;
    private static final Log LOG = LogFactory.getLog(NameValidator.class);
    private static final int MINIMUM_LENGTH = 3;
    private static final int MAXIMUM_LENGTH = 255;
    private static final int STRICT_MAXIMUM_LENGTH = 63;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String VALID_FIRST_CHARACTERS = ALPHABET + "0123456789";
    private static final String VALID_CHARACTERS = VALID_FIRST_CHARACTERS + DOT_DASH + UNDERSCORE;
    private static final int OBJECT_NAME_MAX_LENGTH = 1024;
    private boolean strict = true;

    public NameValidator() {
    }

    @Property(key = "nameValidator.strict", defaultValue = "true")
    public void setStrict(boolean b) {
        LOG.warn(String.format("setStrict(%s)", b));
        this.strict = b;
    }

    public void validateBucketName(final String bucketName) {
        LOG.debug(String.format("validateBucketName(%s)", bucketName));
        if (null == bucketName)
            throw new InvalidBucketNameException("bucket name cannot be null");

        validateLength(bucketName, MAXIMUM_LENGTH);

        if (!VALID_FIRST_CHARACTERS.contains(bucketName.substring(0, 1)))
            throw new InvalidBucketNameException("bucket name must start with a lower case letter or a number");

        // if (!bucketName.equals(bucketName.toLowerCase(Locale.getDefault())))
        // throw new InvalidBucketNameException("bucket name must not contain upper case letters");

        if (!StringUtils.containsOnly(bucketName, VALID_CHARACTERS))
            throw new InvalidBucketNameException("bucket name must only contain letters, numbers, dot (.), dash (-), or underscore");

        if (likeIpAddress(bucketName))
            throw new InvalidBucketNameException("bucket name must not be an IP address");

        if (this.strict) {
            if (bucketName.contains(UNDERSCORE))
                throw new InvalidBucketNameException("bucket name cannot contain underscore (_)");

            validateLength(bucketName, STRICT_MAXIMUM_LENGTH);
            if (bucketName.endsWith(DASH))
                throw new InvalidBucketNameException("bucket name must not end with a dash (-)");
            checkNotContains(bucketName, "..", "bucket name must not contain two adjacent periods");
            checkNotContains(bucketName, DOT_DASH, BUCKET_NAME_MUST_NOT_CONTAIN_ADJACENT_PERIOD_AND_DASH);
            checkNotContains(bucketName, "-.", BUCKET_NAME_MUST_NOT_CONTAIN_ADJACENT_PERIOD_AND_DASH);
        }
    }

    private void checkNotContains(String bucketName, String target, String message) {
        if (bucketName.contains(target))
            throw new InvalidBucketNameException(message);
    }

    public void validateObjectName(String objectName) {
        LOG.debug(String.format("validateObjectName(%s)", objectName));
        if (objectName.length() > OBJECT_NAME_MAX_LENGTH)
            throw new InvalidArgumentException(String.format("object name cannot be more than %d characters long", OBJECT_NAME_MAX_LENGTH));

        if (objectName.endsWith(ObjectMetaData.FILE_SUFFIX))
            throw new InvalidArgumentException(String.format("object name cannot end with reserved suffix \"%s\"", ObjectMetaData.FILE_SUFFIX));
        // TODO: loads more validation, but not sure what the rules are?
    }

    private void validateLength(final String bucketName, int max) {
        if (bucketName.length() < MINIMUM_LENGTH)
            throw new InvalidBucketNameException("bucket name too short");
        if (bucketName.length() > max)
            throw new InvalidBucketNameException("bucket name too long");
    }

    private boolean likeIpAddress(final String bucketName) {
        if (!bucketName.contains("."))
            return false;
        int dotCount = countDots(bucketName);
        if (dotCount != THREE)
            return false;
        for (String part : bucketName.split("\\.")) {
            if (part.length() > THREE)
                return false;
            if (StringUtils.containsAny(part, ALPHABET + DASH))
                return false;
        }
        return true;
    }

    private int countDots(final String bucketName) {
        int result = 0;
        for (int i = 0; i < bucketName.length(); i++)
            if (bucketName.charAt(i) == '.')
                result++;
        return result;
    }
}
