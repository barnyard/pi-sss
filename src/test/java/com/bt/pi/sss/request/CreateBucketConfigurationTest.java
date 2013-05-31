package com.bt.pi.sss.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CreateBucketConfigurationTest {

    @Test
    public void shouldReturnLocation() {
        CreateBucketConfiguration config = new CreateBucketConfiguration("Test Location");
        assertEquals("Test Location", config.getLocationConstraint());
    }

    @Test
    public void shouldParseCreateBucketConfigurationStringWithNamespace() throws Exception {
        // setup
        String createBucketConfigurationString = createBucketConfigurationStringForRegion("TEST_REGION", true);
        // act
        CreateBucketConfiguration createBucketConfiguration = CreateBucketConfiguration.parseCreateBucketConfiguration(createBucketConfigurationString);
        // assert
        assertEquals("TEST_REGION", createBucketConfiguration.getLocationConstraint());
    }

    @Test
    public void shouldParseCreateBucketConfigurationStringWithoutNamespace() throws Exception {
        // setup
        String createBucketConfigurationString = createBucketConfigurationStringForRegion("TEST_REGION", false);
        // act
        CreateBucketConfiguration createBucketConfiguration = CreateBucketConfiguration.parseCreateBucketConfiguration(createBucketConfigurationString);
        // assert
        assertEquals("TEST_REGION", createBucketConfiguration.getLocationConstraint());
    }

    private String createBucketConfigurationStringForRegion(String regionName, boolean withNamespace) {

        return "<CreateBucketConfiguration " + (withNamespace ? "xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"" : "") + "><LocationConstraint>" + regionName + "</LocationConstraint></CreateBucketConfiguration>";
    }

}
