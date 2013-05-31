/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.BucketException;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.filter.TransactionIdRequestFilter;

@Component
public class ResponseUtils {
    private static final String DOUBLE_QUOTE = "\"";
    private static final String DEFAULT_FORCE_CONTENT_ON_GET_OBJECT = "false";
    private static final Log LOG = LogFactory.getLog(ResponseUtils.class);
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private boolean forceContentLengthOnGetObject = Boolean.parseBoolean(DEFAULT_FORCE_CONTENT_ON_GET_OBJECT);

    public ResponseUtils() {
    }

    public Response buildGetObjectResponse(ObjectMetaData objectMetaData) {
        ResponseBuilder builder;
        try {
            builder = Response.ok(objectMetaData.getInputStream());
        } catch (FileNotFoundException e) {
            throw new BucketObjectNotFoundException();
        }
        if (this.forceContentLengthOnGetObject)
            builder.header(HttpHeaders.CONTENT_LENGTH, objectMetaData.getSize());

        addCommonHeaders(objectMetaData, builder);
        buildMetaData(objectMetaData, builder);
        return builder.build();
    }

    public void addCommonHeaders(ObjectMetaData objectMetaData, ResponseBuilder builder) {
        builder.header(HttpHeaders.CONTENT_TYPE, objectMetaData.getContentType());
        builder.header(CONTENT_DISPOSITION, objectMetaData.getContentDisposition());
        builder.header(HttpHeaders.ETAG, DOUBLE_QUOTE + objectMetaData.getETag() + DOUBLE_QUOTE);
    }

    public Response buildHeadObjectResponse(ObjectMetaData objectMetaData) {
        ResponseBuilder builder = Response.ok();
        builder.header(HttpHeaders.CONTENT_LENGTH, objectMetaData.getSize());

        addCommonHeaders(objectMetaData, builder);
        buildMetaData(objectMetaData, builder);
        return builder.build();
    }

    public void buildMetaData(ObjectMetaData objectMetaData, ResponseBuilder builder) {
        Map<String, List<String>> xAmzMetaHeaders = objectMetaData.getXAmzMetaHeaders();
        if (null == xAmzMetaHeaders)
            return;
        for (Entry<String, List<String>> entry : xAmzMetaHeaders.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                builder.header(key, value);
            }
        }
    }

    public Response buildErrorResponse(Throwable t) {
        if (t instanceof BucketException)
            throw (BucketException) t;
        String message = t.getClass().getSimpleName() + ": " + t.getMessage();
        String errorXml = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Error><Code>%s</Code><Message>%s</Message><Resource>%s</Resource><RequestId>%s</RequestId></Error>", StringUtils.remove(Status.INTERNAL_SERVER_ERROR
                .toString(), " "), message, MDC.get(TransactionIdRequestFilter.PI_RESOURCE), MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY));
        return Response.serverError().entity(errorXml).type(MediaType.APPLICATION_XML).build();
    }

    @Property(key = "responseUtils.forceContentLengthOnGetObject", defaultValue = DEFAULT_FORCE_CONTENT_ON_GET_OBJECT)
    public void setForceContentLengthOnGetObject(boolean bool) {
        LOG.warn(String.format("setForceContentLengthOnGetObject(%s)", bool));
        this.forceContentLengthOnGetObject = bool;
    }
}
