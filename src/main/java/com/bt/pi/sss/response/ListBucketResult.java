/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.bt.pi.sss.entities.ObjectMetaData;

/**
 * JAXB pojo to represent the S3 ListBucketResult
 */
@XmlRootElement(name = "ListBucketResult")
// , namespace = "http://doc.s3.amazonaws.com/2006-03-01")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListBucketResult {
    @XmlElement(name = "Metadata")
    private List<MetadataEntry> metadata;
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Prefix")
    private String prefix;
    @XmlElement(name = "Marker")
    private String marker;
    @XmlElement(name = "NextMarker")
    private String nextMarker;
    @XmlElement(name = "MaxKeys")
    private int maxKeys;
    @XmlElement(name = "Delimiter")
    private String delimiter;
    @XmlElement(name = "IsTruncated")
    private boolean isTruncated;
    @XmlElement(name = "Contents")
    private List<ListEntry> contentsList;
    @XmlElement(name = "CommonPrefixes")
    private List<PrefixEntry> commonPrefixs;

    public ListBucketResult() {
        // for JAXB
    }

    public ListBucketResult(String bucketName, String aPrefix, String aMarker, Integer aMaxKeys, SortedSet<ObjectMetaData> listOfFilesInBucket) {
        this.name = bucketName;
        this.prefix = aPrefix;
        this.marker = aMarker;
        if (null != aMaxKeys)
            this.maxKeys = aMaxKeys;
        for (ObjectMetaData objectMetaData : listOfFilesInBucket) {
            if (null != prefix && !objectMetaData.getName().startsWith(prefix))
                continue;
            if (null != marker && objectMetaData.getName().compareTo(marker) <= 0)
                continue;
            ListEntry listEntry = new ListEntry(objectMetaData);
            getContents().add(listEntry);
            Map<String, List<String>> xAmzMetaHeaders = objectMetaData.getXAmzMetaHeaders();

            processMetaHeaders(xAmzMetaHeaders);

            if (this.maxKeys > 0) {
                if (getContents().size() >= this.maxKeys) {
                    break;
                }
            }
            // TODO: other meta data
        }
    }

    private void processMetaHeaders(Map<String, List<String>> xAmzMetaHeaders) {
        if (null == xAmzMetaHeaders)
            return;
        for (Entry<String, List<String>> entry : xAmzMetaHeaders.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                MetadataEntry metadataEntry = new MetadataEntry(key, value);
                getMetadata().add(metadataEntry);
            }
        }
    }

    public List<ListEntry> getContents() {
        if (null == this.contentsList)
            this.contentsList = new ArrayList<ListEntry>();
        return this.contentsList;
    }

    public List<MetadataEntry> getMetadata() {
        if (null == this.metadata)
            this.metadata = new ArrayList<MetadataEntry>();
        return this.metadata;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMarker() {
        return marker;
    }

    public String getNextMarker() {
        return nextMarker;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public List<PrefixEntry> getCommonPrefixs() {
        return commonPrefixs;
    }
}
