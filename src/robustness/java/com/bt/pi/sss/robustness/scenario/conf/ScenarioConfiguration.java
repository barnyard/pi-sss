package com.bt.pi.sss.robustness.scenario.conf;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class ScenarioConfiguration {
    private static final String ACCESS_KEY = "ACCESS_KEY";
    private static final String SECRET_KEY = "SECRET_KEY";
    private static final String PISSS_HOST = "PISSS_HOST";
    private static final String PISSS_PORT = "PISSS_PORT";
    private static final String PROXY_HOST = "http.proxyHost";
    private static final String PROXY_PORT = "http.proxyPort";

    private String accessKey;
    private String secretKey;
    private String pisssHost;
    private String pisssPort;
    private String proxyHost;
    private String proxyPort;

    public String getAccessKey() {
        return accessKey;
    }

    @PostConstruct
    public void setAccessKey() {
        accessKey = System.getProperty(ACCESS_KEY);
    }

    public String getSecretKey() {
        return secretKey;
    }

    @PostConstruct
    public void setSecretKey() {
        secretKey = System.getProperty(SECRET_KEY);
    }

    public String getPisssHost() {
        return pisssHost;
    }

    @PostConstruct
    public void setPisssHost() {
        pisssHost = System.getProperty(PISSS_HOST);
    }

    public String getPisssPort() {
        return pisssPort;
    }

    @PostConstruct
    public void setPisssPort() {
        pisssPort = System.getProperty(PISSS_PORT);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    @PostConstruct
    public void setProxyHost() {
        proxyHost = System.getProperty(PROXY_HOST);
    }

    public String getProxyPort() {
        return proxyPort;
    }

    @PostConstruct
    public void setProxyPort() {
        proxyPort = System.getProperty(PROXY_PORT);
    }
}
