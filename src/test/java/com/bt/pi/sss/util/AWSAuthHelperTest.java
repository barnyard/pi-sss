/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SignatureException;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

public class AWSAuthHelperTest {
    private static final String NON_AMZ = "non-amz";
    private static final String OTHER_NON_AMZ = "other-non-amz";
    private static final String SOME_OTHER_VALUE = "someothervalue";
    private static final String SOMEVALUE = "somevalue";
    private static final String X_AMZ_LOWERCASE = "x-amz-lowercase";
    private static final String X_AMZ_LOWERCASE_A = "x-amz-lowercase-a";
    private static final String X_AMZ_LOWERCASE_B = "x-amz-lowercase-b";
    private static final String X_AMZ_MIXED_CASE = "X-Amz-Lowercase";
    private static final String BUCKET = "bucket";
    private static final String OBJECT = "object";
    private static final String EXTENSION = "acl";
    private static final String CANONICAL_BASIC = "/bucket/object?acl";
    private static final String CANONICAL_BASIC_NO_EXTENSION = "/bucket/object";
    private static final String CANONICAL_BASIC_NO_OBJECT = "/bucket/";

    private final AWSAuthHelper authHelper = new AWSAuthHelper();
    private ContainerRequest request;
    private MultivaluedMap<String, String> headers;

    @Before
    public void before() throws URISyntaxException {
        request = mock(ContainerRequest.class);
        headers = new MultivaluedMapImpl();

        when(request.getRequestHeaders()).thenReturn(headers);
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?" + EXTENSION));
    }

    /**********************************
     * getCanonicalizedResource tests *
     **********************************/

    @Test
    public void getCanonicalizedResourceShouldNotReturnNull() {
        // act and assert
        assertNotNull(authHelper.getCanonicalizedResource(request));
    }

    @Test
    public void getCanonicalizedResourceShoudReturnTheStringCorrectly() {
        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudnotReturnExtraQueryParams() {
        // setup
        when(request.getPath()).thenReturn("/" + BUCKET + "/" + OBJECT + "?" + EXTENSION + "&foo=thing");

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudNotReturnUnknownExtensions() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?foo"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnAclExtension() {
        // setup
        when(request.getPath()).thenReturn("/" + BUCKET + "/" + OBJECT + "?acl");

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "?acl", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnLocationExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?location"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "?location", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnLocationExtensionWithoutAnyValue() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?location=location"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "?location", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnTorrentExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?torrent"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "?torrent", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnLoggingExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?logging"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "?logging", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithNoExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithJustQuestionMark() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/" + OBJECT + "?"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithNoObject() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/" + BUCKET + "/"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_OBJECT, retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithJustRoot() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals("/", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithRootAndExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com" + "/?" + EXTENSION));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals("/?acl", retVal);
    }

    @Test
    public void getCanonicalizedResourceShoudReturnCorrectlyWithRootAndUnknownExtension() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com/?foo"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals("/", retVal);
    }

    @Test
    public void getCanonicalizedResourceShouldReplaceSlashInObjectName() throws URISyntaxException {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://foo.com/" + BUCKET + "/" + OBJECT + "/" + OBJECT));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals(CANONICAL_BASIC_NO_EXTENSION + "%2F" + OBJECT, retVal);
    }

    /************************************
     * getCanonicalizedAmzHeaders tests *
     ************************************/

    @Test
    public void getCanonicalizedAmzHeadersShouldNotReturnNull() {
        // act and assert
        assertNotNull(authHelper.getCanonicalizedAmzHeaders(request));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnEmptyWithNoAMZHeaders() {
        // act and assert
        assertEquals("", authHelper.getCanonicalizedAmzHeaders(request));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnOnlyTheAMZHeaders() {
        // setup
        headers.add(NON_AMZ, SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE);
        headers.add(OTHER_NON_AMZ, SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertFalse(retVal.contains(NON_AMZ));
        assertFalse(retVal.contains(OTHER_NON_AMZ));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeaders() {
        // setup
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertTrue(retVal.contains(X_AMZ_LOWERCASE));
        assertTrue(retVal.contains(SOMEVALUE));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeaderInLowercase() {
        // setup
        headers.add(X_AMZ_MIXED_CASE, SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertTrue(retVal.contains(X_AMZ_LOWERCASE));
        assertTrue(retVal.contains(SOMEVALUE));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersSeperatedByNewLines() {
        // setup
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE_A, SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertTrue(retVal.substring(retVal.indexOf(X_AMZ_LOWERCASE), retVal.indexOf(X_AMZ_LOWERCASE_A)).contains("\n"));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersInOrder() {
        // setup
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE_B, SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE_A, SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertTrue(retVal.indexOf(X_AMZ_LOWERCASE) < retVal.indexOf(X_AMZ_LOWERCASE_A));
        assertTrue(retVal.indexOf(X_AMZ_LOWERCASE_A) < retVal.indexOf(X_AMZ_LOWERCASE_B));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersInOrderAnotherTest() {
        // setup
        headers.add("x-amz-meta-original-filename", "securityGroupCollection.json");
        headers.add("x-amz-meta-content-type", "application/octet-stream");
        headers.add("x-amz-acl", "private");

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);
        System.out.println(retVal);

        // assert
        assertTrue(retVal.indexOf("x-amz-acl") < retVal.indexOf("x-amz-meta-content-type"));
        assertTrue(retVal.indexOf("x-amz-meta-content-type") < retVal.indexOf("x-amz-meta-original-filename"));
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersWithMultipleValuesCommaSeparated() {
        // setup
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE, SOME_OTHER_VALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertEquals(X_AMZ_LOWERCASE + ":" + SOMEVALUE + "," + SOME_OTHER_VALUE + "\n", retVal);
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersWithMultipleValuesCommaSeparatedWithNoWhitespace() {
        // setup
        headers.add(X_AMZ_LOWERCASE, " " + SOMEVALUE);
        headers.add(X_AMZ_LOWERCASE, SOME_OTHER_VALUE + " ");

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertEquals(X_AMZ_LOWERCASE + ":" + SOMEVALUE + "," + SOME_OTHER_VALUE + "\n", retVal);
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersWithLineBreaksShouldBeUnfolded() {
        // setup
        headers.add(X_AMZ_LOWERCASE, SOMEVALUE + "\n" + SOME_OTHER_VALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertEquals(X_AMZ_LOWERCASE + ":" + SOMEVALUE + " " + SOME_OTHER_VALUE + "\n", retVal);
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldReturnTheAMZHeadersWithNoWhitespaceAfterTheKey() {
        // setup
        headers.add(X_AMZ_LOWERCASE + " ", SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert
        assertEquals(X_AMZ_LOWERCASE + ":" + SOMEVALUE + "\n", retVal);
    }

    @Test
    public void getCanonicalizedAmzHeadersShouldHaveATrailingNewLine() {
        // setup
        headers.add(X_AMZ_LOWERCASE + " ", SOMEVALUE);

        // act
        String retVal = authHelper.getCanonicalizedAmzHeaders(request);

        // assert;
        assertTrue(retVal.substring(retVal.length() - 1).contains("\n"));
    }

    @Test
    public void getSignatureShouldNotReturnNull() throws SignatureException {
        // act and assert
        assertNotNull(authHelper.getSignature("a", ""));
    }

    @Test
    public void getSignatureShouldProvideTheCorrectOutput1() {
        // setup
        String stringToSign = "GET\n\n\nTue, 27 Mar 2007 19:36:42 +0000\n/johnsmith/photos/puppy.jpg";

        // act and assert
        checkSignature(stringToSign, "xXjDGYUmKxnwqr5KXNPGldn5LbA=");
    }

    @Test
    public void getSignatureShouldProvideTheCorrectOutput2() {
        // setup
        String stringToSign = "PUT\n\nimage/jpeg\nTue, 27 Mar 2007 21:15:45 +0000\n/johnsmith/photos/puppy.jpg";

        // act and assert
        checkSignature(stringToSign, "hcicpDDvL9SsO6AkvxqmIWkmOuQ=");
    }

    @Test
    public void getSignatureShouldProvideTheCorrectOutput3() {
        // setup
        String stringToSign = "GET\n\n\nTue, 27 Mar 2007 19:42:41 +0000\n/johnsmith/";

        // act and assert
        checkSignature(stringToSign, "jsRt/rhG+Vtp88HrYL706QhE4w4=");
    }

    private void checkSignature(String stringToSign, String expectedSignature) {
        // setup
        String secretKey = "uV3F3YluFJax1cknvbcGwgjvx4QpvB+leU8dUj2o";

        // act
        try {
            String sig = authHelper.getSignature(secretKey, stringToSign);
            // assert
            assertEquals(expectedSignature, sig);
        } catch (SignatureException ex) {
            fail("Method blew up with " + ex.getMessage());
        }
    }

    @Test
    public void testGetCanonicalizedResourceWithBrackets() throws Exception {
        // setup
        when(request.getRequestUri()).thenReturn(new URI("http://10.237.33.162:9080/craig-bonkey/My+Backup+Group%2FSEP+Platform+Roadmap+2007-8+%28v1.2%29.ppt"));

        // act
        String retVal = authHelper.getCanonicalizedResource(request);

        // assert
        assertEquals("/craig-bonkey/My+Backup+Group%2FSEP+Platform+Roadmap+2007-8+%28v1.2%29.ppt", retVal);
    }
}
