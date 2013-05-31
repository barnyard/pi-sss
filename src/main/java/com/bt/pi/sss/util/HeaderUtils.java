/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.exception.EntityTooLargeException;
import com.bt.pi.sss.exception.MissingContentLengthException;

/**
 * some helper method for reading HttpHeaders
 */
@Component
public class HeaderUtils {
    private static final Log LOG = LogFactory.getLog(HeaderUtils.class);
    private long maxObjectSize;

    public HeaderUtils() {
        this.maxObjectSize = Long.parseLong(FileSystemBucketUtils.DEFAULT_MAX_OBJECT_SIZE);
    }

    public void validateContentLength(String contentLength) {
        long result = Long.MIN_VALUE;
        try {
            if (null != contentLength)
                result = Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            throw new MissingContentLengthException();
        }
        if (result == Long.MIN_VALUE)
            throw new MissingContentLengthException();
        if (result > this.maxObjectSize)
            throw new EntityTooLargeException();
    }

    @Property(key = "maxObjectSize", defaultValue = FileSystemBucketUtils.DEFAULT_MAX_OBJECT_SIZE)
    public void setMaxObjectSizeInBytes(Long size) {
        LOG.debug(String.format("setMaxObjectSizeInBytes(%d)", size));
        this.maxObjectSize = size;
    }

    public Map<String, List<String>> getXAmzMetaHeaders(HttpHeaders httpHeaders) {
        LOG.debug(String.format("getXAmzMetaHeaders(%s)", httpHeaders));
        Map<String, List<String>> result = null;
        MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
        for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.startsWithIgnoreCase(key, "x-amz-meta-")) {
                if (null == result)
                    result = new HashMap<String, List<String>>();
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
