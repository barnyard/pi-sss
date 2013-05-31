package com.bt.pi.sss.filter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Resource;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.sss.BucketMetaDataHelper;
import com.bt.pi.sss.PisssApplicationManager;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Component
public class ValidRegionFilter implements ContainerRequestFilter {

    private static final Log LOG = LogFactory.getLog(ValidRegionFilter.class);

    private static final String REGEX_ONLY_BUCKET = "^([^\\/]+)\\/?$";
    @Resource
    private PisssApplicationManager pisssApplicationManager;

    @Resource
    private BucketMetaDataHelper bucketMetaDataHelper;

    public ValidRegionFilter() {
        bucketMetaDataHelper = null;
        pisssApplicationManager = null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        String requestPath = request.getPath();
        // Don't process if we are listing all buckets or creating a new bucket
        if (StringUtils.isEmpty(requestPath) || (requestPath.matches(REGEX_ONLY_BUCKET) && request.getMethod().equals("PUT")))
            return request;
        String bucketName = requestPath.indexOf('/') > 0 ? requestPath.substring(0, requestPath.indexOf('/')) : requestPath;
        LOG.debug(String.format("Validating Region for bucket: %s", bucketName));
        String bucketRegionName = bucketMetaDataHelper.getLocationForBucket(bucketName);
        LOG.debug("bucketRegionName: " + bucketRegionName);
        Region bucketRegion = getRegionForRegionName(bucketRegionName);
        LOG.debug("bucketRegion: " + bucketRegion);
        int thisNodeRegionCode = pisssApplicationManager.getKoalaIdFactory().getRegion();

        if (thisNodeRegionCode == bucketRegion.getRegionCode()) {
            return request;
        }
        // Permanent Redirect. Location set
        return permanentRedirect(request, bucketRegion, bucketName);
    }

    private Region getRegionForRegionName(String regionName) {
        Regions regions = bucketMetaDataHelper.getRegions();
        Region bucketRegion = regions.getRegion(regionName);

        if (bucketRegion == null) {
            ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Invalid Bucket Region for Bucket Name: " + regionName);
            throw new WebApplicationException(builder.build());

        }
        return bucketRegion;

    }

    private ContainerRequest permanentRedirect(ContainerRequest request, Region bucketRegion, String bucketName) {
        try {
            java.net.URI redirectUri = new URI(bucketRegion.getPisssEndpoint() + "/" + request.getPath());
            LOG.debug("Permanent Redirect to " + redirectUri);
            ResponseBuilder builder = Response.status(Status.MOVED_PERMANENTLY).entity(generateRedirectMessage(request, bucketName, bucketRegion.getPisssEndpoint()));
            builder.location(redirectUri);
            throw new WebApplicationException(builder.build());
        } catch (URISyntaxException e) {
            LOG.debug("Redirect URI Syntax Exception", e);
            ResponseBuilder builder = Response.serverError().entity(e.getMessage());
            throw new WebApplicationException(builder.build());

        }
    }

    private String generateRedirectMessage(ContainerRequest request, String bucketName, String pisssEndpoint) {
        String txId = (String) MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY);
        return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>PermanentRedirect</Code>"
                + "<Message>The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint.</Message>"
                + "<RequestId>%s</RequestId><Bucket>%s</Bucket><HostId>%s</HostId><Endpoint>%s</Endpoint></Error>", txId, MDC.get(TransactionIdRequestFilter.PI_RESOURCE), bucketName, pisssEndpoint);
    }

}
