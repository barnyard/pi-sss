package com.bt.pi.sss.filter;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bt.pi.sss.filter.TransactionIdRequestFilter;
import com.bt.pi.sss.filter.TransactionIdResponseFilter;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

public class TransactionIdResponseFilterTest {
    private TransactionIdResponseFilter transactionIdResponseFilter;
    private String txId;
    private ContainerRequest containerRequest;
    private ContainerResponse containerResponse;
    private MultivaluedMap<String, Object> headers;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        this.transactionIdResponseFilter = new TransactionIdResponseFilter();
        this.txId = "abc123";
        MDC.put(TransactionIdRequestFilter.PI_TX_ID_KEY, txId);
        containerRequest = Mockito.mock(ContainerRequest.class);
        containerResponse = Mockito.mock(ContainerResponse.class);
        headers = Mockito.mock(MultivaluedMap.class);
        Mockito.when(containerResponse.getHttpHeaders()).thenReturn(headers);
    }

    @Test
    public void testFilter() {
        // setup

        // act
        ContainerResponse result = this.transactionIdResponseFilter.filter(containerRequest, containerResponse);

        // assert
        assertEquals(containerResponse, result);
        Mockito.verify(headers).add("x-amz-request-id", txId);
    }

    @Test
    public void testFilterNoTxId() {
        // setup
        MDC.remove(TransactionIdRequestFilter.PI_TX_ID_KEY);

        // act
        ContainerResponse result = this.transactionIdResponseFilter.filter(containerRequest, containerResponse);

        // assert
        assertEquals(containerResponse, result);
        Mockito.verify(headers, Mockito.never()).add("x-amz-request-id", txId);
    }
}
