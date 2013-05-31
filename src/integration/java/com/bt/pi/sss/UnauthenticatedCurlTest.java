/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.id.PId;

public class UnauthenticatedCurlTest extends AbstractTestBase {

    private static final String curlBaseCommand = "curl -vs -x \"\" ";

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(curlBaseCommand + "-X DELETE " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Override
    public void testDeleteObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);

        // act
        runCommand(curlBaseCommand + "-X DELETE " + s3Url + "/" + bucketName + "/" + objectName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");

        assertTrue(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testGetBucketWhenAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ);

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
    }

    private void updateBucketAcl(final String bucketName, final CannedAclPolicy cannedAclPolicy) {
        PId id = piIdbuildler.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        dhtCache.update(id, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                existingEntity.setCannedAccessPolicy(cannedAclPolicy);
                return existingEntity;
            }
        });
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName + "?acl");

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName + "?location");

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 404 Not Found");
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName + "/" + objectName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testGetObjectWithAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ);

        // act
        runCommand(curlBaseCommand + s3Url + "/" + bucketName + "/" + objectName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
    }

    @Override
    public void testGetService() throws Exception {
        // setup

        // act
        runCommand(curlBaseCommand + s3Url + "/");

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // not applicable as this is entirely unauthenticated
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // not applicable as this is entirely unauthenticated
    }

    @Override
    public void testHeadObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(curlBaseCommand + " -I " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testHeadObjectWithAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        FileUtils.cleanDirectory(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ);

        // act
        runCommand(curlBaseCommand + " -I " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Override
    public void testPutLargeObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String largeTestData = StringUtils.rightPad("a", 2500000, 'a');
        String tmpFilePath = writeToTmpFile(largeTestData);

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testPutLargeObjectWithAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        String largeTestData = StringUtils.rightPad("a", 2500000, 'a');
        String tmpFilePath = writeToTmpFile(largeTestData);

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testPutObjectWithAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        String tmpFilePath = writeToTmpFile(testData);

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);
        String md5 = new String(Base64.encodeBase64(DigestUtils.md5(testData.getBytes())));

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath + " -H content-md5:" + md5);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 403 Forbidden");
    }

    @Test
    public void testPutObjectMd5WithAnonymousAuthorised() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        String tmpFilePath = writeToTmpFile(testData);
        String md5 = new String(Base64.encodeBase64(DigestUtils.md5(testData.getBytes())));

        // act
        runCommand(curlBaseCommand + "-X PUT " + s3Url + "/" + bucketName + "/" + objectName + " -T " + tmpFilePath + " -H content-md5:" + md5);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 200 OK");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup
        String regionName = "RuyLopez";
        setupDefaultRegions();

        // act

        runCommand(curlBaseCommand + " -X PUT " + s3Url + "/" + bucketName + " --data '" + createBucketConfigurationStringForRegion(regionName) + "'");

        // assert
        // An anonymous user should not be allowed to create buckets under no circumstance
        assertResponse(commandExecutor.getErrorLines(), "403 Forbidden");
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();
        updateBucketAcl(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);

        // act
        runCommand(curlBaseCommand + " " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 301");

    }

}
