package com.bt.pi.sss;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import com.bt.pi.core.conf.Property;
import com.sun.grizzly.SSLConfig;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

public class PisssHttpsServer implements ApplicationContextAware {
    private static final String DEFAULT_HTTPS_PORT = "8883";
    private static final String SUN_X509 = "SunX509";

    private String password;
    private String keyStoreLocation;
    private String keyManagerAlgorithm = SUN_X509;
    private String trustManagerAlgorithm = SUN_X509;
    private String sslContextProtocol = "TLS";
    private String keyStoreType = "JKS";

    private int port;
    private ApplicationContext applicationContext;

    private ResourceConfig resourceConfig;
    private SelectorThread selectorThread;

    public PisssHttpsServer() {
        port = Integer.parseInt(DEFAULT_HTTPS_PORT);
        resourceConfig = null;
        selectorThread = null;
    }

    public void setResourceConfig(ResourceConfig aResourceConfig) {
        resourceConfig = aResourceConfig;
    }

    @Property(key = "pisss.https.port", defaultValue = DEFAULT_HTTPS_PORT)
    public void setPort(int aPort) {
        port = aPort;
    }

    public void setKeyManagerAlgorithm(String aKeyManagerAlgorithm) {
        this.keyManagerAlgorithm = aKeyManagerAlgorithm;
    }

    public void setTrustManagerAlgorithm(String aTrustManagerAlgorithm) {
        this.trustManagerAlgorithm = aTrustManagerAlgorithm;
    }

    public void setSslContextProtocol(String aSslContextProtocol) {
        this.sslContextProtocol = aSslContextProtocol;
    }

    public void setKeyStoreType(String aKeyStoreType) {
        this.keyStoreType = aKeyStoreType;
    }

    public void setKeyStoreLocation(String aKeyStoreLocation) {
        this.keyStoreLocation = aKeyStoreLocation;
    }

    public void setPassword(String aPassword) {
        this.password = aPassword;
    }

    public void startServer() {
        System.setProperty("com.sun.grizzly.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

        String path = String.format("https://localhost:%s/", port);

        SSLConfig sslConfig = newSSLConfig();
        sslConfig.setKeyManagerFactoryAlgorithm(keyManagerAlgorithm);
        sslConfig.setKeyStoreFile(keyStoreLocation);
        sslConfig.setKeyPass(password);
        sslConfig.setKeyStoreType(keyStoreType);
        sslConfig.setSecurityProtocol(sslContextProtocol);
        sslConfig.setTrustManagerFactoryAlgorithm(trustManagerAlgorithm);
        sslConfig.setTrustStoreFile(keyStoreLocation);
        sslConfig.setTrustStorePass(password);
        sslConfig.setTrustStoreType(keyStoreType);
        sslConfig.setWantClientAuth(false);
        sslConfig.setNeedClientAuth(false);

        GrizzlyWebServer ws = newGrizzlyWebServer(path);
        ws.setSSLConfig(sslConfig);
        Adapter adapter = ContainerFactory.createContainer(Adapter.class, resourceConfig, newSpringComponentProviderFactory());
        selectorThread = ws.getSelectorThread();
        selectorThread.setAdapter(adapter);

        try {
            selectorThread.listen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    protected SpringComponentProviderFactory newSpringComponentProviderFactory() {
        return new SpringComponentProviderFactory(resourceConfig, (ConfigurableApplicationContext) applicationContext);
    }

    protected SSLConfig newSSLConfig() {
        return new SSLConfig();
    }

    protected GrizzlyWebServer newGrizzlyWebServer(String path) {
        return new GrizzlyWebServer(port, path, true);
    }

    public void destroy() {
        try {
            if (selectorThread != null)
                selectorThread.getController().stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext anApplicationContext) {
        this.applicationContext = anApplicationContext;
    }
}
