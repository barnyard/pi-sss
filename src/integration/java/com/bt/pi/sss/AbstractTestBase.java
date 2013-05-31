package com.bt.pi.sss;

import org.junit.Test;

/*
 * this class is an attempt to ensure that all the different clients have tests for the same features
 */
public abstract class AbstractTestBase extends IntegrationTestBase {

    @Test
    public abstract void testGetService() throws Exception;

    @Test
    public abstract void testGetBucket() throws Exception;

    @Test
    public abstract void testGetBucketAcl() throws Exception;

    @Test
    public abstract void testGetBucketLocation() throws Exception;

    @Test
    public abstract void testGetServiceWithBadUser() throws Exception;

    @Test
    public abstract void testGetServiceWithBadSecretKey() throws Exception;

    @Test
    public abstract void testPutBucket() throws Exception;

    @Test
    public abstract void testPutBucketWithRegion() throws Exception;

    @Test
    public abstract void testGetBucketNotFound() throws Exception;

    @Test
    public abstract void testHeadObject() throws Exception;

    @Test
    public abstract void testGetObject() throws Exception;

    @Test
    public abstract void testPutObject() throws Exception;

    @Test
    public abstract void testPutLargeObject() throws Exception;

    @Test
    public abstract void testPutObjectMd5() throws Exception;

    @Test
    public abstract void testDeleteObject() throws Exception;

    @Test
    public abstract void testDeleteBucket() throws Exception;

    @Test
    public abstract void testGetBucketInAnotherRegionShouldReturnPermanentRedirect() throws Exception;

}
