package com.bt.pi.sss.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BucketCollectionEntityTest {

    private BucketCollectionEntity bucketCollectionEntity = new BucketCollectionEntity();

    @Test
    public void shouldReturnUrl() {
        // setup
        bucketCollectionEntity.setId("12345678");
        // act
        String url = bucketCollectionEntity.getUrl();
        // assert
        assertEquals("bucketcollection:12345678", url);
    }
}
