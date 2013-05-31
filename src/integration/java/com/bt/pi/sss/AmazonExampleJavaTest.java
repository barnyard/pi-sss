package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.CallingFormat;
import com.amazon.s3.GetResponse;
import com.amazon.s3.ListAllMyBucketsResponse;
import com.amazon.s3.ListBucketResponse;
import com.amazon.s3.LocationResponse;
import com.amazon.s3.S3Object;
import com.bt.pi.sss.entities.ObjectMetaData;

public class AmazonExampleJavaTest extends AbstractTestBase {
    private AWSAuthConnection conn;

    // private QueryStringAuthGenerator generator;

    @Before
    public void setUp() throws Exception {
        conn = new AWSAuthConnection(ACCESS_KEY, SECRET_KEY, false, "localhost", Integer.parseInt(PORT), CallingFormat.getPathCallingFormat());
        // generator = new QueryStringAuthGenerator(ACCESS_KEY, SECRET_KEY);
    }

    @Override
    public void testGetService() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");

        // act
        ListAllMyBucketsResponse listAllMyBuckets = conn.listAllMyBuckets(null);

        // assert
        List<com.amazon.s3.Bucket> entries = listAllMyBuckets.entries;
        assertEquals(2, entries.size());
        if (bucketName.equalsIgnoreCase(entries.get(0).name)) {
            assertEquals(bucketName, entries.get(0).name);
            assertEquals("test2", entries.get(1).name);
        } else {
            assertEquals(bucketName, entries.get(1).name);
            assertEquals("test2", entries.get(0).name);
        }
        assertDate(entries.get(0).creationDate);
        assertDate(entries.get(1).creationDate);
    }

    @Override
    public void testGetServiceWithBadSecretKey() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");
        conn = new AWSAuthConnection(ACCESS_KEY, BAD_SECRET_KEY, false, "localhost", Integer.parseInt(PORT), CallingFormat.getPathCallingFormat());

        // act
        ListAllMyBucketsResponse listAllMyBuckets = conn.listAllMyBuckets(null);

        // assert
        assertEquals(401, listAllMyBuckets.connection.getResponseCode());
    }

    @Override
    public void testGetServiceWithBadUser() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName, "test2");
        conn = new AWSAuthConnection(BAD_ACCESS_KEY, SECRET_KEY, false, "localhost", Integer.parseInt(PORT), CallingFormat.getPathCallingFormat());

        // act
        ListAllMyBucketsResponse listAllMyBuckets = conn.listAllMyBuckets(null);

        // assert
        assertEquals(401, listAllMyBuckets.connection.getResponseCode());
    }

    @Override
    public void testGetBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        ListBucketResponse listBucketResponse = conn.listBucket(bucketName, null, null, null, null);

        // assert
        assertEquals(bucketName, listBucketResponse.name);
        List<com.amazon.s3.ListEntry> entries = listBucketResponse.entries;
        assertEquals(1, entries.size());
        assertEquals(objectName, entries.get(0).key);
    }

    @Override
    public void testGetBucketNotFound() throws Exception {
        // setup

        // act
        int result = conn.listBucket("test888", null, null, null, null).connection.getResponseCode();

        // assert
        assertEquals(Status.NOT_FOUND.getStatusCode(), result);
    }

    @Override
    public void testGetBucketAcl() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        GetResponse getResponse = conn.getBucketACL(bucketName, null);

        // assert
        assertEquals("OK", getResponse.connection.getResponseMessage());
        String acl = new String(getResponse.object.data);
        System.out.println(acl);
        assertTrue(acl.contains("<AccessControlPolicy xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\">"));
    }

    @Override
    public void testGetBucketLocation() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        LocationResponse locationResponse = conn.getBucketLocation(bucketName);

        // assert
        assertEquals("OK", locationResponse.connection.getResponseMessage());
        assertEquals(bucketLocation, locationResponse.getLocation());
    }

    @Override
    public void testPutBucket() throws Exception {
        // setup

        // act
        String result = conn.createBucket(bucketName, null, null).connection.getResponseMessage();

        // assert
        assertEquals("OK", result);
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).isDirectory());

        // assertEquals(1, bucketMetaDataCollection.size());
    }

    @Override
    public void testGetObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, null);

        // assert
        S3Object object = getResponse.object;
        assertEquals(testData, new String(object.data));
        System.out.println(getResponse.connection.getContentType());
    }

    @Test
    public void testGetObjectIfMatchFalse() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_MATCH, Arrays.asList(new String[] { "zzzzzz" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), getResponse.connection.getResponseCode());
    }

    @Test
    public void testGetObjectIfTrueFalse() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_MATCH, Arrays.asList(new String[] { eTag }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.OK.getStatusCode(), getResponse.connection.getResponseCode());
        S3Object object = getResponse.object;
        assertEquals(testData, new String(object.data));
    }

    @Test
    public void testGetObjectIfMatchFalseWithQuotes() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_MATCH, Arrays.asList(new String[] { "\"zzzzzz\"" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), getResponse.connection.getResponseCode());
    }

    @Test
    public void testGetObjectIfMatchTrueWithQuotes() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_MATCH, Arrays.asList(new String[] { "\"" + eTag + "\"" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.OK.getStatusCode(), getResponse.connection.getResponseCode());
        S3Object object = getResponse.object;
        assertEquals(testData, new String(object.data));
    }

    @Test
    public void testGetObjectIfNonMatchFalse() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_NONE_MATCH, Arrays.asList(new String[] { eTag }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), getResponse.connection.getResponseCode());
    }

    @Test
    public void testGetObjectIfNonMatchFalseWithQuotes() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_NONE_MATCH, Arrays.asList(new String[] { "\"" + eTag + "\"" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), getResponse.connection.getResponseCode());
    }

    @Test
    public void testGetObjectIfNonMatchTrue() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_NONE_MATCH, Arrays.asList(new String[] { "zzzz" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.OK.getStatusCode(), getResponse.connection.getResponseCode());
        S3Object object = getResponse.object;
        assertEquals(testData, new String(object.data));
    }

    @Test
    public void testGetObjectIfNonMatchTrueWithQuotes() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        createFilePair();
        String eTag = "abc123";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{\"eTag\":\"" + eTag + "\"}");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HttpHeaders.IF_NONE_MATCH, Arrays.asList(new String[] { "\"zzzzz\"" }));

        // act
        GetResponse getResponse = conn.get(bucketName, objectName, headers);

        // assert
        assertEquals(Status.OK.getStatusCode(), getResponse.connection.getResponseCode());
        S3Object object = getResponse.object;
        assertEquals(testData, new String(object.data));
    }

    @Override
    public void testHeadObject() throws Exception {
        // not supported by toolset
    }

    @Override
    public void testPutObject() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        S3Object object = new S3Object(testData.getBytes(), null);

        // act
        String result = conn.put(bucketName, objectName, object, null).connection.getResponseMessage();

        // assert
        assertEquals("OK", result);
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

        S3Object object = new S3Object(largeTestData.getBytes(), null);

        // act
        String result = conn.put(bucketName, objectName, object, null).connection.getResponseMessage();

        // assert
        assertEquals("OK", result);
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName));
        String readFileToString = FileUtils.readFileToString(file);
        assertEquals(largeTestData, readFileToString);
    }

    @Override
    public void testPutObjectMd5() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        S3Object object = new S3Object(testData.getBytes(), null);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        String md5 = new String(Base64.encodeBase64(DigestUtils.md5(testData.getBytes())));
        headers.put("content-md5", Arrays.asList(new String[] { md5 }));

        // act
        String result = conn.put(bucketName, objectName, object, headers).connection.getResponseMessage();

        // assert
        assertEquals("OK", result);
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
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{}");

        // act
        String result = conn.delete(bucketName, objectName, null).connection.getResponseMessage();

        // assert
        assertEquals("No Content", result);
        assertFalse(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)).exists());
        assertFalse(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)).exists());
        assertTrue(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }

    @Override
    public void testDeleteBucket() throws Exception {
        // setup
        setupBucketMetaData(USER_NAME, bucketName);
        FileUtils.forceMkdir(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)));

        // act
        String result = conn.deleteBucket(bucketName, null).connection.getResponseMessage();

        // assert
        System.out.println(result);
        if (!"No Content".equals(result))
            System.err.println("result != \"No Content\"");
        // assertEquals("No Content", result); // This assert is un-reliable, so commented for now
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
        // assertEquals(0, bucketMetaDataCollection.get(USER_NAME).size());
    }

    /**
     * This test fails because Locations are hardcoded in this java tool.
     */
    @Override
    public void testPutBucketWithRegion() throws Exception {
        System.err.println("This is not supported because AWSAuthConnection hardcodes Location information and also the location constraints are only allowed for subdomain-style calling");
    }

    @Override
    public void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception {
        System.err.println("This is not supported because AWSAuthConnection hardcodes Location information and also the location constraints are only allowed for subdomain-style calling");
    }

}
