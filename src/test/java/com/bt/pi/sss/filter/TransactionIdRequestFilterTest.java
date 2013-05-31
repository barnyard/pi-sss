package com.bt.pi.sss.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bt.pi.sss.filter.TransactionIdRequestFilter;
import com.sun.jersey.spi.container.ContainerRequest;

public class TransactionIdRequestFilterTest {
    private TransactionIdRequestFilter transactionIdFilter;

    @Before
    public void setUp() throws Exception {
        this.transactionIdFilter = new TransactionIdRequestFilter();
    }

    @Test
    public void testFilter() {
        // setup
        ContainerRequest containerRequest = Mockito.mock(ContainerRequest.class);
        String path = "fred/bloggs";
        Mockito.when(containerRequest.getPath()).thenReturn(path);

        // act
        ContainerRequest result = this.transactionIdFilter.filter(containerRequest);

        // assert
        assertNotNull(MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY));
        assertEquals(containerRequest, result);
        assertEquals(path, MDC.get(TransactionIdRequestFilter.PI_RESOURCE));
    }
}
