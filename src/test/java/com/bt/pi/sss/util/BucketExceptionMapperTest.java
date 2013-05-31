package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BadDigestException;
import com.bt.pi.sss.exception.BucketAlreadyExistsException;
import com.bt.pi.sss.exception.BucketAlreadyOwnedByUserException;
import com.bt.pi.sss.exception.BucketException;
import com.bt.pi.sss.exception.BucketNotEmptyException;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.EntityTooLargeException;
import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.exception.InvalidBucketNameException;
import com.bt.pi.sss.exception.MissingContentLengthException;
import com.bt.pi.sss.exception.NoSuchBucketException;
import com.bt.pi.sss.filter.TransactionIdRequestFilter;
import com.bt.pi.sss.util.BucketExceptionMapper;

public class BucketExceptionMapperTest {
    private BucketExceptionMapper bucketExceptionMapper;
    private String txId;
    private String resource;

    @Before
    public void setUp() throws Exception {
        this.bucketExceptionMapper = new BucketExceptionMapper();
        txId = "abcde123";
        MDC.put(TransactionIdRequestFilter.PI_TX_ID_KEY, txId);
        resource = "/bucket/object.jpg";
        MDC.put(TransactionIdRequestFilter.PI_RESOURCE, resource);
    }

    @Test
    public void testToResponseAccessDeniedException() {
        // setup
        BucketException e = new AccessDeniedException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.FORBIDDEN.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>AccessDenied</Code><Message>AccessDenied</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseBucketAlreadyExistsException() {
        // setup
        BucketException e = new BucketAlreadyExistsException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.CONFLICT.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>BucketAlreadyExists</Code><Message>BucketAlreadyExists</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseBucketAlreadyOwnedByUserException() {
        // setup
        BucketException e = new BucketAlreadyOwnedByUserException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.CONFLICT.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>BucketAlreadyOwnedByUser</Code><Message>BucketAlreadyOwnedByUser</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseBucketNotEmptyException() {
        // setup
        BucketException e = new BucketNotEmptyException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.CONFLICT.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>BucketNotEmpty</Code><Message>BucketNotEmpty</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseBucketObjectNotFoundException() {
        // setup
        BucketException e = new BucketObjectNotFoundException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.NOT_FOUND.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>BucketObjectNotFound</Code><Message>BucketObjectNotFound</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseNoSuchBucketException() {
        // setup
        BucketException e = new NoSuchBucketException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.NOT_FOUND.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>NoSuchBucket</Code><Message>NoSuchBucket</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseEntityTooLargeException() {
        // setup
        BucketException e = new EntityTooLargeException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>EntityTooLarge</Code><Message>EntityTooLarge</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseMissingContentLengthException() {
        // setup
        BucketException e = new MissingContentLengthException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(411, result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>MissingContentLength</Code><Message>MissingContentLength</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseInvalidBucketNameException() {
        // setup
        String message = "oh!";
        BucketException e = new InvalidBucketNameException(message);

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>InvalidBucketName</Code><Message>" + message + "</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseInvalidObjectNameException() {
        // setup
        String message = "oh!";
        BucketException e = new InvalidArgumentException(message);

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>InvalidArgument</Code><Message>" + message + "</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseOther() {
        // setup
        String message = "oh!";
        BucketException e = new BucketException(message) {
            private static final long serialVersionUID = 4670797810045496597L;
        };

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>InternalServerError</Code><Message>" + e.getClass().getSimpleName() + ": " + message + "</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void testToResponseBadDigestException() {
        // setup
        BucketException e = new BadDigestException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Error><Code>BadDigest</Code><Message>BadDigest</Message><Resource>" + resource + "</Resource><RequestId>" + txId + "</RequestId></Error>"));
    }

    @Test
    public void theMessageShouldBeTheSimpleNameWhenExceptionIsEmpty() {
        // setup
        BucketException e = new BadDigestException();

        // act
        Response result = this.bucketExceptionMapper.toResponse(e);

        // assert
        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
        assertEquals(MediaType.APPLICATION_XML, result.getMetadata().get(HttpHeaders.CONTENT_TYPE).get(0).toString());
        String xml = (String) result.getEntity();
        assertTrue(xml.contains("<Message>BadDigest</Message>"));
    }
}
