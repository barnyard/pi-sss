/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.security.SignatureException;
import java.util.List;

import javax.annotation.Resource;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.util.AWSAuthHelper;
import com.sun.jersey.spi.container.ContainerRequest;

@Component
public class AWSQueryAuthenticationHandler implements AuthenticationHandler {

    private static final String USER_S_IS_NOT_ENABLED = "User %s is not enabled";
    private static final String BAD_SECRET_KEY = "Bad secret key";
    private static final String THE_SECRET_KEY_WAS_BAD_FOR = "The secret key was bad for ";
    private static final String COLON = ":";
    private static final String NEW_LINE = "\n";
    private static final String AWS = "AWS";
    private static final String DATE_KEY = "Date";

    private static final String AUTHORIZATION_KEY = "Authorization";

    private static final Log LOG = LogFactory.getLog(AWSQueryAuthenticationHandler.class);

    private static final String NEWLINE = NEW_LINE;
    private static final String SIGNATURE_KEY = "Signature";
    private static final String EXPIRES_KEY = "Expires";
    private static final String AWSACCESS_KEY_ID = "AWSAccessKeyId";

    private UserManager userManager;
    private AWSAuthHelper authHelper;

    public AWSQueryAuthenticationHandler() {
        userManager = null;
        authHelper = null;
    }

    @Resource
    public void setUserManager(UserManager value) {
        userManager = value;
    }

    @Resource
    public void setAWSAuthHelper(AWSAuthHelper value) {
        authHelper = value;
    }

    @Override
    public User authenticate(ContainerRequest request) throws AuthenticationException {
        if (!canHandle(request))
            throw new IllegalArgumentException("Unable to handle that request.");

        MultivaluedMap<String, String> queryParams = request.getQueryParameters();

        String key = getAccessKey(request, queryParams);
        User user = userManager.getUserByAccessKey(key);
        if (!user.isEnabled()) {
            LOG.info(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()));
            throw new AuthenticationException(String.format(USER_S_IS_NOT_ENABLED, user.getUsername()));
        }
        String providedSignature = getSignature(request, queryParams);

        String resource = authHelper.getCanonicalizedResource(request);
        String amzHeaders = authHelper.getCanonicalizedAmzHeaders(request);
        String verb = request.getMethod();
        String contentMd5 = getFromHeadersOrQuery("Content-MD5", queryParams, request);
        String contentType = getFromHeadersOrQuery("Content-Type", queryParams, request);
        String expires = getExpiresOrDate(request, queryParams);

        String toSign = createStringToSign(resource, amzHeaders, verb, contentMd5, contentType, expires);

        String calculatedSignature;
        try {
            calculatedSignature = authHelper.getSignature(user.getApiSecretKey(), toSign);
        } catch (SignatureException ex) {
            LOG.warn(THE_SECRET_KEY_WAS_BAD_FOR + user.getUsername(), ex);
            throw new AuthenticationException(BAD_SECRET_KEY, ex);
        }

        /****************************************************
         * nasty addition to allow walrus-signed end points *
         ****************************************************/
        String walrusToSign = createStringToSign("/services/Walrus" + resource, amzHeaders, verb, contentMd5, contentType, expires);
        String walrusCalculatedSignature;
        try {
            walrusCalculatedSignature = authHelper.getSignature(user.getApiSecretKey(), walrusToSign);
        } catch (SignatureException ex) {
            LOG.warn(THE_SECRET_KEY_WAS_BAD_FOR + user.getUsername(), ex);
            throw new AuthenticationException(BAD_SECRET_KEY, ex);
        }
        /********************
         * End of nastiness *
         ********************/

        if (!providedSignature.equals(calculatedSignature)) {
            // additional check to allow walrus-signed end points
            if (!providedSignature.equals(walrusCalculatedSignature)) {
                LOG.debug("Signatures do not match for request to " + request.getAbsolutePath() + " by user " + user.getUsername());
                LOG.debug("Expected " + calculatedSignature + " but received " + providedSignature);
                LOG.debug("The string used for signing was " + toSign.replace(NEW_LINE, "\\n"));
                throw new AuthenticationException("Signatures do not match");
            }
        }

        return user;
    }

    @Override
    public boolean canHandle(ContainerRequest request) {
        MultivaluedMap<String, String> queryParams = request.getQueryParameters();
        try {
            String sig = getSignature(request, queryParams);
            String exp = getExpiresOrDate(request, queryParams);
            String key = getAccessKey(request, queryParams);

            if (isNullOrEmpty(sig) || isNullOrEmpty(exp) || isNullOrEmpty(key)) {
                return false;
            }
            return true;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    private String getSignature(ContainerRequest request, MultivaluedMap<String, String> queryParams) {
        String sig = getFromHeadersOrQuery(SIGNATURE_KEY, queryParams, request);
        if (isNullOrEmpty(sig)) {
            sig = request.getHeaderValue(AUTHORIZATION_KEY);
            if (null != sig && sig.startsWith(AWS)) {
                sig = sig.substring(sig.indexOf(COLON) + 1);
            } else {
                sig = null;
            }
        }
        return sig;
    }

    private String getAccessKey(ContainerRequest request, MultivaluedMap<String, String> queryParams) {
        final int STARTPOS = 4;
        String accessKey = getFromHeadersOrQuery(AWSACCESS_KEY_ID, queryParams, request);
        if (isNullOrEmpty(accessKey)) {
            accessKey = request.getHeaderValue(AUTHORIZATION_KEY);
            if (null != accessKey && accessKey.startsWith(AWS)) {
                accessKey = accessKey.substring(STARTPOS, accessKey.indexOf(COLON));
            } else {
                accessKey = null;
            }
        }
        return accessKey;
    }

    private String getExpiresOrDate(ContainerRequest request, MultivaluedMap<String, String> queryParams) {
        String date = getFromHeadersOrQuery(EXPIRES_KEY, queryParams, request);
        if (isNullOrEmpty(date)) {
            date = request.getHeaderValue(DATE_KEY);
        }
        return date;
    }

    private String getFromHeadersOrQuery(String key, MultivaluedMap<String, String> queryParams, ContainerRequest request) {
        List<String> paramValues = queryParams.get(key);

        if (null != paramValues) {
            return paramValues.get(0);
        } else {
            return request.getHeaderValue(key);
        }
    }

    private String createStringToSign(String resource, String amzHeaders, String verb, String contentMd5, String contentType, String expires) {
        String toSign = verb + NEWLINE + replaceNullForEmtpy(contentMd5) + NEWLINE + replaceNullForEmtpy(contentType) + NEWLINE + replaceNullForEmtpy(expires) + NEWLINE + amzHeaders + resource;
        return toSign;
    }

    private String replaceNullForEmtpy(String value) {
        if (null == value) {
            return "";
        } else {
            return value;
        }
    }

    private boolean isNullOrEmpty(String str) {
        return !((null != str) && (str.length() > 0));
    }
}
