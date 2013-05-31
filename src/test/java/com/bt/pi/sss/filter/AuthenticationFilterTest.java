package com.bt.pi.sss.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.security.sasl.AuthenticationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.AuthenticationHandler;
import com.bt.pi.sss.UserManager;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.sun.jersey.spi.container.ContainerRequest;

public class AuthenticationFilterTest {
    private static final String AUTHORIZATION = "Authorization";
    private static final String AWSACCESS_KEY_ID = "AWSAccessKeyId";
    private static final String USER_ID = "user_id";
    private static final String ACCESS_KEY = "accesskey";

    private AuthenticationFilter authenticationFilter;
    private ContainerRequest containerRequest;
    private MultivaluedMap<String, String> queryParamsMap;
    private MultivaluedMap<String, String> requestHeadersMap;
    private UserManager userManager;
    private User user;
    private AuthenticationHandler authHandler;
    private List<AuthenticationHandler> handlers;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        this.authenticationFilter = new AuthenticationFilter();

        userManager = mock(UserManager.class);
        queryParamsMap = mock(MultivaluedMap.class);
        requestHeadersMap = mock(MultivaluedMap.class);
        containerRequest = mock(ContainerRequest.class);
        user = mock(User.class);
        handlers = new ArrayList<AuthenticationHandler>();
        authHandler = mock(AuthenticationHandler.class);

        when(containerRequest.getQueryParameters()).thenReturn(queryParamsMap);
        when(containerRequest.getRequestHeaders()).thenReturn(requestHeadersMap);
        when(containerRequest.getRequestUri()).thenReturn(new URI("http://foo.com/thing"));

        when(queryParamsMap.getFirst(AWSACCESS_KEY_ID)).thenReturn(ACCESS_KEY);

        when(userManager.userExists(ACCESS_KEY)).thenReturn(true);
        when(userManager.getUserByAccessKey(ACCESS_KEY)).thenReturn(user);
        when(user.getUsername()).thenReturn(USER_ID);
        when(user.getApiAccessKey()).thenReturn(ACCESS_KEY);
        when(authHandler.canHandle(containerRequest)).thenReturn(true);
        when(authHandler.authenticate(containerRequest)).thenReturn(user);

        handlers.add(authHandler);

        this.authenticationFilter.setUserManager(userManager);
        authenticationFilter.setAuthenticationHandlers(handlers);
    }

    @Test
    public void testFilter() {
        // act
        ContainerRequest result = this.authenticationFilter.filter(containerRequest);

        // assert
        assertNotNull(result);
    }

    @Test
    public void testFilterReplacesEmptyContentType() {
        // setup
        when(containerRequest.getHeaderValue(HttpHeaders.CONTENT_TYPE)).thenReturn("");
        when(containerRequest.getRequestHeaders()).thenReturn(requestHeadersMap);

        // act
        ContainerRequest result = this.authenticationFilter.filter(containerRequest);

        // assert
        assertNotNull(result);
        verify(requestHeadersMap, times(1)).remove(HttpHeaders.CONTENT_TYPE);
        verify(requestHeadersMap, times(1)).add(HttpHeaders.CONTENT_TYPE, ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE);
    }

    @Test
    public void shouldReturnTheRequestWhenValid() {
        // assert
        assertSame(containerRequest, authenticationFilter.filter(containerRequest));
    }

    @Test(expected = WebApplicationException.class)
    public void shouldThrow401IfUserDoesntExist() {
        // setup
        when(userManager.userExists(ACCESS_KEY)).thenReturn(false);

        // act
        try {
            authenticationFilter.filter(containerRequest);
        } catch (WebApplicationException ex) {
            // assert
            assertEquals("Exception thrown was not a 401", 401, ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void shouldPopulatePiUserId() {
        // act
        authenticationFilter.filter(containerRequest);

        // assert
        verify(requestHeadersMap).add(AuthenticationFilter.PI_USER_ID_KEY, USER_ID);
        verify(requestHeadersMap).add(AuthenticationFilter.PI_ACCESS_KEY_ID, ACCESS_KEY);
    }

    @Test
    public void shouldMakeUserAnonymousIfNoAccessKeyProvided() {
        // setup
        when(queryParamsMap.getFirst(AWSACCESS_KEY_ID)).thenReturn(null);
        when(containerRequest.getHeaderValue(AUTHORIZATION)).thenReturn(null);

        // act
        authenticationFilter.filter(containerRequest);

        // assert
        verify(requestHeadersMap, times(1)).add(AuthenticationFilter.PI_USER_ID_KEY, AuthenticationFilter.ANONYMOUS_USER);
        verify(requestHeadersMap, times(1)).add(AuthenticationFilter.PI_ACCESS_KEY_ID, AuthenticationFilter.ANONYMOUS_USER);
    }

    @Test
    public void shouldNotUseTheAnonymousUserIfTheAccessKeyIsInTheQuery() {
        // setup
        when(queryParamsMap.getFirst(AWSACCESS_KEY_ID)).thenReturn(ACCESS_KEY);
        when(containerRequest.getHeaderValue(AUTHORIZATION)).thenReturn(null);

        // act
        authenticationFilter.filter(containerRequest);

        // assert
        verify(requestHeadersMap, times(0)).add(AuthenticationFilter.PI_USER_ID_KEY, AuthenticationFilter.ANONYMOUS_USER);
    }

    @Test
    public void shouldNotUseTheAnonymousUserIfTheAccessKeyIsInTheHeaders() {
        // setup
        when(queryParamsMap.getFirst(AWSACCESS_KEY_ID)).thenReturn(null);
        when(containerRequest.getHeaderValue(AUTHORIZATION)).thenReturn("AWS " + ACCESS_KEY + ":FOO");

        // act
        authenticationFilter.filter(containerRequest);

        // assert
        verify(requestHeadersMap, times(0)).add(AuthenticationFilter.PI_USER_ID_KEY, AuthenticationFilter.ANONYMOUS_USER);
    }

    @Test
    public void shouldFindAHandlerThatCanHandleTheRequest() throws AuthenticationException {
        // act
        authenticationFilter.filter(containerRequest);

        // assert
        verify(authHandler).canHandle(containerRequest);
        verify(authHandler).authenticate(containerRequest);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldThrow401IfNothingCanHandleTheRequest() {
        // setup
        when(authHandler.canHandle(containerRequest)).thenReturn(false);

        try {
            // act
            authenticationFilter.filter(containerRequest);
        } catch (WebApplicationException ex) {
            // assert
            assertEquals("Did not return a 401 response", 401, ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void shouldThrow401WhenTheHandlerThrowsAuthenticationException() throws AuthenticationException {
        // setup
        when(authHandler.authenticate(containerRequest)).thenThrow(new AuthenticationException());

        // act
        authenticationFilter.filter(containerRequest);
    }
}
