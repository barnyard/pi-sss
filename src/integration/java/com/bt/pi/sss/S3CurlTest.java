package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/*
 * http://open.eucalyptus.com/wiki/s3curl
 */
public class S3CurlTest extends AbstractTestBase {
    protected String s3CurlBaseCommand = String.format("unset http_proxy; etc/s3curl --id %s --key %s", ACCESS_KEY, SECRET_KEY);
    protected String s3CurlBaseCommandWithBadAccessKey = String.format("unset http_proxy; etc/s3curl --id %s --key %s", BAD_ACCESS_KEY, SECRET_KEY);
    protected String s3CurlBaseCommandWithBadSecretKey = String.format("unset http_proxy; etc/s3curl --id %s --key %s", ACCESS_KEY, BAD_SECRET_KEY);

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        String xml = getXmlFromLines(outputLines);
        System.out.println(xml);
        assertTrue(xml.contains("<Owner><ID>" + ACCESS_KEY + "</ID><DisplayName>" + USER_NAME + "</DisplayName></Owner>"));
        assertTrue(xml.contains("<Bucket><Name>test1</Name><CreationDate>"));
        assertTrue(xml.contains("<Bucket><Name>test2</Name><CreationDate>"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        Document doc = str2Dom(xml);
        assertTrue(doc.getDocumentElement().getTagName().contains("ListAllMyBucketsResult"));
        NodeList nodeList = (NodeList) xpath.evaluate("//CreationDate", doc, XPathConstants.NODESET);
        assertEquals(2, nodeList.getLength());
        String d1 = nodeList.item(0).getTextContent();
        String d2 = nodeList.item(1).getTextContent();
        assertDate(d1);
        assertDate(d2);
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url + "/" + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        String xml = getXmlFromLines(outputLines);
        System.out.println(xml);
        Document doc = str2Dom(xml);
        assertTrue(doc.getDocumentElement().getTagName().contains("ListBucketResult"));
        assertTrue(xml.contains("<Name>test1</Name><MaxKeys>0</MaxKeys><IsTruncated>false</IsTruncated><Contents><Key>testFile1</Key><LastModified>"));
        assertTrue(xml.contains("<Size>" + testData.length() + "</Size></Contents>"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        String lastModified = (String) xpath.evaluate("//LastModified", doc, XPathConstants.STRING);
        assertDate(lastModified);
    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        // setup
        createBucketMetaDataInRegion(USER_NAME, bucketName, "RuyLopez");
        createFilePair();

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url + "/" + bucketName);

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertResponse(errorLines, "HTTP/1.1 301");

    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url + "/" + bucketName + "?acl");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        String xml = getXmlFromLines(outputLines);
        System.out.println(xml);
        assertTrue(xml.contains("<AccessControlPolicy xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\">"));
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url + "/" + bucketName + "?location");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        String xml = getXmlFromLines(outputLines);
        System.out.println(xml);
        Document doc = str2Dom(xml);
        assertTrue(doc.getDocumentElement().getTagName().contains("LocationConstraint"));
        assertEquals(bucketLocation, doc.getDocumentElement().getTextContent());
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        runCommand(s3CurlBaseCommand + " --put /dev/null -- -s -v " + s3Url + "/" + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 200 OK");

        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());
    }

    @Override
    public void testPutBucketWithRegion() throws Exception {
        // setup
        String regionString = "RuyLopez";
        setupDefaultRegions();
        // act
        runCommand(s3CurlBaseCommand + " --contentType text/plain --createBucket \"" + regionString + "\" -- -s -v " + s3Url + "/" + bucketName);

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 200 OK");

        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        runCommand(s3CurlBaseCommand + " --get -- -s -v " + s3Url + "/test99");

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
        runCommand(String.format("%s -- %s/%s/%s -o %s", s3CurlBaseCommand, s3Url, bucketName, objectName, tmpFile.getAbsolutePath()));

        // assert
        String readFileToString = FileUtils.readFileToString(tmpFile);
        assertEquals(testData, readFileToString);
    }

    @Override
    public void testHeadObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        runCommand(String.format("%s --head -- %s/%s/%s", s3CurlBaseCommand, s3Url, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getOutputLines(), "Content-Type: binary/octet-stream");
        assertResponse(commandExecutor.getOutputLines(), "Content-Length: 27");
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));
        String tmpFilePath = writeToTmpFile(testData);

        // act
        runCommand(String.format("%s --put %s -- -s -v %s/%s/%s", s3CurlBaseCommand, tmpFilePath, s3Url, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 200 OK");
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
        runCommand(String.format("%s --put %s -- -s -v %s/%s/%s", s3CurlBaseCommand, tmpFilePath, s3Url, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 200 OK");
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
        runCommand(String.format("%s --put %s --contentMD5 %s -- -s -v %s/%s/%s", s3CurlBaseCommand, tmpFilePath, md5, s3Url, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 200 OK");
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
        runCommand(String.format("%s --delete -- -s -v %s/%s/%s", s3CurlBaseCommand, s3Url, bucketName, objectName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 204 No Content");
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        runCommand(String.format("%s --delete -- -s -v %s/%s", s3CurlBaseCommand, s3Url, bucketName));

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 204 No Content");
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    private Document str2Dom(String xml) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        return doc;
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup

        // act
        runCommand(s3CurlBaseCommandWithBadSecretKey + " --get -- -s -v " + s3Url);

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 401");
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // setup

        // act
        runCommand(s3CurlBaseCommandWithBadAccessKey + " --get -- -s -v " + s3Url);

        // assert
        assertResponse(commandExecutor.getErrorLines(), "HTTP/1.1 401");
    }
}
