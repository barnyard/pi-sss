/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.filter;

import java.util.UUID;

import org.apache.log4j.MDC;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class TransactionIdRequestFilter implements ContainerRequestFilter {
    public static final String PI_TX_ID_KEY = "TRANSACTION_ID";
    public static final String PI_RESOURCE = "pisss-resource";

    public TransactionIdRequestFilter() {
    }

    @Override
    public ContainerRequest filter(ContainerRequest arg0) {
        String txId = UUID.randomUUID().toString();
        MDC.put(PI_TX_ID_KEY, txId);
        MDC.put(PI_RESOURCE, arg0.getPath());
        return arg0;
    }
}
