/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.io.InputStream;
import java.util.SortedSet;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BucketException;
import com.bt.pi.sss.filter.AuthenticationFilter;
import com.bt.pi.sss.request.CreateBucketConfiguration;
import com.bt.pi.sss.response.AccessControlPolicy;
import com.bt.pi.sss.response.ListAllMyBucketsResult;
import com.bt.pi.sss.response.ListBucketResult;
import com.bt.pi.sss.util.DateUtils;
import com.bt.pi.sss.util.HeaderUtils;
import com.bt.pi.sss.util.ResponseUtils;

/**
 * REST resource class - entry point for requests
 */
@Component
@Path(ServiceResource.SLASH)
public class ServiceResource {
    static final String SLASH = "/";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String X_AMZ_ACL = "x-amz-acl";
    private static final String BUCKET_NAME = "bucketName";
    private static final String OBJECT_NAME = "objectName";
    private static final String LEFT_BRACE = "{";
    private static final String RIGHT_BRACE = "}";
    private static final Log LOG = LogFactory.getLog(ServiceResource.class);
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private BucketManager bucketManager;
    private HeaderUtils headerUtils;
    private DateUtils dateUtils;
    private ResponseUtils responseUtils;

    public ServiceResource() {
        LOG.debug("ServiceResource()");
        this.bucketManager = null;
        this.headerUtils = null;
        this.dateUtils = null;
        this.responseUtils = null;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ListAllMyBucketsResult listAllMyBuckets(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @HeaderParam(AuthenticationFilter.PI_ACCESS_KEY_ID) String accessKey) {
        LOG.debug(String.format("listAllMyBuckets(%s, %s)", userName, accessKey));
        checkAnonymous(userName);
        try {
            return new ListAllMyBucketsResult(this.bucketManager.getBucketList(userName), accessKey, userName);
        } catch (Throwable t) {
            throw processThrowable(t);
        }
    }

    private void checkAnonymous(String userName) {
        if (AuthenticationFilter.ANONYMOUS_USER.equals(userName))
            throw new AccessDeniedException();
    }

    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE)
    @PUT
    public Response createBucket(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName, @HeaderParam(X_AMZ_ACL) String xAmzAcl, String createBucketConfigString) {
        LOG.debug(String.format("createBucket(%s, %s, %s, %s)", userName, bucketName, xAmzAcl, createBucketConfigString));
        checkAnonymous(userName);
        try {

            CreateBucketConfiguration createBucketConfig = null;
            if (!StringUtils.isEmpty(createBucketConfigString)) {
                createBucketConfig = CreateBucketConfiguration.parseCreateBucketConfiguration(createBucketConfigString);
            }

            this.bucketManager.createBucket(userName, bucketName, CannedAclPolicy.get(xAmzAcl), createBucketConfig == null ? null : createBucketConfig.getLocationConstraint());
            return Response.ok().build();
        } catch (Throwable t) {
            t.printStackTrace();
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    // TODO: implement delimiter
    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE)
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Object getBucket(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName, @QueryParam("prefix") String prefix, @QueryParam("marker") String marker, @QueryParam("acl") String acl,
            @QueryParam("max-keys") Integer maxKeys, @QueryParam("location") String location) {
        LOG.debug(String.format("getBucket(%s, %s, %s, %s, %s, %d, %s)", userName, bucketName, prefix, marker, acl, maxKeys, location));
        try {
            if (null != location) {
                String bucketLocation = this.bucketManager.getBucketLocation(userName, bucketName);
                return String.format("<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">%s</LocationConstraint>", bucketLocation);
            }
            if (null != acl) {
                CannedAclPolicy cannedAclPolicy = this.bucketManager.getCannedAclPolicy(userName, bucketName);
                return new AccessControlPolicy(userName, cannedAclPolicy);
            }
            SortedSet<ObjectMetaData> listOfFilesInBucket = this.bucketManager.getListOfFilesInBucket(userName, bucketName);
            return new ListBucketResult(bucketName, prefix, marker, maxKeys, listOfFilesInBucket);
        } catch (Throwable t) {
            throw processThrowable(t);
        }
    }

    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE + SLASH + LEFT_BRACE + OBJECT_NAME + RIGHT_BRACE)
    @HEAD
    public Response headObject(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName, @PathParam(OBJECT_NAME) String objectName) {
        LOG.debug(String.format("headObject(%s, %s, %s)", userName, bucketName, objectName));
        try {
            ObjectMetaData objectMetaData = this.bucketManager.readObject(userName, bucketName, objectName);
            return this.responseUtils.buildHeadObjectResponse(objectMetaData);
        } catch (Throwable t) {
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    /*
     * Content-md5 header is a base64 encoded md5 digest
     * etag header is a hex encoded md5 digest
     */
    // TODO: implement Range
    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE + SLASH + LEFT_BRACE + OBJECT_NAME + RIGHT_BRACE)
    @GET
    public Response getObject(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName, @PathParam(OBJECT_NAME) String objectName, @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) String ifNoneMatch, @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) String ifModifiedSince, @HeaderParam(HttpHeaders.IF_UNMODIFIED_SINCE) String ifUnmodifiedSince) {
        LOG.debug(String.format("getObject(%s, %s, %s, %s, %s, %s, %s)", userName, bucketName, objectName, ifMatch, ifNoneMatch, ifModifiedSince, ifUnmodifiedSince));
        try {
            ObjectMetaData objectMetaData = this.bucketManager.readObject(userName, bucketName, objectName);
            if (null != ifMatch) {
                String strippedIfMatch = strip(ifMatch);
                if (!strippedIfMatch.equals(objectMetaData.getETag()))
                    return Response.status(Status.PRECONDITION_FAILED).build();
            }
            if (null != ifNoneMatch) {
                String strippedIfNonMatch = strip(ifNoneMatch);
                if (strippedIfNonMatch.equals(objectMetaData.getETag()))
                    return Response.status(Status.NOT_MODIFIED).build();
            }
            if (null != ifModifiedSince)
                if (objectMetaData.getLastModified().getTimeInMillis() <= this.dateUtils.parseHttpDate(ifModifiedSince).getTimeInMillis())
                    return Response.status(Status.NOT_MODIFIED).build();
            if (null != ifUnmodifiedSince)
                if (objectMetaData.getLastModified().getTimeInMillis() > this.dateUtils.parseHttpDate(ifUnmodifiedSince).getTimeInMillis())
                    return Response.status(Status.PRECONDITION_FAILED).build();
            return this.responseUtils.buildGetObjectResponse(objectMetaData);
        } catch (Throwable t) {
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    private String strip(String s) {
        return StringUtils.strip(s, DOUBLE_QUOTE);
    }

    // TODO: implement Cache-Control
    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE + SLASH + LEFT_BRACE + OBJECT_NAME + RIGHT_BRACE)
    @PUT
    public Response putObject(@Context HttpHeaders httpHeaders, @HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType, @HeaderParam("Content-MD5") String contentMd5,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) String contentLength, @HeaderParam(CONTENT_DISPOSITION) String contentDisposition, @PathParam(BUCKET_NAME) String bucketName, @PathParam(OBJECT_NAME) String objectName, InputStream is) {
        LOG.debug(String.format("putObject(%s, %s, %s, %s, %s, %s, %s, %s)", httpHeaders, userName, contentType, contentMd5, contentLength, bucketName, objectName, is));
        try {
            this.headerUtils.validateContentLength(contentLength);
            String md5 = this.bucketManager.storeObject(userName, bucketName, objectName, is, contentType, contentMd5, contentDisposition, this.headerUtils.getXAmzMetaHeaders(httpHeaders));
            return Response.ok().header(HttpHeaders.ETAG, DOUBLE_QUOTE + md5 + DOUBLE_QUOTE).build();
        } catch (Throwable t) {
            t.printStackTrace();
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE + SLASH + LEFT_BRACE + OBJECT_NAME + RIGHT_BRACE)
    @DELETE
    public Response deleteObject(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName, @PathParam(OBJECT_NAME) String objectName) {
        try {
            LOG.debug(String.format("deleteObject(%s, %s, %s)", userName, bucketName, objectName));
            this.bucketManager.deleteObject(userName, bucketName, objectName);
            return Response.status(Status.NO_CONTENT).build();
        } catch (Throwable t) {
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    @Path(SLASH + LEFT_BRACE + BUCKET_NAME + RIGHT_BRACE)
    @DELETE
    public Response deleteBucket(@HeaderParam(AuthenticationFilter.PI_USER_ID_KEY) String userName, @PathParam(BUCKET_NAME) String bucketName) {
        LOG.debug(String.format("deleteBucket(%s, %s)", userName, bucketName));
        try {
            this.bucketManager.deleteBucket(userName, bucketName);
            return Response.status(Status.NO_CONTENT).build();
        } catch (Throwable t) {
            return this.responseUtils.buildErrorResponse(t);
        }
    }

    @Resource
    public void setBucketManager(BucketManager aBucketManager) {
        this.bucketManager = aBucketManager;
    }

    @Resource
    public void setHeaderUtils(HeaderUtils aHeaderUtils) {
        this.headerUtils = aHeaderUtils;
    }

    @Resource(type = DateUtils.class)
    public void setDateUtils(DateUtils aDateUtils) {
        this.dateUtils = aDateUtils;
    }

    @Resource
    public void setResponseUtils(ResponseUtils aResponseUtils) {
        this.responseUtils = aResponseUtils;
    }

    private RuntimeException processThrowable(Throwable t) {
        if (t instanceof BucketException)
            return (BucketException) t;
        LOG.error(t.getMessage(), t);

        return new WebApplicationException(t, this.responseUtils.buildErrorResponse(t));
    }

}
