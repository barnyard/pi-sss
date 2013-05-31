package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.junit.Before;
import org.junit.Test;

public class Jets3tTest extends AbstractTestBase {
    private RestS3Service walrusService;

    @Before
    public void beforeClass() throws Exception {
        setupWalrusService(ACCESS_KEY, SECRET_KEY);
    }

    private void setupWalrusService(String accessKey, String secretKey) throws Exception {
        String jets3tPropertiesFile = "src/integration/resources/jets3t.properties";
        Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME).loadAndReplaceProperties(new FileInputStream(jets3tPropertiesFile), "jets3t.properties in Cockpit's home folder ");

        AWSCredentials awsCredentials = new AWSCredentials(accessKey, secretKey);
        walrusService = new RestS3Service(awsCredentials);
    }

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        S3Bucket[] result = walrusService.listAllBuckets();

        // assert
        assertEquals(2, result.length);
        if (bucketName.equals(result[0].getName())) {
            assertEquals(bucketName, result[0].getName());
            assertEquals("test2", result[1].getName());
        } else {
            assertEquals(bucketName, result[1].getName());
            assertEquals("test2", result[0].getName());
        }
        assertDate(result[0].getCreationDate());
        assertDate(result[1].getCreationDate());
        assertEquals(ACCESS_KEY, result[0].getOwner().getId());
        assertEquals(USER_NAME, result[0].getOwner().getDisplayName());
        assertEquals(ACCESS_KEY, result[1].getOwner().getId());
        assertEquals(USER_NAME, result[1].getOwner().getDisplayName());
    }

    /* seems like Jets3t uses listAllBuckets, then selects the one with the required name */
    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        S3Bucket result = walrusService.getBucket("test1");

        // assert
        assertEquals(bucketName, result.getName());

        assertDate(result.getCreationDate());
        assertEquals(ACCESS_KEY, result.getOwner().getId());
        assertEquals(USER_NAME, result.getOwner().getDisplayName());
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        AccessControlList result = walrusService.getBucketAcl(bucketName);

        // assert
        assertEquals(USER_NAME, result.getOwner().getId());
        assertEquals(1, result.getGrants().size());
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        String result = walrusService.getBucketLocation(bucketName);

        // assert
        assertEquals(bucketLocation, result);
    }

    @Override
    @Test(expected = S3ServiceException.class)
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");
        setupWalrusService(ACCESS_KEY, BAD_SECRET_KEY);

        try {
            // act
            walrusService.getBucket("test2");
        } catch (S3ServiceException ex) {
            // assert
            assertEquals(401, ex.getResponseCode());
            throw ex;
        }
    }

    @Override
    @Test(expected = S3ServiceException.class)
    public void testGetServiceWithBadUser() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");
        setupWalrusService(BAD_ACCESS_KEY, SECRET_KEY);

        try {
            // act
            walrusService.getBucket("test2");
        } catch (S3ServiceException ex) {
            // assert
            assertEquals(401, ex.getResponseCode());
            throw ex;
        }
    }

    @Test(expected = S3ServiceException.class)
    public void testShouldFailIfUserHasExceededTheMaximumNumberOfBuckets() throws Exception {
        // setup

        // act
        for (int i = 0; i <= 32; i++) {
            String abucketName = String.format("%s-%d", bucketName, i);
            S3Bucket result = walrusService.createBucket(abucketName);
            assertEquals(abucketName, result.getName());
        }
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        S3Bucket result = walrusService.createBucket(bucketName);

        // assert
        assertEquals(bucketName, result.getName());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

        // assertEquals(1, bucketMetaDataCollection.size());
    }

    /* seems like Jets3t used listAllBuckets, then selects the one with the required name */
    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        S3Bucket result = walrusService.getBucket("test99");

        // assert
        assertNull(result);
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        S3Bucket bucket = walrusService.getBucket(bucketName);

        // act
        S3Object result = walrusService.getObject(bucket, objectName);

        // assert
        InputStream is = result.getDataInputStream();

        byte[] buffer = new byte[100];
        int read = is.read(buffer);
        String resultData = new String(buffer, 0, read);
        assertEquals(testData, resultData);
    }

    @Override
    public void testHeadObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        S3Bucket bucket = walrusService.getBucket(bucketName);

        // act
        S3Object result = walrusService.getObjectDetails(bucket, objectName);

        // assert
        assertNull(result.getDataInputStream());
        assertEquals(testData.length(), result.getContentLength());
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        S3Object object = new S3Object(objectName);
        InputStream dataInputStream = new ByteArrayInputStream(testData.getBytes());
        object.setDataInputStream(dataInputStream);

        // act
        S3Object result = walrusService.putObject("test1", object);

        // assert
        assertEquals(testData.length(), result.getContentLength());
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testPutLargeObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        String largeTestData = StringUtils.rightPad("a", 2500000, 'a');

        S3Object object = new S3Object(objectName);
        InputStream dataInputStream = new ByteArrayInputStream(largeTestData.getBytes());
        object.setDataInputStream(dataInputStream);

        // act
        S3Object result = walrusService.putObject("test1", object);

        // assert
        assertEquals(largeTestData.length(), result.getContentLength());
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        S3Object object = new S3Object(objectName);
        InputStream dataInputStream = new ByteArrayInputStream(testData.getBytes());
        object.setDataInputStream(dataInputStream);
        object.setMd5Hash(DigestUtils.md5(testData.getBytes()));

        // act
        S3Object result = walrusService.putObject("test1", object);

        // assert
        assertEquals(testData.length(), result.getContentLength());
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testDeleteObject() throws Exception {
        // setup
        String objectName = "testFile1";
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);

        // act
        walrusService.deleteObject(bucketName, objectName);

        // assert
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        walrusService.deleteBucket(bucketName);

        // assert
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        // assertEquals(0, bucketMetaDataCollection.get(USER_NAME).size());
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup
        String regionName = "RuyLopez";
        // act
        S3Bucket result = walrusService.createBucket(bucketName, regionName);

        // assert
        assertEquals(bucketName, result.getName());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();

        // act
        S3Bucket result = walrusService.getBucket(bucketName);

        // assert
        assertNull(result);
    }

}
