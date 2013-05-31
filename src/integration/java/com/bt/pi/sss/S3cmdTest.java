package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/*
 * http://s3.amazonaws.com/ServEdge_pub/s3sync/README_s3cmd.txt
 */
public class S3cmdTest extends AbstractTestBase {
    private final String s3cmdBaseCommand = "S3CONF=etc/s3cmdconf/default etc/s3sync/s3cmd.rb --debug --verbose";
    private final String s3cmdBaseCommandBadAccessKey = "S3CONF=etc/s3cmdconf/badaccesskey etc/s3sync/s3cmd.rb --debug --verbose";
    private final String s3cmdBaseCommandBadSecretKey = "S3CONF=etc/s3cmdconf/badsecretkey etc/s3sync/s3cmd.rb --debug --verbose";

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        runCommand(s3cmdBaseCommand + " listbuckets");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, bucketName);
        assertResponse(outputLines, "test2");
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(s3cmdBaseCommand + " -v list " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, objectName);
    }

    // this test shows that currently we ignore the ?location in the URL but still correctly authenticate
    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(s3cmdBaseCommand + " -v location " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");
        assertResponse(outputLines, "<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" + bucketLocation + "</LocationConstraint>");
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // TODO: not sure s3cmd.rb can do ACL ?
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        runCommand(s3cmdBaseCommand + " -v createbucket " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");

        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        runCommand(s3cmdBaseCommand + " -v list test99");

        // assert
        assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();

        // act
        runCommand(String.format("%s get %s:%s %s", s3cmdBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath()));

        // assert
        String readFileToString = FileUtils.readFileToString(tmpFile);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testHeadObject() throws Exception {
        // not supported by client tool?
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);

        // act
        runCommand(String.format("%s put %s:%s %s", s3cmdBaseCommand, bucketName, objectName, tmpFilePath));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");
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
        String tmpFilePath = writeToTmpFile(largeTestData);

        // act
        runCommand(String.format("%s put %s:%s %s", s3cmdBaseCommand, bucketName, objectName, tmpFilePath));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);
        String md5 = new String(Base64.encodeBase64(DigestUtils.md5(testData.getBytes())));

        // act
        runCommand(String.format("%s put %s:%s %s content-md5:%s", s3cmdBaseCommand, bucketName, objectName, tmpFilePath, md5));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");
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
        runCommand(String.format("%s delete %s:%s", s3cmdBaseCommand, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "Response code: 204");
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(String.format("%s deletebucket %s", s3cmdBaseCommand, bucketName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "Response code: 204");
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup

        // act
        runCommand(s3cmdBaseCommandBadSecretKey + " listbuckets");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "Response code: 401");
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // setup

        // act
        runCommand(s3cmdBaseCommandBadAccessKey + " listbuckets");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "Response code: 401");
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup

        // act
        runCommand(s3cmdBaseCommand + " -d -v createbucket " + bucketName + " RuyLopez");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "Response code: 200");

        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {

        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();

        // act
        runCommand(s3cmdBaseCommand + " list " + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "301");

    }

}
