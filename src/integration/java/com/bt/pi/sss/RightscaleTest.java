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
 * http://rightscale.rubyforge.org/right_aws_gem_doc/
 */
public class RightscaleTest extends AbstractTestBase {

    private final String rightscaleBaseCommandWithGoodUser = "etc/rightscaleclient.rb " + ACCESS_KEY + " " + SECRET_KEY;
    private final String rightscaleBaseCommandWithBadKey = "etc/rightscaleclient.rb " + BAD_ACCESS_KEY + " " + SECRET_KEY;
    private final String rightscaleBaseCommandWithBadSecret = "etc/rightscaleclient.rb " + ACCESS_KEY + " " + BAD_SECRET_KEY;

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " list_all_my_buckets");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, ":name=>\"test1\"");
        assertResponse(outputLines, ":name=>\"test2\"");
        String d1 = getDateFromLine(findLine(outputLines, "creation_date"));
        assertDate(d1);
        String d2 = getDateFromLine(findLine(outputLines, "creation_date").split("\\}, \\{")[1]);
        assertDate(d2);
    }

    private String getDateFromLine(String line) {
        int start = line.indexOf(":creation_date=>\"") + ":creation_date=>\"".length();
        int end = line.indexOf("\"", start);
        return line.substring(start, end);
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " list_bucket " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, ":key=>\"testFile1\"");
        assertResponse(outputLines, ":size=>" + testData.length());
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " get_acl " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, ":object=>\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"yes\\\"?><AccessControlPolicy xmlns=\\\"http://doc.s3.amazonaws.com/2006-03-01\\\">");
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " bucket_location " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(outputLines, bucketLocation);
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // setup

        // act
        runCommand(rightscaleBaseCommandWithBadKey + " list_bucket " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "returned an error: 401 ");
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup

        // act
        runCommand(rightscaleBaseCommandWithBadSecret + " list_bucket " + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "returned an error: 401 ");
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " create_bucket " + bucketName);

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " list_bucket test99");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "404 Not Found");
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " get " + bucketName + " " + objectName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        String actualResult = getHashMember(outputLines, "object").replaceAll("\\\\n", System.getProperty("line.separator"));
        assertEquals(testData, actualResult);
    }

    @Override
    public void testHeadObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " head " + bucketName + " " + objectName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "\"content-type\"=>\"binary/octet-stream\"");
        assertResponse(outputLines, "\"content-length\"=>\"" + testData.length() + "\"");
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(String.format("%s put %s %s \"%s\"", rightscaleBaseCommandWithGoodUser, bucketName, objectName, testData));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(testData, readFileToString);
    }

    // this test shows that Rightscale fails when the size of the file exceeds 999999 bytes
    @Override
    public void testPutLargeObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String largeTestData = StringUtils.rightPad("a", 999999, 'a');
        String tmpFilePath = writeToTmpFile(largeTestData);

        // act
        runCommand(String.format("%s put %s %s \"File.open('%s')\"", rightscaleBaseCommandWithGoodUser, bucketName, objectName, tmpFilePath));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String md5 = new String(Base64.encodeBase64(DigestUtils.md5(testData.getBytes())));

        // act
        runCommand(String.format("%s put %s %s \"%s\" \"{'content-md5'=>'%s'}\"", rightscaleBaseCommandWithGoodUser, bucketName, objectName, testData, md5));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
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
        runCommand(String.format("%s delete %s %s ", rightscaleBaseCommandWithGoodUser, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(String.format("%s delete_bucket %s", rightscaleBaseCommandWithGoodUser, bucketName));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    private String getHashMember(List<String> lines, String key) {
        String target = key + "=>";
        for (String line : lines) {
            if (line.contains(target)) {
                int start = line.indexOf(target) + target.length() + 1;
                int end = line.indexOf("\"", start);
                return line.substring(start, end);
            }
        }
        return null;
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup
        String regionName = "RuyLopez";
        // act
        runCommand(rightscaleBaseCommandWithGoodUser + " create_bucket " + bucketName + " \":location=>'" + regionName + "'\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "true");
        System.err.println("OUTPUT: " + commandExecutor.getOutputLines());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();

        // act

        runCommand(rightscaleBaseCommandWithGoodUser + " list_bucket " + bucketName);
        assertResponse(commandExecutor.getOutputLines(), "301 Moved Permanently");
    }

}
