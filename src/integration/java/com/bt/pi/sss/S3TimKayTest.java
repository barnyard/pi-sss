package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.entities.ObjectMetaData;

/*
 * http://timkay.com/aws/
 */
public class S3TimKayTest extends AbstractTestBase {
    private final String timKayBaseCommand = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=Y7HqdJgHC7aYIbnxPRNU1A AWS_SECRET_ACCESS_KEY=sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg etc/timkay-aws/aws --simple --insecure-aws";
    private final String timKayBaseCommandWithBadAccessKey = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=XXXXXXXXXXXXXXXXXXXXXX AWS_SECRET_ACCESS_KEY=sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg etc/timkay-aws/aws --simple --insecure-aws";
    private final String timKayBaseCommandWithBadSecretKey = "touch ~/.awsrc;unset http_proxy;AWS_HOST=localhost AWS_PORT=9090 AWS_ACCESS_KEY_ID=Y7HqdJgHC7aYIbnxPRNU1A AWS_SECRET_ACCESS_KEY=xxxxxxxxxxx__xxxxxxxxxxxxxxxx-xxxxxxxx etc/timkay-aws/aws --simple --insecure-aws";

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        runCommand(timKayBaseCommand + " -vvv ls");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "\ttest1");
        assertResponse(outputLines, "\ttest2");
        assertDate(findLine(outputLines, bucketName).split("\t")[1].replaceAll(" ", "T"));
        assertDate(findLine(outputLines, "test2").split("\t")[1].replaceAll(" ", "T"));
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv mkdir " + bucketName);

        // assert
        assertResponse(commandExecutor.getErrorLines(), "200 OK");
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());
    }

    @Test
    public void testPutBucketMixedCaseAlreadyExists() throws Exception {
        // setup
        runCommand(timKayBaseCommand + " -vvv mkdir " + bucketName);

        // act
        runCommand(timKayBaseCommand + " -vvv mkdir " + "TEst1");

        // assert
        assertResponse(commandExecutor.getErrorLines(), "409 Conflict");
        assertResponse(commandExecutor.getOutputLines(), "BucketAlreadyOwnedByUser");
    }

    @Test
    public void testPutBucketMixedCaseAndListIt() throws Exception {
        // setup
        bucketName = "TEst1";
        runCommand(timKayBaseCommand + " -vvv mkdir " + bucketName);

        // act
        runCommand(timKayBaseCommand + " -vvv ls");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(commandExecutor.getErrorLines(), "200 OK");
        assertResponse(outputLines, bucketName);
        assertTrue(new File(BUCKET_ROOT + "/" + bucketName).exists());
        assertTrue(new File(BUCKET_ROOT + "/" + bucketName).isDirectory());
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(timKayBaseCommand + " -vvv ls " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, "\ttestFile1");
        assertResponse(outputLines, testData.length() + "\t");
        assertDate(findLine(outputLines, "testFile1").split("\t")[1].replaceAll(" ", "T").concat(".000Z"));
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv ls " + "test999");

        // assert
        assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    @Test
    public void testGetBucketNotFoundWithDeletedDhtBucketMetaData() throws Exception {
        // setup
        String deletedBucketName = "test999";
        final BucketMetaData fromName = BucketMetaData.fromName(deletedBucketName);
        fromName.setLocation("EU");
        fromName.setDeleted(true);//
        PId id = piIdbuildler.getPId(fromName);

        dhtCache.update(id, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                if (null == existingEntity)
                    return fromName;
                existingEntity.setCannedAccessPolicy(fromName.getCannedAclPolicy());
                existingEntity.setLocation(fromName.getLocation());
                existingEntity.setDeleted(fromName.isDeleted());
                existingEntity.resetCreationDate();
                return existingEntity;
            }
        });

        // act
        runCommand(timKayBaseCommand + " -vvv ls " + deletedBucketName);

        // assert
        assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        runCommand(timKayBaseCommand + " --xml -vvv get " + bucketName + "?acl");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, "<AccessControlPolicy xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\">");
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        runCommand(timKayBaseCommand + " --xml -vvv get " + bucketName + "?location");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, "<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">UK</LocationConstraint>");
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();

        // act
        runCommand(String.format("%s -vvv get %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath()));

        // assert
        String readFileToString = FileUtils.readFileToString(tmpFile);
        assertEquals(testData, readFileToString);
    }

    @Test
    public void testGetObjectNotFound() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        String bucketDir = String.format("%s/%s", BUCKET_ROOT, bucketName);
        new File(bucketDir).mkdirs();

        // act
        runCommand(String.format("%s -vvv get %s/%s", timKayBaseCommand, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "404 Not Found");
    }

    @Ignore
    @Test
    public void testGetLargeObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        String largeTestData = StringUtils.rightPad("a", 100000, 'a');
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), largeTestData);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{}");
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();

        // act
        runCommand(String.format("%s -vvv get %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath()));

        // assert
        String readFileToString = FileUtils.readFileToString(tmpFile);
        assertEquals(largeTestData, readFileToString);
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
        runCommand(String.format("%s -vvv put %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath));

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

        // act
        runCommand(String.format("%s -vvv put %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath));

        // assert
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

        // act
        runCommand(String.format("%s -vvv --md5 put %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath));

        // assert
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
        runCommand(String.format("%s -vvv delete %s/%s", timKayBaseCommand, bucketName, objectName));

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
        runCommand(String.format("%s -vvv delete %s", timKayBaseCommand, bucketName));

        // assert
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Test
    public void testObjectLifeCycle() throws Exception {
        createBucket(bucketName);
        createObject(bucketName, objectName, testData);
        String result = readObject(bucketName, objectName);
        System.out.println(result);
        assertEquals(testData, result);
    }

    // multiple headers of the same key don't seem to be passed into the service correctly, so can't test that
    @Test
    public void testObjectLifeCycleWithMetaHeaders() throws Exception {
        createBucket(bucketName);
        createObjectWithMetas(bucketName, objectName, testData);
        String result = readObjectWithMetas(bucketName, objectName);
        System.out.println(result);
        assertEquals(testData, result);
    }

    private void createObjectWithMetas(String bucketName, String objectName, String testData) throws Exception {
        String tmpFilePath = writeToTmpFile(testData);
        runCommand(String.format("%s -vvv put \"x-amz-meta-1: a\" \"x-amz-meta-2: b\" \"Content-Type: text/plain\" %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath));
    }

    private void createObject(String bucketName, String objectName, String testData) throws Exception {
        String tmpFilePath = writeToTmpFile(testData);
        runCommand(String.format("%s -vvv put \"Content-Type: text/plain\" %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFilePath));
    }

    private void createBucket(String bucketName) throws Exception {
        runCommand(String.format("%s -vvv mkdir %s", timKayBaseCommand, bucketName));
    }

    private String readObject(String bucketName, String objectName) throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();
        runCommand(String.format("%s -vvv get %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath()));
        assertResponse(commandExecutor.getErrorLines(), "text/plain");
        return FileUtils.readFileToString(tmpFile);
    }

    private String readObjectWithMetas(String bucketName, String objectName) throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();
        runCommand(String.format("%s -vvv get %s/%s %s", timKayBaseCommand, bucketName, objectName, tmpFile.getAbsolutePath()));
        assertResponse(commandExecutor.getErrorLines(), "text/plain");
        assertResponse(commandExecutor.getErrorLines(), "x-amz-meta-1: a");
        assertResponse(commandExecutor.getErrorLines(), "x-amz-meta-2: b");
        return FileUtils.readFileToString(tmpFile);
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommandWithBadSecretKey + " -vvv ls");

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 401");
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommandWithBadAccessKey + " -vvv ls");

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 401");
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup
        String regionName = "RuyLopez";
        setupDefaultRegions();

        // act

        runCommand(timKayBaseCommand + " -vvv mkdir " + bucketName + " " + createBucketConfigurationFileForRegionAndReturnFilename(regionName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "200 OK");
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

        deleteBucketConfigurationFileForRegion(regionName);

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();

        // act
        runCommand(timKayBaseCommand + " --xml -vvv ls " + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 301");

    }

    private String createBucketConfigurationFileForRegionAndReturnFilename(String regionName) throws IOException {

        String fileName = BUCKET_ROOT + "/" + regionName + ".xml";
        File createBucketConfigurationFile = new File(fileName);
        createBucketConfigurationFile.createNewFile();
        FileUtils.writeStringToFile(createBucketConfigurationFile, createBucketConfigurationStringForRegion(regionName));

        return fileName;
    }

    private void deleteBucketConfigurationFileForRegion(String regionName) {
        FileUtils.deleteQuietly(new File(BUCKET_ROOT + "/" + regionName + ".xml"));
    }
}
