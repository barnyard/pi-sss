package com.bt.pi.sss.entities;

import java.util.HashSet;
import java.util.Set;

import com.bt.pi.core.entity.PiEntityBase;

public class BucketCollectionEntity extends PiEntityBase {

    private static final String SCHEME = "bucketcollection";

    private String owner;

    private Set<String> bucketNames = new HashSet<String>();

    private String id;

    public BucketCollectionEntity() {

    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String aOwner) {
        this.owner = aOwner;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    @Override
    public String getUrl() {
        return SCHEME + ":" + id;
    }

    public Set<String> getBucketNames() {
        return bucketNames;
    }

    public void setBucketNames(Set<String> theBucketNames) {
        this.bucketNames = theBucketNames;
    }

    public String getId() {
        return id;
    }

    public void setId(String theId) {
        this.id = theId;
    }

    @Override
    public String toString() {
        return getUrl() + "[" + getBucketNames() + "]";
    }
}
