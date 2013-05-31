package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.exception.InvalidBucketNameException;

public class NameValidatorTest {
    private NameValidator bucketNameValidator;

    @Before
    public void setUp() throws Exception {
        this.bucketNameValidator = new NameValidator();
        this.bucketNameValidator.setStrict(true);
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameNull() {
        // setup
        String bucketName = null;

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name cannot be null", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameTooShort() {
        // setup
        String bucketName = "a";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name too short", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameTooLong() {
        // setup
        String bucketName = StringUtils.rightPad("a", 64, "b");

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name too long", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameDoesntStartWithLetterOrNumber() {
        // setup
        String bucketName = "-2aaaa";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must start with a lower case letter or a number", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testValidateBucketNameHasUpperCaseLetter() {
        // setup
        String bucketName = "2aDaa";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameHasInvalidChars() {
        // setup
        String bucketName = "2a:;;aa";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must only contain letters, numbers, dot (.), dash (-), or underscore", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameEndsWithDash() {
        // setup
        String bucketName = "2a999ss-";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not end with a dash (-)", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameTwoAdjacentPeriods() {
        // setup
        String bucketName = "2a999..ss";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not contain two adjacent periods", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameDashPeriod() {
        // setup
        String bucketName = "2a999-.ss";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not contain adjacent period and dash", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNamePeriodDash() {
        // setup
        String bucketName = "2a999.-ss";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not contain adjacent period and dash", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameIpAddress1() {
        // setup
        String bucketName = "123.123.123.123";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not be an IP address", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testValidateBucketNameIpAddress2() {
        // setup
        String bucketName = "1.1.1.1";

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            assertEquals("bucket name must not be an IP address", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testValidateBucketNameShortIpAddress() {
        // setup
        String bucketName = "123.123.123";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);

        // assert
        // no exception
    }

    @Test
    public void testValidateBucketNameLongIpAddress() {
        // setup
        String bucketName = "123.123.123.22.33";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);

        // assert
        // no exception
    }

    @Test
    public void testValidateBucketNameIpAddressWithChars() {
        // setup
        String bucketName = "123.gg3.123.22";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);

        // assert
        // no exception
    }

    @Test(expected = InvalidArgumentException.class)
    public void testValidateObjectNameMetadataSuffix() {
        try {
            this.bucketNameValidator.validateObjectName("fred.metadata.json");
        } catch (InvalidArgumentException e) {
            // assert
            assertEquals("object name cannot end with reserved suffix \".metadata.json\"", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidArgumentException.class)
    public void testValidateObjectNameTooLong() {
        String objectName = StringUtils.leftPad("a", 1025, "b");
        try {
            this.bucketNameValidator.validateObjectName(objectName);
        } catch (InvalidArgumentException e) {
            // assert
            assertEquals("object name cannot be more than 1024 characters long", e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testBucketNameCannotContainUnderscore() {
        // setup

        // act
        try {
            this.bucketNameValidator.validateBucketName("abcd_123");
        } catch (InvalidBucketNameException e) {
            // assert
            assertEquals("bucket name cannot contain underscore (_)", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testBucketNameCanContainUnderscoreIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);

        // act
        this.bucketNameValidator.validateBucketName("abcd_123");
    }

    @Test
    public void testBucketNameCanBe255LongIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = StringUtils.rightPad("a", 255, 'a');

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }

    @Test(expected = InvalidBucketNameException.class)
    public void testBucketNameTooLongIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = StringUtils.rightPad("a", 256, 'a');

        // act
        try {
            this.bucketNameValidator.validateBucketName(bucketName);
        } catch (InvalidBucketNameException e) {
            // assert
            assertEquals("bucket name too long", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testBucketNameCanEndWithDashIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = "abcd-";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }

    @Test
    public void testBucketNameCanHaveAdjacentPeriodsIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = "abcd..asdd";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }

    @Test
    public void testBucketNameCanHaveDotDashIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = "abcd.-asdd";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }

    @Test
    public void testBucketNameCanHaveDashDotIfNotStrict() {
        // setup
        this.bucketNameValidator.setStrict(false);
        String bucketName = "abcd-.asdd";

        // act
        this.bucketNameValidator.validateBucketName(bucketName);
    }
}
