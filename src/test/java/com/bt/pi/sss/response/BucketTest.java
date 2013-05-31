package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.BucketMetaData;

public class BucketTest {
    private Bucket bucket;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testBucket() {
        assertNotNull(new Bucket());
    }

    // rubbish test
    @Test
    public void testGetCreationDate() {
        // setup
        BucketMetaData bucketMetaData = new BucketMetaData("bucket1");
        bucket = new Bucket(bucketMetaData);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        String expectedResult = simpleDateFormat.format(bucketMetaData.getCreationDate().getTime());

        // act
        String result = bucket.getCreationDate();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetName() {
        // setup
        BucketMetaData bucketMetaData = new BucketMetaData("bucket1");
        bucket = new Bucket(bucketMetaData);

        // act
        String result = bucket.getName();

        // assert
        assertEquals("bucket1", result);
    }
}
