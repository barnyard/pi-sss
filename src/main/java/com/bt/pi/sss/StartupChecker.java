/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.exception.PisssConfigException;

/*
 * check that config is OK to start up
 */
@Component
public class StartupChecker implements InitializingBean {
    private static final Log LOG = LogFactory.getLog(StartupChecker.class);

    private String root;

    public StartupChecker() {
    }

    @Property(key = "bucketRootDirectory", defaultValue = "var/buckets")
    public void setBucketRootDirectory(String bucketRootDirectory) {
        this.root = bucketRootDirectory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.debug("afterPropertiesSet()");
        File rootDir = new File(this.root);
        if (!rootDir.exists()) {
            LOG.error(String.format("directory %s does not exist", this.root));
            if (!rootDir.mkdirs()) {
                String message = String.format("Could not create directory %s", this.root);
                LOG.error(message);
                throw new PisssConfigException(message);
            }
        }
        if (!rootDir.isDirectory()) {
            String message = String.format("%s is not a directory", this.root);
            LOG.error(message);
            throw new PisssConfigException(message);
        }
        LOG.debug(String.format("java.class.path: %s", System.getProperty("java.class.path")));
    }
}
