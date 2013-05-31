/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import javax.security.sasl.AuthenticationException;

import com.bt.pi.app.common.entities.User;
import com.sun.jersey.spi.container.ContainerRequest;

public interface AuthenticationHandler {
    boolean canHandle(ContainerRequest request);

    User authenticate(ContainerRequest request) throws AuthenticationException;
}
