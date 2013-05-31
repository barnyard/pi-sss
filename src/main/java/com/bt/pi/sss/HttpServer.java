/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import com.bt.pi.core.conf.Property;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

/**
 * Simple wrapper for HttpServerFactory so that we can use an embedded http server and tell it to use Spring beans as
 * the REST resources
 */
public class HttpServer implements ApplicationContextAware {
    private static final Log LOG = LogFactory.getLog(HttpServer.class);
    private static final String DEFAULT_HTTP_PORT = "8080";
    private SelectorThread server;
    private String port;
    private ResourceConfig resourceConfig;
    private ApplicationContext applicationContext;

    public HttpServer(ResourceConfig aResourceConfig) {
        this.port = DEFAULT_HTTP_PORT;
        this.resourceConfig = aResourceConfig;
    }

    @Property(key = "pisss.http.port", defaultValue = DEFAULT_HTTP_PORT)
    public void setPort(String aPort) {
        this.port = aPort;
    }

    public void init() {
        try {
            System.setProperty("com.sun.grizzly.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
            IoCComponentProviderFactory ioCComponentProviderFactory = new SpringComponentProviderFactory(this.resourceConfig, (ConfigurableApplicationContext) this.applicationContext);
            server = GrizzlyServerFactory.create(String.format("http://localhost:%s/", this.port), resourceConfig, ioCComponentProviderFactory);
            LOG.debug("Started pi-sss on port " + this.port);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        try {
            this.server.getController().stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext arg0) {
        this.applicationContext = arg0;
    }
}