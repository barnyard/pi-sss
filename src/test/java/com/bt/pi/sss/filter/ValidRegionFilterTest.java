package com.bt.pi.sss.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.sss.BucketMetaDataHelper;
import com.bt.pi.sss.PisssApplicationManager;
import com.sun.jersey.spi.container.ContainerRequest;

@RunWith(MockitoJUnitRunner.class)
public class ValidRegionFilterTest {

    private static final String REGION_1 = "REGION_1";

    private static final int REGION_1_CODE = 1;
    private static final String bucketName = "bucketName";

    @InjectMocks
    private ValidRegionFilter validRegionFilter = new ValidRegionFilter();

    @Mock
    private PisssApplicationManager pisssApplicationManager;

    @Mock
    private KoalaIdFactory koalaIdFactory;

    @Mock
    private BucketMetaDataHelper bucketMetaDataHelper;

    @Before
    public void setup() {
        Regions regions = new Regions();
        regions.addRegion(new Region(REGION_1, REGION_1_CODE, "", ""));
        regions.addRegion(new Region("REGION_2", 2, "", ""));
        doReturn(regions).when(bucketMetaDataHelper).getRegions();
        doReturn(koalaIdFactory).when(pisssApplicationManager).getKoalaIdFactory();
        doReturn(REGION_1_CODE).when(koalaIdFactory).getRegion();

    }

    @Test
    public void shouldFilterEmptyRequestPath() {
        // setup
        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getPath()).thenReturn("");
        // act
        ContainerRequest result = validRegionFilter.filter(request);
        // assert
        assertEquals(request, result);
    }

    @Test
    public void shouldReturnSameRequestWhenBucketInRegion() {
        // setup
        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getPath()).thenReturn(bucketName);
        when(request.getMethod()).thenReturn("GET");
        doReturn(REGION_1).when(bucketMetaDataHelper).getLocationForBucket(bucketName);
        // act
        ContainerRequest result = validRegionFilter.filter(request);
        // assert
        assertEquals(request, result);
    }

    @Test
    public void shouldReturnPermanentRedirectWhenBucketNotInRegion() {
        // setup
        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getPath()).thenReturn(bucketName);
        when(request.getMethod()).thenReturn("GET");
        doReturn("REGION_2").when(bucketMetaDataHelper).getLocationForBucket(bucketName);
        MDC.put(TransactionIdRequestFilter.PI_RESOURCE, "1111111");
        MDC.put(TransactionIdRequestFilter.PI_TX_ID_KEY, "2222222");

        // act
        try {
            validRegionFilter.filter(request);
            fail();
        } catch (WebApplicationException e) {
            // assert
            assertEquals(Status.MOVED_PERMANENTLY.getStatusCode(), e.getResponse().getStatus());
            String errorMessage = (String) e.getResponse().getEntity();
            assertTrue(errorMessage.contains((String) MDC.get(TransactionIdRequestFilter.PI_RESOURCE)));
            assertTrue(errorMessage.contains((String) MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY)));
            assertTrue(errorMessage.contains(bucketName));
        }
    }

}