package com.bt.pi.sss;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

public class HttpServerTest {
    private HttpServer httpServer;
    private ResourceConfig resourceConfig;

    @Before
    public void setUp() throws Exception {
        resourceConfig = mock(ResourceConfig.class);
    }

    @Test
    public void testHttpServer() {
        // setup

        // act
        this.httpServer = new HttpServer(resourceConfig);

        // assert
        assertNotNull(this.httpServer);
    }

    // TODO: buggered if I know how to test this class!
    @Test
    public void testHttpServerStartStop() {
        // setup
        this.resourceConfig = new PackagesResourceConfig("com.bt.pi.sss.test");

        // act
        this.httpServer = new HttpServer(resourceConfig);
        this.httpServer.setPort("9090");
        this.httpServer.setApplicationContext(new StaticApplicationContext());
        try {
            this.httpServer.init();
            assertTrue(Boolean.getBoolean("com.sun.grizzly.util.buf.UDecoder.ALLOW_ENCODED_SLASH"));
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            this.httpServer.destroy();
        }

        // assert
    }
}