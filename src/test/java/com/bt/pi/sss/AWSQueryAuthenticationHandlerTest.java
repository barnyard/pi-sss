/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SignatureException;
import java.util.List;

import javax.security.sasl.AuthenticationException;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.util.AWSAuthHelper;
import com.sun.jersey.spi.container.ContainerRequest;

public class AWSQueryAuthenticationHandlerTest {

    private static final String CONTENT_MD5_KEY = "Content-MD5";
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String RESOURCE = "/bucket/object";
    private static final String AMZ_HEADERS = "amz-headers";
    private static final String CONTENT_MD5 = "content-md5";
    private static final String CONTENT_TYPE = "content-type";
    private static final String REQUEST_SIGNATURE = "vjbyPxybdZaNmGa%2ByT272YEAiv4%3D";
    private static final String REQUEST_EXPIRES = "1141889120";
    private static final String REQUEST_ACCESS_KEY = "0PN5J17HBGZHT7JJ3X82";
    private static final String SIGNATURE_KEY = "Signature";
    private static final String EXPIRES_KEY = "Expires";
    private static final String AWSACCESS_KEY_ID = "AWSAccessKeyId";
    private static final String SECRET_KEY = "secret";
    private static final String DATE_KEY = "Date";
    private static final String DATE = "Mon, 26 Mar 2007 19:37:58 +0000";
    private final AWSQueryAuthenticationHandler authHandler = new AWSQueryAuthenticationHandler();
    private ContainerRequest request;
    private UserManager userManager;
    private User user;
    private AWSAuthHelper awsAuthHelper;
    private MultivaluedMap<String, String> queryParams;
    private List<String> expiresValues;
    private List<String> accessKeyValues;
    private List<String> signatureValues;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws SignatureException {
        request = mock(ContainerRequest.class);
        userManager = mock(UserManager.class);
        user = mock(User.class);
        awsAuthHelper = mock(AWSAuthHelper.class);
        queryParams = mock(MultivaluedMap.class);
        expiresValues = mock(List.class);
        signatureValues = mock(List.class);
        accessKeyValues = mock(List.class);

        when(request.getHeaderValue(any(String.class))).thenReturn(null);
        when(request.getHeaderValue(CONTENT_TYPE_KEY)).thenReturn(CONTENT_TYPE);
        when(request.getHeaderValue(CONTENT_MD5_KEY)).thenReturn(CONTENT_MD5);
        when(request.getMethod()).thenReturn("GET");

        when(expiresValues.get(anyInt())).thenReturn(REQUEST_EXPIRES);
        when(accessKeyValues.get(anyInt())).thenReturn(REQUEST_ACCESS_KEY);
        when(signatureValues.get(anyInt())).thenReturn(REQUEST_SIGNATURE);

        when(queryParams.get(EXPIRES_KEY)).thenReturn(expiresValues);
        when(queryParams.get(AWSACCESS_KEY_ID)).thenReturn(accessKeyValues);
        when(queryParams.get(SIGNATURE_KEY)).thenReturn(signatureValues);

        when(request.getQueryParameters()).thenReturn(queryParams);

        when(userManager.userExists(REQUEST_ACCESS_KEY)).thenReturn(false);
        when(userManager.getUserByAccessKey(REQUEST_ACCESS_KEY)).thenReturn(user);

        when(awsAuthHelper.getCanonicalizedResource(request)).thenReturn(RESOURCE);
        when(awsAuthHelper.getCanonicalizedAmzHeaders(request)).thenReturn(AMZ_HEADERS);
        when(awsAuthHelper.getSignature(any(String.class), any(String.class))).thenReturn(REQUEST_SIGNATURE);

        when(user.getApiSecretKey()).thenReturn(SECRET_KEY);
        when(user.isEnabled()).thenReturn(true);

        authHandler.setUserManager(userManager);
        authHandler.setAWSAuthHelper(awsAuthHelper);
    }

    /*******************
     * canHandle tests *
     *******************/

    @Test
    public void canHandleShouldReturnFalseIfExpiresIsMissing() {
        // setup
        when(queryParams.get(EXPIRES_KEY)).thenReturn(null);

        // act and assert
        assertFalse(authHandler.canHandle(request));
    }

    @Test
    public void canHandleShouldReturnFalseIfSignatureIsMissing() {
        // setup
        when(queryParams.get(SIGNATURE_KEY)).thenReturn(null);

        // act and assert
        assertFalse(authHandler.canHandle(request));
    }

    @Test
    public void canHandleShouldReturnFalseIfAccessKeyIsMissing() {
        // setup
        when(queryParams.get(AWSACCESS_KEY_ID)).thenReturn(null);

        // act and assert
        assertFalse(authHandler.canHandle(request));
    }

    @Test
    public void canHandleShouldReturnTrueIfKeyExpiresAndSignatureAreInQueryString() {
        // act and assert
        assertTrue(authHandler.canHandle(request));
    }

    @Test
    public void canHandleShouldReturnTrueIfAWSAuthHeaderIsPresentInPlaceOfAccesKeyAndSignature() {
        // setup
        when(queryParams.get(AWSACCESS_KEY_ID)).thenReturn(null);
        when(queryParams.get(SIGNATURE_KEY)).thenReturn(null);
        when(request.getHeaderValue("Authorization")).thenReturn("AWS " + REQUEST_ACCESS_KEY + ":" + REQUEST_SIGNATURE);

        // act and assert
        assertTrue(authHandler.canHandle(request));
    }

    @Test
    public void canHandleShouldReturnTrueIfDateHeaderIsPresentInPlaceOfExpiresParameter() {
        // setup
        when(queryParams.get(EXPIRES_KEY)).thenReturn(null);
        when(request.getHeaderValue(DATE_KEY)).thenReturn(DATE);

        // act and assert
        assertTrue(authHandler.canHandle(request));
    }

    /**********************
     * authenticate tests *
     **********************/

    @Test(expected = IllegalArgumentException.class)
    public void authenticateShouldThrowIfAccessKeyIsMissing() throws AuthenticationException {
        // setup
        when(queryParams.get(AWSACCESS_KEY_ID)).thenReturn(null);

        // act and assert
        authHandler.authenticate(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticateShouldThrowIfSignatureIsMissing() throws AuthenticationException {
        // setup
        when(queryParams.get(SIGNATURE_KEY)).thenReturn(null);

        // act and assert
        authHandler.authenticate(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticateShouldThrowIfExpiresIsMissing() throws AuthenticationException {
        // setup
        when(queryParams.get(EXPIRES_KEY)).thenReturn(null);

        // act and assert
        authHandler.authenticate(request);
    }

    @Test
    public void authenticateShouldNotReturnNull() throws AuthenticationException {
        // act and assert
        assertNotNull(authHandler.authenticate(request));
    }

    @Test
    public void authenticateShouldDelegateToGetTheCanonicalRequest() throws AuthenticationException {
        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper).getCanonicalizedResource(request);
    }

    @Test
    public void authenticateShouldReturnTheUserWhenSuccessful() throws AuthenticationException {
        // act and assert
        assertEquals(user, authHandler.authenticate(request));
    }

    @Test
    public void authenticateShouldBeSuccessfulWithWalrusUri() throws AuthenticationException {
        // setup
        when(awsAuthHelper.getCanonicalizedResource(request)).thenReturn("/services/Walrus" + RESOURCE);

        // act and assert
        assertEquals(user, authHandler.authenticate(request));
    }

    @Test
    public void authenticateShouldDelegateToGetTheCanonicalAMZHeaders() throws AuthenticationException {
        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper).getCanonicalizedAmzHeaders(request);
    }

    @Test
    public void authenticateShouldDelegateToGetTheSignature() throws SignatureException, AuthenticationException {
        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper, atLeastOnce()).getSignature(any(String.class), any(String.class));
    }

    @Test
    public void theStringToSignShouldBeCorrect() throws SignatureException, AuthenticationException {
        // setup
        String toSign = "GET\n" + CONTENT_MD5 + "\n" + CONTENT_TYPE + "\n" + REQUEST_EXPIRES + "\n" + AMZ_HEADERS + RESOURCE;

        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper).getSignature(any(String.class), eq(toSign));
    }

    @Test
    public void theStringToSignShouldHaveEmptyLinesIfThereIsNoData() throws SignatureException, AuthenticationException {
        // setup
        String toSign = "GET\n" + "\n" + "\n" + REQUEST_EXPIRES + "\n" + AMZ_HEADERS + RESOURCE;
        when(request.getHeaderValue(CONTENT_TYPE_KEY)).thenReturn(null);
        when(request.getHeaderValue(CONTENT_MD5_KEY)).thenReturn(null);

        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper).getSignature(any(String.class), eq(toSign));
    }

    @Test
    public void theSeretKeyShouldBeCorrect() throws SignatureException, AuthenticationException {
        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper, atLeastOnce()).getSignature(eq(SECRET_KEY), any(String.class));
    }

    @Test(expected = AuthenticationException.class)
    public void signatureExceptionsShouldBeRecastAsAuthenticationException() throws AuthenticationException, SignatureException {
        // setup
        when(awsAuthHelper.getSignature(any(String.class), any(String.class))).thenThrow(new SignatureException());

        // act
        authHandler.authenticate(request);
    }

    @Test(expected = AuthenticationException.class)
    public void shouldThrowAuthenticationExceptionIfTheSignaturesDoNotMatch() throws AuthenticationException, SignatureException {
        // setup
        when(awsAuthHelper.getSignature(any(String.class), any(String.class))).thenReturn("foo");
        when(signatureValues.get(anyInt())).thenReturn("different");

        // act
        authHandler.authenticate(request);
    }

    @Test
    public void shouldUseTheSignatureInTheAMZHeaderIfThereIsNotOneInTheQuery() throws AuthenticationException, SignatureException {
        // setup
        when(awsAuthHelper.getSignature(any(String.class), any(String.class))).thenReturn(REQUEST_SIGNATURE);
        when(queryParams.get(SIGNATURE_KEY)).thenReturn(null);
        when(request.getHeaderValue("Authorization")).thenReturn("AWS user:" + REQUEST_SIGNATURE);

        // act
        authHandler.authenticate(request);
    }

    @Test
    public void shouldUseTheAccessKeyInTheAMZHeaderIfThereIsNotOneInTheQuery() throws AuthenticationException, SignatureException {
        // setup
        when(awsAuthHelper.getSignature(any(String.class), any(String.class))).thenReturn(REQUEST_SIGNATURE);
        when(queryParams.get(AWSACCESS_KEY_ID)).thenReturn(null);
        when(request.getHeaderValue("Authorization")).thenReturn("AWS " + REQUEST_ACCESS_KEY + ":" + REQUEST_SIGNATURE);

        // act
        authHandler.authenticate(request);
        verify(userManager).getUserByAccessKey(REQUEST_ACCESS_KEY);
    }

    @Test
    public void shouldUseTheDateHeaderIfThereIsNoExpiresInTheQuery() throws AuthenticationException, SignatureException {
        // setup
        String toSign = "GET\n" + CONTENT_MD5 + "\n" + CONTENT_TYPE + "\n" + DATE + "\n" + AMZ_HEADERS + RESOURCE;
        when(queryParams.get(EXPIRES_KEY)).thenReturn(null);
        when(request.getHeaderValue(DATE_KEY)).thenReturn(DATE);

        // act
        authHandler.authenticate(request);

        // assert
        verify(awsAuthHelper).getSignature(any(String.class), eq(toSign));
    }

    @Test(expected = AuthenticationException.class)
    public void shouldNotAuthenticateDisabledUsers() throws Exception {
        // setup
        when(user.isEnabled()).thenReturn(false);
        // act
        authHandler.authenticate(request);

    }
}
