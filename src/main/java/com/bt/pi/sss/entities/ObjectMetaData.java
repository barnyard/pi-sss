/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.entities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.bt.pi.sss.exception.BucketObjectReadException;
import com.bt.pi.sss.exception.EntityMarshallingException;

/*
 * pojo to store object info
 */
public class ObjectMetaData extends AbstractMetaData implements Comparable<ObjectMetaData> {
    public static final String DEFAULT_OBJECT_CONTENT_TYPE = "binary/octet-stream";
    public static final String FILE_SUFFIX = ".metadata.json";
    private static final Log LOG = LogFactory.getLog(ObjectMetaData.class);
    @JsonIgnore
    private Calendar lastModified;
    @JsonIgnore
    private long size;
    @JsonProperty
    private String contentType;
    @JsonIgnore
    private String absolutePath;
    @JsonProperty
    private String contentDisposition;
    @JsonProperty
    private Map<String, List<String>> xAmzMetaHeaders;
    @JsonProperty
    private String eTag;

    public ObjectMetaData() {
        this.xAmzMetaHeaders = null;
    }

    public ObjectMetaData(String objectName) {
        super(objectName);
        this.xAmzMetaHeaders = null;
    }

    public ObjectMetaData(File theFile) {
        super(decodeFileName(theFile.getName()));
        this.absolutePath = theFile.getAbsolutePath();
        this.size = theFile.length();
        this.lastModified = Calendar.getInstance();
        this.lastModified.setTimeInMillis(theFile.lastModified());
        // make sure this is null to stop NFS mounted drives mis-behaving
        theFile = null;
        fromJson();
    }

    public ObjectMetaData(File theFile, String aContentType, String aContentDisposition, Map<String, List<String>> metaHeaders, String md5) {
        super(decodeFileName(theFile.getName()));
        if (null == aContentType)
            this.contentType = DEFAULT_OBJECT_CONTENT_TYPE;
        else
            this.contentType = aContentType;
        this.absolutePath = theFile.getAbsolutePath();
        this.contentDisposition = aContentDisposition;
        this.xAmzMetaHeaders = metaHeaders;
        this.eTag = md5;
        // make sure this is null to stop NFS mounted drives mis-behaving
        theFile = null;
        toJson();
    }

    private static String decodeFileName(String name) {
        try {
            return URLDecoder.decode(name, "UTF8");
        } catch (UnsupportedEncodingException e) {
            String message = String.format("error decoding object name: %s", name);
            LOG.error(message, e);
            throw new BucketObjectReadException(message);
        }
    }

    @JsonIgnore
    public Calendar getLastModified() {
        return lastModified;
    }

    @JsonIgnore
    public long getSize() {
        return this.size;
    }

    @JsonIgnore
    public String getContentType() {
        return this.contentType;
    }

    @JsonIgnore
    public String getContentDisposition() {
        return this.contentDisposition;
    }

    public static ObjectMetaData fromName(String objectName) {
        return new ObjectMetaData(objectName);
    }

    @Override
    public int compareTo(ObjectMetaData o) {
        return getName().compareTo(o.getName());
    }

    @JsonIgnore
    public String getAbsolutePath() {
        return this.absolutePath;
    }

    // only to used to return data to the client
    @JsonIgnore
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(new File(this.absolutePath));
    }

    @JsonIgnore
    public String getETag() {
        return this.eTag;
    }

    @JsonIgnore
    public Map<String, List<String>> getXAmzMetaHeaders() {
        return this.xAmzMetaHeaders;
    }

    private void fromJson() {
        ObjectMapper objectMapper = getObjectMapper();
        try {
            ObjectMetaData readValue = objectMapper.readValue(getMetaFile(), getClass());
            this.contentType = readValue.contentType;
            if (null == this.contentType)
                this.contentType = DEFAULT_OBJECT_CONTENT_TYPE;
            this.contentDisposition = readValue.contentDisposition;
            this.xAmzMetaHeaders = readValue.xAmzMetaHeaders;
            this.eTag = readValue.eTag;
        } catch (FileNotFoundException e) {
            LOG.warn(String.format("metadata file %s not found, defaulting Content-Type to %s", getMetaFile(), DEFAULT_OBJECT_CONTENT_TYPE));
            this.contentType = DEFAULT_OBJECT_CONTENT_TYPE;
        } catch (JsonParseException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        }
    }

    private void toJson() {
        ObjectMapper objectMapper = getObjectMapper();
        try {
            objectMapper.writeValue(getMetaFile(), this);
        } catch (JsonGenerationException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityMarshallingException(e.getMessage(), e);
        }
    }

    @JsonIgnore
    protected ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @JsonIgnore
    private File getMetaFile() {
        return new File(String.format("%s%s", absolutePath, FILE_SUFFIX));
    }
}
