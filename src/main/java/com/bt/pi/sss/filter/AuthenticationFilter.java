/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.filter;

import java.util.List;

import javax.annotation.Resource;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.AuthenticationHandler;
import com.bt.pi.sss.UserManager;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.sun.jersey.spi.container.ContainerRequest;

public class AuthenticationFilter implements com.sun.jersey.spi.container.ContainerRequestFilter {
    public static final String PI_USER_ID_KEY = "pi-userid";
    public static final String PI_ACCESS_KEY_ID = "pi-access-key-id";
    public static final String ANONYMOUS_USER = "ANONYMOUS";
    private static final String AWSACCESS_KEY_ID = "AWSAccessKeyId";

    private static final Log LOG = LogFactory.getLog(AuthenticationFilter.class);

    private UserManager userManager;
    private List<AuthenticationHandler> authHandlers;

    public AuthenticationFilter() {
        LOG.debug("AuthenticationFilter()");
        userManager = null;
        authHandlers = null;
    }

    @Resource
    public void setUserManager(UserManager value) {
        userManager = value;
    }

    @Resource
    public void setAuthenticationHandlers(List<AuthenticationHandler> handlers) {
        authHandlers = handlers;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        LOG.debug(String.format("Authenticating request to %s", request.getRequestUri().toString()));
        LOG.debug("headers: " + request.getRequestHeaders());

        LOG.debug(request.getQueryParameters());

        // validate the user
        String accessKey = getAccessKey(request);

        if (null == accessKey || accessKey.length() == 0) {
            LOG.debug("Request received with no accesskey to " + request.getAbsolutePath());
            request.getRequestHeaders().add(PI_USER_ID_KEY, ANONYMOUS_USER);
            request.getRequestHeaders().add(PI_ACCESS_KEY_ID, ANONYMOUS_USER);
        } else {
            if (!userManager.userExists(accessKey)) {
                LOG.debug("Rejecting request from inexistent access key " + accessKey);
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }
            User user = null;

            for (AuthenticationHandler handler : authHandlers) {
                if (handler.canHandle(request)) {
                    try {
                        user = handler.authenticate(request);
                    } catch (AuthenticationException e) {
                        LOG.debug("Authentication handler did not like request from user with access key " + accessKey, e);
                        throw new WebApplicationException(e, Status.UNAUTHORIZED);
                    }
                }
            }

            if (null != user) {
                request.getRequestHeaders().add(PI_USER_ID_KEY, user.getUsername());
                request.getRequestHeaders().add(PI_ACCESS_KEY_ID, user.getApiAccessKey());
            } else {
                LOG.debug("Unable to handle request for " + request.getAbsolutePath() + " with headers: " + request.getRequestHeaders());
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }
        }

        // massive hack to make various clients work - maybe factor out into another filter?
        String contentType = request.getHeaderValue(HttpHeaders.CONTENT_TYPE);
        LOG.debug(String.format("%s: %s", HttpHeaders.CONTENT_TYPE, contentType));
        if ("".equals(contentType)) {
            LOG.info(String.format("Setting content type for request without one..."));
            MultivaluedMap<String, String> requestHeaders = request.getRequestHeaders();
            requestHeaders.remove(HttpHeaders.CONTENT_TYPE);
            requestHeaders.add(HttpHeaders.CONTENT_TYPE, ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE);
        }

        return request;
    }

    private String getAccessKey(ContainerRequest request) {
        final int STARTPOS = 4;

        String accessKey = request.getQueryParameters().getFirst(AWSACCESS_KEY_ID);

        if (null == accessKey) {
            accessKey = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
            if (null != accessKey && accessKey.startsWith("AWS")) {
                accessKey = accessKey.substring(STARTPOS, accessKey.indexOf(":"));
            } else {
                accessKey = null;
            }
        }
        return accessKey;
    }
}
