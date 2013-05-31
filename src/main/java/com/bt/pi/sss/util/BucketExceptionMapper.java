/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;

import com.bt.pi.sss.exception.BucketException;
import com.bt.pi.sss.filter.TransactionIdRequestFilter;

/*
 * mapping of internal exceptions to http responses
 * see http://docs.amazonwebservices.com/AmazonS3/latest/index.html?ErrorCodeList.html
 */
@Component
@Provider
public class BucketExceptionMapper implements ExceptionMapper<BucketException> {
    private static final String EXCEPTION = "Exception";
    private static final int MISSING_CONTENT_LENGTH = 411;
    private Map<String, Integer> exceptionToStatusMap;

    public BucketExceptionMapper() {
        this.exceptionToStatusMap = new HashMap<String, Integer>();
        this.exceptionToStatusMap.put("BucketAlreadyExistsException", Status.CONFLICT.getStatusCode());
        this.exceptionToStatusMap.put("BucketAlreadyOwnedByUserException", Status.CONFLICT.getStatusCode());
        this.exceptionToStatusMap.put("BucketNotEmptyException", Status.CONFLICT.getStatusCode());
        this.exceptionToStatusMap.put("AccessDeniedException", Status.FORBIDDEN.getStatusCode());
        this.exceptionToStatusMap.put("BucketObjectNotFoundException", Status.NOT_FOUND.getStatusCode());
        this.exceptionToStatusMap.put("NoSuchBucketException", Status.NOT_FOUND.getStatusCode());
        this.exceptionToStatusMap.put("EntityTooLargeException", Status.BAD_REQUEST.getStatusCode());
        this.exceptionToStatusMap.put("InvalidBucketNameException", Status.BAD_REQUEST.getStatusCode());
        this.exceptionToStatusMap.put("InvalidArgumentException", Status.BAD_REQUEST.getStatusCode());
        this.exceptionToStatusMap.put("MissingContentLengthException", MISSING_CONTENT_LENGTH);
        this.exceptionToStatusMap.put("BadDigestException", Status.BAD_REQUEST.getStatusCode());
    }

    public Response toResponse(BucketException e) {
        String name = e.getClass().getSimpleName();
        String message = e.getMessage();
        if (this.exceptionToStatusMap.containsKey(name)) {
            int status = this.exceptionToStatusMap.get(name);
            String simpleName = getSimpleName(name);
            if (null == message || message.length() == 0)
                message = simpleName;
            return this.createResponseFromInt(status, simpleName, message);
        }

        message = e.getClass().getSimpleName() + ": " + message;
        return this.createResponseFromInt(Status.INTERNAL_SERVER_ERROR.getStatusCode(), StringUtils.remove(Status.INTERNAL_SERVER_ERROR.toString(), " "), message);
    }

    private String getSimpleName(final String name) {
        String prefix = name;
        if (name.endsWith(EXCEPTION))
            prefix = name.substring(0, name.length() - EXCEPTION.length());
        return prefix;
    }

    private Response createResponseFromInt(final int status, final String code, final String message) {
        String errorXml = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Error><Code>%s</Code><Message>%s</Message><Resource>%s</Resource><RequestId>%s</RequestId></Error>", code, message, MDC
                .get(TransactionIdRequestFilter.PI_RESOURCE), MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY));
        return Response.status(status).entity(errorXml).type(MediaType.APPLICATION_XML).build();
    }
}
