/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class TransactionIdResponseFilter implements ContainerResponseFilter {
    private static final Log LOG = LogFactory.getLog(TransactionIdResponseFilter.class);

    public TransactionIdResponseFilter() {
    }

    @Override
    public ContainerResponse filter(ContainerRequest arg0, ContainerResponse arg1) {
        LOG.debug(String.format("filter(%s, %s)", arg0, arg1));
        String txId = (String) MDC.get(TransactionIdRequestFilter.PI_TX_ID_KEY);
        if (null != txId) {
            arg1.getHttpHeaders().add("x-amz-request-id", txId);
        }
        return arg1;
    }
}
