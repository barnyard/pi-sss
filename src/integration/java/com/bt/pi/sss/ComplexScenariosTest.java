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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.CallingFormat;
import com.amazon.s3.GetResponse;
import com.amazon.s3.ListAllMyBucketsResponse;
import com.amazon.s3.ListBucketResponse;
import com.amazon.s3.S3Object;
import com.bt.pi.sss.entities.ObjectMetaData;

/*
 * this class is for more complex scenarios that are not client dependent, but exercise the service in a more complex way with
 * chained calls etc.
 */
public class ComplexScenariosTest extends IntegrationTestBase {
    private AWSAuthConnection conn;
    private static final String ANOTHER_SECRET_KEY = "aaaaaaaaaaa__Oa7Oe5Ssuhjalg6m-cd0RDwOg";
    private static final String ANOTHER_ACCESS_KEY = "aaaaaaaaaaaYIbnxPRNU1A";
    private AWSAuthConnection anotherConn;
    private String objectUri = String.format("http://localhost:%s/%s/%s", PORT, bucketName, objectName);
    @Rule
    public TestName testname = new TestName();

    @Before
    public void setUp() throws Exception {
        conn = new AWSAuthConnection(ACCESS_KEY, SECRET_KEY, false, "localhost", Integer.parseInt(PORT), CallingFormat.getPathCallingFormat());
        anotherConn = new AWSAuthConnection(ANOTHER_ACCESS_KEY, ANOTHER_SECRET_KEY, false, "localhost", Integer.parseInt(PORT), CallingFormat.getPathCallingFormat());
        System.err.println("#### RUNNING TEST " + testname.getMethodName() + " #######");
    }

    // 2 tests for the duplicate content-length header problem in Grizzly that we override by putting
    // an amended class ahead in the classpath
    @Ignore
    @Test
    public void testDoubleContentLengthHeadersSame() throws Exception {
        // setup
        HttpClient httpClient = new HttpClient();
        PutMethod putMethod = new PutMethod("http://localhost:" + PORT);
        putMethod.addRequestHeader("Content-Length", "0");
        putMethod.addRequestHeader("content-length", "0");

        // act
        int rc = httpClient.executeMethod(putMethod);

        // assert
        assertFalse(400 == rc);
    }

    @Ignore
    @Test
    public void testDoubleContentLengthHeadersDifferent() throws Exception {
        // setup
        HttpClient httpClient = new HttpClient();
        PutMethod putMethod = new PutMethod("http://localhost:" + PORT);
        putMethod.addRequestHeader("Content-Length", "0");
        putMethod.addRequestHeader("content-length", "2");

        // act
        int rc = httpClient.executeMethod(putMethod);

        // assert
        assertEquals(400, rc);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContentTypeRoundTrip() throws Exception {
        // act
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        Map headers = new HashMap();
        String contentType = "text/plain";
        headers.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(new String[] { contentType }));
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, headers).connection.getResponseMessage());
        GetResponse getResponse = conn.get(bucketName, objectName, null);
        conn.delete(bucketName, objectName, headers);
        // assert
        S3Object result = getResponse.object;
        assertEquals(testData, new String(result.data));
        assertEquals(contentType, getResponse.connection.getContentType());
    }

    // multiple headers of the same key don't seem to be passed into the service correctly, so can't test that
    @SuppressWarnings("unchecked")
    @Test
    public void testMetaHeaderRoundTrip() throws Exception {
        // act
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        Map headers = new HashMap();
        String metaHeader1 = "x-amz-meta-1";
        headers.put(metaHeader1, Arrays.asList(new String[] { "a" }));
        String metaHeader2 = "x-amz-meta-2";
        headers.put(metaHeader2, Arrays.asList(new String[] { "b" }));
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, headers).connection.getResponseMessage());
        GetResponse getResponse = conn.get(bucketName, objectName, null);

        // assert
        S3Object result = getResponse.object;
        assertEquals(testData, new String(result.data));
        Map<String, List<String>> headerFields = getResponse.connection.getHeaderFields();
        assertTrue(headerFields.containsKey(metaHeader1));
        assertEquals(1, headerFields.get(metaHeader1).size());
        assertTrue(headerFields.get(metaHeader1).contains("a"));

        assertTrue(headerFields.containsKey(metaHeader2));
        assertEquals(1, headerFields.get(metaHeader2).size());
        assertTrue(headerFields.get(metaHeader2).contains("b"));
    }

    @Test
    public void testETag() throws Exception {
        // act
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());
        GetResponse getResponse = conn.get(bucketName, objectName, null);

        // assert
        S3Object result = getResponse.object;
        assertEquals(testData, new String(result.data));

        String expectedResult = "\"" + Hex.encodeHexString(DigestUtils.md5(testData.getBytes())) + "\"";
        assertEquals(expectedResult, getResponse.connection.getHeaderField(HttpHeaders.ETAG));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContentDispositionRoundTrip() throws Exception {
        // act
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        Map headers = new HashMap();
        String contentDisposition = "some stuff";
        headers.put("Content-Disposition", Arrays.asList(new String[] { contentDisposition }));
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, headers).connection.getResponseMessage());
        GetResponse getResponse = conn.get(bucketName, objectName, null);

        // assert
        S3Object result = getResponse.object;
        assertEquals(testData, new String(result.data));
        assertEquals(contentDisposition, getResponse.connection.getHeaderField("Content-Disposition"));
    }

    @Test
    public void testObjectNameEscapingRoundTrip() throws Exception {
        // act
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        String key = "fred/bloggs";
        assertEquals("OK", conn.put(bucketName, key, object, null).connection.getResponseMessage());
        GetResponse getResponse = conn.get(bucketName, key, null);

        ListBucketResponse listBucketResponse1 = conn.listBucket(bucketName, null, null, null, null);
        List<com.amazon.s3.ListEntry> entries1 = listBucketResponse1.entries;
        S3Object result = getResponse.object;
        ListBucketResponse listBucketResponse2 = conn.listBucket(bucketName, "fred/", null, null, null);
        List<com.amazon.s3.ListEntry> entries2 = listBucketResponse2.entries;

        // assert
        assertEquals(1, entries1.size());
        assertEquals(key, entries1.get(0).key);
        assertEquals(testData, new String(result.data));
        assertEquals(1, entries2.size());
        assertEquals(key, entries2.get(0).key);
    }

    @Test
    public void testBucketLevelAclGetEnforcementForAnonymousUser() throws Exception {
        // create bucket and store object
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());

        // anon access should fail
        int rc1 = getFile(objectUri);
        assertEquals(403, rc1);

        // grant public read access to the bucket
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "public-read" }));
        assertEquals("OK", conn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon access should now work OK
        int rc2 = getFile(objectUri);
        assertEquals(200, rc2);
    }

    @Test
    public void testHeadObject() throws Exception {
        // create bucket and store object
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());

        // grant public read access to the bucket
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "public-read" }));
        assertEquals("OK", conn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        HttpClient httpClient = new HttpClient();
        HeadMethod headMethod = new HeadMethod(objectUri);
        int rc = httpClient.executeMethod(headMethod);
        assertEquals(200, rc);
        System.out.println(FileUtils.readFileToString(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX))));
        assertEquals(Integer.toString(testData.length()), headMethod.getResponseHeaders(HttpHeaders.CONTENT_LENGTH)[0].getValue());
        assertEquals(ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE, headMethod.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    @Test
    public void testBucketLevelAclGetEnforcementForAuthenticatedUser() throws Exception {
        // create bucket and store object
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());

        // anon access should fail
        int rc1 = getFile(objectUri);
        assertEquals(403, rc1);

        // auth access from another user should fail
        assertEquals(403, this.anotherConn.get(bucketName, objectName, null).connection.getResponseCode());
        assertEquals(403, this.anotherConn.put(bucketName, "anotherObject", object, null).connection.getResponseCode());

        // grant authenticated read access to the bucket
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "authenticated-read" }));
        assertEquals("OK", conn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon access should still fail
        int rc2 = getFile(objectUri);
        assertEquals(403, rc2);

        // authenticated access should now work
        GetResponse getResponse = this.anotherConn.get(bucketName, objectName, null);
        assertEquals(200, getResponse.connection.getResponseCode());
        assertEquals(testData, new String(getResponse.object.data));

        // put should still fail
        assertEquals(403, this.anotherConn.put(bucketName, "anotherObject", object, null).connection.getResponseCode());
    }

    @Test
    public void testBucketLevelAclCannotBeUpdatedByAnotherAuthenticatedUser() throws Exception {
        // create bucket
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());

        // try and update the bucket ACL from another user - should fail
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "authenticated-read" }));
        assertEquals("Conflict", anotherConn.createBucket(bucketName, null, headers).connection.getResponseMessage());
    }

    @Test
    public void testBucketLevelAclCanBeUpdatedByAnotherAuthenticatedUserWhenGrantedPublicReadWrite() throws Exception {
        // create bucket
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());

        // try and update the bucket ACL from another user - should fail
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "authenticated-read" }));
        assertEquals("Conflict", anotherConn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon access should fail
        assertEquals(403, getFile(objectUri));

        // grant public read-write access to the bucket
        headers.put("x-amz-acl", Arrays.asList(new String[] { "public-read-write" }));
        assertEquals("OK", conn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon read access should work
        assertEquals(200, getFile(objectUri));

        // try and update the bucket ACL from another user - should now work
        headers.put("x-amz-acl", Arrays.asList(new String[] { "private" }));
        assertEquals("OK", anotherConn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon read access should now fail again
        assertEquals(403, getFile(objectUri));

        // as should authenticated read
        assertEquals(403, this.anotherConn.get(bucketName, objectName, null).connection.getResponseCode());

        // bucket owner should still be OK
        assertEquals(200, this.conn.get(bucketName, objectName, null).connection.getResponseCode());
    }

    @Test
    public void testBucketLevelAclPutEnforcementForAnonymousUser() throws Exception {
        // create bucket and store object
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());

        // anon access should fail
        int rc1 = getFile(objectUri);
        assertEquals(403, rc1);

        // auth access from another user should fail
        assertEquals(403, this.anotherConn.get(bucketName, objectName, null).connection.getResponseCode());
        assertEquals(403, this.anotherConn.put(bucketName, "anotherObject", object, null).connection.getResponseCode());

        // grant public read-write access to the bucket
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-acl", Arrays.asList(new String[] { "public-read-write" }));
        assertEquals("OK", conn.createBucket(bucketName, null, headers).connection.getResponseMessage());

        // anon access should now work
        int rc2 = getFile(objectUri);
        assertEquals(200, rc2);

        // authenticated access should now work
        GetResponse getResponse = this.anotherConn.get(bucketName, objectName, null);
        assertEquals(200, getResponse.connection.getResponseCode());
        assertEquals(testData, new String(getResponse.object.data));

        // put should also now work
        assertEquals(200, this.anotherConn.put(bucketName, "anotherObject", object, null).connection.getResponseCode());

        // now list objects
        ListBucketResponse listBucketResponse = this.anotherConn.listBucket(bucketName, null, null, null, null);
        List<com.amazon.s3.ListEntry> entries = listBucketResponse.entries;
        assertEquals(2, entries.size());
        assertEquals("anotherObject", entries.get(0).key);
        assertEquals(objectName, entries.get(1).key);
    }

    @Test
    public void testBucketAndObjectLifeCycle() throws Exception {
        // create bucket and store object
        assertEquals("OK", conn.createBucket(bucketName, null, null).connection.getResponseMessage());
        S3Object object = new S3Object(testData.getBytes(), null);
        assertEquals("OK", conn.put(bucketName, objectName, object, null).connection.getResponseMessage());
        String anotherObjectName = "anotherObjectName";
        assertEquals("OK", conn.put(bucketName, anotherObjectName, object, null).connection.getResponseMessage());

        // list bucket
        ListBucketResponse listBucketResponse = conn.listBucket(bucketName, null, null, null, null);
        List<com.amazon.s3.ListEntry> entries = listBucketResponse.entries;
        assertEquals(anotherObjectName, entries.get(0).key);
        assertEquals(objectName, entries.get(1).key);

        // delete objects
        assertEquals("No Content", conn.delete(bucketName, anotherObjectName, null).connection.getResponseMessage());
        assertEquals("No Content", conn.delete(bucketName, objectName, null).connection.getResponseMessage());

        // delete bucket
        assertEquals("No Content", conn.deleteBucket(bucketName, null).connection.getResponseMessage());

        ListAllMyBucketsResponse listAllMyBucketsResponse = conn.listAllMyBuckets(null);
        assertEquals(0, listAllMyBucketsResponse.entries.size());
    }

    private int getFile(String uri) throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(uri);
        int rc = httpClient.executeMethod(getMethod);
        String responseBody = getMethod.getResponseBodyAsString();
        System.out.println(responseBody);
        return rc;
    }
}
