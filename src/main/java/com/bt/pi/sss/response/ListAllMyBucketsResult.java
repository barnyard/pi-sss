/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.BucketMetaData;

/**
 * JAXB pojo to represent the S3 ListAllMyBucketsResult
 */
@XmlRootElement(name = "ListAllMyBucketsResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListAllMyBucketsResult {
    private static final Log LOG = LogFactory.getLog(ListAllMyBucketsResult.class);

    @XmlElement(name = "Owner")
    private CanonicalUser owner;
    @XmlElementWrapper(name = "Buckets")
    @XmlElement(name = "Bucket")
    private List<Bucket> buckets;

    public ListAllMyBucketsResult() {
    }

    public ListAllMyBucketsResult(List<BucketMetaData> bucketMetaDataList, String anAccessKey, String aUserName) {
        this.buckets = new ArrayList<Bucket>();
        for (BucketMetaData bucketMetaData : bucketMetaDataList) {
            LOG.debug("Adding bucket:" + bucketMetaData);
            this.buckets.add(new Bucket(bucketMetaData));
        }

        this.owner = new CanonicalUser(anAccessKey, aUserName);
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public CanonicalUser getOwner() {
        return this.owner;
    }
}
