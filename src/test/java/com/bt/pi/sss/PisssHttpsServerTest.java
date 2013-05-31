package com.bt.pi.sss;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.grizzly.Controller;
import com.sun.grizzly.SSLConfig;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PisssHttpsServer.class, ContainerFactory.class })
@PowerMockIgnore( { "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class PisssHttpsServerTest {
    private String keystoreLocation = "location";
    private String keystorePassword = "password";
    private Adapter adapter;
    protected SSLConfig sslConfig;
    protected GrizzlyWebServer webServer;
    protected SpringComponentProviderFactory springComponentProviderFactory;
    private PisssHttpsServer pisssHttpsServer;
    private SelectorThread selectorThread;

    @Before
    public void setup() {
        sslConfig = mock(SSLConfig.class);
        adapter = mock(Adapter.class);
        springComponentProviderFactory = mock(SpringComponentProviderFactory.class);
        ResourceConfig resourceConfig = mock(ResourceConfig.class);
        selectorThread = mock(SelectorThread.class);

        webServer = mock(GrizzlyWebServer.class);
        when(webServer.getSelectorThread()).thenReturn(selectorThread);

        PowerMockito.mockStatic(ContainerFactory.class);
        when(ContainerFactory.createContainer(eq(Adapter.class), eq(resourceConfig), isA(SpringComponentProviderFactory.class))).thenReturn(adapter);

        pisssHttpsServer = new PisssHttpsServer() {
            @Override
            protected GrizzlyWebServer newGrizzlyWebServer(String path) {
                assertThat(path, equalTo("https://localhost:8883/"));
                return webServer;
            }

            @Override
            protected SSLConfig newSSLConfig() {
                return sslConfig;
            }

            @Override
            protected SpringComponentProviderFactory newSpringComponentProviderFactory() {
                return springComponentProviderFactory;
            }
        };
        pisssHttpsServer.setKeyStoreLocation(keystoreLocation);
        pisssHttpsServer.setPassword(keystorePassword);
        pisssHttpsServer.setResourceConfig(resourceConfig);
    }

    @Test
    public void shouldSetupSSLConfig() throws Exception {
        // act
        pisssHttpsServer.startServer();

        // assert
        verify(webServer).setSSLConfig(sslConfig);
        verify(sslConfig).setKeyManagerFactoryAlgorithm("SunX509");
        verify(sslConfig).setKeyStoreFile(keystoreLocation);
        verify(sslConfig).setKeyPass(keystorePassword);
        verify(sslConfig).setKeyStoreType("JKS");
        verify(sslConfig).setTrustManagerFactoryAlgorithm("SunX509");
        verify(sslConfig).setTrustStoreFile(keystoreLocation);
        verify(sslConfig).setTrustStorePass(keystorePassword);
        verify(sslConfig).setTrustStoreType("JKS");
        verify(sslConfig).setSecurityProtocol("TLS");
        verify(sslConfig).setNeedClientAuth(false);
        verify(sslConfig).setWantClientAuth(false);
    }

    @Test
    public void shouldListenOnSelectorThread() throws Exception {
        // act
        pisssHttpsServer.startServer();

        // assert
        verify(selectorThread).setAdapter(adapter);
        verify(selectorThread).listen();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionOnIOException() throws Exception {
        // setup
        doThrow(new IOException()).when(selectorThread).listen();

        // act
        pisssHttpsServer.startServer();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionOnInstantiationException() throws Exception {
        // setup
        doThrow(new InstantiationException()).when(selectorThread).listen();

        // act
        pisssHttpsServer.startServer();
    }

    @Test
    public void shouldStopSelectorThreadOnDestroy() throws Exception {
        // setup
        Controller controller = mock(Controller.class);
        when(selectorThread.getController()).thenReturn(controller);
        pisssHttpsServer.startServer();

        // act
        pisssHttpsServer.destroy();

        // assert
        verify(controller).stop();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionOnIOExceptionOnDestroy() throws Exception {
        // setup
        doThrow(new IOException()).when(selectorThread).getController();
        pisssHttpsServer.startServer();

        // act
        pisssHttpsServer.destroy();
    }
}
