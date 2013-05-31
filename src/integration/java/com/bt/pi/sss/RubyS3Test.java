package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;

public class RubyS3Test extends AbstractTestBase {

    private List<String> script;
    private File scriptFile;

    @Before
    public void before() {
        super.before();
        script = new ArrayList<String>();
        script.add("#!/usr/bin/env ruby");
        File etc = new File("etc");
        script.add(String.format("require '%s/aws-s3-0.6.2/lib/aws/s3'", etc.getAbsolutePath()));
        script.add("include AWS::S3");
        script.add(String.format("Base.establish_connection!(:server => 'localhost', :port => '%s', :access_key_id => '%s', :secret_access_key => '%s')", PORT, ACCESS_KEY, SECRET_KEY));
    }

    @After
    public void after() throws Exception {
        if (null != this.scriptFile)
            this.scriptFile.delete();
        super.after();
    }

    private void runCommand() throws Exception {
        scriptFile = File.createTempFile("unittesting", ".sh");
        scriptFile.setExecutable(true);
        FileUtils.writeLines(scriptFile, script);

        System.out.println(FileUtils.readFileToString(scriptFile));

        String command = String.format("%s", scriptFile.getAbsolutePath());
        String[] commands = new String[] { "/bin/bash", "-c", command };
        commandExecutor.executeScript(commands, Runtime.getRuntime());
    }

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        script.add("buckets = Service.buckets");
        script.add("buckets.each{|bucket| puts bucket.name }");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), "test1");
        assertResponse(commandExecutor.getOutputLines(), "test2");
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup
        script.add(String.format("result = Bucket.create('%s')", bucketName));
        script.add("puts result");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);

        script.add(String.format("result = S3Object.store('%s', open('%s'), '%s')", objectName, tmpFilePath, bucketName));
        script.add("puts result");

        // act
        runCommand();

        // assert
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

        script.add(String.format("result = S3Object.store('%s', open('%s'), '%s')", objectName, tmpFilePath, bucketName));
        script.add("puts result");

        // act
        runCommand();

        // assert
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        script.add(String.format("result = Bucket.delete('%s')", bucketName));
        script.add("puts result");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        // assertEquals(0, bucketMetaDataCollection.get(USER_NAME).size());
    }

    @Override
    public void testDeleteObject() throws Exception {
        // setup
        String objectName = "testFile1";
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);

        script.add(String.format("result = S3Object.delete('%s', '%s')", objectName, bucketName));
        script.add("puts result");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        script.add(String.format("bucket = Bucket.find('%s')", bucketName));
        script.add("bucket.objects.each{|object| puts object.key }");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), objectName);
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        script.add(String.format("bucket = Bucket.find('%s')", bucketName));
        script.add("p bucket.acl");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getOutputLines(), "FULL_CONTROL to bob");
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // TODO
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        script.add("bucket = Bucket.find('bogus')");
        script.add("p bucket");

        // act
        runCommand();

        // assert
        assertResponse(commandExecutor.getErrorLines(), "AWS::S3::NoSuchBucket");
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        script.add(String.format("object = S3Object.find('%s', '%s')", objectName, bucketName));
        script.add("puts object.value");

        // act
        runCommand();

        // assert
        for (String line : testData.split("\n")) {
            assertResponse(commandExecutor.getOutputLines(), line);
        }
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void testHeadObject() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // TODO Auto-generated method stub

    }

}
