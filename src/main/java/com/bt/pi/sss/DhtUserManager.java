package com.bt.pi.sss;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

/*
 * A DHT implementation of the User Manager
 */
@Component
public class DhtUserManager implements UserManager {
    private static final Log LOG = LogFactory.getLog(DhtUserManager.class);
    private PiIdBuilder piIdBuilder;
    private BlockingDhtCache blockingDhtCache;

    public DhtUserManager() {
        this.piIdBuilder = null;
        this.blockingDhtCache = null;
    }

    @Override
    public User getUserByAccessKey(String accessKey) {
        LOG.debug(String.format("getUserByAccessKey(%s)", accessKey));
        PId userAccessKeyId = piIdBuilder.getPId(UserAccessKey.getUrl(accessKey));
        UserAccessKey userAccessKey = (UserAccessKey) this.blockingDhtCache.get(userAccessKeyId);
        if (null == userAccessKey)
            return null;

        PId userId = piIdBuilder.getPId(User.getUrl(userAccessKey.getUsername()));
        return (User) this.blockingDhtCache.get(userId);
    }

    @Override
    public boolean userExists(String accessKey) {
        LOG.debug(String.format("userExists(%s)", accessKey));
        User user = getUserByAccessKey(accessKey);
        return user != null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource(name = "userBlockingCache")
    public void setUserCache(BlockingDhtCache aBlockingDhtCache) {
        this.blockingDhtCache = aBlockingDhtCache;
    }
}
