package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BucketException;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.MissingContentLengthException;
import com.bt.pi.sss.filter.AuthenticationFilter;
import com.bt.pi.sss.filter.TransactionIdRequestFilter;
import com.bt.pi.sss.response.AccessControlPolicy;
import com.bt.pi.sss.response.ListAllMyBucketsResult;
import com.bt.pi.sss.response.ListBucketResult;
import com.bt.pi.sss.response.ListEntry;
import com.bt.pi.sss.util.DateUtils;
import com.bt.pi.sss.util.HeaderUtils;
import com.bt.pi.sss.util.ResponseUtils;

public class ServiceResourceTest {
    private ServiceResource serviceResource;
    private HttpHeaders httpHeaders;
    private BucketManager bucketManager;
    private String userName = "testUser";
    private String accessKey = "accessKey";
    private String bucketName = "test1";
    private String objectName = "object1";
    private MultivaluedMap<String, String> headers;
    private String eTag;
    private ObjectMetaData objectMetaData;
    private String contentType;
    private String contentDisposition;
    private String txId;
    private String resource;
    private String data = "abcd1234";
    private ResponseUtils responseUtils;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        this.serviceResource = new ServiceResource();
        this.bucketManager = Mockito.mock(BucketManager.class);
        this.serviceResource.setBucketManager(bucketManager);
        this.responseUtils = new ResponseUtils();
        this.serviceResource.setResponseUtils(this.responseUtils);
        this.httpHeaders = Mockito.mock(HttpHeaders.class);
        List<String> headerList = Arrays.asList(new String[] { userName });
        Mockito.when(this.httpHeaders.getRequestHeader(AuthenticationFilter.PI_USER_ID_KEY)).thenReturn(headerList);
        headers = Mockito.mock(MultivaluedMap.class);
        Mockito.when(this.httpHeaders.getRequestHeaders()).thenReturn(headers);
        this.serviceResource.setHeaderUtils(new HeaderUtils());
        this.serviceResource.setDateUtils(new DateUtils());
        txId = "abcde123";
        MDC.put(TransactionIdRequestFilter.PI_TX_ID_KEY, txId);
        resource = "/" + bucketName + "/" + objectName;
        MDC.put(TransactionIdRequestFilter.PI_RESOURCE, resource);
    }

    @Test
    public void testListAllMyBuckets() throws Exception {
        // setup
        List<BucketMetaData> bucketList = Arrays.asList(new BucketMetaData[] { BucketMetaData.fromName("test1"), BucketMetaData.fromName("test2") });
        Mockito.when(this.bucketManager.getBucketList(userName)).thenReturn(bucketList);

        // act
        ListAllMyBucketsResult result = this.serviceResource.listAllMyBuckets(userName, accessKey);

        // assert
        assertNotNull(result);
        assertEquals(accessKey, result.getOwner().getId());
        assertEquals(userName, result.getOwner().getDisplayName());
        assertEquals(2, result.getBuckets().size());
        assertEquals("test1", result.getBuckets().get(0).getName());
        assertDate(result.getBuckets().get(0).getCreationDate());
        assertEquals("test2", result.getBuckets().get(1).getName());
        assertDate(result.getBuckets().get(1).getCreationDate());
    }

    @Test(expected = AccessDeniedException.class)
    public void testListAllMyBucketsAnonymous() throws Exception {
        // act
        this.serviceResource.listAllMyBuckets(AuthenticationFilter.ANONYMOUS_USER, accessKey);
    }

    @Test(expected = WebApplicationException.class)
    public void testListAllMyBucketsThrowable() throws Exception {
        // setup
        String message = "shit happens";
        Mockito.when(this.bucketManager.getBucketList(userName)).thenThrow(new RuntimeException(message));

        // act
        try {
            this.serviceResource.listAllMyBuckets(userName, accessKey);
        } catch (WebApplicationException e) {
            // assert
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getResponse().getStatus());
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @Test
    public void testCreateBucket() throws Exception {
        // act
        Response result = this.serviceResource.createBucket(userName, bucketName, null, null);

        // assert
        Mockito.verify(this.bucketManager).createBucket(userName, bucketName, null, null);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
    }

    @Test(expected = AccessDeniedException.class)
    public void testCreateBucketAnonymous() throws Exception {
        // act
        this.serviceResource.createBucket(AuthenticationFilter.ANONYMOUS_USER, bucketName, null, null);
    }

    @SuppressWarnings("serial")
    @Test(expected = BucketException.class)
    public void testCreateBucketBucketException() throws Exception {
        // setup
        Mockito.doThrow(new BucketException() {
        }).when(this.bucketManager).createBucket(userName, bucketName, null, null);

        // act
        this.serviceResource.createBucket(userName, bucketName, null, null);
    }

    @Test
    public void testCreateBucketThrowable() throws Exception {
        // setup
        String message = "oj!";
        Mockito.doThrow(new RuntimeException(message)).when(this.bucketManager).createBucket(userName, bucketName, null, null);

        // act
        Response result = this.serviceResource.createBucket(userName, bucketName, null, null);

        // assert
        // assertEquals(message, e.getResponse().getEntity());
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>InternalServerError</Code><Message>RuntimeException: " + message + "</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testCreateBucketWithAclHeader() {
        // setup

        // act
        Response result = this.serviceResource.createBucket(userName, bucketName, "public-read-write", null);

        // assert
        Mockito.verify(this.bucketManager).createBucket(userName, bucketName, CannedAclPolicy.PUBLIC_READ_WRITE, null);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetBucket() throws Exception {
        // setup
        long size = 123L;

        ObjectMetaData object1 = Mockito.mock(ObjectMetaData.class);
        Mockito.when(object1.getName()).thenReturn("object1");
        Mockito.when(object1.getSize()).thenReturn(size);
        SortedSet<ObjectMetaData> objectList = new TreeSet<ObjectMetaData>(Arrays.asList(new ObjectMetaData[] { object1 }));
        Mockito.when(this.bucketManager.getListOfFilesInBucket(userName, bucketName)).thenReturn(objectList);

        // act
        ListBucketResult result = (ListBucketResult) this.serviceResource.getBucket(userName, bucketName, null, null, null, null, null);

        // assert
        assertEquals(bucketName, result.getName());
        assertNull(result.getDelimiter());
        assertFalse(result.isTruncated());
        assertEquals(0, result.getMaxKeys());
        assertNull(result.getPrefix());
        assertEquals(1, result.getContents().size());
        ListEntry listEntry = result.getContents().get(0);
        assertEquals("object1", listEntry.getKey());
        assertEquals(size, listEntry.getSize());
    }

    @Test
    public void testGetBucketWithPrefix() throws Exception {
        // setup
        SortedSet<ObjectMetaData> objectList = new TreeSet<ObjectMetaData>(Arrays.asList(new ObjectMetaData[] { ObjectMetaData.fromName("object1"), ObjectMetaData.fromName("fred"), ObjectMetaData.fromName("object2") }));
        Mockito.when(this.bucketManager.getListOfFilesInBucket(userName, bucketName)).thenReturn(objectList);
        String prefix = "obj";

        // act
        ListBucketResult result = (ListBucketResult) this.serviceResource.getBucket(userName, bucketName, prefix, null, null, null, null);

        // assert
        assertEquals(bucketName, result.getName());
        assertEquals(prefix, result.getPrefix());
        assertEquals(2, result.getContents().size());
        assertEquals("object1", result.getContents().get(0).getKey());
        assertEquals("object2", result.getContents().get(1).getKey());
    }

    @Test
    public void testGetBucketWithMaxKeys() throws Exception {
        // setup
        SortedSet<ObjectMetaData> objectList = new TreeSet<ObjectMetaData>(Arrays.asList(new ObjectMetaData[] { ObjectMetaData.fromName("object1"), ObjectMetaData.fromName("fred"), ObjectMetaData.fromName("object2") }));
        Mockito.when(this.bucketManager.getListOfFilesInBucket(userName, bucketName)).thenReturn(objectList);
        Integer maxKeys = 2;

        // act
        ListBucketResult result = (ListBucketResult) this.serviceResource.getBucket(userName, bucketName, null, null, null, maxKeys, null);

        // assert
        assertEquals(bucketName, result.getName());
        assertEquals(maxKeys.intValue(), result.getMaxKeys());
        assertEquals(2, result.getContents().size());
        assertEquals("fred", result.getContents().get(0).getKey());
        assertEquals("object1", result.getContents().get(1).getKey());
    }

    @Test
    public void testGetBucketAcl() throws Exception {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.PUBLIC_READ;
        Mockito.when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(cannedAclPolicy);

        // act
        Object result = this.serviceResource.getBucket(userName, bucketName, null, null, "", null, null);

        // assert
        assertTrue(result instanceof AccessControlPolicy);
    }

    @Test
    public void testGetBucketLocation() throws Exception {
        // setup
        String location = "USA";
        Mockito.when(this.bucketManager.getBucketLocation(userName, bucketName)).thenReturn(location);

        // act
        Object result = this.serviceResource.getBucket(userName, bucketName, null, null, null, null, "");

        // assert
        assertTrue(result instanceof String);
        String s = (String) result;
        assertTrue(s.contains("<LocationConstraint"));
        assertTrue(s.contains(">" + location + "<"));
        assertTrue(s.contains("LocationConstraint>"));
    }

    @Test
    public void testGetObjectWithContentLength() throws Exception {
        // setup
        this.responseUtils.setForceContentLengthOnGetObject(true);

        Response result = testGetObject();

        // assert
        assertTrue(result.getMetadata().containsKey(HttpHeaders.CONTENT_LENGTH));
    }

    private Response testGetObject() throws Exception {
        // setup
        setupGetObject();

        Map<String, List<String>> metaMap = new HashMap<String, List<String>>();
        String metaKey1 = "x-amz-meta-1";
        metaMap.put(metaKey1, Arrays.asList(new String[] { "a", "b" }));
        String metaKey2 = "x-amz-meta-2";
        metaMap.put(metaKey2, Arrays.asList(new String[] { "c", "d" }));
        Mockito.when(objectMetaData.getXAmzMetaHeaders()).thenReturn(metaMap);

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, null, null);

        // assert
        assertTrue(result.getEntity() instanceof InputStream);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertEquals(this.data, readInputStream((InputStream) result.getEntity()));
        assertTrue(result.getMetadata().containsKey(HttpHeaders.CONTENT_TYPE));
        assertTrue(result.getMetadata().get(HttpHeaders.CONTENT_TYPE).contains(contentType));
        assertTrue(result.getMetadata().get("Content-Disposition").contains(contentDisposition));
        assertTrue(result.getMetadata().get(HttpHeaders.ETAG).contains("\"" + eTag + "\""));
        assertEquals(2, result.getMetadata().get(metaKey1).size());
        assertTrue(result.getMetadata().get(metaKey1).contains("a"));
        assertTrue(result.getMetadata().get(metaKey1).contains("b"));
        assertEquals(2, result.getMetadata().get(metaKey2).size());
        assertTrue(result.getMetadata().get(metaKey2).contains("c"));
        assertTrue(result.getMetadata().get(metaKey2).contains("d"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private String readInputStream(InputStream is) throws Exception {
        List<String> readLines = IOUtils.readLines(is);
        return readLines.get(0);
    }

    @Test
    public void testGetObjectNoContentLength() throws Exception {
        // setup/act/assert
        Response result = testGetObject();

        // assert
        assertFalse(result.getMetadata().containsKey(HttpHeaders.CONTENT_LENGTH));
    }

    private void setupGetObject() throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, this.data);
        objectMetaData = Mockito.mock(ObjectMetaData.class);
        Mockito.when(objectMetaData.getInputStream()).thenReturn(new FileInputStream(new File(tmpFile.getAbsolutePath())));
        contentType = "text/plain";
        Mockito.when(objectMetaData.getContentType()).thenReturn(contentType);
        contentDisposition = "stuff";
        Mockito.when(objectMetaData.getContentDisposition()).thenReturn(contentDisposition);
        eTag = "eeeee:tttt";
        Mockito.when(objectMetaData.getETag()).thenReturn(eTag);
        Mockito.when(this.bucketManager.readObject(userName, bucketName, objectName)).thenReturn(objectMetaData);
    }

    @Test
    public void testGetObjectIfMatchTrue() throws Exception {
        // setup
        setupGetObject();

        String ifMatch = eTag;

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, ifMatch, null, null, null);

        // setup
        assertTrue(result.getEntity() instanceof InputStream);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
    }

    @Test
    public void testGetObjectIfMatchTrueWithQuotes() throws Exception {
        // setup
        setupGetObject();

        String ifMatch = "\"" + eTag + "\"";

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, ifMatch, null, null, null);

        // setup
        assertTrue(result.getEntity() instanceof InputStream);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
    }

    @Test
    public void testGetObjectIfMatchFalse() throws Exception {
        // setup
        setupGetObject();

        String ifMatch = "fred";

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, ifMatch, null, null, null);

        // setup
        assertNull(result.getEntity());
        assertEquals(412, result.getStatus());
    }

    @Test
    public void testGetObjectIfNoneMatchTrue() throws Exception {
        // setup
        setupGetObject();

        String ifNoneMatch = "fred";

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, ifNoneMatch, null, null);

        // setup
        assertTrue(result.getEntity() instanceof InputStream);
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
    }

    @Test
    public void testGetObjectIfNoneMatchFalse() throws Exception {
        // setup
        setupGetObject();

        String ifNoneMatch = eTag;

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, ifNoneMatch, null, null);

        // setup
        assertNull(result.getEntity());
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfModifiedSinceEqualsFalse() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(now);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifModifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, ifModifiedSince, null);

        // setup
        assertNull(result.getEntity());
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfModifiedSinceHistoryFalse() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        Calendar past = Calendar.getInstance();
        past.add(Calendar.MONTH, -1);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(past);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifModifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, ifModifiedSince, null);

        // setup
        assertNull(result.getEntity());
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfModifiedSinceTrue() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, 1);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(future);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifModifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, ifModifiedSince, null);

        // setup
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfUnmodifiedSinceEquals() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(now);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifUnmodifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, null, ifUnmodifiedSince);

        // setup
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfUnmodifiedSinceHistory() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        Calendar past = Calendar.getInstance();
        past.add(Calendar.MONTH, -1);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(past);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifUnmodifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, null, ifUnmodifiedSince);

        // setup
        assertEquals(data, readInputStream((InputStream) result.getEntity()));
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
    }

    @Test
    public void testGetObjectIfUnmodifiedSinceTrue() throws Exception {
        // setup
        setupGetObject();
        Calendar now = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.MONTH, 1);
        Mockito.when(objectMetaData.getLastModified()).thenReturn(future);

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String ifUnmodifiedSince = df.format(now.getTime());

        // act
        Response result = this.serviceResource.getObject(userName, bucketName, objectName, null, null, null, ifUnmodifiedSince);

        // setup
        assertNull(result.getEntity());
        assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), result.getStatus());
    }

    @Test
    public void testHeadObject() throws Exception {
        // setup
        setupGetObject();

        Long length = 123L;
        Mockito.when(objectMetaData.getSize()).thenReturn(length);

        // act
        Response result = this.serviceResource.headObject(userName, bucketName, objectName);

        // assert
        assertNull(result.getEntity());
        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertTrue(result.getMetadata().containsKey(HttpHeaders.CONTENT_TYPE));
        assertTrue(result.getMetadata().get(HttpHeaders.CONTENT_TYPE).contains(contentType));
        assertTrue(result.getMetadata().get("Content-Disposition").contains(contentDisposition));
        assertTrue(result.getMetadata().get(HttpHeaders.ETAG).contains("\"" + eTag + "\""));

        assertEquals(length, result.getMetadata().get(HttpHeaders.CONTENT_LENGTH).get(0));
    }

    @Test(expected = BucketObjectNotFoundException.class)
    public void testGetObjectNotFound() throws Exception {
        // setup
        Mockito.when(this.bucketManager.readObject(userName, bucketName, objectName)).thenThrow(new BucketObjectNotFoundException());

        // act
        this.serviceResource.getObject(userName, bucketName, objectName, null, null, null, null);
    }

    @Test
    public void testPutObject() {
        // setup
        final byte[] buf = "this is a test file".getBytes();
        InputStream is = new ByteArrayInputStream(buf);
        // setContentLengthHeaderMock(buf.length);
        String contentLength = Integer.toString(buf.length);
        String contentMd5 = null;
        String contentType = null;
        String contentDisposition = null;
        String md5 = "abc123";
        Mockito.when(this.bucketManager.storeObject(userName, bucketName, objectName, is, contentType, contentMd5, contentDisposition, null)).thenReturn(md5);

        // act
        Response result = this.serviceResource.putObject(httpHeaders, userName, contentType, contentMd5, contentLength, contentDisposition, bucketName, objectName, is);

        // assert
        assertEquals(200, result.getStatus());

        assertTrue(result.getMetadata().containsKey(HttpHeaders.ETAG));
        assertTrue(result.getMetadata().get(HttpHeaders.ETAG).contains("\"" + md5 + "\""));
    }

    @Test(expected = MissingContentLengthException.class)
    public void testPutObjectMissingContentLength() {
        // setup
        byte[] buf = "this is a test file".getBytes();
        InputStream is = new ByteArrayInputStream(buf);
        String contentLength = null;
        String contentMd5 = null;
        String contentType = null;
        String contentDisposition = null;

        // act
        this.serviceResource.putObject(httpHeaders, userName, contentType, contentMd5, contentLength, contentDisposition, "test1", "object1", is);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutObjectThrowsRuntime() {
        // setup
        byte[] buf = "this is a test file".getBytes();
        InputStream is = new ByteArrayInputStream(buf);
        String message = "shit happens";
        Mockito.doThrow(new RuntimeException(message)).when(this.bucketManager).storeObject(eq(userName), eq(bucketName), eq(objectName), eq(is), (String) Matchers.isNull(), (String) Matchers.isNull(), (String) Matchers.isNull(), Matchers.anyMap());
        String contentLength = Integer.toString(buf.length);
        String contentMd5 = null;
        String contentType = null;
        String contentDisposition = null;

        // act
        Response result = this.serviceResource.putObject(httpHeaders, userName, contentType, contentMd5, contentLength, contentDisposition, bucketName, objectName, is);

        // assert
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>InternalServerError</Code><Message>RuntimeException: " + message + "</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testDeleteObject() {
        // act
        Response result = this.serviceResource.deleteObject(userName, bucketName, objectName);

        // assert
        assertEquals(204, result.getStatus());
        Mockito.verify(this.bucketManager).deleteObject(userName, bucketName, objectName);
    }

    @Test
    public void testDeleteBucket() {
        // act
        Response result = this.serviceResource.deleteBucket(userName, bucketName);

        // assert
        assertEquals(204, result.getStatus());
        Mockito.verify(this.bucketManager).deleteBucket(userName, bucketName);
    }

    private void assertDate(Date d) throws ParseException {
        long date = d.getTime();
        long now = Calendar.getInstance().getTimeInMillis();
        assertTrue(Math.abs(date - now) < 2000);
    }

    private void assertDate(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        Date d = sdf.parse(s);
        assertDate(d);
    }

}
